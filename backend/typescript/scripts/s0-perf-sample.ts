/**
 * Quick p50/p95 sampler for GET /v1/me and POST /v1/me/devices.
 * Usage: npx tsx scripts/s0-perf-sample.ts
 */
import { randomUUID } from 'crypto';
import request from 'supertest';
import { createApp } from '../src/app';
import { closePool } from '../src/platform/database/pool';

process.env.ALLOW_DEV_AUTH = '1';

function percentile(sorted: number[], p: number): number {
  if (!sorted.length) return 0;
  const idx = Math.min(sorted.length - 1, Math.ceil((p / 100) * sorted.length) - 1);
  return sorted[idx]!;
}

async function sample(label: string, n: number, fn: () => Promise<number>): Promise<void> {
  const times: number[] = [];
  for (let i = 0; i < n; i++) {
    times.push(await fn());
  }
  times.sort((a, b) => a - b);
  console.log(
    JSON.stringify({
      label,
      n,
      p50: percentile(times, 50),
      p95: percentile(times, 95),
      max: times[times.length - 1],
      unit: 'ms',
    })
  );
}

async function main(): Promise<void> {
  const app = createApp();
  const uid = `perf-${randomUUID().slice(0, 8)}`;

  await sample('GET /v1/me', 20, async () => {
    const t0 = Date.now();
    const res = await request(app).get('/v1/me').set('X-Dev-Firebase-Uid', uid);
    if (res.status !== 200) throw new Error(`me ${res.status}`);
    return Date.now() - t0;
  });

  await sample('POST /v1/me/devices', 10, async () => {
    const t0 = Date.now();
    const res = await request(app)
      .post('/v1/me/devices')
      .set('X-Dev-Firebase-Uid', uid)
      .set('Idempotency-Key', `perf-${randomUUID()}`)
      .send({ platform: 'ANDROID', pushToken: `tok-${randomUUID()}` });
    if (res.status !== 201) throw new Error(`device ${res.status}`);
    return Date.now() - t0;
  });

  await closePool();
}

main().catch(async (e) => {
  console.error(e);
  await closePool();
  process.exit(1);
});
