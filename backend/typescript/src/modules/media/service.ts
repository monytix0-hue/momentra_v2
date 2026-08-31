import { randomUUID } from 'crypto';
import type { PoolClient } from 'pg';
import { createClient, type SupabaseClient } from '@supabase/supabase-js';
import type { RequestContext } from '../../platform/request-context/context';
import { AppError, ErrorCode } from '../../platform/errors/errors';
import { z } from 'zod';

const ALLOWED_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp', 'video/mp4', 'application/pdf']);
const MAX_BYTES = 50 * 1024 * 1024;

export const uploadIntentSchema = z
  .object({
    contentType: z.string().min(1),
    byteSize: z.number().int().positive(),
    scopeType: z.enum(['USER', 'MOMENT', 'COMPANY']),
    scopeId: z.string().uuid(),
    checksumSha256: z.string().length(64).optional(),
  })
  .strict();

export const uploadCompleteSchema = z
  .object({
    storageKey: z.string().min(1),
    checksumSha256: z.string().length(64).optional(),
  })
  .strict();

let supabaseAdmin: SupabaseClient | null = null;

function getSupabaseAdmin(): SupabaseClient {
  if (supabaseAdmin) return supabaseAdmin;
  const url = process.env.SUPABASE_URL?.trim();
  const key = process.env.SUPABASE_SECRET_KEY?.trim();
  if (!url || !key) {
    throw new AppError(
      ErrorCode.INFRASTRUCTURE_UNAVAILABLE,
      'Supabase Storage is not configured (SUPABASE_URL / SUPABASE_SECRET_KEY).',
      500
    );
  }
  supabaseAdmin = createClient(url, key, {
    auth: { persistSession: false, autoRefreshToken: false },
  });
  return supabaseAdmin;
}

function mediaBucket(): string {
  return process.env.MEDIA_BUCKET?.trim() || 'momentra-media';
}

export async function createUploadIntent(
  client: PoolClient,
  ctx: RequestContext,
  body: z.infer<typeof uploadIntentSchema>
): Promise<{
  uploadId: string;
  signedUrl: string;
  storageKey: string;
  expiresAt: string;
  ownerUserId: string;
}> {
  if (!ALLOWED_TYPES.has(body.contentType)) {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Content type not allowed.', 400);
  }
  if (body.byteSize > MAX_BYTES) {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'File exceeds maximum size.', 400);
  }

  const uploadId = randomUUID();
  const bucket = mediaBucket();
  const objectKey = `${body.scopeType.toLowerCase()}/${body.scopeId}/${uploadId}`;
  const expiresAt = new Date(Date.now() + 15 * 60 * 1000).toISOString();

  const supabase = getSupabaseAdmin();
  // Ensure bucket exists (idempotent). Create as private for signed uploads.
  const { data: buckets } = await supabase.storage.listBuckets();
  const bucketNames = new Set((buckets ?? []).map((b) => b.name));
  if (!bucketNames.has(bucket)) {
    const { error: createErr } = await supabase.storage.createBucket(bucket, {
      public: false,
      fileSizeLimit: MAX_BYTES,
      allowedMimeTypes: [...ALLOWED_TYPES],
    });
    if (createErr && !/already exists|duplicate/i.test(createErr.message)) {
      throw new AppError(
        ErrorCode.INFRASTRUCTURE_UNAVAILABLE,
        `Failed to create Supabase media bucket "${bucket}": ${createErr.message}`,
        500
      );
    }
  }

  const { data: signed, error } = await supabase.storage.from(bucket).createSignedUploadUrl(objectKey);
  if (error || !signed?.signedUrl) {
    throw new AppError(
      ErrorCode.INFRASTRUCTURE_UNAVAILABLE,
      `Failed to create Supabase signed upload URL: ${error?.message ?? 'unknown'}`,
      500
    );
  }

  await client.query(
    `INSERT INTO platform.media_upload (
       media_upload_id, user_id, scope_type, scope_id, content_type,
       bucket, object_key, size_bytes, checksum_sha256, status
     ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, 'PENDING')`,
    [
      uploadId,
      ctx.userId,
      body.scopeType,
      body.scopeId,
      body.contentType,
      bucket,
      objectKey,
      body.byteSize,
      body.checksumSha256 ?? null,
    ]
  );

  return {
    uploadId,
    signedUrl: signed.signedUrl,
    storageKey: objectKey,
    expiresAt,
    ownerUserId: ctx.userId,
  };
}

export async function completeUpload(
  client: PoolClient,
  ctx: RequestContext,
  uploadId: string,
  body: z.infer<typeof uploadCompleteSchema>
): Promise<{ uploadId: string; mediaId: string; status: string }> {
  const pending = await client.query<{ object_key: string }>(
    `SELECT object_key FROM platform.media_upload
     WHERE media_upload_id = $1 AND user_id = $2 AND status = 'PENDING'`,
    [uploadId, ctx.userId]
  );
  if (!pending.rows[0]) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Upload intent not found or already completed.', 404);
  }

  const storageKey = body.storageKey.trim();
  if (storageKey !== pending.rows[0].object_key) {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'storageKey does not match upload intent.', 400);
  }

  const updated = await client.query<{ media_upload_id: string }>(
    `UPDATE platform.media_upload SET
       checksum_sha256 = COALESCE($3, checksum_sha256),
       status = 'COMPLETED',
       completed_at = now()
     WHERE media_upload_id = $1 AND user_id = $2 AND status = 'PENDING'
     RETURNING media_upload_id`,
    [uploadId, ctx.userId, body.checksumSha256 ?? null]
  );
  if (!updated.rows[0]) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Upload intent not found or already completed.', 404);
  }
  return { uploadId, mediaId: uploadId, status: 'READY' };
}
