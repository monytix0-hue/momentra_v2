BEGIN;

CREATE TABLE IF NOT EXISTS analytics_core.event_registry (
  event_name VARCHAR(100) NOT NULL,
  event_version SMALLINT NOT NULL,
  event_family VARCHAR(40) NOT NULL,
  authority VARCHAR(30) NOT NULL,
  priority VARCHAR(5) NOT NULL CHECK (priority IN ('P0','P1','P2')),
  is_meaningful BOOLEAN NOT NULL DEFAULT FALSE,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  description TEXT NOT NULL,
  introduced_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  retired_at TIMESTAMPTZ NULL,
  PRIMARY KEY (event_name, event_version)
);

CREATE TABLE IF NOT EXISTS analytics_core.activity_type_registry (
  activity_type VARCHAR(100) PRIMARY KEY,
  is_meaningful BOOLEAN NOT NULL DEFAULT TRUE,
  moment_domain VARCHAR(20) NULL,
  feature_group VARCHAR(60) NULL,
  active_from TIMESTAMPTZ NOT NULL DEFAULT now(),
  active_to TIMESTAMPTZ NULL
);

INSERT INTO analytics_core.event_registry
  (event_name, event_version, event_family, authority, priority, is_meaningful, is_active, description)
VALUES
  ('acquisition_touch_recorded', 1, 'acquisition', 'Client/System', 'P0', FALSE, TRUE, 'Acquisition attribution touch'),
  ('user_registered', 1, 'identity', 'Backend/Auth', 'P0', FALSE, TRUE, 'Account successfully created'),
  ('user_onboarding_completed', 1, 'identity', 'Backend', 'P0', FALSE, TRUE, 'Official onboarding completion persisted'),
  ('session_started', 1, 'session_navigation', 'Client', 'P0', FALSE, TRUE, 'New analytics session'),
  ('screen_viewed', 1, 'session_navigation', 'Client', 'P0', FALSE, TRUE, 'Meaningful screen becomes visible'),
  ('critical_flow_started', 1, 'session_navigation', 'Client', 'P0', FALSE, TRUE, 'Designated critical flow begins'),
  ('moment_creation_started', 1, 'moment', 'Client', 'P0', FALSE, TRUE, 'Moment creation workflow begins'),
  ('moment_type_selected', 1, 'moment', 'Client', 'P0', FALSE, TRUE, 'Valid Moment type selection'),
  ('moment_creation_step_completed', 1, 'moment', 'Client', 'P0', FALSE, TRUE, 'Defined creation step passed'),
  ('moment_created', 1, 'moment', 'Backend', 'P0', TRUE, TRUE, 'Moment successfully persisted'),
  ('moment_viewed', 1, 'moment', 'Client', 'P0', FALSE, TRUE, 'Moment workspace/detail viewed'),
  ('moment_activated', 1, 'moment', 'Backend/System', 'P0', FALSE, TRUE, 'Moment reaches activation state'),
  ('moment_completed', 1, 'moment', 'Backend', 'P0', TRUE, TRUE, 'Moment reaches valid completed state'),
  ('moment_cancelled', 1, 'moment', 'Backend', 'P0', FALSE, TRUE, 'Moment explicitly cancelled'),
  ('participant_invited', 1, 'participation', 'Backend', 'P0', TRUE, TRUE, 'Valid invite issued'),
  ('invite_opened', 1, 'participation', 'Client/System', 'P0', FALSE, TRUE, 'Invite deep-link/token opened'),
  ('participant_joined', 1, 'participation', 'Backend', 'P0', TRUE, TRUE, 'Participant relationship becomes active'),
  ('participant_exited', 1, 'participation', 'Backend', 'P1', FALSE, TRUE, 'Participant left/removed/declined'),
  ('moment_activity_completed', 1, 'moment', 'Backend', 'P0', TRUE, TRUE, 'Generic meaningful Moment activity completed'),
  ('quick_add_started', 1, 'product', 'Client', 'P0', FALSE, TRUE, 'Quick Add flow started'),
  ('expense_added', 1, 'financial', 'Backend', 'P0', TRUE, TRUE, 'Expense persisted'),
  ('contribution_recorded', 1, 'financial', 'Backend', 'P0', TRUE, TRUE, 'Contribution persisted'),
  ('split_created', 1, 'financial', 'Backend', 'P1', TRUE, TRUE, 'Split persisted'),
  ('memory_created', 1, 'memory', 'Backend', 'P1', TRUE, TRUE, 'Memory persisted'),
  ('moment_reopened', 1, 'moment', 'Backend', 'P1', FALSE, TRUE, 'Operational lifecycle reopened'),
  ('critical_operation_failed', 1, 'reliability', 'System', 'P0', FALSE, TRUE, 'Critical operation failed'),
  ('app_crashed', 1, 'reliability', 'Observability', 'P0', FALSE, TRUE, 'App crash linked to session'),
  ('critical_flow_completed', 1, 'session_navigation', 'Client/Backend', 'P0', FALSE, TRUE, 'Critical flow completed successfully')
ON CONFLICT (event_name, event_version) DO UPDATE SET
  authority = EXCLUDED.authority,
  priority = EXCLUDED.priority,
  is_meaningful = EXCLUDED.is_meaningful,
  is_active = EXCLUDED.is_active,
  description = EXCLUDED.description;

INSERT INTO analytics_core.activity_type_registry (activity_type, is_meaningful, feature_group) VALUES
  ('poll_created', TRUE, 'poll'),
  ('poll_vote', TRUE, 'poll'),
  ('approval_requested', TRUE, 'approval'),
  ('approval_responded', TRUE, 'approval'),
  ('milestone_updated', TRUE, 'milestone'),
  ('activity_added', TRUE, 'activity'),
  ('team_update', TRUE, 'team'),
  ('issue_created', TRUE, 'issue'),
  ('risk_created', TRUE, 'risk'),
  ('note_added', TRUE, 'note'),
  ('quick_add_completed', TRUE, 'quick')
ON CONFLICT (activity_type) DO UPDATE SET
  is_meaningful = EXCLUDED.is_meaningful,
  feature_group = EXCLUDED.feature_group;

GRANT SELECT ON analytics_core.event_registry, analytics_core.activity_type_registry
  TO momentra_app, momentra_analytics_worker;

COMMIT;
