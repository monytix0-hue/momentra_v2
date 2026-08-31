-- Momentra Phase 11.6 / V030
-- Production Validation Pack
-- NON-DESTRUCTIVE: creates only session-local temporary validation state.
-- P0 and P1 findings fail the validation run.

BEGIN;

CREATE TEMP TABLE momentra_validation_result (
    validation_code TEXT PRIMARY KEY,
    severity TEXT NOT NULL CHECK (severity IN ('P0','P1','P2')),
    domain TEXT NOT NULL,
    description TEXT NOT NULL,
    violation_count BIGINT NOT NULL,
    details TEXT,
    passed BOOLEAN GENERATED ALWAYS AS (violation_count = 0) STORED
) ON COMMIT DROP;

-- -----------------------------------------------------------------------------
-- 01. STRUCTURE / MIGRATION SHAPE
-- -----------------------------------------------------------------------------
WITH required(schema_name) AS (
    VALUES
        ('core'),
        ('personal'),
        ('collaboration'),
        ('business'),
        ('work'),
        ('finance'),
        ('governance'),
        ('analytics'),
        ('memory'),
        ('events'),
        ('audit'),
        ('platform'),
        ('ai'),
        ('projection'),
        ('security')
), missing AS (
    SELECT r.schema_name FROM required r
    LEFT JOIN information_schema.schemata s ON s.schema_name=r.schema_name
    WHERE s.schema_name IS NULL
)
INSERT INTO momentra_validation_result
SELECT 'VAL-STRUCT-001','P0','STRUCTURE','All required Momentra schemas exist',COUNT(*),string_agg(schema_name, ', ' ORDER BY schema_name) FROM missing;

WITH required(qualified_name) AS (
    VALUES
        ('ai.action_proposal'),
        ('ai.action_proposal_parameter'),
        ('ai.ai_insight'),
        ('ai.context_item'),
        ('ai.context_session'),
        ('ai.inference_run'),
        ('ai.provenance'),
        ('ai.recommendation'),
        ('analytics.attention_item'),
        ('analytics.calculation_run'),
        ('analytics.deterministic_insight'),
        ('analytics.metric_current'),
        ('analytics.metric_definition'),
        ('analytics.metric_dependency'),
        ('analytics.metric_input_definition'),
        ('analytics.metric_observation'),
        ('analytics.metric_version'),
        ('analytics.threshold_definition'),
        ('audit.audit_record'),
        ('business.business_moment_context'),
        ('business.business_operations_context'),
        ('business.business_review'),
        ('business.business_runway_context'),
        ('business.business_update'),
        ('business.company'),
        ('business.company_membership'),
        ('business.decision'),
        ('business.events_operations_context'),
        ('business.issue'),
        ('business.risk'),
        ('business.sla_check'),
        ('business.sla_definition'),
        ('business.team'),
        ('business.team_membership'),
        ('business.team_operations_context'),
        ('business.vendor'),
        ('business.vendor_contract'),
        ('business.vendor_operations_context'),
        ('collaboration.attendance'),
        ('collaboration.booking'),
        ('collaboration.community_coordination_context'),
        ('collaboration.coordination_item'),
        ('collaboration.delivery_handover'),
        ('collaboration.group_moment_context'),
        ('collaboration.group_update'),
        ('collaboration.group_vendor'),
        ('collaboration.living_rule'),
        ('collaboration.maintenance_record'),
        ('collaboration.moment_participant'),
        ('collaboration.ownership_record'),
        ('collaboration.planning_item'),
        ('collaboration.poll'),
        ('collaboration.poll_option'),
        ('collaboration.poll_vote'),
        ('collaboration.purchase_item'),
        ('collaboration.resident'),
        ('collaboration.shared_asset'),
        ('collaboration.shared_experience_context'),
        ('collaboration.shared_goal_context'),
        ('collaboration.shared_living_context'),
        ('collaboration.shared_purchase_context'),
        ('core.capability'),
        ('core.external_party'),
        ('core.moment'),
        ('core.moment_category'),
        ('core.moment_type'),
        ('core.moment_type_capability'),
        ('core.user_profile'),
        ('events.dead_letter_event'),
        ('events.domain_event'),
        ('events.event_consumer_state'),
        ('events.event_delivery_attempt'),
        ('events.outbox_event'),
        ('finance.budget'),
        ('finance.budget_revision'),
        ('finance.business_expense_context'),
        ('finance.contribution'),
        ('finance.expense'),
        ('finance.expense_resource_link'),
        ('finance.expense_share'),
        ('finance.expense_split'),
        ('finance.financial_account'),
        ('finance.financial_movement'),
        ('finance.financial_movement_link'),
        ('finance.group_expense_context'),
        ('finance.invoice'),
        ('finance.invoice_line'),
        ('finance.invoice_payment'),
        ('finance.participant_obligation'),
        ('finance.personal_expense_context'),
        ('finance.revenue'),
        ('finance.settlement'),
        ('finance.settlement_allocation'),
        ('governance.approval_decision'),
        ('governance.approval_request'),
        ('governance.approval_step'),
        ('governance.consent'),
        ('governance.consent_purpose'),
        ('governance.data_category'),
        ('governance.permission'),
        ('governance.policy'),
        ('governance.policy_version'),
        ('governance.role'),
        ('governance.role_assignment'),
        ('governance.role_permission'),
        ('memory.learning'),
        ('memory.learning_evidence'),
        ('memory.memory'),
        ('memory.memory_evidence'),
        ('memory.pattern'),
        ('memory.pattern_occurrence'),
        ('memory.playbook'),
        ('memory.playbook_evidence'),
        ('memory.playbook_version'),
        ('personal.future_learning_activity'),
        ('personal.future_opportunity'),
        ('personal.future_pivot'),
        ('personal.future_progress_observation'),
        ('personal.life_operation_observation'),
        ('personal.lifestyle_activity'),
        ('personal.personal_moment_context'),
        ('personal.relationship_activity'),
        ('personal.relationship_connection'),
        ('platform.distributed_lock'),
        ('platform.idempotency_record'),
        ('platform.job_execution'),
        ('platform.processing_checkpoint'),
        ('projection.attention_summary'),
        ('projection.available_action'),
        ('projection.business_finance_snapshot'),
        ('projection.business_life'),
        ('projection.business_memory'),
        ('projection.business_moments'),
        ('projection.business_pulse'),
        ('projection.group_finance_position'),
        ('projection.group_finance_snapshot'),
        ('projection.group_life'),
        ('projection.group_memory'),
        ('projection.group_moments'),
        ('projection.group_pulse'),
        ('projection.moment_summary'),
        ('projection.pending_approval_summary'),
        ('projection.personal_finance_snapshot'),
        ('projection.personal_life'),
        ('projection.personal_memory'),
        ('projection.personal_moments'),
        ('projection.personal_pulse'),
        ('projection.projection_state'),
        ('projection.recent_activity'),
        ('projection.user_company_access'),
        ('work.assignment'),
        ('work.goal'),
        ('work.milestone'),
        ('work.task'),
        ('work.task_dependency')
), missing AS (
    SELECT qualified_name FROM required
    WHERE to_regclass(qualified_name) IS NULL
)
INSERT INTO momentra_validation_result
SELECT 'VAL-STRUCT-002','P0','STRUCTURE','All required V001-V029 tables exist',COUNT(*),string_agg(qualified_name, ', ' ORDER BY qualified_name) FROM missing;

