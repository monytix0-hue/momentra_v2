BEGIN;

-- Internal group roles. Login credentials are provisioned outside SQL and granted membership in these roles.
DO $$
DECLARE r TEXT;
BEGIN
    FOREACH r IN ARRAY ARRAY[
        'momentra_app',
        'momentra_outbox_worker',
        'momentra_analytics_worker',
        'momentra_memory_worker',
        'momentra_ai_worker',
        'momentra_projection_worker'
    ]
    LOOP
        IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname=r) THEN
            EXECUTE format('CREATE ROLE %I NOLOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT',r);
        END IF;
    END LOOP;
END $$;

-- Security helpers: authenticated users may execute only bounded helper functions; internal roles inherit the same.
GRANT USAGE ON SCHEMA security TO momentra_app, momentra_analytics_worker, momentra_memory_worker, momentra_projection_worker;
GRANT EXECUTE ON FUNCTION security.current_user_id() TO momentra_app, momentra_analytics_worker, momentra_memory_worker, momentra_projection_worker;
GRANT EXECUTE ON FUNCTION security.has_database_role(TEXT) TO momentra_app, momentra_analytics_worker, momentra_memory_worker, momentra_projection_worker;
GRANT EXECUTE ON FUNCTION security.is_backend_app() TO momentra_app, momentra_analytics_worker, momentra_memory_worker, momentra_projection_worker;
GRANT EXECUTE ON FUNCTION security.is_analytics_worker() TO momentra_app, momentra_analytics_worker, momentra_memory_worker, momentra_projection_worker;
GRANT EXECUTE ON FUNCTION security.is_memory_worker() TO momentra_app, momentra_analytics_worker, momentra_memory_worker, momentra_projection_worker;
GRANT EXECUTE ON FUNCTION security.is_projection_worker() TO momentra_app, momentra_analytics_worker, momentra_memory_worker, momentra_projection_worker;
GRANT EXECUTE ON FUNCTION security.owns_personal_moment(UUID) TO momentra_app, momentra_analytics_worker, momentra_memory_worker, momentra_projection_worker;
GRANT EXECUTE ON FUNCTION security.is_active_group_participant(UUID) TO momentra_app, momentra_analytics_worker, momentra_memory_worker, momentra_projection_worker;
GRANT EXECUTE ON FUNCTION security.is_active_company_member(UUID) TO momentra_app, momentra_analytics_worker, momentra_memory_worker, momentra_projection_worker;
GRANT EXECUTE ON FUNCTION security.can_access_moment(UUID) TO momentra_app, momentra_analytics_worker, momentra_memory_worker, momentra_projection_worker;
GRANT EXECUTE ON FUNCTION security.can_access_scope(TEXT,UUID) TO momentra_app, momentra_analytics_worker, momentra_memory_worker, momentra_projection_worker;

-- Backend application: DML but never DDL. RLS backend policies permit this group role.
GRANT USAGE ON SCHEMA core, personal, collaboration, business, work, finance, governance, analytics, memory, events, audit, platform, ai, projection TO momentra_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA core, personal, collaboration, business, work, finance, governance, memory, ai TO momentra_app;
GRANT SELECT ON ALL TABLES IN SCHEMA analytics, projection TO momentra_app;
GRANT SELECT, INSERT ON events.domain_event, events.outbox_event TO momentra_app;
GRANT SELECT, INSERT ON audit.audit_record TO momentra_app;
GRANT SELECT, INSERT, UPDATE ON platform.idempotency_record TO momentra_app;
GRANT SELECT, INSERT, UPDATE ON platform.job_execution, platform.processing_checkpoint TO momentra_app;

-- Outbox worker: technical event delivery only.
GRANT USAGE ON SCHEMA events, platform TO momentra_outbox_worker;
GRANT SELECT, UPDATE ON events.outbox_event TO momentra_outbox_worker;
GRANT SELECT ON events.domain_event TO momentra_outbox_worker;
GRANT INSERT ON events.event_delivery_attempt TO momentra_outbox_worker;
GRANT SELECT, INSERT, UPDATE ON events.dead_letter_event TO momentra_outbox_worker;
GRANT SELECT, INSERT, UPDATE ON platform.job_execution TO momentra_outbox_worker;

-- Analytics worker: reads canonical inputs, writes analytics and event/outbox results.
GRANT USAGE ON SCHEMA core, personal, collaboration, business, work, finance, governance, analytics, events, platform TO momentra_analytics_worker;
GRANT SELECT ON ALL TABLES IN SCHEMA core, personal, collaboration, business, work, finance, governance TO momentra_analytics_worker;
GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA analytics TO momentra_analytics_worker;
GRANT SELECT, INSERT ON events.domain_event, events.outbox_event TO momentra_analytics_worker;
GRANT SELECT, INSERT, UPDATE ON events.event_consumer_state TO momentra_analytics_worker;
GRANT SELECT, INSERT, UPDATE ON platform.job_execution, platform.processing_checkpoint TO momentra_analytics_worker;

