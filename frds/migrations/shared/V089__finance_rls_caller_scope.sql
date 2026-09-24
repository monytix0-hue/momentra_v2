-- Finance RLS follows the signed-in caller.
-- momentra_app no longer sees or writes every finance row.
-- Writes require is_backend_app(), a non-null security.current_user_id(), and the same
-- scope predicate the SELECT policy used before the app-role bypass.
-- Analytics, memory, and projection workers keep unrestricted SELECT.
-- No background worker writes finance.* outside a user request, so there is no null-user bypass.

BEGIN;

CREATE OR REPLACE FUNCTION security.finance_caller_write(p_in_scope BOOLEAN)
RETURNS BOOLEAN
LANGUAGE sql
STABLE
AS $$
  SELECT security.is_backend_app()
     AND security.current_user_id() IS NOT NULL
     AND COALESCE(p_in_scope, false);
$$;

-- Shared finance tables (V028).

DROP POLICY IF EXISTS rls_budget__select_scope ON finance.budget;
CREATE POLICY rls_budget__select_scope ON finance.budget
FOR SELECT USING (
  security.can_access_scope(scope_type, scope_id)
  OR security.is_analytics_worker()
  OR security.is_memory_worker()
  OR security.is_projection_worker()
);
DROP POLICY IF EXISTS rls_budget__backend_write ON finance.budget;
CREATE POLICY rls_budget__backend_write ON finance.budget
FOR ALL
USING (security.finance_caller_write(security.can_access_scope(scope_type, scope_id)))
WITH CHECK (security.finance_caller_write(security.can_access_scope(scope_type, scope_id)));

DROP POLICY IF EXISTS rls_budget_revision__select_scope ON finance.budget_revision;
CREATE POLICY rls_budget_revision__select_scope ON finance.budget_revision
FOR SELECT USING (
  EXISTS (
    SELECT 1 FROM finance.budget b
    WHERE b.budget_id = budget_revision.budget_id
      AND security.can_access_scope(b.scope_type, b.scope_id)
  )
  OR security.is_analytics_worker()
  OR security.is_memory_worker()
  OR security.is_projection_worker()
);
DROP POLICY IF EXISTS rls_budget_revision__backend_write ON finance.budget_revision;
CREATE POLICY rls_budget_revision__backend_write ON finance.budget_revision
FOR ALL
USING (security.finance_caller_write(EXISTS (
  SELECT 1 FROM finance.budget b
  WHERE b.budget_id = budget_revision.budget_id
    AND security.can_access_scope(b.scope_type, b.scope_id)
)))
WITH CHECK (security.finance_caller_write(EXISTS (
  SELECT 1 FROM finance.budget b
  WHERE b.budget_id = budget_revision.budget_id
    AND security.can_access_scope(b.scope_type, b.scope_id)
)));

DROP POLICY IF EXISTS rls_business_expense_context__select_scope ON finance.business_expense_context;
CREATE POLICY rls_business_expense_context__select_scope ON finance.business_expense_context
FOR SELECT USING (
  security.is_active_company_member(company_id)
  OR security.is_analytics_worker()
  OR security.is_memory_worker()
  OR security.is_projection_worker()
);
DROP POLICY IF EXISTS rls_business_expense_context__backend_write ON finance.business_expense_context;
CREATE POLICY rls_business_expense_context__backend_write ON finance.business_expense_context
FOR ALL
USING (security.finance_caller_write(security.is_active_company_member(company_id)))
WITH CHECK (security.finance_caller_write(security.is_active_company_member(company_id)));

DROP POLICY IF EXISTS rls_contribution__select_scope ON finance.contribution;
CREATE POLICY rls_contribution__select_scope ON finance.contribution
FOR SELECT USING (
  security.is_active_group_participant(moment_id)
  OR security.is_analytics_worker()
  OR security.is_memory_worker()
  OR security.is_projection_worker()
);
DROP POLICY IF EXISTS rls_contribution__backend_write ON finance.contribution;
CREATE POLICY rls_contribution__backend_write ON finance.contribution
FOR ALL
USING (security.finance_caller_write(security.is_active_group_participant(moment_id)))
WITH CHECK (security.finance_caller_write(security.is_active_group_participant(moment_id)));

