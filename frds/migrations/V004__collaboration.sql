BEGIN;

CREATE SCHEMA collaboration;
COMMENT ON SCHEMA collaboration IS 'Momentra Group domain: Moment-centric participation and shared experience, purchase, living, shared-goal and community-coordination facts.';

CREATE TABLE collaboration.group_moment_context (
    moment_id UUID PRIMARY KEY,
    domain_code TEXT NOT NULL DEFAULT 'GROUP',
    group_family TEXT NOT NULL,
    organizer_user_id UUID NOT NULL,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_group_moment_context__moment_domain
        FOREIGN KEY (moment_id, domain_code)
        REFERENCES core.moment(moment_id, domain_code)
        ON DELETE RESTRICT,
    CONSTRAINT fk_group_moment_context__organizer
        FOREIGN KEY (organizer_user_id)
        REFERENCES core.user_profile(user_id)
        ON DELETE RESTRICT,
    CONSTRAINT uq_group_moment_context__moment_family UNIQUE (moment_id, group_family),
    CONSTRAINT ck_group_moment_context__domain CHECK (domain_code = 'GROUP'),
    CONSTRAINT ck_group_moment_context__family CHECK (group_family IN ('SHARED_EXPERIENCE','SHARED_PURCHASE','SHARED_LIVING','SHARED_GOAL','COMMUNITY_COORDINATION')),
    CONSTRAINT ck_group_moment_context__status CHECK (status IN ('ACTIVE','COMPLETED','CANCELLED','ARCHIVED')),
    CONSTRAINT ck_group_moment_context__version CHECK (version > 0)
);

