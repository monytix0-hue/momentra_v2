BEGIN;

CREATE POLICY rls_future_learning_activity__select_owner ON personal.future_learning_activity
FOR SELECT USING ((user_id = security.current_user_id()) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_future_learning_activity__backend_write ON personal.future_learning_activity
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_future_opportunity__select_owner ON personal.future_opportunity
FOR SELECT USING ((user_id = security.current_user_id()) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_future_opportunity__backend_write ON personal.future_opportunity
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_future_pivot__select_owner ON personal.future_pivot
FOR SELECT USING ((user_id = security.current_user_id()) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_future_pivot__backend_write ON personal.future_pivot
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_future_progress_observation__select_owner ON personal.future_progress_observation
FOR SELECT USING ((user_id = security.current_user_id()) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_future_progress_observation__backend_write ON personal.future_progress_observation
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_life_operation_observation__select_owner ON personal.life_operation_observation
FOR SELECT USING ((user_id = security.current_user_id()) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_life_operation_observation__backend_write ON personal.life_operation_observation
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_lifestyle_activity__select_owner ON personal.lifestyle_activity
FOR SELECT USING ((user_id = security.current_user_id()) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_lifestyle_activity__backend_write ON personal.lifestyle_activity
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_personal_moment_context__select_owner ON personal.personal_moment_context
FOR SELECT USING ((user_id = security.current_user_id()) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_personal_moment_context__backend_write ON personal.personal_moment_context
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_relationship_activity__select_owner ON personal.relationship_activity
FOR SELECT USING ((user_id = security.current_user_id()) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_relationship_activity__backend_write ON personal.relationship_activity
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_relationship_connection__select_owner ON personal.relationship_connection
FOR SELECT USING ((user_id = security.current_user_id()) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_relationship_connection__backend_write ON personal.relationship_connection
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

COMMIT;
