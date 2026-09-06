/**
 * Materialize Phase 13 Founder Lean KPIs into analytics_mart.kpi_period
 * (undomain'd) and refresh Group Lean KPIs.
 *
 * Usage: npx tsx scripts/refresh-founder-lean-kpis.ts
 */
import path from 'path';
import dotenv from 'dotenv';
import { Pool } from 'pg';
import { refreshFounderLeanKpis } from '../src/modules/analytics/founder-lean-kpis';

dotenv.config({ path: path.resolve(__dirname, '../../.env') });

async function main(): Promise<void> {
  const dbUrl = process.env.DATABASE_URL_DIRECT ?? process.env.DATABASE_URL;
  if (!dbUrl) throw new Error('DATABASE_URL_DIRECT or DATABASE_URL required');
  const pool = new Pool({ connectionString: dbUrl });
  const client = await pool.connect();
  try {
    const result = await refreshFounderLeanKpis(client);
    console.log('Founder Lean KPIs refreshed:');
    for (const row of result.founder) {
      console.log(
        `  ${row.kpiCode}: value=${row.kpiValue} num=${row.numerator} den=${row.denominator} n=${row.sampleSize}`
      );
    }
    console.log(`Second-moment cohorts upserted: ${result.cohortsUpserted}`);
    console.log('Group Lean KPIs refreshed:');
    for (const row of result.group) {
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
