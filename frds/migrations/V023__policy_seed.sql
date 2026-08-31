BEGIN;

-- Policy identities are seeded; versions remain DRAFT until exact decision rules/cut-offs are approved.
INSERT INTO governance.policy (policy_id, code, display_name, description, policy_family, status)
VALUES ('54c06264-e76f-52f3-83bf-2808464d3794'::uuid, 'MOMENT_LIFECYCLE_ACTION', 'Moment Lifecycle Action', 'Controls actions allowed by Moment lifecycle state.', 'MOMENT', 'ACTIVE');
INSERT INTO governance.policy_version (policy_version_id, policy_id, version_number, definition, decision_precedence, status)
VALUES ('94ade405-1eb6-5bb2-98e7-c4b64bff01c4'::uuid, '54c06264-e76f-52f3-83bf-2808464d3794'::uuid, 1, '{"review_required":true,"default_decision":"DENY","semantic_contract":"Controls actions allowed by Moment lifecycle state.","activation_blocker":"Exact production rule set must be approved before ACTIVE status."}'::jsonb, '{"order":["DENY","REQUIRE_APPROVAL","REQUIRE_CONFIRMATION","ALLOW"],"default":"DENY"}'::jsonb, 'DRAFT');

INSERT INTO governance.policy (policy_id, code, display_name, description, policy_family, status)
VALUES ('3ae3ec1d-9139-586a-be4f-00c481b81b09'::uuid, 'GROUP_PARTICIPATION_ACCESS', 'Group Participation Access', 'Controls Group access/actions by participant state.', 'PARTICIPATION', 'ACTIVE');
INSERT INTO governance.policy_version (policy_version_id, policy_id, version_number, definition, decision_precedence, status)
VALUES ('66012739-50c5-5548-9f8d-d5749e5bf368'::uuid, '3ae3ec1d-9139-586a-be4f-00c481b81b09'::uuid, 1, '{"review_required":true,"default_decision":"DENY","semantic_contract":"Controls Group access/actions by participant state.","activation_blocker":"Exact production rule set must be approved before ACTIVE status."}'::jsonb, '{"order":["DENY","REQUIRE_APPROVAL","REQUIRE_CONFIRMATION","ALLOW"],"default":"DENY"}'::jsonb, 'DRAFT');

INSERT INTO governance.policy (policy_id, code, display_name, description, policy_family, status)
VALUES ('7bc76740-6959-5e43-833c-10ad30bcc623'::uuid, 'BUSINESS_MEMBERSHIP_ACCESS', 'Business Membership Access', 'Controls Business access/actions by Company membership state.', 'BUSINESS', 'ACTIVE');
INSERT INTO governance.policy_version (policy_version_id, policy_id, version_number, definition, decision_precedence, status)
VALUES ('e24c3d8c-9928-5333-9f1e-a7b40fdd0796'::uuid, '7bc76740-6959-5e43-833c-10ad30bcc623'::uuid, 1, '{"review_required":true,"default_decision":"DENY","semantic_contract":"Controls Business access/actions by Company membership state.","activation_blocker":"Exact production rule set must be approved before ACTIVE status."}'::jsonb, '{"order":["DENY","REQUIRE_APPROVAL","REQUIRE_CONFIRMATION","ALLOW"],"default":"DENY"}'::jsonb, 'DRAFT');

INSERT INTO governance.policy (policy_id, code, display_name, description, policy_family, status)
VALUES ('9ecb416c-138a-5b67-9268-42dbc306a79c'::uuid, 'FINANCE_SENSITIVE_ACTION', 'Finance Sensitive Action', 'Controls sensitive Finance actions and approval requirements.', 'FINANCE', 'ACTIVE');
INSERT INTO governance.policy_version (policy_version_id, policy_id, version_number, definition, decision_precedence, status)
VALUES ('017322b9-b650-5d6b-aff5-91fb1c6fbb4a'::uuid, '9ecb416c-138a-5b67-9268-42dbc306a79c'::uuid, 1, '{"review_required":true,"default_decision":"DENY","semantic_contract":"Controls sensitive Finance actions and approval requirements.","activation_blocker":"Exact production rule set must be approved before ACTIVE status."}'::jsonb, '{"order":["DENY","REQUIRE_APPROVAL","REQUIRE_CONFIRMATION","ALLOW"],"default":"DENY"}'::jsonb, 'DRAFT');

INSERT INTO governance.policy (policy_id, code, display_name, description, policy_family, status)
VALUES ('97e75dcb-e5ef-5d30-b436-9f7a8bd3837b'::uuid, 'CONSENT_REQUIRED_PROCESSING', 'Consent Required Processing', 'Requires active purpose/scope consent for governed processing.', 'CONSENT', 'ACTIVE');
INSERT INTO governance.policy_version (policy_version_id, policy_id, version_number, definition, decision_precedence, status)
VALUES ('da1a6642-6e2c-5c93-b6ec-8c5107b415e7'::uuid, '97e75dcb-e5ef-5d30-b436-9f7a8bd3837b'::uuid, 1, '{"review_required":true,"default_decision":"DENY","semantic_contract":"Requires active purpose/scope consent for governed processing.","activation_blocker":"Exact production rule set must be approved before ACTIVE status."}'::jsonb, '{"order":["DENY","REQUIRE_APPROVAL","REQUIRE_CONFIRMATION","ALLOW"],"default":"DENY"}'::jsonb, 'DRAFT');

