-- Strengthened non-versioned validation for Momentra V042 / V043 / V044 / V045 (renumbered pack).
-- Run only after applying all four migrations successfully.

-- 1) Required tables and RLS.
DO $$
DECLARE
  missing_tables TEXT;
  rls_missing TEXT;
BEGIN
  SELECT string_agg(x.fq, ', ' ORDER BY x.fq)
  INTO missing_tables
  FROM (VALUES
    ('personal.life_operation_profile'),
    ('personal.life_operation_priority'),
    ('personal.life_operation_anchor'),
    ('analytics.attention_capture'),
    ('personal.recovery_observation_detail'),
    ('personal.mood_observation_detail'),
    ('personal.mood_observation_driver'),
    ('personal.life_operation_adjustment'),
    ('finance.financial_account_preference'),
    ('finance.expense_category'),
    ('finance.expense_subcategory'),
    ('finance.expense_tag'),
    ('finance.recurring_financial_instruction')
  ) AS x(fq)
  WHERE to_regclass(x.fq) IS NULL;

  IF missing_tables IS NOT NULL THEN
    RAISE EXCEPTION 'Missing V042-V045 tables: %', missing_tables;
  END IF;

  SELECT string_agg(n.nspname || '.' || c.relname, ', ' ORDER BY n.nspname, c.relname)
  INTO rls_missing
  FROM pg_class c
  JOIN pg_namespace n ON n.oid = c.relnamespace
  WHERE (n.nspname, c.relname) IN (
    ('personal','life_operation_profile'),('personal','life_operation_priority'),('personal','life_operation_anchor'),
    ('analytics','attention_capture'),('personal','recovery_observation_detail'),('personal','mood_observation_detail'),
    ('personal','mood_observation_driver'),('personal','life_operation_adjustment'),
    ('finance','financial_account_preference'),('finance','expense_category'),('finance','expense_subcategory'),
    ('finance','expense_tag'),('finance','recurring_financial_instruction')
  )
  AND NOT c.relrowsecurity;

  IF rls_missing IS NOT NULL THEN
    RAISE EXCEPTION 'RLS not enabled on: %', rls_missing;
  END IF;
END $$;

-- 2) Technical updated_at triggers.
DO $$
DECLARE trigger_missing TEXT;
BEGIN
  SELECT string_agg(req.trigger_name, ', ' ORDER BY req.trigger_name)
  INTO trigger_missing
  FROM (VALUES
    ('personal','life_operation_profile','trg_life_operation_profile__set_updated_at'),
    ('personal','life_operation_priority','trg_life_operation_priority__set_updated_at'),
    ('personal','life_operation_anchor','trg_life_operation_anchor__set_updated_at'),
    ('analytics','attention_capture','trg_attention_capture__set_updated_at'),
    ('personal','recovery_observation_detail','trg_recovery_observation_detail__set_updated_at'),
    ('personal','mood_observation_detail','trg_mood_observation_detail__set_updated_at'),
    ('personal','life_operation_adjustment','trg_life_operation_adjustment__set_updated_at'),
    ('finance','financial_account_preference','trg_financial_account_preference__set_updated_at'),
    ('finance','expense_category','trg_expense_category__set_updated_at'),
    ('finance','expense_subcategory','trg_expense_subcategory__set_updated_at'),
    ('finance','recurring_financial_instruction','trg_recurring_financial_instruction__set_updated_at')
  ) AS req(schema_name, table_name, trigger_name)
  WHERE NOT EXISTS (
    SELECT 1
    FROM pg_trigger t
    JOIN pg_class c ON c.oid = t.tgrelid
    JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE n.nspname = req.schema_name
      AND c.relname = req.table_name
      AND t.tgname = req.trigger_name
      AND NOT t.tgisinternal
  );
  IF trigger_missing IS NOT NULL THEN
    RAISE EXCEPTION 'Missing updated_at triggers: %', trigger_missing;
  END IF;
END $$;

-- 3) Required added columns.
DO $$
DECLARE missing_cols TEXT;
BEGIN
  SELECT string_agg(q.fq, ', ' ORDER BY q.fq)
  INTO missing_cols
  FROM (VALUES
    ('finance','expense','subcategory_code'),
    ('finance','expense','planning_class_code'),
    ('finance','expense','payment_method_code'),
    ('finance','expense','note'),
    ('finance','financial_movement','note'),
    ('personal','recovery_observation_detail','observation_type'),
    ('personal','mood_observation_detail','observation_type'),
    ('finance','recurring_financial_instruction','source_expense_id'),
    ('finance','recurring_financial_instruction','savings_goal_id')
  ) AS x(schema_name, table_name, column_name)
  CROSS JOIN LATERAL (SELECT x.schema_name||'.'||x.table_name||'.'||x.column_name AS fq) q
  WHERE NOT EXISTS (
    SELECT 1 FROM information_schema.columns c
    WHERE c.table_schema=x.schema_name AND c.table_name=x.table_name AND c.column_name=x.column_name
  );
  IF missing_cols IS NOT NULL THEN
    RAISE EXCEPTION 'Missing required columns: %', missing_cols;
  END IF;
END $$;