-- Memory worker: reads governed evidence, writes Memory and event/outbox results.
GRANT USAGE ON SCHEMA core, personal, collaboration, business, work, finance, governance, analytics, memory, events, platform TO momentra_memory_worker;
GRANT SELECT ON ALL TABLES IN SCHEMA core, personal, collaboration, business, work, finance, governance, analytics TO momentra_memory_worker;
GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA memory TO momentra_memory_worker;
GRANT SELECT, INSERT ON events.domain_event, events.outbox_event TO momentra_memory_worker;
GRANT SELECT, INSERT, UPDATE ON events.event_consumer_state TO momentra_memory_worker;
GRANT SELECT, INSERT, UPDATE ON platform.job_execution, platform.processing_checkpoint TO momentra_memory_worker;

-- AI worker: intentionally does not receive canonical domain mutation rights.
GRANT USAGE ON SCHEMA ai, governance, events, platform TO momentra_ai_worker;
GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA ai TO momentra_ai_worker;
GRANT SELECT ON governance.consent, governance.consent_purpose, governance.data_category, governance.approval_request, governance.approval_step, governance.approval_decision TO momentra_ai_worker;
GRANT SELECT, INSERT ON events.domain_event, events.outbox_event TO momentra_ai_worker;
GRANT SELECT, INSERT, UPDATE ON events.event_consumer_state TO momentra_ai_worker;
GRANT SELECT, INSERT, UPDATE ON platform.job_execution, platform.processing_checkpoint TO momentra_ai_worker;

-- Projection worker: reads authoritative state and writes only projection/event-consumer state.
GRANT USAGE ON SCHEMA core, personal, collaboration, business, work, finance, governance, analytics, memory, ai, events, projection, platform TO momentra_projection_worker;
GRANT SELECT ON ALL TABLES IN SCHEMA core, personal, collaboration, business, work, finance, governance, analytics, memory, ai, events TO momentra_projection_worker;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA projection TO momentra_projection_worker;
GRANT SELECT, INSERT, UPDATE ON events.event_consumer_state TO momentra_projection_worker;
GRANT SELECT, INSERT, UPDATE ON platform.job_execution, platform.processing_checkpoint TO momentra_projection_worker;

-- Ordinary Supabase roles are optional in clean PostgreSQL. Apply direct-client grants only when they exist.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname='authenticated') THEN
        GRANT USAGE ON SCHEMA core, projection, security TO authenticated;
        GRANT SELECT ON core.moment_category, core.moment_type, core.capability, core.moment_type_capability TO authenticated;
        GRANT SELECT ON
            projection.moment_summary,
            projection.personal_pulse,
            projection.personal_moments,
            projection.personal_life,
            projection.personal_memory,
            projection.personal_finance_snapshot,
            projection.group_pulse,
            projection.group_moments,
            projection.group_life,
            projection.group_memory,
            projection.group_finance_snapshot,
            projection.group_finance_position,
            projection.business_pulse,
            projection.business_moments,
            projection.business_life,
            projection.business_memory,
            projection.business_finance_snapshot,
            projection.available_action,
            projection.pending_approval_summary,
            projection.attention_summary,
            projection.user_company_access,
            projection.recent_activity,
            projection.life360
        TO authenticated;
        GRANT EXECUTE ON FUNCTION security.current_user_id() TO authenticated;
        GRANT EXECUTE ON FUNCTION security.is_backend_app() TO authenticated;
        GRANT EXECUTE ON FUNCTION security.is_analytics_worker() TO authenticated;
        GRANT EXECUTE ON FUNCTION security.is_memory_worker() TO authenticated;
        GRANT EXECUTE ON FUNCTION security.is_projection_worker() TO authenticated;
        GRANT EXECUTE ON FUNCTION security.owns_personal_moment(UUID) TO authenticated;
        GRANT EXECUTE ON FUNCTION security.is_active_group_participant(UUID) TO authenticated;
        GRANT EXECUTE ON FUNCTION security.is_active_company_member(UUID) TO authenticated;
        GRANT EXECUTE ON FUNCTION security.can_access_moment(UUID) TO authenticated;
        GRANT EXECUTE ON FUNCTION security.can_access_scope(TEXT,UUID) TO authenticated;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname='anon') THEN
        GRANT USAGE ON SCHEMA core TO anon;
        GRANT SELECT ON core.moment_category, core.moment_type, core.capability, core.moment_type_capability TO anon;
    END IF;
END $$;

-- Explicitly keep internal schemas away from ordinary clients when Supabase roles exist.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname='authenticated') THEN
        REVOKE ALL ON ALL TABLES IN SCHEMA events, audit, platform, analytics, ai, governance FROM authenticated;
    END IF;
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname='anon') THEN
        REVOKE ALL ON ALL TABLES IN SCHEMA events, audit, platform, analytics, ai, governance, personal, collaboration, business, work, finance, memory, projection FROM anon;
    END IF;
END $$;

-- Future objects do not inherit broad client privileges from this migration.
REVOKE CREATE ON SCHEMA core, personal, collaboration, business, work, finance, governance, analytics, memory, events, audit, platform, ai, projection, security FROM PUBLIC;

COMMIT;
