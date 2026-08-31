BEGIN;

-- Final query-path indexes that are intentionally deferred until the complete structural schema exists.
-- These complement, rather than duplicate, domain-local indexes from V002-V014.

CREATE INDEX ix_financial_account__owner_user_status
    ON finance.financial_account (owner_user_id, status, updated_at DESC)
    WHERE owner_user_id IS NOT NULL;
CREATE INDEX ix_financial_account__owner_company_status
    ON finance.financial_account (owner_company_id, status, updated_at DESC)
    WHERE owner_company_id IS NOT NULL;

CREATE INDEX ix_expense__moment_effective
    ON finance.expense (moment_id, effective_at DESC, status);
CREATE INDEX ix_group_expense_context__moment_expense
    ON finance.group_expense_context (moment_id, expense_id);
CREATE INDEX ix_business_expense_context__company_expense
    ON finance.business_expense_context (company_id, expense_id);
CREATE INDEX ix_participant_obligation__moment_participant_status
    ON finance.participant_obligation (moment_id, participant_id, status, updated_at DESC);
CREATE INDEX ix_settlement__moment_status_time
    ON finance.settlement (moment_id, status, settled_at DESC);


CREATE INDEX ix_personal_moment_context__user_moment
    ON personal.personal_moment_context (user_id, moment_id);


CREATE INDEX ix_memory__user_scope
    ON memory.memory (scope_id, status, updated_at DESC) WHERE scope_type='USER';
CREATE INDEX ix_memory__company_scope
    ON memory.memory (scope_id, status, updated_at DESC) WHERE scope_type='COMPANY';

CREATE INDEX ix_audit_record__scope_time
    ON audit.audit_record (scope_type, scope_id, occurred_at DESC) WHERE scope_type IS NOT NULL;
CREATE INDEX ix_domain_event__name_time
    ON events.domain_event (event_name, occurred_at DESC);
CREATE INDEX ix_outbox_event__topic_status_available
    ON events.outbox_event (topic_code, status, available_at);

CREATE INDEX ix_ai_insight__inference
    ON ai.ai_insight (inference_run_id, created_at DESC);
CREATE INDEX ix_action_proposal__target_status
    ON ai.action_proposal (target_resource_type, target_resource_id, status, updated_at DESC)
    WHERE target_resource_id IS NOT NULL;

COMMIT;
