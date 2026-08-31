/**
 * Reset then seed Maestro QA fixtures.
 *
 *   npm run qa:prepare-fixtures
 */
import { spawnSync } from 'child_process';
import path from 'path';

const root = path.resolve(__dirname, '../..');
process.env.QA_FIXTURES_ENABLED = process.env.QA_FIXTURES_ENABLED || 'true';
process.env.ALLOW_DEV_AUTH = process.env.ALLOW_DEV_AUTH || '1';

function run(rel: string): void {
  const script = path.join(__dirname, rel);
  const r = spawnSync('npx', ['tsx', script], {
    cwd: root,
    env: process.env,
    stdio: 'inherit',
    shell: true,
  });
  if (r.status !== 0) process.exit(r.status ?? 1);
}

run('reset-maestro-fixtures.ts');
run('seed-maestro-fixtures.ts');