WITH forbidden(qualified_name) AS (
    VALUES
        ('personal.expense'),
        ('personal.master_expense'),
        ('finance.master_expense'),
        ('collaboration.expense'),
        ('business.expense'),
        ('personal.task'),
        ('collaboration.task'),
        ('business.task'),
        ('personal.attention'),
        ('personal.memory'),
        ('collaboration.memory'),
        ('business.memory'),
        ('business.runway_metric'),
        ('ai.expense'),
        ('ai.task'),
        ('ai.goal'),
        ('ai.metric'),
        ('ai.memory'),
        ('projection.metric_formula')
), found AS (
    SELECT qualified_name FROM forbidden WHERE to_regclass(qualified_name) IS NOT NULL
)
INSERT INTO momentra_validation_result
SELECT 'VAL-STRUCT-003','P0','STRUCTURE','No forbidden duplicate-ownership tables exist',COUNT(*),string_agg(qualified_name, ', ' ORDER BY qualified_name) FROM found;

WITH required(constraint_name) AS (
    VALUES
        ('core.uq_moment__id_domain'),
        ('personal.uq_personal_moment_context__moment_user'),
        ('collaboration.uq_moment_participant__id_moment'),
        ('business.uq_team__id_company'),
        ('business.uq_business_moment_context__moment_company'),
        ('business.uq_vendor__id_company'),
        ('business.uq_vendor_contract__id_company_vendor'),
        ('collaboration.uq_moment_participant__id_moment_user')
), found AS (
    SELECT r.constraint_name, c.oid
    FROM required r
    LEFT JOIN pg_constraint c
      ON c.conname = split_part(r.constraint_name,'.',2)
     AND c.connamespace = to_regnamespace(split_part(r.constraint_name,'.',1))
), missing AS (SELECT constraint_name FROM found WHERE oid IS NULL)
INSERT INTO momentra_validation_result
SELECT 'VAL-STRUCT-004','P0','STRUCTURE','Critical composite ownership/tenant constraints exist',COUNT(*),string_agg(constraint_name, ', ' ORDER BY constraint_name) FROM missing;

INSERT INTO momentra_validation_result
SELECT 'VAL-STRUCT-005','P0','STRUCTURE','No unvalidated constraints remain in Momentra schemas',COUNT(*),string_agg(n.nspname||'.'||c.relname||'.'||con.conname, ', ' ORDER BY n.nspname,c.relname,con.conname)
FROM pg_constraint con JOIN pg_class c ON c.oid=con.conrelid JOIN pg_namespace n ON n.oid=c.relnamespace
WHERE n.nspname IN ('core','personal','collaboration','business','work','finance','governance','analytics','memory','events','audit','platform','ai','projection') AND con.convalidated=false;

-- -----------------------------------------------------------------------------
-- 02. TAXONOMY / SEED CONTRACTS
-- -----------------------------------------------------------------------------
WITH expected(code) AS (VALUES ('EXPERIENCE'),('WELLBEING'),('DISCOVERY'),('CREATION'),('LIFESTYLE')),
actual(code) AS (SELECT DISTINCT lifestyle_context FROM personal.lifestyle_activity WHERE false),
constraint_def AS (
  SELECT pg_get_constraintdef(oid) AS def FROM pg_constraint
  WHERE conname='ck_lifestyle_activity__context' AND connamespace=to_regnamespace('personal')
), missing AS (
  SELECT e.code FROM expected e, constraint_def c WHERE c.def NOT LIKE '%'||quote_literal(e.code)||'%'
)
INSERT INTO momentra_validation_result
SELECT 'VAL-TAX-001','P0','TAXONOMY','Lifestyle physical context constraint contains exactly the five frozen contexts',
       (SELECT COUNT(*) FROM missing) + CASE WHEN (SELECT COUNT(*) FROM constraint_def)=0 THEN 1 ELSE 0 END,
       (SELECT string_agg(code, ', ' ORDER BY code) FROM missing);

WITH expected(code) AS (VALUES
 ('TRIP'),('WEDDING'),('HOUSE_PARTY'),('OFFICE_OUTING'),
 ('GIFT_POOL'),('GROUP_PURCHASE'),('SHARED_ASSET'),('COMMUNITY_PURCHASE'),
 ('FLATMATES'),('CO_LIVING'),('SHARED_LIVING'),('COMMUNITY_LIVING')
), missing AS (
 SELECT e.code FROM expected e LEFT JOIN core.moment_type mt ON mt.domain_code='GROUP' AND mt.code=e.code WHERE mt.moment_type_id IS NULL
)
INSERT INTO momentra_validation_result
SELECT 'VAL-TAX-002','P0','TAXONOMY','Frozen Group Moment Types are seeded',COUNT(*),string_agg(code, ', ' ORDER BY code) FROM missing;

WITH expected(code) AS (VALUES ('TEAM_OPERATIONS'),('BUSINESS_RUNWAY'),('BUSINESS_OPERATIONS')), missing AS (
 SELECT e.code FROM expected e LEFT JOIN core.moment_type mt ON mt.domain_code='BUSINESS' AND mt.code=e.code WHERE mt.moment_type_id IS NULL
)
INSERT INTO momentra_validation_result
SELECT 'VAL-TAX-003','P0','TAXONOMY','Core Business Moment families are seeded',COUNT(*),string_agg(code, ', ' ORDER BY code) FROM missing;

INSERT INTO momentra_validation_result
SELECT 'VAL-CAP-001','P0','CAPABILITY','Every ACTIVE Moment Type has at least one ACTIVE capability mapping',COUNT(*),string_agg(mt.domain_code||':'||mt.code, ', ' ORDER BY mt.domain_code,mt.code)
FROM core.moment_type mt
WHERE mt.status='ACTIVE' AND NOT EXISTS (SELECT 1 FROM core.moment_type_capability mtc WHERE mtc.moment_type_id=mt.moment_type_id AND mtc.status='ACTIVE');

INSERT INTO momentra_validation_result
SELECT 'VAL-CAP-002','P0','CAPABILITY','Every active Moment-Type capability points to an ACTIVE capability',COUNT(*),NULL
FROM core.moment_type_capability mtc JOIN core.capability c ON c.capability_id=mtc.capability_id
WHERE mtc.status='ACTIVE' AND c.status<>'ACTIVE';

-- -----------------------------------------------------------------------------
-- 03. CANONICAL DOMAIN / TENANT INTEGRITY
-- -----------------------------------------------------------------------------
INSERT INTO momentra_validation_result
SELECT 'VAL-DOM-001','P0','PERSONAL','Personal Moment Context only references PERSONAL moments',COUNT(*),NULL
FROM personal.personal_moment_context pmc JOIN core.moment m ON m.moment_id=pmc.moment_id WHERE m.domain_code<>'PERSONAL';

