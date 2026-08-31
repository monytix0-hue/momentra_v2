import type { PoolClient } from 'pg';

/** Execute-time consent check — required before AI processing; also used for DET persist gates. */
export async function hasActiveConsent(
  client: PoolClient,
  userId: string,
  purposeCode: string,
): Promise<boolean> {
  const row = await client.query<{ ok: boolean }>(
    `SELECT EXISTS (
       SELECT 1
       FROM governance.consent c
       JOIN governance.consent_purpose p ON p.consent_purpose_id = c.consent_purpose_id
       WHERE c.subject_user_id = $1
         AND p.code = $2
         AND p.status = 'ACTIVE'
         AND c.status = 'ACTIVE'
         AND c.scope_type = 'USER'
         AND c.scope_id = $1
     ) AS ok`,
    [userId, purposeCode],
  );
  return Boolean(row.rows[0]?.ok);
}
