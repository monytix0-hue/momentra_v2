import dotenv from 'dotenv';
import path from 'path';
import { Pool } from 'pg';
import { resolveDevIdentity, provisionUserProfile } from '../src/platform/auth';

dotenv.config({ path: path.resolve(__dirname, '../../.env') });
dotenv.config({ path: path.resolve(__dirname, '../../.env'), override: true });

async function main(): Promise<void> {
  console.log('ALLOW_DEV_AUTH', process.env.ALLOW_DEV_AUTH);
  console.log('FIREBASE_PROJECT_ID', process.env.FIREBASE_PROJECT_ID);
  console.log('DATABASE_URL', process.env.DATABASE_URL?.slice(0, 40));
  const id = resolveDevIdentity('dev-smoke-user');
  console.log('identity', id);
  try {
    await provisionUserProfile(id.userId, 'dev@test.local', 'Dev');
    console.log('provision ok');
  } catch (e) {
    console.error('provision fail', e);
  }
  const pool = new Pool({ connectionString: process.env.DATABASE_URL });
  const r = await pool.query('SELECT 1');
  console.log('db', r.rows[0]);
  await pool.end();
}

main();