INSERT INTO momentra_validation_result
SELECT 'VAL-DOM-002','P0','GROUP','Group Moment Context only references GROUP moments',COUNT(*),NULL
FROM collaboration.group_moment_context gmc JOIN core.moment m ON m.moment_id=gmc.moment_id WHERE m.domain_code<>'GROUP';

INSERT INTO momentra_validation_result
SELECT 'VAL-DOM-003','P0','BUSINESS','Business Moment Context only references BUSINESS moments',COUNT(*),NULL
FROM business.business_moment_context bmc JOIN core.moment m ON m.moment_id=bmc.moment_id WHERE m.domain_code<>'BUSINESS';

INSERT INTO momentra_validation_result
SELECT 'VAL-DOM-004','P0','BUSINESS','Business Moment team belongs to the same Company',COUNT(*),NULL
FROM business.business_moment_context bmc JOIN business.team t ON t.team_id=bmc.team_id WHERE bmc.team_id IS NOT NULL AND t.company_id<>bmc.company_id;

INSERT INTO momentra_validation_result
SELECT 'VAL-DOM-005','P0','BUSINESS','SLA checks preserve SLA/Vendor/Company identity',COUNT(*),NULL
FROM business.sla_check sc JOIN business.sla_definition sd ON sd.sla_definition_id=sc.sla_definition_id
WHERE sc.company_id<>sd.company_id OR sc.vendor_id<>sd.vendor_id;

-- -----------------------------------------------------------------------------
-- 04. WORK INTEGRITY
-- -----------------------------------------------------------------------------
INSERT INTO momentra_validation_result
SELECT 'VAL-WRK-001','P0','WORK','No Task depends on itself',COUNT(*),NULL FROM work.task_dependency WHERE task_id=depends_on_task_id;

WITH RECURSIVE walk(root_id,node_id,path,cycle) AS (
    SELECT td.task_id, td.depends_on_task_id, ARRAY[td.task_id,td.depends_on_task_id], td.task_id=td.depends_on_task_id
    FROM work.task_dependency td
    UNION ALL
    SELECT w.root_id, td.depends_on_task_id, w.path||td.depends_on_task_id, td.depends_on_task_id=ANY(w.path)
    FROM walk w JOIN work.task_dependency td ON td.task_id=w.node_id
    WHERE NOT w.cycle AND cardinality(w.path)<100
)
INSERT INTO momentra_validation_result
SELECT 'VAL-WRK-002','P0','WORK','Task dependency graph is acyclic',COUNT(*),NULL FROM walk WHERE cycle;

INSERT INTO momentra_validation_result
SELECT 'VAL-WRK-003','P0','WORK','Task milestone and goal identities agree',COUNT(*),NULL
FROM work.task t JOIN work.milestone m ON m.milestone_id=t.milestone_id
WHERE t.milestone_id IS NOT NULL AND t.goal_id IS DISTINCT FROM m.goal_id;

-- -----------------------------------------------------------------------------
-- 05. FINANCE INTEGRITY
-- -----------------------------------------------------------------------------
WITH x AS (
 SELECT e.expense_id,e.domain_code,
        (pec.expense_id IS NOT NULL)::int + (gec.expense_id IS NOT NULL)::int + (bec.expense_id IS NOT NULL)::int AS context_count,
        pec.expense_id AS p, gec.expense_id AS g, bec.expense_id AS b
 FROM finance.expense e
 LEFT JOIN finance.personal_expense_context pec ON pec.expense_id=e.expense_id
 LEFT JOIN finance.group_expense_context gec ON gec.expense_id=e.expense_id
 LEFT JOIN finance.business_expense_context bec ON bec.expense_id=e.expense_id
)
INSERT INTO momentra_validation_result
SELECT 'VAL-FIN-001','P0','FINANCE','Each Expense has exactly one matching Personal/Group/Business context',COUNT(*),NULL
FROM x WHERE context_count<>1 OR (domain_code='PERSONAL' AND p IS NULL) OR (domain_code='GROUP' AND g IS NULL) OR (domain_code='BUSINESS' AND b IS NULL);

INSERT INTO momentra_validation_result
SELECT 'VAL-FIN-002','P0','FINANCE','Personal Expense context user/moment ownership is consistent',COUNT(*),NULL
FROM finance.personal_expense_context pec
JOIN personal.personal_moment_context pmc ON pmc.moment_id=pec.moment_id
WHERE pmc.user_id<>pec.user_id;

INSERT INTO momentra_validation_result
SELECT 'VAL-FIN-003','P0','FINANCE','Group Expense shares use participants from the same Moment',COUNT(*),NULL
FROM finance.expense_share es JOIN collaboration.moment_participant mp ON mp.participant_id=es.participant_id
WHERE mp.moment_id<>es.moment_id;

INSERT INTO momentra_validation_result
SELECT 'VAL-FIN-004','P0','FINANCE','Participant obligations use participants from the same Moment',COUNT(*),NULL
FROM finance.participant_obligation po JOIN collaboration.moment_participant mp ON mp.participant_id=po.participant_id
WHERE mp.moment_id<>po.moment_id;

INSERT INTO momentra_validation_result
SELECT 'VAL-FIN-005','P0','FINANCE','Settlement parties belong to the settlement Moment',COUNT(*),NULL
FROM finance.settlement s
JOIN collaboration.moment_participant p1 ON p1.participant_id=s.payer_participant_id
JOIN collaboration.moment_participant p2 ON p2.participant_id=s.payee_participant_id
WHERE p1.moment_id<>s.moment_id OR p2.moment_id<>s.moment_id;

INSERT INTO momentra_validation_result
SELECT 'VAL-FIN-006','P0','FINANCE','Settlement allocations cannot exceed the Settlement amount',COUNT(*),NULL
FROM finance.settlement s JOIN (SELECT settlement_id,SUM(amount) total FROM finance.settlement_allocation GROUP BY settlement_id) a USING(settlement_id)
WHERE a.total>s.amount;

INSERT INTO momentra_validation_result
SELECT 'VAL-FIN-007','P0','FINANCE','Settlement allocations cannot exceed an Obligation original amount',COUNT(*),NULL
FROM finance.participant_obligation po JOIN (SELECT participant_obligation_id,SUM(amount) total FROM finance.settlement_allocation GROUP BY participant_obligation_id) a USING(participant_obligation_id)
WHERE a.total>po.original_amount;

INSERT INTO momentra_validation_result
SELECT 'VAL-FIN-008','P0','FINANCE','Business Expense Vendor/Contract remain inside the Expense Company',COUNT(*),NULL
FROM finance.business_expense_context bec
LEFT JOIN business.vendor v ON v.vendor_id=bec.vendor_id
LEFT JOIN business.vendor_contract vc ON vc.vendor_contract_id=bec.vendor_contract_id
WHERE (bec.vendor_id IS NOT NULL AND v.company_id IS DISTINCT FROM bec.company_id)
   OR (bec.vendor_contract_id IS NOT NULL AND (vc.company_id IS DISTINCT FROM bec.company_id OR vc.vendor_id IS DISTINCT FROM bec.vendor_id));

