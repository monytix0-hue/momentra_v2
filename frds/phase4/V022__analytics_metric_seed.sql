BEGIN;

-- Metric definitions are seeded now; formula versions remain DRAFT because exact numeric formula/threshold semantics were not recoverable from the frozen source context.
-- Phase 11.4 deliberately does not invent financial or behavioral thresholds.

INSERT INTO analytics.metric_definition (metric_definition_id, code, display_name, description, domain_code, output_type, unit_code, status)
VALUES ('deefde6f-fd2b-5b90-8caf-812d39b8b5ec'::uuid, 'RECOVERY_SCORE', 'Recovery Score', 'Recovery state derived from Personal recovery observations.', 'PERSONAL', 'NUMBER', 'SCORE', 'ACTIVE');
INSERT INTO analytics.metric_version (metric_version_id, metric_definition_id, version_number, formula_type, formula_definition, null_behavior, time_window_definition, minimum_evidence_count, status)
VALUES ('c735d9e7-c91c-5c15-ab5d-ae3faf187889'::uuid, 'deefde6f-fd2b-5b90-8caf-812d39b8b5ec'::uuid, 1, 'COMPOSITE', '{"review_required":true,"semantic_contract":"Recovery state derived from Personal recovery observations.","activation_blocker":"Exact formula and threshold catalogue must be approved before ACTIVE status."}'::jsonb, 'NO_RESULT', '{}'::jsonb, 0, 'DRAFT');
INSERT INTO analytics.metric_input_definition (metric_input_definition_id, metric_version_id, input_code, source_type, source_reference, required, data_type, aggregation_rule)
VALUES ('acf83fa8-1dd7-52a3-af31-a3b67998d4e0'::uuid, 'c735d9e7-c91c-5c15-ab5d-ae3faf187889'::uuid, 'RECOVERY_OBSERVATIONS', 'TABLE_COLUMN', 'personal.life_observation:observation_type=RECOVERY', true, 'JSON', NULL);

INSERT INTO analytics.metric_definition (metric_definition_id, code, display_name, description, domain_code, output_type, unit_code, status)
VALUES ('0e206028-b38e-5f16-9782-15d97a1d1636'::uuid, 'MOOD_STATE', 'Mood State', 'Current deterministic mood state.', 'PERSONAL', 'CATEGORY', NULL, 'ACTIVE');
INSERT INTO analytics.metric_version (metric_version_id, metric_definition_id, version_number, formula_type, formula_definition, null_behavior, time_window_definition, minimum_evidence_count, status)
VALUES ('ba31f3d8-7615-5991-9efb-5276cdf15475'::uuid, '0e206028-b38e-5f16-9782-15d97a1d1636'::uuid, 1, 'COMPOSITE', '{"review_required":true,"semantic_contract":"Current deterministic mood state.","activation_blocker":"Exact formula and threshold catalogue must be approved before ACTIVE status."}'::jsonb, 'NO_RESULT', '{}'::jsonb, 0, 'DRAFT');
INSERT INTO analytics.metric_input_definition (metric_input_definition_id, metric_version_id, input_code, source_type, source_reference, required, data_type, aggregation_rule)
VALUES ('0469ee92-b72a-5ab0-be56-5fa7d6507130'::uuid, 'ba31f3d8-7615-5991-9efb-5276cdf15475'::uuid, 'MOOD_OBSERVATIONS', 'TABLE_COLUMN', 'personal.life_observation:observation_type=MOOD', true, 'JSON', NULL);

