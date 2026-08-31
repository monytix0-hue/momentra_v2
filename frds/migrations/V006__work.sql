BEGIN;

CREATE SCHEMA work;
COMMENT ON SCHEMA work IS 'Momentra shared Work domain: cross-domain goals, milestones, tasks, assignments and dependencies.';

CREATE TABLE work.goal (
    goal_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    moment_id UUID NOT NULL,
    domain_code TEXT NOT NULL,
    title TEXT NOT NULL,
    description TEXT,
    owner_user_id UUID,
    target_date DATE,
    progress_percent NUMERIC(7,4) NOT NULL DEFAULT 0,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_goal__moment_domain
        FOREIGN KEY (moment_id, domain_code)
        REFERENCES core.moment(moment_id, domain_code)
        ON DELETE RESTRICT,
    CONSTRAINT fk_goal__owner_user
        FOREIGN KEY (owner_user_id)
        REFERENCES core.user_profile(user_id)
        ON DELETE RESTRICT,
    CONSTRAINT uq_goal__id_moment UNIQUE (goal_id, moment_id),
    CONSTRAINT ck_goal__domain CHECK (domain_code IN ('PERSONAL','GROUP','BUSINESS')),
    CONSTRAINT ck_goal__progress CHECK (progress_percent >= 0 AND progress_percent <= 100),
    CONSTRAINT ck_goal__status CHECK (status IN ('DRAFT','ACTIVE','ON_HOLD','COMPLETED','CANCELLED','ARCHIVED')),
    CONSTRAINT ck_goal__version CHECK (version > 0)
);

CREATE TABLE work.milestone (
    milestone_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    goal_id UUID NOT NULL,
    moment_id UUID NOT NULL,
    title TEXT NOT NULL,
    description TEXT,
    target_date DATE,
    completed_at TIMESTAMPTZ,
    progress_percent NUMERIC(7,4) NOT NULL DEFAULT 0,
    status TEXT NOT NULL DEFAULT 'PLANNED',
    sort_order INTEGER NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_milestone__goal_moment
        FOREIGN KEY (goal_id, moment_id)
        REFERENCES work.goal(goal_id, moment_id)
        ON DELETE RESTRICT,
    CONSTRAINT uq_milestone__id_moment UNIQUE (milestone_id, moment_id),
    CONSTRAINT uq_milestone__id_moment_goal UNIQUE (milestone_id, moment_id, goal_id),
    CONSTRAINT ck_milestone__progress CHECK (progress_percent >= 0 AND progress_percent <= 100),
    CONSTRAINT ck_milestone__sort CHECK (sort_order >= 0),
    CONSTRAINT ck_milestone__status CHECK (status IN ('PLANNED','ACTIVE','BLOCKED','COMPLETED','CANCELLED','ARCHIVED')),
    CONSTRAINT ck_milestone__completed CHECK (status <> 'COMPLETED' OR completed_at IS NOT NULL),
    CONSTRAINT ck_milestone__version CHECK (version > 0)
);

