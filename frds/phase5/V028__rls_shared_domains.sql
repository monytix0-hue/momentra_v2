BEGIN;
-- Shared Work RLS

CREATE POLICY rls_assignment__select_scope ON work.assignment
FOR SELECT USING ((security.can_access_moment(moment_id)) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_assignment__backend_write ON work.assignment
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_goal__select_scope ON work.goal
FOR SELECT USING ((security.can_access_moment(moment_id)) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_goal__backend_write ON work.goal
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_milestone__select_scope ON work.milestone
FOR SELECT USING ((security.can_access_moment(moment_id)) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_milestone__backend_write ON work.milestone
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_task__select_scope ON work.task
FOR SELECT USING ((security.can_access_moment(moment_id)) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_task__backend_write ON work.task
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_task_dependency__select_scope ON work.task_dependency
FOR SELECT USING ((EXISTS (SELECT 1 FROM work.task wt WHERE wt.task_id = task_dependency.task_id AND security.can_access_moment(wt.moment_id))) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_task_dependency__backend_write ON work.task_dependency
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

-- Cross-Domain Finance RLS

CREATE POLICY rls_budget__select_scope ON finance.budget
FOR SELECT USING ((security.can_access_scope(scope_type, scope_id)) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_budget__backend_write ON finance.budget
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_budget_revision__select_scope ON finance.budget_revision
FOR SELECT USING ((EXISTS (SELECT 1 FROM finance.budget b WHERE b.budget_id=budget_revision.budget_id AND security.can_access_scope(b.scope_type,b.scope_id))) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_budget_revision__backend_write ON finance.budget_revision
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_business_expense_context__select_scope ON finance.business_expense_context
FOR SELECT USING ((security.is_active_company_member(company_id)) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_business_expense_context__backend_write ON finance.business_expense_context
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_contribution__select_scope ON finance.contribution
FOR SELECT USING ((security.is_active_group_participant(moment_id)) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_contribution__backend_write ON finance.contribution
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_expense__select_scope ON finance.expense
FOR SELECT USING ((security.can_access_moment(moment_id)) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_expense__backend_write ON finance.expense
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_expense_resource_link__select_scope ON finance.expense_resource_link
FOR SELECT USING ((EXISTS (SELECT 1 FROM finance.expense e WHERE e.expense_id=expense_resource_link.expense_id AND security.can_access_moment(e.moment_id))) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_expense_resource_link__backend_write ON finance.expense_resource_link
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_expense_share__select_scope ON finance.expense_share
FOR SELECT USING ((security.is_active_group_participant(moment_id)) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_expense_share__backend_write ON finance.expense_share
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_expense_split__select_scope ON finance.expense_split
FOR SELECT USING ((EXISTS (SELECT 1 FROM finance.expense e WHERE e.expense_id=expense_split.expense_id AND security.can_access_moment(e.moment_id))) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_expense_split__backend_write ON finance.expense_split
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_financial_account__select_scope ON finance.financial_account
FOR SELECT USING ((((owner_scope_type='USER' AND owner_user_id=security.current_user_id()) OR (owner_scope_type='COMPANY' AND security.is_active_company_member(owner_company_id)))) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_financial_account__backend_write ON finance.financial_account
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_financial_movement__select_scope ON finance.financial_movement
FOR SELECT USING ((EXISTS (SELECT 1 FROM finance.financial_account a WHERE a.financial_account_id=financial_movement.financial_account_id AND ((a.owner_scope_type='USER' AND a.owner_user_id=security.current_user_id()) OR (a.owner_scope_type='COMPANY' AND security.is_active_company_member(a.owner_company_id))))) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_financial_movement__backend_write ON finance.financial_movement
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_financial_movement_link__select_scope ON finance.financial_movement_link
FOR SELECT USING ((EXISTS (SELECT 1 FROM finance.financial_movement fm JOIN finance.financial_account a ON a.financial_account_id=fm.financial_account_id WHERE fm.financial_movement_id=financial_movement_link.financial_movement_id AND ((a.owner_scope_type='USER' AND a.owner_user_id=security.current_user_id()) OR (a.owner_scope_type='COMPANY' AND security.is_active_company_member(a.owner_company_id))))) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_financial_movement_link__backend_write ON finance.financial_movement_link
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_group_expense_context__select_scope ON finance.group_expense_context
FOR SELECT USING ((security.is_active_group_participant(moment_id)) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_group_expense_context__backend_write ON finance.group_expense_context
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_invoice__select_scope ON finance.invoice
FOR SELECT USING ((security.is_active_company_member(company_id)) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_invoice__backend_write ON finance.invoice
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_invoice_line__select_scope ON finance.invoice_line
FOR SELECT USING ((EXISTS (SELECT 1 FROM finance.invoice i WHERE i.invoice_id=invoice_line.invoice_id AND security.is_active_company_member(i.company_id))) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_invoice_line__backend_write ON finance.invoice_line
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_invoice_payment__select_scope ON finance.invoice_payment
FOR SELECT USING ((EXISTS (SELECT 1 FROM finance.invoice i WHERE i.invoice_id=invoice_payment.invoice_id AND security.is_active_company_member(i.company_id))) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_invoice_payment__backend_write ON finance.invoice_payment
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_participant_obligation__select_scope ON finance.participant_obligation
FOR SELECT USING ((security.is_active_group_participant(moment_id)) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_participant_obligation__backend_write ON finance.participant_obligation
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_personal_expense_context__select_scope ON finance.personal_expense_context
FOR SELECT USING ((user_id=security.current_user_id()) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_personal_expense_context__backend_write ON finance.personal_expense_context
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_revenue__select_scope ON finance.revenue
FOR SELECT USING ((security.is_active_company_member(company_id)) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_revenue__backend_write ON finance.revenue
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_settlement__select_scope ON finance.settlement
FOR SELECT USING ((security.is_active_group_participant(moment_id)) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_settlement__backend_write ON finance.settlement
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

CREATE POLICY rls_settlement_allocation__select_scope ON finance.settlement_allocation
FOR SELECT USING ((security.is_active_group_participant(moment_id)) OR security.is_backend_app() OR security.is_analytics_worker() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_settlement_allocation__backend_write ON finance.settlement_allocation
FOR ALL USING (security.is_backend_app()) WITH CHECK (security.is_backend_app());

-- Memory RLS

CREATE POLICY rls_learning__select_scope ON memory.learning
FOR SELECT USING ((security.can_access_scope(scope_type, scope_id)) OR security.is_backend_app() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_learning__backend_write ON memory.learning
FOR ALL USING (security.is_backend_app() OR security.is_memory_worker()) WITH CHECK (security.is_backend_app() OR security.is_memory_worker());

CREATE POLICY rls_learning_evidence__select_scope ON memory.learning_evidence
FOR SELECT USING ((EXISTS (SELECT 1 FROM memory.learning p WHERE p.learning_id=learning_evidence.learning_id AND security.can_access_scope(p.scope_type,p.scope_id))) OR security.is_backend_app() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_learning_evidence__backend_write ON memory.learning_evidence
FOR ALL USING (security.is_backend_app() OR security.is_memory_worker()) WITH CHECK (security.is_backend_app() OR security.is_memory_worker());

CREATE POLICY rls_memory__select_scope ON memory.memory
FOR SELECT USING ((security.can_access_scope(scope_type, scope_id)) OR security.is_backend_app() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_memory__backend_write ON memory.memory
FOR ALL USING (security.is_backend_app() OR security.is_memory_worker()) WITH CHECK (security.is_backend_app() OR security.is_memory_worker());

CREATE POLICY rls_memory_evidence__select_scope ON memory.memory_evidence
FOR SELECT USING ((EXISTS (SELECT 1 FROM memory.memory p WHERE p.memory_id=memory_evidence.memory_id AND security.can_access_scope(p.scope_type,p.scope_id))) OR security.is_backend_app() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_memory_evidence__backend_write ON memory.memory_evidence
FOR ALL USING (security.is_backend_app() OR security.is_memory_worker()) WITH CHECK (security.is_backend_app() OR security.is_memory_worker());

CREATE POLICY rls_pattern__select_scope ON memory.pattern
FOR SELECT USING ((security.can_access_scope(scope_type, scope_id)) OR security.is_backend_app() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_pattern__backend_write ON memory.pattern
FOR ALL USING (security.is_backend_app() OR security.is_memory_worker()) WITH CHECK (security.is_backend_app() OR security.is_memory_worker());

CREATE POLICY rls_pattern_occurrence__select_scope ON memory.pattern_occurrence
FOR SELECT USING ((EXISTS (SELECT 1 FROM memory.pattern p WHERE p.pattern_id=pattern_occurrence.pattern_id AND security.can_access_scope(p.scope_type,p.scope_id))) OR security.is_backend_app() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_pattern_occurrence__backend_write ON memory.pattern_occurrence
FOR ALL USING (security.is_backend_app() OR security.is_memory_worker()) WITH CHECK (security.is_backend_app() OR security.is_memory_worker());

CREATE POLICY rls_playbook__select_scope ON memory.playbook
FOR SELECT USING ((security.can_access_scope(scope_type, scope_id)) OR security.is_backend_app() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_playbook__backend_write ON memory.playbook
FOR ALL USING (security.is_backend_app() OR security.is_memory_worker()) WITH CHECK (security.is_backend_app() OR security.is_memory_worker());

CREATE POLICY rls_playbook_evidence__select_scope ON memory.playbook_evidence
FOR SELECT USING ((EXISTS (SELECT 1 FROM memory.playbook_version pv JOIN memory.playbook p ON p.playbook_id=pv.playbook_id WHERE pv.playbook_version_id=playbook_evidence.playbook_version_id AND security.can_access_scope(p.scope_type,p.scope_id))) OR security.is_backend_app() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_playbook_evidence__backend_write ON memory.playbook_evidence
FOR ALL USING (security.is_backend_app() OR security.is_memory_worker()) WITH CHECK (security.is_backend_app() OR security.is_memory_worker());

CREATE POLICY rls_playbook_version__select_scope ON memory.playbook_version
FOR SELECT USING ((EXISTS (SELECT 1 FROM memory.playbook p WHERE p.playbook_id=playbook_version.playbook_id AND security.can_access_scope(p.scope_type,p.scope_id))) OR security.is_backend_app() OR security.is_memory_worker() OR security.is_projection_worker());
CREATE POLICY rls_playbook_version__backend_write ON memory.playbook_version
FOR ALL USING (security.is_backend_app() OR security.is_memory_worker()) WITH CHECK (security.is_backend_app() OR security.is_memory_worker());

-- Projection RLS

CREATE POLICY rls_attention_summary__select_scope ON projection.attention_summary
FOR SELECT USING ((user_id=security.current_user_id()) OR security.is_backend_app() OR security.is_projection_worker());
CREATE POLICY rls_attention_summary__worker_write ON projection.attention_summary
FOR ALL USING (security.is_backend_app() OR security.is_projection_worker()) WITH CHECK (security.is_backend_app() OR security.is_projection_worker());

CREATE POLICY rls_available_action__select_scope ON projection.available_action
FOR SELECT USING ((user_id=security.current_user_id()) OR security.is_backend_app() OR security.is_projection_worker());
CREATE POLICY rls_available_action__worker_write ON projection.available_action
FOR ALL USING (security.is_backend_app() OR security.is_projection_worker()) WITH CHECK (security.is_backend_app() OR security.is_projection_worker());

CREATE POLICY rls_business_finance_snapshot__select_scope ON projection.business_finance_snapshot
FOR SELECT USING ((security.is_active_company_member(company_id)) OR security.is_backend_app() OR security.is_projection_worker());
CREATE POLICY rls_business_finance_snapshot__worker_write ON projection.business_finance_snapshot
FOR ALL USING (security.is_backend_app() OR security.is_projection_worker()) WITH CHECK (security.is_backend_app() OR security.is_projection_worker());

CREATE POLICY rls_business_life__select_scope ON projection.business_life
FOR SELECT USING ((security.is_active_company_member(company_id)) OR security.is_backend_app() OR security.is_projection_worker());
CREATE POLICY rls_business_life__worker_write ON projection.business_life
FOR ALL USING (security.is_backend_app() OR security.is_projection_worker()) WITH CHECK (security.is_backend_app() OR security.is_projection_worker());

CREATE POLICY rls_business_memory__select_scope ON projection.business_memory
FOR SELECT USING ((security.is_active_company_member(company_id)) OR security.is_backend_app() OR security.is_projection_worker());
CREATE POLICY rls_business_memory__worker_write ON projection.business_memory
FOR ALL USING (security.is_backend_app() OR security.is_projection_worker()) WITH CHECK (security.is_backend_app() OR security.is_projection_worker());

CREATE POLICY rls_business_moments__select_scope ON projection.business_moments
FOR SELECT USING ((security.is_active_company_member(company_id)) OR security.is_backend_app() OR security.is_projection_worker());
CREATE POLICY rls_business_moments__worker_write ON projection.business_moments
FOR ALL USING (security.is_backend_app() OR security.is_projection_worker()) WITH CHECK (security.is_backend_app() OR security.is_projection_worker());

CREATE POLICY rls_business_pulse__select_scope ON projection.business_pulse
FOR SELECT USING ((security.is_active_company_member(company_id)) OR security.is_backend_app() OR security.is_projection_worker());
CREATE POLICY rls_business_pulse__worker_write ON projection.business_pulse
FOR ALL USING (security.is_backend_app() OR security.is_projection_worker()) WITH CHECK (security.is_backend_app() OR security.is_projection_worker());

CREATE POLICY rls_group_finance_position__select_scope ON projection.group_finance_position
FOR SELECT USING ((security.is_active_group_participant(moment_id)) OR security.is_backend_app() OR security.is_projection_worker());
CREATE POLICY rls_group_finance_position__worker_write ON projection.group_finance_position
FOR ALL USING (security.is_backend_app() OR security.is_projection_worker()) WITH CHECK (security.is_backend_app() OR security.is_projection_worker());

CREATE POLICY rls_group_finance_snapshot__select_scope ON projection.group_finance_snapshot
FOR SELECT USING ((security.is_active_group_participant(moment_id)) OR security.is_backend_app() OR security.is_projection_worker());
CREATE POLICY rls_group_finance_snapshot__worker_write ON projection.group_finance_snapshot
FOR ALL USING (security.is_backend_app() OR security.is_projection_worker()) WITH CHECK (security.is_backend_app() OR security.is_projection_worker());

CREATE POLICY rls_group_life__select_scope ON projection.group_life
FOR SELECT USING ((security.is_active_group_participant(moment_id)) OR security.is_backend_app() OR security.is_projection_worker());
CREATE POLICY rls_group_life__worker_write ON projection.group_life
FOR ALL USING (security.is_backend_app() OR security.is_projection_worker()) WITH CHECK (security.is_backend_app() OR security.is_projection_worker());

CREATE POLICY rls_group_memory__select_scope ON projection.group_memory
FOR SELECT USING ((security.is_active_group_participant(moment_id)) OR security.is_backend_app() OR security.is_projection_worker());
CREATE POLICY rls_group_memory__worker_write ON projection.group_memory
FOR ALL USING (security.is_backend_app() OR security.is_projection_worker()) WITH CHECK (security.is_backend_app() OR security.is_projection_worker());

CREATE POLICY rls_group_moments__select_scope ON projection.group_moments
FOR SELECT USING ((user_id=security.current_user_id()) OR security.is_backend_app() OR security.is_projection_worker());
CREATE POLICY rls_group_moments__worker_write ON projection.group_moments
FOR ALL USING (security.is_backend_app() OR security.is_projection_worker()) WITH CHECK (security.is_backend_app() OR security.is_projection_worker());

CREATE POLICY rls_group_pulse__select_scope ON projection.group_pulse
FOR SELECT USING ((security.is_active_group_participant(moment_id)) OR security.is_backend_app() OR security.is_projection_worker());
CREATE POLICY rls_group_pulse__worker_write ON projection.group_pulse
FOR ALL USING (security.is_backend_app() OR security.is_projection_worker()) WITH CHECK (security.is_backend_app() OR security.is_projection_worker());

CREATE POLICY rls_moment_summary__select_scope ON projection.moment_summary
FOR SELECT USING ((security.can_access_scope(primary_scope_type,primary_scope_id)) OR security.is_backend_app() OR security.is_projection_worker());
CREATE POLICY rls_moment_summary__worker_write ON projection.moment_summary
FOR ALL USING (security.is_backend_app() OR security.is_projection_worker()) WITH CHECK (security.is_backend_app() OR security.is_projection_worker());

CREATE POLICY rls_pending_approval_summary__select_scope ON projection.pending_approval_summary
FOR SELECT USING ((user_id=security.current_user_id()) OR security.is_backend_app() OR security.is_projection_worker());
CREATE POLICY rls_pending_approval_summary__worker_write ON projection.pending_approval_summary
FOR ALL USING (security.is_backend_app() OR security.is_projection_worker()) WITH CHECK (security.is_backend_app() OR security.is_projection_worker());

CREATE POLICY rls_personal_finance_snapshot__select_scope ON projection.personal_finance_snapshot
FOR SELECT USING ((user_id=security.current_user_id()) OR security.is_backend_app() OR security.is_projection_worker());
CREATE POLICY rls_personal_finance_snapshot__worker_write ON projection.personal_finance_snapshot
FOR ALL USING (security.is_backend_app() OR security.is_projection_worker()) WITH CHECK (security.is_backend_app() OR security.is_projection_worker());

CREATE POLICY rls_personal_life__select_scope ON projection.personal_life
FOR SELECT USING ((user_id=security.current_user_id()) OR security.is_backend_app() OR security.is_projection_worker());
CREATE POLICY rls_personal_life__worker_write ON projection.personal_life
FOR ALL USING (security.is_backend_app() OR security.is_projection_worker()) WITH CHECK (security.is_backend_app() OR security.is_projection_worker());

CREATE POLICY rls_personal_memory__select_scope ON projection.personal_memory
FOR SELECT USING ((user_id=security.current_user_id()) OR security.is_backend_app() OR security.is_projection_worker());
CREATE POLICY rls_personal_memory__worker_write ON projection.personal_memory
FOR ALL USING (security.is_backend_app() OR security.is_projection_worker()) WITH CHECK (security.is_backend_app() OR security.is_projection_worker());

CREATE POLICY rls_personal_moments__select_scope ON projection.personal_moments
FOR SELECT USING ((user_id=security.current_user_id()) OR security.is_backend_app() OR security.is_projection_worker());
CREATE POLICY rls_personal_moments__worker_write ON projection.personal_moments
FOR ALL USING (security.is_backend_app() OR security.is_projection_worker()) WITH CHECK (security.is_backend_app() OR security.is_projection_worker());

CREATE POLICY rls_personal_pulse__select_scope ON projection.personal_pulse
FOR SELECT USING ((user_id=security.current_user_id()) OR security.is_backend_app() OR security.is_projection_worker());
CREATE POLICY rls_personal_pulse__worker_write ON projection.personal_pulse
FOR ALL USING (security.is_backend_app() OR security.is_projection_worker()) WITH CHECK (security.is_backend_app() OR security.is_projection_worker());

CREATE POLICY rls_projection_state__select_scope ON projection.projection_state
FOR SELECT USING ((FALSE) OR security.is_backend_app() OR security.is_projection_worker());
CREATE POLICY rls_projection_state__worker_write ON projection.projection_state
FOR ALL USING (security.is_backend_app() OR security.is_projection_worker()) WITH CHECK (security.is_backend_app() OR security.is_projection_worker());

CREATE POLICY rls_recent_activity__select_scope ON projection.recent_activity
FOR SELECT USING ((user_id=security.current_user_id()) OR security.is_backend_app() OR security.is_projection_worker());
CREATE POLICY rls_recent_activity__worker_write ON projection.recent_activity
FOR ALL USING (security.is_backend_app() OR security.is_projection_worker()) WITH CHECK (security.is_backend_app() OR security.is_projection_worker());

CREATE POLICY rls_user_company_access__select_scope ON projection.user_company_access
FOR SELECT USING ((user_id=security.current_user_id()) OR security.is_backend_app() OR security.is_projection_worker());
CREATE POLICY rls_user_company_access__worker_write ON projection.user_company_access
FOR ALL USING (security.is_backend_app() OR security.is_projection_worker()) WITH CHECK (security.is_backend_app() OR security.is_projection_worker());

COMMIT;
