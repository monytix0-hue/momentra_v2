BEGIN;

-- Capability catalogue. Per-type mappings for the known Group families use the frozen master Quick Add sets.
-- Exact historical per-subtype subset differences were not recoverable in the current source context; review manifest before production RC.

INSERT INTO core.capability (capability_id, code, display_name, description, owning_service, resource_type, action_type, sensitivity_level, approval_eligible, status)
VALUES ('15020a90-ef38-5cdc-80a0-df4115b9062b'::uuid, 'MOMENT_VIEW', 'View Moment', 'View Moment capability.', 'CORE', 'MOMENT', 'VIEW', 'STANDARD', false, 'ACTIVE');

INSERT INTO core.capability (capability_id, code, display_name, description, owning_service, resource_type, action_type, sensitivity_level, approval_eligible, status)
VALUES ('1ee95342-36aa-5a40-9ad0-68f8ffd299d8'::uuid, 'MOMENT_UPDATE', 'Update Moment', 'Update Moment capability.', 'CORE', 'MOMENT', 'UPDATE', 'STANDARD', false, 'ACTIVE');

INSERT INTO core.capability (capability_id, code, display_name, description, owning_service, resource_type, action_type, sensitivity_level, approval_eligible, status)
VALUES ('ea163c55-9f18-578c-9215-bc19a7254fba'::uuid, 'PARTICIPANT_MANAGE', 'Manage Participant', 'Manage Participant capability.', 'COLLABORATION', 'PARTICIPANT', 'MANAGE', 'SENSITIVE', false, 'ACTIVE');

INSERT INTO core.capability (capability_id, code, display_name, description, owning_service, resource_type, action_type, sensitivity_level, approval_eligible, status)
VALUES ('a4135e91-c9a7-5fe0-8be7-4b8ca97abc64'::uuid, 'PLANNING_ITEM_CREATE', 'Add Planning Item', 'Add Planning Item capability.', 'COLLABORATION', 'PLANNING_ITEM', 'CREATE', 'STANDARD', false, 'ACTIVE');

INSERT INTO core.capability (capability_id, code, display_name, description, owning_service, resource_type, action_type, sensitivity_level, approval_eligible, status)
VALUES ('0344b3fb-ba83-5480-9a3b-2eb36eaac1f0'::uuid, 'BOOKING_CREATE', 'Add Booking', 'Add Booking capability.', 'COLLABORATION', 'BOOKING', 'CREATE', 'SENSITIVE', false, 'ACTIVE');

INSERT INTO core.capability (capability_id, code, display_name, description, owning_service, resource_type, action_type, sensitivity_level, approval_eligible, status)
VALUES ('d6ee55d7-2f96-555e-b12f-d0eb520bd0df'::uuid, 'EXPENSE_CREATE', 'Add Expense', 'Add Expense capability.', 'FINANCE', 'EXPENSE', 'CREATE', 'SENSITIVE', true, 'ACTIVE');

INSERT INTO core.capability (capability_id, code, display_name, description, owning_service, resource_type, action_type, sensitivity_level, approval_eligible, status)
VALUES ('0fa0f706-9ffc-5dbc-9c8a-fa5d65983676'::uuid, 'BUDGET_MANAGE', 'Manage Budget', 'Manage Budget capability.', 'FINANCE', 'BUDGET', 'MANAGE', 'SENSITIVE', true, 'ACTIVE');

INSERT INTO core.capability (capability_id, code, display_name, description, owning_service, resource_type, action_type, sensitivity_level, approval_eligible, status)
VALUES ('f2bd62d0-6aa8-51ff-812a-6f1181a8d19f'::uuid, 'CONTRIBUTION_RECORD', 'Record Contribution', 'Record Contribution capability.', 'FINANCE', 'CONTRIBUTION', 'RECORD', 'SENSITIVE', false, 'ACTIVE');

INSERT INTO core.capability (capability_id, code, display_name, description, owning_service, resource_type, action_type, sensitivity_level, approval_eligible, status)
VALUES ('4730fee0-af11-552b-b05b-556f4f33f616'::uuid, 'SETTLEMENT_RECORD', 'Record Settlement', 'Record Settlement capability.', 'FINANCE', 'SETTLEMENT', 'RECORD', 'HIGHLY_SENSITIVE', true, 'ACTIVE');

INSERT INTO core.capability (capability_id, code, display_name, description, owning_service, resource_type, action_type, sensitivity_level, approval_eligible, status)
VALUES ('b7454817-9da3-5738-af31-1a98c561a916'::uuid, 'VENDOR_MANAGE', 'Manage Vendor', 'Manage Vendor capability.', 'BUSINESS', 'VENDOR', 'MANAGE', 'SENSITIVE', true, 'ACTIVE');

INSERT INTO core.capability (capability_id, code, display_name, description, owning_service, resource_type, action_type, sensitivity_level, approval_eligible, status)
VALUES ('2ea883df-5db9-5816-bf1e-16395065b814'::uuid, 'UPDATE_CREATE', 'Add Update', 'Add Update capability.', 'COLLABORATION', 'UPDATE', 'CREATE', 'STANDARD', false, 'ACTIVE');

INSERT INTO core.capability (capability_id, code, display_name, description, owning_service, resource_type, action_type, sensitivity_level, approval_eligible, status)
VALUES ('6361b3db-2bbb-5a1d-9035-bd931753d9f8'::uuid, 'POLL_CREATE', 'Create Poll', 'Create Poll capability.', 'COLLABORATION', 'POLL', 'CREATE', 'STANDARD', false, 'ACTIVE');

INSERT INTO core.capability (capability_id, code, display_name, description, owning_service, resource_type, action_type, sensitivity_level, approval_eligible, status)
VALUES ('b3d5b58c-b52d-57ed-9eff-e99717bd7685'::uuid, 'MEMORY_CREATE', 'Create Memory', 'Create Memory capability.', 'MEMORY', 'MEMORY', 'CREATE', 'SENSITIVE', false, 'ACTIVE');

INSERT INTO core.capability (capability_id, code, display_name, description, owning_service, resource_type, action_type, sensitivity_level, approval_eligible, status)
VALUES ('e23a7487-040c-555a-802f-af27accf9acc'::uuid, 'ATTENDANCE_RECORD', 'Record Attendance', 'Record Attendance capability.', 'COLLABORATION', 'ATTENDANCE', 'RECORD', 'STANDARD', false, 'ACTIVE');

INSERT INTO core.capability (capability_id, code, display_name, description, owning_service, resource_type, action_type, sensitivity_level, approval_eligible, status)
VALUES ('34965dd6-c0a4-5555-80a6-6090b9041083'::uuid, 'PURCHASE_ITEM_CREATE', 'Add Purchase Item', 'Add Purchase Item capability.', 'COLLABORATION', 'PURCHASE_ITEM', 'CREATE', 'STANDARD', false, 'ACTIVE');

INSERT INTO core.capability (capability_id, code, display_name, description, owning_service, resource_type, action_type, sensitivity_level, approval_eligible, status)
VALUES ('fa4a7e1a-93cc-5def-8b8f-de7427642c34'::uuid, 'OWNERSHIP_MANAGE', 'Manage Ownership', 'Manage Ownership capability.', 'COLLABORATION', 'OWNERSHIP', 'MANAGE', 'SENSITIVE', false, 'ACTIVE');

INSERT INTO core.capability (capability_id, code, display_name, description, owning_service, resource_type, action_type, sensitivity_level, approval_eligible, status)
VALUES ('e800e788-5cbe-5664-a701-05aef9d1f247'::uuid, 'DELIVERY_HANDOVER_RECORD', 'Record Delivery / Handover', 'Record Delivery / Handover capability.', 'COLLABORATION', 'DELIVERY_HANDOVER', 'RECORD', 'STANDARD', false, 'ACTIVE');

INSERT INTO core.capability (capability_id, code, display_name, description, owning_service, resource_type, action_type, sensitivity_level, approval_eligible, status)
VALUES ('7a069df9-69c8-56fa-b1aa-44999ad6e5dc'::uuid, 'RESIDENT_MANAGE', 'Manage Resident', 'Manage Resident capability.', 'COLLABORATION', 'RESIDENT', 'MANAGE', 'SENSITIVE', false, 'ACTIVE');

INSERT INTO core.capability (capability_id, code, display_name, description, owning_service, resource_type, action_type, sensitivity_level, approval_eligible, status)
VALUES ('56dee392-89c0-52bc-ab95-2346f750fb45'::uuid, 'TASK_CREATE', 'Create Task', 'Create Task capability.', 'WORK', 'TASK', 'CREATE', 'STANDARD', false, 'ACTIVE');

INSERT INTO core.capability (capability_id, code, display_name, description, owning_service, resource_type, action_type, sensitivity_level, approval_eligible, status)
VALUES ('40aa63fd-d850-54bc-91f7-30145d369a03'::uuid, 'TASK_ASSIGN', 'Assign Task', 'Assign Task capability.', 'WORK', 'TASK', 'MANAGE', 'STANDARD', false, 'ACTIVE');

INSERT INTO core.capability (capability_id, code, display_name, description, owning_service, resource_type, action_type, sensitivity_level, approval_eligible, status)
VALUES ('2107eb71-ddd6-5261-b8c7-a1a941ac8afa'::uuid, 'RULE_MANAGE', 'Manage Rule', 'Manage Rule capability.', 'COLLABORATION', 'RULE', 'MANAGE', 'STANDARD', false, 'ACTIVE');

INSERT INTO core.capability (capability_id, code, display_name, description, owning_service, resource_type, action_type, sensitivity_level, approval_eligible, status)
VALUES ('5811f1ab-c4ec-5e51-aa3f-3376ebda16fd'::uuid, 'ASSET_MANAGE', 'Manage Asset', 'Manage Asset capability.', 'COLLABORATION', 'ASSET', 'MANAGE', 'SENSITIVE', false, 'ACTIVE');

INSERT INTO core.capability (capability_id, code, display_name, description, owning_service, resource_type, action_type, sensitivity_level, approval_eligible, status)
VALUES ('2cd766aa-0606-513d-ac75-a324541cbb85'::uuid, 'MAINTENANCE_CREATE', 'Add Maintenance', 'Add Maintenance capability.', 'COLLABORATION', 'MAINTENANCE', 'CREATE', 'STANDARD', false, 'ACTIVE');

INSERT INTO core.capability (capability_id, code, display_name, description, owning_service, resource_type, action_type, sensitivity_level, approval_eligible, status)
VALUES ('f6eb8c32-d911-55e0-9e25-11ab94987642'::uuid, 'GOAL_CREATE', 'Create Goal', 'Create Goal capability.', 'WORK', 'GOAL', 'CREATE', 'STANDARD', false, 'ACTIVE');

INSERT INTO core.capability (capability_id, code, display_name, description, owning_service, resource_type, action_type, sensitivity_level, approval_eligible, status)
VALUES ('fda9d489-770c-530b-bfa9-454eb2008148'::uuid, 'MILESTONE_CREATE', 'Create Milestone', 'Create Milestone capability.', 'WORK', 'MILESTONE', 'CREATE', 'STANDARD', false, 'ACTIVE');

INSERT INTO core.capability (capability_id, code, display_name, description, owning_service, resource_type, action_type, sensitivity_level, approval_eligible, status)
VALUES ('e466124e-4121-54bb-b9bd-5e8a52a6fff4'::uuid, 'PROGRESS_RECORD', 'Record Progress', 'Record Progress capability.', 'WORK', 'PROGRESS', 'RECORD', 'STANDARD', false, 'ACTIVE');

INSERT INTO core.capability (capability_id, code, display_name, description, owning_service, resource_type, action_type, sensitivity_level, approval_eligible, status)
VALUES ('1fdaa018-b654-5c7f-91bc-cff75d60bfa3'::uuid, 'OPPORTUNITY_CREATE', 'Create Opportunity', 'Create Opportunity capability.', 'PERSONAL', 'OPPORTUNITY', 'CREATE', 'SENSITIVE', false, 'ACTIVE');

INSERT INTO core.capability (capability_id, code, display_name, description, owning_service, resource_type, action_type, sensitivity_level, approval_eligible, status)
VALUES ('21fc722b-1ab4-5568-ba67-3ebdcaac2f43'::uuid, 'PIVOT_RECORD', 'Record Pivot', 'Record Pivot capability.', 'PERSONAL', 'PIVOT', 'RECORD', 'SENSITIVE', false, 'ACTIVE');

INSERT INTO core.capability (capability_id, code, display_name, description, owning_service, resource_type, action_type, sensitivity_level, approval_eligible, status)
VALUES ('2d4caa9c-e1c9-59b8-8c8d-dbabca146e3e'::uuid, 'LEARNING_ACTIVITY_CREATE', 'Add Learning Activity', 'Add Learning Activity capability.', 'PERSONAL', 'LEARNING_ACTIVITY', 'CREATE', 'STANDARD', false, 'ACTIVE');

INSERT INTO core.capability (capability_id, code, display_name, description, owning_service, resource_type, action_type, sensitivity_level, approval_eligible, status)
VALUES ('ef239ed1-dcae-53a5-b87e-7e7a0467e9cf'::uuid, 'LIFE_OBSERVATION_RECORD', 'Record Life Observation', 'Record Life Observation capability.', 'PERSONAL', 'LIFE_OBSERVATION', 'RECORD', 'HIGHLY_SENSITIVE', false, 'ACTIVE');

INSERT INTO core.capability (capability_id, code, display_name, description, owning_service, resource_type, action_type, sensitivity_level, approval_eligible, status)
VALUES ('427f49cc-095f-5cf1-8cae-32db2a33fbda'::uuid, 'LIFESTYLE_ACTIVITY_CREATE', 'Add Lifestyle Activity', 'Add Lifestyle Activity capability.', 'PERSONAL', 'LIFESTYLE_ACTIVITY', 'CREATE', 'SENSITIVE', false, 'ACTIVE');

INSERT INTO core.capability (capability_id, code, display_name, description, owning_service, resource_type, action_type, sensitivity_level, approval_eligible, status)
VALUES ('f17fa89e-7c51-5cd1-9b6a-d8e98507c9aa'::uuid, 'RELATIONSHIP_ACTIVITY_RECORD', 'Record Relationship Activity', 'Record Relationship Activity capability.', 'PERSONAL', 'RELATIONSHIP_ACTIVITY', 'RECORD', 'HIGHLY_SENSITIVE', false, 'ACTIVE');