CREATE TABLE work.task (
    task_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    moment_id UUID NOT NULL,
    domain_code TEXT NOT NULL,
    goal_id UUID,
    milestone_id UUID,
    title TEXT NOT NULL,
    description TEXT,
    priority TEXT NOT NULL DEFAULT 'MEDIUM',
    due_at TIMESTAMPTZ,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    status TEXT NOT NULL DEFAULT 'OPEN',
    version BIGINT NOT NULL DEFAULT 1,
    created_by_user_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_task__moment_domain
        FOREIGN KEY (moment_id, domain_code)
        REFERENCES core.moment(moment_id, domain_code)
        ON DELETE RESTRICT,
    CONSTRAINT fk_task__goal_moment
        FOREIGN KEY (goal_id, moment_id)
        REFERENCES work.goal(goal_id, moment_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_task__milestone_goal_moment
        FOREIGN KEY (milestone_id, moment_id, goal_id)
        REFERENCES work.milestone(milestone_id, moment_id, goal_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_task__created_by
        FOREIGN KEY (created_by_user_id)
        REFERENCES core.user_profile(user_id)
        ON DELETE RESTRICT,
    CONSTRAINT uq_task__id_moment UNIQUE (task_id, moment_id),
    CONSTRAINT ck_task__domain CHECK (domain_code IN ('PERSONAL','GROUP','BUSINESS')),
    CONSTRAINT ck_task__milestone_requires_goal CHECK (milestone_id IS NULL OR goal_id IS NOT NULL),
    CONSTRAINT ck_task__priority CHECK (priority IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    CONSTRAINT ck_task__status CHECK (status IN ('OPEN','IN_PROGRESS','BLOCKED','COMPLETED','CANCELLED','ARCHIVED')),
    CONSTRAINT ck_task__completed CHECK (status <> 'COMPLETED' OR completed_at IS NOT NULL),
    CONSTRAINT ck_task__version CHECK (version > 0)
);

CREATE TABLE work.assignment (
    assignment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL,
    moment_id UUID NOT NULL,
    assignee_type TEXT NOT NULL,
    assignee_user_id UUID,
    assignee_participant_id UUID,
    assignee_company_membership_id UUID,
    assignee_team_id UUID,
    company_id UUID,
    assignment_role TEXT NOT NULL DEFAULT 'OWNER',
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_assignment__task_moment
        FOREIGN KEY (task_id, moment_id)
        REFERENCES work.task(task_id, moment_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_assignment__user
        FOREIGN KEY (assignee_user_id)
        REFERENCES core.user_profile(user_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_assignment__participant_moment
        FOREIGN KEY (assignee_participant_id, moment_id)
        REFERENCES collaboration.moment_participant(participant_id, moment_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_assignment__company_membership
        FOREIGN KEY (assignee_company_membership_id, company_id)
        REFERENCES business.company_membership(company_membership_id, company_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_assignment__team_company
        FOREIGN KEY (assignee_team_id, company_id)
        REFERENCES business.team(team_id, company_id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_assignment__type CHECK (assignee_type IN ('USER','PARTICIPANT','COMPANY_MEMBER','TEAM')),
    CONSTRAINT ck_assignment__exact_assignee CHECK (
        (assignee_type = 'USER' AND assignee_user_id IS NOT NULL AND assignee_participant_id IS NULL AND assignee_company_membership_id IS NULL AND assignee_team_id IS NULL AND company_id IS NULL)
        OR
        (assignee_type = 'PARTICIPANT' AND assignee_user_id IS NULL AND assignee_participant_id IS NOT NULL AND assignee_company_membership_id IS NULL AND assignee_team_id IS NULL AND company_id IS NULL)
        OR
        (assignee_type = 'COMPANY_MEMBER' AND assignee_user_id IS NULL AND assignee_participant_id IS NULL AND assignee_company_membership_id IS NOT NULL AND assignee_team_id IS NULL AND company_id IS NOT NULL)
        OR
        (assignee_type = 'TEAM' AND assignee_user_id IS NULL AND assignee_participant_id IS NULL AND assignee_company_membership_id IS NULL AND assignee_team_id IS NOT NULL AND company_id IS NOT NULL)
    ),
    CONSTRAINT ck_assignment__role CHECK (assignment_role IN ('OWNER','ASSIGNEE','CONTRIBUTOR','REVIEWER','OBSERVER')),
    CONSTRAINT ck_assignment__status CHECK (status IN ('ACTIVE','COMPLETED','CANCELLED','REMOVED')),
    CONSTRAINT ck_assignment__completed CHECK (status <> 'COMPLETED' OR completed_at IS NOT NULL)
);

CREATE TABLE work.task_dependency (
    task_dependency_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL,
    depends_on_task_id UUID NOT NULL,
    dependency_type TEXT NOT NULL DEFAULT 'BLOCKS',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_task_dependency__task
        FOREIGN KEY (task_id)
        REFERENCES work.task(task_id)
        ON DELETE RESTRICT,
    CONSTRAINT fk_task_dependency__depends_on
        FOREIGN KEY (depends_on_task_id)
        REFERENCES work.task(task_id)
        ON DELETE RESTRICT,
    CONSTRAINT uq_task_dependency__pair UNIQUE (task_id, depends_on_task_id),
    CONSTRAINT ck_task_dependency__self CHECK (task_id <> depends_on_task_id),
    CONSTRAINT ck_task_dependency__type CHECK (dependency_type IN ('BLOCKS','STARTS_AFTER','FINISHES_AFTER','RELATED'))
);

CREATE INDEX ix_goal__moment_status ON work.goal (moment_id, status, updated_at DESC);
CREATE INDEX ix_goal__owner_status ON work.goal (owner_user_id, status, target_date) WHERE owner_user_id IS NOT NULL;
CREATE INDEX ix_milestone__goal_status ON work.milestone (goal_id, status, sort_order);
CREATE INDEX ix_task__moment_status_due ON work.task (moment_id, status, due_at);
CREATE INDEX ix_task__goal_status ON work.task (goal_id, status, due_at) WHERE goal_id IS NOT NULL;
CREATE INDEX ix_task__milestone_status ON work.task (milestone_id, status, due_at) WHERE milestone_id IS NOT NULL;
CREATE INDEX ix_assignment__task_status ON work.assignment (task_id, status);
CREATE INDEX ix_assignment__user_status ON work.assignment (assignee_user_id, status, updated_at DESC) WHERE assignee_user_id IS NOT NULL;
CREATE INDEX ix_assignment__participant_status ON work.assignment (assignee_participant_id, status) WHERE assignee_participant_id IS NOT NULL;
CREATE INDEX ix_assignment__company_member_status ON work.assignment (company_id, assignee_company_membership_id, status) WHERE assignee_company_membership_id IS NOT NULL;
CREATE INDEX ix_assignment__team_status ON work.assignment (company_id, assignee_team_id, status) WHERE assignee_team_id IS NOT NULL;
CREATE INDEX ix_task_dependency__depends_on ON work.task_dependency (depends_on_task_id, task_id);

COMMIT;
