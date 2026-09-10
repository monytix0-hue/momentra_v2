import type { PoolClient } from 'pg';
import type { RequestContext } from '../../platform/request-context/context';
import { AppError, ErrorCode } from '../../platform/errors/errors';
import { trySignedDownloadUrl } from '../media/service';
import { assertGroupMember } from '../collaboration/group-membership';

export interface ContributionAttachmentDto {
  uploadId: string;
  contentType: string | null;
  status: string;
  downloadUrl: string | null;
  createdAt: string;
}

export async function listContributionAttachments(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  contributionId: string
): Promise<ContributionAttachmentDto[]> {
  await assertGroupMember(client, ctx, momentId);
  const owned = await client.query(
    `SELECT 1 FROM finance.contribution
     WHERE contribution_id = $1::uuid AND moment_id = $2::uuid
       AND status IN ('RECORDED', 'PENDING')`,
    [contributionId, momentId]
  );
  if (!owned.rowCount) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Contribution not found.', 404);
  }

  const rows = await client.query<{
    upload_id: string;
    content_type: string | null;
    status: string;
    bucket: string | null;
    object_key: string | null;
    created_at: Date;
  }>(
    `SELECT ca.upload_id, mu.content_type, mu.status, mu.bucket, mu.object_key, ca.created_at
     FROM finance.contribution_attachment ca
     JOIN platform.media_upload mu ON mu.media_upload_id = ca.upload_id
     WHERE ca.contribution_id = $1::uuid
     ORDER BY ca.created_at ASC`,
    [contributionId]
  );

  const items: ContributionAttachmentDto[] = [];
  for (const r of rows.rows) {
    const downloadUrl =
      r.status === 'COMPLETED' && r.bucket && r.object_key
        ? await trySignedDownloadUrl(r.bucket, r.object_key)
        : null;
    items.push({
      uploadId: r.upload_id,
      contentType: r.content_type,
      status: r.status,
      downloadUrl,
      createdAt: r.created_at.toISOString(),
    });
  }
  return items;
}