INSERT INTO momentra_validation_result
SELECT 'VAL-FIN-009','P0','FINANCE','Vendor Contract is never populated without Vendor',COUNT(*),NULL
FROM finance.business_expense_context WHERE vendor_contract_id IS NOT NULL AND vendor_id IS NULL;

INSERT INTO momentra_validation_result
SELECT 'VAL-FIN-010','P0','FINANCE','Financial Accounts have exactly one physically valid owner',COUNT(*),NULL
FROM finance.financial_account
WHERE NOT ((owner_scope_type='USER' AND owner_user_id IS NOT NULL AND owner_company_id IS NULL AND owner_scope_id=owner_user_id)
        OR (owner_scope_type='COMPANY' AND owner_company_id IS NOT NULL AND owner_user_id IS NULL AND owner_scope_id=owner_company_id));

INSERT INTO momentra_validation_result
SELECT 'VAL-FIN-011','P0','FINANCE','Posted Group Expenses are fully allocated to active Expense Shares',COUNT(*),NULL
FROM finance.expense e JOIN finance.group_expense_context gec ON gec.expense_id=e.expense_id
LEFT JOIN LATERAL (SELECT COALESCE(SUM(es.share_amount),0) AS share_total FROM finance.expense_share es WHERE es.expense_id=e.expense_id AND es.status='ALLOCATED') s ON true
WHERE e.status='POSTED' AND s.share_total<>e.amount;

INSERT INTO momentra_validation_result
SELECT 'VAL-FIN-012','P0','FINANCE','Posted Invoice payments never exceed Invoice total',COUNT(*),NULL
FROM finance.invoice i LEFT JOIN LATERAL (SELECT COALESCE(SUM(ip.amount),0) AS paid FROM finance.invoice_payment ip WHERE ip.invoice_id=i.invoice_id AND ip.status='POSTED') p ON true
WHERE p.paid>i.total_amount;

INSERT INTO momentra_validation_result
SELECT 'VAL-FIN-013','P1','FINANCE','Invoice paid_amount agrees with effective posted payment rows',COUNT(*),NULL
FROM finance.invoice i LEFT JOIN LATERAL (SELECT COALESCE(SUM(ip.amount),0) AS paid FROM finance.invoice_payment ip WHERE ip.invoice_id=i.invoice_id AND ip.status='POSTED') p ON true
WHERE i.paid_amount<>p.paid;

-- -----------------------------------------------------------------------------
-- 06. GOVERNANCE / CONSENT / POLICY
-- -----------------------------------------------------------------------------
INSERT INTO momentra_validation_result
SELECT 'VAL-GOV-001','P0','GOVERNANCE','ROLE approval steps always name a Role and non-ROLE steps do not',COUNT(*),NULL
FROM governance.approval_step WHERE (step_type='ROLE' AND approver_role_id IS NULL) OR (step_type<>'ROLE' AND approver_role_id IS NOT NULL);

INSERT INTO momentra_validation_result
SELECT 'VAL-GOV-002','P0','GOVERNANCE','Completed approval requests have completed_at',COUNT(*),NULL
FROM governance.approval_request WHERE status IN ('APPROVED','REJECTED','CANCELLED','EXPIRED') AND completed_at IS NULL;

INSERT INTO momentra_validation_result
SELECT 'VAL-GOV-003','P0','GOVERNANCE','Completed approval steps have completed_at',COUNT(*),NULL
FROM governance.approval_step WHERE status IN ('APPROVED','REJECTED','SKIPPED','CANCELLED') AND completed_at IS NULL;

INSERT INTO momentra_validation_result
SELECT 'VAL-GOV-004','P0','GOVERNANCE','Withdrawn Consents have withdrawn_at',COUNT(*),NULL FROM governance.consent WHERE status='WITHDRAWN' AND withdrawn_at IS NULL;

INSERT INTO momentra_validation_result
SELECT 'VAL-GOV-005','P0','GOVERNANCE','Active Policies each have exactly one ACTIVE Policy Version',COUNT(*),string_agg(p.code, ', ' ORDER BY p.code)
FROM governance.policy p
LEFT JOIN LATERAL (SELECT COUNT(*) n FROM governance.policy_version pv WHERE pv.policy_id=p.policy_id AND pv.status='ACTIVE') v ON true
WHERE p.status='ACTIVE' AND v.n<>1;

INSERT INTO momentra_validation_result
SELECT 'VAL-GOV-006','P0','GOVERNANCE','No ACTIVE Policy Version contains review_required=true',COUNT(*),NULL
FROM governance.policy_version WHERE status='ACTIVE' AND COALESCE((definition->>'review_required')::boolean,false)=true;

INSERT INTO momentra_validation_result
SELECT 'VAL-GOV-007','P0','GOVERNANCE','No duplicate ACTIVE Role Assignment exists for the same user/role/scope',COUNT(*),NULL
FROM (SELECT user_id,role_id,scope_type,scope_id,COUNT(*) n FROM governance.role_assignment WHERE status='ACTIVE' GROUP BY user_id,role_id,scope_type,scope_id HAVING COUNT(*)>1) x;

INSERT INTO momentra_validation_result
SELECT 'VAL-GOV-008','P0','GOVERNANCE','No duplicate ACTIVE Consent exists for the same subject/purpose/scope',COUNT(*),NULL
FROM (SELECT subject_user_id,consent_purpose_id,scope_type,scope_id,COUNT(*) n FROM governance.consent WHERE status='ACTIVE' GROUP BY subject_user_id,consent_purpose_id,scope_type,scope_id HAVING COUNT(*)>1) x;

-- -----------------------------------------------------------------------------
-- 07. ANALYTICS / DETERMINISTIC INTELLIGENCE
-- -----------------------------------------------------------------------------
INSERT INTO momentra_validation_result
SELECT 'VAL-ANA-001','P0','ANALYTICS','Every ACTIVE Metric Definition has exactly one ACTIVE Metric Version',COUNT(*),string_agg(md.code, ', ' ORDER BY md.code)
FROM analytics.metric_definition md
LEFT JOIN LATERAL (SELECT COUNT(*) n FROM analytics.metric_version mv WHERE mv.metric_definition_id=md.metric_definition_id AND mv.status='ACTIVE') v ON true
WHERE md.status='ACTIVE' AND v.n<>1;

INSERT INTO momentra_validation_result
SELECT 'VAL-ANA-002','P0','ANALYTICS','No ACTIVE Metric Version contains review_required=true',COUNT(*),NULL
FROM analytics.metric_version WHERE status='ACTIVE' AND COALESCE((formula_definition->>'review_required')::boolean,false)=true;

