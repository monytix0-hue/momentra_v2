/**
 * QA-only seed for a DISTRIBUTION_DEALERSHIP moment.
 *
 * Inserts a small master set + approved event chain so Pulse / Moments / Life / Memory
 * read as populated after setup activation. Never run against production.
 *
 * Usage:
 *   MOMENT_ID=<uuid> COMPANY_ID=<uuid> npx tsx scripts/qa/seed-distribution-operating.ts
 */
import { Pool } from 'pg';

async function main() {
  const momentId = process.env.MOMENT_ID;
  const companyId = process.env.COMPANY_ID;
  const userId = process.env.USER_ID ?? '11111111-1111-1111-1111-111111111111';
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
    const masters = [
      { kind: 'DEALER', name: 'Dealer B (Route N-2)', code: 'DLR-B' },
      { kind: 'PRINCIPAL', name: 'Principal A', code: 'PRN-A' },
      { kind: 'SKU', name: 'Carton Mixed FMCG', code: 'SKU-MIX' },
      { kind: 'ROUTE', name: 'Route R-7', code: 'R-7' },
      { kind: 'WAREHOUSE', name: 'Bhiwandi Depot', code: 'WH-BHI' },
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

    const chain: Array<{ type: string; title: string; amount: string; payment?: string }> = [
      { type: 'ORDER', title: 'Dealer B open order', amount: '45000' },
      { type: 'SHIPMENT', title: 'Dispatch 237-532 loaded: 40 cartons', amount: '45000' },
      { type: 'DELIVERY', title: 'Route R-7 delivered', amount: '45000' },
      { type: 'SALE', title: 'Dealer B invoice INV-2047', amount: '45000' },
      { type: 'COLLECTION', title: 'Collection ₹1.5L allocated', amount: '150000', payment: 'COLLECTED' },
      { type: 'STOCK', title: 'Bhiwandi on-hand count', amount: '0' },
      { type: 'PURCHASE', title: 'Principal A GRN-318', amount: '61000' },
      { type: 'ISSUE', title: 'GSTIN mismatch on Route R-7', amount: '0' },
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
    console.log(`Seeded distribution operating data for moment ${momentId}`);
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
