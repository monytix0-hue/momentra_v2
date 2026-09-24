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

/**
 * Production refuses a login that ignores RLS. Dev logs a warning and continues.
 * A failed role check in production also refuses boot.
 */
export async function assertDatabaseRoleSafe(): Promise<void> {
  let client: PoolClient | null = null;
  try {
    client = await getPool().connect();
    const result = await client.query<{ bypass: boolean }>(
      `SELECT (rolsuper OR rolbypassrls) AS bypass
       FROM pg_roles
       WHERE rolname = current_user`
    );
    if (result.rows[0]?.bypass) {
      const detail = 'Database role bypasses row-level security (superuser or BYPASSRLS).';
      if (config.isProduction) {
        throw new Error(`Production fail-closed: ${detail}`);
      }
      console.log(JSON.stringify({ level: 'warn', msg: 'db_role_bypasses_rls', detail }));
    }
  } catch (e) {
    if (config.isProduction) throw e;
    console.log(JSON.stringify({ level: 'warn', msg: 'db_role_check_skipped', err: String(e) }));
  } finally {
    client?.release();
  }
}

function releaseClient(client: PoolClient, destroy: boolean): void {
  client.release(destroy ? new Error('discard pooled connection') : undefined);
}

async function clearRequestUser(client: PoolClient): Promise<void> {
  await client.query(`SELECT set_config('request.jwt.claim.sub', '', false)`);
}

/** Session-scoped caller id for non-transaction checkouts. Cleared before the client returns to the pool. */
export async function withUserConnection<T>(
  userId: string,
  fn: (client: PoolClient) => Promise<T>
): Promise<T> {
  const client = await getPool().connect();
  let released = false;
  const release = (destroy: boolean) => {
    if (released) return;
    released = true;
    releaseClient(client, destroy);
  };
  try {
    await client.query(`SELECT set_config('request.jwt.claim.sub', $1, false)`, [userId]);
    return await fn(client);
  } catch (e) {
    try {
      await client.query('ROLLBACK');
    } catch {
      // No open transaction, or the connection is already dead.
    }
    throw e;
  } finally {
    try {
      await clearRequestUser(client);
      release(false);
    } catch {
      try {
        await client.query('ROLLBACK');
        await clearRequestUser(client);
        release(false);
      } catch {
        release(true);
      }
    }
  }
}

export async function withTransaction<T>(
  fn: (client: PoolClient) => Promise<T>,
  userId?: string
): Promise<T> {
  const client = await getPool().connect();
  try {
    await client.query('BEGIN');
    if (userId) {
      await client.query(`SELECT set_config('request.jwt.claim.sub', $1, true)`, [userId]);
    }
    const result = await fn(client);
    await client.query('COMMIT');
    return result;
  } catch (e) {
    try {
      await client.query('ROLLBACK');
    } catch {
      // Connection already aborted.
    }
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