WITH RECURSIVE walk(root_id,node_id,path,cycle) AS (
 SELECT md.metric_definition_id, md.depends_on_metric_definition_id, ARRAY[md.metric_definition_id,md.depends_on_metric_definition_id], md.metric_definition_id=md.depends_on_metric_definition_id
 FROM analytics.metric_dependency md
 UNION ALL
 SELECT w.root_id, md.depends_on_metric_definition_id, w.path||md.depends_on_metric_definition_id, md.depends_on_metric_definition_id=ANY(w.path)
 FROM walk w JOIN analytics.metric_dependency md ON md.metric_definition_id=w.node_id
 WHERE NOT w.cycle AND cardinality(w.path)<100
)
INSERT INTO momentra_validation_result
SELECT 'VAL-ANA-003','P0','ANALYTICS','Metric dependency graph is acyclic',COUNT(*),NULL FROM walk WHERE cycle;

INSERT INTO momentra_validation_result
SELECT 'VAL-ANA-004','P0','ANALYTICS','Metric Current version matches its Metric Observation version',COUNT(*),NULL
FROM analytics.metric_current mc JOIN analytics.metric_observation mo ON mo.metric_observation_id=mc.metric_observation_id
WHERE mc.metric_definition_id<>mo.metric_definition_id OR mc.metric_version_id<>mo.metric_version_id OR mc.scope_type<>mo.scope_type OR mc.scope_id<>mo.scope_id;

INSERT INTO momentra_validation_result
SELECT 'VAL-ANA-005','P0','ANALYTICS','Resolved Attention rows have resolved_at',COUNT(*),NULL FROM analytics.attention_item WHERE status='RESOLVED' AND resolved_at IS NULL;

-- -----------------------------------------------------------------------------
-- 08. MEMORY / LEARNING / PLAYBOOK
-- -----------------------------------------------------------------------------
INSERT INTO momentra_validation_result
SELECT 'VAL-MEM-001','P0','MEMORY','Learning never supersedes itself',COUNT(*),NULL FROM memory.learning WHERE learning_id=supersedes_learning_id;

WITH RECURSIVE walk(root_id,node_id,path,cycle) AS (
 SELECT l.learning_id,l.supersedes_learning_id,ARRAY[l.learning_id,l.supersedes_learning_id],l.learning_id=l.supersedes_learning_id
 FROM memory.learning l WHERE l.supersedes_learning_id IS NOT NULL
 UNION ALL
 SELECT w.root_id,l.supersedes_learning_id,w.path||l.supersedes_learning_id,l.supersedes_learning_id=ANY(w.path)
 FROM walk w JOIN memory.learning l ON l.learning_id=w.node_id
 WHERE l.supersedes_learning_id IS NOT NULL AND NOT w.cycle AND cardinality(w.path)<100
)
INSERT INTO momentra_validation_result
SELECT 'VAL-MEM-002','P0','MEMORY','Learning supersession graph is acyclic',COUNT(*),NULL FROM walk WHERE cycle;

INSERT INTO momentra_validation_result
SELECT 'VAL-MEM-003','P0','MEMORY','At most one ACTIVE Playbook Version exists per Playbook',COUNT(*),NULL
FROM (SELECT playbook_id,COUNT(*) n FROM memory.playbook_version WHERE status='ACTIVE' GROUP BY playbook_id HAVING COUNT(*)>1) x;

INSERT INTO momentra_validation_result
SELECT 'VAL-MEM-004','P0','MEMORY','MOMENT-scoped Memory references its declared Moment',COUNT(*),NULL
FROM memory.memory WHERE scope_type='MOMENT' AND (moment_id IS NULL OR scope_id<>moment_id);

-- -----------------------------------------------------------------------------
-- 09. AI TRUST / APPROVAL / PROVENANCE
-- -----------------------------------------------------------------------------
INSERT INTO momentra_validation_result
SELECT 'VAL-AI-001','P0','AI','Completed AI Context Sessions have closed_at',COUNT(*),NULL
FROM ai.context_session WHERE status IN ('COMPLETED','EXPIRED','REVOKED','FAILED') AND closed_at IS NULL;

INSERT INTO momentra_validation_result
SELECT 'VAL-AI-002','P0','AI','Completed Inference Runs have completed_at',COUNT(*),NULL
FROM ai.inference_run WHERE status<>'RUNNING' AND completed_at IS NULL;

INSERT INTO momentra_validation_result
SELECT 'VAL-AI-003','P0','AI','Confirmation-required Action Proposals cannot advance without confirmation',COUNT(*),NULL
FROM ai.action_proposal WHERE requires_user_confirmation=true AND status IN ('PENDING_APPROVAL','APPROVED','EXECUTING','EXECUTED','FAILED') AND (confirmed_by_user_id IS NULL OR confirmed_at IS NULL);

INSERT INTO momentra_validation_result
SELECT 'VAL-AI-004','P0','AI','Approval-required Action Proposals cannot advance without Approval Request',COUNT(*),NULL
FROM ai.action_proposal WHERE requires_governance_approval=true AND status IN ('APPROVED','EXECUTING','EXECUTED','FAILED') AND governance_approval_request_id IS NULL;

INSERT INTO momentra_validation_result
SELECT 'VAL-AI-005','P0','AI','Approved/executed AI proposals have APPROVED Governance state',COUNT(*),NULL
FROM ai.action_proposal ap JOIN governance.approval_request ar ON ar.approval_request_id=ap.governance_approval_request_id
WHERE ap.requires_governance_approval=true AND ap.status IN ('APPROVED','EXECUTING','EXECUTED') AND ar.status<>'APPROVED';

INSERT INTO momentra_validation_result
SELECT 'VAL-AI-006','P0','AI','Executed Action Proposals have executed_at',COUNT(*),NULL FROM ai.action_proposal WHERE status='EXECUTED' AND executed_at IS NULL;

INSERT INTO momentra_validation_result
SELECT 'VAL-AI-007','P1','AI','Active AI Insights have provenance',COUNT(*),NULL
FROM ai.ai_insight i WHERE i.status='ACTIVE' AND NOT EXISTS (SELECT 1 FROM ai.provenance p WHERE p.output_type='AI_INSIGHT' AND p.output_id=i.ai_insight_id);

-- -----------------------------------------------------------------------------
-- 10. EVENTS / OUTBOX / AUDIT / PLATFORM
-- -----------------------------------------------------------------------------
INSERT INTO momentra_validation_result
SELECT 'VAL-EVT-001','P0','EVENTS','Published Outbox rows have published_at',COUNT(*),NULL FROM events.outbox_event WHERE status='PUBLISHED' AND published_at IS NULL;

INSERT INTO momentra_validation_result
SELECT 'VAL-EVT-002','P0','EVENTS','Processing Outbox rows have lock ownership',COUNT(*),NULL FROM events.outbox_event WHERE status='PROCESSING' AND (locked_by IS NULL OR locked_at IS NULL);

INSERT INTO momentra_validation_result
SELECT 'VAL-EVT-003','P0','EVENTS','Outbox attempt_count never exceeds max_attempts',COUNT(*),NULL FROM events.outbox_event WHERE attempt_count>max_attempts;

