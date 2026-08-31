BEGIN;

-- Baseline system permissions and roles. Role bundles are reviewable production configuration.
INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
VALUES ('a61889e1-7fb5-5578-bc5e-280432f327a0'::uuid, 'MOMENT_VIEW', 'View Moment', 'View Moment permission.', 'MOMENT', 'VIEW', 'STANDARD', 'ACTIVE');

INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
VALUES ('198a66c9-df74-5b8d-8778-2f09ebd53599'::uuid, 'MOMENT_UPDATE', 'Update Moment', 'Update Moment permission.', 'MOMENT', 'UPDATE', 'STANDARD', 'ACTIVE');

INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
VALUES ('7b0fea0a-8542-5821-af82-b1c1b8aafeee'::uuid, 'PARTICIPANT_MANAGE', 'Manage Participant', 'Manage Participant permission.', 'PARTICIPANT', 'MANAGE', 'SENSITIVE', 'ACTIVE');

INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
VALUES ('d3884b5c-e9d6-5b36-a5a7-ed95fa7264df'::uuid, 'PLANNING_ITEM_CREATE', 'Add Planning Item', 'Add Planning Item permission.', 'PLANNING_ITEM', 'CREATE', 'STANDARD', 'ACTIVE');

INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
VALUES ('86f2223e-2338-51c9-8b3b-fe726f2c524f'::uuid, 'BOOKING_CREATE', 'Add Booking', 'Add Booking permission.', 'BOOKING', 'CREATE', 'SENSITIVE', 'ACTIVE');

INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
VALUES ('4132868a-d43f-5df9-b972-e8f0f1cf84f3'::uuid, 'EXPENSE_CREATE', 'Add Expense', 'Add Expense permission.', 'EXPENSE', 'CREATE', 'SENSITIVE', 'ACTIVE');

INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
VALUES ('99a3ea10-4b44-592d-9ed3-1dc5048fb6a5'::uuid, 'BUDGET_MANAGE', 'Manage Budget', 'Manage Budget permission.', 'BUDGET', 'MANAGE', 'SENSITIVE', 'ACTIVE');

INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
VALUES ('d035e6bb-4dad-5d59-9039-211fc8e5de3b'::uuid, 'CONTRIBUTION_RECORD', 'Record Contribution', 'Record Contribution permission.', 'CONTRIBUTION', 'RECORD', 'SENSITIVE', 'ACTIVE');

INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
VALUES ('8ba78d23-b81e-503b-aa15-da211a77a43d'::uuid, 'SETTLEMENT_RECORD', 'Record Settlement', 'Record Settlement permission.', 'SETTLEMENT', 'RECORD', 'HIGHLY_SENSITIVE', 'ACTIVE');

INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
VALUES ('c63908c7-1b74-5556-bc44-f0a0c1f9d937'::uuid, 'VENDOR_MANAGE', 'Manage Vendor', 'Manage Vendor permission.', 'VENDOR', 'MANAGE', 'SENSITIVE', 'ACTIVE');

INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
VALUES ('b8b323d0-b81d-56bd-ae38-9e379eb65a38'::uuid, 'UPDATE_CREATE', 'Add Update', 'Add Update permission.', 'UPDATE', 'CREATE', 'STANDARD', 'ACTIVE');

INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
VALUES ('5dfe8f98-be4d-577e-8daf-f241a9ffba09'::uuid, 'POLL_CREATE', 'Create Poll', 'Create Poll permission.', 'POLL', 'CREATE', 'STANDARD', 'ACTIVE');

INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
VALUES ('4aecda1f-0e91-563f-b2b2-d87a196e5135'::uuid, 'MEMORY_CREATE', 'Create Memory', 'Create Memory permission.', 'MEMORY', 'CREATE', 'SENSITIVE', 'ACTIVE');

INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
VALUES ('0cdba34e-cb73-5d65-8a49-5f39efe9de33'::uuid, 'ATTENDANCE_RECORD', 'Record Attendance', 'Record Attendance permission.', 'ATTENDANCE', 'RECORD', 'STANDARD', 'ACTIVE');

INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
VALUES ('11702c0d-be0f-5647-8b6f-6a7d9b566f17'::uuid, 'PURCHASE_ITEM_CREATE', 'Add Purchase Item', 'Add Purchase Item permission.', 'PURCHASE_ITEM', 'CREATE', 'STANDARD', 'ACTIVE');

INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
VALUES ('ceba4928-fe6f-565d-9824-516cb3781ed0'::uuid, 'OWNERSHIP_MANAGE', 'Manage Ownership', 'Manage Ownership permission.', 'OWNERSHIP', 'MANAGE', 'SENSITIVE', 'ACTIVE');

INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
VALUES ('ce812b7e-27a2-591d-ab57-6f71ee1e491d'::uuid, 'DELIVERY_HANDOVER_RECORD', 'Record Delivery / Handover', 'Record Delivery / Handover permission.', 'DELIVERY_HANDOVER', 'RECORD', 'STANDARD', 'ACTIVE');

INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
VALUES ('27f5996e-df39-59a7-b41b-cdb3925884b4'::uuid, 'RESIDENT_MANAGE', 'Manage Resident', 'Manage Resident permission.', 'RESIDENT', 'MANAGE', 'SENSITIVE', 'ACTIVE');

INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
VALUES ('47938492-5832-5a2d-9608-ed10f589a32b'::uuid, 'TASK_CREATE', 'Create Task', 'Create Task permission.', 'TASK', 'CREATE', 'STANDARD', 'ACTIVE');

INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
VALUES ('c55cd955-6799-552b-868e-87c019f14666'::uuid, 'TASK_ASSIGN', 'Assign Task', 'Assign Task permission.', 'TASK', 'MANAGE', 'STANDARD', 'ACTIVE');

INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
VALUES ('92cdf9d8-418e-5f8f-b6c8-b890539b9e30'::uuid, 'RULE_MANAGE', 'Manage Rule', 'Manage Rule permission.', 'RULE', 'MANAGE', 'STANDARD', 'ACTIVE');

INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
VALUES ('06120999-c8d2-5e68-a46a-5f64148cc9bc'::uuid, 'ASSET_MANAGE', 'Manage Asset', 'Manage Asset permission.', 'ASSET', 'MANAGE', 'SENSITIVE', 'ACTIVE');

INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
VALUES ('3ed4e386-1a37-5616-b883-cbaf52311a06'::uuid, 'MAINTENANCE_CREATE', 'Add Maintenance', 'Add Maintenance permission.', 'MAINTENANCE', 'CREATE', 'STANDARD', 'ACTIVE');

INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
VALUES ('bf8a8420-5fe3-5877-af10-94f69f1db9c8'::uuid, 'GOAL_CREATE', 'Create Goal', 'Create Goal permission.', 'GOAL', 'CREATE', 'STANDARD', 'ACTIVE');

INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
VALUES ('6222cd9f-6bc6-5f9a-a82a-30e78d3f6b45'::uuid, 'MILESTONE_CREATE', 'Create Milestone', 'Create Milestone permission.', 'MILESTONE', 'CREATE', 'STANDARD', 'ACTIVE');

INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
VALUES ('d6368e73-a6a0-5643-956c-9d72fb5e5699'::uuid, 'PROGRESS_RECORD', 'Record Progress', 'Record Progress permission.', 'PROGRESS', 'RECORD', 'STANDARD', 'ACTIVE');

INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
VALUES ('1292eb10-8357-586c-9714-679a37ebaa8e'::uuid, 'OPPORTUNITY_CREATE', 'Create Opportunity', 'Create Opportunity permission.', 'OPPORTUNITY', 'CREATE', 'SENSITIVE', 'ACTIVE');

INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
VALUES ('b8942f13-cf56-526c-b352-84ff3c729dce'::uuid, 'PIVOT_RECORD', 'Record Pivot', 'Record Pivot permission.', 'PIVOT', 'RECORD', 'SENSITIVE', 'ACTIVE');

INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
VALUES ('2f81b0fa-17a6-5f54-8fc5-1d718046cfcc'::uuid, 'LEARNING_ACTIVITY_CREATE', 'Add Learning Activity', 'Add Learning Activity permission.', 'LEARNING_ACTIVITY', 'CREATE', 'STANDARD', 'ACTIVE');

INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
VALUES ('e53d9abd-10e4-52ce-a777-8cb5031dd5c1'::uuid, 'LIFE_OBSERVATION_RECORD', 'Record Life Observation', 'Record Life Observation permission.', 'LIFE_OBSERVATION', 'RECORD', 'HIGHLY_SENSITIVE', 'ACTIVE');

INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
VALUES ('3b121a31-d7ea-569e-961e-34408f06f7ed'::uuid, 'LIFESTYLE_ACTIVITY_CREATE', 'Add Lifestyle Activity', 'Add Lifestyle Activity permission.', 'LIFESTYLE_ACTIVITY', 'CREATE', 'SENSITIVE', 'ACTIVE');

INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
VALUES ('4c3e90db-5285-50be-a8e6-e114618ec053'::uuid, 'RELATIONSHIP_ACTIVITY_RECORD', 'Record Relationship Activity', 'Record Relationship Activity permission.', 'RELATIONSHIP_ACTIVITY', 'RECORD', 'HIGHLY_SENSITIVE', 'ACTIVE');

INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
VALUES ('60832409-583b-52a1-bdd6-5f5f9a4e8461'::uuid, 'ISSUE_CREATE', 'Create Issue', 'Create Issue permission.', 'ISSUE', 'CREATE', 'SENSITIVE', 'ACTIVE');

INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
VALUES ('84768b8c-3306-5adf-8d28-d8646a806805'::uuid, 'RISK_CREATE', 'Create Risk', 'Create Risk permission.', 'RISK', 'CREATE', 'SENSITIVE', 'ACTIVE');

INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
VALUES ('4c6339c7-d163-5313-9546-c1193e600fe3'::uuid, 'DECISION_RECORD', 'Record Decision', 'Record Decision permission.', 'DECISION', 'RECORD', 'SENSITIVE', 'ACTIVE');

INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
VALUES ('7f25b8e7-7c8e-569c-8c92-ff661affcc23'::uuid, 'REVENUE_RECORD', 'Record Revenue', 'Record Revenue permission.', 'REVENUE', 'RECORD', 'HIGHLY_SENSITIVE', 'ACTIVE');

INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
VALUES ('7ca18c55-7660-548f-ac2a-707898f4557a'::uuid, 'INVOICE_CREATE', 'Create Invoice', 'Create Invoice permission.', 'INVOICE', 'CREATE', 'HIGHLY_SENSITIVE', 'ACTIVE');

INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
VALUES ('f4828640-92ea-548f-9f93-854541f1e94d'::uuid, 'SLA_MANAGE', 'Manage SLA', 'Manage SLA permission.', 'SLA', 'MANAGE', 'SENSITIVE', 'ACTIVE');

INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
VALUES ('b3814be0-0bdd-580c-8437-836e85d92e20'::uuid, 'REVIEW_CREATE', 'Create Review', 'Create Review permission.', 'REVIEW', 'CREATE', 'STANDARD', 'ACTIVE');

INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
VALUES ('456f701a-ecef-5ade-b993-aad79d56dc30'::uuid, 'EXPENSE_APPROVE', 'Approve Expense', 'Approve Expense permission.', 'EXPENSE', 'APPROVE', 'HIGHLY_SENSITIVE', 'ACTIVE');

INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
VALUES ('f0380b6a-794e-5143-aff3-f1373cba3cb2'::uuid, 'EXPENSE_REVERSE', 'Reverse Expense', 'Reverse Expense permission.', 'EXPENSE', 'EXECUTE', 'HIGHLY_SENSITIVE', 'ACTIVE');

INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
VALUES ('2c540ecd-b255-5327-9f1a-d078b3a11a02'::uuid, 'INVOICE_APPLY_PAYMENT', 'Apply Invoice Payment', 'Apply Invoice Payment permission.', 'INVOICE', 'RECORD', 'HIGHLY_SENSITIVE', 'ACTIVE');

INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
VALUES ('df1ecc13-657a-5a7b-b012-d22f914597ca'::uuid, 'ROLE_ASSIGN', 'Assign Role', 'Assign Role permission.', 'ROLE_ASSIGNMENT', 'MANAGE', 'HIGHLY_SENSITIVE', 'ACTIVE');

INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
VALUES ('eb9aa768-b91e-5d83-b691-c372e11f7449'::uuid, 'CONSENT_MANAGE', 'Manage Consent', 'Manage Consent permission.', 'CONSENT', 'MANAGE', 'HIGHLY_SENSITIVE', 'ACTIVE');

INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
VALUES ('41de0e66-d2b6-5e3a-894c-33a279d06a02'::uuid, 'POLICY_MANAGE', 'Manage Policy', 'Manage Policy permission.', 'POLICY', 'MANAGE', 'HIGHLY_SENSITIVE', 'ACTIVE');

INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
VALUES ('fe628f26-ca23-5b7e-9154-56c71d8b149c'::uuid, 'APPROVAL_DECIDE', 'Decide Approval', 'Decide Approval permission.', 'APPROVAL', 'APPROVE', 'HIGHLY_SENSITIVE', 'ACTIVE');

INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
VALUES ('c7438c67-dae7-5c11-8083-d8d4caad0d21'::uuid, 'AI_INSIGHT_VIEW', 'View AI Insight', 'View AI Insight permission.', 'AI_INSIGHT', 'VIEW', 'SENSITIVE', 'ACTIVE');

INSERT INTO governance.permission (permission_id, code, display_name, description, resource_type, action_type, sensitivity_level, status)
VALUES ('687bca0a-f930-51b6-8edc-64fe90ac7566'::uuid, 'AI_ACTION_EXECUTE', 'Execute AI Action', 'Execute AI Action permission.', 'AI_ACTION_PROPOSAL', 'EXECUTE', 'HIGHLY_SENSITIVE', 'ACTIVE');

INSERT INTO governance.role (role_id, code, display_name, description, role_type, status)
VALUES ('db51e287-eea9-5b56-8fa5-2f6a325bf5d1'::uuid, 'PERSONAL_OWNER', 'Personal Owner', 'Owner of a Personal scope.', 'SYSTEM', 'ACTIVE');

INSERT INTO governance.role (role_id, code, display_name, description, role_type, status)
VALUES ('8c890a17-ae42-5b6c-b2c7-65989d9a4a40'::uuid, 'GROUP_ORGANIZER', 'Group Organizer', 'Organizer of a Group Moment.', 'SYSTEM', 'ACTIVE');

INSERT INTO governance.role (role_id, code, display_name, description, role_type, status)
VALUES ('362d2715-d339-55dd-841a-0c48021adce1'::uuid, 'GROUP_PARTICIPANT', 'Group Participant', 'Active participant in a Group Moment.', 'SYSTEM', 'ACTIVE');

INSERT INTO governance.role (role_id, code, display_name, description, role_type, status)
VALUES ('1414495b-cde6-56b3-8309-a5a61f0be31a'::uuid, 'BUSINESS_OWNER', 'Business Owner', 'Owner of a Company scope.', 'SYSTEM', 'ACTIVE');

INSERT INTO governance.role (role_id, code, display_name, description, role_type, status)
VALUES ('aaee7924-46ce-5be8-a021-e54b29ebf2fb'::uuid, 'BUSINESS_ADMIN', 'Business Admin', 'Administrative role for a Company.', 'SYSTEM', 'ACTIVE');

INSERT INTO governance.role (role_id, code, display_name, description, role_type, status)
VALUES ('79a3fc7a-c037-58d2-bc91-3de816cff4fc'::uuid, 'BUSINESS_MANAGER', 'Business Manager', 'Manager role for Business operations.', 'SYSTEM', 'ACTIVE');

INSERT INTO governance.role (role_id, code, display_name, description, role_type, status)
VALUES ('12602da7-a8b5-534e-99be-b55b97232c40'::uuid, 'BUSINESS_MEMBER', 'Business Member', 'Standard Company member.', 'SYSTEM', 'ACTIVE');

INSERT INTO governance.role (role_id, code, display_name, description, role_type, status)
VALUES ('dd3af806-b286-5cd7-836d-364934684782'::uuid, 'READ_ONLY_REVIEWER', 'Read Only Reviewer', 'Read-only reviewer role.', 'SYSTEM', 'ACTIVE');

INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('bfecc910-ce76-525a-b5a7-1b0d0be0e1d5'::uuid, 'db51e287-eea9-5b56-8fa5-2f6a325bf5d1'::uuid, '687bca0a-f930-51b6-8edc-64fe90ac7566'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('02c0ea24-5a08-552a-a5f4-239c07358ed1'::uuid, 'db51e287-eea9-5b56-8fa5-2f6a325bf5d1'::uuid, 'c7438c67-dae7-5c11-8083-d8d4caad0d21'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('85e3776f-7b02-5e9d-ab84-e42221d9a00f'::uuid, 'db51e287-eea9-5b56-8fa5-2f6a325bf5d1'::uuid, '06120999-c8d2-5e68-a46a-5f64148cc9bc'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('31dc5c80-a4ec-5525-ad13-23a2a52d2e3d'::uuid, 'db51e287-eea9-5b56-8fa5-2f6a325bf5d1'::uuid, '0cdba34e-cb73-5d65-8a49-5f39efe9de33'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('193c1034-edca-5f8e-9a4d-86981cb3e4b3'::uuid, 'db51e287-eea9-5b56-8fa5-2f6a325bf5d1'::uuid, '86f2223e-2338-51c9-8b3b-fe726f2c524f'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('a170210d-78d4-5df4-a2b2-c71919a97143'::uuid, 'db51e287-eea9-5b56-8fa5-2f6a325bf5d1'::uuid, '99a3ea10-4b44-592d-9ed3-1dc5048fb6a5'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('41f7b824-b6aa-5460-be73-11a58b408085'::uuid, 'db51e287-eea9-5b56-8fa5-2f6a325bf5d1'::uuid, 'eb9aa768-b91e-5d83-b691-c372e11f7449'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('5bdc16d1-b505-53f7-8f28-df94777e0b89'::uuid, 'db51e287-eea9-5b56-8fa5-2f6a325bf5d1'::uuid, 'd035e6bb-4dad-5d59-9039-211fc8e5de3b'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('6b5b3129-08d4-5884-a87c-4fcce8240267'::uuid, 'db51e287-eea9-5b56-8fa5-2f6a325bf5d1'::uuid, '4c6339c7-d163-5313-9546-c1193e600fe3'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('8a38961a-67cb-581a-877c-56e3f0d675f6'::uuid, 'db51e287-eea9-5b56-8fa5-2f6a325bf5d1'::uuid, 'ce812b7e-27a2-591d-ab57-6f71ee1e491d'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('9188a4df-2d36-5cda-8374-aedb14cfc0ec'::uuid, 'db51e287-eea9-5b56-8fa5-2f6a325bf5d1'::uuid, '456f701a-ecef-5ade-b993-aad79d56dc30'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('eb0ec1d2-6736-5fb2-abfa-c22d8031841a'::uuid, 'db51e287-eea9-5b56-8fa5-2f6a325bf5d1'::uuid, '4132868a-d43f-5df9-b972-e8f0f1cf84f3'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('8aa4a594-4c3e-57d0-bdeb-c8281b9a7ba1'::uuid, 'db51e287-eea9-5b56-8fa5-2f6a325bf5d1'::uuid, 'f0380b6a-794e-5143-aff3-f1373cba3cb2'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('cb60dc24-0196-5df3-9456-bf5dda6a1ae8'::uuid, 'db51e287-eea9-5b56-8fa5-2f6a325bf5d1'::uuid, 'bf8a8420-5fe3-5877-af10-94f69f1db9c8'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('0604b480-6404-51ac-94b0-f7d9e2ba1216'::uuid, 'db51e287-eea9-5b56-8fa5-2f6a325bf5d1'::uuid, '2c540ecd-b255-5327-9f1a-d078b3a11a02'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('3a5efc94-3b32-5e03-a78c-3160c8a50954'::uuid, 'db51e287-eea9-5b56-8fa5-2f6a325bf5d1'::uuid, '7ca18c55-7660-548f-ac2a-707898f4557a'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('e3cf99c4-8341-5dfd-b504-dea2ceb7ce08'::uuid, 'db51e287-eea9-5b56-8fa5-2f6a325bf5d1'::uuid, '60832409-583b-52a1-bdd6-5f5f9a4e8461'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('71654ef6-174c-556d-a497-98ebf43d44df'::uuid, 'db51e287-eea9-5b56-8fa5-2f6a325bf5d1'::uuid, '2f81b0fa-17a6-5f54-8fc5-1d718046cfcc'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('07dc72e6-d81e-57c3-a9d2-5792bb8c1c9f'::uuid, 'db51e287-eea9-5b56-8fa5-2f6a325bf5d1'::uuid, '3b121a31-d7ea-569e-961e-34408f06f7ed'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('6d2d7d38-a424-557b-91b6-a49367e646eb'::uuid, 'db51e287-eea9-5b56-8fa5-2f6a325bf5d1'::uuid, 'e53d9abd-10e4-52ce-a777-8cb5031dd5c1'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('8c318658-c25b-5412-9fd2-2438dba783a0'::uuid, 'db51e287-eea9-5b56-8fa5-2f6a325bf5d1'::uuid, '3ed4e386-1a37-5616-b883-cbaf52311a06'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('c5302e13-3c06-551e-bee0-b72fe904fb1e'::uuid, 'db51e287-eea9-5b56-8fa5-2f6a325bf5d1'::uuid, '4aecda1f-0e91-563f-b2b2-d87a196e5135'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('36bf25b2-57f9-5434-aa1d-eea52fac0928'::uuid, 'db51e287-eea9-5b56-8fa5-2f6a325bf5d1'::uuid, '6222cd9f-6bc6-5f9a-a82a-30e78d3f6b45'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('3f3bc214-2f36-570e-8baa-8e737e9013fa'::uuid, 'db51e287-eea9-5b56-8fa5-2f6a325bf5d1'::uuid, '198a66c9-df74-5b8d-8778-2f09ebd53599'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('b2f24371-2a4a-5f6d-b47a-0da861c84ca9'::uuid, 'db51e287-eea9-5b56-8fa5-2f6a325bf5d1'::uuid, 'a61889e1-7fb5-5578-bc5e-280432f327a0'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('06d17069-1809-5881-b90c-d75efcf72cbe'::uuid, 'db51e287-eea9-5b56-8fa5-2f6a325bf5d1'::uuid, '1292eb10-8357-586c-9714-679a37ebaa8e'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('b4e60dc0-9ba0-5826-ba82-49ea4ec939f4'::uuid, 'db51e287-eea9-5b56-8fa5-2f6a325bf5d1'::uuid, 'ceba4928-fe6f-565d-9824-516cb3781ed0'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('5145f86f-8bf0-5cff-a992-6d5e67a30fa1'::uuid, 'db51e287-eea9-5b56-8fa5-2f6a325bf5d1'::uuid, '7b0fea0a-8542-5821-af82-b1c1b8aafeee'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('e4425b07-50a1-52c2-af5a-8a8a5c19c869'::uuid, 'db51e287-eea9-5b56-8fa5-2f6a325bf5d1'::uuid, 'b8942f13-cf56-526c-b352-84ff3c729dce'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('b9d61f4b-e9cb-5ee6-b25c-0686964c7f00'::uuid, 'db51e287-eea9-5b56-8fa5-2f6a325bf5d1'::uuid, 'd3884b5c-e9d6-5b36-a5a7-ed95fa7264df'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('6ba78dea-ccf1-51d4-bc9d-392584530bce'::uuid, 'db51e287-eea9-5b56-8fa5-2f6a325bf5d1'::uuid, '5dfe8f98-be4d-577e-8daf-f241a9ffba09'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('dce29707-7c94-59b0-98cf-a9540008e7ea'::uuid, 'db51e287-eea9-5b56-8fa5-2f6a325bf5d1'::uuid, 'd6368e73-a6a0-5643-956c-9d72fb5e5699'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('69474d44-9f42-5a70-b776-e2b7dceffbea'::uuid, 'db51e287-eea9-5b56-8fa5-2f6a325bf5d1'::uuid, '11702c0d-be0f-5647-8b6f-6a7d9b566f17'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('121ddba1-0332-5e95-a750-724d9c6d5950'::uuid, 'db51e287-eea9-5b56-8fa5-2f6a325bf5d1'::uuid, '4c3e90db-5285-50be-a8e6-e114618ec053'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('07bfebcb-830e-5171-afdd-7a7c3f023901'::uuid, 'db51e287-eea9-5b56-8fa5-2f6a325bf5d1'::uuid, '27f5996e-df39-59a7-b41b-cdb3925884b4'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('32376b7b-8202-5ac0-a1c9-b1b1e22fedb9'::uuid, 'db51e287-eea9-5b56-8fa5-2f6a325bf5d1'::uuid, '7f25b8e7-7c8e-569c-8c92-ff661affcc23'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('da0dbdd5-7d8b-5a69-bed7-649a4e24323e'::uuid, 'db51e287-eea9-5b56-8fa5-2f6a325bf5d1'::uuid, 'b3814be0-0bdd-580c-8437-836e85d92e20'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('f08352cc-f69e-527b-bac2-a53e6b4998b4'::uuid, 'db51e287-eea9-5b56-8fa5-2f6a325bf5d1'::uuid, '84768b8c-3306-5adf-8d28-d8646a806805'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('5c3f4c00-b223-5edf-83f3-6acd237e22ac'::uuid, 'db51e287-eea9-5b56-8fa5-2f6a325bf5d1'::uuid, '92cdf9d8-418e-5f8f-b6c8-b890539b9e30'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('877badb6-c09e-5a72-bd14-f987bc899af4'::uuid, 'db51e287-eea9-5b56-8fa5-2f6a325bf5d1'::uuid, '8ba78d23-b81e-503b-aa15-da211a77a43d'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('f220867a-52a2-58ed-b724-84cc3f765272'::uuid, 'db51e287-eea9-5b56-8fa5-2f6a325bf5d1'::uuid, 'f4828640-92ea-548f-9f93-854541f1e94d'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('8d31781b-51df-5837-a1e1-002e02d9d053'::uuid, 'db51e287-eea9-5b56-8fa5-2f6a325bf5d1'::uuid, 'c55cd955-6799-552b-868e-87c019f14666'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('30cbeee2-e151-5c84-a04d-ced99e295d7b'::uuid, 'db51e287-eea9-5b56-8fa5-2f6a325bf5d1'::uuid, '47938492-5832-5a2d-9608-ed10f589a32b'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('f96a9fd0-4a5e-512a-b7c1-fb9dc68c1948'::uuid, 'db51e287-eea9-5b56-8fa5-2f6a325bf5d1'::uuid, 'b8b323d0-b81d-56bd-ae38-9e379eb65a38'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('1c78fd85-b03a-5921-9e87-1873715be489'::uuid, 'db51e287-eea9-5b56-8fa5-2f6a325bf5d1'::uuid, 'c63908c7-1b74-5556-bc44-f0a0c1f9d937'::uuid);

INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('5db98319-67be-5679-93b8-aa63043e3690'::uuid, '8c890a17-ae42-5b6c-b2c7-65989d9a4a40'::uuid, '687bca0a-f930-51b6-8edc-64fe90ac7566'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('237302d2-5c48-536f-8c78-66681096ce2b'::uuid, '8c890a17-ae42-5b6c-b2c7-65989d9a4a40'::uuid, 'c7438c67-dae7-5c11-8083-d8d4caad0d21'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('fa6c8708-6a2a-5525-8f5b-3ec3919b4cc0'::uuid, '8c890a17-ae42-5b6c-b2c7-65989d9a4a40'::uuid, 'fe628f26-ca23-5b7e-9154-56c71d8b149c'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('4ee12110-2aa8-5302-b750-235e2f7e7425'::uuid, '8c890a17-ae42-5b6c-b2c7-65989d9a4a40'::uuid, '06120999-c8d2-5e68-a46a-5f64148cc9bc'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('aa6d6540-8618-55fa-b501-b811d7e46427'::uuid, '8c890a17-ae42-5b6c-b2c7-65989d9a4a40'::uuid, '0cdba34e-cb73-5d65-8a49-5f39efe9de33'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('a28a11bf-dcda-593e-b36b-ba6970526984'::uuid, '8c890a17-ae42-5b6c-b2c7-65989d9a4a40'::uuid, '86f2223e-2338-51c9-8b3b-fe726f2c524f'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('6aa8e945-53ad-5e99-904d-ce522e9de1f8'::uuid, '8c890a17-ae42-5b6c-b2c7-65989d9a4a40'::uuid, '99a3ea10-4b44-592d-9ed3-1dc5048fb6a5'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('9cb8929a-ea4c-5d78-8b4d-c6cd4a8852fe'::uuid, '8c890a17-ae42-5b6c-b2c7-65989d9a4a40'::uuid, 'd035e6bb-4dad-5d59-9039-211fc8e5de3b'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('1373d56e-4f8a-5b67-8200-2cc58345a8f3'::uuid, '8c890a17-ae42-5b6c-b2c7-65989d9a4a40'::uuid, 'ce812b7e-27a2-591d-ab57-6f71ee1e491d'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('97186436-f473-5802-9252-9a7c64e4144c'::uuid, '8c890a17-ae42-5b6c-b2c7-65989d9a4a40'::uuid, '4132868a-d43f-5df9-b972-e8f0f1cf84f3'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('8aedb425-c367-5fbf-8d6c-d16a5f74fd49'::uuid, '8c890a17-ae42-5b6c-b2c7-65989d9a4a40'::uuid, 'bf8a8420-5fe3-5877-af10-94f69f1db9c8'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('dc38f49b-640c-520d-899b-09feb938eee0'::uuid, '8c890a17-ae42-5b6c-b2c7-65989d9a4a40'::uuid, '3ed4e386-1a37-5616-b883-cbaf52311a06'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('753120fe-2ca6-518a-9c7c-3cce1a701c96'::uuid, '8c890a17-ae42-5b6c-b2c7-65989d9a4a40'::uuid, '4aecda1f-0e91-563f-b2b2-d87a196e5135'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('0dc2e4d6-6381-5490-ac8b-d0d83bca63b8'::uuid, '8c890a17-ae42-5b6c-b2c7-65989d9a4a40'::uuid, '6222cd9f-6bc6-5f9a-a82a-30e78d3f6b45'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('67352e2a-7713-56dd-9aec-eeb0ebfaad05'::uuid, '8c890a17-ae42-5b6c-b2c7-65989d9a4a40'::uuid, '198a66c9-df74-5b8d-8778-2f09ebd53599'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('432f0a3a-8431-57a0-9a0a-a0dcd9e63251'::uuid, '8c890a17-ae42-5b6c-b2c7-65989d9a4a40'::uuid, 'a61889e1-7fb5-5578-bc5e-280432f327a0'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('b3d46dad-7039-5d21-8fc1-14ea958a673a'::uuid, '8c890a17-ae42-5b6c-b2c7-65989d9a4a40'::uuid, 'ceba4928-fe6f-565d-9824-516cb3781ed0'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('0c72b232-8b14-5b61-afb6-8ddc7be5e41b'::uuid, '8c890a17-ae42-5b6c-b2c7-65989d9a4a40'::uuid, '7b0fea0a-8542-5821-af82-b1c1b8aafeee'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('2c5ce0a6-d85b-5017-9da1-38b33d06a669'::uuid, '8c890a17-ae42-5b6c-b2c7-65989d9a4a40'::uuid, 'd3884b5c-e9d6-5b36-a5a7-ed95fa7264df'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('b2afedd9-d958-5d7b-96e6-d7153e18d8a0'::uuid, '8c890a17-ae42-5b6c-b2c7-65989d9a4a40'::uuid, '5dfe8f98-be4d-577e-8daf-f241a9ffba09'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('ff26dc5e-e2bd-57a9-8070-e8c7fe44a180'::uuid, '8c890a17-ae42-5b6c-b2c7-65989d9a4a40'::uuid, 'd6368e73-a6a0-5643-956c-9d72fb5e5699'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('7e6cb6c1-6cc4-51e6-9ac6-1b8d567a6f81'::uuid, '8c890a17-ae42-5b6c-b2c7-65989d9a4a40'::uuid, '11702c0d-be0f-5647-8b6f-6a7d9b566f17'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('4837fc4f-9b09-5fb6-bd11-1e0ed92288db'::uuid, '8c890a17-ae42-5b6c-b2c7-65989d9a4a40'::uuid, '27f5996e-df39-59a7-b41b-cdb3925884b4'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('230ba5a5-e432-54cb-b52e-7b318ec7b9a0'::uuid, '8c890a17-ae42-5b6c-b2c7-65989d9a4a40'::uuid, '92cdf9d8-418e-5f8f-b6c8-b890539b9e30'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('51445423-8bec-5758-8fa4-aa4346a7901a'::uuid, '8c890a17-ae42-5b6c-b2c7-65989d9a4a40'::uuid, '8ba78d23-b81e-503b-aa15-da211a77a43d'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('e7803c57-3268-5a20-bc34-c0c105408ae1'::uuid, '8c890a17-ae42-5b6c-b2c7-65989d9a4a40'::uuid, 'c55cd955-6799-552b-868e-87c019f14666'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('9f1ae2b9-5ab7-5356-88ee-422929c48f42'::uuid, '8c890a17-ae42-5b6c-b2c7-65989d9a4a40'::uuid, '47938492-5832-5a2d-9608-ed10f589a32b'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('79f0fd5e-e2ba-5867-a828-ae9f6795d242'::uuid, '8c890a17-ae42-5b6c-b2c7-65989d9a4a40'::uuid, 'b8b323d0-b81d-56bd-ae38-9e379eb65a38'::uuid);

INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('6ce190a8-bee2-5cf9-860f-430203922ff8'::uuid, '362d2715-d339-55dd-841a-0c48021adce1'::uuid, 'c7438c67-dae7-5c11-8083-d8d4caad0d21'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('5f057d54-db26-52d4-97e7-e20d330f5334'::uuid, '362d2715-d339-55dd-841a-0c48021adce1'::uuid, '0cdba34e-cb73-5d65-8a49-5f39efe9de33'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('c443fe60-fe81-5112-8ad4-ce4e8dfb4325'::uuid, '362d2715-d339-55dd-841a-0c48021adce1'::uuid, '86f2223e-2338-51c9-8b3b-fe726f2c524f'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('39936fff-0387-5d11-bd0c-5b9dbdd3feea'::uuid, '362d2715-d339-55dd-841a-0c48021adce1'::uuid, 'd035e6bb-4dad-5d59-9039-211fc8e5de3b'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('d0f237a4-8b6f-563a-80a5-a28a605c011d'::uuid, '362d2715-d339-55dd-841a-0c48021adce1'::uuid, 'ce812b7e-27a2-591d-ab57-6f71ee1e491d'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('b2a314ba-2701-5099-8f32-7302a28e97bc'::uuid, '362d2715-d339-55dd-841a-0c48021adce1'::uuid, '4132868a-d43f-5df9-b972-e8f0f1cf84f3'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('447053d5-c025-5e33-903f-53cb0f190a59'::uuid, '362d2715-d339-55dd-841a-0c48021adce1'::uuid, '4aecda1f-0e91-563f-b2b2-d87a196e5135'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('ac0cfa32-f113-50b7-ab26-16bd968948e4'::uuid, '362d2715-d339-55dd-841a-0c48021adce1'::uuid, 'a61889e1-7fb5-5578-bc5e-280432f327a0'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('86ed6529-fda8-5a19-9e29-6af32a0139af'::uuid, '362d2715-d339-55dd-841a-0c48021adce1'::uuid, 'd3884b5c-e9d6-5b36-a5a7-ed95fa7264df'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('f15c88c3-023f-5f2d-a483-be0e3c1453e6'::uuid, '362d2715-d339-55dd-841a-0c48021adce1'::uuid, '5dfe8f98-be4d-577e-8daf-f241a9ffba09'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('2a6cbfc2-c942-5e69-980b-d8f0ad1c3d38'::uuid, '362d2715-d339-55dd-841a-0c48021adce1'::uuid, 'd6368e73-a6a0-5643-956c-9d72fb5e5699'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('e4966beb-3533-5978-9256-85d6779edc73'::uuid, '362d2715-d339-55dd-841a-0c48021adce1'::uuid, '11702c0d-be0f-5647-8b6f-6a7d9b566f17'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('2fceb7b3-7d9a-5615-b9b1-a4dad837abbd'::uuid, '362d2715-d339-55dd-841a-0c48021adce1'::uuid, '47938492-5832-5a2d-9608-ed10f589a32b'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('407c73a0-a480-5fed-9eca-a34845dc218d'::uuid, '362d2715-d339-55dd-841a-0c48021adce1'::uuid, 'b8b323d0-b81d-56bd-ae38-9e379eb65a38'::uuid);

INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('0dc4e4f1-1d69-5cb2-8a75-f7d6a7e5f929'::uuid, '1414495b-cde6-56b3-8309-a5a61f0be31a'::uuid, '687bca0a-f930-51b6-8edc-64fe90ac7566'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('614c91c5-07aa-5666-8e10-0e82cfc00538'::uuid, '1414495b-cde6-56b3-8309-a5a61f0be31a'::uuid, 'c7438c67-dae7-5c11-8083-d8d4caad0d21'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('9d25a352-8dae-51f8-b396-7f016c1c4750'::uuid, '1414495b-cde6-56b3-8309-a5a61f0be31a'::uuid, 'fe628f26-ca23-5b7e-9154-56c71d8b149c'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('a9cf9e24-013d-54bc-b81f-84baf0029164'::uuid, '1414495b-cde6-56b3-8309-a5a61f0be31a'::uuid, '06120999-c8d2-5e68-a46a-5f64148cc9bc'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('9a22edc5-7ac6-5f9e-8cc0-2b6983b67a0d'::uuid, '1414495b-cde6-56b3-8309-a5a61f0be31a'::uuid, '0cdba34e-cb73-5d65-8a49-5f39efe9de33'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('d66bd0ef-5bfa-5dc7-98e3-1d00cfd94a3c'::uuid, '1414495b-cde6-56b3-8309-a5a61f0be31a'::uuid, '86f2223e-2338-51c9-8b3b-fe726f2c524f'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('6f8f0279-4eb7-5351-bdd6-eb43f22c929f'::uuid, '1414495b-cde6-56b3-8309-a5a61f0be31a'::uuid, '99a3ea10-4b44-592d-9ed3-1dc5048fb6a5'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('4e583e49-5948-5a17-9171-c2df7ab87872'::uuid, '1414495b-cde6-56b3-8309-a5a61f0be31a'::uuid, 'eb9aa768-b91e-5d83-b691-c372e11f7449'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('d261790e-fdb1-5b06-a3fd-5c3625b1a337'::uuid, '1414495b-cde6-56b3-8309-a5a61f0be31a'::uuid, 'd035e6bb-4dad-5d59-9039-211fc8e5de3b'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('87b3b824-cf34-59a3-a7f9-d3bd8d8cab7c'::uuid, '1414495b-cde6-56b3-8309-a5a61f0be31a'::uuid, '4c6339c7-d163-5313-9546-c1193e600fe3'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('97bc7219-f3e3-5198-8055-48a35790aad5'::uuid, '1414495b-cde6-56b3-8309-a5a61f0be31a'::uuid, 'ce812b7e-27a2-591d-ab57-6f71ee1e491d'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('70513c12-6191-5449-83cc-4d02bb17057c'::uuid, '1414495b-cde6-56b3-8309-a5a61f0be31a'::uuid, '456f701a-ecef-5ade-b993-aad79d56dc30'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('81704a94-af5a-5b08-b1c8-e03c21190797'::uuid, '1414495b-cde6-56b3-8309-a5a61f0be31a'::uuid, '4132868a-d43f-5df9-b972-e8f0f1cf84f3'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('8292f13a-4751-5a22-bab0-b25d921d6878'::uuid, '1414495b-cde6-56b3-8309-a5a61f0be31a'::uuid, 'f0380b6a-794e-5143-aff3-f1373cba3cb2'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('719301c5-2b8e-5f32-9777-214c1a83256e'::uuid, '1414495b-cde6-56b3-8309-a5a61f0be31a'::uuid, 'bf8a8420-5fe3-5877-af10-94f69f1db9c8'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('437479da-6a34-5f97-b54b-373afb6b42bd'::uuid, '1414495b-cde6-56b3-8309-a5a61f0be31a'::uuid, '2c540ecd-b255-5327-9f1a-d078b3a11a02'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('06b4c08f-34c8-5d75-9e0e-7bb8ddc4d7fc'::uuid, '1414495b-cde6-56b3-8309-a5a61f0be31a'::uuid, '7ca18c55-7660-548f-ac2a-707898f4557a'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('c4441825-2c53-50ef-92fe-85c4efa32d5d'::uuid, '1414495b-cde6-56b3-8309-a5a61f0be31a'::uuid, '60832409-583b-52a1-bdd6-5f5f9a4e8461'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('a12e2fe3-44d7-582c-bf0f-0a0f2bbae2bc'::uuid, '1414495b-cde6-56b3-8309-a5a61f0be31a'::uuid, '2f81b0fa-17a6-5f54-8fc5-1d718046cfcc'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('188a303d-d260-5e6a-8e60-909cbd9fe1e3'::uuid, '1414495b-cde6-56b3-8309-a5a61f0be31a'::uuid, '3b121a31-d7ea-569e-961e-34408f06f7ed'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('74813062-ab91-5dac-8784-952ea6b57692'::uuid, '1414495b-cde6-56b3-8309-a5a61f0be31a'::uuid, 'e53d9abd-10e4-52ce-a777-8cb5031dd5c1'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('c7da4428-e926-5879-ba5f-e1d413f9ac18'::uuid, '1414495b-cde6-56b3-8309-a5a61f0be31a'::uuid, '3ed4e386-1a37-5616-b883-cbaf52311a06'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('0d411682-d0c3-54a1-a975-6f6c08d8696b'::uuid, '1414495b-cde6-56b3-8309-a5a61f0be31a'::uuid, '4aecda1f-0e91-563f-b2b2-d87a196e5135'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('4dba6b59-4d9a-53ed-a8c1-e8245ca2ebac'::uuid, '1414495b-cde6-56b3-8309-a5a61f0be31a'::uuid, '6222cd9f-6bc6-5f9a-a82a-30e78d3f6b45'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('46b2f918-61e0-5cab-87b2-c0356dedc5fb'::uuid, '1414495b-cde6-56b3-8309-a5a61f0be31a'::uuid, '198a66c9-df74-5b8d-8778-2f09ebd53599'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('39bf476b-41fa-52ce-8862-7c4b4ea2e89c'::uuid, '1414495b-cde6-56b3-8309-a5a61f0be31a'::uuid, 'a61889e1-7fb5-5578-bc5e-280432f327a0'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('1c26b975-1bfa-567c-ae96-c7a21c5572ff'::uuid, '1414495b-cde6-56b3-8309-a5a61f0be31a'::uuid, '1292eb10-8357-586c-9714-679a37ebaa8e'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('f806f6ae-07c8-548d-9e95-beb1071e7876'::uuid, '1414495b-cde6-56b3-8309-a5a61f0be31a'::uuid, 'ceba4928-fe6f-565d-9824-516cb3781ed0'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('d90a2b38-d0a3-5116-81ba-bc5a06f88343'::uuid, '1414495b-cde6-56b3-8309-a5a61f0be31a'::uuid, '7b0fea0a-8542-5821-af82-b1c1b8aafeee'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('c42232d6-81c4-53d1-967e-e1868c17fc27'::uuid, '1414495b-cde6-56b3-8309-a5a61f0be31a'::uuid, 'b8942f13-cf56-526c-b352-84ff3c729dce'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('9589ca4d-5373-5cda-b9c5-ae1a1d6f415e'::uuid, '1414495b-cde6-56b3-8309-a5a61f0be31a'::uuid, 'd3884b5c-e9d6-5b36-a5a7-ed95fa7264df'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('971bceaa-eea2-5c71-96e3-b0d90563f572'::uuid, '1414495b-cde6-56b3-8309-a5a61f0be31a'::uuid, '41de0e66-d2b6-5e3a-894c-33a279d06a02'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('633b68b5-d71c-5113-b80d-ab3410bbee78'::uuid, '1414495b-cde6-56b3-8309-a5a61f0be31a'::uuid, '5dfe8f98-be4d-577e-8daf-f241a9ffba09'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('9e08d7c4-f392-55db-8443-ad1c84688447'::uuid, '1414495b-cde6-56b3-8309-a5a61f0be31a'::uuid, 'd6368e73-a6a0-5643-956c-9d72fb5e5699'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('1ea6cc37-2d57-50b9-9bae-539a04c50177'::uuid, '1414495b-cde6-56b3-8309-a5a61f0be31a'::uuid, '11702c0d-be0f-5647-8b6f-6a7d9b566f17'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('c02c4468-2f3d-5944-8684-d607bdfcde03'::uuid, '1414495b-cde6-56b3-8309-a5a61f0be31a'::uuid, '4c3e90db-5285-50be-a8e6-e114618ec053'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('f483e45d-412b-561d-9f1d-1fdeee1ef3e3'::uuid, '1414495b-cde6-56b3-8309-a5a61f0be31a'::uuid, '27f5996e-df39-59a7-b41b-cdb3925884b4'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('0f3e2489-b1f0-5b38-a7e6-dad3dd3845f5'::uuid, '1414495b-cde6-56b3-8309-a5a61f0be31a'::uuid, '7f25b8e7-7c8e-569c-8c92-ff661affcc23'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('85dcb010-5b7c-578f-8a58-15edd5e52812'::uuid, '1414495b-cde6-56b3-8309-a5a61f0be31a'::uuid, 'b3814be0-0bdd-580c-8437-836e85d92e20'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('ed518766-4fad-5352-b876-bec119d9a56a'::uuid, '1414495b-cde6-56b3-8309-a5a61f0be31a'::uuid, '84768b8c-3306-5adf-8d28-d8646a806805'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('81c2da3a-ca77-5ae8-b739-74279e301265'::uuid, '1414495b-cde6-56b3-8309-a5a61f0be31a'::uuid, 'df1ecc13-657a-5a7b-b012-d22f914597ca'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('dfa55903-ef97-57c8-b1a4-280fa8371710'::uuid, '1414495b-cde6-56b3-8309-a5a61f0be31a'::uuid, '92cdf9d8-418e-5f8f-b6c8-b890539b9e30'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('a9ac5468-54ae-5ed3-ad47-382d475a2827'::uuid, '1414495b-cde6-56b3-8309-a5a61f0be31a'::uuid, '8ba78d23-b81e-503b-aa15-da211a77a43d'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('6b4beeca-d86a-55d2-80b6-5489d1a2eec6'::uuid, '1414495b-cde6-56b3-8309-a5a61f0be31a'::uuid, 'f4828640-92ea-548f-9f93-854541f1e94d'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('4ff29d90-815e-50a3-b844-730986859c5c'::uuid, '1414495b-cde6-56b3-8309-a5a61f0be31a'::uuid, 'c55cd955-6799-552b-868e-87c019f14666'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('6453965d-1934-5e98-9086-aeb72d26a403'::uuid, '1414495b-cde6-56b3-8309-a5a61f0be31a'::uuid, '47938492-5832-5a2d-9608-ed10f589a32b'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('8eaa0ceb-fc83-5563-b447-7181273128b1'::uuid, '1414495b-cde6-56b3-8309-a5a61f0be31a'::uuid, 'b8b323d0-b81d-56bd-ae38-9e379eb65a38'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('949dd844-381c-55f6-b2d1-39d23125cc4c'::uuid, '1414495b-cde6-56b3-8309-a5a61f0be31a'::uuid, 'c63908c7-1b74-5556-bc44-f0a0c1f9d937'::uuid);

INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('c8920574-4073-5f18-9c67-b44a2a4fc59b'::uuid, 'aaee7924-46ce-5be8-a021-e54b29ebf2fb'::uuid, '687bca0a-f930-51b6-8edc-64fe90ac7566'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('8bb4136c-7000-5be1-a029-b253e16d6f2d'::uuid, 'aaee7924-46ce-5be8-a021-e54b29ebf2fb'::uuid, 'c7438c67-dae7-5c11-8083-d8d4caad0d21'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('0916d10f-1fd4-5612-a3cb-84c1dfb4204d'::uuid, 'aaee7924-46ce-5be8-a021-e54b29ebf2fb'::uuid, 'fe628f26-ca23-5b7e-9154-56c71d8b149c'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('27569b43-7ac5-5dc3-a6d9-cb21b557d762'::uuid, 'aaee7924-46ce-5be8-a021-e54b29ebf2fb'::uuid, '06120999-c8d2-5e68-a46a-5f64148cc9bc'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('24a0fd09-1535-50d1-a8af-a61ebb652a2a'::uuid, 'aaee7924-46ce-5be8-a021-e54b29ebf2fb'::uuid, '0cdba34e-cb73-5d65-8a49-5f39efe9de33'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('13db58c2-51e7-5c74-81db-7fb068959ea8'::uuid, 'aaee7924-46ce-5be8-a021-e54b29ebf2fb'::uuid, '86f2223e-2338-51c9-8b3b-fe726f2c524f'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('fd46c72c-b0a2-5df9-8e2b-3e9e9f9b630c'::uuid, 'aaee7924-46ce-5be8-a021-e54b29ebf2fb'::uuid, '99a3ea10-4b44-592d-9ed3-1dc5048fb6a5'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('bcaeeb02-3b0b-5d39-8169-3ad56f06e93e'::uuid, 'aaee7924-46ce-5be8-a021-e54b29ebf2fb'::uuid, 'eb9aa768-b91e-5d83-b691-c372e11f7449'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('8155171b-ebc9-5d46-9214-dd822a7ddaa3'::uuid, 'aaee7924-46ce-5be8-a021-e54b29ebf2fb'::uuid, 'd035e6bb-4dad-5d59-9039-211fc8e5de3b'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('b0dcbbcf-6424-5cc9-aec6-00ba53b85a54'::uuid, 'aaee7924-46ce-5be8-a021-e54b29ebf2fb'::uuid, '4c6339c7-d163-5313-9546-c1193e600fe3'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('ceac65fd-d827-5563-a821-ff56fa69c26d'::uuid, 'aaee7924-46ce-5be8-a021-e54b29ebf2fb'::uuid, 'ce812b7e-27a2-591d-ab57-6f71ee1e491d'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('1a5e23e1-4ef9-523f-b3dd-701f24dc99bd'::uuid, 'aaee7924-46ce-5be8-a021-e54b29ebf2fb'::uuid, '456f701a-ecef-5ade-b993-aad79d56dc30'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('7de0ecfd-73e2-54b9-9e3c-3e6721efad52'::uuid, 'aaee7924-46ce-5be8-a021-e54b29ebf2fb'::uuid, '4132868a-d43f-5df9-b972-e8f0f1cf84f3'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('326ea809-f82b-5abb-8e4a-d2b71c7db05d'::uuid, 'aaee7924-46ce-5be8-a021-e54b29ebf2fb'::uuid, 'f0380b6a-794e-5143-aff3-f1373cba3cb2'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('2bc873c4-c341-5994-a450-2ee17af64381'::uuid, 'aaee7924-46ce-5be8-a021-e54b29ebf2fb'::uuid, 'bf8a8420-5fe3-5877-af10-94f69f1db9c8'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('22a2fdf3-7a2f-5c36-a64f-77e07b2d180f'::uuid, 'aaee7924-46ce-5be8-a021-e54b29ebf2fb'::uuid, '2c540ecd-b255-5327-9f1a-d078b3a11a02'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('7955da07-8b14-5d10-ad17-62ff9fef9c67'::uuid, 'aaee7924-46ce-5be8-a021-e54b29ebf2fb'::uuid, '7ca18c55-7660-548f-ac2a-707898f4557a'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('9f289b58-d3c9-5a8c-a869-a7f16f26f817'::uuid, 'aaee7924-46ce-5be8-a021-e54b29ebf2fb'::uuid, '60832409-583b-52a1-bdd6-5f5f9a4e8461'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('7e90ec20-65c2-543d-8d88-522d79fcc549'::uuid, 'aaee7924-46ce-5be8-a021-e54b29ebf2fb'::uuid, '2f81b0fa-17a6-5f54-8fc5-1d718046cfcc'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('04732114-983a-5d84-b514-eea2e0657344'::uuid, 'aaee7924-46ce-5be8-a021-e54b29ebf2fb'::uuid, '3b121a31-d7ea-569e-961e-34408f06f7ed'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('75e4e55b-a1ca-53f7-b479-c5675f10c627'::uuid, 'aaee7924-46ce-5be8-a021-e54b29ebf2fb'::uuid, 'e53d9abd-10e4-52ce-a777-8cb5031dd5c1'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('92dfbe67-4af5-5981-b31e-49b5a3500421'::uuid, 'aaee7924-46ce-5be8-a021-e54b29ebf2fb'::uuid, '3ed4e386-1a37-5616-b883-cbaf52311a06'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('d7745889-881a-52b9-9a60-81cd1fa16727'::uuid, 'aaee7924-46ce-5be8-a021-e54b29ebf2fb'::uuid, '4aecda1f-0e91-563f-b2b2-d87a196e5135'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('5afcdc94-e9bd-5f9c-9258-50d3f49d13b4'::uuid, 'aaee7924-46ce-5be8-a021-e54b29ebf2fb'::uuid, '6222cd9f-6bc6-5f9a-a82a-30e78d3f6b45'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('ae8c97d9-4ed5-5374-a79f-065192882171'::uuid, 'aaee7924-46ce-5be8-a021-e54b29ebf2fb'::uuid, '198a66c9-df74-5b8d-8778-2f09ebd53599'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('f54369ed-7f02-5d2e-9529-a6c6d17f489f'::uuid, 'aaee7924-46ce-5be8-a021-e54b29ebf2fb'::uuid, 'a61889e1-7fb5-5578-bc5e-280432f327a0'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('338f95f4-ce1b-5dde-899e-6a033300ccb6'::uuid, 'aaee7924-46ce-5be8-a021-e54b29ebf2fb'::uuid, '1292eb10-8357-586c-9714-679a37ebaa8e'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('d40950e7-9892-5c39-82f8-4a15cd39c20c'::uuid, 'aaee7924-46ce-5be8-a021-e54b29ebf2fb'::uuid, 'ceba4928-fe6f-565d-9824-516cb3781ed0'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('40954763-d7ba-516a-abb0-e25f311f6dce'::uuid, 'aaee7924-46ce-5be8-a021-e54b29ebf2fb'::uuid, '7b0fea0a-8542-5821-af82-b1c1b8aafeee'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('54d1b52f-5f5b-56da-b2a0-00088bc28a25'::uuid, 'aaee7924-46ce-5be8-a021-e54b29ebf2fb'::uuid, 'b8942f13-cf56-526c-b352-84ff3c729dce'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('fb0de18b-5f79-5a57-a07e-8b19619d19b1'::uuid, 'aaee7924-46ce-5be8-a021-e54b29ebf2fb'::uuid, 'd3884b5c-e9d6-5b36-a5a7-ed95fa7264df'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('b0b42ebf-57b4-5d43-b9bb-b6c2ecdf93fa'::uuid, 'aaee7924-46ce-5be8-a021-e54b29ebf2fb'::uuid, '5dfe8f98-be4d-577e-8daf-f241a9ffba09'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('39fe9cbf-4679-5559-8f1f-dfa33d9d563e'::uuid, 'aaee7924-46ce-5be8-a021-e54b29ebf2fb'::uuid, 'd6368e73-a6a0-5643-956c-9d72fb5e5699'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('555c5dbf-f04b-537d-9d84-d689484dc4cb'::uuid, 'aaee7924-46ce-5be8-a021-e54b29ebf2fb'::uuid, '11702c0d-be0f-5647-8b6f-6a7d9b566f17'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('457b3dfd-a2f5-5149-a676-a1bbce99965b'::uuid, 'aaee7924-46ce-5be8-a021-e54b29ebf2fb'::uuid, '4c3e90db-5285-50be-a8e6-e114618ec053'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('13042a20-2297-52e2-afdf-702f5c7c2045'::uuid, 'aaee7924-46ce-5be8-a021-e54b29ebf2fb'::uuid, '27f5996e-df39-59a7-b41b-cdb3925884b4'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('427d626d-d071-5c12-994f-2890eb4b533a'::uuid, 'aaee7924-46ce-5be8-a021-e54b29ebf2fb'::uuid, '7f25b8e7-7c8e-569c-8c92-ff661affcc23'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('ae6e24df-9f0b-52a6-ade9-b5b1bdb2b621'::uuid, 'aaee7924-46ce-5be8-a021-e54b29ebf2fb'::uuid, 'b3814be0-0bdd-580c-8437-836e85d92e20'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('065af47e-ff70-5dde-a623-d06519f544af'::uuid, 'aaee7924-46ce-5be8-a021-e54b29ebf2fb'::uuid, '84768b8c-3306-5adf-8d28-d8646a806805'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('884a7b1c-d78a-5eae-91eb-d33308950c5d'::uuid, 'aaee7924-46ce-5be8-a021-e54b29ebf2fb'::uuid, 'df1ecc13-657a-5a7b-b012-d22f914597ca'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('dc17d051-92d1-56b3-9b76-84cf2e316402'::uuid, 'aaee7924-46ce-5be8-a021-e54b29ebf2fb'::uuid, '92cdf9d8-418e-5f8f-b6c8-b890539b9e30'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('94649e1d-0cec-56d3-a84a-6e09f68f9c69'::uuid, 'aaee7924-46ce-5be8-a021-e54b29ebf2fb'::uuid, '8ba78d23-b81e-503b-aa15-da211a77a43d'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('918a3e27-735e-5458-b0b3-6486c91dcbe7'::uuid, 'aaee7924-46ce-5be8-a021-e54b29ebf2fb'::uuid, 'f4828640-92ea-548f-9f93-854541f1e94d'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('dd96eaa3-1d5f-5d21-a952-33304707b0b0'::uuid, 'aaee7924-46ce-5be8-a021-e54b29ebf2fb'::uuid, 'c55cd955-6799-552b-868e-87c019f14666'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('7d338380-2b7e-5e07-abda-f9e7aa85e553'::uuid, 'aaee7924-46ce-5be8-a021-e54b29ebf2fb'::uuid, '47938492-5832-5a2d-9608-ed10f589a32b'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('660ab570-1297-5200-bb8b-93a34e74d398'::uuid, 'aaee7924-46ce-5be8-a021-e54b29ebf2fb'::uuid, 'b8b323d0-b81d-56bd-ae38-9e379eb65a38'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('0f0d4fde-f596-5318-866a-33d39c318834'::uuid, 'aaee7924-46ce-5be8-a021-e54b29ebf2fb'::uuid, 'c63908c7-1b74-5556-bc44-f0a0c1f9d937'::uuid);

INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('8d3840bd-e976-53bb-937e-60213748891f'::uuid, '79a3fc7a-c037-58d2-bc91-3de816cff4fc'::uuid, '687bca0a-f930-51b6-8edc-64fe90ac7566'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('c9bf6166-a4c7-5dbd-97e1-e4d272f3c038'::uuid, '79a3fc7a-c037-58d2-bc91-3de816cff4fc'::uuid, 'c7438c67-dae7-5c11-8083-d8d4caad0d21'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('793cce83-3baf-528e-b8fa-1e670fb56f92'::uuid, '79a3fc7a-c037-58d2-bc91-3de816cff4fc'::uuid, 'fe628f26-ca23-5b7e-9154-56c71d8b149c'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('11f8e370-4543-5af5-851a-f5ccca660f32'::uuid, '79a3fc7a-c037-58d2-bc91-3de816cff4fc'::uuid, '99a3ea10-4b44-592d-9ed3-1dc5048fb6a5'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('789079aa-e57d-5434-80cb-61bdaa22d752'::uuid, '79a3fc7a-c037-58d2-bc91-3de816cff4fc'::uuid, '4c6339c7-d163-5313-9546-c1193e600fe3'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('006e9a3f-1c9b-50f2-8584-5f41c55ce541'::uuid, '79a3fc7a-c037-58d2-bc91-3de816cff4fc'::uuid, '456f701a-ecef-5ade-b993-aad79d56dc30'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('c097067e-4d93-5fd9-b132-78d7ff29a9fd'::uuid, '79a3fc7a-c037-58d2-bc91-3de816cff4fc'::uuid, '4132868a-d43f-5df9-b972-e8f0f1cf84f3'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('461a0506-b56f-5b13-b009-53f1f0b212b1'::uuid, '79a3fc7a-c037-58d2-bc91-3de816cff4fc'::uuid, 'bf8a8420-5fe3-5877-af10-94f69f1db9c8'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('781c629f-6c87-5b94-b395-057accdde30b'::uuid, '79a3fc7a-c037-58d2-bc91-3de816cff4fc'::uuid, '7ca18c55-7660-548f-ac2a-707898f4557a'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('c50afdef-c60b-5464-acc6-ea03915a8d40'::uuid, '79a3fc7a-c037-58d2-bc91-3de816cff4fc'::uuid, '60832409-583b-52a1-bdd6-5f5f9a4e8461'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('850f045a-fea6-55c8-8417-00fa64e822b0'::uuid, '79a3fc7a-c037-58d2-bc91-3de816cff4fc'::uuid, '4aecda1f-0e91-563f-b2b2-d87a196e5135'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('ee2bde7f-ba6f-5ea1-88e8-1d29e1e6e1e8'::uuid, '79a3fc7a-c037-58d2-bc91-3de816cff4fc'::uuid, '6222cd9f-6bc6-5f9a-a82a-30e78d3f6b45'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('d60bfd4b-c610-5254-875d-c27bca7f2219'::uuid, '79a3fc7a-c037-58d2-bc91-3de816cff4fc'::uuid, '198a66c9-df74-5b8d-8778-2f09ebd53599'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('b3027445-1271-5a72-a05f-62b40589107b'::uuid, '79a3fc7a-c037-58d2-bc91-3de816cff4fc'::uuid, 'a61889e1-7fb5-5578-bc5e-280432f327a0'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('fcc4f44b-6888-50e3-adf6-cb36e10bc4ff'::uuid, '79a3fc7a-c037-58d2-bc91-3de816cff4fc'::uuid, 'd6368e73-a6a0-5643-956c-9d72fb5e5699'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('958c0a6c-226c-5921-b4ad-d44bada812bd'::uuid, '79a3fc7a-c037-58d2-bc91-3de816cff4fc'::uuid, '7f25b8e7-7c8e-569c-8c92-ff661affcc23'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('e77404a9-d407-5b11-8240-6a3001d2236f'::uuid, '79a3fc7a-c037-58d2-bc91-3de816cff4fc'::uuid, 'b3814be0-0bdd-580c-8437-836e85d92e20'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('51f118a3-b8a1-540f-a288-d2610bc80750'::uuid, '79a3fc7a-c037-58d2-bc91-3de816cff4fc'::uuid, '84768b8c-3306-5adf-8d28-d8646a806805'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('14fc293f-cdf7-5250-832f-90a2571257ee'::uuid, '79a3fc7a-c037-58d2-bc91-3de816cff4fc'::uuid, 'f4828640-92ea-548f-9f93-854541f1e94d'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('54d5cc6f-3cd7-53e3-a439-155495484e06'::uuid, '79a3fc7a-c037-58d2-bc91-3de816cff4fc'::uuid, 'c55cd955-6799-552b-868e-87c019f14666'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('766120f3-5223-506b-ad5c-e8feae087103'::uuid, '79a3fc7a-c037-58d2-bc91-3de816cff4fc'::uuid, '47938492-5832-5a2d-9608-ed10f589a32b'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('9c344c7a-f7b1-5115-871f-fc1daf462358'::uuid, '79a3fc7a-c037-58d2-bc91-3de816cff4fc'::uuid, 'c63908c7-1b74-5556-bc44-f0a0c1f9d937'::uuid);

INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('19dd5f54-a794-5cb0-944e-b1a25c65dba6'::uuid, '12602da7-a8b5-534e-99be-b55b97232c40'::uuid, 'c7438c67-dae7-5c11-8083-d8d4caad0d21'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('f13299b4-aaa9-5007-9cc5-3f109cf1111d'::uuid, '12602da7-a8b5-534e-99be-b55b97232c40'::uuid, '4132868a-d43f-5df9-b972-e8f0f1cf84f3'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('0aad47b1-c7b7-58ce-8986-a423f4dc632d'::uuid, '12602da7-a8b5-534e-99be-b55b97232c40'::uuid, '60832409-583b-52a1-bdd6-5f5f9a4e8461'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('43580da9-689b-55a6-87fb-f3249462d669'::uuid, '12602da7-a8b5-534e-99be-b55b97232c40'::uuid, '4aecda1f-0e91-563f-b2b2-d87a196e5135'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('8acb2d8d-ab0a-5988-b8e0-1aa77ff89ad2'::uuid, '12602da7-a8b5-534e-99be-b55b97232c40'::uuid, 'a61889e1-7fb5-5578-bc5e-280432f327a0'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('61d1c09b-d295-5723-8749-4f4ff8b28181'::uuid, '12602da7-a8b5-534e-99be-b55b97232c40'::uuid, 'd6368e73-a6a0-5643-956c-9d72fb5e5699'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('18338181-7c13-52c0-9096-22820be22683'::uuid, '12602da7-a8b5-534e-99be-b55b97232c40'::uuid, '47938492-5832-5a2d-9608-ed10f589a32b'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('b395fdb0-7d18-5245-87cf-b79cf27a389f'::uuid, '12602da7-a8b5-534e-99be-b55b97232c40'::uuid, 'b8b323d0-b81d-56bd-ae38-9e379eb65a38'::uuid);

INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('edab3cd0-aca0-5089-88fd-9d0827f59ada'::uuid, 'dd3af806-b286-5cd7-836d-364934684782'::uuid, 'c7438c67-dae7-5c11-8083-d8d4caad0d21'::uuid);
INSERT INTO governance.role_permission (role_permission_id, role_id, permission_id)
VALUES ('c197f64f-5729-5d6e-8bee-054ae87f98a0'::uuid, 'dd3af806-b286-5cd7-836d-364934684782'::uuid, 'a61889e1-7fb5-5578-bc5e-280432f327a0'::uuid);

COMMIT;