DROP POLICY IF EXISTS rls_expense__select_scope ON finance.expense;
CREATE POLICY rls_expense__select_scope ON finance.expense
FOR SELECT USING (
  security.can_access_moment(moment_id)
  OR security.is_analytics_worker()
  OR security.is_memory_worker()
  OR security.is_projection_worker()
);
DROP POLICY IF EXISTS rls_expense__backend_write ON finance.expense;
CREATE POLICY rls_expense__backend_write ON finance.expense
FOR ALL
USING (security.finance_caller_write(security.can_access_moment(moment_id)))
WITH CHECK (security.finance_caller_write(security.can_access_moment(moment_id)));

DROP POLICY IF EXISTS rls_expense_resource_link__select_scope ON finance.expense_resource_link;
CREATE POLICY rls_expense_resource_link__select_scope ON finance.expense_resource_link
FOR SELECT USING (
  EXISTS (
    SELECT 1 FROM finance.expense e
    WHERE e.expense_id = expense_resource_link.expense_id
      AND security.can_access_moment(e.moment_id)
  )
  OR security.is_analytics_worker()
  OR security.is_memory_worker()
  OR security.is_projection_worker()
);
DROP POLICY IF EXISTS rls_expense_resource_link__backend_write ON finance.expense_resource_link;
CREATE POLICY rls_expense_resource_link__backend_write ON finance.expense_resource_link
FOR ALL
USING (security.finance_caller_write(EXISTS (
  SELECT 1 FROM finance.expense e
  WHERE e.expense_id = expense_resource_link.expense_id
    AND security.can_access_moment(e.moment_id)
)))
WITH CHECK (security.finance_caller_write(EXISTS (
  SELECT 1 FROM finance.expense e
  WHERE e.expense_id = expense_resource_link.expense_id
    AND security.can_access_moment(e.moment_id)
)));

DROP POLICY IF EXISTS rls_expense_share__select_scope ON finance.expense_share;
CREATE POLICY rls_expense_share__select_scope ON finance.expense_share
FOR SELECT USING (
  security.is_active_group_participant(moment_id)
  OR security.is_analytics_worker()
  OR security.is_memory_worker()
  OR security.is_projection_worker()
);
DROP POLICY IF EXISTS rls_expense_share__backend_write ON finance.expense_share;
CREATE POLICY rls_expense_share__backend_write ON finance.expense_share
FOR ALL
USING (security.finance_caller_write(security.is_active_group_participant(moment_id)))
WITH CHECK (security.finance_caller_write(security.is_active_group_participant(moment_id)));

DROP POLICY IF EXISTS rls_expense_split__select_scope ON finance.expense_split;
CREATE POLICY rls_expense_split__select_scope ON finance.expense_split
FOR SELECT USING (
  EXISTS (
    SELECT 1 FROM finance.expense e
    WHERE e.expense_id = expense_split.expense_id
      AND security.can_access_moment(e.moment_id)
  )
  OR security.is_analytics_worker()
  OR security.is_memory_worker()
  OR security.is_projection_worker()
);
DROP POLICY IF EXISTS rls_expense_split__backend_write ON finance.expense_split;
CREATE POLICY rls_expense_split__backend_write ON finance.expense_split
FOR ALL
USING (security.finance_caller_write(EXISTS (
  SELECT 1 FROM finance.expense e
  WHERE e.expense_id = expense_split.expense_id
    AND security.can_access_moment(e.moment_id)
)))
WITH CHECK (security.finance_caller_write(EXISTS (
  SELECT 1 FROM finance.expense e
  WHERE e.expense_id = expense_split.expense_id
    AND security.can_access_moment(e.moment_id)
)));

