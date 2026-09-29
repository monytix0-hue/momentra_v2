import type { PoolClient } from 'pg';
import { z } from 'zod';
import Decimal from 'decimal.js';
import type { RequestContext } from '../../platform/request-context/context';
import { AppError, ErrorCode } from '../../platform/errors/errors';
import { assertGovernanceAllowed } from '../governance/resolver';
import {
  assertActiveCompanyMember,
  assertCompanyMomentAccess,
  assertNotCompanyObserver,
} from './membership';
import { travelCurrencyCodeSchema } from '../finance/travel-currencies';
import { paymentMethodCodeSchema } from '../finance/financial-account';

const moneyString = z.string().regex(/^\d+(\.\d{1,4})?$/);
const OVERDUE_DAYS = 30;

export const createKhataPartySchema = z
  .object({
    name: z.string().min(1).max(300),
    partyKind: z.enum(['CUSTOMER', 'SUPPLIER']),
    phone: z.string().max(30).optional(),
  })
  .strict();

export const createKhataEntrySchema = z
  .object({
    partyId: z.string().uuid(),
    entryType: z.enum(['CREDIT', 'PAYMENT']),
    amount: moneyString,
    currencyCode: travelCurrencyCodeSchema.optional(),
    note: z.string().max(500).optional(),
    paymentMethodCode: paymentMethodCodeSchema.optional(),
    effectiveAt: z
      .string()
      .refine((s) => !Number.isNaN(Date.parse(s)), { message: 'Invalid date' })
      .optional(),
  })
  .strict();

function parseMoney(raw: string): Decimal {
  return new Decimal(raw);
}

export async function createKhataParty(
  client: PoolClient,
  ctx: RequestContext,
  companyId: string,
  body: z.infer<typeof createKhataPartySchema>
): Promise<{
  partyId: string;
  companyId: string;
  name: string;
  partyKind: string;
  phone: string | null;
}> {
  await assertActiveCompanyMember(client, ctx, companyId);
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'COMPANY_UPDATE',
    resourceType: 'VENDOR',
    companyId,
  });

  const phone = body.phone?.trim() || null;
  const contact = phone ? { phone } : {};
  const inserted = await client.query<{ vendor_id: string }>(
    `INSERT INTO business.vendor (
       company_id, name, vendor_type, contact_details, status, version
     ) VALUES ($1, $2, $3, $4::jsonb, 'ACTIVE', 1)
     RETURNING vendor_id`,
    [companyId, body.name.trim(), body.partyKind, JSON.stringify(contact)]
  );
  return {
    partyId: inserted.rows[0]!.vendor_id,
    companyId,
    name: body.name.trim(),
    partyKind: body.partyKind,
    phone,
  };
}

export async function createKhataEntry(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  body: z.infer<typeof createKhataEntrySchema>
): Promise<{
  entryId: string;
  partyId: string;
  companyId: string;
  entryType: string;
  amount: string;
  currencyCode: string;
  balanceDue: string;
}> {
  const scope = await assertCompanyMomentAccess(client, ctx, momentId);
  assertNotCompanyObserver(scope.membershipType);
  await assertGovernanceAllowed(client, ctx, {
    actionCode: 'COMPANY_UPDATE',
    resourceType: 'VENDOR',
    companyId: scope.companyId,
  });

  const amount = parseMoney(body.amount);
  if (amount.lte(0)) {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Amount must be positive.', 400);
  }

  const party = await client.query<{ vendor_id: string; status: string }>(
    `SELECT vendor_id, status FROM business.vendor
     WHERE vendor_id = $1 AND company_id = $2`,
    [body.partyId, scope.companyId]
  );
  if (!party.rows[0] || party.rows[0].status !== 'ACTIVE') {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Party not found for this company.', 400);
  }

  if (body.entryType === 'PAYMENT' && !body.paymentMethodCode) {
    throw new AppError(
      ErrorCode.VALIDATION_FAILED,
      'Payment method is required for collections.',
      400
    );
  }

  const currencyCode = (body.currencyCode ?? 'INR').toUpperCase();
  const effectiveAt = body.effectiveAt
    ? new Date(body.effectiveAt).toISOString()
    : new Date().toISOString();

  const inserted = await client.query<{ entry_id: string }>(
    `INSERT INTO business.khata_entry (
       company_id, moment_id, party_vendor_id, entry_type, amount, currency_code,
       payment_method_code, note, effective_at, created_by_user_id
     ) VALUES ($1, $2::uuid, $3::uuid, $4, $5::numeric, $6, $7, $8, $9::timestamptz, $10::uuid)
     RETURNING entry_id`,
    [
      scope.companyId,
      momentId,
      body.partyId,
      body.entryType,
      amount.toFixed(4),
      currencyCode,
      body.paymentMethodCode ?? null,
      body.note?.trim() || null,
      effectiveAt,
      ctx.userId,
    ]
  );

  const bal = await partyBalance(client, scope.companyId, body.partyId, currencyCode);
  return {
    entryId: inserted.rows[0]!.entry_id,
    partyId: body.partyId,
    companyId: scope.companyId,
    entryType: body.entryType,
    amount: amount.toFixed(4),
    currencyCode,
    balanceDue: bal.balanceDue,
  };
}

