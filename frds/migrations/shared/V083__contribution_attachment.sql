-- V083: Link media uploads (receipts/bills) to group contributions.
CREATE TABLE IF NOT EXISTS finance.contribution_attachment (
  contribution_id UUID NOT NULL,
  upload_id UUID NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (contribution_id, upload_id),
  CONSTRAINT fk_contribution_attachment__contribution
    FOREIGN KEY (contribution_id) REFERENCES finance.contribution(contribution_id) ON DELETE CASCADE,
  CONSTRAINT fk_contribution_attachment__upload
    FOREIGN KEY (upload_id) REFERENCES platform.media_upload(media_upload_id) ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS ix_contribution_attachment__upload
  ON finance.contribution_attachment (upload_id);

GRANT SELECT, INSERT, UPDATE, DELETE ON finance.contribution_attachment TO momentra_app;
GRANT SELECT ON finance.contribution_attachment TO momentra_projection_worker, momentra_analytics_worker, momentra_memory_worker;

ALTER TABLE finance.contribution_attachment ENABLE ROW LEVEL SECURITY;

CREATE POLICY rls_contribution_attachment__select_participant ON finance.contribution_attachment
FOR SELECT USING (
  (EXISTS (
    SELECT 1 FROM finance.contribution c
    WHERE c.contribution_id = contribution_attachment.contribution_id
      AND security.is_active_group_participant(c.moment_id)
  ))
  OR security.is_backend_app()
  OR security.is_analytics_worker()
  OR security.is_memory_worker()
  OR security.is_projection_worker()
);

CREATE POLICY rls_contribution_attachment__backend_write ON finance.contribution_attachment
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

COMMENT ON TABLE finance.contribution_attachment IS 'Media uploads (receipts/bills) linked to a contribution.';
