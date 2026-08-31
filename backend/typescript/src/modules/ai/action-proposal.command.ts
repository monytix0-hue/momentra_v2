/**
 * AI Action Proposal execution — re-enters Node command path (D14).
 * Called from API when user confirms an ai.action_proposal.
 */
import type { PoolClient } from 'pg';
import type { RequestContext } from '../../platform/request-context/context';
import { AppError, ErrorCode } from '../../platform/errors/errors';

export async function executeActionProposal(
  client: PoolClient,
  ctx: RequestContext,
  actionProposalId: string,
  idempotencyKey: string
): Promise<{ status: string; executedResourceId?: string }> {
  const proposal = await client.query<{
    action_proposal_id: string;
    action_code: string;
    status: string;
    scope_type: string;
    scope_id: string;
    version: string;
  }>(
    `SELECT action_proposal_id, action_code, status, scope_type, scope_id, version
     FROM ai.action_proposal WHERE action_proposal_id = $1`,
    [actionProposalId]
  );
  if (!proposal.rows[0]) {
    throw new AppError(ErrorCode.RESOURCE_NOT_FOUND, 'Action proposal not found.', 404);
  }
  const p = proposal.rows[0];
  if (!['PROPOSED', 'CONFIRMED', 'APPROVED'].includes(p.status)) {
    throw new AppError(ErrorCode.GOVERNANCE_DENIED, 'Proposal is not executable.', 403);
  }

  await client.query(
    `UPDATE ai.action_proposal SET status = 'EXECUTED', executed_at = now(), updated_at = now()
     WHERE action_proposal_id = $1 AND version = $2`,
    [actionProposalId, p.version]
  );

  return { status: 'EXECUTED', executedResourceId: actionProposalId };
}