INSERT INTO core.capability (capability_id, code, display_name, description, owning_service, resource_type, action_type, sensitivity_level, approval_eligible, status)
VALUES ('052569d5-c6b8-5a16-ad75-f9b272260e58'::uuid, 'ISSUE_CREATE', 'Create Issue', 'Create Issue capability.', 'BUSINESS', 'ISSUE', 'CREATE', 'SENSITIVE', false, 'ACTIVE');

INSERT INTO core.capability (capability_id, code, display_name, description, owning_service, resource_type, action_type, sensitivity_level, approval_eligible, status)
VALUES ('13688561-48e6-5cfa-b9d8-64ca417d9679'::uuid, 'RISK_CREATE', 'Create Risk', 'Create Risk capability.', 'BUSINESS', 'RISK', 'CREATE', 'SENSITIVE', true, 'ACTIVE');

INSERT INTO core.capability (capability_id, code, display_name, description, owning_service, resource_type, action_type, sensitivity_level, approval_eligible, status)
VALUES ('932c295d-49cf-5c0b-a2a3-7f3b5c61311b'::uuid, 'DECISION_RECORD', 'Record Decision', 'Record Decision capability.', 'BUSINESS', 'DECISION', 'RECORD', 'SENSITIVE', true, 'ACTIVE');

INSERT INTO core.capability (capability_id, code, display_name, description, owning_service, resource_type, action_type, sensitivity_level, approval_eligible, status)
VALUES ('78880073-69b4-5ce0-86cc-8f53d987a7e6'::uuid, 'REVENUE_RECORD', 'Record Revenue', 'Record Revenue capability.', 'FINANCE', 'REVENUE', 'RECORD', 'HIGHLY_SENSITIVE', false, 'ACTIVE');

INSERT INTO core.capability (capability_id, code, display_name, description, owning_service, resource_type, action_type, sensitivity_level, approval_eligible, status)
VALUES ('d6c7a349-9547-50b5-b92f-146c47df0246'::uuid, 'INVOICE_CREATE', 'Create Invoice', 'Create Invoice capability.', 'FINANCE', 'INVOICE', 'CREATE', 'HIGHLY_SENSITIVE', true, 'ACTIVE');

INSERT INTO core.capability (capability_id, code, display_name, description, owning_service, resource_type, action_type, sensitivity_level, approval_eligible, status)
VALUES ('f959daf7-7cb8-5342-b594-57b9a165a97c'::uuid, 'SLA_MANAGE', 'Manage SLA', 'Manage SLA capability.', 'BUSINESS', 'SLA', 'MANAGE', 'SENSITIVE', true, 'ACTIVE');

INSERT INTO core.capability (capability_id, code, display_name, description, owning_service, resource_type, action_type, sensitivity_level, approval_eligible, status)
VALUES ('da92aee8-b16a-5a47-95c7-8425b40c919a'::uuid, 'REVIEW_CREATE', 'Create Review', 'Create Review capability.', 'BUSINESS', 'REVIEW', 'CREATE', 'STANDARD', false, 'ACTIVE');

INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('321c44d8-c5cb-5ecc-8c08-bc5ef5944581'::uuid, '891ad9b7-4246-52ad-9d48-aacc991d4caa'::uuid, '15020a90-ef38-5cdc-80a0-df4115b9062b'::uuid, true, 10, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('39f5e896-4129-5892-8af5-3fb5adcf98e2'::uuid, '891ad9b7-4246-52ad-9d48-aacc991d4caa'::uuid, '1ee95342-36aa-5a40-9ad0-68f8ffd299d8'::uuid, true, 11, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('bd1017d6-4d4c-587b-911f-bdb2e3f4f4b8'::uuid, '891ad9b7-4246-52ad-9d48-aacc991d4caa'::uuid, 'ef239ed1-dcae-53a5-b87e-7e7a0467e9cf'::uuid, true, 12, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('41565277-13b4-5433-b848-a57901f23c69'::uuid, '891ad9b7-4246-52ad-9d48-aacc991d4caa'::uuid, 'd6ee55d7-2f96-555e-b12f-d0eb520bd0df'::uuid, true, 13, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('56933a52-5c2e-5da8-802a-a61f73fa3188'::uuid, '891ad9b7-4246-52ad-9d48-aacc991d4caa'::uuid, '56dee392-89c0-52bc-ab95-2346f750fb45'::uuid, true, 14, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('063e6e73-9028-5cf7-957c-bebe68ffe9b4'::uuid, '891ad9b7-4246-52ad-9d48-aacc991d4caa'::uuid, 'b3d5b58c-b52d-57ed-9eff-e99717bd7685'::uuid, true, 15, 'ACTIVE');

INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('50c3b51d-f4de-5761-95d0-43f0bfd544d8'::uuid, '040dd2a3-3429-542b-8560-1141f55cacd6'::uuid, '15020a90-ef38-5cdc-80a0-df4115b9062b'::uuid, true, 10, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('21d566bc-7e24-54bd-9f0a-ec93b851c134'::uuid, '040dd2a3-3429-542b-8560-1141f55cacd6'::uuid, '1ee95342-36aa-5a40-9ad0-68f8ffd299d8'::uuid, true, 11, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('9f7969ca-058d-5ad6-b97e-ccef28263cd4'::uuid, '040dd2a3-3429-542b-8560-1141f55cacd6'::uuid, 'ef239ed1-dcae-53a5-b87e-7e7a0467e9cf'::uuid, true, 12, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('0f930ebe-1d00-5290-863c-9ae65630c112'::uuid, '040dd2a3-3429-542b-8560-1141f55cacd6'::uuid, 'd6ee55d7-2f96-555e-b12f-d0eb520bd0df'::uuid, true, 13, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('649d2acb-3f9c-5493-a8d9-b741fc49d2d8'::uuid, '040dd2a3-3429-542b-8560-1141f55cacd6'::uuid, '56dee392-89c0-52bc-ab95-2346f750fb45'::uuid, true, 14, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('47e849d1-d016-5cdd-8315-8d4ce6b5e415'::uuid, '040dd2a3-3429-542b-8560-1141f55cacd6'::uuid, 'b3d5b58c-b52d-57ed-9eff-e99717bd7685'::uuid, true, 15, 'ACTIVE');

INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('7106bc55-06d0-53b0-b408-7c1607870e85'::uuid, '23974b6a-55f8-5b81-af53-aa0766b29e6a'::uuid, '15020a90-ef38-5cdc-80a0-df4115b9062b'::uuid, true, 10, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('62f89789-64ea-5a1f-85a6-3c3a279cf52a'::uuid, '23974b6a-55f8-5b81-af53-aa0766b29e6a'::uuid, '1ee95342-36aa-5a40-9ad0-68f8ffd299d8'::uuid, true, 11, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('493f598d-ec6a-550d-944d-d33981356a0d'::uuid, '23974b6a-55f8-5b81-af53-aa0766b29e6a'::uuid, 'ef239ed1-dcae-53a5-b87e-7e7a0467e9cf'::uuid, true, 12, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('f7a1f76d-54a6-55fc-bda0-3e59db1cf398'::uuid, '23974b6a-55f8-5b81-af53-aa0766b29e6a'::uuid, 'd6ee55d7-2f96-555e-b12f-d0eb520bd0df'::uuid, true, 13, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('939b7019-ac14-54a6-8100-1f8e5935096f'::uuid, '23974b6a-55f8-5b81-af53-aa0766b29e6a'::uuid, '56dee392-89c0-52bc-ab95-2346f750fb45'::uuid, true, 14, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('6dbb0a3c-56a0-58c2-a812-776cbaf3e92b'::uuid, '23974b6a-55f8-5b81-af53-aa0766b29e6a'::uuid, 'b3d5b58c-b52d-57ed-9eff-e99717bd7685'::uuid, true, 15, 'ACTIVE');

INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('7d0de742-cf1e-5e6c-8557-b80152b244e6'::uuid, '0e2cfd13-fcd4-5bd4-8215-6164aae0a330'::uuid, '15020a90-ef38-5cdc-80a0-df4115b9062b'::uuid, true, 10, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('cb9e21ec-db12-55fe-96ea-51dcd559508a'::uuid, '0e2cfd13-fcd4-5bd4-8215-6164aae0a330'::uuid, '1ee95342-36aa-5a40-9ad0-68f8ffd299d8'::uuid, true, 11, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('29b81ae1-9ce6-534e-b1f3-db1f368380d4'::uuid, '0e2cfd13-fcd4-5bd4-8215-6164aae0a330'::uuid, 'ef239ed1-dcae-53a5-b87e-7e7a0467e9cf'::uuid, true, 12, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('c0c76e16-0f5f-5c77-b3af-8830a181765d'::uuid, '0e2cfd13-fcd4-5bd4-8215-6164aae0a330'::uuid, 'd6ee55d7-2f96-555e-b12f-d0eb520bd0df'::uuid, true, 13, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('783213dc-d656-5739-a88d-ad2aa3190bdd'::uuid, '0e2cfd13-fcd4-5bd4-8215-6164aae0a330'::uuid, '56dee392-89c0-52bc-ab95-2346f750fb45'::uuid, true, 14, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('db5b7983-6ef1-58ad-a6c5-b4d268e186c4'::uuid, '0e2cfd13-fcd4-5bd4-8215-6164aae0a330'::uuid, 'b3d5b58c-b52d-57ed-9eff-e99717bd7685'::uuid, true, 15, 'ACTIVE');

INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('b4b48285-2a3c-5faa-b566-bd998ee2566d'::uuid, '5015fd32-d116-55c6-9fe5-5e306f7f8988'::uuid, '15020a90-ef38-5cdc-80a0-df4115b9062b'::uuid, true, 10, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('f79bd787-5a40-5b0d-8263-ba5859e5f0d2'::uuid, '5015fd32-d116-55c6-9fe5-5e306f7f8988'::uuid, '1ee95342-36aa-5a40-9ad0-68f8ffd299d8'::uuid, true, 11, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('591af589-74a6-54a8-8838-a1aee3116d01'::uuid, '5015fd32-d116-55c6-9fe5-5e306f7f8988'::uuid, 'f6eb8c32-d911-55e0-9e25-11ab94987642'::uuid, true, 12, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('25335f08-8883-59df-a854-60ba146f5002'::uuid, '5015fd32-d116-55c6-9fe5-5e306f7f8988'::uuid, 'fda9d489-770c-530b-bfa9-454eb2008148'::uuid, true, 13, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('a0dc1ccf-b05b-5349-904e-24543d1ec2af'::uuid, '5015fd32-d116-55c6-9fe5-5e306f7f8988'::uuid, 'e466124e-4121-54bb-b9bd-5e8a52a6fff4'::uuid, true, 14, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('94061913-4f26-5096-a42b-e400cb4635d8'::uuid, '5015fd32-d116-55c6-9fe5-5e306f7f8988'::uuid, '56dee392-89c0-52bc-ab95-2346f750fb45'::uuid, true, 15, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('ce2d5af5-a5f1-5c9c-8f76-af5b16fea00d'::uuid, '5015fd32-d116-55c6-9fe5-5e306f7f8988'::uuid, 'd6ee55d7-2f96-555e-b12f-d0eb520bd0df'::uuid, true, 16, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('9758d693-f151-55b1-96fd-a4983c717567'::uuid, '5015fd32-d116-55c6-9fe5-5e306f7f8988'::uuid, 'b3d5b58c-b52d-57ed-9eff-e99717bd7685'::uuid, true, 17, 'ACTIVE');

INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('254798da-b1fe-5c70-8240-218761f0b62f'::uuid, '43654aea-824d-55df-aa8c-32a88b8a54af'::uuid, '15020a90-ef38-5cdc-80a0-df4115b9062b'::uuid, true, 10, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('976771a2-0bce-56ea-ab6a-cbad41ef0e14'::uuid, '43654aea-824d-55df-aa8c-32a88b8a54af'::uuid, '1ee95342-36aa-5a40-9ad0-68f8ffd299d8'::uuid, true, 11, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('020ebb36-0ce8-528e-8f98-93fa045ce5d7'::uuid, '43654aea-824d-55df-aa8c-32a88b8a54af'::uuid, 'fda9d489-770c-530b-bfa9-454eb2008148'::uuid, true, 12, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('8f808727-1e3d-542d-a21d-f4b44a9ceaca'::uuid, '43654aea-824d-55df-aa8c-32a88b8a54af'::uuid, 'e466124e-4121-54bb-b9bd-5e8a52a6fff4'::uuid, true, 13, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('0404e533-7d27-5053-8181-9106ab0f9c5f'::uuid, '43654aea-824d-55df-aa8c-32a88b8a54af'::uuid, '56dee392-89c0-52bc-ab95-2346f750fb45'::uuid, true, 14, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('6c2451b4-3fc5-5d7a-8001-e80cc468c73a'::uuid, '43654aea-824d-55df-aa8c-32a88b8a54af'::uuid, 'd6ee55d7-2f96-555e-b12f-d0eb520bd0df'::uuid, true, 15, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('3a1940b5-29f2-53bb-8b6c-2a6c86db7bb0'::uuid, '43654aea-824d-55df-aa8c-32a88b8a54af'::uuid, 'b3d5b58c-b52d-57ed-9eff-e99717bd7685'::uuid, true, 16, 'ACTIVE');

INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('316a2c56-5510-5ef5-a11b-d17c4b564736'::uuid, '037485d1-e163-5e4f-8c70-6271eb32cf5d'::uuid, '15020a90-ef38-5cdc-80a0-df4115b9062b'::uuid, true, 10, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('8590ed21-933f-5d64-9935-0babd2e79464'::uuid, '037485d1-e163-5e4f-8c70-6271eb32cf5d'::uuid, '1ee95342-36aa-5a40-9ad0-68f8ffd299d8'::uuid, true, 11, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('53702c33-55cc-5164-adb5-15f2e32eb359'::uuid, '037485d1-e163-5e4f-8c70-6271eb32cf5d'::uuid, 'fda9d489-770c-530b-bfa9-454eb2008148'::uuid, true, 12, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('d6db1db3-b366-5370-bbbb-2d7d84011356'::uuid, '037485d1-e163-5e4f-8c70-6271eb32cf5d'::uuid, 'e466124e-4121-54bb-b9bd-5e8a52a6fff4'::uuid, true, 13, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('0fd13c75-15ea-5561-a1af-3ed53ff83abe'::uuid, '037485d1-e163-5e4f-8c70-6271eb32cf5d'::uuid, '56dee392-89c0-52bc-ab95-2346f750fb45'::uuid, true, 14, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('74f04a10-9d3e-5b21-ad99-4bc06860c760'::uuid, '037485d1-e163-5e4f-8c70-6271eb32cf5d'::uuid, 'd6ee55d7-2f96-555e-b12f-d0eb520bd0df'::uuid, true, 15, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('5b5a7e02-d966-56f5-9a12-f8ac0fa9f1b4'::uuid, '037485d1-e163-5e4f-8c70-6271eb32cf5d'::uuid, 'b3d5b58c-b52d-57ed-9eff-e99717bd7685'::uuid, true, 16, 'ACTIVE');

INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('8cd82748-1c2b-5304-b594-241a82be24f3'::uuid, 'a9213c15-1045-55ba-9332-1765479a66a9'::uuid, '15020a90-ef38-5cdc-80a0-df4115b9062b'::uuid, true, 10, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('72024f18-3a36-55cc-9965-e00a0815c897'::uuid, 'a9213c15-1045-55ba-9332-1765479a66a9'::uuid, '1ee95342-36aa-5a40-9ad0-68f8ffd299d8'::uuid, true, 11, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('44d3c4d1-5a18-510e-89ae-468ca2eb7ac5'::uuid, 'a9213c15-1045-55ba-9332-1765479a66a9'::uuid, '1fdaa018-b654-5c7f-91bc-cff75d60bfa3'::uuid, true, 12, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('6b762e8a-9326-5352-b5a7-6abf0a13210c'::uuid, 'a9213c15-1045-55ba-9332-1765479a66a9'::uuid, 'f6eb8c32-d911-55e0-9e25-11ab94987642'::uuid, true, 13, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('609f71c9-4f78-5032-b44f-516f88531809'::uuid, 'a9213c15-1045-55ba-9332-1765479a66a9'::uuid, '56dee392-89c0-52bc-ab95-2346f750fb45'::uuid, true, 14, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('00af2c92-6b5a-5849-8a31-a02e4d58191a'::uuid, 'a9213c15-1045-55ba-9332-1765479a66a9'::uuid, 'd6ee55d7-2f96-555e-b12f-d0eb520bd0df'::uuid, true, 15, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('4f55967b-a4f2-55e0-bdfe-e95534324725'::uuid, 'a9213c15-1045-55ba-9332-1765479a66a9'::uuid, 'b3d5b58c-b52d-57ed-9eff-e99717bd7685'::uuid, true, 16, 'ACTIVE');

INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('15e12f2b-665e-566a-8c88-8b45168bfc95'::uuid, 'e8d4b0b5-d5cb-5136-bb01-e039941faf87'::uuid, '15020a90-ef38-5cdc-80a0-df4115b9062b'::uuid, true, 10, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('623d4002-a2d1-5d0e-b143-96ee6a88b3e9'::uuid, 'e8d4b0b5-d5cb-5136-bb01-e039941faf87'::uuid, '1ee95342-36aa-5a40-9ad0-68f8ffd299d8'::uuid, true, 11, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('88ca446b-caaf-5fc8-a3b3-6204a382d917'::uuid, 'e8d4b0b5-d5cb-5136-bb01-e039941faf87'::uuid, '21fc722b-1ab4-5568-ba67-3ebdcaac2f43'::uuid, true, 12, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('c80aa38a-39a7-54bd-885c-b6e4a5478922'::uuid, 'e8d4b0b5-d5cb-5136-bb01-e039941faf87'::uuid, 'f6eb8c32-d911-55e0-9e25-11ab94987642'::uuid, true, 13, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('85c9c709-782c-5c21-93d5-e277ac66282a'::uuid, 'e8d4b0b5-d5cb-5136-bb01-e039941faf87'::uuid, '56dee392-89c0-52bc-ab95-2346f750fb45'::uuid, true, 14, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('a5bf2a71-c598-5e64-b30b-45cd29f6ef47'::uuid, 'e8d4b0b5-d5cb-5136-bb01-e039941faf87'::uuid, 'b3d5b58c-b52d-57ed-9eff-e99717bd7685'::uuid, true, 15, 'ACTIVE');

INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('3e0bf9ad-b934-5f7b-87da-6df7cfed7670'::uuid, '90cde495-1c97-5ded-871c-ad94e06ec7b7'::uuid, '15020a90-ef38-5cdc-80a0-df4115b9062b'::uuid, true, 10, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('a9050629-bee4-5dc1-8b46-d9fc5fcc2ee1'::uuid, '90cde495-1c97-5ded-871c-ad94e06ec7b7'::uuid, '1ee95342-36aa-5a40-9ad0-68f8ffd299d8'::uuid, true, 11, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('fa9a151b-957a-51d0-8977-2e4d2adfbb6a'::uuid, '90cde495-1c97-5ded-871c-ad94e06ec7b7'::uuid, '2d4caa9c-e1c9-59b8-8c8d-dbabca146e3e'::uuid, true, 12, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('57455ac5-a8a5-51b2-baaf-fe1bc1a0fd29'::uuid, '90cde495-1c97-5ded-871c-ad94e06ec7b7'::uuid, '56dee392-89c0-52bc-ab95-2346f750fb45'::uuid, true, 13, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('4f34604b-9260-50ec-94fa-4859b236ad97'::uuid, '90cde495-1c97-5ded-871c-ad94e06ec7b7'::uuid, 'd6ee55d7-2f96-555e-b12f-d0eb520bd0df'::uuid, true, 14, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('acb6d478-db2e-503e-9b30-e10a9705c09d'::uuid, '90cde495-1c97-5ded-871c-ad94e06ec7b7'::uuid, 'b3d5b58c-b52d-57ed-9eff-e99717bd7685'::uuid, true, 15, 'ACTIVE');

INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('4b0e0d06-f2ae-5abc-aa01-a98842573de0'::uuid, '615158c2-7cc5-5479-85ff-19b9c62f0eb5'::uuid, '15020a90-ef38-5cdc-80a0-df4115b9062b'::uuid, true, 10, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('bf507f36-72f3-50dd-8d0c-95c4cad266ad'::uuid, '615158c2-7cc5-5479-85ff-19b9c62f0eb5'::uuid, '1ee95342-36aa-5a40-9ad0-68f8ffd299d8'::uuid, true, 11, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('63594270-72ab-58e3-bb39-a2f598c4e4c5'::uuid, '615158c2-7cc5-5479-85ff-19b9c62f0eb5'::uuid, '427f49cc-095f-5cf1-8cae-32db2a33fbda'::uuid, true, 12, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('a8256431-7dad-554a-8e22-d3bf4dc0e503'::uuid, '615158c2-7cc5-5479-85ff-19b9c62f0eb5'::uuid, 'd6ee55d7-2f96-555e-b12f-d0eb520bd0df'::uuid, true, 13, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('92910811-f145-5857-98a9-184ca82caf91'::uuid, '615158c2-7cc5-5479-85ff-19b9c62f0eb5'::uuid, '56dee392-89c0-52bc-ab95-2346f750fb45'::uuid, true, 14, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('6871e74d-4ad1-5089-b340-22af1128285b'::uuid, '615158c2-7cc5-5479-85ff-19b9c62f0eb5'::uuid, 'b3d5b58c-b52d-57ed-9eff-e99717bd7685'::uuid, true, 15, 'ACTIVE');

INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('a7f569dd-f369-5a67-85fc-6eedd3f73f18'::uuid, '40114d71-353e-5e0f-9c39-0d048b573a5d'::uuid, '15020a90-ef38-5cdc-80a0-df4115b9062b'::uuid, true, 10, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('1c768e1d-e6e3-5d20-b31b-81e344f9b7ad'::uuid, '40114d71-353e-5e0f-9c39-0d048b573a5d'::uuid, '1ee95342-36aa-5a40-9ad0-68f8ffd299d8'::uuid, true, 11, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('8aef61a7-d742-5c97-9d58-03625335ccac'::uuid, '40114d71-353e-5e0f-9c39-0d048b573a5d'::uuid, '427f49cc-095f-5cf1-8cae-32db2a33fbda'::uuid, true, 12, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('057a9f94-77cd-5d23-99af-db1e2287988a'::uuid, '40114d71-353e-5e0f-9c39-0d048b573a5d'::uuid, 'd6ee55d7-2f96-555e-b12f-d0eb520bd0df'::uuid, true, 13, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('5c4e4f7e-8fd1-5dd6-ae7c-b8bbc5934ba0'::uuid, '40114d71-353e-5e0f-9c39-0d048b573a5d'::uuid, '56dee392-89c0-52bc-ab95-2346f750fb45'::uuid, true, 14, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('c80551b4-e9fc-5568-bcdb-5ee13f04b8a0'::uuid, '40114d71-353e-5e0f-9c39-0d048b573a5d'::uuid, 'b3d5b58c-b52d-57ed-9eff-e99717bd7685'::uuid, true, 15, 'ACTIVE');

INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('a60c13dd-1ce8-5de1-8dc8-9b63ab53e3a3'::uuid, '15281777-5f46-55db-afec-3955862e8552'::uuid, '15020a90-ef38-5cdc-80a0-df4115b9062b'::uuid, true, 10, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('157d583f-3169-56bd-bd1b-93fb1179353f'::uuid, '15281777-5f46-55db-afec-3955862e8552'::uuid, '1ee95342-36aa-5a40-9ad0-68f8ffd299d8'::uuid, true, 11, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('0b24292c-36a2-574d-869b-d8c2c3775904'::uuid, '15281777-5f46-55db-afec-3955862e8552'::uuid, '427f49cc-095f-5cf1-8cae-32db2a33fbda'::uuid, true, 12, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('2ca9e982-ee3e-50f8-b90a-d5ffbcb07d21'::uuid, '15281777-5f46-55db-afec-3955862e8552'::uuid, 'd6ee55d7-2f96-555e-b12f-d0eb520bd0df'::uuid, true, 13, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('03f35c90-50fc-506d-80e4-cc11bf4af233'::uuid, '15281777-5f46-55db-afec-3955862e8552'::uuid, '56dee392-89c0-52bc-ab95-2346f750fb45'::uuid, true, 14, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('369c663a-8ef0-54fd-8f02-dd0427768080'::uuid, '15281777-5f46-55db-afec-3955862e8552'::uuid, 'b3d5b58c-b52d-57ed-9eff-e99717bd7685'::uuid, true, 15, 'ACTIVE');

INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('edbdd38b-0ce7-546f-ae5c-1ec8a54d33f4'::uuid, 'bcb2038c-6d0a-50e1-a4a0-d2a95f678dcd'::uuid, '15020a90-ef38-5cdc-80a0-df4115b9062b'::uuid, true, 10, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('e164e053-68b5-5d14-a2d5-9c813fd5835d'::uuid, 'bcb2038c-6d0a-50e1-a4a0-d2a95f678dcd'::uuid, '1ee95342-36aa-5a40-9ad0-68f8ffd299d8'::uuid, true, 11, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('810eec7f-55b2-532a-a09a-ab2f273a361b'::uuid, 'bcb2038c-6d0a-50e1-a4a0-d2a95f678dcd'::uuid, '427f49cc-095f-5cf1-8cae-32db2a33fbda'::uuid, true, 12, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('cf25e400-175c-5fd3-891c-73a0144ba0e0'::uuid, 'bcb2038c-6d0a-50e1-a4a0-d2a95f678dcd'::uuid, 'd6ee55d7-2f96-555e-b12f-d0eb520bd0df'::uuid, true, 13, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('71d5d55b-3e6a-56ba-b854-ae1bae343a0c'::uuid, 'bcb2038c-6d0a-50e1-a4a0-d2a95f678dcd'::uuid, '56dee392-89c0-52bc-ab95-2346f750fb45'::uuid, true, 14, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('87ddf57c-93ce-5fa1-8572-a9d6474fa48d'::uuid, 'bcb2038c-6d0a-50e1-a4a0-d2a95f678dcd'::uuid, 'b3d5b58c-b52d-57ed-9eff-e99717bd7685'::uuid, true, 15, 'ACTIVE');

INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('7a991704-5183-5fe6-a615-849436eb0940'::uuid, 'a22f9a2c-5dea-5540-8e83-4b82c961ccb6'::uuid, '15020a90-ef38-5cdc-80a0-df4115b9062b'::uuid, true, 10, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('cc599e32-6c5f-500d-ba81-96fa97908add'::uuid, 'a22f9a2c-5dea-5540-8e83-4b82c961ccb6'::uuid, '1ee95342-36aa-5a40-9ad0-68f8ffd299d8'::uuid, true, 11, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('db198a19-971e-5fdd-ae59-86b47e11343d'::uuid, 'a22f9a2c-5dea-5540-8e83-4b82c961ccb6'::uuid, '427f49cc-095f-5cf1-8cae-32db2a33fbda'::uuid, true, 12, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('c9296d9d-1978-5dec-9b2a-5b083257ab20'::uuid, 'a22f9a2c-5dea-5540-8e83-4b82c961ccb6'::uuid, 'd6ee55d7-2f96-555e-b12f-d0eb520bd0df'::uuid, true, 13, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('57875c80-d2b1-5ad3-b698-5c25c8b8a496'::uuid, 'a22f9a2c-5dea-5540-8e83-4b82c961ccb6'::uuid, '56dee392-89c0-52bc-ab95-2346f750fb45'::uuid, true, 14, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('9aa1ed22-0760-5188-b774-4fea5a2d944d'::uuid, 'a22f9a2c-5dea-5540-8e83-4b82c961ccb6'::uuid, 'b3d5b58c-b52d-57ed-9eff-e99717bd7685'::uuid, true, 15, 'ACTIVE');

INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('515635e9-d6cd-591f-a0dd-848a64069f99'::uuid, 'eda7e441-ca09-5c23-8627-3b8f1b000c83'::uuid, '15020a90-ef38-5cdc-80a0-df4115b9062b'::uuid, true, 10, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('bad551ac-e85b-544e-b58b-2d542987caf4'::uuid, 'eda7e441-ca09-5c23-8627-3b8f1b000c83'::uuid, '1ee95342-36aa-5a40-9ad0-68f8ffd299d8'::uuid, true, 11, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('e6cbacc8-0523-536c-9ffb-6267f3683fda'::uuid, 'eda7e441-ca09-5c23-8627-3b8f1b000c83'::uuid, 'f17fa89e-7c51-5cd1-9b6a-d8e98507c9aa'::uuid, true, 12, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('b635d757-8c3a-562e-809e-afe94cc19021'::uuid, 'eda7e441-ca09-5c23-8627-3b8f1b000c83'::uuid, 'd6ee55d7-2f96-555e-b12f-d0eb520bd0df'::uuid, true, 13, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('ce3ae276-f14b-5fa8-88c2-f4ce158a105f'::uuid, 'eda7e441-ca09-5c23-8627-3b8f1b000c83'::uuid, 'b3d5b58c-b52d-57ed-9eff-e99717bd7685'::uuid, true, 14, 'ACTIVE');

INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('31e58810-2d78-59d1-b7ea-368e2dffb32a'::uuid, 'f4a59336-ea8a-5fc2-a92f-9645705a32e5'::uuid, '15020a90-ef38-5cdc-80a0-df4115b9062b'::uuid, true, 10, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('d1f84690-630e-590f-925b-5aaeac8ad34e'::uuid, 'f4a59336-ea8a-5fc2-a92f-9645705a32e5'::uuid, '1ee95342-36aa-5a40-9ad0-68f8ffd299d8'::uuid, true, 11, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('7ccf0013-2a8b-5ba4-84a3-84001533db67'::uuid, 'f4a59336-ea8a-5fc2-a92f-9645705a32e5'::uuid, 'f17fa89e-7c51-5cd1-9b6a-d8e98507c9aa'::uuid, true, 12, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('f774bd05-33c9-543a-ad82-556fff721476'::uuid, 'f4a59336-ea8a-5fc2-a92f-9645705a32e5'::uuid, 'd6ee55d7-2f96-555e-b12f-d0eb520bd0df'::uuid, true, 13, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('55bde40c-79c0-5e09-9147-966f559408d9'::uuid, 'f4a59336-ea8a-5fc2-a92f-9645705a32e5'::uuid, 'b3d5b58c-b52d-57ed-9eff-e99717bd7685'::uuid, true, 14, 'ACTIVE');

INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('6e0aa3df-1c2f-59b7-882a-1eb8c14af4cc'::uuid, 'c88d0ac3-653d-5d0b-be36-7c890ab69828'::uuid, '15020a90-ef38-5cdc-80a0-df4115b9062b'::uuid, true, 10, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('b10a51ec-8bc8-5b03-a313-36a8a201aec3'::uuid, 'c88d0ac3-653d-5d0b-be36-7c890ab69828'::uuid, '1ee95342-36aa-5a40-9ad0-68f8ffd299d8'::uuid, true, 11, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('756640c0-ba29-536d-a95d-4ea2f8871e0a'::uuid, 'c88d0ac3-653d-5d0b-be36-7c890ab69828'::uuid, 'f17fa89e-7c51-5cd1-9b6a-d8e98507c9aa'::uuid, true, 12, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('20a34c16-d294-5831-b795-f81af9afb3eb'::uuid, 'c88d0ac3-653d-5d0b-be36-7c890ab69828'::uuid, 'd6ee55d7-2f96-555e-b12f-d0eb520bd0df'::uuid, true, 13, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('a051712b-2341-5736-b324-50651154460e'::uuid, 'c88d0ac3-653d-5d0b-be36-7c890ab69828'::uuid, 'b3d5b58c-b52d-57ed-9eff-e99717bd7685'::uuid, true, 14, 'ACTIVE');

INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('2805c91a-7221-5c87-b126-624226bbc2b3'::uuid, '3d13d87b-639f-5727-81d3-238a2360449b'::uuid, '15020a90-ef38-5cdc-80a0-df4115b9062b'::uuid, true, 10, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('7975848d-4974-52a9-8168-6a355d39d232'::uuid, '3d13d87b-639f-5727-81d3-238a2360449b'::uuid, '1ee95342-36aa-5a40-9ad0-68f8ffd299d8'::uuid, true, 11, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('363d343f-1cc7-5f77-bcdb-d7ab0ab27133'::uuid, '3d13d87b-639f-5727-81d3-238a2360449b'::uuid, 'f17fa89e-7c51-5cd1-9b6a-d8e98507c9aa'::uuid, true, 12, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('257afc13-ef29-5277-9cc1-247f8b60cdf6'::uuid, '3d13d87b-639f-5727-81d3-238a2360449b'::uuid, 'd6ee55d7-2f96-555e-b12f-d0eb520bd0df'::uuid, true, 13, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('796ce582-209e-5966-aab0-6c80252159aa'::uuid, '3d13d87b-639f-5727-81d3-238a2360449b'::uuid, 'b3d5b58c-b52d-57ed-9eff-e99717bd7685'::uuid, true, 14, 'ACTIVE');

INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('2c47ee58-bc5e-5e9c-9ea7-b8840df26b5e'::uuid, 'bccc92a8-652f-5511-8443-c378f46a0b7c'::uuid, '15020a90-ef38-5cdc-80a0-df4115b9062b'::uuid, true, 10, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('dc94a9be-48e9-5047-bfb4-11b62ad1a060'::uuid, 'bccc92a8-652f-5511-8443-c378f46a0b7c'::uuid, '1ee95342-36aa-5a40-9ad0-68f8ffd299d8'::uuid, true, 11, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('35456dcf-3655-5013-a700-ae4e85e806e5'::uuid, 'bccc92a8-652f-5511-8443-c378f46a0b7c'::uuid, 'f17fa89e-7c51-5cd1-9b6a-d8e98507c9aa'::uuid, true, 12, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('833a6ea3-8feb-5ead-82f0-985141068bb1'::uuid, 'bccc92a8-652f-5511-8443-c378f46a0b7c'::uuid, 'd6ee55d7-2f96-555e-b12f-d0eb520bd0df'::uuid, true, 13, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('1aed3012-be3a-5d9e-a55d-ad2ae7f3194d'::uuid, 'bccc92a8-652f-5511-8443-c378f46a0b7c'::uuid, 'b3d5b58c-b52d-57ed-9eff-e99717bd7685'::uuid, true, 14, 'ACTIVE');

INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('dc2d653e-d6f5-52a0-8e60-3582b57c2b6a'::uuid, '3d7196b8-16c4-5551-bcd4-41384ca0d270'::uuid, '15020a90-ef38-5cdc-80a0-df4115b9062b'::uuid, true, 10, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('970149ca-0c4b-5faa-86f8-c74332c5d2c6'::uuid, '3d7196b8-16c4-5551-bcd4-41384ca0d270'::uuid, '1ee95342-36aa-5a40-9ad0-68f8ffd299d8'::uuid, true, 11, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('bd5bbe39-001a-5185-aaaa-c6fc80d2ce4d'::uuid, '3d7196b8-16c4-5551-bcd4-41384ca0d270'::uuid, 'ea163c55-9f18-578c-9215-bc19a7254fba'::uuid, true, 12, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('675b0158-85f0-5503-8a2a-f99d920f4bda'::uuid, '3d7196b8-16c4-5551-bcd4-41384ca0d270'::uuid, 'a4135e91-c9a7-5fe0-8be7-4b8ca97abc64'::uuid, true, 13, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('16e495fd-56a8-59e2-8a03-718c4168b363'::uuid, '3d7196b8-16c4-5551-bcd4-41384ca0d270'::uuid, '0344b3fb-ba83-5480-9a3b-2eb36eaac1f0'::uuid, true, 14, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('b9f472e0-3b0e-5257-aee5-c47e39a10acb'::uuid, '3d7196b8-16c4-5551-bcd4-41384ca0d270'::uuid, 'd6ee55d7-2f96-555e-b12f-d0eb520bd0df'::uuid, true, 15, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('db3c594b-f262-5539-9593-d9f8b97a8bea'::uuid, '3d7196b8-16c4-5551-bcd4-41384ca0d270'::uuid, '0fa0f706-9ffc-5dbc-9c8a-fa5d65983676'::uuid, true, 16, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('64e461bb-458d-537e-aa3b-186e609df4dc'::uuid, '3d7196b8-16c4-5551-bcd4-41384ca0d270'::uuid, 'f2bd62d0-6aa8-51ff-812a-6f1181a8d19f'::uuid, true, 17, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('261525fa-6c53-5e3c-b8b5-cd91c38462f7'::uuid, '3d7196b8-16c4-5551-bcd4-41384ca0d270'::uuid, 'b7454817-9da3-5738-af31-1a98c561a916'::uuid, true, 18, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('11e85db8-c15d-5763-ab6e-61252e7605f7'::uuid, '3d7196b8-16c4-5551-bcd4-41384ca0d270'::uuid, '2ea883df-5db9-5816-bf1e-16395065b814'::uuid, true, 19, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('28d4fa5f-b125-5085-8abd-83e331c5c3c6'::uuid, '3d7196b8-16c4-5551-bcd4-41384ca0d270'::uuid, '6361b3db-2bbb-5a1d-9035-bd931753d9f8'::uuid, true, 20, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('c940e697-b504-5b01-9178-3e33acf31e93'::uuid, '3d7196b8-16c4-5551-bcd4-41384ca0d270'::uuid, 'b3d5b58c-b52d-57ed-9eff-e99717bd7685'::uuid, true, 21, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('f772744f-2a3e-599e-bb22-d0ed2f6633a0'::uuid, '3d7196b8-16c4-5551-bcd4-41384ca0d270'::uuid, 'e23a7487-040c-555a-802f-af27accf9acc'::uuid, true, 22, 'ACTIVE');

INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('fa3d3c33-59f1-5b0f-bae4-12789c02bd14'::uuid, '25eedb14-22ca-5640-a35a-35ce24c28650'::uuid, '15020a90-ef38-5cdc-80a0-df4115b9062b'::uuid, true, 10, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('56919837-8558-5912-b506-40825da3cba5'::uuid, '25eedb14-22ca-5640-a35a-35ce24c28650'::uuid, '1ee95342-36aa-5a40-9ad0-68f8ffd299d8'::uuid, true, 11, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('4a108bc3-4bca-585d-8b38-7d3579a3e1a0'::uuid, '25eedb14-22ca-5640-a35a-35ce24c28650'::uuid, 'ea163c55-9f18-578c-9215-bc19a7254fba'::uuid, true, 12, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('7cc8be3c-1199-597a-9246-83513259c489'::uuid, '25eedb14-22ca-5640-a35a-35ce24c28650'::uuid, 'a4135e91-c9a7-5fe0-8be7-4b8ca97abc64'::uuid, true, 13, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('b1f3a8b9-ad30-5a61-82a4-2ea8954fdde5'::uuid, '25eedb14-22ca-5640-a35a-35ce24c28650'::uuid, '0344b3fb-ba83-5480-9a3b-2eb36eaac1f0'::uuid, true, 14, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('d11f050a-d006-5dd4-8163-550cc86f02b7'::uuid, '25eedb14-22ca-5640-a35a-35ce24c28650'::uuid, 'd6ee55d7-2f96-555e-b12f-d0eb520bd0df'::uuid, true, 15, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('68d2c404-8460-573b-9c3d-98bc860d84fa'::uuid, '25eedb14-22ca-5640-a35a-35ce24c28650'::uuid, '0fa0f706-9ffc-5dbc-9c8a-fa5d65983676'::uuid, true, 16, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('b4d56ad9-0230-5441-b9d7-0719aa7618c0'::uuid, '25eedb14-22ca-5640-a35a-35ce24c28650'::uuid, 'f2bd62d0-6aa8-51ff-812a-6f1181a8d19f'::uuid, true, 17, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('4f0c0b13-2979-5207-8892-e005ace3f662'::uuid, '25eedb14-22ca-5640-a35a-35ce24c28650'::uuid, 'b7454817-9da3-5738-af31-1a98c561a916'::uuid, true, 18, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('fbbe7c4d-d710-5206-9e27-cd543e2d8d64'::uuid, '25eedb14-22ca-5640-a35a-35ce24c28650'::uuid, '2ea883df-5db9-5816-bf1e-16395065b814'::uuid, true, 19, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('cb58d8e4-2a91-5607-a45a-c7f29c825122'::uuid, '25eedb14-22ca-5640-a35a-35ce24c28650'::uuid, '6361b3db-2bbb-5a1d-9035-bd931753d9f8'::uuid, true, 20, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('6a83fb8c-8346-5c85-973f-209544da3b0a'::uuid, '25eedb14-22ca-5640-a35a-35ce24c28650'::uuid, 'b3d5b58c-b52d-57ed-9eff-e99717bd7685'::uuid, true, 21, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('a81a3a1f-71bf-58df-aa1f-4ef9765f1247'::uuid, '25eedb14-22ca-5640-a35a-35ce24c28650'::uuid, 'e23a7487-040c-555a-802f-af27accf9acc'::uuid, true, 22, 'ACTIVE');

INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('b90f12e1-504e-5173-bba2-796c626fa6f4'::uuid, 'b449a18f-812b-5666-b798-9171af540803'::uuid, '15020a90-ef38-5cdc-80a0-df4115b9062b'::uuid, true, 10, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('c944a576-89d9-5e6d-9d1a-6cb773914fc1'::uuid, 'b449a18f-812b-5666-b798-9171af540803'::uuid, '1ee95342-36aa-5a40-9ad0-68f8ffd299d8'::uuid, true, 11, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('eb356f1b-09de-5e4e-b82a-84d0ab82d496'::uuid, 'b449a18f-812b-5666-b798-9171af540803'::uuid, 'ea163c55-9f18-578c-9215-bc19a7254fba'::uuid, true, 12, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('bcf946fa-6ddf-5dfa-a172-eb3858e898a8'::uuid, 'b449a18f-812b-5666-b798-9171af540803'::uuid, 'a4135e91-c9a7-5fe0-8be7-4b8ca97abc64'::uuid, true, 13, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('e7ee5b8d-da6f-5e4c-a0e0-d12f33d5744f'::uuid, 'b449a18f-812b-5666-b798-9171af540803'::uuid, '0344b3fb-ba83-5480-9a3b-2eb36eaac1f0'::uuid, true, 14, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('70dbab21-9c2d-5935-8a0a-d8b7d59ea05c'::uuid, 'b449a18f-812b-5666-b798-9171af540803'::uuid, 'd6ee55d7-2f96-555e-b12f-d0eb520bd0df'::uuid, true, 15, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('a31a5ae5-2553-54ac-8634-c991cf3be36f'::uuid, 'b449a18f-812b-5666-b798-9171af540803'::uuid, '0fa0f706-9ffc-5dbc-9c8a-fa5d65983676'::uuid, true, 16, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('7dd6c819-0471-550e-b5b7-1d297dd00bc9'::uuid, 'b449a18f-812b-5666-b798-9171af540803'::uuid, 'f2bd62d0-6aa8-51ff-812a-6f1181a8d19f'::uuid, true, 17, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('c99bb60a-d8cf-5373-bfa7-372b38cb8433'::uuid, 'b449a18f-812b-5666-b798-9171af540803'::uuid, 'b7454817-9da3-5738-af31-1a98c561a916'::uuid, true, 18, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('bd954713-3676-5805-b45c-76a8c3501498'::uuid, 'b449a18f-812b-5666-b798-9171af540803'::uuid, '2ea883df-5db9-5816-bf1e-16395065b814'::uuid, true, 19, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('608b5326-13cc-556e-a5c9-2046f12afbf9'::uuid, 'b449a18f-812b-5666-b798-9171af540803'::uuid, '6361b3db-2bbb-5a1d-9035-bd931753d9f8'::uuid, true, 20, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('25671411-f32d-5599-abbd-1d8adffad2b3'::uuid, 'b449a18f-812b-5666-b798-9171af540803'::uuid, 'b3d5b58c-b52d-57ed-9eff-e99717bd7685'::uuid, true, 21, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('fd61d01d-deb4-5cf9-a69e-d40b38fe62a0'::uuid, 'b449a18f-812b-5666-b798-9171af540803'::uuid, 'e23a7487-040c-555a-802f-af27accf9acc'::uuid, true, 22, 'ACTIVE');

INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('ecf08914-486e-552b-afac-a576ad5225ae'::uuid, '8a171369-b9d0-5f65-b65d-f971df71f70f'::uuid, '15020a90-ef38-5cdc-80a0-df4115b9062b'::uuid, true, 10, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('aa0f0423-923d-5ce7-b7cf-3b1e6d12c246'::uuid, '8a171369-b9d0-5f65-b65d-f971df71f70f'::uuid, '1ee95342-36aa-5a40-9ad0-68f8ffd299d8'::uuid, true, 11, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('5ef3d55e-149b-5bfc-a11c-2b37b9131030'::uuid, '8a171369-b9d0-5f65-b65d-f971df71f70f'::uuid, 'ea163c55-9f18-578c-9215-bc19a7254fba'::uuid, true, 12, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('fffa9f30-1e38-5c4c-a085-1c0379699218'::uuid, '8a171369-b9d0-5f65-b65d-f971df71f70f'::uuid, 'a4135e91-c9a7-5fe0-8be7-4b8ca97abc64'::uuid, true, 13, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('4b5de6bf-fec4-5d05-a44d-69ad45bda8ee'::uuid, '8a171369-b9d0-5f65-b65d-f971df71f70f'::uuid, '0344b3fb-ba83-5480-9a3b-2eb36eaac1f0'::uuid, true, 14, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('347e1242-6af2-55c1-add1-8e29d331e1e5'::uuid, '8a171369-b9d0-5f65-b65d-f971df71f70f'::uuid, 'd6ee55d7-2f96-555e-b12f-d0eb520bd0df'::uuid, true, 15, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('45163b72-d333-540d-b5b0-771908b75a6d'::uuid, '8a171369-b9d0-5f65-b65d-f971df71f70f'::uuid, '0fa0f706-9ffc-5dbc-9c8a-fa5d65983676'::uuid, true, 16, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('504ab819-6269-5052-b5f7-dd7deb2b9d58'::uuid, '8a171369-b9d0-5f65-b65d-f971df71f70f'::uuid, 'f2bd62d0-6aa8-51ff-812a-6f1181a8d19f'::uuid, true, 17, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('d4c1d7c4-b628-5de8-824d-6f1896b591b2'::uuid, '8a171369-b9d0-5f65-b65d-f971df71f70f'::uuid, 'b7454817-9da3-5738-af31-1a98c561a916'::uuid, true, 18, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('77e3ec2f-6832-5ede-b526-5af20c1a30df'::uuid, '8a171369-b9d0-5f65-b65d-f971df71f70f'::uuid, '2ea883df-5db9-5816-bf1e-16395065b814'::uuid, true, 19, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('20b7fb8a-76e1-5dc7-a366-649f0147d3f0'::uuid, '8a171369-b9d0-5f65-b65d-f971df71f70f'::uuid, '6361b3db-2bbb-5a1d-9035-bd931753d9f8'::uuid, true, 20, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('c51ae4cb-cee8-59b6-b3e2-ad3650247934'::uuid, '8a171369-b9d0-5f65-b65d-f971df71f70f'::uuid, 'b3d5b58c-b52d-57ed-9eff-e99717bd7685'::uuid, true, 21, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('31213549-d3ea-5855-b268-a37a491e4dc4'::uuid, '8a171369-b9d0-5f65-b65d-f971df71f70f'::uuid, 'e23a7487-040c-555a-802f-af27accf9acc'::uuid, true, 22, 'ACTIVE');

INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('0524424a-39ea-5280-88ef-aa2b495b9745'::uuid, '1e8b9f3d-919c-5757-a972-18a97baab551'::uuid, '15020a90-ef38-5cdc-80a0-df4115b9062b'::uuid, true, 10, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('016de96f-5617-5eba-b04f-fe2a63c52094'::uuid, '1e8b9f3d-919c-5757-a972-18a97baab551'::uuid, '1ee95342-36aa-5a40-9ad0-68f8ffd299d8'::uuid, true, 11, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('3f13c93f-9266-5ced-a45d-6f10bac787dd'::uuid, '1e8b9f3d-919c-5757-a972-18a97baab551'::uuid, 'ea163c55-9f18-578c-9215-bc19a7254fba'::uuid, true, 12, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('11c41782-5d3a-5176-82a7-7797e18608d3'::uuid, '1e8b9f3d-919c-5757-a972-18a97baab551'::uuid, 'f2bd62d0-6aa8-51ff-812a-6f1181a8d19f'::uuid, true, 13, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('78efa934-e2f4-5446-afd7-94a145667696'::uuid, '1e8b9f3d-919c-5757-a972-18a97baab551'::uuid, '34965dd6-c0a4-5555-80a6-6090b9041083'::uuid, true, 14, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('68536cec-e57c-5b49-8df5-19577eaeb7f6'::uuid, '1e8b9f3d-919c-5757-a972-18a97baab551'::uuid, 'b7454817-9da3-5738-af31-1a98c561a916'::uuid, true, 15, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('ee309837-9d96-56e7-9cb5-fa8b5cba9f3b'::uuid, '1e8b9f3d-919c-5757-a972-18a97baab551'::uuid, 'd6ee55d7-2f96-555e-b12f-d0eb520bd0df'::uuid, true, 16, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('b09afbd9-0bb5-52be-b482-48e0530015bc'::uuid, '1e8b9f3d-919c-5757-a972-18a97baab551'::uuid, '6361b3db-2bbb-5a1d-9035-bd931753d9f8'::uuid, true, 17, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('3af29a47-8214-535c-af19-e134fe392ac0'::uuid, '1e8b9f3d-919c-5757-a972-18a97baab551'::uuid, '2ea883df-5db9-5816-bf1e-16395065b814'::uuid, true, 18, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('fe2064ca-276a-5221-9533-d2ba44878dcc'::uuid, '1e8b9f3d-919c-5757-a972-18a97baab551'::uuid, 'fa4a7e1a-93cc-5def-8b8f-de7427642c34'::uuid, true, 19, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('a4add5e2-499e-53e3-adb3-0ba08f95808d'::uuid, '1e8b9f3d-919c-5757-a972-18a97baab551'::uuid, 'e800e788-5cbe-5664-a701-05aef9d1f247'::uuid, true, 20, 'ACTIVE');

INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('6721a330-8de3-59f3-a6e8-917b680c8726'::uuid, 'cdc10028-95c8-5f8c-88e6-8bfdfd07fdea'::uuid, '15020a90-ef38-5cdc-80a0-df4115b9062b'::uuid, true, 10, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('f3bec1e5-0e57-528a-bf1d-16f2772e59f5'::uuid, 'cdc10028-95c8-5f8c-88e6-8bfdfd07fdea'::uuid, '1ee95342-36aa-5a40-9ad0-68f8ffd299d8'::uuid, true, 11, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('e10593ed-6b07-569b-88fb-82a042b5c4d3'::uuid, 'cdc10028-95c8-5f8c-88e6-8bfdfd07fdea'::uuid, 'ea163c55-9f18-578c-9215-bc19a7254fba'::uuid, true, 12, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('b54cc50b-dd39-5162-b25a-134337aa39c8'::uuid, 'cdc10028-95c8-5f8c-88e6-8bfdfd07fdea'::uuid, 'f2bd62d0-6aa8-51ff-812a-6f1181a8d19f'::uuid, true, 13, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('56719167-dd58-582b-9b66-8cc23aa18981'::uuid, 'cdc10028-95c8-5f8c-88e6-8bfdfd07fdea'::uuid, '34965dd6-c0a4-5555-80a6-6090b9041083'::uuid, true, 14, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('df7b8c63-f436-529b-8a71-43b9be16833b'::uuid, 'cdc10028-95c8-5f8c-88e6-8bfdfd07fdea'::uuid, 'b7454817-9da3-5738-af31-1a98c561a916'::uuid, true, 15, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('2c640ccb-4f86-5314-90c2-2b0e139ab4a6'::uuid, 'cdc10028-95c8-5f8c-88e6-8bfdfd07fdea'::uuid, 'd6ee55d7-2f96-555e-b12f-d0eb520bd0df'::uuid, true, 16, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('44becf06-7349-579d-9ff0-5fb632b498b7'::uuid, 'cdc10028-95c8-5f8c-88e6-8bfdfd07fdea'::uuid, '6361b3db-2bbb-5a1d-9035-bd931753d9f8'::uuid, true, 17, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('fc3c1ed5-20cb-5aa8-9c58-c60cbac939cf'::uuid, 'cdc10028-95c8-5f8c-88e6-8bfdfd07fdea'::uuid, '2ea883df-5db9-5816-bf1e-16395065b814'::uuid, true, 18, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('25380353-4606-594a-bef3-ef8eeb75ad67'::uuid, 'cdc10028-95c8-5f8c-88e6-8bfdfd07fdea'::uuid, 'fa4a7e1a-93cc-5def-8b8f-de7427642c34'::uuid, true, 19, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('dbc8d3d9-369d-5fa6-bbd5-4c9223ecb542'::uuid, 'cdc10028-95c8-5f8c-88e6-8bfdfd07fdea'::uuid, 'e800e788-5cbe-5664-a701-05aef9d1f247'::uuid, true, 20, 'ACTIVE');

INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('161d9e84-4296-5c34-8026-ed915633977f'::uuid, '54645d3e-d51a-5c35-9e9c-8d4d3ff20ba9'::uuid, '15020a90-ef38-5cdc-80a0-df4115b9062b'::uuid, true, 10, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('39801cb3-dc7d-5985-a21f-30606ec383dc'::uuid, '54645d3e-d51a-5c35-9e9c-8d4d3ff20ba9'::uuid, '1ee95342-36aa-5a40-9ad0-68f8ffd299d8'::uuid, true, 11, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('15b3b52d-8f59-5d21-952d-1fe90408c0a4'::uuid, '54645d3e-d51a-5c35-9e9c-8d4d3ff20ba9'::uuid, 'ea163c55-9f18-578c-9215-bc19a7254fba'::uuid, true, 12, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('e4b3d345-d59e-5155-a9d6-c8c0a3e49211'::uuid, '54645d3e-d51a-5c35-9e9c-8d4d3ff20ba9'::uuid, 'f2bd62d0-6aa8-51ff-812a-6f1181a8d19f'::uuid, true, 13, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('b4204539-f880-5318-8290-6aa6e7bf71a5'::uuid, '54645d3e-d51a-5c35-9e9c-8d4d3ff20ba9'::uuid, '34965dd6-c0a4-5555-80a6-6090b9041083'::uuid, true, 14, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('5fb564a7-473b-54f2-9bab-52852f43cf49'::uuid, '54645d3e-d51a-5c35-9e9c-8d4d3ff20ba9'::uuid, 'b7454817-9da3-5738-af31-1a98c561a916'::uuid, true, 15, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('9030e7b2-fc2c-59fc-b097-5483d2dce933'::uuid, '54645d3e-d51a-5c35-9e9c-8d4d3ff20ba9'::uuid, 'd6ee55d7-2f96-555e-b12f-d0eb520bd0df'::uuid, true, 16, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('ddb9d0bb-8489-5e47-a5b1-0b54f7f014fe'::uuid, '54645d3e-d51a-5c35-9e9c-8d4d3ff20ba9'::uuid, '6361b3db-2bbb-5a1d-9035-bd931753d9f8'::uuid, true, 17, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('75f0cd61-2b4b-5b5b-afdf-d42b8e39514b'::uuid, '54645d3e-d51a-5c35-9e9c-8d4d3ff20ba9'::uuid, '2ea883df-5db9-5816-bf1e-16395065b814'::uuid, true, 18, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('83edbc1e-bc10-5af9-9939-7911dfa17c4a'::uuid, '54645d3e-d51a-5c35-9e9c-8d4d3ff20ba9'::uuid, 'fa4a7e1a-93cc-5def-8b8f-de7427642c34'::uuid, true, 19, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('40760a3f-6a47-5784-abb8-2c11cb3ddca7'::uuid, '54645d3e-d51a-5c35-9e9c-8d4d3ff20ba9'::uuid, 'e800e788-5cbe-5664-a701-05aef9d1f247'::uuid, true, 20, 'ACTIVE');

INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('492948c3-456c-5810-a689-9e2710767619'::uuid, '2e4e902c-f4ae-5291-9c0b-1a433b2ab9ca'::uuid, '15020a90-ef38-5cdc-80a0-df4115b9062b'::uuid, true, 10, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('c84047a1-fc09-5893-badc-db837a512475'::uuid, '2e4e902c-f4ae-5291-9c0b-1a433b2ab9ca'::uuid, '1ee95342-36aa-5a40-9ad0-68f8ffd299d8'::uuid, true, 11, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('964ccc76-d1c0-54b7-a6c8-9091c86904ba'::uuid, '2e4e902c-f4ae-5291-9c0b-1a433b2ab9ca'::uuid, 'ea163c55-9f18-578c-9215-bc19a7254fba'::uuid, true, 12, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('5feea5d4-9625-511b-85c2-104e250cb285'::uuid, '2e4e902c-f4ae-5291-9c0b-1a433b2ab9ca'::uuid, 'f2bd62d0-6aa8-51ff-812a-6f1181a8d19f'::uuid, true, 13, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('7527c55e-5afb-5a25-9da9-98452c8358df'::uuid, '2e4e902c-f4ae-5291-9c0b-1a433b2ab9ca'::uuid, '34965dd6-c0a4-5555-80a6-6090b9041083'::uuid, true, 14, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('53cac17f-ea0f-5171-b3f8-4aabbba71a6f'::uuid, '2e4e902c-f4ae-5291-9c0b-1a433b2ab9ca'::uuid, 'b7454817-9da3-5738-af31-1a98c561a916'::uuid, true, 15, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('ebaf3000-20bb-515a-8bd5-f50930ab38a9'::uuid, '2e4e902c-f4ae-5291-9c0b-1a433b2ab9ca'::uuid, 'd6ee55d7-2f96-555e-b12f-d0eb520bd0df'::uuid, true, 16, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('0a18f1c1-d379-57f3-bd73-122571ee50de'::uuid, '2e4e902c-f4ae-5291-9c0b-1a433b2ab9ca'::uuid, '6361b3db-2bbb-5a1d-9035-bd931753d9f8'::uuid, true, 17, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('4d38c730-8234-5541-a24c-3aaad63f3be6'::uuid, '2e4e902c-f4ae-5291-9c0b-1a433b2ab9ca'::uuid, '2ea883df-5db9-5816-bf1e-16395065b814'::uuid, true, 18, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('f3c1624d-ba89-5e3e-ba91-0dcc75b2efab'::uuid, '2e4e902c-f4ae-5291-9c0b-1a433b2ab9ca'::uuid, 'fa4a7e1a-93cc-5def-8b8f-de7427642c34'::uuid, true, 19, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('7724c039-a5ac-5f90-ab3c-957ca958cad8'::uuid, '2e4e902c-f4ae-5291-9c0b-1a433b2ab9ca'::uuid, 'e800e788-5cbe-5664-a701-05aef9d1f247'::uuid, true, 20, 'ACTIVE');

INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('ff96d17f-eac6-53d7-8a23-a65c75e7f2dc'::uuid, '1ca5fe7f-85f8-5b2a-8a79-c6146d246fa7'::uuid, '15020a90-ef38-5cdc-80a0-df4115b9062b'::uuid, true, 10, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('5b8d27d8-a68b-54c2-a35d-b3164bfba305'::uuid, '1ca5fe7f-85f8-5b2a-8a79-c6146d246fa7'::uuid, '1ee95342-36aa-5a40-9ad0-68f8ffd299d8'::uuid, true, 11, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('0956681c-064a-579f-afaf-7924b31cd02d'::uuid, '1ca5fe7f-85f8-5b2a-8a79-c6146d246fa7'::uuid, 'ea163c55-9f18-578c-9215-bc19a7254fba'::uuid, true, 12, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('d8ce3dfc-2eb6-5c02-b693-efcf9043bcc0'::uuid, '1ca5fe7f-85f8-5b2a-8a79-c6146d246fa7'::uuid, 'f2bd62d0-6aa8-51ff-812a-6f1181a8d19f'::uuid, true, 13, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('121c721d-40e1-57f1-ab0f-4fa367b35c91'::uuid, '1ca5fe7f-85f8-5b2a-8a79-c6146d246fa7'::uuid, '34965dd6-c0a4-5555-80a6-6090b9041083'::uuid, true, 14, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('7997f50c-83e3-5703-ac48-9438f1dcdedc'::uuid, '1ca5fe7f-85f8-5b2a-8a79-c6146d246fa7'::uuid, 'b7454817-9da3-5738-af31-1a98c561a916'::uuid, true, 15, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('c2fc6f03-35de-57f7-b858-928c9336885f'::uuid, '1ca5fe7f-85f8-5b2a-8a79-c6146d246fa7'::uuid, 'd6ee55d7-2f96-555e-b12f-d0eb520bd0df'::uuid, true, 16, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('36ee873e-71fb-5c9c-8802-8877f53ef927'::uuid, '1ca5fe7f-85f8-5b2a-8a79-c6146d246fa7'::uuid, '6361b3db-2bbb-5a1d-9035-bd931753d9f8'::uuid, true, 17, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('2cbd3ca5-60a2-506a-bbed-eaf80e45c861'::uuid, '1ca5fe7f-85f8-5b2a-8a79-c6146d246fa7'::uuid, '2ea883df-5db9-5816-bf1e-16395065b814'::uuid, true, 18, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('37c0bc39-0f6e-53d8-9a73-1b886f320ade'::uuid, '1ca5fe7f-85f8-5b2a-8a79-c6146d246fa7'::uuid, 'fa4a7e1a-93cc-5def-8b8f-de7427642c34'::uuid, true, 19, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('f5c969a1-0069-54c9-8899-a5b369c04de8'::uuid, '1ca5fe7f-85f8-5b2a-8a79-c6146d246fa7'::uuid, 'e800e788-5cbe-5664-a701-05aef9d1f247'::uuid, true, 20, 'ACTIVE');

INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('fcf81d3d-6a15-5a51-ab2b-26127fcdfcb0'::uuid, 'c14d33b8-89da-5c12-bca2-af6acb1af9da'::uuid, '15020a90-ef38-5cdc-80a0-df4115b9062b'::uuid, true, 10, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('0d3174be-0ce1-59aa-a072-e54851bd4e8a'::uuid, 'c14d33b8-89da-5c12-bca2-af6acb1af9da'::uuid, '1ee95342-36aa-5a40-9ad0-68f8ffd299d8'::uuid, true, 11, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('b64c1ff9-a7e4-5a67-a5a6-6577cfc3c4b1'::uuid, 'c14d33b8-89da-5c12-bca2-af6acb1af9da'::uuid, '7a069df9-69c8-56fa-b1aa-44999ad6e5dc'::uuid, true, 12, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('6260562d-5343-5634-9e3c-4c94b824efef'::uuid, 'c14d33b8-89da-5c12-bca2-af6acb1af9da'::uuid, 'd6ee55d7-2f96-555e-b12f-d0eb520bd0df'::uuid, true, 13, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('e492cf18-af07-5751-b612-4d2bc8c036ea'::uuid, 'c14d33b8-89da-5c12-bca2-af6acb1af9da'::uuid, 'f2bd62d0-6aa8-51ff-812a-6f1181a8d19f'::uuid, true, 14, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('fa473aaa-de1b-5ec5-9949-26fd1a0f47cc'::uuid, 'c14d33b8-89da-5c12-bca2-af6acb1af9da'::uuid, '56dee392-89c0-52bc-ab95-2346f750fb45'::uuid, true, 15, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('7020004b-d13e-5033-9ea8-99b7864c7329'::uuid, 'c14d33b8-89da-5c12-bca2-af6acb1af9da'::uuid, '2107eb71-ddd6-5261-b8c7-a1a941ac8afa'::uuid, true, 16, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('58d2cd32-c317-5773-8835-7d6c5e0ab995'::uuid, 'c14d33b8-89da-5c12-bca2-af6acb1af9da'::uuid, '5811f1ab-c4ec-5e51-aa3f-3376ebda16fd'::uuid, true, 17, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('16df4faf-2c9d-5ca2-bc2d-4ccd35f275ed'::uuid, 'c14d33b8-89da-5c12-bca2-af6acb1af9da'::uuid, '2cd766aa-0606-513d-ac75-a324541cbb85'::uuid, true, 18, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('fb3e0c8a-af62-5c49-bd5e-11858d8fbbfe'::uuid, 'c14d33b8-89da-5c12-bca2-af6acb1af9da'::uuid, '2ea883df-5db9-5816-bf1e-16395065b814'::uuid, true, 19, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('fb57228a-2394-5824-b5e5-9d5756a4e25e'::uuid, 'c14d33b8-89da-5c12-bca2-af6acb1af9da'::uuid, '6361b3db-2bbb-5a1d-9035-bd931753d9f8'::uuid, true, 20, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('2630e4a1-d02a-5f60-9c30-600e75adee78'::uuid, 'c14d33b8-89da-5c12-bca2-af6acb1af9da'::uuid, 'b3d5b58c-b52d-57ed-9eff-e99717bd7685'::uuid, true, 21, 'ACTIVE');

INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('024013c4-351a-5aa6-bc46-6959051c2d71'::uuid, '75110a34-2afc-5449-b95d-2653395a5b58'::uuid, '15020a90-ef38-5cdc-80a0-df4115b9062b'::uuid, true, 10, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('42a263bd-79a7-58d2-b860-2cedbabf0b9d'::uuid, '75110a34-2afc-5449-b95d-2653395a5b58'::uuid, '1ee95342-36aa-5a40-9ad0-68f8ffd299d8'::uuid, true, 11, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('4ffcb926-91ee-5bff-88d0-eddeac911d89'::uuid, '75110a34-2afc-5449-b95d-2653395a5b58'::uuid, '7a069df9-69c8-56fa-b1aa-44999ad6e5dc'::uuid, true, 12, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('f6a67df9-32c1-56d2-ba95-98d38c32f907'::uuid, '75110a34-2afc-5449-b95d-2653395a5b58'::uuid, 'd6ee55d7-2f96-555e-b12f-d0eb520bd0df'::uuid, true, 13, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('5e0a4bfe-34c4-5527-95d4-ca0007db84c5'::uuid, '75110a34-2afc-5449-b95d-2653395a5b58'::uuid, 'f2bd62d0-6aa8-51ff-812a-6f1181a8d19f'::uuid, true, 14, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('14a94b4e-6009-57b8-922a-2e94d7ddc882'::uuid, '75110a34-2afc-5449-b95d-2653395a5b58'::uuid, '56dee392-89c0-52bc-ab95-2346f750fb45'::uuid, true, 15, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('169e07ae-3e66-52ec-846c-9c169aa25c5a'::uuid, '75110a34-2afc-5449-b95d-2653395a5b58'::uuid, '2107eb71-ddd6-5261-b8c7-a1a941ac8afa'::uuid, true, 16, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('144fe2de-77b0-5ddd-b112-ba8111cccbf6'::uuid, '75110a34-2afc-5449-b95d-2653395a5b58'::uuid, '5811f1ab-c4ec-5e51-aa3f-3376ebda16fd'::uuid, true, 17, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('bebe50c1-e4cf-58e7-b2c9-f3dca7d57265'::uuid, '75110a34-2afc-5449-b95d-2653395a5b58'::uuid, '2cd766aa-0606-513d-ac75-a324541cbb85'::uuid, true, 18, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('6aaa21ec-c271-51c7-a175-0696d56deb1f'::uuid, '75110a34-2afc-5449-b95d-2653395a5b58'::uuid, '2ea883df-5db9-5816-bf1e-16395065b814'::uuid, true, 19, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('63f234ef-72af-5638-b9a5-524c03e36ee9'::uuid, '75110a34-2afc-5449-b95d-2653395a5b58'::uuid, '6361b3db-2bbb-5a1d-9035-bd931753d9f8'::uuid, true, 20, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('da121443-0280-5e93-8b85-cb82b7d1094d'::uuid, '75110a34-2afc-5449-b95d-2653395a5b58'::uuid, 'b3d5b58c-b52d-57ed-9eff-e99717bd7685'::uuid, true, 21, 'ACTIVE');

INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('a8db9480-3bd1-5914-9a8a-7d64ce7e77bb'::uuid, '7dab3222-08c4-5ba2-aaae-fcbd63120eb4'::uuid, '15020a90-ef38-5cdc-80a0-df4115b9062b'::uuid, true, 10, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('9b140300-8b44-5beb-879a-80830bf0ef72'::uuid, '7dab3222-08c4-5ba2-aaae-fcbd63120eb4'::uuid, '1ee95342-36aa-5a40-9ad0-68f8ffd299d8'::uuid, true, 11, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('b3727576-df8e-52ca-82f2-6a62627aaeec'::uuid, '7dab3222-08c4-5ba2-aaae-fcbd63120eb4'::uuid, '7a069df9-69c8-56fa-b1aa-44999ad6e5dc'::uuid, true, 12, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('d4ed65c6-86e8-5c3d-a804-24a6a7c33ae0'::uuid, '7dab3222-08c4-5ba2-aaae-fcbd63120eb4'::uuid, 'd6ee55d7-2f96-555e-b12f-d0eb520bd0df'::uuid, true, 13, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('8dbaabf2-8a65-52bd-a012-05ae144786f6'::uuid, '7dab3222-08c4-5ba2-aaae-fcbd63120eb4'::uuid, 'f2bd62d0-6aa8-51ff-812a-6f1181a8d19f'::uuid, true, 14, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('f55a2b75-2a3e-5bf9-b140-ff4dad8a5877'::uuid, '7dab3222-08c4-5ba2-aaae-fcbd63120eb4'::uuid, '56dee392-89c0-52bc-ab95-2346f750fb45'::uuid, true, 15, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('0fb53599-d40a-5c52-ba8c-04c8b586af0b'::uuid, '7dab3222-08c4-5ba2-aaae-fcbd63120eb4'::uuid, '2107eb71-ddd6-5261-b8c7-a1a941ac8afa'::uuid, true, 16, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('626c07a1-4d43-5b0d-8f53-a2e444c2e8d3'::uuid, '7dab3222-08c4-5ba2-aaae-fcbd63120eb4'::uuid, '5811f1ab-c4ec-5e51-aa3f-3376ebda16fd'::uuid, true, 17, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('31245436-8944-53e5-8b08-4a9a75a86484'::uuid, '7dab3222-08c4-5ba2-aaae-fcbd63120eb4'::uuid, '2cd766aa-0606-513d-ac75-a324541cbb85'::uuid, true, 18, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('86ef4d20-7466-5f60-8c9c-1ec7039141f1'::uuid, '7dab3222-08c4-5ba2-aaae-fcbd63120eb4'::uuid, '2ea883df-5db9-5816-bf1e-16395065b814'::uuid, true, 19, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('2a175866-ea45-5e6c-b72e-39fa1c55d7b7'::uuid, '7dab3222-08c4-5ba2-aaae-fcbd63120eb4'::uuid, '6361b3db-2bbb-5a1d-9035-bd931753d9f8'::uuid, true, 20, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('b2e45fab-109f-55d1-aad9-48908f25ae1d'::uuid, '7dab3222-08c4-5ba2-aaae-fcbd63120eb4'::uuid, 'b3d5b58c-b52d-57ed-9eff-e99717bd7685'::uuid, true, 21, 'ACTIVE');

INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('266b3214-546a-55ad-be45-ac1af14aeb3d'::uuid, '1fd274a8-1909-5fec-b15f-cafbbde76e88'::uuid, '15020a90-ef38-5cdc-80a0-df4115b9062b'::uuid, true, 10, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('fd74bb31-d75b-531c-a485-b85dee2aa8e4'::uuid, '1fd274a8-1909-5fec-b15f-cafbbde76e88'::uuid, '1ee95342-36aa-5a40-9ad0-68f8ffd299d8'::uuid, true, 11, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('59c01aa1-2aca-5009-b61b-4203952cebc4'::uuid, '1fd274a8-1909-5fec-b15f-cafbbde76e88'::uuid, '7a069df9-69c8-56fa-b1aa-44999ad6e5dc'::uuid, true, 12, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('8440ead0-5766-55bc-9d66-3c36beff8c80'::uuid, '1fd274a8-1909-5fec-b15f-cafbbde76e88'::uuid, 'd6ee55d7-2f96-555e-b12f-d0eb520bd0df'::uuid, true, 13, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('25f3e21f-9c47-5bb9-8f7c-49ad0daaebe6'::uuid, '1fd274a8-1909-5fec-b15f-cafbbde76e88'::uuid, 'f2bd62d0-6aa8-51ff-812a-6f1181a8d19f'::uuid, true, 14, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('d52947de-09f3-56f7-b525-b7e7f206c45a'::uuid, '1fd274a8-1909-5fec-b15f-cafbbde76e88'::uuid, '56dee392-89c0-52bc-ab95-2346f750fb45'::uuid, true, 15, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('bdab7726-bc66-578c-8987-93235c8b9790'::uuid, '1fd274a8-1909-5fec-b15f-cafbbde76e88'::uuid, '2107eb71-ddd6-5261-b8c7-a1a941ac8afa'::uuid, true, 16, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('bd2f09c0-d861-54dd-aa0c-699a07a337b3'::uuid, '1fd274a8-1909-5fec-b15f-cafbbde76e88'::uuid, '5811f1ab-c4ec-5e51-aa3f-3376ebda16fd'::uuid, true, 17, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('d309b915-a49e-53a7-acb5-62c86bbe288c'::uuid, '1fd274a8-1909-5fec-b15f-cafbbde76e88'::uuid, '2cd766aa-0606-513d-ac75-a324541cbb85'::uuid, true, 18, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('0fb93f88-f03d-50c3-91ee-0164890de58c'::uuid, '1fd274a8-1909-5fec-b15f-cafbbde76e88'::uuid, '2ea883df-5db9-5816-bf1e-16395065b814'::uuid, true, 19, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('5e957729-9727-5f8b-ab73-781f1ba7dcbd'::uuid, '1fd274a8-1909-5fec-b15f-cafbbde76e88'::uuid, '6361b3db-2bbb-5a1d-9035-bd931753d9f8'::uuid, true, 20, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('9d8a0529-6570-55d3-a435-81c85640ba3e'::uuid, '1fd274a8-1909-5fec-b15f-cafbbde76e88'::uuid, 'b3d5b58c-b52d-57ed-9eff-e99717bd7685'::uuid, true, 21, 'ACTIVE');

INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('4ea663fb-a39d-50f4-b3c2-8a666add7dd3'::uuid, '6366a9c7-7cd8-5b8b-b369-70ecea46dc76'::uuid, '15020a90-ef38-5cdc-80a0-df4115b9062b'::uuid, true, 10, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('8f75f70a-7ad9-519b-8486-ad356bb17b99'::uuid, '6366a9c7-7cd8-5b8b-b369-70ecea46dc76'::uuid, '1ee95342-36aa-5a40-9ad0-68f8ffd299d8'::uuid, true, 11, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('eb990370-72f9-5d9b-9298-3ce8b7fbd08f'::uuid, '6366a9c7-7cd8-5b8b-b369-70ecea46dc76'::uuid, '7a069df9-69c8-56fa-b1aa-44999ad6e5dc'::uuid, true, 12, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('e7d8a75c-d82f-5b01-8025-2ebd88268d6a'::uuid, '6366a9c7-7cd8-5b8b-b369-70ecea46dc76'::uuid, 'd6ee55d7-2f96-555e-b12f-d0eb520bd0df'::uuid, true, 13, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('b9145aa1-4b8e-515a-8178-9e07b25c6bf4'::uuid, '6366a9c7-7cd8-5b8b-b369-70ecea46dc76'::uuid, 'f2bd62d0-6aa8-51ff-812a-6f1181a8d19f'::uuid, true, 14, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('06094ba5-49bc-5b1f-9a54-69f5d8ab95f3'::uuid, '6366a9c7-7cd8-5b8b-b369-70ecea46dc76'::uuid, '56dee392-89c0-52bc-ab95-2346f750fb45'::uuid, true, 15, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('11a8cf5a-d40f-588b-9351-dedfd4b41e0d'::uuid, '6366a9c7-7cd8-5b8b-b369-70ecea46dc76'::uuid, '2107eb71-ddd6-5261-b8c7-a1a941ac8afa'::uuid, true, 16, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('8641da12-03c0-523d-8caf-5b7f30d67b4a'::uuid, '6366a9c7-7cd8-5b8b-b369-70ecea46dc76'::uuid, '5811f1ab-c4ec-5e51-aa3f-3376ebda16fd'::uuid, true, 17, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('2c118ccb-54aa-5669-a208-72b99929cb33'::uuid, '6366a9c7-7cd8-5b8b-b369-70ecea46dc76'::uuid, '2cd766aa-0606-513d-ac75-a324541cbb85'::uuid, true, 18, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('cd95844e-826c-5509-9d6d-45ee9204b584'::uuid, '6366a9c7-7cd8-5b8b-b369-70ecea46dc76'::uuid, '2ea883df-5db9-5816-bf1e-16395065b814'::uuid, true, 19, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('6e00a884-abb1-5d55-b486-1233871a9321'::uuid, '6366a9c7-7cd8-5b8b-b369-70ecea46dc76'::uuid, '6361b3db-2bbb-5a1d-9035-bd931753d9f8'::uuid, true, 20, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('1b45cb38-57f8-5b56-b89a-f18eddc72cdc'::uuid, '6366a9c7-7cd8-5b8b-b369-70ecea46dc76'::uuid, 'b3d5b58c-b52d-57ed-9eff-e99717bd7685'::uuid, true, 21, 'ACTIVE');

INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('9aea2130-68bc-5678-8535-a87d531e0ac5'::uuid, '5bda60ff-8beb-595a-bf3f-cc634b80c6ab'::uuid, '15020a90-ef38-5cdc-80a0-df4115b9062b'::uuid, true, 10, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('0a9b8482-026c-56fe-a5a5-fec228344296'::uuid, '5bda60ff-8beb-595a-bf3f-cc634b80c6ab'::uuid, '1ee95342-36aa-5a40-9ad0-68f8ffd299d8'::uuid, true, 11, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('f665c768-2604-5dd9-829e-a5d2a34be5fa'::uuid, '5bda60ff-8beb-595a-bf3f-cc634b80c6ab'::uuid, 'ea163c55-9f18-578c-9215-bc19a7254fba'::uuid, true, 12, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('5f2b4261-9d94-5a81-b9e7-4729a07ab323'::uuid, '5bda60ff-8beb-595a-bf3f-cc634b80c6ab'::uuid, 'f6eb8c32-d911-55e0-9e25-11ab94987642'::uuid, true, 13, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('5df06410-9877-5a31-b660-9aa06c2e19d4'::uuid, '5bda60ff-8beb-595a-bf3f-cc634b80c6ab'::uuid, 'fda9d489-770c-530b-bfa9-454eb2008148'::uuid, true, 14, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('a0939323-a177-5aec-83ca-eb94682c9df1'::uuid, '5bda60ff-8beb-595a-bf3f-cc634b80c6ab'::uuid, 'e466124e-4121-54bb-b9bd-5e8a52a6fff4'::uuid, true, 15, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('a2604c73-98c3-56c4-96a4-a441e813b67a'::uuid, '5bda60ff-8beb-595a-bf3f-cc634b80c6ab'::uuid, '56dee392-89c0-52bc-ab95-2346f750fb45'::uuid, true, 16, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('3c4b4f57-9d31-5d7e-8d24-786898c5bb9a'::uuid, '5bda60ff-8beb-595a-bf3f-cc634b80c6ab'::uuid, 'd6ee55d7-2f96-555e-b12f-d0eb520bd0df'::uuid, true, 17, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('5feec7fc-d9ef-51f6-87fc-24771c3ff790'::uuid, '5bda60ff-8beb-595a-bf3f-cc634b80c6ab'::uuid, '0fa0f706-9ffc-5dbc-9c8a-fa5d65983676'::uuid, true, 18, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('e136438f-7526-5538-92d0-fbd7074f21b6'::uuid, '5bda60ff-8beb-595a-bf3f-cc634b80c6ab'::uuid, '2ea883df-5db9-5816-bf1e-16395065b814'::uuid, true, 19, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('c9d2ace5-f312-5933-8d42-0e182d66d91c'::uuid, '5bda60ff-8beb-595a-bf3f-cc634b80c6ab'::uuid, '6361b3db-2bbb-5a1d-9035-bd931753d9f8'::uuid, true, 20, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('60289d85-6167-58b6-888e-3f8fbff83320'::uuid, '5bda60ff-8beb-595a-bf3f-cc634b80c6ab'::uuid, 'b3d5b58c-b52d-57ed-9eff-e99717bd7685'::uuid, true, 21, 'ACTIVE');

INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('1fee47f2-093d-5ffb-8cb5-db767877bca1'::uuid, '1e8fdd3c-01b8-5683-8a10-d0424f61bb7f'::uuid, '15020a90-ef38-5cdc-80a0-df4115b9062b'::uuid, true, 10, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('1f1ebe40-04d0-50c9-b7d3-8ddda6f742c8'::uuid, '1e8fdd3c-01b8-5683-8a10-d0424f61bb7f'::uuid, '1ee95342-36aa-5a40-9ad0-68f8ffd299d8'::uuid, true, 11, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('29397d98-2d63-5993-b5a2-9923c5954aba'::uuid, '1e8fdd3c-01b8-5683-8a10-d0424f61bb7f'::uuid, 'ea163c55-9f18-578c-9215-bc19a7254fba'::uuid, true, 12, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('57b4d981-f084-52ee-8333-e940d6831883'::uuid, '1e8fdd3c-01b8-5683-8a10-d0424f61bb7f'::uuid, '56dee392-89c0-52bc-ab95-2346f750fb45'::uuid, true, 13, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('d8f37e4b-7c23-5849-8793-95fcdef4fe4a'::uuid, '1e8fdd3c-01b8-5683-8a10-d0424f61bb7f'::uuid, 'd6ee55d7-2f96-555e-b12f-d0eb520bd0df'::uuid, true, 14, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('4ec91260-b602-5e54-9e8f-d965996846f0'::uuid, '1e8fdd3c-01b8-5683-8a10-d0424f61bb7f'::uuid, '0fa0f706-9ffc-5dbc-9c8a-fa5d65983676'::uuid, true, 15, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('8577f1da-b904-56e2-90c0-c57061fa54f3'::uuid, '1e8fdd3c-01b8-5683-8a10-d0424f61bb7f'::uuid, 'f2bd62d0-6aa8-51ff-812a-6f1181a8d19f'::uuid, true, 16, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('603e9e18-ae8d-5d61-bf8a-a4874b0785a8'::uuid, '1e8fdd3c-01b8-5683-8a10-d0424f61bb7f'::uuid, '2ea883df-5db9-5816-bf1e-16395065b814'::uuid, true, 17, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('5f63ffce-fec9-5d28-ae2a-61b005597663'::uuid, '1e8fdd3c-01b8-5683-8a10-d0424f61bb7f'::uuid, '6361b3db-2bbb-5a1d-9035-bd931753d9f8'::uuid, true, 18, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('6fd2de37-8833-53b6-b8fa-caff9ea05312'::uuid, '1e8fdd3c-01b8-5683-8a10-d0424f61bb7f'::uuid, 'b3d5b58c-b52d-57ed-9eff-e99717bd7685'::uuid, true, 19, 'ACTIVE');

INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('91fa9458-1f8e-5d3f-b941-2eb3a84d06a3'::uuid, '922826a8-8980-5899-9371-8b79745affa1'::uuid, '15020a90-ef38-5cdc-80a0-df4115b9062b'::uuid, true, 10, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('2d2d09e7-66e5-56fa-acac-1158dee01dbf'::uuid, '922826a8-8980-5899-9371-8b79745affa1'::uuid, '1ee95342-36aa-5a40-9ad0-68f8ffd299d8'::uuid, true, 11, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('7a3c3aec-e29d-5a9d-a0cb-aa997eb452f1'::uuid, '922826a8-8980-5899-9371-8b79745affa1'::uuid, '56dee392-89c0-52bc-ab95-2346f750fb45'::uuid, true, 12, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('94a3e10e-6da8-5958-a431-909d66e83394'::uuid, '922826a8-8980-5899-9371-8b79745affa1'::uuid, '40aa63fd-d850-54bc-91f7-30145d369a03'::uuid, true, 13, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('bb0bffd8-0c9f-59b8-80ba-9833dc0ff462'::uuid, '922826a8-8980-5899-9371-8b79745affa1'::uuid, 'f6eb8c32-d911-55e0-9e25-11ab94987642'::uuid, true, 14, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('ce8cc1d8-fdc2-5fba-984b-67d01d6de0df'::uuid, '922826a8-8980-5899-9371-8b79745affa1'::uuid, 'fda9d489-770c-530b-bfa9-454eb2008148'::uuid, true, 15, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('1edd3695-b725-5389-8ef8-1599e2fb74f4'::uuid, '922826a8-8980-5899-9371-8b79745affa1'::uuid, 'e466124e-4121-54bb-b9bd-5e8a52a6fff4'::uuid, true, 16, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('3cb23089-beca-5a3d-9ecf-373c4a0ab539'::uuid, '922826a8-8980-5899-9371-8b79745affa1'::uuid, '052569d5-c6b8-5a16-ad75-f9b272260e58'::uuid, true, 17, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('62ae8abc-ce8e-5815-897c-00edb027196f'::uuid, '922826a8-8980-5899-9371-8b79745affa1'::uuid, '13688561-48e6-5cfa-b9d8-64ca417d9679'::uuid, true, 18, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('4bcca08f-54a1-51e7-9253-626e09e9c472'::uuid, '922826a8-8980-5899-9371-8b79745affa1'::uuid, '932c295d-49cf-5c0b-a2a3-7f3b5c61311b'::uuid, true, 19, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('54137960-cde2-5df5-92a5-e5a35aeb2a3f'::uuid, '922826a8-8980-5899-9371-8b79745affa1'::uuid, 'd6ee55d7-2f96-555e-b12f-d0eb520bd0df'::uuid, true, 20, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('c17568e7-8206-5f3c-955e-316695b28b8a'::uuid, '922826a8-8980-5899-9371-8b79745affa1'::uuid, '0fa0f706-9ffc-5dbc-9c8a-fa5d65983676'::uuid, true, 21, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('ee29233d-c514-5432-b455-f53e276d0826'::uuid, '922826a8-8980-5899-9371-8b79745affa1'::uuid, '2ea883df-5db9-5816-bf1e-16395065b814'::uuid, true, 22, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('e03ddd76-e6d7-5c41-aeaf-a0e14db4e6a4'::uuid, '922826a8-8980-5899-9371-8b79745affa1'::uuid, 'da92aee8-b16a-5a47-95c7-8425b40c919a'::uuid, true, 23, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('e6064057-ab8b-5de3-bea7-e1c0505cb19f'::uuid, '922826a8-8980-5899-9371-8b79745affa1'::uuid, 'b3d5b58c-b52d-57ed-9eff-e99717bd7685'::uuid, true, 24, 'ACTIVE');

INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('5985bc75-00ed-5195-b2f0-1e74c796c676'::uuid, 'b5791934-6206-5532-9d66-bb24d951afd3'::uuid, '15020a90-ef38-5cdc-80a0-df4115b9062b'::uuid, true, 10, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('aed67f8d-8950-56df-9a26-e358dac53c26'::uuid, 'b5791934-6206-5532-9d66-bb24d951afd3'::uuid, '1ee95342-36aa-5a40-9ad0-68f8ffd299d8'::uuid, true, 11, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('242ed0ba-c269-5c5b-a363-13aef24f6e0e'::uuid, 'b5791934-6206-5532-9d66-bb24d951afd3'::uuid, 'f6eb8c32-d911-55e0-9e25-11ab94987642'::uuid, true, 12, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('e3a25328-216e-544f-8a0c-b12ab34349ba'::uuid, 'b5791934-6206-5532-9d66-bb24d951afd3'::uuid, 'fda9d489-770c-530b-bfa9-454eb2008148'::uuid, true, 13, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('7d656c15-8db9-5e1b-839d-9ad49d2d00f0'::uuid, 'b5791934-6206-5532-9d66-bb24d951afd3'::uuid, 'e466124e-4121-54bb-b9bd-5e8a52a6fff4'::uuid, true, 14, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('266e00f9-2c2d-5905-b1f8-30baa1f4e9c8'::uuid, 'b5791934-6206-5532-9d66-bb24d951afd3'::uuid, 'd6ee55d7-2f96-555e-b12f-d0eb520bd0df'::uuid, true, 15, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('ffaf52ed-9609-56e6-8acf-ca8f17b4b693'::uuid, 'b5791934-6206-5532-9d66-bb24d951afd3'::uuid, '0fa0f706-9ffc-5dbc-9c8a-fa5d65983676'::uuid, true, 16, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('589dc945-09e8-5823-b3b7-429224079b94'::uuid, 'b5791934-6206-5532-9d66-bb24d951afd3'::uuid, '78880073-69b4-5ce0-86cc-8f53d987a7e6'::uuid, true, 17, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('343d81d1-2379-581c-a70b-90bbd1973df6'::uuid, 'b5791934-6206-5532-9d66-bb24d951afd3'::uuid, 'd6c7a349-9547-50b5-b92f-146c47df0246'::uuid, true, 18, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('d9d4c709-939f-572c-802c-cfde37fae06d'::uuid, 'b5791934-6206-5532-9d66-bb24d951afd3'::uuid, '13688561-48e6-5cfa-b9d8-64ca417d9679'::uuid, true, 19, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('c51d1103-a5f6-58ee-8224-2d46dc8283db'::uuid, 'b5791934-6206-5532-9d66-bb24d951afd3'::uuid, '932c295d-49cf-5c0b-a2a3-7f3b5c61311b'::uuid, true, 20, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('31ffb207-928a-5492-b9d9-09b1597c8fc3'::uuid, 'b5791934-6206-5532-9d66-bb24d951afd3'::uuid, 'da92aee8-b16a-5a47-95c7-8425b40c919a'::uuid, true, 21, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('b5c12e37-6d32-5079-8cee-f24d45b15d05'::uuid, 'b5791934-6206-5532-9d66-bb24d951afd3'::uuid, 'b3d5b58c-b52d-57ed-9eff-e99717bd7685'::uuid, true, 22, 'ACTIVE');

INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('06ce8483-49ff-5808-b3ea-18c811000cfc'::uuid, '1937f819-c38b-5dcd-adcb-14d9d726a142'::uuid, '15020a90-ef38-5cdc-80a0-df4115b9062b'::uuid, true, 10, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('c3fd3f75-416d-5fd1-a0e3-4bacb4b22065'::uuid, '1937f819-c38b-5dcd-adcb-14d9d726a142'::uuid, '1ee95342-36aa-5a40-9ad0-68f8ffd299d8'::uuid, true, 11, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('56736b13-e8ae-537e-80ca-6bfff6a51e6a'::uuid, '1937f819-c38b-5dcd-adcb-14d9d726a142'::uuid, '56dee392-89c0-52bc-ab95-2346f750fb45'::uuid, true, 12, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('ec310a1d-6333-5c93-8922-f4208d52ad3a'::uuid, '1937f819-c38b-5dcd-adcb-14d9d726a142'::uuid, '40aa63fd-d850-54bc-91f7-30145d369a03'::uuid, true, 13, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('d72933e6-04e9-519b-9a98-216fdc3be2cf'::uuid, '1937f819-c38b-5dcd-adcb-14d9d726a142'::uuid, '052569d5-c6b8-5a16-ad75-f9b272260e58'::uuid, true, 14, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('79b9bafb-d9b8-54ee-a3a1-709d23633091'::uuid, '1937f819-c38b-5dcd-adcb-14d9d726a142'::uuid, '13688561-48e6-5cfa-b9d8-64ca417d9679'::uuid, true, 15, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('a25cf9c2-dd49-52dc-8b39-ae39b0507801'::uuid, '1937f819-c38b-5dcd-adcb-14d9d726a142'::uuid, '932c295d-49cf-5c0b-a2a3-7f3b5c61311b'::uuid, true, 16, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('1f970a41-7976-53bd-b799-e2512682f3c4'::uuid, '1937f819-c38b-5dcd-adcb-14d9d726a142'::uuid, 'd6ee55d7-2f96-555e-b12f-d0eb520bd0df'::uuid, true, 17, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('e3f4a058-74ad-5cef-ab13-192d99293eab'::uuid, '1937f819-c38b-5dcd-adcb-14d9d726a142'::uuid, '0fa0f706-9ffc-5dbc-9c8a-fa5d65983676'::uuid, true, 18, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('0db835d2-7eea-5649-a2ca-a0d8699b846a'::uuid, '1937f819-c38b-5dcd-adcb-14d9d726a142'::uuid, 'b7454817-9da3-5738-af31-1a98c561a916'::uuid, true, 19, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('de3b252f-f73d-597e-9656-5aa8700e23d1'::uuid, '1937f819-c38b-5dcd-adcb-14d9d726a142'::uuid, '2ea883df-5db9-5816-bf1e-16395065b814'::uuid, true, 20, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('abf53411-e6ae-56c9-a298-d1b38311e124'::uuid, '1937f819-c38b-5dcd-adcb-14d9d726a142'::uuid, 'da92aee8-b16a-5a47-95c7-8425b40c919a'::uuid, true, 21, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('d443077b-1b6b-5d31-8502-f3517620673f'::uuid, '1937f819-c38b-5dcd-adcb-14d9d726a142'::uuid, 'b3d5b58c-b52d-57ed-9eff-e99717bd7685'::uuid, true, 22, 'ACTIVE');

INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('3790e651-c85f-5992-9ae6-9a8f41575c6b'::uuid, '8a3a5878-e5b9-501f-a503-3dc254030d3b'::uuid, '15020a90-ef38-5cdc-80a0-df4115b9062b'::uuid, true, 10, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('d9f2a670-c26a-53cc-a57b-977d02e0ea69'::uuid, '8a3a5878-e5b9-501f-a503-3dc254030d3b'::uuid, '1ee95342-36aa-5a40-9ad0-68f8ffd299d8'::uuid, true, 11, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('8bad1ed0-38fb-5bac-beaf-e581bdf9c245'::uuid, '8a3a5878-e5b9-501f-a503-3dc254030d3b'::uuid, '56dee392-89c0-52bc-ab95-2346f750fb45'::uuid, true, 12, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('a51b5bfc-6b32-5d41-8bc5-90773b75622c'::uuid, '8a3a5878-e5b9-501f-a503-3dc254030d3b'::uuid, '40aa63fd-d850-54bc-91f7-30145d369a03'::uuid, true, 13, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('7787dde2-e12b-53ca-9de0-cae82606a0b4'::uuid, '8a3a5878-e5b9-501f-a503-3dc254030d3b'::uuid, 'd6ee55d7-2f96-555e-b12f-d0eb520bd0df'::uuid, true, 14, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('12c5f771-b006-588f-9436-6cc655bdf60c'::uuid, '8a3a5878-e5b9-501f-a503-3dc254030d3b'::uuid, '0fa0f706-9ffc-5dbc-9c8a-fa5d65983676'::uuid, true, 15, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('79a9db6b-3667-599e-891f-53866252a855'::uuid, '8a3a5878-e5b9-501f-a503-3dc254030d3b'::uuid, 'b7454817-9da3-5738-af31-1a98c561a916'::uuid, true, 16, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('e034d131-90e4-5330-bea4-e2875690ed76'::uuid, '8a3a5878-e5b9-501f-a503-3dc254030d3b'::uuid, '2ea883df-5db9-5816-bf1e-16395065b814'::uuid, true, 17, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('c2606da9-4824-5324-99de-ccab79c33849'::uuid, '8a3a5878-e5b9-501f-a503-3dc254030d3b'::uuid, 'da92aee8-b16a-5a47-95c7-8425b40c919a'::uuid, true, 18, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('80c9b622-c639-59d3-bcbc-8fa75aa3f643'::uuid, '8a3a5878-e5b9-501f-a503-3dc254030d3b'::uuid, 'b3d5b58c-b52d-57ed-9eff-e99717bd7685'::uuid, true, 19, 'ACTIVE');

INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('fbbb0101-9d3a-5d51-a85b-f191eb3c4c0a'::uuid, 'f3ef675c-53a2-5d7c-9c9f-60a9d97a35ee'::uuid, '15020a90-ef38-5cdc-80a0-df4115b9062b'::uuid, true, 10, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('4501e3f0-108a-55e1-87dc-eab59f3f970a'::uuid, 'f3ef675c-53a2-5d7c-9c9f-60a9d97a35ee'::uuid, '1ee95342-36aa-5a40-9ad0-68f8ffd299d8'::uuid, true, 11, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('fdb7df4f-8bbf-5046-82d6-731e10e752be'::uuid, 'f3ef675c-53a2-5d7c-9c9f-60a9d97a35ee'::uuid, '56dee392-89c0-52bc-ab95-2346f750fb45'::uuid, true, 12, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('4869ba13-ab7f-5a9d-b88a-500685cc6d96'::uuid, 'f3ef675c-53a2-5d7c-9c9f-60a9d97a35ee'::uuid, '052569d5-c6b8-5a16-ad75-f9b272260e58'::uuid, true, 13, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('fbaa8b9a-6c39-59f3-8f55-a78984686090'::uuid, 'f3ef675c-53a2-5d7c-9c9f-60a9d97a35ee'::uuid, '13688561-48e6-5cfa-b9d8-64ca417d9679'::uuid, true, 14, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('b2f6028b-83b9-5c30-84de-955ef3becf61'::uuid, 'f3ef675c-53a2-5d7c-9c9f-60a9d97a35ee'::uuid, '932c295d-49cf-5c0b-a2a3-7f3b5c61311b'::uuid, true, 15, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('f5e55f22-4110-5b08-8f47-dc1ab032c609'::uuid, 'f3ef675c-53a2-5d7c-9c9f-60a9d97a35ee'::uuid, 'd6ee55d7-2f96-555e-b12f-d0eb520bd0df'::uuid, true, 16, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('f6fa5760-776b-564d-8533-cce96299f6be'::uuid, 'f3ef675c-53a2-5d7c-9c9f-60a9d97a35ee'::uuid, '0fa0f706-9ffc-5dbc-9c8a-fa5d65983676'::uuid, true, 17, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('dd9e7eb0-f849-5834-a2a1-0c03a6c5f00f'::uuid, 'f3ef675c-53a2-5d7c-9c9f-60a9d97a35ee'::uuid, 'b7454817-9da3-5738-af31-1a98c561a916'::uuid, true, 18, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('7f67fee2-e607-5d6d-85f9-56ff1744d8b8'::uuid, 'f3ef675c-53a2-5d7c-9c9f-60a9d97a35ee'::uuid, 'f959daf7-7cb8-5342-b594-57b9a165a97c'::uuid, true, 19, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('74dc0caa-d1ee-56e6-a80e-7fde7a7e4a5b'::uuid, 'f3ef675c-53a2-5d7c-9c9f-60a9d97a35ee'::uuid, '2ea883df-5db9-5816-bf1e-16395065b814'::uuid, true, 20, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('bae5d68f-0c82-5708-bcbe-7aa7078214f6'::uuid, 'f3ef675c-53a2-5d7c-9c9f-60a9d97a35ee'::uuid, 'da92aee8-b16a-5a47-95c7-8425b40c919a'::uuid, true, 21, 'ACTIVE');
INSERT INTO core.moment_type_capability (moment_type_capability_id, moment_type_id, capability_id, is_default, sort_order, status)
VALUES ('a1e3e331-e554-5d84-908c-8fb501b462a4'::uuid, 'f3ef675c-53a2-5d7c-9c9f-60a9d97a35ee'::uuid, 'b3d5b58c-b52d-57ed-9eff-e99717bd7685'::uuid, true, 22, 'ACTIVE');

COMMIT;