DROP POLICY IF EXISTS rls_financial_account__select_scope ON finance.financial_account;
CREATE POLICY rls_financial_account__select_scope ON finance.financial_account
FOR SELECT USING (
  (
    (owner_scope_type = 'USER' AND owner_user_id = security.current_user_id())
    OR (owner_scope_type = 'COMPANY' AND security.is_active_company_member(owner_company_id))
  )
  OR security.is_analytics_worker()
  OR security.is_memory_worker()
  OR security.is_projection_worker()
);
DROP POLICY IF EXISTS rls_financial_account__backend_write ON finance.financial_account;
CREATE POLICY rls_financial_account__backend_write ON finance.financial_account
FOR ALL
USING (security.finance_caller_write(
  (owner_scope_type = 'USER' AND owner_user_id = security.current_user_id())
  OR (owner_scope_type = 'COMPANY' AND security.is_active_company_member(owner_company_id))
))
WITH CHECK (security.finance_caller_write(
  (owner_scope_type = 'USER' AND owner_user_id = security.current_user_id())
  OR (owner_scope_type = 'COMPANY' AND security.is_active_company_member(owner_company_id))
));

DROP POLICY IF EXISTS rls_financial_movement__select_scope ON finance.financial_movement;
CREATE POLICY rls_financial_movement__select_scope ON finance.financial_movement
FOR SELECT USING (
  EXISTS (
    SELECT 1 FROM finance.financial_account a
    WHERE a.financial_account_id = financial_movement.financial_account_id
      AND (
        (a.owner_scope_type = 'USER' AND a.owner_user_id = security.current_user_id())
        OR (a.owner_scope_type = 'COMPANY' AND security.is_active_company_member(a.owner_company_id))
      )
  )
  OR security.is_analytics_worker()
  OR security.is_memory_worker()
  OR security.is_projection_worker()
);
DROP POLICY IF EXISTS rls_financial_movement__backend_write ON finance.financial_movement;
CREATE POLICY rls_financial_movement__backend_write ON finance.financial_movement
FOR ALL
USING (security.finance_caller_write(EXISTS (
  SELECT 1 FROM finance.financial_account a
  WHERE a.financial_account_id = financial_movement.financial_account_id
    AND (
      (a.owner_scope_type = 'USER' AND a.owner_user_id = security.current_user_id())
      OR (a.owner_scope_type = 'COMPANY' AND security.is_active_company_member(a.owner_company_id))
    )
)))
WITH CHECK (security.finance_caller_write(EXISTS (
  SELECT 1 FROM finance.financial_account a
  WHERE a.financial_account_id = financial_movement.financial_account_id
    AND (
      (a.owner_scope_type = 'USER' AND a.owner_user_id = security.current_user_id())
      OR (a.owner_scope_type = 'COMPANY' AND security.is_active_company_member(a.owner_company_id))
    )
)));

DROP POLICY IF EXISTS rls_financial_movement_link__select_scope ON finance.financial_movement_link;
CREATE POLICY rls_financial_movement_link__select_scope ON finance.financial_movement_link
FOR SELECT USING (
  EXISTS (
    SELECT 1
    FROM finance.financial_movement fm
    JOIN finance.financial_account a ON a.financial_account_id = fm.financial_account_id
    WHERE fm.financial_movement_id = financial_movement_link.financial_movement_id
      AND (
        (a.owner_scope_type = 'USER' AND a.owner_user_id = security.current_user_id())
        OR (a.owner_scope_type = 'COMPANY' AND security.is_active_company_member(a.owner_company_id))
      )
  )
  OR security.is_analytics_worker()
  OR security.is_memory_worker()
  OR security.is_projection_worker()
);
DROP POLICY IF EXISTS rls_financial_movement_link__backend_write ON finance.financial_movement_link;
CREATE POLICY rls_financial_movement_link__backend_write ON finance.financial_movement_link
FOR ALL
USING (security.finance_caller_write(EXISTS (
  SELECT 1
  FROM finance.financial_movement fm
  JOIN finance.financial_account a ON a.financial_account_id = fm.financial_account_id
  WHERE fm.financial_movement_id = financial_movement_link.financial_movement_id
    AND (
      (a.owner_scope_type = 'USER' AND a.owner_user_id = security.current_user_id())
      OR (a.owner_scope_type = 'COMPANY' AND security.is_active_company_member(a.owner_company_id))
    )
)))
WITH CHECK (security.finance_caller_write(EXISTS (
  SELECT 1
  FROM finance.financial_movement fm
  JOIN finance.financial_account a ON a.financial_account_id = fm.financial_account_id
  WHERE fm.financial_movement_id = financial_movement_link.financial_movement_id
    AND (
      (a.owner_scope_type = 'USER' AND a.owner_user_id = security.current_user_id())
      OR (a.owner_scope_type = 'COMPANY' AND security.is_active_company_member(a.owner_company_id))
    )
)));

