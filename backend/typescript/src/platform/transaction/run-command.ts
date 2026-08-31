import type { PoolClient } from 'pg';
import type { RequestContext } from '../request-context/context';
import { withTransaction } from '../database/pool';
import { acquireIdempotency, completeIdempotency, failIdempotency, hashRequest } from '../idempotency/store';

export interface CommandOptions<TBody, TResult> {
  operationCode: string;
  idempotencyKey: string;
  body: TBody;
  ctx: RequestContext;
  resourceType: string;
  execute: (client: PoolClient, body: TBody) => Promise<{ result: TResult; resourceId: string }>;
}

/** Standard command transaction: idempotency → domain work → complete idempotency. */
export async function runCommand<TBody, TResult>(opts: CommandOptions<TBody, TResult>): Promise<TResult> {
  const requestHash = hashRequest(opts.body);

  return withTransaction(async (client) => {
    const idem = await acquireIdempotency<TResult>(
      client,
      opts.operationCode,
      opts.idempotencyKey,
      requestHash
    );
    if (idem.replay && idem.payload) {
      return idem.payload;
    }

    try {
      const { result, resourceId } = await opts.execute(client, opts.body);
      await completeIdempotency(client, opts.operationCode, opts.idempotencyKey, opts.resourceType, resourceId, result);
      return result;
    } catch (e) {
      try {
        await failIdempotency(client, opts.operationCode, opts.idempotencyKey);
      } catch {
        // Domain SQL already aborted this transaction; keep the original error.
      }
      throw e;
    }
  });
}