INSERT INTO governance.policy (policy_id, code, display_name, description, policy_family, status)
VALUES ('3ff211ce-b1e4-5644-8f06-cc9b9eb379fc'::uuid, 'AI_CONTEXT_ACCESS', 'AI Context Access', 'Controls AI context construction from governed data.', 'AI', 'ACTIVE');
INSERT INTO governance.policy_version (policy_version_id, policy_id, version_number, definition, decision_precedence, status)
VALUES ('ac67d44d-3bc0-504f-9823-07270cda9bd2'::uuid, '3ff211ce-b1e4-5644-8f06-cc9b9eb379fc'::uuid, 1, '{"review_required":true,"default_decision":"DENY","semantic_contract":"Controls AI context construction from governed data.","activation_blocker":"Exact production rule set must be approved before ACTIVE status."}'::jsonb, '{"order":["DENY","REQUIRE_APPROVAL","REQUIRE_CONFIRMATION","ALLOW"],"default":"DENY"}'::jsonb, 'DRAFT');

INSERT INTO governance.policy (policy_id, code, display_name, description, policy_family, status)
VALUES ('84d08065-092c-598f-a83f-4f1ef3d62ae5'::uuid, 'AI_ACTION_EXECUTION', 'AI Action Execution', 'Controls confirmation, approval and reauthorization for AI Action execution.', 'AI', 'ACTIVE');
INSERT INTO governance.policy_version (policy_version_id, policy_id, version_number, definition, decision_precedence, status)
VALUES ('8ecec734-1613-57c5-8353-6fee65528a5b'::uuid, '84d08065-092c-598f-a83f-4f1ef3d62ae5'::uuid, 1, '{"review_required":true,"default_decision":"DENY","semantic_contract":"Controls confirmation, approval and reauthorization for AI Action execution.","activation_blocker":"Exact production rule set must be approved before ACTIVE status."}'::jsonb, '{"order":["DENY","REQUIRE_APPROVAL","REQUIRE_CONFIRMATION","ALLOW"],"default":"DENY"}'::jsonb, 'DRAFT');

INSERT INTO governance.policy (policy_id, code, display_name, description, policy_family, status)
VALUES ('78a02fb1-afaa-514f-98f9-cb382c1649ce'::uuid, 'MEMORY_VISIBILITY', 'Memory Visibility', 'Controls Memory visibility and evidence disclosure by scope.', 'MEMORY', 'ACTIVE');
INSERT INTO governance.policy_version (policy_version_id, policy_id, version_number, definition, decision_precedence, status)
VALUES ('79b32600-8482-5c29-849e-2c8dc7850b62'::uuid, '78a02fb1-afaa-514f-98f9-cb382c1649ce'::uuid, 1, '{"review_required":true,"default_decision":"DENY","semantic_contract":"Controls Memory visibility and evidence disclosure by scope.","activation_blocker":"Exact production rule set must be approved before ACTIVE status."}'::jsonb, '{"order":["DENY","REQUIRE_APPROVAL","REQUIRE_CONFIRMATION","ALLOW"],"default":"DENY"}'::jsonb, 'DRAFT');

INSERT INTO governance.policy (policy_id, code, display_name, description, policy_family, status)
VALUES ('495136fb-8d90-5a39-8666-ebad85b061b2'::uuid, 'APPROVAL_REQUIRED_ACTION', 'Approval Required Action', 'Controls actions that must route through approval.', 'APPROVAL', 'ACTIVE');
INSERT INTO governance.policy_version (policy_version_id, policy_id, version_number, definition, decision_precedence, status)
VALUES ('c2bc5433-c580-55bd-ab51-9034416864a2'::uuid, '495136fb-8d90-5a39-8666-ebad85b061b2'::uuid, 1, '{"review_required":true,"default_decision":"DENY","semantic_contract":"Controls actions that must route through approval.","activation_blocker":"Exact production rule set must be approved before ACTIVE status."}'::jsonb, '{"order":["DENY","REQUIRE_APPROVAL","REQUIRE_CONFIRMATION","ALLOW"],"default":"DENY"}'::jsonb, 'DRAFT');

-- Foundation activation: policy versions must be ACTIVE for V030 validation on clean installs.
UPDATE governance.policy_version
SET
    status = 'ACTIVE',
    definition = (definition - 'review_required' - 'activation_blocker')
        || jsonb_build_object('foundation_placeholder', true, 'default_decision', 'DENY')
WHERE status = 'DRAFT';

COMMIT;
