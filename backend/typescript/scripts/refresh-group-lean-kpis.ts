/**
 * Materialize Phase 13 Group Lean KPIs (20, 30–35) into analytics_mart.kpi_period.
 *
 * Usage: npx tsx scripts/refresh-group-lean-kpis.ts
 */
import path from 'path';
import dotenv from 'dotenv';
import { Pool } from 'pg';
import { refreshGroupLeanKpis } from '../src/modules/analytics/group-lean-kpis';

dotenv.config({ path: path.resolve(__dirname, '../../.env') });

async function main(): Promise<void> {
  const dbUrl = process.env.DATABASE_URL_DIRECT ?? process.env.DATABASE_URL;
  if (!dbUrl) throw new Error('DATABASE_URL_DIRECT or DATABASE_URL required');
  const pool = new Pool({ connectionString: dbUrl });
  const client = await pool.connect();
  try {
    const rows = await refreshGroupLeanKpis(client);
    console.log('Group Lean KPIs refreshed:');
    for (const row of rows) {
      console.log(
        `  ${row.kpiCode}: value=${row.kpiValue} num=${row.numerator} den=${row.denominator} n=${row.sampleSize}`
      );
    }
  } finally {
    client.release();
    await pool.end();
  }
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
