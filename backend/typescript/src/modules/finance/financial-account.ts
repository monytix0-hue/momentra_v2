import type { PoolClient } from 'pg';
import type { RequestContext } from '../../platform/request-context/context';
import { AppError, ErrorCode } from '../../platform/errors/errors';
import { z } from 'zod';

export const PAYMENT_METHOD_CODES = ['CASH', 'CARD', 'UPI', 'BANK_TRANSFER', 'WALLET', 'OTHER'] as const;
export type PaymentMethodCode = (typeof PAYMENT_METHOD_CODES)[number];

export const paymentMethodCodeSchema = z.enum(PAYMENT_METHOD_CODES);

export const createFinancialAccountSchema = z
  .object({
    accountType: z.enum(['CASH', 'BANK', 'CARD', 'WALLET', 'INVESTMENT', 'LOAN', 'OTHER']),
    accountName: z.string().min(1).max(200),
    currencyCode: z.string().length(3).toUpperCase(),
    institutionName: z.string().max(200).optional(),
  })
  .strict();

export function derivePaymentMethodFromAccountType(accountType: string): PaymentMethodCode {
  switch (accountType.toUpperCase()) {
    case 'CASH':
      return 'CASH';
    case 'BANK':
      return 'BANK_TRANSFER';
    case 'CARD':
      return 'CARD';
    case 'WALLET':
      return 'WALLET';
    default:
      return 'OTHER';
  }
}

export async function resolveUserAccount(
  client: PoolClient,
  ctx: RequestContext,
  currencyCode: string,
  accountId?: string | null
): Promise<string> {
  if (accountId) {
    const row = await client.query<{ financial_account_id: string; currency_code: string }>(
      `SELECT financial_account_id, currency_code FROM finance.financial_account
       WHERE financial_account_id = $1 AND owner_user_id = $2 AND status = 'ACTIVE'`,
      [accountId, ctx.userId]
    );
    if (!row.rowCount) {
      throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Financial account not found.', 404);
    }
    if (row.rows[0]!.currency_code !== currencyCode) {
      throw new AppError(ErrorCode.VALIDATION_FAILED, 'Account currency does not match transaction currency.', 400);
    }
    return accountId;
  }

  const existing = await client.query<{ financial_account_id: string }>(
    `SELECT financial_account_id FROM finance.financial_account
     WHERE owner_user_id = $1 AND currency_code = $2 AND account_type = 'CASH' AND status = 'ACTIVE'
     LIMIT 1`,
    [ctx.userId, currencyCode]
  );
  if (existing.rows[0]) {
    return existing.rows[0].financial_account_id;
  }

  const created = await client.query<{ financial_account_id: string }>(
    `INSERT INTO finance.financial_account (
       owner_scope_type, owner_scope_id, owner_user_id, account_type, account_name, currency_code, status
     ) VALUES ('USER', $1, $1, 'CASH', 'Primary Cash', $2, 'ACTIVE')
     RETURNING financial_account_id`,
    [ctx.userId, currencyCode]
  );
  return created.rows[0]!.financial_account_id;
}

export async function getAccountType(
  client: PoolClient,
  accountId: string
): Promise<string | null> {
  const row = await client.query<{ account_type: string }>(
    `SELECT account_type FROM finance.financial_account WHERE financial_account_id = $1`,
    [accountId]
  );
  return row.rows[0]?.account_type ?? null;
}

export interface FinancialAccountDto {
  financialAccountId: string;
  accountType: string;
  accountName: string;
  currencyCode: string;
  institutionName: string | null;
  status: string;
}

export async function listUserFinancialAccounts(
  client: PoolClient,
  ctx: RequestContext
): Promise<FinancialAccountDto[]> {
  await resolveUserAccount(client, ctx, 'INR');
  const rows = await client.query<{
    financial_account_id: string;
    account_type: string;
    account_name: string;
    currency_code: string;
    institution_name: string | null;
    status: string;
  }>(
    `SELECT financial_account_id, account_type, account_name, currency_code, institution_name, status
     FROM finance.financial_account
     WHERE owner_user_id = $1 AND status = 'ACTIVE'
     ORDER BY account_name ASC`,
    [ctx.userId]
  );
  return rows.rows.map((r) => ({
    financialAccountId: r.financial_account_id,
    accountType: r.account_type,
    accountName: r.account_name,
    currencyCode: r.currency_code,
    institutionName: r.institution_name,
    status: r.status,
  }));
}

export async function createUserFinancialAccount(
  client: PoolClient,
  ctx: RequestContext,
  body: z.infer<typeof createFinancialAccountSchema>
): Promise<FinancialAccountDto> {
  const inserted = await client.query<{
    financial_account_id: string;
    account_type: string;
    account_name: string;
    currency_code: string;
    institution_name: string | null;
    status: string;
  }>(
    `INSERT INTO finance.financial_account (
       owner_scope_type, owner_scope_id, owner_user_id, account_type, account_name,
       currency_code, institution_name, status
     ) VALUES ('USER', $1, $1, $2, $3, $4, $5, 'ACTIVE')
     RETURNING financial_account_id, account_type, account_name, currency_code, institution_name, status`,
    [ctx.userId, body.accountType, body.accountName, body.currencyCode, body.institutionName ?? null]
  );
  const r = inserted.rows[0]!;
  return {
    financialAccountId: r.financial_account_id,
    accountType: r.account_type,
    accountName: r.account_name,
    currencyCode: r.currency_code,
    institutionName: r.institution_name,
    status: r.status,
  };
}
