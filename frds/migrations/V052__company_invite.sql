BEGIN;

CREATE TABLE business.company_invite (
    invite_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invite_code TEXT NOT NULL,
    created_by_user_id UUID NOT NULL,
    company_id UUID NOT NULL,
    membership_type TEXT NOT NULL DEFAULT 'MEMBER',
    title_snapshot TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    expires_at TIMESTAMPTZ NOT NULL DEFAULT (now() + interval '30 days'),
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_company_invite__code UNIQUE (invite_code),
    CONSTRAINT fk_company_invite__creator
        FOREIGN KEY (created_by_user_id)
        REFERENCES core.user_profile(user_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_company_invite__company
        FOREIGN KEY (company_id)
        REFERENCES business.company(company_id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_company_invite__code CHECK (invite_code ~ '^[a-hj-np-z2-9]{8}$'),
    CONSTRAINT ck_company_invite__membership_type CHECK (
        membership_type IN ('ADMIN', 'MEMBER', 'CONTRACTOR', 'OBSERVER')
    ),
    CONSTRAINT ck_company_invite__status CHECK (status IN ('ACTIVE', 'REVOKED', 'EXPIRED')),
    CONSTRAINT ck_company_invite__version CHECK (version > 0)
);

CREATE INDEX ix_company_invite__company
    ON business.company_invite (company_id, created_at DESC)
    WHERE status = 'ACTIVE';

CREATE INDEX ix_company_invite__creator
    ON business.company_invite (created_by_user_id, created_at DESC);

CREATE TABLE business.company_invite_claim (
    invite_claim_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invite_id UUID NOT NULL,
    user_id UUID NOT NULL,
    company_membership_id UUID,
    claimed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_company_invite_claim__invite
        FOREIGN KEY (invite_id)
        REFERENCES business.company_invite(invite_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_company_invite_claim__user
        FOREIGN KEY (user_id)
        REFERENCES core.user_profile(user_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_company_invite_claim__membership
        FOREIGN KEY (company_membership_id)
        REFERENCES business.company_membership(company_membership_id)
        ON DELETE SET NULL,
    CONSTRAINT uq_company_invite_claim__invite_user UNIQUE (invite_id, user_id)
);

ALTER TABLE business.company_invite ENABLE ROW LEVEL SECURITY;
ALTER TABLE business.company_invite_claim ENABLE ROW LEVEL SECURITY;

CREATE POLICY rls_company_invite__select ON business.company_invite
FOR SELECT USING (
    created_by_user_id = security.current_user_id()
    OR security.is_active_company_member(company_id)
    OR security.is_backend_app()
    OR security.is_analytics_worker()
    OR security.is_memory_worker()
    OR security.is_projection_worker()
);
CREATE POLICY rls_company_invite__backend_write ON business.company_invite
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_company_invite_claim__select ON business.company_invite_claim
FOR SELECT USING (
    user_id = security.current_user_id()
    OR security.is_backend_app()
    OR security.is_analytics_worker()
    OR security.is_memory_worker()
    OR security.is_projection_worker()
);
CREATE POLICY rls_company_invite_claim__backend_write ON business.company_invite_claim
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

GRANT SELECT, INSERT, UPDATE, DELETE ON business.company_invite TO momentra_app;
GRANT SELECT ON business.company_invite TO momentra_analytics_worker, momentra_projection_worker;
GRANT SELECT, INSERT, UPDATE, DELETE ON business.company_invite_claim TO momentra_app;
GRANT SELECT ON business.company_invite_claim TO momentra_analytics_worker, momentra_projection_worker;

COMMENT ON TABLE business.company_invite IS
  'Shareable join codes for companies. QR/deeplink uses momentra://c/{code}.';
COMMENT ON TABLE business.company_invite_claim IS
  'Users who redeemed a company invite code.';

COMMIT;
