BEGIN;

CREATE SCHEMA IF NOT EXISTS shared;
COMMENT ON SCHEMA shared IS 'Cross-domain shared kernels (Poll, etc.) used by Group and Business.';

CREATE TABLE shared.poll (
    poll_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    moment_id UUID NOT NULL,
    domain_code TEXT NOT NULL,
    company_id UUID,
    question TEXT NOT NULL,
    poll_type TEXT NOT NULL DEFAULT 'SINGLE_CHOICE',
    closes_at TIMESTAMPTZ,
    status TEXT NOT NULL DEFAULT 'OPEN',
    created_by_user_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_shared_poll__moment
        FOREIGN KEY (moment_id)
        REFERENCES core.moment(moment_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_shared_poll__company
        FOREIGN KEY (company_id)
        REFERENCES business.company(company_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_shared_poll__created_by
        FOREIGN KEY (created_by_user_id)
        REFERENCES core.user_profile(user_id)
        ON DELETE RESTRICT,
    CONSTRAINT uq_shared_poll__id_moment UNIQUE (poll_id, moment_id),
    CONSTRAINT ck_shared_poll__domain CHECK (domain_code IN ('GROUP','BUSINESS')),
    CONSTRAINT ck_shared_poll__type CHECK (poll_type IN ('SINGLE_CHOICE','MULTI_CHOICE','YES_NO')),
    CONSTRAINT ck_shared_poll__status CHECK (status IN ('DRAFT','OPEN','CLOSED','CANCELLED')),
    CONSTRAINT ck_shared_poll__business_company CHECK (
        (domain_code = 'GROUP' AND company_id IS NULL)
        OR (domain_code = 'BUSINESS' AND company_id IS NOT NULL)
    )
);

CREATE TABLE shared.poll_option (
    poll_option_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    poll_id UUID NOT NULL,
    option_text TEXT NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_shared_poll_option__poll
        FOREIGN KEY (poll_id)
        REFERENCES shared.poll(poll_id)
        ON DELETE CASCADE,
    CONSTRAINT uq_shared_poll_option__id_poll UNIQUE (poll_option_id, poll_id),
    CONSTRAINT uq_shared_poll_option__poll_sort UNIQUE (poll_id, sort_order),
    CONSTRAINT ck_shared_poll_option__sort_order CHECK (sort_order >= 0)
);

CREATE TABLE shared.poll_vote (
    poll_vote_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    poll_id UUID NOT NULL,
    poll_option_id UUID NOT NULL,
    moment_id UUID NOT NULL,
    voter_user_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_shared_poll_vote__poll_moment
        FOREIGN KEY (poll_id, moment_id)
        REFERENCES shared.poll(poll_id, moment_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_shared_poll_vote__option_poll
        FOREIGN KEY (poll_option_id, poll_id)
        REFERENCES shared.poll_option(poll_option_id, poll_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_shared_poll_vote__voter
        FOREIGN KEY (voter_user_id)
        REFERENCES core.user_profile(user_id)
        ON DELETE RESTRICT,
    CONSTRAINT uq_shared_poll_vote__voter_option UNIQUE (poll_option_id, voter_user_id)
);

-- Migrate existing Group polls into shared kernel (if any exist).
INSERT INTO shared.poll (
    poll_id, moment_id, domain_code, company_id, question, poll_type,
    closes_at, status, created_by_user_id, created_at, updated_at
)
SELECT
    p.poll_id,
    p.moment_id,
    'GROUP',
    NULL,
    p.question,
    p.poll_type,
    p.closes_at,
    p.status,
    COALESCE(
        (SELECT mp.user_id FROM collaboration.moment_participant mp
         WHERE mp.participant_id = p.created_by_participant_id LIMIT 1),
        (SELECT m.created_by_user_id FROM core.moment m WHERE m.moment_id = p.moment_id)
    ),
    p.created_at,
    p.updated_at
FROM collaboration.poll p
ON CONFLICT (poll_id) DO NOTHING;

INSERT INTO shared.poll_option (poll_option_id, poll_id, option_text, sort_order, created_at)
SELECT poll_option_id, poll_id, option_text, sort_order, created_at
FROM collaboration.poll_option
ON CONFLICT (poll_option_id) DO NOTHING;

-- Deprecate legacy Group-only poll tables (retain for rollback; app uses shared.*).
COMMENT ON TABLE collaboration.poll IS 'DEPRECATED: use shared.poll. Migrated in V032.';
COMMENT ON TABLE collaboration.poll_option IS 'DEPRECATED: use shared.poll_option.';
COMMENT ON TABLE collaboration.poll_vote IS 'DEPRECATED: use shared.poll_vote.';

COMMIT;