async function partyBalance(
  client: PoolClient,
  companyId: string,
  partyId: string,
  currencyCode: string
): Promise<{ balanceDue: string; oldestOpenAt: string | null; lastActivityAt: string | null }> {
  const row = await client.query<{
    balance: string | null;
    oldest_credit: Date | null;
    last_at: Date | null;
  }>(
    `SELECT
       COALESCE(SUM(CASE WHEN entry_type = 'CREDIT' THEN amount ELSE -amount END), 0)::text AS balance,
       MIN(CASE WHEN entry_type = 'CREDIT' THEN effective_at END) AS oldest_credit,
       MAX(effective_at) AS last_at
     FROM business.khata_entry
     WHERE company_id = $1 AND party_vendor_id = $2 AND currency_code = $3`,
    [companyId, partyId, currencyCode]
  );
  const balanceDue = new Decimal(row.rows[0]?.balance ?? '0').toFixed(4);
  return {
    balanceDue,
    oldestOpenAt: row.rows[0]?.oldest_credit?.toISOString() ?? null,
    lastActivityAt: row.rows[0]?.last_at?.toISOString() ?? null,
  };
}

export async function listKhataParties(
  client: PoolClient,
  ctx: RequestContext,
  companyId: string,
  partyKind: 'CUSTOMER' | 'SUPPLIER' = 'CUSTOMER',
  limit = 100
): Promise<{
  companyId: string;
  partyKind: string;
  items: Array<{
    partyId: string;
    name: string;
    phone: string | null;
    partyKind: string;
    balanceDue: string;
    currencyCode: string;
    lastActivityAt: string | null;
    overdue: boolean;
  }>;
}> {
  await assertActiveCompanyMember(client, ctx, companyId);

  const rows = await client.query<{
    vendor_id: string;
    name: string;
    vendor_type: string | null;
    contact_details: Record<string, unknown> | null;
    balance: string | null;
    currency_code: string | null;
    oldest_credit: Date | null;
    last_at: Date | null;
  }>(
    `SELECT
       v.vendor_id,
       v.name,
       v.vendor_type,
       v.contact_details,
       COALESCE(b.balance, 0)::text AS balance,
       COALESCE(b.currency_code, 'INR') AS currency_code,
       b.oldest_credit,
       b.last_at
     FROM business.vendor v
     LEFT JOIN LATERAL (
       SELECT
         SUM(CASE WHEN e.entry_type = 'CREDIT' THEN e.amount ELSE -e.amount END) AS balance,
         (array_agg(e.currency_code ORDER BY e.effective_at DESC))[1] AS currency_code,
         MIN(CASE WHEN e.entry_type = 'CREDIT' THEN e.effective_at END) AS oldest_credit,
         MAX(e.effective_at) AS last_at
       FROM business.khata_entry e
       WHERE e.company_id = v.company_id AND e.party_vendor_id = v.vendor_id
     ) b ON true
     WHERE v.company_id = $1
       AND v.status = 'ACTIVE'
       AND UPPER(COALESCE(v.vendor_type, '')) = $2
     ORDER BY COALESCE(b.balance, 0) DESC, COALESCE(b.last_at, v.updated_at) DESC NULLS LAST, v.name ASC
     LIMIT $3`,
    [companyId, partyKind, limit]
  );

  const cutoff = Date.now() - OVERDUE_DAYS * 24 * 60 * 60 * 1000;

  return {
    companyId,
    partyKind,
    items: rows.rows.map((r) => {
      const phoneRaw = r.contact_details?.phone;
      const phone = typeof phoneRaw === 'string' && phoneRaw.trim() ? phoneRaw.trim() : null;
      const balanceDue = new Decimal(r.balance ?? '0');
      const overdue =
        balanceDue.gt(0) &&
        r.oldest_credit != null &&
        r.oldest_credit.getTime() < cutoff;
      return {
        partyId: r.vendor_id,
        name: r.name,
        phone,
        partyKind: (r.vendor_type ?? partyKind).toUpperCase(),
        balanceDue: balanceDue.toFixed(4),
        currencyCode: r.currency_code ?? 'INR',
        lastActivityAt: r.last_at?.toISOString() ?? null,
        overdue,
      };
    }),
  };
}

export async function listKhataPartyEntries(
  client: PoolClient,
  ctx: RequestContext,
  companyId: string,
  partyId: string,
  limit = 50
): Promise<{
  companyId: string;
  partyId: string;
  partyName: string;
  balanceDue: string;
  currencyCode: string;
  items: Array<{
    entryId: string;
    entryType: string;
    amount: string;
    currencyCode: string;
    paymentMethodCode: string | null;
    note: string | null;
    effectiveAt: string;
  }>;
}> {
  await assertActiveCompanyMember(client, ctx, companyId);

  const party = await client.query<{ name: string; status: string }>(
    `SELECT name, status FROM business.vendor
     WHERE vendor_id = $1 AND company_id = $2`,
    [partyId, companyId]
  );
  if (!party.rows[0] || party.rows[0].status !== 'ACTIVE') {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Party not found for this company.', 400);
  }

  const rows = await client.query<{
    entry_id: string;
    entry_type: string;
    amount: string;
    currency_code: string;
    payment_method_code: string | null;
    note: string | null;
    effective_at: Date;
  }>(
    `SELECT entry_id, entry_type, amount::text, currency_code, payment_method_code, note, effective_at
     FROM business.khata_entry
     WHERE company_id = $1 AND party_vendor_id = $2
     ORDER BY effective_at DESC, created_at DESC
     LIMIT $3`,
    [companyId, partyId, limit]
  );

  const bal = await partyBalance(client, companyId, partyId, rows.rows[0]?.currency_code ?? 'INR');

  return {
    companyId,
    partyId,
    partyName: party.rows[0].name,
    balanceDue: bal.balanceDue,
    currencyCode: rows.rows[0]?.currency_code ?? 'INR',
    items: rows.rows.map((r) => ({
      entryId: r.entry_id,
      entryType: r.entry_type,
      amount: new Decimal(r.amount).toFixed(4),
      currencyCode: r.currency_code,
      paymentMethodCode: r.payment_method_code,
      note: r.note,
      effectiveAt: r.effective_at.toISOString(),
    })),
  };
}
