BEGIN;

-- First-party client telemetry (screen time, widgets, demographics) — separate from Firebase Analytics.
CREATE TABLE analytics.client_session (
    client_session_id UUID PRIMARY KEY,
    anonymous_id UUID NOT NULL,
    user_id UUID,
    platform TEXT NOT NULL,
    app_version TEXT,
    device_model TEXT,
    started_at TIMESTAMPTZ NOT NULL,
    ended_at TIMESTAMPTZ,
    user_snapshot JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_client_session__user FOREIGN KEY (user_id) REFERENCES core.user_profile(user_id) ON DELETE SET NULL,
    CONSTRAINT ck_client_session__platform CHECK (platform IN ('android', 'ios', 'web'))
);

CREATE TABLE analytics.client_event (
    client_event_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_session_id UUID NOT NULL,
    anonymous_id UUID NOT NULL,
    user_id UUID,
    event_name TEXT NOT NULL,
    screen_name TEXT,
    widget_name TEXT,
    properties JSONB NOT NULL DEFAULT '{}'::jsonb,
    client_occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_client_event__session FOREIGN KEY (client_session_id) REFERENCES analytics.client_session(client_session_id) ON DELETE CASCADE,
    CONSTRAINT fk_client_event__user FOREIGN KEY (user_id) REFERENCES core.user_profile(user_id) ON DELETE SET NULL,
    CONSTRAINT ck_client_event__name CHECK (event_name ~ '^[a-z][a-z0-9_]*$')
);

CREATE INDEX ix_client_event__user_time ON analytics.client_event (user_id, client_occurred_at DESC);
CREATE INDEX ix_client_event__anonymous_time ON analytics.client_event (anonymous_id, client_occurred_at DESC);
CREATE INDEX ix_client_event__session_time ON analytics.client_event (client_session_id, client_occurred_at);
CREATE INDEX ix_client_event__screen_event ON analytics.client_event (screen_name, event_name, client_occurred_at DESC);
CREATE INDEX ix_client_session__user_started ON analytics.client_session (user_id, started_at DESC);

GRANT INSERT, SELECT, UPDATE ON analytics.client_session, analytics.client_event TO momentra_app;

COMMENT ON TABLE analytics.client_session IS 'App foreground sessions with optional user demographics snapshot.';
COMMENT ON TABLE analytics.client_event IS 'Batched first-party telemetry events (screen_enter, screen_tick, widget_interaction, etc.).';

COMMIT;