INSERT INTO analytics.metric_definition (metric_definition_id, code, display_name, description, domain_code, output_type, unit_code, status)
VALUES ('9641e123-000f-51ce-8efa-e0f9213393d4'::uuid, 'RHYTHM_CONSISTENCY', 'Rhythm Consistency', 'Consistency of rhythm / discipline observations.', 'PERSONAL', 'PERCENT', 'PERCENT', 'ACTIVE');
INSERT INTO analytics.metric_version (metric_version_id, metric_definition_id, version_number, formula_type, formula_definition, null_behavior, time_window_definition, minimum_evidence_count, status)
VALUES ('b51ea96e-30b5-5483-b4fd-1ead5def4323'::uuid, '9641e123-000f-51ce-8efa-e0f9213393d4'::uuid, 1, 'COMPOSITE', '{"review_required":true,"semantic_contract":"Consistency of rhythm / discipline observations.","activation_blocker":"Exact formula and threshold catalogue must be approved before ACTIVE status."}'::jsonb, 'NO_RESULT', '{}'::jsonb, 0, 'DRAFT');
INSERT INTO analytics.metric_input_definition (metric_input_definition_id, metric_version_id, input_code, source_type, source_reference, required, data_type, aggregation_rule)
VALUES ('88e3636c-565a-5297-9a82-1e446cae1df2'::uuid, 'b51ea96e-30b5-5483-b4fd-1ead5def4323'::uuid, 'RHYTHM_OBSERVATIONS', 'TABLE_COLUMN', 'personal.life_observation:observation_type=RHYTHM', true, 'JSON', NULL);

INSERT INTO analytics.metric_definition (metric_definition_id, code, display_name, description, domain_code, output_type, unit_code, status)
VALUES ('cb950ee0-8a10-524c-ace4-af02f418543f'::uuid, 'WELLBEING_STATE', 'Wellbeing State', 'Current deterministic wellbeing state.', 'PERSONAL', 'CATEGORY', NULL, 'ACTIVE');
INSERT INTO analytics.metric_version (metric_version_id, metric_definition_id, version_number, formula_type, formula_definition, null_behavior, time_window_definition, minimum_evidence_count, status)
VALUES ('ccc395a6-e67e-5348-baeb-a032f8e43f55'::uuid, 'cb950ee0-8a10-524c-ace4-af02f418543f'::uuid, 1, 'COMPOSITE', '{"review_required":true,"semantic_contract":"Current deterministic wellbeing state.","activation_blocker":"Exact formula and threshold catalogue must be approved before ACTIVE status."}'::jsonb, 'NO_RESULT', '{}'::jsonb, 0, 'DRAFT');
INSERT INTO analytics.metric_input_definition (metric_input_definition_id, metric_version_id, input_code, source_type, source_reference, required, data_type, aggregation_rule)
VALUES ('8b7167ec-63a1-50f6-90ba-16251832ea06'::uuid, 'ccc395a6-e67e-5348-baeb-a032f8e43f55'::uuid, 'WELLBEING_OBSERVATIONS', 'TABLE_COLUMN', 'personal.life_observation:observation_type=WELLBEING', true, 'JSON', NULL);

INSERT INTO analytics.metric_definition (metric_definition_id, code, display_name, description, domain_code, output_type, unit_code, status)
VALUES ('cad092cc-ac9c-5d88-a0ed-7b53aa8c19de'::uuid, 'GOAL_PROGRESS', 'Goal Progress', 'Progress of a Goal from milestones/progress records.', 'CROSS_DOMAIN', 'PERCENT', 'PERCENT', 'ACTIVE');
INSERT INTO analytics.metric_version (metric_version_id, metric_definition_id, version_number, formula_type, formula_definition, null_behavior, time_window_definition, minimum_evidence_count, status)
VALUES ('fbb555c7-2149-5bc8-b9bc-a5302038a9a9'::uuid, 'cad092cc-ac9c-5d88-a0ed-7b53aa8c19de'::uuid, 1, 'COMPOSITE', '{"review_required":true,"semantic_contract":"Progress of a Goal from milestones/progress records.","activation_blocker":"Exact formula and threshold catalogue must be approved before ACTIVE status."}'::jsonb, 'NO_RESULT', '{}'::jsonb, 0, 'DRAFT');
INSERT INTO analytics.metric_input_definition (metric_input_definition_id, metric_version_id, input_code, source_type, source_reference, required, data_type, aggregation_rule)
VALUES ('aa3a2196-af97-5e6b-a839-3e34b61fb5e5'::uuid, 'fbb555c7-2149-5bc8-b9bc-a5302038a9a9'::uuid, 'GOAL', 'TABLE_COLUMN', 'work.goal', true, 'JSON', NULL);
INSERT INTO analytics.metric_input_definition (metric_input_definition_id, metric_version_id, input_code, source_type, source_reference, required, data_type, aggregation_rule)
VALUES ('7a3bbffa-8021-5de0-a50b-b9ee672e58e1'::uuid, 'fbb555c7-2149-5bc8-b9bc-a5302038a9a9'::uuid, 'MILESTONES', 'TABLE_COLUMN', 'work.milestone', false, 'JSON', 'AGGREGATE');

