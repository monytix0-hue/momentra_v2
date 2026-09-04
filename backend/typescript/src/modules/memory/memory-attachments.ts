import type { PoolClient } from 'pg';
import { z } from 'zod';
import type { RequestContext } from '../../platform/request-context/context';
import { AppError, ErrorCode } from '../../platform/errors/errors';
import { assertGroupMember } from '../collaboration/group-membership';
import { trySignedDownloadUrl } from '../media/service';

export const attachMemoryMediaSchema = z.object({ uploadId: z.string().uuid() }).strict();

export interface MemoryAttachmentDto {
  uploadId: string;
  contentType: string | null;
  status: string;
  createdAt: string;
  downloadUrl: string | null;
}

const MEDIA_PER_MEMORY_CAP = 3;

export async function listMemoryAttachments(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  memoryId: string
): Promise<MemoryAttachmentDto[]> {
  await assertMemoryAccess(client, ctx, momentId, memoryId);
  const rows = await client.query<{
    source_id: string;
    content_type: string | null;
    status: string;
    created_at: Date;
    bucket: string | null;
    object_key: string | null;
  }>(
    `SELECT me.source_id, mu.content_type, mu.status, me.created_at, mu.bucket, mu.object_key
     FROM memory.memory_evidence me
     JOIN platform.media_upload mu ON mu.media_upload_id = me.source_id
     WHERE me.memory_id = $1 AND me.source_type = 'MEDIA'
     ORDER BY me.created_at ASC`,
    [memoryId]
  );
  return Promise.all(
    rows.rows.map(async (r) => ({
      uploadId: r.source_id,
      contentType: r.content_type,
      status: r.status,
      createdAt: r.created_at.toISOString(),
      downloadUrl:
        r.status === 'COMPLETED' ? await trySignedDownloadUrl(r.bucket, r.object_key) : null,
    }))
  );
}

/** Batch-load media (capped) for many memories — used by list/facet payloads. */
export async function listMediaForMemories(
  client: PoolClient,
  memoryIds: string[],
  perMemoryCap = MEDIA_PER_MEMORY_CAP
): Promise<Map<string, MemoryAttachmentDto[]>> {
  const result = new Map<string, MemoryAttachmentDto[]>();
  if (memoryIds.length === 0) return result;

  const rows = await client.query<{
    memory_id: string;
    source_id: string;
    content_type: string | null;
    status: string;
    created_at: Date;
    bucket: string | null;
    object_key: string | null;
    rn: string;
  }>(
    `SELECT * FROM (
       SELECT me.memory_id, me.source_id, mu.content_type, mu.status, me.created_at,
              mu.bucket, mu.object_key,
              ROW_NUMBER() OVER (PARTITION BY me.memory_id ORDER BY me.created_at ASC) AS rn
       FROM memory.memory_evidence me
       JOIN platform.media_upload mu ON mu.media_upload_id = me.source_id
       WHERE me.memory_id = ANY($1::uuid[]) AND me.source_type = 'MEDIA'
     ) t
     WHERE rn <= $2
     ORDER BY memory_id, rn`,
    [memoryIds, perMemoryCap]
  );

  for (const r of rows.rows) {
    const list = result.get(r.memory_id) ?? [];
    list.push({
      uploadId: r.source_id,
      contentType: r.content_type,
      status: r.status,
      createdAt: r.created_at.toISOString(),
      downloadUrl: null, // filled below
    });
    result.set(r.memory_id, list);
  }

  // Sign URLs in parallel for COMPLETED uploads.
  const signJobs: Array<Promise<void>> = [];
  for (const r of rows.rows) {
    if (r.status !== 'COMPLETED') continue;
    const list = result.get(r.memory_id);
    const item = list?.find((m) => m.uploadId === r.source_id);
    if (!item) continue;
    signJobs.push(
      trySignedDownloadUrl(r.bucket, r.object_key).then((url) => {
        item.downloadUrl = url;
      })
    );
  }
  await Promise.all(signJobs);
  return result;
}

export async function attachMemoryMedia(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  memoryId: string,
  uploadId: string
): Promise<MemoryAttachmentDto> {
  await assertMemoryAccess(client, ctx, momentId, memoryId);

  const upload = await client.query<{
    media_upload_id: string;
    content_type: string;
    status: string;
    user_id: string;
    scope_type: string;
    scope_id: string;
    bucket: string | null;
    object_key: string | null;
  }>(
    `SELECT media_upload_id, content_type, status, user_id, scope_type, scope_id, bucket, object_key
     FROM platform.media_upload
     WHERE media_upload_id = $1`,
    [uploadId]
  );
  if (!upload.rows[0] || upload.rows[0].user_id !== ctx.userId) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Upload not found.', 404);
  }
  if (upload.rows[0].status !== 'COMPLETED') {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Upload must be completed before attaching.', 400);
  }
  if (upload.rows[0].scope_type !== 'MOMENT' || upload.rows[0].scope_id !== momentId) {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Upload scope does not match this moment.', 400);
  }

  await client.query(
    `INSERT INTO memory.memory_evidence (
       memory_id, source_type, source_id, evidence_role, observed_at
     ) VALUES ($1, 'MEDIA', $2, 'PRIMARY', now())
     ON CONFLICT (memory_id, source_type, source_id, evidence_role) DO NOTHING`,
    [memoryId, uploadId]
  );

  const downloadUrl = await trySignedDownloadUrl(upload.rows[0].bucket, upload.rows[0].object_key);

  return {
    uploadId,
    contentType: upload.rows[0].content_type,
    status: upload.rows[0].status,
    createdAt: new Date().toISOString(),
    downloadUrl,
  };
}

async function assertMemoryAccess(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  memoryId: string
): Promise<void> {
  await assertGroupMember(client, ctx, momentId);
  const row = await client.query(
    `SELECT 1 FROM memory.memory
     WHERE memory_id = $1 AND moment_id = $2 AND status = 'ACTIVE'`,
    [memoryId, momentId]
  );
  if (!row.rowCount) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Memory not found.', 404);
  }
}
