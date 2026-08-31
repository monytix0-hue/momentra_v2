import { createHash } from 'crypto';
import type { PoolClient } from 'pg';
import { AppError, ErrorCode } from '../errors/errors';

export interface IdempotencyResult<T> {
  replay: boolean;
  payload?: T;
  resourceId?: string;
}

export function hashRequest(body: unknown): string {
  return createHash('sha256').update(JSON.stringify(body)).digest('hex');
}

/**
 * Acquire idempotency lock within the current transaction.
 * Uses advisory lock + row lock so concurrent same-key requests serialize
 * across backend replicas sharing PostgreSQL.
 *
 * S9-H-OPT: common path (first use of key) is one RTT — lock + insert + return.
 */
export async function acquireIdempotency<T>(
  client: PoolClient,
  operationCode: string,
  idempotencyKey: string,
  requestHash: string
): Promise<IdempotencyResult<T>> {
  const lockKey = hashRequest(`${operationCode}:${idempotencyKey}`);
  const k1 = parseInt(lockKey.slice(0, 8), 16) & 0x7fffffff;
  const k2 = parseInt(lockKey.slice(8, 16), 16) & 0x7fffffff;

  const row = await client.query<{
    status: string;
    request_hash: string;
    response_payload: T | null;
    resource_id: string | null;
    inserted: boolean;
  }>(
    `WITH lock AS (
       SELECT pg_advisory_xact_lock($1::int, $2::int)
     ),
     ins AS (
       INSERT INTO platform.idempotency_record (operation_code, idempotency_key, request_hash, status)
       SELECT $3, $4, $5, 'PROCESSING'
       WHERE NOT EXISTS (
         SELECT 1 FROM platform.idempotency_record
         WHERE operation_code = $3 AND idempotency_key = $4
       )
       RETURNING status, request_hash, response_payload, resource_id
     ),
     sel AS (
       SELECT status, request_hash, response_payload, resource_id, false AS inserted
       FROM platform.idempotency_record
       WHERE operation_code = $3 AND idempotency_key = $4
       FOR UPDATE
     )
     SELECT status, request_hash, response_payload, resource_id, true AS inserted FROM ins
     UNION ALL
     SELECT status, request_hash, response_payload, resource_id, inserted FROM sel
     WHERE NOT EXISTS (SELECT 1 FROM ins)
     LIMIT 1`,
    [k1, k2, operationCode, idempotencyKey, requestHash]
  );

  const current = row.rows[0];
  if (!current) {
    throw new AppError(ErrorCode.INFRASTRUCTURE_UNAVAILABLE, 'Idempotency acquire failed.', 500);
  }

  if (current.inserted) {
    return { replay: false };
  }

  if (current.request_hash !== requestHash) {
    throw new AppError(
      ErrorCode.IDEMPOTENCY_CONFLICT,
      'Idempotency key reused with a different request body.',
      409
    );
  }
  if (current.status === 'SUCCEEDED' && current.response_payload) {
    return {
      replay: true,
      payload: current.response_payload,
      resourceId: current.resource_id ?? undefined,
    };
  }
  if (current.status === 'PROCESSING') {
    throw new AppError(ErrorCode.INFRASTRUCTURE_UNAVAILABLE, 'Operation in progress.', 409);
  }
  if (current.status === 'FAILED') {
    await client.query(
      `UPDATE platform.idempotency_record
       SET status = 'PROCESSING', request_hash = $3, completed_at = NULL, updated_at = now(),
           response_payload = NULL, resource_id = NULL, resource_type = NULL
       WHERE operation_code = $1 AND idempotency_key = $2`,
      [operationCode, idempotencyKey, requestHash]
    );
    return { replay: false };
  }

  await client.query(
    `UPDATE platform.idempotency_record
     SET status = 'PROCESSING', request_hash = $3, completed_at = NULL, updated_at = now()
     WHERE operation_code = $1 AND idempotency_key = $2`,
    [operationCode, idempotencyKey, requestHash]
  );
  return { replay: false };
}

export async function completeIdempotency<T>(
  client: PoolClient,
  operationCode: string,
  idempotencyKey: string,
  resourceType: string,
  resourceId: string,
  responsePayload: T
): Promise<void> {
  await client.query(
    `UPDATE platform.idempotency_record
     SET status = 'SUCCEEDED', completed_at = now(), resource_type = $3, resource_id = $4,
         response_payload = $5::jsonb, updated_at = now()
     WHERE operation_code = $1 AND idempotency_key = $2`,
    [operationCode, idempotencyKey, resourceType, resourceId, JSON.stringify(responsePayload)]
  );
}

export async function failIdempotency(
  client: PoolClient,
  operationCode: string,
  idempotencyKey: string
): Promise<void> {
  await client.query(
    `UPDATE platform.idempotency_record
     SET status = 'FAILED', completed_at = now(), updated_at = now()
     WHERE operation_code = $1 AND idempotency_key = $2`,
    [operationCode, idempotencyKey]
  );
}