DROP POLICY IF EXISTS rls_group_expense_context__select_scope ON finance.group_expense_context;
CREATE POLICY rls_group_expense_context__select_scope ON finance.group_expense_context
FOR SELECT USING (
  security.is_active_group_participant(moment_id)
  OR security.is_analytics_worker()
  OR security.is_memory_worker()
  OR security.is_projection_worker()
);
DROP POLICY IF EXISTS rls_group_expense_context__backend_write ON finance.group_expense_context;
CREATE POLICY rls_group_expense_context__backend_write ON finance.group_expense_context
FOR ALL
USING (security.finance_caller_write(security.is_active_group_participant(moment_id)))
WITH CHECK (security.finance_caller_write(security.is_active_group_participant(moment_id)));

DROP POLICY IF EXISTS rls_invoice__select_scope ON finance.invoice;
CREATE POLICY rls_invoice__select_scope ON finance.invoice
FOR SELECT USING (
  security.is_active_company_member(company_id)
  OR security.is_analytics_worker()
  OR security.is_memory_worker()
  OR security.is_projection_worker()
);
DROP POLICY IF EXISTS rls_invoice__backend_write ON finance.invoice;
CREATE POLICY rls_invoice__backend_write ON finance.invoice
FOR ALL
USING (security.finance_caller_write(security.is_active_company_member(company_id)))
WITH CHECK (security.finance_caller_write(security.is_active_company_member(company_id)));

DROP POLICY IF EXISTS rls_invoice_line__select_scope ON finance.invoice_line;
CREATE POLICY rls_invoice_line__select_scope ON finance.invoice_line
FOR SELECT USING (
  EXISTS (
    SELECT 1 FROM finance.invoice i
    WHERE i.invoice_id = invoice_line.invoice_id
      AND security.is_active_company_member(i.company_id)
  )
  OR security.is_analytics_worker()
  OR security.is_memory_worker()
  OR security.is_projection_worker()
);
DROP POLICY IF EXISTS rls_invoice_line__backend_write ON finance.invoice_line;
CREATE POLICY rls_invoice_line__backend_write ON finance.invoice_line
FOR ALL
USING (security.finance_caller_write(EXISTS (
  SELECT 1 FROM finance.invoice i
  WHERE i.invoice_id = invoice_line.invoice_id
    AND security.is_active_company_member(i.company_id)
)))
WITH CHECK (security.finance_caller_write(EXISTS (
  SELECT 1 FROM finance.invoice i
  WHERE i.invoice_id = invoice_line.invoice_id
    AND security.is_active_company_member(i.company_id)
)));

DROP POLICY IF EXISTS rls_invoice_payment__select_scope ON finance.invoice_payment;
CREATE POLICY rls_invoice_payment__select_scope ON finance.invoice_payment
FOR SELECT USING (
  EXISTS (
    SELECT 1 FROM finance.invoice i
    WHERE i.invoice_id = invoice_payment.invoice_id
      AND security.is_active_company_member(i.company_id)
  )
  OR security.is_analytics_worker()
  OR security.is_memory_worker()
  OR security.is_projection_worker()
);
DROP POLICY IF EXISTS rls_invoice_payment__backend_write ON finance.invoice_payment;
CREATE POLICY rls_invoice_payment__backend_write ON finance.invoice_payment
FOR ALL
USING (security.finance_caller_write(EXISTS (
  SELECT 1 FROM finance.invoice i
  WHERE i.invoice_id = invoice_payment.invoice_id
    AND security.is_active_company_member(i.company_id)
)))
WITH CHECK (security.finance_caller_write(EXISTS (
  SELECT 1 FROM finance.invoice i
  WHERE i.invoice_id = invoice_payment.invoice_id
    AND security.is_active_company_member(i.company_id)
)));

