BEGIN;

CREATE POLICY rls_attendance__select_participant ON collaboration.attendance
FOR SELECT USING ((security.is_active_group_participant(moment_id)) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_attendance__backend_write ON collaboration.attendance
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_booking__select_participant ON collaboration.booking
FOR SELECT USING ((security.is_active_group_participant(moment_id)) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_booking__backend_write ON collaboration.booking
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_community_coordination_context__select_participant ON collaboration.community_coordination_context
FOR SELECT USING ((security.is_active_group_participant(moment_id)) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_community_coordination_context__backend_write ON collaboration.community_coordination_context
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_coordination_item__select_participant ON collaboration.coordination_item
FOR SELECT USING ((security.is_active_group_participant(moment_id)) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_coordination_item__backend_write ON collaboration.coordination_item
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_delivery_handover__select_participant ON collaboration.delivery_handover
FOR SELECT USING ((security.is_active_group_participant(moment_id)) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_delivery_handover__backend_write ON collaboration.delivery_handover
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_group_moment_context__select_participant ON collaboration.group_moment_context
FOR SELECT USING ((security.is_active_group_participant(moment_id)) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_group_moment_context__backend_write ON collaboration.group_moment_context
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_group_update__select_participant ON collaboration.group_update
FOR SELECT USING ((security.is_active_group_participant(moment_id)) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_group_update__backend_write ON collaboration.group_update
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_group_vendor__select_participant ON collaboration.group_vendor
FOR SELECT USING ((security.is_active_group_participant(moment_id)) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_group_vendor__backend_write ON collaboration.group_vendor
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_living_rule__select_participant ON collaboration.living_rule
FOR SELECT USING ((security.is_active_group_participant(moment_id)) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_living_rule__backend_write ON collaboration.living_rule
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_maintenance_record__select_participant ON collaboration.maintenance_record
FOR SELECT USING ((security.is_active_group_participant(moment_id)) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_maintenance_record__backend_write ON collaboration.maintenance_record
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_moment_participant__select_participant ON collaboration.moment_participant
FOR SELECT USING ((security.is_active_group_participant(moment_id)) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_moment_participant__backend_write ON collaboration.moment_participant
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_ownership_record__select_participant ON collaboration.ownership_record
FOR SELECT USING ((security.is_active_group_participant(moment_id)) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_ownership_record__backend_write ON collaboration.ownership_record
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_planning_item__select_participant ON collaboration.planning_item
FOR SELECT USING ((security.is_active_group_participant(moment_id)) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_planning_item__backend_write ON collaboration.planning_item
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_poll__select_participant ON collaboration.poll
FOR SELECT USING ((security.is_active_group_participant(moment_id)) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_poll__backend_write ON collaboration.poll
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_poll_option__select_participant ON collaboration.poll_option
FOR SELECT USING ((EXISTS (SELECT 1 FROM collaboration.poll p WHERE p.poll_id = poll_option.poll_id AND security.is_active_group_participant(p.moment_id))) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_poll_option__backend_write ON collaboration.poll_option
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_poll_vote__select_participant ON collaboration.poll_vote
FOR SELECT USING ((security.is_active_group_participant(moment_id)) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_poll_vote__backend_write ON collaboration.poll_vote
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_purchase_item__select_participant ON collaboration.purchase_item
FOR SELECT USING ((security.is_active_group_participant(moment_id)) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_purchase_item__backend_write ON collaboration.purchase_item
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_resident__select_participant ON collaboration.resident
FOR SELECT USING ((security.is_active_group_participant(moment_id)) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_resident__backend_write ON collaboration.resident
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_shared_asset__select_participant ON collaboration.shared_asset
FOR SELECT USING ((security.is_active_group_participant(moment_id)) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_shared_asset__backend_write ON collaboration.shared_asset
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_shared_experience_context__select_participant ON collaboration.shared_experience_context
FOR SELECT USING ((security.is_active_group_participant(moment_id)) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_shared_experience_context__backend_write ON collaboration.shared_experience_context
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_shared_goal_context__select_participant ON collaboration.shared_goal_context
FOR SELECT USING ((security.is_active_group_participant(moment_id)) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_shared_goal_context__backend_write ON collaboration.shared_goal_context
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_shared_living_context__select_participant ON collaboration.shared_living_context
FOR SELECT USING ((security.is_active_group_participant(moment_id)) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_shared_living_context__backend_write ON collaboration.shared_living_context
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_shared_purchase_context__select_participant ON collaboration.shared_purchase_context
FOR SELECT USING ((security.is_active_group_participant(moment_id)) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_shared_purchase_context__backend_write ON collaboration.shared_purchase_context
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

COMMIT;
