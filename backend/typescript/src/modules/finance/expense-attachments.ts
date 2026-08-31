import type { PoolClient } from 'pg';
import type { RequestContext } from '../../platform/request-context/context';
import { AppError, ErrorCode } from '../../platform/errors/errors';
import { z } from 'zod';

export const attachExpenseMediaSchema = z.object({ uploadId: z.string().uuid() }).strict();

export interface ExpenseAttachmentDto {
  uploadId: string;
  contentType: string | null;
  status: string;
  createdAt: string;
}

export async function listExpenseAttachments(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  expenseId: string
): Promise<ExpenseAttachmentDto[]> {
  await assertExpenseAccess(client, ctx, momentId, expenseId);
  const rows = await client.query<{
    resource_id: string;
    content_type: string | null;
    status: string;
    created_at: Date;
  }>(
    `SELECT erl.resource_id, mu.content_type, mu.status, erl.created_at
     FROM finance.expense_resource_link erl
     JOIN platform.media_upload mu ON mu.media_upload_id = erl.resource_id
     WHERE erl.expense_id = $1 AND erl.resource_type = 'MEDIA'
     ORDER BY erl.created_at ASC`,
    [expenseId]
  );
  return rows.rows.map((r) => ({
    uploadId: r.resource_id,
    contentType: r.content_type,
    status: r.status,
    createdAt: r.created_at.toISOString(),
  }));
}

export async function attachExpenseMedia(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  expenseId: string,
  uploadId: string
): Promise<ExpenseAttachmentDto> {
  await assertExpenseAccess(client, ctx, momentId, expenseId);
  const upload = await client.query<{ media_upload_id: string; content_type: string; status: string; user_id: string }>(
    `SELECT media_upload_id, content_type, status, user_id FROM platform.media_upload
     WHERE media_upload_id = $1`,
    [uploadId]
  );
  if (!upload.rows[0] || upload.rows[0].user_id !== ctx.userId) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Upload not found.', 404);
  }
  if (upload.rows[0].status !== 'COMPLETED') {
    throw new AppError(ErrorCode.VALIDATION_FAILED, 'Upload must be completed before attaching.', 400);
  }
  await client.query(
    `INSERT INTO finance.expense_resource_link (expense_id, resource_type, resource_id, relation_type)
     VALUES ($1, 'MEDIA', $2, 'RELATED')
     ON CONFLICT (expense_id, resource_type, resource_id, relation_type) DO NOTHING`,
    [expenseId, uploadId]
  );
  return {
    uploadId,
    contentType: upload.rows[0].content_type,
    status: upload.rows[0].status,
    createdAt: new Date().toISOString(),
  };
}

export async function detachExpenseMedia(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  expenseId: string,
  uploadId: string
): Promise<void> {
  await assertExpenseAccess(client, ctx, momentId, expenseId);
  await client.query(
    `DELETE FROM finance.expense_resource_link
     WHERE expense_id = $1 AND resource_type = 'MEDIA' AND resource_id = $2`,
    [expenseId, uploadId]
  );
}

async function assertExpenseAccess(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string,
  expenseId: string
): Promise<void> {
  const row = await client.query(
    `SELECT 1 FROM finance.expense e
     JOIN finance.personal_expense_context pec ON pec.expense_id = e.expense_id
     WHERE e.expense_id = $1 AND e.moment_id = $2 AND pec.user_id = $3 AND e.status = 'POSTED'`,
    [expenseId, momentId, ctx.userId]
  );
  if (!row.rowCount) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Expense not found.', 404);
  }
}