CREATE TABLE collaboration.moment_participant (
    participant_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    moment_id UUID NOT NULL,
    user_id UUID,
    external_party_id UUID,
    participant_role TEXT NOT NULL DEFAULT 'PARTICIPANT',
    status TEXT NOT NULL DEFAULT 'INVITED',
    invited_at TIMESTAMPTZ,
    joined_at TIMESTAMPTZ,
    left_at TIMESTAMPTZ,
    removed_at TIMESTAMPTZ,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_moment_participant__group_moment
        FOREIGN KEY (moment_id)
        REFERENCES collaboration.group_moment_context(moment_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_moment_participant__user
        FOREIGN KEY (user_id)
        REFERENCES core.user_profile(user_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_moment_participant__external_party
        FOREIGN KEY (external_party_id)
        REFERENCES core.external_party(external_party_id)
        ON DELETE RESTRICT,
    CONSTRAINT uq_moment_participant__id_moment UNIQUE (participant_id, moment_id),
    CONSTRAINT ck_moment_participant__identity CHECK (((user_id IS NOT NULL)::int + (external_party_id IS NOT NULL)::int) = 1),
    CONSTRAINT ck_moment_participant__role CHECK (participant_role IN ('ORGANIZER','CO_ORGANIZER','PARTICIPANT','RESIDENT','CONTRIBUTOR','OBSERVER')),
    CONSTRAINT ck_moment_participant__status CHECK (status IN ('INVITED','ACTIVE','LEFT','REMOVED','DECLINED')),
    CONSTRAINT ck_moment_participant__version CHECK (version > 0)
);

CREATE UNIQUE INDEX uq_moment_participant__moment_user_open
    ON collaboration.moment_participant (moment_id, user_id)
    WHERE user_id IS NOT NULL AND status IN ('INVITED','ACTIVE');

CREATE TABLE collaboration.shared_experience_context (
    moment_id UUID PRIMARY KEY,
    group_family TEXT NOT NULL DEFAULT 'SHARED_EXPERIENCE',
    experience_kind TEXT,
    destination_text TEXT,
    venue_text TEXT,
    start_at TIMESTAMPTZ,
    end_at TIMESTAMPTZ,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_shared_experience_context__group_family
        FOREIGN KEY (moment_id, group_family)
        REFERENCES collaboration.group_moment_context(moment_id, group_family)
        ON DELETE RESTRICT,
    CONSTRAINT ck_shared_experience_context__family CHECK (group_family = 'SHARED_EXPERIENCE'),
    CONSTRAINT ck_shared_experience_context__time CHECK (end_at IS NULL OR start_at IS NULL OR end_at >= start_at),
    CONSTRAINT ck_shared_experience_context__status CHECK (status IN ('ACTIVE','COMPLETED','CANCELLED','ARCHIVED'))
);

CREATE TABLE collaboration.planning_item (
    planning_item_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    moment_id UUID NOT NULL,
    title TEXT NOT NULL,
    description TEXT,
    due_at TIMESTAMPTZ,
    status TEXT NOT NULL DEFAULT 'OPEN',
    created_by_participant_id UUID,
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_planning_item__group_moment
        FOREIGN KEY (moment_id)
        REFERENCES collaboration.group_moment_context(moment_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_planning_item__participant_moment
        FOREIGN KEY (created_by_participant_id, moment_id)
        REFERENCES collaboration.moment_participant(participant_id, moment_id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_planning_item__status CHECK (status IN ('OPEN','IN_PROGRESS','DONE','CANCELLED')),
    CONSTRAINT ck_planning_item__version CHECK (version > 0)
);

CREATE TABLE collaboration.booking (
    booking_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    moment_id UUID NOT NULL,
    booking_type TEXT NOT NULL,
    provider_name TEXT,
    reference_code TEXT,
    booked_at TIMESTAMPTZ,
    start_at TIMESTAMPTZ,
    end_at TIMESTAMPTZ,
    amount NUMERIC(19,4),
    currency_code CHAR(3),
    status TEXT NOT NULL DEFAULT 'PLANNED',
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_booking__group_moment
        FOREIGN KEY (moment_id)
        REFERENCES collaboration.group_moment_context(moment_id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_booking__status CHECK (status IN ('PLANNED','BOOKED','CONFIRMED','CANCELLED','COMPLETED')),
    CONSTRAINT ck_booking__time CHECK (end_at IS NULL OR start_at IS NULL OR end_at >= start_at),
    CONSTRAINT ck_booking__amount CHECK (amount IS NULL OR amount >= 0),
    CONSTRAINT ck_booking__currency CHECK (currency_code IS NULL OR currency_code ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_booking__version CHECK (version > 0)
);

CREATE TABLE collaboration.group_vendor (
    group_vendor_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    moment_id UUID NOT NULL,
    external_party_id UUID,
    vendor_name TEXT NOT NULL,
    vendor_type TEXT,
    contact_details JSONB NOT NULL DEFAULT '{}'::jsonb,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_group_vendor__group_moment
        FOREIGN KEY (moment_id)
        REFERENCES collaboration.group_moment_context(moment_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_group_vendor__external_party
        FOREIGN KEY (external_party_id)
        REFERENCES core.external_party(external_party_id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_group_vendor__status CHECK (status IN ('ACTIVE','INACTIVE','ARCHIVED')),
    CONSTRAINT ck_group_vendor__version CHECK (version > 0)
);

CREATE TABLE collaboration.group_update (
    group_update_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    moment_id UUID NOT NULL,
    participant_id UUID,
    title TEXT,
    body TEXT NOT NULL,
    visibility TEXT NOT NULL DEFAULT 'PARTICIPANTS',
    status TEXT NOT NULL DEFAULT 'PUBLISHED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_group_update__group_moment
        FOREIGN KEY (moment_id)
        REFERENCES collaboration.group_moment_context(moment_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_group_update__participant_moment
        FOREIGN KEY (participant_id, moment_id)
        REFERENCES collaboration.moment_participant(participant_id, moment_id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_group_update__visibility CHECK (visibility IN ('PARTICIPANTS','ORGANIZERS')),
    CONSTRAINT ck_group_update__status CHECK (status IN ('DRAFT','PUBLISHED','ARCHIVED'))
);

CREATE TABLE collaboration.poll (
    poll_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    moment_id UUID NOT NULL,
    question TEXT NOT NULL,
    poll_type TEXT NOT NULL DEFAULT 'SINGLE_CHOICE',
    closes_at TIMESTAMPTZ,
    status TEXT NOT NULL DEFAULT 'OPEN',
    created_by_participant_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_poll__group_moment
        FOREIGN KEY (moment_id)
        REFERENCES collaboration.group_moment_context(moment_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_poll__participant_moment
        FOREIGN KEY (created_by_participant_id, moment_id)
        REFERENCES collaboration.moment_participant(participant_id, moment_id)
        ON DELETE RESTRICT,
    CONSTRAINT uq_poll__id_moment UNIQUE (poll_id, moment_id),
    CONSTRAINT ck_poll__type CHECK (poll_type IN ('SINGLE_CHOICE','MULTI_CHOICE','YES_NO')),
    CONSTRAINT ck_poll__status CHECK (status IN ('DRAFT','OPEN','CLOSED','CANCELLED'))
);

CREATE TABLE collaboration.poll_option (
    poll_option_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    poll_id UUID NOT NULL,
    option_text TEXT NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_poll_option__poll
        FOREIGN KEY (poll_id)
        REFERENCES collaboration.poll(poll_id)
        ON DELETE CASCADE,
    CONSTRAINT uq_poll_option__id_poll UNIQUE (poll_option_id, poll_id),
    CONSTRAINT uq_poll_option__poll_sort UNIQUE (poll_id, sort_order),
    CONSTRAINT ck_poll_option__sort_order CHECK (sort_order >= 0)
);

CREATE TABLE collaboration.poll_vote (
    poll_vote_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    poll_id UUID NOT NULL,
    poll_option_id UUID NOT NULL,
    moment_id UUID NOT NULL,
    participant_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_poll_vote__poll_moment
        FOREIGN KEY (poll_id, moment_id)
        REFERENCES collaboration.poll(poll_id, moment_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_poll_vote__option_poll
        FOREIGN KEY (poll_option_id, poll_id)
        REFERENCES collaboration.poll_option(poll_option_id, poll_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_poll_vote__participant_moment
        FOREIGN KEY (participant_id, moment_id)
        REFERENCES collaboration.moment_participant(participant_id, moment_id)
        ON DELETE RESTRICT,
    CONSTRAINT uq_poll_vote__participant_option UNIQUE (poll_option_id, participant_id)
);

CREATE TABLE collaboration.attendance (
    attendance_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    moment_id UUID NOT NULL,
    participant_id UUID NOT NULL,
    attendance_status TEXT NOT NULL,
    checked_at TIMESTAMPTZ,
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_attendance__participant_moment
        FOREIGN KEY (participant_id, moment_id)
        REFERENCES collaboration.moment_participant(participant_id, moment_id)
        ON DELETE RESTRICT,
    CONSTRAINT uq_attendance__moment_participant UNIQUE (moment_id, participant_id),
    CONSTRAINT ck_attendance__status CHECK (attendance_status IN ('UNKNOWN','EXPECTED','CONFIRMED','ATTENDED','ABSENT'))
);

CREATE TABLE collaboration.shared_purchase_context (
    moment_id UUID PRIMARY KEY,
    group_family TEXT NOT NULL DEFAULT 'SHARED_PURCHASE',
    purchase_purpose TEXT,
    target_date DATE,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_shared_purchase_context__group_family
        FOREIGN KEY (moment_id, group_family)
        REFERENCES collaboration.group_moment_context(moment_id, group_family)
        ON DELETE RESTRICT,
    CONSTRAINT ck_shared_purchase_context__family CHECK (group_family = 'SHARED_PURCHASE'),
    CONSTRAINT ck_shared_purchase_context__status CHECK (status IN ('ACTIVE','COMPLETED','CANCELLED','ARCHIVED'))
);

CREATE TABLE collaboration.purchase_item (
    purchase_item_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    moment_id UUID NOT NULL,
    title TEXT NOT NULL,
    description TEXT,
    quantity NUMERIC(12,3) NOT NULL DEFAULT 1,
    target_amount NUMERIC(19,4),
    currency_code CHAR(3),
    status TEXT NOT NULL DEFAULT 'PLANNED',
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_purchase_item__shared_purchase
        FOREIGN KEY (moment_id)
        REFERENCES collaboration.shared_purchase_context(moment_id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_purchase_item__quantity CHECK (quantity > 0),
    CONSTRAINT ck_purchase_item__target_amount CHECK (target_amount IS NULL OR target_amount >= 0),
    CONSTRAINT ck_purchase_item__currency CHECK (currency_code IS NULL OR currency_code ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_purchase_item__status CHECK (status IN ('PLANNED','SELECTED','PURCHASED','CANCELLED','HANDED_OVER')),
    CONSTRAINT ck_purchase_item__version CHECK (version > 0)
);

CREATE TABLE collaboration.ownership_record (
    ownership_record_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    moment_id UUID NOT NULL,
    purchase_item_id UUID,
    participant_id UUID,
    external_party_id UUID,
    ownership_share NUMERIC(9,6),
    ownership_note TEXT,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_ownership_record__shared_purchase
        FOREIGN KEY (moment_id)
        REFERENCES collaboration.shared_purchase_context(moment_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_ownership_record__purchase_item
        FOREIGN KEY (purchase_item_id)
        REFERENCES collaboration.purchase_item(purchase_item_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_ownership_record__participant_moment
        FOREIGN KEY (participant_id, moment_id)
        REFERENCES collaboration.moment_participant(participant_id, moment_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_ownership_record__external_party
        FOREIGN KEY (external_party_id)
        REFERENCES core.external_party(external_party_id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_ownership_record__owner CHECK (((participant_id IS NOT NULL)::int + (external_party_id IS NOT NULL)::int) = 1),
    CONSTRAINT ck_ownership_record__share CHECK (ownership_share IS NULL OR (ownership_share > 0 AND ownership_share <= 1)),
    CONSTRAINT ck_ownership_record__status CHECK (status IN ('ACTIVE','TRANSFERRED','ENDED'))
);

CREATE TABLE collaboration.delivery_handover (
    delivery_handover_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    moment_id UUID NOT NULL,
    purchase_item_id UUID,
    participant_id UUID,
    handover_type TEXT NOT NULL DEFAULT 'DELIVERY',
    scheduled_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    status TEXT NOT NULL DEFAULT 'PLANNED',
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_delivery_handover__shared_purchase
        FOREIGN KEY (moment_id)
        REFERENCES collaboration.shared_purchase_context(moment_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_delivery_handover__purchase_item
        FOREIGN KEY (purchase_item_id)
        REFERENCES collaboration.purchase_item(purchase_item_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_delivery_handover__participant_moment
        FOREIGN KEY (participant_id, moment_id)
        REFERENCES collaboration.moment_participant(participant_id, moment_id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_delivery_handover__type CHECK (handover_type IN ('DELIVERY','HANDOVER','PICKUP')),
    CONSTRAINT ck_delivery_handover__status CHECK (status IN ('PLANNED','IN_PROGRESS','COMPLETED','CANCELLED')),
    CONSTRAINT ck_delivery_handover__completed CHECK (status <> 'COMPLETED' OR completed_at IS NOT NULL)
);

CREATE TABLE collaboration.shared_living_context (
    moment_id UUID PRIMARY KEY,
    group_family TEXT NOT NULL DEFAULT 'SHARED_LIVING',
    property_name TEXT,
    address_text TEXT,
    start_date DATE,
    end_date DATE,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_shared_living_context__group_family
        FOREIGN KEY (moment_id, group_family)
        REFERENCES collaboration.group_moment_context(moment_id, group_family)
        ON DELETE RESTRICT,
    CONSTRAINT ck_shared_living_context__family CHECK (group_family = 'SHARED_LIVING'),
    CONSTRAINT ck_shared_living_context__date CHECK (end_date IS NULL OR start_date IS NULL OR end_date >= start_date),
    CONSTRAINT ck_shared_living_context__status CHECK (status IN ('ACTIVE','COMPLETED','CANCELLED','ARCHIVED'))
);

CREATE TABLE collaboration.resident (
    resident_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    moment_id UUID NOT NULL,
    participant_id UUID NOT NULL,
    resident_role TEXT NOT NULL DEFAULT 'RESIDENT',
    move_in_date DATE,
    move_out_date DATE,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_resident__shared_living
        FOREIGN KEY (moment_id)
        REFERENCES collaboration.shared_living_context(moment_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_resident__participant_moment
        FOREIGN KEY (participant_id, moment_id)
        REFERENCES collaboration.moment_participant(participant_id, moment_id)
        ON DELETE RESTRICT,
    CONSTRAINT uq_resident__moment_participant UNIQUE (moment_id, participant_id),
    CONSTRAINT ck_resident__role CHECK (resident_role IN ('RESIDENT','PRIMARY_RESIDENT','OWNER','TENANT','GUARDIAN')),
    CONSTRAINT ck_resident__date CHECK (move_out_date IS NULL OR move_in_date IS NULL OR move_out_date >= move_in_date),
    CONSTRAINT ck_resident__status CHECK (status IN ('ACTIVE','MOVED_OUT','REMOVED'))
);

CREATE TABLE collaboration.living_rule (
    living_rule_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    moment_id UUID NOT NULL,
    title TEXT NOT NULL,
    rule_text TEXT NOT NULL,
    effective_from DATE,
    effective_to DATE,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_living_rule__shared_living
        FOREIGN KEY (moment_id)
        REFERENCES collaboration.shared_living_context(moment_id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_living_rule__date CHECK (effective_to IS NULL OR effective_from IS NULL OR effective_to >= effective_from),
    CONSTRAINT ck_living_rule__status CHECK (status IN ('ACTIVE','INACTIVE','ARCHIVED')),
    CONSTRAINT ck_living_rule__version CHECK (version > 0)
);

CREATE TABLE collaboration.shared_asset (
    shared_asset_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    moment_id UUID NOT NULL,
    title TEXT NOT NULL,
    asset_type TEXT,
    acquired_on DATE,
    condition_code TEXT,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_shared_asset__shared_living
        FOREIGN KEY (moment_id)
        REFERENCES collaboration.shared_living_context(moment_id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_shared_asset__condition CHECK (condition_code IS NULL OR condition_code IN ('NEW','GOOD','FAIR','POOR','OUT_OF_SERVICE')),
    CONSTRAINT ck_shared_asset__status CHECK (status IN ('ACTIVE','INACTIVE','DISPOSED','ARCHIVED')),
    CONSTRAINT ck_shared_asset__version CHECK (version > 0)
);

CREATE TABLE collaboration.maintenance_record (
    maintenance_record_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    moment_id UUID NOT NULL,
    shared_asset_id UUID,
    title TEXT NOT NULL,
    description TEXT,
    scheduled_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    status TEXT NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_maintenance_record__shared_living
        FOREIGN KEY (moment_id)
        REFERENCES collaboration.shared_living_context(moment_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_maintenance_record__asset
        FOREIGN KEY (shared_asset_id)
        REFERENCES collaboration.shared_asset(shared_asset_id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_maintenance_record__status CHECK (status IN ('OPEN','SCHEDULED','IN_PROGRESS','COMPLETED','CANCELLED')),
    CONSTRAINT ck_maintenance_record__completed CHECK (status <> 'COMPLETED' OR completed_at IS NOT NULL)
);

CREATE TABLE collaboration.shared_goal_context (
    moment_id UUID PRIMARY KEY,
    group_family TEXT NOT NULL DEFAULT 'SHARED_GOAL',
    goal_theme TEXT,
    target_date DATE,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_shared_goal_context__group_family
        FOREIGN KEY (moment_id, group_family)
        REFERENCES collaboration.group_moment_context(moment_id, group_family)
        ON DELETE RESTRICT,
    CONSTRAINT ck_shared_goal_context__family CHECK (group_family = 'SHARED_GOAL'),
    CONSTRAINT ck_shared_goal_context__status CHECK (status IN ('ACTIVE','COMPLETED','CANCELLED','ARCHIVED'))
);

CREATE TABLE collaboration.community_coordination_context (
    moment_id UUID PRIMARY KEY,
    group_family TEXT NOT NULL DEFAULT 'COMMUNITY_COORDINATION',
    coordination_theme TEXT,
    area_text TEXT,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_community_coordination_context__group_family
        FOREIGN KEY (moment_id, group_family)
        REFERENCES collaboration.group_moment_context(moment_id, group_family)
        ON DELETE RESTRICT,
    CONSTRAINT ck_community_coordination_context__family CHECK (group_family = 'COMMUNITY_COORDINATION'),
    CONSTRAINT ck_community_coordination_context__status CHECK (status IN ('ACTIVE','COMPLETED','CANCELLED','ARCHIVED'))
);

CREATE TABLE collaboration.coordination_item (
    coordination_item_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    moment_id UUID NOT NULL,
    title TEXT NOT NULL,
    description TEXT,
    owner_participant_id UUID,
    due_at TIMESTAMPTZ,
    status TEXT NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_coordination_item__community_context
        FOREIGN KEY (moment_id)
        REFERENCES collaboration.community_coordination_context(moment_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_coordination_item__participant_moment
        FOREIGN KEY (owner_participant_id, moment_id)
        REFERENCES collaboration.moment_participant(participant_id, moment_id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_coordination_item__status CHECK (status IN ('OPEN','IN_PROGRESS','DONE','CANCELLED'))
);

CREATE INDEX ix_group_moment_context__organizer_status
    ON collaboration.group_moment_context (organizer_user_id, status, updated_at DESC);
CREATE INDEX ix_group_moment_context__family_status
    ON collaboration.group_moment_context (group_family, status, updated_at DESC);
CREATE INDEX ix_moment_participant__user_moment_status
    ON collaboration.moment_participant (user_id, moment_id, status)
    WHERE user_id IS NOT NULL;
CREATE INDEX ix_moment_participant__moment_status
    ON collaboration.moment_participant (moment_id, status, updated_at DESC);
CREATE INDEX ix_planning_item__moment_status_due
    ON collaboration.planning_item (moment_id, status, due_at);
CREATE INDEX ix_booking__moment_status_start
    ON collaboration.booking (moment_id, status, start_at);
CREATE INDEX ix_group_update__moment_created
    ON collaboration.group_update (moment_id, created_at DESC);
CREATE INDEX ix_poll__moment_status
    ON collaboration.poll (moment_id, status, created_at DESC);
CREATE INDEX ix_purchase_item__moment_status
    ON collaboration.purchase_item (moment_id, status, updated_at DESC);
CREATE INDEX ix_resident__moment_status
    ON collaboration.resident (moment_id, status);
CREATE INDEX ix_shared_asset__moment_status
    ON collaboration.shared_asset (moment_id, status);
CREATE INDEX ix_maintenance_record__moment_status
    ON collaboration.maintenance_record (moment_id, status, scheduled_at);
CREATE INDEX ix_coordination_item__moment_status_due
    ON collaboration.coordination_item (moment_id, status, due_at);

COMMIT;