INSERT INTO momentra_validation_result
SELECT 'VAL-EVT-004','P0','EVENTS','Completed consumer states have completed_at',COUNT(*),NULL
FROM events.event_consumer_state WHERE status IN ('SUCCEEDED','SKIPPED','DEAD_LETTER') AND completed_at IS NULL;

INSERT INTO momentra_validation_result
SELECT 'VAL-EVT-005','P0','EVENTS','Resolved/discarded Dead Letter rows have resolved_at',COUNT(*),NULL
FROM events.dead_letter_event WHERE status IN ('RESOLVED','DISCARDED') AND resolved_at IS NULL;

INSERT INTO momentra_validation_result
SELECT 'VAL-AUD-001','P0','AUDIT','USER/ADMIN Audit rows always identify actor_user_id',COUNT(*),NULL
FROM audit.audit_record WHERE actor_type IN ('USER','ADMIN') AND actor_user_id IS NULL;

INSERT INTO momentra_validation_result
SELECT 'VAL-PLT-001','P0','PLATFORM','Successful Idempotency records have completed_at',COUNT(*),NULL
FROM platform.idempotency_record WHERE status='SUCCEEDED' AND completed_at IS NULL;

INSERT INTO momentra_validation_result
SELECT 'VAL-PLT-002','P0','PLATFORM','Idempotency expiry never predates start',COUNT(*),NULL
FROM platform.idempotency_record WHERE expires_at IS NOT NULL AND expires_at<started_at;

INSERT INTO momentra_validation_result
SELECT 'VAL-PLT-003','P0','PLATFORM','Distributed Lock expiry follows acquisition',COUNT(*),NULL
FROM platform.distributed_lock WHERE expires_at<=acquired_at;

INSERT INTO momentra_validation_result
SELECT 'VAL-PLT-004','P0','PLATFORM','Job counters are never negative',COUNT(*),NULL
FROM platform.job_execution WHERE processed_count<0 OR success_count<0 OR failure_count<0;

-- -----------------------------------------------------------------------------
-- 11. PROJECTION CONSISTENCY
-- -----------------------------------------------------------------------------
INSERT INTO momentra_validation_result
SELECT 'VAL-PRJ-001','P0','PROJECTION','Personal Moment projections match canonical owner',COUNT(*),NULL
FROM projection.personal_moments pm JOIN personal.personal_moment_context pmc ON pmc.moment_id=pm.moment_id
WHERE pm.user_id<>pmc.user_id;

INSERT INTO momentra_validation_result
SELECT 'VAL-PRJ-002','P0','PROJECTION','Group Moment projections match participant/moment/user identity',COUNT(*),NULL
FROM projection.group_moments gm JOIN collaboration.moment_participant mp ON mp.participant_id=gm.participant_id
WHERE gm.moment_id<>mp.moment_id OR gm.user_id IS DISTINCT FROM mp.user_id;

INSERT INTO momentra_validation_result
SELECT 'VAL-PRJ-003','P0','PROJECTION','Business Moment projections preserve Company identity',COUNT(*),NULL
FROM projection.business_moments bm JOIN business.business_moment_context bmc ON bmc.moment_id=bm.moment_id
WHERE bm.company_id<>bmc.company_id;

INSERT INTO momentra_validation_result
SELECT 'VAL-PRJ-004','P0','PROJECTION','Group Finance Position participant belongs to the same Moment',COUNT(*),NULL
FROM projection.group_finance_position gfp JOIN collaboration.moment_participant mp ON mp.participant_id=gfp.participant_id
WHERE mp.moment_id<>gfp.moment_id;

INSERT INTO momentra_validation_result
SELECT 'VAL-PRJ-005','P0','PROJECTION','Available Actions reference matching Capability identity',COUNT(*),NULL
FROM projection.available_action aa JOIN core.capability c ON c.capability_id=aa.capability_id
WHERE aa.capability_code<>c.code;

INSERT INTO momentra_validation_result
SELECT 'VAL-PRJ-006','P0','PROJECTION','Projection lag_seconds is never negative',COUNT(*),NULL FROM projection.projection_state WHERE lag_seconds<0;

INSERT INTO momentra_validation_result
SELECT 'VAL-PRJ-007','P1','PROJECTION','Current projection AI references point only to ACTIVE AI Insights',COUNT(*),NULL
FROM (
 SELECT latest_ai_insight_id FROM projection.personal_pulse UNION ALL SELECT latest_ai_insight_id FROM projection.personal_life UNION ALL SELECT latest_ai_insight_id FROM projection.personal_memory
 UNION ALL SELECT latest_ai_insight_id FROM projection.group_pulse UNION ALL SELECT latest_ai_insight_id FROM projection.group_life UNION ALL SELECT latest_ai_insight_id FROM projection.group_memory
 UNION ALL SELECT latest_ai_insight_id FROM projection.business_pulse UNION ALL SELECT latest_ai_insight_id FROM projection.business_life UNION ALL SELECT latest_ai_insight_id FROM projection.business_memory
) x JOIN ai.ai_insight i ON i.ai_insight_id=x.latest_ai_insight_id WHERE x.latest_ai_insight_id IS NOT NULL AND i.status<>'ACTIVE';

