BEGIN;

-- Deterministic architecture-owned taxonomy seeds. UUIDs are stable across environments.
INSERT INTO core.moment_category (moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('eead428c-adf0-5884-8ea9-636cba956d69'::uuid, 'PERSONAL', 'LIFE_OPERATIONS', 'Life Operations', 'Recovery, mood, rhythm and wellbeing moment context.', 10, 'ACTIVE');

INSERT INTO core.moment_category (moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('74d91b75-8133-569d-b15f-42469bb8fb8c'::uuid, 'PERSONAL', 'FUTURE_BUILDING', 'Future Building', 'Goals, milestones, opportunities, pivots and future learning.', 20, 'ACTIVE');

INSERT INTO core.moment_category (moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('a4b0a5de-b986-5697-972d-93cb7ff76a07'::uuid, 'PERSONAL', 'LIFESTYLE', 'Lifestyle', 'Experience, wellbeing, discovery, creation and lifestyle context.', 30, 'ACTIVE');

INSERT INTO core.moment_category (moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('c871346c-663b-5844-bb26-9bf763b8d942'::uuid, 'PERSONAL', 'RELATIONSHIPS', 'Relationships', 'Connections, interactions, support, shared experience and relationship investment.', 40, 'ACTIVE');

INSERT INTO core.moment_category (moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('ade628af-bdcc-577c-bc78-9ec11fbe3349'::uuid, 'GROUP', 'SHARED_EXPERIENCE', 'Shared Experience', 'Trips, weddings, parties, outings and other shared experiences.', 10, 'ACTIVE');

INSERT INTO core.moment_category (moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('d600d317-d270-5296-b69f-351ba3352d65'::uuid, 'GROUP', 'SHARED_PURCHASE', 'Shared Purchase', 'Gift pools, group purchases, shared assets and family/community purchases.', 20, 'ACTIVE');

INSERT INTO core.moment_category (moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('e0858266-388e-5dc0-b684-4b933cf61500'::uuid, 'GROUP', 'SHARED_LIVING', 'Shared Living', 'Flatmates, family household, co-living, shared rental and community living.', 30, 'ACTIVE');

INSERT INTO core.moment_category (moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('f4d25464-dbf4-5aca-904c-202697b7e02f'::uuid, 'GROUP', 'SHARED_GOAL', 'Shared Goal', 'Collaborative goal-oriented moments.', 40, 'ACTIVE');

INSERT INTO core.moment_category (moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('14c5fa17-f6fa-5ec7-9273-16cf6a25ceef'::uuid, 'GROUP', 'COMMUNITY_COORDINATION', 'Community Coordination', 'Community coordination moments.', 50, 'ACTIVE');

INSERT INTO core.moment_category (moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('f2799012-effe-5683-9568-a5d107431e45'::uuid, 'BUSINESS', 'TEAM_OPERATIONS', 'Team Operations', 'Team execution and operating moments.', 10, 'ACTIVE');

INSERT INTO core.moment_category (moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('02fe7b48-994e-5bf4-8941-6aeba03c6132'::uuid, 'BUSINESS', 'BUSINESS_RUNWAY', 'Business Runway', 'Business runway and financial planning moments.', 20, 'ACTIVE');

INSERT INTO core.moment_category (moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('ecf054e2-fd5e-5e08-85ad-2d841268a24a'::uuid, 'BUSINESS', 'BUSINESS_OPERATIONS', 'Business Operations', 'Business operating moments.', 30, 'ACTIVE');

INSERT INTO core.moment_category (moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('64044c78-e7ac-518e-a755-abc3d07ec0e6'::uuid, 'BUSINESS', 'EVENTS_OPERATIONS', 'Events Operations', 'Business event operating moments.', 40, 'ACTIVE');

INSERT INTO core.moment_category (moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('b61d5438-6a92-5780-ba2d-0bfe151346ab'::uuid, 'BUSINESS', 'VENDOR_OPERATIONS', 'Vendor Operations', 'Vendor and SLA operating moments.', 50, 'ACTIVE');

INSERT INTO core.moment_type (moment_type_id, moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('891ad9b7-4246-52ad-9d48-aacc991d4caa'::uuid, 'eead428c-adf0-5884-8ea9-636cba956d69'::uuid, 'PERSONAL', 'LIFE_RECOVERY', 'Recovery', 'Recovery-focused life operations moment.', 10, 'ACTIVE');

INSERT INTO core.moment_type (moment_type_id, moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('040dd2a3-3429-542b-8560-1141f55cacd6'::uuid, 'eead428c-adf0-5884-8ea9-636cba956d69'::uuid, 'PERSONAL', 'LIFE_MOOD', 'Mood', 'Mood-focused life operations moment.', 20, 'ACTIVE');

INSERT INTO core.moment_type (moment_type_id, moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('23974b6a-55f8-5b81-af53-aa0766b29e6a'::uuid, 'eead428c-adf0-5884-8ea9-636cba956d69'::uuid, 'PERSONAL', 'LIFE_RHYTHM', 'Rhythm', 'Rhythm / discipline-focused life operations moment.', 30, 'ACTIVE');

INSERT INTO core.moment_type (moment_type_id, moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('0e2cfd13-fcd4-5bd4-8215-6164aae0a330'::uuid, 'eead428c-adf0-5884-8ea9-636cba956d69'::uuid, 'PERSONAL', 'LIFE_WELLBEING', 'Wellbeing', 'Wellbeing-focused life operations moment.', 40, 'ACTIVE');

INSERT INTO core.moment_type (moment_type_id, moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('5015fd32-d116-55c6-9fe5-5e306f7f8988'::uuid, '74d91b75-8133-569d-b15f-42469bb8fb8c'::uuid, 'PERSONAL', 'FUTURE_GOAL', 'Goal', 'Goal-focused future building moment.', 10, 'ACTIVE');

INSERT INTO core.moment_type (moment_type_id, moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('43654aea-824d-55df-aa8c-32a88b8a54af'::uuid, '74d91b75-8133-569d-b15f-42469bb8fb8c'::uuid, 'PERSONAL', 'FUTURE_MILESTONE', 'Milestone', 'Milestone-focused future building moment.', 20, 'ACTIVE');

INSERT INTO core.moment_type (moment_type_id, moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('037485d1-e163-5e4f-8c70-6271eb32cf5d'::uuid, '74d91b75-8133-569d-b15f-42469bb8fb8c'::uuid, 'PERSONAL', 'FUTURE_PROGRESS', 'Progress', 'Progress-focused future building moment.', 30, 'ACTIVE');

INSERT INTO core.moment_type (moment_type_id, moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('a9213c15-1045-55ba-9332-1765479a66a9'::uuid, '74d91b75-8133-569d-b15f-42469bb8fb8c'::uuid, 'PERSONAL', 'FUTURE_OPPORTUNITY', 'Opportunity', 'Opportunity-focused future building moment.', 40, 'ACTIVE');

INSERT INTO core.moment_type (moment_type_id, moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('e8d4b0b5-d5cb-5136-bb01-e039941faf87'::uuid, '74d91b75-8133-569d-b15f-42469bb8fb8c'::uuid, 'PERSONAL', 'FUTURE_PIVOT', 'Pivot', 'Pivot-focused future building moment.', 50, 'ACTIVE');

INSERT INTO core.moment_type (moment_type_id, moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('90cde495-1c97-5ded-871c-ad94e06ec7b7'::uuid, '74d91b75-8133-569d-b15f-42469bb8fb8c'::uuid, 'PERSONAL', 'FUTURE_LEARNING_ACTIVITY', 'Future Learning Activity', 'Future learning-focused moment.', 60, 'ACTIVE');

INSERT INTO core.moment_type (moment_type_id, moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('615158c2-7cc5-5479-85ff-19b9c62f0eb5'::uuid, 'a4b0a5de-b986-5697-972d-93cb7ff76a07'::uuid, 'PERSONAL', 'LIFESTYLE_EXPERIENCE', 'Experience', 'Lifestyle experience context.', 10, 'ACTIVE');

INSERT INTO core.moment_type (moment_type_id, moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('40114d71-353e-5e0f-9c39-0d048b573a5d'::uuid, 'a4b0a5de-b986-5697-972d-93cb7ff76a07'::uuid, 'PERSONAL', 'LIFESTYLE_WELLBEING', 'Wellbeing', 'Lifestyle wellbeing context.', 20, 'ACTIVE');

INSERT INTO core.moment_type (moment_type_id, moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('15281777-5f46-55db-afec-3955862e8552'::uuid, 'a4b0a5de-b986-5697-972d-93cb7ff76a07'::uuid, 'PERSONAL', 'LIFESTYLE_DISCOVERY', 'Discovery', 'Lifestyle discovery context.', 30, 'ACTIVE');

INSERT INTO core.moment_type (moment_type_id, moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('bcb2038c-6d0a-50e1-a4a0-d2a95f678dcd'::uuid, 'a4b0a5de-b986-5697-972d-93cb7ff76a07'::uuid, 'PERSONAL', 'LIFESTYLE_CREATION', 'Creation', 'Lifestyle creation context.', 40, 'ACTIVE');

INSERT INTO core.moment_type (moment_type_id, moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('a22f9a2c-5dea-5540-8e83-4b82c961ccb6'::uuid, 'a4b0a5de-b986-5697-972d-93cb7ff76a07'::uuid, 'PERSONAL', 'LIFESTYLE', 'Lifestyle', 'General lifestyle context.', 50, 'ACTIVE');

INSERT INTO core.moment_type (moment_type_id, moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('eda7e441-ca09-5c23-8627-3b8f1b000c83'::uuid, 'c871346c-663b-5844-bb26-9bf763b8d942'::uuid, 'PERSONAL', 'RELATIONSHIP_CONNECTION', 'Connection', 'Relationship connection context.', 10, 'ACTIVE');

INSERT INTO core.moment_type (moment_type_id, moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('f4a59336-ea8a-5fc2-a92f-9645705a32e5'::uuid, 'c871346c-663b-5844-bb26-9bf763b8d942'::uuid, 'PERSONAL', 'RELATIONSHIP_INTERACTION', 'Interaction / Activity', 'Relationship interaction and activity context.', 20, 'ACTIVE');

INSERT INTO core.moment_type (moment_type_id, moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('c88d0ac3-653d-5d0b-be36-7c890ab69828'::uuid, 'c871346c-663b-5844-bb26-9bf763b8d942'::uuid, 'PERSONAL', 'RELATIONSHIP_SUPPORT', 'Support', 'Relationship support context.', 30, 'ACTIVE');

INSERT INTO core.moment_type (moment_type_id, moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('3d13d87b-639f-5727-81d3-238a2360449b'::uuid, 'c871346c-663b-5844-bb26-9bf763b8d942'::uuid, 'PERSONAL', 'RELATIONSHIP_SHARED_EXPERIENCE', 'Shared Experience', 'Relationship shared experience context.', 40, 'ACTIVE');

INSERT INTO core.moment_type (moment_type_id, moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('bccc92a8-652f-5511-8443-c378f46a0b7c'::uuid, 'c871346c-663b-5844-bb26-9bf763b8d942'::uuid, 'PERSONAL', 'RELATIONSHIP_INVESTMENT', 'Relationship Investment', 'Relationship investment context.', 50, 'ACTIVE');

INSERT INTO core.moment_type (moment_type_id, moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('3d7196b8-16c4-5551-bcd4-41384ca0d270'::uuid, 'ade628af-bdcc-577c-bc78-9ec11fbe3349'::uuid, 'GROUP', 'TRIP', 'Trip', 'Shared trip.', 10, 'ACTIVE');

INSERT INTO core.moment_type (moment_type_id, moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('25eedb14-22ca-5640-a35a-35ce24c28650'::uuid, 'ade628af-bdcc-577c-bc78-9ec11fbe3349'::uuid, 'GROUP', 'WEDDING', 'Wedding', 'Shared wedding coordination.', 20, 'ACTIVE');

INSERT INTO core.moment_type (moment_type_id, moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('b449a18f-812b-5666-b798-9171af540803'::uuid, 'ade628af-bdcc-577c-bc78-9ec11fbe3349'::uuid, 'GROUP', 'HOUSE_PARTY', 'House Party', 'Shared house-party coordination.', 30, 'ACTIVE');

INSERT INTO core.moment_type (moment_type_id, moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('8a171369-b9d0-5f65-b65d-f971df71f70f'::uuid, 'ade628af-bdcc-577c-bc78-9ec11fbe3349'::uuid, 'GROUP', 'OFFICE_OUTING', 'Office Outing', 'Shared office outing.', 40, 'ACTIVE');

INSERT INTO core.moment_type (moment_type_id, moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('1e8b9f3d-919c-5757-a972-18a97baab551'::uuid, 'd600d317-d270-5296-b69f-351ba3352d65'::uuid, 'GROUP', 'GIFT_POOL', 'Gift Pool', 'Shared gift contribution/purchase.', 10, 'ACTIVE');

INSERT INTO core.moment_type (moment_type_id, moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('cdc10028-95c8-5f8c-88e6-8bfdfd07fdea'::uuid, 'd600d317-d270-5296-b69f-351ba3352d65'::uuid, 'GROUP', 'GROUP_PURCHASE', 'Group Purchase', 'Shared group purchase.', 20, 'ACTIVE');

INSERT INTO core.moment_type (moment_type_id, moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('54645d3e-d51a-5c35-9e9c-8d4d3ff20ba9'::uuid, 'd600d317-d270-5296-b69f-351ba3352d65'::uuid, 'GROUP', 'SHARED_ASSET', 'Shared Asset', 'Shared asset purchase/ownership.', 30, 'ACTIVE');

INSERT INTO core.moment_type (moment_type_id, moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('2e4e902c-f4ae-5291-9c0b-1a433b2ab9ca'::uuid, 'd600d317-d270-5296-b69f-351ba3352d65'::uuid, 'GROUP', 'FAMILY_PURCHASE', 'Family Purchase', 'Family shared purchase.', 40, 'ACTIVE');

INSERT INTO core.moment_type (moment_type_id, moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('1ca5fe7f-85f8-5b2a-8a79-c6146d246fa7'::uuid, 'd600d317-d270-5296-b69f-351ba3352d65'::uuid, 'GROUP', 'COMMUNITY_PURCHASE', 'Community Purchase', 'Community shared purchase.', 50, 'ACTIVE');

INSERT INTO core.moment_type (moment_type_id, moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('c14d33b8-89da-5c12-bca2-af6acb1af9da'::uuid, 'e0858266-388e-5dc0-b684-4b933cf61500'::uuid, 'GROUP', 'FLATMATES', 'Flatmates', 'Flatmate shared living.', 10, 'ACTIVE');

INSERT INTO core.moment_type (moment_type_id, moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('75110a34-2afc-5449-b95d-2653395a5b58'::uuid, 'e0858266-388e-5dc0-b684-4b933cf61500'::uuid, 'GROUP', 'FAMILY_HOUSEHOLD', 'Family Household', 'Family household shared living.', 20, 'ACTIVE');

INSERT INTO core.moment_type (moment_type_id, moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('7dab3222-08c4-5ba2-aaae-fcbd63120eb4'::uuid, 'e0858266-388e-5dc0-b684-4b933cf61500'::uuid, 'GROUP', 'CO_LIVING', 'Co-Living', 'Co-living arrangement.', 30, 'ACTIVE');

INSERT INTO core.moment_type (moment_type_id, moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('a8f3c2e1-4b5d-5a9c-8e7f-1d2c3b4a5968'::uuid, 'e0858266-388e-5dc0-b684-4b933cf61500'::uuid, 'GROUP', 'SHARED_LIVING', 'Shared Living', 'Shared living arrangement (frozen taxonomy).', 35, 'ACTIVE');

INSERT INTO core.moment_type (moment_type_id, moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('1fd274a8-1909-5fec-b15f-cafbbde76e88'::uuid, 'e0858266-388e-5dc0-b684-4b933cf61500'::uuid, 'GROUP', 'SHARED_RENTAL', 'Shared Rental', 'Shared rental arrangement.', 40, 'ACTIVE');

INSERT INTO core.moment_type (moment_type_id, moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('6366a9c7-7cd8-5b8b-b369-70ecea46dc76'::uuid, 'e0858266-388e-5dc0-b684-4b933cf61500'::uuid, 'GROUP', 'COMMUNITY_LIVING', 'Community Living', 'Community living arrangement.', 50, 'ACTIVE');

INSERT INTO core.moment_type (moment_type_id, moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('5bda60ff-8beb-595a-bf3f-cc634b80c6ab'::uuid, 'f4d25464-dbf4-5aca-904c-202697b7e02f'::uuid, 'GROUP', 'SHARED_GOAL', 'Shared Goal', 'Collaborative shared goal.', 10, 'ACTIVE');

INSERT INTO core.moment_type (moment_type_id, moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('1e8fdd3c-01b8-5683-8a10-d0424f61bb7f'::uuid, '14c5fa17-f6fa-5ec7-9273-16cf6a25ceef'::uuid, 'GROUP', 'COMMUNITY_COORDINATION', 'Community Coordination', 'Community coordination moment.', 10, 'ACTIVE');

INSERT INTO core.moment_type (moment_type_id, moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('922826a8-8980-5899-9371-8b79745affa1'::uuid, 'f2799012-effe-5683-9568-a5d107431e45'::uuid, 'BUSINESS', 'TEAM_OPERATIONS', 'Team Operations', 'Team operations business moment.', 10, 'ACTIVE');

INSERT INTO core.moment_type (moment_type_id, moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('b5791934-6206-5532-9d66-bb24d951afd3'::uuid, '02fe7b48-994e-5bf4-8941-6aeba03c6132'::uuid, 'BUSINESS', 'BUSINESS_RUNWAY', 'Business Runway', 'Business runway moment.', 10, 'ACTIVE');

INSERT INTO core.moment_type (moment_type_id, moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('1937f819-c38b-5dcd-adcb-14d9d726a142'::uuid, 'ecf054e2-fd5e-5e08-85ad-2d841268a24a'::uuid, 'BUSINESS', 'BUSINESS_OPERATIONS', 'Business Operations', 'Business operations moment.', 10, 'ACTIVE');

INSERT INTO core.moment_type (moment_type_id, moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('8a3a5878-e5b9-501f-a503-3dc254030d3b'::uuid, '64044c78-e7ac-518e-a755-abc3d07ec0e6'::uuid, 'BUSINESS', 'EVENTS_OPERATIONS', 'Events Operations', 'Business events operations moment.', 10, 'ACTIVE');

INSERT INTO core.moment_type (moment_type_id, moment_category_id, domain_code, code, display_name, description, sort_order, status)
VALUES ('f3ef675c-53a2-5d7c-9c9f-60a9d97a35ee'::uuid, 'b61d5438-6a92-5780-ba2d-0bfe151346ab'::uuid, 'BUSINESS', 'VENDOR_OPERATIONS', 'Vendor Operations', 'Business vendor operations moment.', 10, 'ACTIVE');

COMMIT;