INSERT INTO analytics.metric_definition (metric_definition_id, code, display_name, description, domain_code, output_type, unit_code, status)
VALUES ('a722c486-375b-5a7c-ba70-e561400ad796'::uuid, 'MILESTONE_PROGRESS', 'Milestone Progress', 'Progress of a Milestone.', 'CROSS_DOMAIN', 'PERCENT', 'PERCENT', 'ACTIVE');
INSERT INTO analytics.metric_version (metric_version_id, metric_definition_id, version_number, formula_type, formula_definition, null_behavior, time_window_definition, minimum_evidence_count, status)
VALUES ('6595e048-daa2-5f18-ab1c-40171751c73b'::uuid, 'a722c486-375b-5a7c-ba70-e561400ad796'::uuid, 1, 'COMPOSITE', '{"review_required":true,"semantic_contract":"Progress of a Milestone.","activation_blocker":"Exact formula and threshold catalogue must be approved before ACTIVE status."}'::jsonb, 'NO_RESULT', '{}'::jsonb, 0, 'DRAFT');
INSERT INTO analytics.metric_input_definition (metric_input_definition_id, metric_version_id, input_code, source_type, source_reference, required, data_type, aggregation_rule)
VALUES ('98c5e848-a025-5f4d-8757-379b3bb41e64'::uuid, '6595e048-daa2-5f18-ab1c-40171751c73b'::uuid, 'MILESTONE', 'TABLE_COLUMN', 'work.milestone', true, 'JSON', NULL);
INSERT INTO analytics.metric_input_definition (metric_input_definition_id, metric_version_id, input_code, source_type, source_reference, required, data_type, aggregation_rule)
VALUES ('e74cdfcf-8146-528b-a0cb-b00e220097f8'::uuid, '6595e048-daa2-5f18-ab1c-40171751c73b'::uuid, 'TASKS', 'TABLE_COLUMN', 'work.task', false, 'JSON', 'AGGREGATE');

INSERT INTO analytics.metric_definition (metric_definition_id, code, display_name, description, domain_code, output_type, unit_code, status)
VALUES ('b891861e-297c-508f-8902-f613abfaeaf7'::uuid, 'BUDGET_UTILIZATION', 'Budget Utilization', 'Eligible spend divided by applicable budget.', 'CROSS_DOMAIN', 'PERCENT', 'PERCENT', 'ACTIVE');
INSERT INTO analytics.metric_version (metric_version_id, metric_definition_id, version_number, formula_type, formula_definition, null_behavior, time_window_definition, minimum_evidence_count, status)
VALUES ('0f8a565b-6d45-5831-bea3-90459e136c7e'::uuid, 'b891861e-297c-508f-8902-f613abfaeaf7'::uuid, 1, 'COMPOSITE', '{"review_required":true,"semantic_contract":"Eligible spend divided by applicable budget.","activation_blocker":"Exact formula and threshold catalogue must be approved before ACTIVE status."}'::jsonb, 'NO_RESULT', '{}'::jsonb, 0, 'DRAFT');
INSERT INTO analytics.metric_input_definition (metric_input_definition_id, metric_version_id, input_code, source_type, source_reference, required, data_type, aggregation_rule)
VALUES ('bed6f743-61da-5754-8ac5-e9429c2b07bb'::uuid, '0f8a565b-6d45-5831-bea3-90459e136c7e'::uuid, 'BUDGET_AMOUNT', 'TABLE_COLUMN', 'finance.budget', true, 'NUMBER', 'CURRENT_ACTIVE');
INSERT INTO analytics.metric_input_definition (metric_input_definition_id, metric_version_id, input_code, source_type, source_reference, required, data_type, aggregation_rule)
VALUES ('8d812e20-dfe2-503e-b5c1-bbae0908cf3c'::uuid, '0f8a565b-6d45-5831-bea3-90459e136c7e'::uuid, 'ELIGIBLE_SPEND', 'TABLE_COLUMN', 'finance.expense', true, 'NUMBER', 'SUM_POSTED');

