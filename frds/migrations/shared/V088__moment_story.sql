-- Moment Story: versioned snapshot + artifacts + shares; scheduler index for end_at auto-complete.

CREATE INDEX IF NOT EXISTS ix_moment__active_end_at
  ON core.moment (end_at)
  WHERE status = 'ACTIVE' AND end_at IS NOT NULL;

CREATE TABLE IF NOT EXISTS core.moment_story (
  story_id UUID PRIMARY KEY,
  moment_id UUID NOT NULL REFERENCES core.moment (moment_id),
  completion_id UUID NOT NULL,
  story_version INT NOT NULL DEFAULT 1,
  status TEXT NOT NULL DEFAULT 'GENERATING',
  family_profile TEXT NOT NULL DEFAULT 'SHARED_EXPERIENCE',
  generated_at TIMESTAMPTZ,
  generated_by_user_id UUID,
  data_version INT NOT NULL DEFAULT 1,
  narrative_version INT NOT NULL DEFAULT 1,
  render_version INT NOT NULL DEFAULT 1,
  error_message TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT ck_moment_story__status CHECK (
    status IN ('GENERATING', 'READY', 'FAILED')
  ),
  CONSTRAINT uq_moment_story__moment_version UNIQUE (moment_id, story_version)
);

CREATE INDEX IF NOT EXISTS ix_moment_story__moment
  ON core.moment_story (moment_id, story_version DESC);

CREATE TABLE IF NOT EXISTS core.moment_story_snapshot (
  snapshot_id UUID PRIMARY KEY,
  story_id UUID NOT NULL REFERENCES core.moment_story (story_id) ON DELETE CASCADE,
  snapshot_json JSONB NOT NULL,
  source_manifest JSONB NOT NULL DEFAULT '{}'::jsonb,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT uq_moment_story_snapshot__story UNIQUE (story_id)
);

CREATE TABLE IF NOT EXISTS core.moment_story_artifact (
  artifact_id UUID PRIMARY KEY,
  story_id UUID NOT NULL REFERENCES core.moment_story (story_id) ON DELETE CASCADE,
  artifact_type TEXT NOT NULL,
  content_type TEXT NOT NULL,
  storage_ref TEXT,
  inline_body TEXT,
  checksum TEXT,
  status TEXT NOT NULL DEFAULT 'READY',
  generated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT ck_moment_story_artifact__type CHECK (
    artifact_type IN (
      'STORY_PAYLOAD',
      'CHAPTER_HTML',
      'CHAPTER_PNG_SVG',
      'BOOKLET_HTML',
      'SHARE_COVER',
      'VIDEO_REEL_SPEC'
    )
  )
);

CREATE INDEX IF NOT EXISTS ix_moment_story_artifact__story
  ON core.moment_story_artifact (story_id, artifact_type);

CREATE TABLE IF NOT EXISTS core.moment_story_share (
  share_id UUID PRIMARY KEY,
  story_id UUID NOT NULL REFERENCES core.moment_story (story_id) ON DELETE CASCADE,
  share_token TEXT NOT NULL UNIQUE,
  created_by_user_id UUID NOT NULL,
  access_mode TEXT NOT NULL DEFAULT 'LINK_VIEW',
  expires_at TIMESTAMPTZ,
  revoked_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  CONSTRAINT ck_moment_story_share__mode CHECK (
    access_mode IN ('LINK_VIEW', 'PARTICIPANT')
  )
);

CREATE INDEX IF NOT EXISTS ix_moment_story_share__story
  ON core.moment_story_share (story_id)
  WHERE revoked_at IS NULL;