DROP POLICY IF EXISTS rls_participant_obligation__select_scope ON finance.participant_obligation;
CREATE POLICY rls_participant_obligation__select_scope ON finance.participant_obligation
FOR SELECT USING (
  security.is_active_group_participant(moment_id)
  OR security.is_analytics_worker()
  OR security.is_memory_worker()
  OR security.is_projection_worker()
);
DROP POLICY IF EXISTS rls_participant_obligation__backend_write ON finance.participant_obligation;
CREATE POLICY rls_participant_obligation__backend_write ON finance.participant_obligation
FOR ALL
USING (security.finance_caller_write(security.is_active_group_participant(moment_id)))
WITH CHECK (security.finance_caller_write(security.is_active_group_participant(moment_id)));

DROP POLICY IF EXISTS rls_personal_expense_context__select_scope ON finance.personal_expense_context;
CREATE POLICY rls_personal_expense_context__select_scope ON finance.personal_expense_context
FOR SELECT USING (
  user_id = security.current_user_id()
  OR security.is_analytics_worker()
  OR security.is_memory_worker()
  OR security.is_projection_worker()
);
DROP POLICY IF EXISTS rls_personal_expense_context__backend_write ON finance.personal_expense_context;
CREATE POLICY rls_personal_expense_context__backend_write ON finance.personal_expense_context
FOR ALL
USING (security.finance_caller_write(user_id = security.current_user_id()))
WITH CHECK (security.finance_caller_write(user_id = security.current_user_id()));

DROP POLICY IF EXISTS rls_revenue__select_scope ON finance.revenue;
CREATE POLICY rls_revenue__select_scope ON finance.revenue
FOR SELECT USING (
  security.is_active_company_member(company_id)
  OR security.is_analytics_worker()
  OR security.is_memory_worker()
  OR security.is_projection_worker()
);
DROP POLICY IF EXISTS rls_revenue__backend_write ON finance.revenue;
CREATE POLICY rls_revenue__backend_write ON finance.revenue
FOR ALL
USING (security.finance_caller_write(security.is_active_company_member(company_id)))
WITH CHECK (security.finance_caller_write(security.is_active_company_member(company_id)));

DROP POLICY IF EXISTS rls_settlement__select_scope ON finance.settlement;
CREATE POLICY rls_settlement__select_scope ON finance.settlement
FOR SELECT USING (
  security.is_active_group_participant(moment_id)
  OR security.is_analytics_worker()
  OR security.is_memory_worker()
  OR security.is_projection_worker()
);
DROP POLICY IF EXISTS rls_settlement__backend_write ON finance.settlement;
CREATE POLICY rls_settlement__backend_write ON finance.settlement
FOR ALL
USING (security.finance_caller_write(security.is_active_group_participant(moment_id)))
WITH CHECK (security.finance_caller_write(security.is_active_group_participant(moment_id)));

DROP POLICY IF EXISTS rls_settlement_allocation__select_scope ON finance.settlement_allocation;
CREATE POLICY rls_settlement_allocation__select_scope ON finance.settlement_allocation
FOR SELECT USING (
  security.is_active_group_participant(moment_id)
  OR security.is_analytics_worker()
  OR security.is_memory_worker()
  OR security.is_projection_worker()
);
DROP POLICY IF EXISTS rls_settlement_allocation__backend_write ON finance.settlement_allocation;
CREATE POLICY rls_settlement_allocation__backend_write ON finance.settlement_allocation
FOR ALL
USING (security.finance_caller_write(security.is_active_group_participant(moment_id)))
WITH CHECK (security.finance_caller_write(security.is_active_group_participant(moment_id)));

-- V083 contribution attachments. Skip when the table is not present yet.
DO $$
BEGIN
  IF to_regclass('finance.contribution_attachment') IS NULL THEN
    RETURN;
  END IF;
  EXECUTE 'DROP POLICY IF EXISTS rls_contribution_attachment__select_participant ON finance.contribution_attachment';
  EXECUTE $pol$
    CREATE POLICY rls_contribution_attachment__select_participant ON finance.contribution_attachment
    FOR SELECT USING (
      EXISTS (
        SELECT 1 FROM finance.contribution c
        WHERE c.contribution_id = contribution_attachment.contribution_id
          AND security.is_active_group_participant(c.moment_id)
      )
      OR security.is_analytics_worker()
      OR security.is_memory_worker()
      OR security.is_projection_worker()
    )
  $pol$;
  EXECUTE 'DROP POLICY IF EXISTS rls_contribution_attachment__backend_write ON finance.contribution_attachment';
  EXECUTE $pol$
    CREATE POLICY rls_contribution_attachment__backend_write ON finance.contribution_attachment
    FOR ALL
    USING (security.finance_caller_write(EXISTS (
      SELECT 1 FROM finance.contribution c
      WHERE c.contribution_id = contribution_attachment.contribution_id
        AND security.is_active_group_participant(c.moment_id)
    )))
    WITH CHECK (security.finance_caller_write(EXISTS (
      SELECT 1 FROM finance.contribution c
      WHERE c.contribution_id = contribution_attachment.contribution_id
        AND security.is_active_group_participant(c.moment_id)
    )))
  $pol$;