INSERT INTO analytics.metric_definition (metric_definition_id, code, display_name, description, domain_code, output_type, unit_code, status)
VALUES ('311a24f9-d169-5671-a361-0b416ea83299'::uuid, 'GROUP_CONTRIBUTION_COMPLETION', 'Group Contribution Completion', 'Completed contributions relative to expected contributions.', 'GROUP', 'PERCENT', 'PERCENT', 'ACTIVE');
INSERT INTO analytics.metric_version (metric_version_id, metric_definition_id, version_number, formula_type, formula_definition, null_behavior, time_window_definition, minimum_evidence_count, status)
VALUES ('bb05afa2-c8b4-5394-8174-e44c49bc8b02'::uuid, '311a24f9-d169-5671-a361-0b416ea83299'::uuid, 1, 'COMPOSITE', '{"review_required":true,"semantic_contract":"Completed contributions relative to expected contributions.","activation_blocker":"Exact formula and threshold catalogue must be approved before ACTIVE status."}'::jsonb, 'NO_RESULT', '{}'::jsonb, 0, 'DRAFT');
INSERT INTO analytics.metric_input_definition (metric_input_definition_id, metric_version_id, input_code, source_type, source_reference, required, data_type, aggregation_rule)
VALUES ('f59daa6f-5a8d-50ce-b574-c75b52accc23'::uuid, 'bb05afa2-c8b4-5394-8174-e44c49bc8b02'::uuid, 'CONTRIBUTIONS', 'TABLE_COLUMN', 'finance.contribution', true, 'JSON', 'AGGREGATE');

INSERT INTO analytics.metric_definition (metric_definition_id, code, display_name, description, domain_code, output_type, unit_code, status)
VALUES ('ba6cd66e-266e-5a29-8598-6441337852ab'::uuid, 'PARTICIPATION_RATE', 'Participation Rate', 'Active participation relative to expected participants/activities.', 'GROUP', 'PERCENT', 'PERCENT', 'ACTIVE');
INSERT INTO analytics.metric_version (metric_version_id, metric_definition_id, version_number, formula_type, formula_definition, null_behavior, time_window_definition, minimum_evidence_count, status)
VALUES ('29979eba-9313-566e-9962-ad26c391183c'::uuid, 'ba6cd66e-266e-5a29-8598-6441337852ab'::uuid, 1, 'COMPOSITE', '{"review_required":true,"semantic_contract":"Active participation relative to expected participants/activities.","activation_blocker":"Exact formula and threshold catalogue must be approved before ACTIVE status."}'::jsonb, 'NO_RESULT', '{}'::jsonb, 0, 'DRAFT');
INSERT INTO analytics.metric_input_definition (metric_input_definition_id, metric_version_id, input_code, source_type, source_reference, required, data_type, aggregation_rule)
VALUES ('1cb147f9-9ed1-5bcc-aca1-f7a53b7ce0b2'::uuid, '29979eba-9313-566e-9962-ad26c391183c'::uuid, 'PARTICIPANTS', 'TABLE_COLUMN', 'collaboration.moment_participant', true, 'JSON', 'AGGREGATE');

INSERT INTO analytics.metric_definition (metric_definition_id, code, display_name, description, domain_code, output_type, unit_code, status)
VALUES ('2a26c127-fdde-5621-a0c6-1a73d2038d17'::uuid, 'BUSINESS_RUNWAY_MONTHS', 'Business Runway Months', 'Available liquid resources divided by normalized monthly burn.', 'BUSINESS', 'NUMBER', 'MONTH', 'ACTIVE');
INSERT INTO analytics.metric_version (metric_version_id, metric_definition_id, version_number, formula_type, formula_definition, null_behavior, time_window_definition, minimum_evidence_count, status)
VALUES ('2779c03d-980b-5834-8bd2-e00260a62488'::uuid, '2a26c127-fdde-5621-a0c6-1a73d2038d17'::uuid, 1, 'COMPOSITE', '{"review_required":true,"semantic_contract":"Available liquid resources divided by normalized monthly burn.","activation_blocker":"Exact formula and threshold catalogue must be approved before ACTIVE status."}'::jsonb, 'NO_RESULT', '{}'::jsonb, 0, 'DRAFT');
INSERT INTO analytics.metric_input_definition (metric_input_definition_id, metric_version_id, input_code, source_type, source_reference, required, data_type, aggregation_rule)
VALUES ('66b6144d-d9c4-5880-b967-25ea27e5512a'::uuid, '2779c03d-980b-5834-8bd2-e00260a62488'::uuid, 'LIQUID_RESOURCES', 'SERVICE', 'finance:company_liquid_resources', true, 'NUMBER', NULL);
INSERT INTO analytics.metric_input_definition (metric_input_definition_id, metric_version_id, input_code, source_type, source_reference, required, data_type, aggregation_rule)
VALUES ('7d8dd155-e89f-56bd-ad2f-e70e2f8cb2d2'::uuid, '2779c03d-980b-5834-8bd2-e00260a62488'::uuid, 'MONTHLY_BURN', 'METRIC', 'BUSINESS_BURN_RATE', true, 'NUMBER', NULL);

