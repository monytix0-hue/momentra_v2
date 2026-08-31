BEGIN;

-- User devices (FCM)
CREATE TABLE IF NOT EXISTS platform.user_device (
    user_device_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    device_id TEXT NOT NULL,
    platform TEXT NOT NULL,
    push_token TEXT NOT NULL,
    app_version TEXT,
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_user_device__user
        FOREIGN KEY (user_id)
        REFERENCES core.user_profile(user_id)
        ON DELETE CASCADE,
    CONSTRAINT uq_user_device__user_device UNIQUE (user_id, device_id),
    CONSTRAINT ck_user_device__platform CHECK (platform IN ('ANDROID','IOS','WEB'))
);

-- Media upload intents
CREATE TABLE IF NOT EXISTS platform.media_upload (
    media_upload_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    scope_type TEXT NOT NULL,
    scope_id UUID NOT NULL,
    content_type TEXT NOT NULL,
    bucket TEXT NOT NULL,
    object_key TEXT NOT NULL,
    size_bytes BIGINT,
    checksum_sha256 TEXT,
    status TEXT NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ,
    CONSTRAINT fk_media_upload__user
        FOREIGN KEY (user_id)
        REFERENCES core.user_profile(user_id)
        ON DELETE CASCADE,
    CONSTRAINT ck_media_upload__scope CHECK (scope_type IN ('USER','MOMENT','COMPANY')),
    CONSTRAINT ck_media_upload__status CHECK (status IN ('PENDING','COMPLETED','FAILED','EXPIRED'))
);

CREATE INDEX ix_media_upload__user_status ON platform.media_upload (user_id, status);
CREATE INDEX ix_user_device__user_active ON platform.user_device (user_id) WHERE revoked_at IS NULL;

-- Grants for new objects
GRANT USAGE ON SCHEMA shared TO momentra_app, momentra_projection_worker;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA shared TO momentra_app;
GRANT SELECT ON ALL TABLES IN SCHEMA shared TO momentra_projection_worker;

GRANT SELECT, INSERT, UPDATE, DELETE ON platform.user_device TO momentra_app;
GRANT SELECT, INSERT, UPDATE ON platform.media_upload TO momentra_app;

GRANT SELECT, INSERT, UPDATE, DELETE ON business.company_location TO momentra_app;
GRANT SELECT ON business.company_location TO momentra_projection_worker;

ALTER TABLE business.company_location ENABLE ROW LEVEL SECURITY;
ALTER TABLE shared.poll ENABLE ROW LEVEL SECURITY;
ALTER TABLE shared.poll_option ENABLE ROW LEVEL SECURITY;
ALTER TABLE shared.poll_vote ENABLE ROW LEVEL SECURITY;
ALTER TABLE platform.user_device ENABLE ROW LEVEL SECURITY;
ALTER TABLE platform.media_upload ENABLE ROW LEVEL SECURITY;

-- Backend app policies (coarse; Node re-authorizes commands)
CREATE POLICY company_location_member_select ON business.company_location
    FOR SELECT TO momentra_app
    USING (security.is_active_company_member(company_id));

CREATE POLICY company_location_member_write ON business.company_location
    FOR ALL TO momentra_app
    USING (security.is_active_company_member(company_id))
    WITH CHECK (security.is_active_company_member(company_id));

CREATE POLICY shared_poll_access ON shared.poll
    FOR ALL TO momentra_app
    USING (security.can_access_moment(moment_id))
    WITH CHECK (security.can_access_moment(moment_id));

CREATE POLICY shared_poll_option_access ON shared.poll_option
    FOR ALL TO momentra_app
    USING (
        EXISTS (
            SELECT 1 FROM shared.poll p
            WHERE p.poll_id = poll_option.poll_id
              AND security.can_access_moment(p.moment_id)
        )
    )
    WITH CHECK (
        EXISTS (
            SELECT 1 FROM shared.poll p
            WHERE p.poll_id = poll_option.poll_id
              AND security.can_access_moment(p.moment_id)
        )
    );

CREATE POLICY shared_poll_vote_access ON shared.poll_vote
    FOR ALL TO momentra_app
    USING (
        EXISTS (
            SELECT 1 FROM shared.poll p
            WHERE p.poll_id = poll_vote.poll_id
              AND security.can_access_moment(p.moment_id)
        )
    )
    WITH CHECK (
        voter_user_id = security.current_user_id()
        AND EXISTS (
            SELECT 1 FROM shared.poll p
            WHERE p.poll_id = poll_vote.poll_id
              AND security.can_access_moment(p.moment_id)
              AND p.status = 'OPEN'
        )
    );

CREATE POLICY user_device_own ON platform.user_device
    FOR ALL TO momentra_app
    USING (user_id = security.current_user_id())
    WITH CHECK (user_id = security.current_user_id());

CREATE POLICY media_upload_own ON platform.media_upload
    FOR ALL TO momentra_app
    USING (user_id = security.current_user_id())
    WITH CHECK (user_id = security.current_user_id());

COMMIT;
