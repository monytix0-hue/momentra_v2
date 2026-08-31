import { Pool, PoolClient, QueryResultRow } from 'pg';
import { config } from '../config';
import { AppError, ErrorCode } from '../errors/errors';

let pool: Pool | null = null;

export function getPool(): Pool {
  if (!pool) {
    pool = new Pool({
      connectionString: config.database.url,
      max: config.database.poolMax,
      idleTimeoutMillis: config.database.poolIdleMs,
      connectionTimeoutMillis: config.database.connectionTimeoutMs,
      statement_timeout: config.database.statementTimeoutMs,
      allowExitOnIdle: false,
    });
    pool.on('error', (err) => {
      console.log(
        JSON.stringify({
          level: 'error',
          msg: 'pg_pool_error',
          err: String(err),
        })
      );
    });
  }
  return pool;
}

export function getPoolStats(): {
  totalCount: number;
  idleCount: number;
  waitingCount: number;
  max: number;
  min: number;
} {
  const p = getPool();
  return {
    totalCount: p.totalCount,
    idleCount: p.idleCount,
    waitingCount: p.waitingCount,
    max: config.database.poolMax,
    min: config.database.poolMin,
  };
}

/**
 * Establish up to `count` idle connections (TLS/pooler handshake paid at startup, not first request).
 * Caps at poolMax. Safe default is config.database.poolMin (2).
 */
export async function prewarmPool(count = config.database.poolMin): Promise<{
  warmed: number;
  durationMs: number;
}> {
  const n = Math.max(0, Math.min(count, config.database.poolMax));
  if (n === 0) return { warmed: 0, durationMs: 0 };
  const p = getPool();
  const t0 = Date.now();
  const clients: PoolClient[] = [];
  try {
    for (let i = 0; i < n; i++) {
      const client = await p.connect();
      await client.query('SELECT 1');
      clients.push(client);
    }
  } finally {
    for (const c of clients) c.release();
  }
  const durationMs = Date.now() - t0;
  console.log(
    JSON.stringify({
      level: 'info',
      msg: 'pg_pool_prewarm',
      warmed: clients.length,
      durationMs,
      ...getPoolStats(),
    })
  );
  return { warmed: clients.length, durationMs };
}

export async function checkDatabaseReady(): Promise<boolean> {
  try {
    const result = await getPool().query('SELECT 1 AS ok');
    return result.rows[0]?.ok === 1;
  } catch {
    return false;
  }
}

export async function withTransaction<T>(fn: (client: PoolClient) => Promise<T>): Promise<T> {
  const client = await getPool().connect();
  try {
    await client.query('BEGIN');
    const result = await fn(client);
    await client.query('COMMIT');
    return result;
  } catch (e) {
    await client.query('ROLLBACK');
    throw e;
  } finally {
    client.release();
  }
}

export async function queryOne<T extends QueryResultRow>(
  client: PoolClient,
  sql: string,
  params?: unknown[]
): Promise<T | null> {
  const r = await client.query<T>(sql, params);
  return r.rows[0] ?? null;
}

export async function assertRows(
  result: { rowCount: number | null },
  code: ErrorCode,
  message: string,
  httpStatus = 404
): Promise<void> {
  if ((result.rowCount ?? 0) === 0) {
    throw new AppError(code, message, httpStatus);
  }
}

export async function closePool(): Promise<void> {
  if (pool) {
    await pool.end();
    pool = null;
  }
}