-- -----------------------------------------------------------------------------
-- 12. RLS / PRIVILEGES / SECURITY METADATA
-- -----------------------------------------------------------------------------
WITH required(qualified_name) AS (VALUES
        ('business.business_moment_context'),
        ('business.business_operations_context'),
        ('business.business_review'),
        ('business.business_runway_context'),
        ('business.business_update'),
        ('business.company'),
        ('business.company_membership'),
        ('business.decision'),
        ('business.events_operations_context'),
        ('business.issue'),
        ('business.risk'),
        ('business.sla_check'),
        ('business.sla_definition'),
        ('business.team'),
        ('business.team_membership'),
        ('business.team_operations_context'),
        ('business.vendor'),
        ('business.vendor_contract'),
        ('business.vendor_operations_context'),
        ('collaboration.attendance'),
        ('collaboration.booking'),
        ('collaboration.community_coordination_context'),
        ('collaboration.coordination_item'),
        ('collaboration.delivery_handover'),
        ('collaboration.group_moment_context'),
        ('collaboration.group_update'),
        ('collaboration.group_vendor'),
        ('collaboration.living_rule'),
        ('collaboration.maintenance_record'),
        ('collaboration.moment_participant'),
        ('collaboration.ownership_record'),
        ('collaboration.planning_item'),
        ('collaboration.poll'),
        ('collaboration.poll_option'),
        ('collaboration.poll_vote'),
        ('collaboration.purchase_item'),
        ('collaboration.resident'),
        ('collaboration.shared_asset'),
        ('collaboration.shared_experience_context'),
        ('collaboration.shared_goal_context'),
        ('collaboration.shared_living_context'),
        ('collaboration.shared_purchase_context'),
        ('core.moment'),
        ('core.user_profile'),
        ('finance.budget'),
        ('finance.budget_revision'),
        ('finance.business_expense_context'),
        ('finance.contribution'),
        ('finance.expense'),
        ('finance.expense_resource_link'),
        ('finance.expense_share'),
        ('finance.expense_split'),
        ('finance.financial_account'),
        ('finance.financial_movement'),
        ('finance.financial_movement_link'),
        ('finance.group_expense_context'),
        ('finance.invoice'),
        ('finance.invoice_line'),
        ('finance.invoice_payment'),
        ('finance.participant_obligation'),
        ('finance.personal_expense_context'),
        ('finance.revenue'),
        ('finance.settlement'),
        ('finance.settlement_allocation'),
        ('memory.learning'),
        ('memory.learning_evidence'),
        ('memory.memory'),
        ('memory.memory_evidence'),
        ('memory.pattern'),
        ('memory.pattern_occurrence'),
        ('memory.playbook'),
        ('memory.playbook_evidence'),
        ('memory.playbook_version'),
        ('personal.future_learning_activity'),
        ('personal.future_opportunity'),
        ('personal.future_pivot'),
        ('personal.future_progress_observation'),
        ('personal.life_operation_observation'),
        ('personal.lifestyle_activity'),
        ('personal.personal_moment_context'),
        ('personal.relationship_activity'),
        ('personal.relationship_connection'),
        ('projection.attention_summary'),
        ('projection.available_action'),
        ('projection.business_finance_snapshot'),
        ('projection.business_life'),
        ('projection.business_memory'),
        ('projection.business_moments'),
        ('projection.business_pulse'),
        ('projection.group_finance_position'),
        ('projection.group_finance_snapshot'),
        ('projection.group_life'),
        ('projection.group_memory'),
        ('projection.group_moments'),
        ('projection.group_pulse'),
        ('projection.moment_summary'),
        ('projection.pending_approval_summary'),
        ('projection.personal_finance_snapshot'),
        ('projection.personal_life'),
        ('projection.personal_memory'),
        ('projection.personal_moments'),
        ('projection.personal_pulse'),
        ('projection.projection_state'),
        ('projection.recent_activity'),
        ('projection.user_company_access'),
        ('work.assignment'),
        ('work.goal'),
        ('work.milestone'),
        ('work.task'),
        ('work.task_dependency')
), missing AS (
 SELECT r.qualified_name
 FROM required r
 LEFT JOIN pg_class c ON c.oid=to_regclass(r.qualified_name)
 WHERE c.oid IS NULL OR c.relrowsecurity=false
)
INSERT INTO momentra_validation_result
SELECT 'VAL-SEC-001','P0','SECURITY','RLS is enabled on every protected table',COUNT(*),string_agg(qualified_name, ', ' ORDER BY qualified_name) FROM missing;

WITH required(qualified_name) AS (VALUES
        ('business.business_moment_context'),
        ('business.business_operations_context'),
        ('business.business_review'),
        ('business.business_runway_context'),
        ('business.business_update'),
        ('business.company'),
        ('business.company_membership'),
        ('business.decision'),
        ('business.events_operations_context'),
        ('business.issue'),
        ('business.risk'),
        ('business.sla_check'),
        ('business.sla_definition'),
        ('business.team'),
        ('business.team_membership'),
        ('business.team_operations_context'),
        ('business.vendor'),
        ('business.vendor_contract'),
        ('business.vendor_operations_context'),
        ('collaboration.attendance'),
        ('collaboration.booking'),
        ('collaboration.community_coordination_context'),
        ('collaboration.coordination_item'),
        ('collaboration.delivery_handover'),
        ('collaboration.group_moment_context'),
        ('collaboration.group_update'),
        ('collaboration.group_vendor'),
        ('collaboration.living_rule'),
        ('collaboration.maintenance_record'),
        ('collaboration.moment_participant'),
        ('collaboration.ownership_record'),
        ('collaboration.planning_item'),
        ('collaboration.poll'),
        ('collaboration.poll_option'),
        ('collaboration.poll_vote'),
        ('collaboration.purchase_item'),
        ('collaboration.resident'),
        ('collaboration.shared_asset'),
        ('collaboration.shared_experience_context'),
        ('collaboration.shared_goal_context'),
        ('collaboration.shared_living_context'),
        ('collaboration.shared_purchase_context'),
        ('core.moment'),
        ('core.user_profile'),
        ('finance.budget'),
        ('finance.budget_revision'),
        ('finance.business_expense_context'),
        ('finance.contribution'),
        ('finance.expense'),
        ('finance.expense_resource_link'),
        ('finance.expense_share'),
        ('finance.expense_split'),
        ('finance.financial_account'),
        ('finance.financial_movement'),
        ('finance.financial_movement_link'),
        ('finance.group_expense_context'),
        ('finance.invoice'),
        ('finance.invoice_line'),
        ('finance.invoice_payment'),
        ('finance.participant_obligation'),
        ('finance.personal_expense_context'),
        ('finance.revenue'),
        ('finance.settlement'),
        ('finance.settlement_allocation'),
        ('memory.learning'),
        ('memory.learning_evidence'),
        ('memory.memory'),
        ('memory.memory_evidence'),
        ('memory.pattern'),
        ('memory.pattern_occurrence'),
        ('memory.playbook'),
        ('memory.playbook_evidence'),
        ('memory.playbook_version'),
        ('personal.future_learning_activity'),
        ('personal.future_opportunity'),
        ('personal.future_pivot'),
        ('personal.future_progress_observation'),
        ('personal.life_operation_observation'),
        ('personal.lifestyle_activity'),
        ('personal.personal_moment_context'),
        ('personal.relationship_activity'),
        ('personal.relationship_connection'),
        ('projection.attention_summary'),
        ('projection.available_action'),
        ('projection.business_finance_snapshot'),
        ('projection.business_life'),
        ('projection.business_memory'),
        ('projection.business_moments'),
        ('projection.business_pulse'),
        ('projection.group_finance_position'),
        ('projection.group_finance_snapshot'),
        ('projection.group_life'),
        ('projection.group_memory'),
        ('projection.group_moments'),
        ('projection.group_pulse'),
        ('projection.moment_summary'),
        ('projection.pending_approval_summary'),
        ('projection.personal_finance_snapshot'),
        ('projection.personal_life'),
        ('projection.personal_memory'),
        ('projection.personal_moments'),
        ('projection.personal_pulse'),
        ('projection.projection_state'),
        ('projection.recent_activity'),
        ('projection.user_company_access'),
        ('work.assignment'),
        ('work.goal'),
        ('work.milestone'),
        ('work.task'),
        ('work.task_dependency')
), missing AS (
 SELECT r.qualified_name
 FROM required r
 WHERE NOT EXISTS (
   SELECT 1 FROM pg_policy p JOIN pg_class c ON c.oid=p.polrelid
   WHERE c.oid=to_regclass(r.qualified_name)
 )
)
INSERT INTO momentra_validation_result
SELECT 'VAL-SEC-002','P0','SECURITY','Every protected table has at least one RLS policy',COUNT(*),string_agg(qualified_name, ', ' ORDER BY qualified_name) FROM missing;

