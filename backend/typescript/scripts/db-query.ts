import { Pool } from 'pg';
import dotenv from 'dotenv';
import path from 'path';

dotenv.config({ path: path.resolve(__dirname, '../../.env') });

const pool = new Pool({
  connectionString: process.env.DATABASE_URL_DIRECT ?? process.env.DATABASE_URL,
});

async function main(): Promise<void> {
  const queries: [string, string][] = [
    ['missing group types', `
      SELECT e.code FROM (VALUES
        ('TRIP'),('WEDDING'),('HOUSE_PARTY'),('OFFICE_OUTING'),
        ('GIFT_POOL'),('GROUP_PURCHASE'),('SHARED_ASSET'),('COMMUNITY_PURCHASE'),
        ('FLATMATES'),('CO_LIVING'),('SHARED_LIVING'),('COMMUNITY_LIVING')
      ) AS e(code)
      LEFT JOIN core.moment_type mt ON mt.domain_code='GROUP' AND mt.code=e.code
      WHERE mt.moment_type_id IS NULL
    `],
    ['metrics without active version', `
      SELECT md.code FROM analytics.metric_definition md
      WHERE md.status='ACTIVE'
        AND NOT EXISTS (
          SELECT 1 FROM analytics.metric_version mv
          WHERE mv.metric_definition_id=md.metric_definition_id AND mv.status='ACTIVE'
        )
    `],
    ['policies without active version', `
      SELECT p.code FROM governance.policy p
      WHERE p.status='ACTIVE'
        AND NOT EXISTS (
          SELECT 1 FROM governance.policy_version pv
          WHERE pv.policy_id=p.policy_id AND pv.status='ACTIVE'
        )
    `],
    ['group moment types', `
      SELECT code, status FROM core.moment_type WHERE domain_code='GROUP' ORDER BY code
    `],
    ['V030 in ledger', `
      SELECT migration_file FROM public.momentra_migration_ledger
      WHERE migration_file LIKE 'V030%' OR migration_file LIKE 'V034%'
    `],
    ['postgres version', 'SELECT version()'],
  ];

  for (const [label, sql] of queries) {
    const r = await pool.query(sql);
    console.log(`\n--- ${label} (${r.rowCount}) ---`);
    console.log(JSON.stringify(r.rows, null, 2));
  }
  await pool.end();
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
