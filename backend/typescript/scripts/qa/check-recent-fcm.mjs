import fs from 'fs';
import pg from 'pg';

function load(p) {
  const o = {};
  for (const l of fs.readFileSync(p, 'utf8').split(/\r?\n/)) {
    if (!l || l.startsWith('#') || !l.includes('=')) continue;
    const i = l.indexOf('=');
    let v = l.slice(i + 1).trim();
    if (
      (v.startsWith('"') && v.endsWith('"')) ||
      (v.startsWith("'") && v.endsWith("'"))
    ) {
      v = v.slice(1, -1);
    }
    o[l.slice(0, i).trim()] = v;
  }
  return o;
}

const env = load('g:/momentra_v2/backend/.env');
const c = new pg.Client({
  connectionString: env.DATABASE_URL,
  ssl: { rejectUnauthorized: false },
});
await c.connect();
const recent = await c.query(`
  select sent_count, failure_reason, event_name, sent_at
  from platform.notification_dispatch
  where sent_at > now() - interval '2 hours'
  order by sent_at desc
  limit 15
`);
const totals = await c.query(`
  select
    count(*) filter (where sent_count > 0)::int as successes_all_time,
    count(*) filter (where sent_at > now() - interval '30 minutes')::int as events_30m,
    count(*) filter (where sent_count > 0 and sent_at > now() - interval '30 minutes')::int as success_30m
  from platform.notification_dispatch
`);
console.log(JSON.stringify({ totals: totals.rows[0], recent: recent.rows }, null, 2));
await c.end();