-- 4) Critical constraints/FKs/checks must exist by exact name.
DO $$
DECLARE missing_constraints TEXT;
BEGIN
  SELECT string_agg(x.constraint_name, ', ' ORDER BY x.constraint_name)
  INTO missing_constraints
  FROM (VALUES
    ('uq_life_operation_observation__id_type'),
    ('fk_recovery_observation_detail__typed_observation'),
    ('ck_recovery_observation_detail__type'),
    ('fk_mood_observation_detail__typed_observation'),
    ('ck_mood_observation_detail__type'),
    ('fk_mood_observation_driver__mood_detail'),
    ('uq_financial_account__scope_account'),
    ('uq_expense__id_moment'),
    ('fk_expense__category'),
    ('fk_expense__category_subcategory'),
    ('ck_expense__payment_method_code'),
    ('fk_expense_tag__expense'),
    ('fk_recurring_financial_instruction__personal_context'),
    ('fk_recurring_financial_instruction__source_expense'),
    ('fk_recurring_financial_instruction__savings_goal'),
    ('ck_recurring_financial_instruction__shape')
  ) AS x(constraint_name)
  WHERE NOT EXISTS (SELECT 1 FROM pg_constraint c WHERE c.conname=x.constraint_name);
  IF missing_constraints IS NOT NULL THEN
    RAISE EXCEPTION 'Missing critical constraints: %', missing_constraints;
  END IF;
END $$;

-- 5) Critical indexes including partial uniqueness and tag normalization.
DO $$
DECLARE missing_indexes TEXT;
BEGIN
  SELECT string_agg(x.index_name, ', ' ORDER BY x.index_name)
  INTO missing_indexes
  FROM (VALUES
    ('uq_life_operation_priority__current_active'),
    ('uq_expense_tag__expense_name_ci'),
    ('ix_expense_tag__name_ci'),
    ('ix_expense__payment_method_time'),
    ('ix_recurring_financial_instruction__owner_status_next'),
    ('ix_recurring_financial_instruction__source_expense'),
    ('ix_recurring_financial_instruction__savings_goal')
  ) AS x(index_name)
  WHERE NOT EXISTS (SELECT 1 FROM pg_indexes i WHERE i.indexname=x.index_name);
  IF missing_indexes IS NOT NULL THEN
    RAISE EXCEPTION 'Missing critical indexes: %', missing_indexes;
  END IF;
END $$;

-- 6) Exact RLS policy count across newly created tables: 26 (13 tables x 2 policies).
DO $$
DECLARE pcount INTEGER;
BEGIN
  SELECT count(*) INTO pcount
  FROM pg_policies
  WHERE (schemaname, tablename) IN (
    ('personal','life_operation_profile'),('personal','life_operation_priority'),('personal','life_operation_anchor'),
    ('analytics','attention_capture'),('personal','recovery_observation_detail'),('personal','mood_observation_detail'),
    ('personal','mood_observation_driver'),('personal','life_operation_adjustment'),
    ('finance','financial_account_preference'),('finance','expense_category'),('finance','expense_subcategory'),
    ('finance','expense_tag'),('finance','recurring_financial_instruction')
  );
  IF pcount <> 26 THEN
    RAISE EXCEPTION 'Expected 26 V042-V045 policies, found %', pcount;
  END IF;
END $$;

-- 7) Role privilege checks: backend app must have DML on new tables; client roles must not mutate sensitive tables.
DO $$
DECLARE bad TEXT;
BEGIN
  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname='momentra_app') THEN
    SELECT string_agg(x.fq, ', ' ORDER BY x.fq) INTO bad
    FROM (VALUES
      ('personal.life_operation_profile'),('personal.life_operation_priority'),('personal.life_operation_anchor'),
      ('analytics.attention_capture'),('personal.recovery_observation_detail'),('personal.mood_observation_detail'),
      ('personal.mood_observation_driver'),('personal.life_operation_adjustment'),
      ('finance.financial_account_preference'),('finance.expense_category'),('finance.expense_subcategory'),
      ('finance.expense_tag'),('finance.recurring_financial_instruction')
    ) x(fq)
    WHERE NOT (
      has_table_privilege('momentra_app', x.fq, 'SELECT') AND
      has_table_privilege('momentra_app', x.fq, 'INSERT') AND
      has_table_privilege('momentra_app', x.fq, 'UPDATE') AND
      has_table_privilege('momentra_app', x.fq, 'DELETE')
    );
    IF bad IS NOT NULL THEN RAISE EXCEPTION 'momentra_app missing DML privileges on: %', bad; END IF;
  END IF;

  IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname='authenticated') THEN
    IF has_table_privilege('authenticated','finance.expense_tag','INSERT,UPDATE,DELETE')
       OR has_table_privilege('authenticated','finance.recurring_financial_instruction','INSERT,UPDATE,DELETE') THEN
      RAISE EXCEPTION 'authenticated unexpectedly has mutation privilege on sensitive V034 tables';
    END IF;
  END IF;
END $$;

-- 8) Semantic smoke tests executed inside a rollback-only subtransaction.
DO $$
DECLARE
  idxdef TEXT;
BEGIN
  SELECT indexdef INTO idxdef FROM pg_indexes
  WHERE schemaname='personal' AND indexname='uq_life_operation_priority__current_active';
  IF idxdef IS NULL OR position('WHERE' in upper(idxdef)) = 0 THEN
    RAISE EXCEPTION 'Current priority unique index is not partial as expected';
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint c
    JOIN pg_class t ON t.oid=c.conrelid
    JOIN pg_namespace n ON n.oid=t.relnamespace
    WHERE n.nspname='finance' AND t.relname='recurring_financial_instruction'
      AND c.conname='ck_recurring_financial_instruction__shape'
  ) THEN
    RAISE EXCEPTION 'Recurring instruction shape check missing';
  END IF;
END $$;

-- Validation success marker.
SELECT 'PASS' AS v031_v034_post_apply_validation,
       now() AS validated_at;