INSERT INTO analytics.metric_definition (metric_definition_id, code, display_name, description, domain_code, output_type, unit_code, status)
VALUES ('b2074c6b-3296-59ed-81f3-8ad86df1e3a2'::uuid, 'BUSINESS_BURN_RATE', 'Business Burn Rate', 'Normalized business cash burn rate.', 'BUSINESS', 'NUMBER', 'CURRENCY_PER_MONTH', 'ACTIVE');
INSERT INTO analytics.metric_version (metric_version_id, metric_definition_id, version_number, formula_type, formula_definition, null_behavior, time_window_definition, minimum_evidence_count, status)
VALUES ('5a156888-bf19-5113-ae80-2d15db9c5469'::uuid, 'b2074c6b-3296-59ed-81f3-8ad86df1e3a2'::uuid, 1, 'COMPOSITE', '{"review_required":true,"semantic_contract":"Normalized business cash burn rate.","activation_blocker":"Exact formula and threshold catalogue must be approved before ACTIVE status."}'::jsonb, 'NO_RESULT', '{}'::jsonb, 0, 'DRAFT');
INSERT INTO analytics.metric_input_definition (metric_input_definition_id, metric_version_id, input_code, source_type, source_reference, required, data_type, aggregation_rule)
VALUES ('ffeda2c5-d4d2-5c32-b40b-4d94bc60a0b3'::uuid, '5a156888-bf19-5113-ae80-2d15db9c5469'::uuid, 'BUSINESS_MOVEMENTS', 'TABLE_COLUMN', 'finance.financial_movement', true, 'JSON', 'MONTHLY_AGGREGATE');

INSERT INTO analytics.metric_definition (metric_definition_id, code, display_name, description, domain_code, output_type, unit_code, status)
VALUES ('94b09269-97e6-5fb9-aec5-c7087fc15b8d'::uuid, 'SLA_COMPLIANCE', 'SLA Compliance', 'SLA checks meeting target relative to evaluated checks.', 'BUSINESS', 'PERCENT', 'PERCENT', 'ACTIVE');
INSERT INTO analytics.metric_version (metric_version_id, metric_definition_id, version_number, formula_type, formula_definition, null_behavior, time_window_definition, minimum_evidence_count, status)
VALUES ('9e63a0b0-2a84-5120-9f16-760d5479947c'::uuid, '94b09269-97e6-5fb9-aec5-c7087fc15b8d'::uuid, 1, 'COMPOSITE', '{"review_required":true,"semantic_contract":"SLA checks meeting target relative to evaluated checks.","activation_blocker":"Exact formula and threshold catalogue must be approved before ACTIVE status."}'::jsonb, 'NO_RESULT', '{}'::jsonb, 0, 'DRAFT');
INSERT INTO analytics.metric_input_definition (metric_input_definition_id, metric_version_id, input_code, source_type, source_reference, required, data_type, aggregation_rule)
VALUES ('e89bc73f-6c12-56d8-a3e2-88e9f20b3e92'::uuid, '9e63a0b0-2a84-5120-9f16-760d5479947c'::uuid, 'SLA_CHECKS', 'TABLE_COLUMN', 'business.sla_check', true, 'JSON', 'AGGREGATE');