END $$;

-- Personal catalogue and owner tables (V045 / V077). Applied only when those tables exist.
DO $$
BEGIN
  IF to_regclass('finance.financial_account_preference') IS NOT NULL THEN
    EXECUTE 'DROP POLICY IF EXISTS rls_financial_account_preference__select_scope ON finance.financial_account_preference';
    EXECUTE $pol$
      CREATE POLICY rls_financial_account_preference__select_scope ON finance.financial_account_preference
      FOR SELECT USING (
        (owner_scope_type = 'USER' AND owner_scope_id = security.current_user_id())
        OR (owner_scope_type = 'COMPANY' AND security.is_active_company_member(owner_scope_id))
        OR security.is_analytics_worker()
        OR security.is_memory_worker()
        OR security.is_projection_worker()
      )
    $pol$;
    EXECUTE 'DROP POLICY IF EXISTS rls_financial_account_preference__backend_write ON finance.financial_account_preference';
    EXECUTE $pol$
      CREATE POLICY rls_financial_account_preference__backend_write ON finance.financial_account_preference
      FOR ALL
      USING (security.finance_caller_write(
        (owner_scope_type = 'USER' AND owner_scope_id = security.current_user_id())
        OR (owner_scope_type = 'COMPANY' AND security.is_active_company_member(owner_scope_id))
      ))
      WITH CHECK (security.finance_caller_write(
        (owner_scope_type = 'USER' AND owner_scope_id = security.current_user_id())
        OR (owner_scope_type = 'COMPANY' AND security.is_active_company_member(owner_scope_id))
      ))
    $pol$;
  END IF;

  IF to_regclass('finance.expense_category') IS NOT NULL THEN
    EXECUTE 'DROP POLICY IF EXISTS rls_expense_category__select_catalogue ON finance.expense_category';
    EXECUTE $pol$
      CREATE POLICY rls_expense_category__select_catalogue ON finance.expense_category
      FOR SELECT USING (
        status = 'ACTIVE'
        OR security.is_projection_worker()
        OR security.is_analytics_worker()
        OR security.is_memory_worker()
      )
    $pol$;
    EXECUTE 'DROP POLICY IF EXISTS rls_expense_category__backend_write ON finance.expense_category';
    EXECUTE $pol$
      CREATE POLICY rls_expense_category__backend_write ON finance.expense_category
      FOR ALL
      USING (security.finance_caller_write(true))
      WITH CHECK (security.finance_caller_write(status = 'ACTIVE' OR status IS NULL))
    $pol$;
  END IF;

  IF to_regclass('finance.expense_subcategory') IS NOT NULL THEN
    EXECUTE 'DROP POLICY IF EXISTS rls_expense_subcategory__select_catalogue ON finance.expense_subcategory';
    EXECUTE $pol$
      CREATE POLICY rls_expense_subcategory__select_catalogue ON finance.expense_subcategory
      FOR SELECT USING (
        status = 'ACTIVE'
        OR security.is_projection_worker()
        OR security.is_analytics_worker()
        OR security.is_memory_worker()
      )
    $pol$;
    EXECUTE 'DROP POLICY IF EXISTS rls_expense_subcategory__backend_write ON finance.expense_subcategory';
    EXECUTE $pol$
      CREATE POLICY rls_expense_subcategory__backend_write ON finance.expense_subcategory
      FOR ALL
      USING (security.finance_caller_write(true))
      WITH CHECK (security.finance_caller_write(status = 'ACTIVE' OR status IS NULL))
    $pol$;
  END IF;

  IF to_regclass('finance.expense_tag') IS NOT NULL THEN
    EXECUTE 'DROP POLICY IF EXISTS rls_expense_tag__select_scope ON finance.expense_tag';
    EXECUTE $pol$
      CREATE POLICY rls_expense_tag__select_scope ON finance.expense_tag
      FOR SELECT USING (
        EXISTS (
          SELECT 1 FROM finance.expense e
          WHERE e.expense_id = expense_tag.expense_id
            AND security.can_access_moment(e.moment_id)
        )
        OR security.is_analytics_worker()
        OR security.is_memory_worker()
        OR security.is_projection_worker()
      )
    $pol$;
    EXECUTE 'DROP POLICY IF EXISTS rls_expense_tag__backend_write ON finance.expense_tag';
    EXECUTE $pol$
      CREATE POLICY rls_expense_tag__backend_write ON finance.expense_tag
      FOR ALL
      USING (security.finance_caller_write(EXISTS (
        SELECT 1 FROM finance.expense e
        WHERE e.expense_id = expense_tag.expense_id
          AND security.can_access_moment(e.moment_id)
      )))
      WITH CHECK (security.finance_caller_write(EXISTS (
        SELECT 1 FROM finance.expense e
        WHERE e.expense_id = expense_tag.expense_id
          AND security.can_access_moment(e.moment_id)
      )))
    $pol$;
  END IF;

  IF to_regclass('finance.recurring_financial_instruction') IS NOT NULL THEN
    EXECUTE 'DROP POLICY IF EXISTS rls_recurring_financial_instruction__select_owner ON finance.recurring_financial_instruction';
    EXECUTE $pol$
      CREATE POLICY rls_recurring_financial_instruction__select_owner ON finance.recurring_financial_instruction
      FOR SELECT USING (
        owner_user_id = security.current_user_id()
        OR security.is_analytics_worker()
        OR security.is_memory_worker()
        OR security.is_projection_worker()
      )
    $pol$;
    EXECUTE 'DROP POLICY IF EXISTS rls_recurring_financial_instruction__backend_write ON finance.recurring_financial_instruction';
    EXECUTE $pol$
      CREATE POLICY rls_recurring_financial_instruction__backend_write ON finance.recurring_financial_instruction
      FOR ALL
      USING (security.finance_caller_write(owner_user_id = security.current_user_id()))
      WITH CHECK (security.finance_caller_write(owner_user_id = security.current_user_id()))
    $pol$;
  END IF;

  IF to_regclass('finance.expense_dimension_contribution') IS NOT NULL THEN
    EXECUTE 'DROP POLICY IF EXISTS rls_expense_dimension_contribution__select_scope ON finance.expense_dimension_contribution';
    EXECUTE $pol$
      CREATE POLICY rls_expense_dimension_contribution__select_scope ON finance.expense_dimension_contribution
      FOR SELECT USING (
        EXISTS (
          SELECT 1 FROM finance.expense e
          WHERE e.expense_id = expense_dimension_contribution.expense_id
            AND security.can_access_moment(e.moment_id)
        )
        OR security.is_analytics_worker()
        OR security.is_memory_worker()
        OR security.is_projection_worker()
      )
    $pol$;
    EXECUTE 'DROP POLICY IF EXISTS rls_expense_dimension_contribution__backend_write ON finance.expense_dimension_contribution';
    EXECUTE $pol$
      CREATE POLICY rls_expense_dimension_contribution__backend_write ON finance.expense_dimension_contribution
      FOR ALL
      USING (security.finance_caller_write(EXISTS (
        SELECT 1 FROM finance.expense e
        WHERE e.expense_id = expense_dimension_contribution.expense_id
          AND security.can_access_moment(e.moment_id)
      )))
      WITH CHECK (security.finance_caller_write(EXISTS (
        SELECT 1 FROM finance.expense e
        WHERE e.expense_id = expense_dimension_contribution.expense_id
          AND security.can_access_moment(e.moment_id)
      )))
    $pol$;
  END IF;
END $$;

REVOKE ALL ON FUNCTION security.finance_caller_write(BOOLEAN) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION security.finance_caller_write(BOOLEAN)
  TO momentra_app, momentra_analytics_worker, momentra_memory_worker, momentra_projection_worker;

COMMIT;
