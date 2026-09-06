# Momentra product-grouped SQL catalog

Browse map for Supabase Table Editor and repo navigation.

**Apply order** is the global V-sequence in [`manifest/MIGRATION_ORDER.txt`](../manifest/MIGRATION_ORDER.txt) (V001–V075+).  
**On-disk layout** groups files under `migrations/{personal,group,business,shared}/` for humans; the migrate runner resolves by **basename** only (ledger unchanged).

Phase 11.9 checksums cover baseline **V001–V030** only. Forward pack is V031+ via the order file.

## Migration file buckets

### personal (`migrations/personal/`)

- `V003__personal.sql`
- `V025__rls_personal.sql`
- `V036__personal_life_system_setup.sql`
- `V042__personal_life_operations_setup_v2.sql`
- `V044__personal_life_operations_observation_details_v2.sql`
- `V045__finance_personal_ui_gap_closure_v2.sql`
- `V046__personal_family_precision_profiles.sql`
- `V050__personal_movement_record_capability.sql`
- `V058__personal_phase7_pulse_metric_freeze.sql`

### group (`migrations/group/`)

- `V004__collaboration.sql`
- `V026__rls_group.sql`
- `V049__group_vendor_capability.sql`
- `V056__shared_living_participant_manage.sql`
- `V072__shared_experience_places_multi_currency_draft.sql`

### business (`migrations/business/`)

- `V005__business.sql`
- `V027__rls_business.sql`
- `V031__company_location_custom_label.sql`
- `V037__business_system_setup.sql`
- `V051__business_ops_improvement_capabilities.sql`
- `V052__company_invite.sql`
- `V055__business_life_api_parity.sql`
- `V075__business_expense_paid_by.sql`

### shared (`migrations/shared/`)

Platform, finance core, projection, seeds, analytics, notifications, and mixed-domain files (e.g. `V054`, `V057`, `V073`). New cross-cutting work lands here; product-primary work lands in the matching bucket as **V076+**.

---

## personal — tables

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

## group — tables

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

## business — tables

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

- `finance.business_expense_context` (source: V007) — `paid_by_label` from V075
- `finance.revenue` (source: V007)
- `finance.invoice` (source: V007)
- `finance.invoice_line` (source: V007)
- `finance.invoice_payment` (source: V007)
- `projection.business_finance_snapshot` (source: V014)

## shared — tables

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