INSERT INTO analytics.metric_definition (metric_definition_id, code, display_name, description, domain_code, output_type, unit_code, status)
VALUES ('0c1ff247-3bdc-59d7-bfab-2b8425376c35'::uuid, 'RELATIONSHIP_INVESTMENT', 'Relationship Investment', 'Deterministic measure derived from relationship activity/investment facts.', 'PERSONAL', 'NUMBER', 'SCORE', 'ACTIVE');
INSERT INTO analytics.metric_version (metric_version_id, metric_definition_id, version_number, formula_type, formula_definition, null_behavior, time_window_definition, minimum_evidence_count, status)
VALUES ('713d9d8a-5dca-5859-9c07-92b24dfb73a6'::uuid, '0c1ff247-3bdc-59d7-bfab-2b8425376c35'::uuid, 1, 'COMPOSITE', '{"review_required":true,"semantic_contract":"Deterministic measure derived from relationship activity/investment facts.","activation_blocker":"Exact formula and threshold catalogue must be approved before ACTIVE status."}'::jsonb, 'NO_RESULT', '{}'::jsonb, 0, 'DRAFT');
INSERT INTO analytics.metric_input_definition (metric_input_definition_id, metric_version_id, input_code, source_type, source_reference, required, data_type, aggregation_rule)
VALUES ('4cd1cd74-2e1f-5e9c-9fd4-c945c5c51b75'::uuid, '713d9d8a-5dca-5859-9c07-92b24dfb73a6'::uuid, 'RELATIONSHIP_ACTIVITY', 'TABLE_COLUMN', 'personal.relationship_activity', true, 'JSON', 'AGGREGATE');

INSERT INTO analytics.metric_definition (metric_definition_id, code, display_name, description, domain_code, output_type, unit_code, status)
VALUES ('318cfdaf-05fe-5ace-af54-f39056c41a6d'::uuid, 'MEMORY_STRENGTH', 'Memory Strength', 'Deterministic memory-strength measure from durable Memory evidence.', 'CROSS_DOMAIN', 'NUMBER', 'SCORE', 'ACTIVE');
INSERT INTO analytics.metric_version (metric_version_id, metric_definition_id, version_number, formula_type, formula_definition, null_behavior, time_window_definition, minimum_evidence_count, status)
VALUES ('175e468f-436b-54e8-8fc9-fe5365ff5748'::uuid, '318cfdaf-05fe-5ace-af54-f39056c41a6d'::uuid, 1, 'COMPOSITE', '{"review_required":true,"semantic_contract":"Deterministic memory-strength measure from durable Memory evidence.","activation_blocker":"Exact formula and threshold catalogue must be approved before ACTIVE status."}'::jsonb, 'NO_RESULT', '{}'::jsonb, 0, 'DRAFT');
INSERT INTO analytics.metric_input_definition (metric_input_definition_id, metric_version_id, input_code, source_type, source_reference, required, data_type, aggregation_rule)
VALUES ('3740afe1-48c8-5f0c-a1ce-24962bc4c038'::uuid, '175e468f-436b-54e8-8fc9-fe5365ff5748'::uuid, 'MEMORIES', 'TABLE_COLUMN', 'memory.memory', true, 'JSON', 'AGGREGATE');
INSERT INTO analytics.metric_input_definition (metric_input_definition_id, metric_version_id, input_code, source_type, source_reference, required, data_type, aggregation_rule)
VALUES ('6b27b2d3-3f75-5ec5-a6ea-9dbd0450d184'::uuid, '175e468f-436b-54e8-8fc9-fe5365ff5748'::uuid, 'EVIDENCE', 'TABLE_COLUMN', 'memory.memory_evidence', false, 'JSON', 'AGGREGATE');

INSERT INTO analytics.metric_dependency (metric_dependency_id, metric_definition_id, depends_on_metric_definition_id, dependency_type)
VALUES ('657f8fda-5a3e-5469-8e38-8020e0a57ab4'::uuid, '2a26c127-fdde-5621-a0c6-1a73d2038d17'::uuid, 'b2074c6b-3296-59ed-81f3-8ad86df1e3a2'::uuid, 'VALUE');

-- No analytics.threshold_definition rows are activated in this baseline. Exact threshold values were not frozen in recoverable source material.
-- V030 must fail production readiness if a UI/attention contract expects an ACTIVE metric version or threshold that has not been approved.
COMMIT;
