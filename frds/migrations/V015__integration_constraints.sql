BEGIN;

-- Deferred event references intentionally added only after events/audit/platform/ai/projection all exist.

-- Strengthen user identity consistency in user-specific Group and Business access projections.
ALTER TABLE collaboration.moment_participant
    ADD CONSTRAINT uq_moment_participant__id_moment_user UNIQUE (participant_id, moment_id, user_id);
ALTER TABLE projection.group_moments
    ADD CONSTRAINT fk_group_moments__participant_user
    FOREIGN KEY (participant_id, moment_id, user_id)
    REFERENCES collaboration.moment_participant(participant_id, moment_id, user_id)
    ON DELETE CASCADE;

ALTER TABLE business.company_membership
    ADD CONSTRAINT uq_company_membership__id_company_user UNIQUE (company_membership_id, company_id, user_id);
ALTER TABLE projection.user_company_access
    ADD CONSTRAINT fk_user_company_access__membership_user
    FOREIGN KEY (membership_id, company_id, user_id)
    REFERENCES business.company_membership(company_membership_id, company_id, user_id)
    ON DELETE CASCADE;

ALTER TABLE analytics.calculation_run
    ADD CONSTRAINT fk_calculation_run__trigger_event
    FOREIGN KEY (trigger_event_id) REFERENCES events.domain_event(domain_event_id) ON DELETE SET NULL;

ALTER TABLE audit.audit_record
    ADD CONSTRAINT fk_audit_record__domain_event
    FOREIGN KEY (domain_event_id) REFERENCES events.domain_event(domain_event_id) ON DELETE SET NULL;

ALTER TABLE platform.processing_checkpoint
    ADD CONSTRAINT fk_processing_checkpoint__event
    FOREIGN KEY (checkpoint_event_id) REFERENCES events.domain_event(domain_event_id) ON DELETE SET NULL;

ALTER TABLE projection.moment_summary ADD CONSTRAINT fk_moment_summary__source_event FOREIGN KEY (source_event_id) REFERENCES events.domain_event(domain_event_id) ON DELETE SET NULL;
ALTER TABLE projection.personal_pulse ADD CONSTRAINT fk_personal_pulse__source_event FOREIGN KEY (source_event_id) REFERENCES events.domain_event(domain_event_id) ON DELETE SET NULL;
ALTER TABLE projection.personal_moments ADD CONSTRAINT fk_personal_moments__source_event FOREIGN KEY (source_event_id) REFERENCES events.domain_event(domain_event_id) ON DELETE SET NULL;
ALTER TABLE projection.personal_life ADD CONSTRAINT fk_personal_life__source_event FOREIGN KEY (source_event_id) REFERENCES events.domain_event(domain_event_id) ON DELETE SET NULL;
ALTER TABLE projection.personal_memory ADD CONSTRAINT fk_personal_memory__source_event FOREIGN KEY (source_event_id) REFERENCES events.domain_event(domain_event_id) ON DELETE SET NULL;
ALTER TABLE projection.personal_finance_snapshot ADD CONSTRAINT fk_personal_finance_snapshot__source_event FOREIGN KEY (source_event_id) REFERENCES events.domain_event(domain_event_id) ON DELETE SET NULL;
ALTER TABLE projection.group_pulse ADD CONSTRAINT fk_group_pulse__source_event FOREIGN KEY (source_event_id) REFERENCES events.domain_event(domain_event_id) ON DELETE SET NULL;
ALTER TABLE projection.group_moments ADD CONSTRAINT fk_group_moments__source_event FOREIGN KEY (source_event_id) REFERENCES events.domain_event(domain_event_id) ON DELETE SET NULL;
ALTER TABLE projection.group_life ADD CONSTRAINT fk_group_life__source_event FOREIGN KEY (source_event_id) REFERENCES events.domain_event(domain_event_id) ON DELETE SET NULL;
ALTER TABLE projection.group_memory ADD CONSTRAINT fk_group_memory__source_event FOREIGN KEY (source_event_id) REFERENCES events.domain_event(domain_event_id) ON DELETE SET NULL;
ALTER TABLE projection.group_finance_snapshot ADD CONSTRAINT fk_group_finance_snapshot__source_event FOREIGN KEY (source_event_id) REFERENCES events.domain_event(domain_event_id) ON DELETE SET NULL;
ALTER TABLE projection.group_finance_position ADD CONSTRAINT fk_group_finance_position__source_event FOREIGN KEY (source_event_id) REFERENCES events.domain_event(domain_event_id) ON DELETE SET NULL;
ALTER TABLE projection.business_pulse ADD CONSTRAINT fk_business_pulse__source_event FOREIGN KEY (source_event_id) REFERENCES events.domain_event(domain_event_id) ON DELETE SET NULL;
ALTER TABLE projection.business_moments ADD CONSTRAINT fk_business_moments__source_event FOREIGN KEY (source_event_id) REFERENCES events.domain_event(domain_event_id) ON DELETE SET NULL;
ALTER TABLE projection.business_life ADD CONSTRAINT fk_business_life__source_event FOREIGN KEY (source_event_id) REFERENCES events.domain_event(domain_event_id) ON DELETE SET NULL;
ALTER TABLE projection.business_memory ADD CONSTRAINT fk_business_memory__source_event FOREIGN KEY (source_event_id) REFERENCES events.domain_event(domain_event_id) ON DELETE SET NULL;
ALTER TABLE projection.business_finance_snapshot ADD CONSTRAINT fk_business_finance_snapshot__source_event FOREIGN KEY (source_event_id) REFERENCES events.domain_event(domain_event_id) ON DELETE SET NULL;
ALTER TABLE projection.available_action ADD CONSTRAINT fk_available_action__source_event FOREIGN KEY (source_event_id) REFERENCES events.domain_event(domain_event_id) ON DELETE SET NULL;
ALTER TABLE projection.pending_approval_summary ADD CONSTRAINT fk_pending_approval_summary__source_event FOREIGN KEY (source_event_id) REFERENCES events.domain_event(domain_event_id) ON DELETE SET NULL;
ALTER TABLE projection.attention_summary ADD CONSTRAINT fk_attention_summary__source_event FOREIGN KEY (source_event_id) REFERENCES events.domain_event(domain_event_id) ON DELETE SET NULL;
ALTER TABLE projection.life360 ADD CONSTRAINT fk_life360__source_event FOREIGN KEY (source_event_id) REFERENCES events.domain_event(domain_event_id) ON DELETE SET NULL;
ALTER TABLE projection.user_company_access ADD CONSTRAINT fk_user_company_access__source_event FOREIGN KEY (source_event_id) REFERENCES events.domain_event(domain_event_id) ON DELETE SET NULL;
ALTER TABLE projection.recent_activity ADD CONSTRAINT fk_recent_activity__source_event FOREIGN KEY (source_event_id) REFERENCES events.domain_event(domain_event_id) ON DELETE RESTRICT;
ALTER TABLE projection.projection_state ADD CONSTRAINT fk_projection_state__last_event FOREIGN KEY (last_event_id) REFERENCES events.domain_event(domain_event_id) ON DELETE SET NULL;

COMMIT;
