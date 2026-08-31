import { Pool } from 'pg';
import dotenv from 'dotenv';
import path from 'path';

dotenv.config({ path: path.resolve(__dirname, '../../.env') });

async function main(): Promise<void> {
  const pool = new Pool({ connectionString: process.env.DATABASE_URL });
  const schemas = await pool.query<{ schema_name: string }>(
    `SELECT schema_name FROM information_schema.schemata WHERE schema_name IN ('audit','platform','governance','analytics','memory','ai') ORDER BY 1`
  );
  console.log('schemas:', schemas.rows.map((r) => r.schema_name).join(', '));
  const tables = await pool.query<{ table_schema: string; table_name: string }>(
    `SELECT table_schema, table_name FROM information_schema.tables WHERE table_schema='platform' ORDER BY table_name`
  );
  for (const t of tables.rows) console.log(`${t.table_schema}.${t.table_name}`);
  await pool.end();
}

main();
