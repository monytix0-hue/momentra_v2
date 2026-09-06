BEGIN;

INSERT INTO governance.consent_purpose (consent_purpose_id, code, display_name, description, status)
VALUES ('c18e618d-27f6-5cf1-b957-37138469e2df'::uuid, 'PERSONAL_ANALYTICS', 'Personal Analytics', 'Deterministic analytics over the user''s Personal data.', 'ACTIVE');

INSERT INTO governance.consent_purpose (consent_purpose_id, code, display_name, description, status)
VALUES ('60c32b13-4d03-5ca5-9005-c4c6d7530d87'::uuid, 'CROSS_DOMAIN_LIFE360', 'Cross-Domain Life 360', 'Authorized cross-domain summary processing for Life 360.', 'ACTIVE');

INSERT INTO governance.consent_purpose (consent_purpose_id, code, display_name, description, status)
VALUES ('6d470ca8-4be3-588d-a0cd-2c14ada8d28b'::uuid, 'AI_INSIGHT_GENERATION', 'AI Insight Generation', 'Generate AI insights from purpose-limited context.', 'ACTIVE');

INSERT INTO governance.consent_purpose (consent_purpose_id, code, display_name, description, status)
VALUES ('c1e7400e-563f-586e-971e-e86fe98fce49'::uuid, 'AI_RECOMMENDATION_GENERATION', 'AI Recommendation Generation', 'Generate AI recommendations from purpose-limited context.', 'ACTIVE');

INSERT INTO governance.consent_purpose (consent_purpose_id, code, display_name, description, status)
VALUES ('4a6a9762-94d3-5cc1-9402-a116756dfd31'::uuid, 'AI_ACTION_ASSISTANCE', 'AI Action Assistance', 'Generate and assist with governed AI action proposals.', 'ACTIVE');

INSERT INTO governance.consent_purpose (consent_purpose_id, code, display_name, description, status)
VALUES ('0969c767-5680-51ed-b113-74068f098f01'::uuid, 'MEMORY_PATTERN_ANALYSIS', 'Memory and Pattern Analysis', 'Analyze memories/evidence for patterns and learnings.', 'ACTIVE');

INSERT INTO governance.consent_purpose (consent_purpose_id, code, display_name, description, status)
VALUES ('69325271-b82e-5b88-bb81-e20757be338a'::uuid, 'GROUP_DATA_SHARING', 'Group Data Sharing', 'Use data within an authorized Group Moment scope.', 'ACTIVE');

INSERT INTO governance.consent_purpose (consent_purpose_id, code, display_name, description, status)
VALUES ('80921c87-6671-5e58-a0e6-529338a6fdb4'::uuid, 'BUSINESS_ANALYTICS', 'Business Analytics', 'Deterministic analytics within an authorized Company scope.', 'ACTIVE');

INSERT INTO governance.data_category (data_category_id, code, display_name, description, default_sensitivity_level, status)
VALUES ('6d7169a1-50b9-5d18-8654-dd42dbe90866'::uuid, 'IDENTITY', 'Identity', 'Identity/profile data.', 'CONFIDENTIAL', 'ACTIVE');

INSERT INTO governance.data_category (data_category_id, code, display_name, description, default_sensitivity_level, status)
VALUES ('ead1fd78-c021-57de-90cb-f0ffbe2295a5'::uuid, 'MOMENT', 'Moment', 'Moment metadata and lifecycle data.', 'INTERNAL', 'ACTIVE');

INSERT INTO governance.data_category (data_category_id, code, display_name, description, default_sensitivity_level, status)
VALUES ('eb617a9d-c680-5d17-b6eb-56dcc3c4e820'::uuid, 'FINANCIAL', 'Financial', 'Expenses, accounts, budgets, settlements, revenue and invoices.', 'RESTRICTED', 'ACTIVE');

INSERT INTO governance.data_category (data_category_id, code, display_name, description, default_sensitivity_level, status)
VALUES ('bd77c33b-dc29-50b9-bd36-0c22ce976d8b'::uuid, 'RELATIONSHIP', 'Relationship', 'Personal relationship and interaction data.', 'RESTRICTED', 'ACTIVE');

INSERT INTO governance.data_category (data_category_id, code, display_name, description, default_sensitivity_level, status)
VALUES ('8dec731a-9b54-54a5-8a6d-a70514139587'::uuid, 'WELLBEING', 'Wellbeing', 'Mood, recovery, rhythm and wellbeing observations.', 'RESTRICTED', 'ACTIVE');

INSERT INTO governance.data_category (data_category_id, code, display_name, description, default_sensitivity_level, status)
VALUES ('2876f3f6-2763-54e1-9353-f27ec485bab6'::uuid, 'LOCATION', 'Location', 'Location/place information.', 'HIGHLY_CONFIDENTIAL', 'ACTIVE');

INSERT INTO governance.data_category (data_category_id, code, display_name, description, default_sensitivity_level, status)
VALUES ('4855c343-9b52-588c-882a-cd1b21befeb8'::uuid, 'BUSINESS', 'Business', 'Company, team, vendor and business operational data.', 'CONFIDENTIAL', 'ACTIVE');

INSERT INTO governance.data_category (data_category_id, code, display_name, description, default_sensitivity_level, status)
VALUES ('2ea52f2d-8a2a-53fe-8fae-8259cd03022a'::uuid, 'COLLABORATION', 'Collaboration', 'Group participation and shared activity data.', 'CONFIDENTIAL', 'ACTIVE');

INSERT INTO governance.data_category (data_category_id, code, display_name, description, default_sensitivity_level, status)
VALUES ('a89eaf23-5715-5aa1-aa36-4cb239325cc3'::uuid, 'MEMORY', 'Memory', 'Durable memory, evidence, learning and playbook data.', 'HIGHLY_CONFIDENTIAL', 'ACTIVE');

INSERT INTO governance.data_category (data_category_id, code, display_name, description, default_sensitivity_level, status)
VALUES ('a508fa76-1057-582d-bce3-6e087a4472a2'::uuid, 'AI_CONTEXT', 'AI Context', 'Purpose-limited context assembled for AI inference.', 'RESTRICTED', 'ACTIVE');

COMMIT;