WITH expected(role_name) AS (VALUES ('momentra_app'),('momentra_outbox_worker'),('momentra_analytics_worker'),('momentra_memory_worker'),('momentra_ai_worker'),('momentra_projection_worker')), missing AS (
 SELECT e.role_name FROM expected e LEFT JOIN pg_roles r ON r.rolname=e.role_name WHERE r.oid IS NULL
)
INSERT INTO momentra_validation_result
SELECT 'VAL-SEC-003','P0','SECURITY','All internal least-privilege group roles exist',COUNT(*),string_agg(role_name, ', ' ORDER BY role_name) FROM missing;

INSERT INTO momentra_validation_result
SELECT 'VAL-SEC-004','P0','SECURITY','Only approved SECURITY DEFINER functions exist in the security schema',COUNT(*),string_agg(p.proname, ', ' ORDER BY p.proname)
FROM pg_proc p WHERE p.pronamespace=to_regnamespace('security') AND p.prosecdef=true
AND p.proname NOT IN ('current_user_id','owns_personal_moment','is_active_group_participant','is_active_company_member','can_access_moment','can_access_scope');

DO $security_privs$
DECLARE v_count BIGINT:=0; v_details TEXT;
BEGIN
 IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname='authenticated') THEN
   SELECT COUNT(*), string_agg(table_schema||'.'||table_name||':'||privilege_type, ', ' ORDER BY table_schema,table_name,privilege_type) INTO v_count,v_details
   FROM information_schema.role_table_grants
   WHERE grantee='authenticated' AND table_schema IN ('events','audit','platform','analytics','ai','governance')
     AND privilege_type IN ('SELECT','INSERT','UPDATE','DELETE');
 END IF;
 INSERT INTO momentra_validation_result VALUES ('VAL-SEC-005','P0','SECURITY','Authenticated clients have no direct DML privileges on internal schemas',v_count,v_details);
END $security_privs$;

DO $anon_privs$
DECLARE v_count BIGINT:=0; v_details TEXT;
BEGIN
 IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname='anon') THEN
   SELECT COUNT(*), string_agg(table_schema||'.'||table_name||':'||privilege_type, ', ' ORDER BY table_schema,table_name,privilege_type) INTO v_count,v_details
   FROM information_schema.role_table_grants
   WHERE grantee='anon' AND table_schema IN ('personal','collaboration','business','work','finance','memory','projection','events','audit','platform','analytics','ai','governance')
     AND privilege_type IN ('SELECT','INSERT','UPDATE','DELETE');
 END IF;
 INSERT INTO momentra_validation_result VALUES ('VAL-SEC-006','P0','SECURITY','Anonymous clients have no direct DML privileges on protected/internal schemas',v_count,v_details);
END $anon_privs$;

INSERT INTO momentra_validation_result
SELECT 'VAL-SEC-007','P0','SECURITY','AI worker has no direct mutation privileges on canonical product schemas',COUNT(*),string_agg(table_schema||'.'||table_name||':'||privilege_type, ', ' ORDER BY table_schema,table_name,privilege_type)
FROM information_schema.role_table_grants
WHERE grantee='momentra_ai_worker' AND table_schema IN ('personal','collaboration','business','work','finance') AND privilege_type IN ('INSERT','UPDATE','DELETE');

INSERT INTO momentra_validation_result
SELECT 'VAL-SEC-008','P0','SECURITY','Projection worker has no direct mutation privileges on canonical product schemas',COUNT(*),string_agg(table_schema||'.'||table_name||':'||privilege_type, ', ' ORDER BY table_schema,table_name,privilege_type)
FROM information_schema.role_table_grants
WHERE grantee='momentra_projection_worker' AND table_schema IN ('personal','collaboration','business','work','finance','governance','analytics','memory','ai') AND privilege_type IN ('INSERT','UPDATE','DELETE');

-- -----------------------------------------------------------------------------
-- 13. RELEASE BLOCKERS FROM REVIEW-REQUIRED SEED SEMANTICS
-- -----------------------------------------------------------------------------
INSERT INTO momentra_validation_result
SELECT 'VAL-RC-001','P0','RELEASE','No ACTIVE Metric Definition is left without production-ready ACTIVE formula version',COUNT(*),string_agg(md.code, ', ' ORDER BY md.code)
FROM analytics.metric_definition md WHERE md.status='ACTIVE' AND NOT EXISTS (SELECT 1 FROM analytics.metric_version mv WHERE mv.metric_definition_id=md.metric_definition_id AND mv.status='ACTIVE');

INSERT INTO momentra_validation_result
SELECT 'VAL-RC-002','P0','RELEASE','No ACTIVE Governance Policy is left without production-ready ACTIVE version',COUNT(*),string_agg(p.code, ', ' ORDER BY p.code)
FROM governance.policy p WHERE p.status='ACTIVE' AND NOT EXISTS (SELECT 1 FROM governance.policy_version pv WHERE pv.policy_id=p.policy_id AND pv.status='ACTIVE');

-- Current Phase 11.4 capability subsets were generated deterministically but the historical per-subtype matrix
-- must still be reconciled in Phase 11.7. SQL cannot infer that human product-design decision from database state.
INSERT INTO momentra_validation_result VALUES (
 'VAL-RC-003','P1','RELEASE',
 'Per-Moment-Type Quick Add subset source reconciliation is an external release-manifest gate',
 0,
 'V030 proves database referential completeness. Phase 11.7 must separately reconcile V019 against the frozen historical product matrix before release.'
);

-- -----------------------------------------------------------------------------
-- FINAL REPORT / FAIL-CLOSED RELEASE GATE
-- -----------------------------------------------------------------------------
SELECT severity, domain, COUNT(*) AS checks, COUNT(*) FILTER (WHERE NOT passed) AS failed_checks, SUM(violation_count) AS violations
FROM momentra_validation_result GROUP BY severity,domain ORDER BY severity,domain;

SELECT validation_code,severity,domain,description,violation_count,details
FROM momentra_validation_result WHERE NOT passed ORDER BY severity,validation_code;

DO $final_gate$
DECLARE
    v_p0 BIGINT; v_p1 BIGINT; v_codes TEXT;
BEGIN
    SELECT COUNT(*) FILTER (WHERE severity='P0' AND NOT passed),
           COUNT(*) FILTER (WHERE severity='P1' AND NOT passed)
    INTO v_p0,v_p1 FROM momentra_validation_result;
    SELECT string_agg(validation_code, ', ' ORDER BY severity,validation_code) INTO v_codes
    FROM momentra_validation_result WHERE severity IN ('P0','P1') AND NOT passed;
    IF v_p0>0 OR v_p1>0 THEN
        RAISE EXCEPTION 'Momentra V030 validation FAILED: P0 failed checks=%, P1 failed checks=%. Blockers: %',v_p0,v_p1,COALESCE(v_codes,'none');
    END IF;
    RAISE NOTICE 'Momentra V030 validation PASSED: no P0/P1 release blockers.';
END $final_gate$;

COMMIT;
