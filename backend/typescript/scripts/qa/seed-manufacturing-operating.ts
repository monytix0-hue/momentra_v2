/**
 * QA-only seed for STANDARD_PRODUCTION / MADE_TO_ORDER moments.
 *
 * Inserts a small master set + approved event chain so Pulse / Moments / Life / Memory
 * read as populated after setup activation. Never run against production.
 *
 * Usage:
 *   MOMENT_ID=<uuid> COMPANY_ID=<uuid> SUBTYPE=STANDARD_PRODUCTION \
 *     npx tsx scripts/qa/seed-manufacturing-operating.ts
 *   MOMENT_ID=<uuid> COMPANY_ID=<uuid> SUBTYPE=MADE_TO_ORDER \
 *     npx tsx scripts/qa/seed-manufacturing-operating.ts
 */
import { Pool } from 'pg';

async function main() {
  const momentId = process.env.MOMENT_ID;
  const companyId = process.env.COMPANY_ID;
  const userId = process.env.USER_ID ?? '11111111-1111-1111-1111-111111111111';
  const subtype = (process.env.SUBTYPE ?? 'STANDARD_PRODUCTION').toUpperCase();
  const mto = subtype === 'MADE_TO_ORDER';
  if (!momentId || !companyId) {
    throw new Error('MOMENT_ID and COMPANY_ID are required');
  }
  if (process.env.NODE_ENV === 'production') {
    throw new Error('Refusing to seed operating demo data in production');
  }

  const pool = new Pool({ connectionString: process.env.DATABASE_URL });
  const client = await pool.connect();
  try {
    await client.query('BEGIN');
    const masters = mto
      ? [
          { kind: 'CUSTOMER', name: 'Job customer North', code: 'CUS-N' },
          { kind: 'SUPPLIER', name: 'Alloy Mill', code: 'SUP-AL' },
          { kind: 'SKU', name: 'Custom bracket kit', code: 'SKU-JB' },
          { kind: 'WAREHOUSE', name: 'Workshop bay A', code: 'WH-BAY' },
        ]
      : [
          { kind: 'BUYER', name: 'Buyer OEM West', code: 'BYR-W' },
          { kind: 'SUPPLIER', name: 'Steel Mill', code: 'SUP-ST' },
          { kind: 'SKU', name: 'FG Carton A', code: 'SKU-FG' },
          { kind: 'WAREHOUSE', name: 'Plant store FG', code: 'WH-FG' },
          { kind: 'ROUTE', name: 'Line L-3', code: 'LN-3' },
        ];
    for (const master of masters) {
      await client.query(
        `INSERT INTO business.operating_master
           (company_id, kind, name, code, status, created_by_user_id)
         VALUES ($1, $2, $3, $4, 'ACTIVE', $5)
         ON CONFLICT DO NOTHING`,
        [companyId, master.kind, master.name, master.code, userId]
      ).catch(async () => {
        await client.query(
          `INSERT INTO business.operating_master
             (company_id, kind, name, status, created_by_user_id)
           SELECT $1, $2, $3, 'ACTIVE', $4
           WHERE NOT EXISTS (
             SELECT 1 FROM business.operating_master
             WHERE company_id = $1 AND kind = $2 AND lower(btrim(name)) = lower(btrim($3))
           )`,
          [companyId, master.kind, master.name, userId]
        );
      });
    }

    const chain: Array<{ type: string; title: string; amount: string; payment?: string }> = mto
      ? [
          { type: 'ORDER', title: 'Job JO-104 confirmed', amount: '82000' },
          { type: 'PRODUCTION', title: 'Bay A accepted qty against JO-104', amount: '82000' },
          { type: 'SHIPMENT', title: 'Dispatch JO-104 partial', amount: '41000' },
          { type: 'SALE', title: 'Invoice INV-JO-104', amount: '82000' },
          { type: 'COLLECTION', title: 'Collection against JO-104', amount: '40000', payment: 'COLLECTED' },
          { type: 'PURCHASE', title: 'Alloy Mill material GRN', amount: '22000' },
          { type: 'STOCK', title: 'Workshop WIP count', amount: '0' },
          { type: 'ISSUE', title: 'Hold on JO-104 finishing', amount: '0' },
        ]
      : [
          { type: 'ORDER', title: 'Buyer OEM West open order', amount: '125000' },
          { type: 'PRODUCTION', title: 'Line L-3 output batch B-19', amount: '125000' },
          { type: 'SHIPMENT', title: 'FG dispatch loaded: 60 cartons', amount: '125000' },
          { type: 'SALE', title: 'Buyer OEM invoice INV-3188', amount: '125000' },
          { type: 'COLLECTION', title: 'Collection ₹80k allocated', amount: '80000', payment: 'COLLECTED' },
          { type: 'PURCHASE', title: 'Steel Mill GRN-441', amount: '54000' },
          { type: 'STOCK', title: 'Plant FG on-hand count', amount: '0' },
          { type: 'ISSUE', title: 'Scrap spike on Line L-3', amount: '0' },
        ];

    for (const event of chain) {
      await client.query(
        `INSERT INTO business.operating_event (
           moment_id, event_type, title, amount, currency_code,
           payment_relevance, payment_status, approval_status,
           submitted_by_user_id, occurred_at, payload
         ) VALUES (
           $1, $2, $3, $4::numeric, 'INR',
           'NONE', $5, 'APPROVED',
           $6, now(), '{}'::jsonb
         )`,
        [momentId, event.type, event.title, event.amount, event.payment ?? 'NONE', userId]
      );
    }

    await client.query('COMMIT');
    console.log(`Seeded manufacturing (${subtype}) operating data for moment ${momentId}`);
  } catch (error) {
    await client.query('ROLLBACK');
    throw error;
  } finally {
    client.release();
    await pool.end();
  }
}

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
