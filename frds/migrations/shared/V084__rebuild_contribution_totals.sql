-- V084: Rebuild group finance contribution_total from finance.contribution (RECORDED only).
-- Decouples Collected from expense_total (expenses no longer inflate contribution_total).

UPDATE projection.group_finance_snapshot s
SET contribution_total = COALESCE(x.total, 0),
    updated_at = now()
FROM (
  SELECT moment_id, currency_code, SUM(amount) AS total
  FROM finance.contribution
  WHERE status = 'RECORDED'
  GROUP BY moment_id, currency_code
) x
WHERE s.moment_id = x.moment_id
  AND s.currency_code = x.currency_code;

-- Snapshots with no RECORDED contributions → 0 (clears prior expense-mirrored values)
UPDATE projection.group_finance_snapshot s
SET contribution_total = 0,
    updated_at = now()
WHERE NOT EXISTS (
  SELECT 1
  FROM finance.contribution c
  WHERE c.moment_id = s.moment_id
    AND c.currency_code = s.currency_code
    AND c.status = 'RECORDED'
);

UPDATE projection.group_finance_position p
SET contribution_total = COALESCE(x.total, 0),
    updated_at = now()
FROM (
  SELECT moment_id, participant_id, currency_code, SUM(amount) AS total
  FROM finance.contribution
  WHERE status = 'RECORDED'
  GROUP BY moment_id, participant_id, currency_code
) x
WHERE p.moment_id = x.moment_id
  AND p.participant_id = x.participant_id
  AND p.currency_code = x.currency_code;

UPDATE projection.group_finance_position p
SET contribution_total = 0,
    updated_at = now()
WHERE NOT EXISTS (
  SELECT 1
  FROM finance.contribution c
  WHERE c.moment_id = p.moment_id
    AND c.participant_id = p.participant_id
    AND c.currency_code = p.currency_code
    AND c.status = 'RECORDED'
);
