BEGIN;

CREATE TABLE collaboration.moment_invite (
    invite_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invite_code TEXT NOT NULL,
    created_by_user_id UUID NOT NULL,
    moment_id UUID,
    moment_type_code TEXT NOT NULL,
    group_family TEXT NOT NULL,
    title_snapshot TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'PENDING',
    expires_at TIMESTAMPTZ NOT NULL DEFAULT (now() + interval '30 days'),
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_moment_invite__code UNIQUE (invite_code),
    CONSTRAINT fk_moment_invite__creator
        FOREIGN KEY (created_by_user_id)
        REFERENCES core.user_profile(user_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_moment_invite__moment
        FOREIGN KEY (moment_id)
        REFERENCES collaboration.group_moment_context(moment_id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_moment_invite__code CHECK (invite_code ~ '^[a-z0-9]+-[a-z0-9-]+-[a-f0-9]{8}$'),
    CONSTRAINT ck_moment_invite__family CHECK (group_family IN (
        'SHARED_EXPERIENCE','SHARED_PURCHASE','SHARED_LIVING','SHARED_GOAL','COMMUNITY_COORDINATION'
    )),
    CONSTRAINT ck_moment_invite__status CHECK (status IN ('PENDING','ACTIVE','REVOKED','EXPIRED')),
    CONSTRAINT ck_moment_invite__bound CHECK (
        (status = 'PENDING' AND moment_id IS NULL)
        OR (status IN ('ACTIVE','REVOKED','EXPIRED'))
    ),
    CONSTRAINT ck_moment_invite__version CHECK (version > 0)
);

CREATE INDEX ix_moment_invite__creator_pending
    ON collaboration.moment_invite (created_by_user_id, created_at DESC)
    WHERE status = 'PENDING';

CREATE INDEX ix_moment_invite__moment
    ON collaboration.moment_invite (moment_id)
    WHERE moment_id IS NOT NULL;

CREATE TABLE collaboration.moment_invite_claim (
    invite_claim_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invite_id UUID NOT NULL,
    user_id UUID NOT NULL,
    participant_id UUID,
    claimed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_moment_invite_claim__invite
        FOREIGN KEY (invite_id)
        REFERENCES collaboration.moment_invite(invite_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_moment_invite_claim__user
        FOREIGN KEY (user_id)
        REFERENCES core.user_profile(user_id)
        ON DELETE RESTRICT,
    CONSTRAINT uq_moment_invite_claim__invite_user UNIQUE (invite_id, user_id)
);

ALTER TABLE collaboration.moment_invite ENABLE ROW LEVEL SECURITY;
ALTER TABLE collaboration.moment_invite_claim ENABLE ROW LEVEL SECURITY;

CREATE POLICY rls_moment_invite__select ON collaboration.moment_invite
FOR SELECT USING (
    created_by_user_id = security.current_user_id()
    OR (moment_id IS NOT NULL AND security.is_active_group_participant(moment_id))
    OR security.is_backend_app()
    OR security.is_analytics_worker()
    OR security.is_memory_worker()
    OR security.is_projection_worker()
);
CREATE POLICY rls_moment_invite__backend_write ON collaboration.moment_invite
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_moment_invite_claim__select ON collaboration.moment_invite_claim
FOR SELECT USING (
    user_id = security.current_user_id()
    OR security.is_backend_app()
    OR security.is_analytics_worker()
    OR security.is_memory_worker()
    OR security.is_projection_worker()
);
CREATE POLICY rls_moment_invite_claim__backend_write ON collaboration.moment_invite_claim
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

GRANT SELECT, INSERT, UPDATE, DELETE ON collaboration.moment_invite TO momentra_app;
GRANT SELECT ON collaboration.moment_invite TO momentra_analytics_worker, momentra_projection_worker;
GRANT SELECT, INSERT, UPDATE, DELETE ON collaboration.moment_invite_claim TO momentra_app;
GRANT SELECT ON collaboration.moment_invite_claim TO momentra_analytics_worker, momentra_projection_worker;

COMMENT ON TABLE collaboration.moment_invite IS
  'Shareable join codes for Group moments. PENDING before activation, ACTIVE after bind.';
COMMENT ON TABLE collaboration.moment_invite_claim IS
  'Users who scanned or opened a join code before or after the moment was activated.';

COMMIT;
