# Momentra product-grouped SQL catalog

Browse map for Supabase Table Editor, Node modules, and Prisma layout.
Apply order remains V001–V030 from `manifest/MIGRATION_ORDER.txt`.

## personal

### pulse

- `projection.personal_pulse` (source: V014)
- `projection.attention_summary` (source: V014)

### moments

- `core.moment` (source: V002)
- `personal.personal_moment_context` (source: V003)
- `projection.personal_moments` (source: V014)
- `projection.moment_summary` (source: V014)

### life

- `personal.life_operation_observation` (source: V003)
- `personal.future_opportunity` (source: V003)
- `personal.future_pivot` (source: V003)
- `personal.future_learning_activity` (source: V003)
- `personal.future_progress_observation` (source: V003)
- `personal.lifestyle_activity` (source: V003)
- `personal.relationship_connection` (source: V003)
- `personal.relationship_activity` (source: V003)
- `projection.personal_life` (source: V014)

### memory

- `memory.memory` (source: V010)
- `memory.memory_evidence` (source: V010)
- `memory.pattern` (source: V010)
- `memory.pattern_occurrence` (source: V010)
- `memory.learning` (source: V010)
- `memory.learning_evidence` (source: V010)
- `memory.playbook` (source: V010)
- `memory.playbook_version` (source: V010)
- `memory.playbook_evidence` (source: V010)
- `projection.personal_memory` (source: V014)

### finance

- `finance.expense` (source: V007)
- `finance.personal_expense_context` (source: V007)
- `finance.financial_account` (source: V007)
- `finance.financial_movement` (source: V007)
- `finance.budget` (source: V007)
- `finance.budget_revision` (source: V007)
- `projection.personal_finance_snapshot` (source: V014)

## group

### pulse

- `projection.group_pulse` (source: V014)
- `projection.group_finance_position` (source: V014)

### moments

- `collaboration.group_moment_context` (source: V004)
- `collaboration.moment_participant` (source: V004)
- `projection.group_moments` (source: V014)

### life

- `collaboration.shared_experience_context` (source: V004)
- `collaboration.planning_item` (source: V004)
- `collaboration.booking` (source: V004)
- `collaboration.shared_purchase_context` (source: V004)
- `collaboration.shared_living_context` (source: V004)
- `collaboration.shared_goal_context` (source: V004)
- `collaboration.community_coordination_context` (source: V004)
- `projection.group_life` (source: V014)

### memory

- `projection.group_memory` (source: V014)

### finance

- `finance.group_expense_context` (source: V007)
- `finance.expense_share` (source: V007)
- `finance.contribution` (source: V007)
- `finance.participant_obligation` (source: V007)
- `finance.settlement` (source: V007)
- `finance.settlement_allocation` (source: V007)
- `projection.group_finance_snapshot` (source: V014)

## business

### pulse

- `projection.business_pulse` (source: V014)

### moments

- `business.business_moment_context` (source: V005)
- `business.company` (source: V005)
- `business.company_membership` (source: V005)
- `projection.business_moments` (source: V014)

### life

- `business.team` (source: V005)
- `business.vendor` (source: V005)
- `business.issue` (source: V005)
- `business.risk` (source: V005)
- `business.decision` (source: V005)
- `projection.business_life` (source: V014)

### memory

- `projection.business_memory` (source: V014)

### finance

- `finance.business_expense_context` (source: V007)
- `finance.revenue` (source: V007)
- `finance.invoice` (source: V007)
- `finance.invoice_line` (source: V007)
- `finance.invoice_payment` (source: V007)
- `projection.business_finance_snapshot` (source: V014)

## shared

### work

- `work.goal` (source: V006)
- `work.milestone` (source: V006)
- `work.task` (source: V006)
- `work.assignment` (source: V006)
- `work.task_dependency` (source: V006)

### governance

- `governance.permission` (source: V008)
- `governance.role` (source: V008)
- `governance.role_permission` (source: V008)
- `governance.role_assignment` (source: V008)
- `governance.consent_purpose` (source: V008)
- `governance.data_category` (source: V008)
- `governance.consent` (source: V008)
- `governance.policy` (source: V008)
- `governance.policy_version` (source: V008)
- `governance.approval_request` (source: V008)
- `governance.approval_step` (source: V008)
- `governance.approval_decision` (source: V008)

### platform

- `events.domain_event` (source: V011)
- `events.outbox_event` (source: V011)
- `events.event_consumer_state` (source: V011)
- `events.event_delivery_attempt` (source: V011)
- `events.dead_letter_event` (source: V011)
- `audit.audit_record` (source: V012)
- `platform.idempotency_record` (source: V012)
- `platform.distributed_lock` (source: V012)
- `platform.job_execution` (source: V012)
- `platform.processing_checkpoint` (source: V012)
- `security.*` (source: V024)

### ai

- `ai.context_session` (source: V013)
- `ai.context_item` (source: V013)
- `ai.inference_run` (source: V013)
- `ai.ai_insight` (source: V013)
- `ai.recommendation` (source: V013)
- `ai.action_proposal` (source: V013)
- `ai.action_proposal_parameter` (source: V013)
- `ai.provenance` (source: V013)

### shell

- `projection.life360` (source: V014)
- `projection.available_action` (source: V014)
- `projection.pending_approval_summary` (source: V014)
- `projection.recent_activity` (source: V014)
- `projection.projection_state` (source: V014)
- `projection.user_company_access` (source: V014)
- `core.user_profile` (source: V002)
- `core.moment_category` (source: V002)
- `core.moment_type` (source: V002)
- `core.capability` (source: V002)
- `core.moment_type_capability` (source: V002)
- `analytics.metric_definition` (source: V009)
- `analytics.attention_item` (source: V009)
