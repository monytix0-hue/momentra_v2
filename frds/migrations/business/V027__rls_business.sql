BEGIN;

CREATE POLICY rls_business_moment_context__select_member ON business.business_moment_context
FOR SELECT USING ((security.is_active_company_member(company_id)) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_business_moment_context__backend_write ON business.business_moment_context
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_business_operations_context__select_member ON business.business_operations_context
FOR SELECT USING ((EXISTS (SELECT 1 FROM business.business_moment_context bmc WHERE bmc.moment_id = business_operations_context.moment_id AND security.is_active_company_member(bmc.company_id))) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_business_operations_context__backend_write ON business.business_operations_context
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_business_review__select_member ON business.business_review
FOR SELECT USING ((security.is_active_company_member(company_id)) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_business_review__backend_write ON business.business_review
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_business_runway_context__select_member ON business.business_runway_context
FOR SELECT USING ((EXISTS (SELECT 1 FROM business.business_moment_context bmc WHERE bmc.moment_id = business_runway_context.moment_id AND security.is_active_company_member(bmc.company_id))) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_business_runway_context__backend_write ON business.business_runway_context
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_business_update__select_member ON business.business_update
FOR SELECT USING ((security.is_active_company_member(company_id)) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_business_update__backend_write ON business.business_update
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_company__select_member ON business.company
FOR SELECT USING ((security.is_active_company_member(company_id)) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_company__backend_write ON business.company
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_company_membership__select_member ON business.company_membership
FOR SELECT USING ((security.is_active_company_member(company_id)) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_company_membership__backend_write ON business.company_membership
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_decision__select_member ON business.decision
FOR SELECT USING ((security.is_active_company_member(company_id)) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_decision__backend_write ON business.decision
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_events_operations_context__select_member ON business.events_operations_context
FOR SELECT USING ((EXISTS (SELECT 1 FROM business.business_moment_context bmc WHERE bmc.moment_id = events_operations_context.moment_id AND security.is_active_company_member(bmc.company_id))) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_events_operations_context__backend_write ON business.events_operations_context
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_issue__select_member ON business.issue
FOR SELECT USING ((security.is_active_company_member(company_id)) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_issue__backend_write ON business.issue
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_risk__select_member ON business.risk
FOR SELECT USING ((security.is_active_company_member(company_id)) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_risk__backend_write ON business.risk
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_sla_check__select_member ON business.sla_check
FOR SELECT USING ((security.is_active_company_member(company_id)) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_sla_check__backend_write ON business.sla_check
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_sla_definition__select_member ON business.sla_definition
FOR SELECT USING ((security.is_active_company_member(company_id)) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_sla_definition__backend_write ON business.sla_definition
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_team__select_member ON business.team
FOR SELECT USING ((security.is_active_company_member(company_id)) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_team__backend_write ON business.team
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_team_membership__select_member ON business.team_membership
FOR SELECT USING ((security.is_active_company_member(company_id)) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_team_membership__backend_write ON business.team_membership
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_team_operations_context__select_member ON business.team_operations_context
FOR SELECT USING ((EXISTS (SELECT 1 FROM business.business_moment_context bmc WHERE bmc.moment_id = team_operations_context.moment_id AND security.is_active_company_member(bmc.company_id))) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_team_operations_context__backend_write ON business.team_operations_context
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_vendor__select_member ON business.vendor
FOR SELECT USING ((security.is_active_company_member(company_id)) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_vendor__backend_write ON business.vendor
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_vendor_contract__select_member ON business.vendor_contract
FOR SELECT USING ((security.is_active_company_member(company_id)) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_vendor_contract__backend_write ON business.vendor_contract
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_vendor_operations_context__select_member ON business.vendor_operations_context
FOR SELECT USING ((EXISTS (SELECT 1 FROM business.business_moment_context bmc WHERE bmc.moment_id = vendor_operations_context.moment_id AND security.is_active_company_member(bmc.company_id))) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_vendor_operations_context__backend_write ON business.vendor_operations_context
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

COMMIT;
