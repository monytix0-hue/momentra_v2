import { Pool } from 'pg';
import dotenv from 'dotenv';
import path from 'path';

dotenv.config({ path: path.resolve(__dirname, '../../.env') });

async function main(): Promise<void> {
  const pool = new Pool({ connectionString: process.env.DATABASE_URL });
  try {
    await pool.query(
      `INSERT INTO core.user_profile (user_id, email, display_name, status)
       VALUES ($1, $2, $3, 'ACTIVE')
       ON CONFLICT (user_id) DO UPDATE SET email = EXCLUDED.email`,
      ['891ad9b7-4246-52ad-9d48-aacc991d4caa', 'dev@test.local', 'Dev']
    );
    console.log('profile ok');
  } catch (e) {
    console.error('profile fail', e);
  }
  await pool.end();
}

main();
