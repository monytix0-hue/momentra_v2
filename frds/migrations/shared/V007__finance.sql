BEGIN;

CREATE SCHEMA finance;
COMMENT ON SCHEMA finance IS 'Momentra cross-domain Finance: accounts, movements, expenses, budgets, contributions, obligations, settlements, revenue and invoices.';

CREATE TABLE finance.financial_account (
    financial_account_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_scope_type TEXT NOT NULL,
    owner_scope_id UUID NOT NULL,
    owner_user_id UUID,
    owner_company_id UUID,
    account_type TEXT NOT NULL,
    account_name TEXT NOT NULL,
    institution_name TEXT,
    currency_code CHAR(3) NOT NULL,
    opening_balance NUMERIC(19,4) NOT NULL DEFAULT 0,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_financial_account__owner_user
        FOREIGN KEY (owner_user_id) REFERENCES core.user_profile(user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_financial_account__owner_company
        FOREIGN KEY (owner_company_id) REFERENCES business.company(company_id) ON DELETE RESTRICT,
    CONSTRAINT uq_financial_account__id_user UNIQUE (financial_account_id, owner_user_id),
    CONSTRAINT uq_financial_account__id_company UNIQUE (financial_account_id, owner_company_id),
    CONSTRAINT ck_financial_account__owner_scope CHECK (owner_scope_type IN ('USER','COMPANY')),
    CONSTRAINT ck_financial_account__exact_owner CHECK (
        (owner_scope_type = 'USER' AND owner_user_id IS NOT NULL AND owner_company_id IS NULL AND owner_scope_id = owner_user_id)
        OR
        (owner_scope_type = 'COMPANY' AND owner_company_id IS NOT NULL AND owner_user_id IS NULL AND owner_scope_id = owner_company_id)
    ),
    CONSTRAINT ck_financial_account__type CHECK (account_type IN ('CASH','BANK','CARD','WALLET','INVESTMENT','LOAN','OTHER')),
    CONSTRAINT ck_financial_account__currency CHECK (currency_code ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_financial_account__status CHECK (status IN ('ACTIVE','INACTIVE','CLOSED','ARCHIVED')),
    CONSTRAINT ck_financial_account__version CHECK (version > 0)
);

CREATE TABLE finance.expense (
    expense_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    moment_id UUID NOT NULL,
    domain_code TEXT NOT NULL,
    financial_account_id UUID,
    created_by_user_id UUID NOT NULL,
    merchant_name TEXT,
    description TEXT,
    category_code TEXT,
    amount NUMERIC(19,4) NOT NULL,
    currency_code CHAR(3) NOT NULL,
    effective_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    status TEXT NOT NULL DEFAULT 'DRAFT',
    posted_at TIMESTAMPTZ,
    reversed_at TIMESTAMPTZ,
    reversed_expense_id UUID,
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_expense__moment_domain
        FOREIGN KEY (moment_id, domain_code) REFERENCES core.moment(moment_id, domain_code) ON DELETE RESTRICT,
    CONSTRAINT fk_expense__account
        FOREIGN KEY (financial_account_id) REFERENCES finance.financial_account(financial_account_id) ON DELETE RESTRICT,
    CONSTRAINT fk_expense__created_by
        FOREIGN KEY (created_by_user_id) REFERENCES core.user_profile(user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_expense__reversed_expense
        FOREIGN KEY (reversed_expense_id) REFERENCES finance.expense(expense_id) ON DELETE RESTRICT,
    CONSTRAINT uq_expense__id_moment_domain UNIQUE (expense_id, moment_id, domain_code),
    CONSTRAINT ck_expense__domain CHECK (domain_code IN ('PERSONAL','GROUP','BUSINESS')),
    CONSTRAINT ck_expense__amount CHECK (amount > 0),
    CONSTRAINT ck_expense__currency CHECK (currency_code ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_expense__status CHECK (status IN ('DRAFT','POSTED','VOIDED','REVERSED')),
    CONSTRAINT ck_expense__posted CHECK (status NOT IN ('POSTED','REVERSED') OR posted_at IS NOT NULL),
    CONSTRAINT ck_expense__reversed CHECK (status <> 'REVERSED' OR reversed_at IS NOT NULL),
    CONSTRAINT ck_expense__version CHECK (version > 0)
);

CREATE TABLE finance.personal_expense_context (
    expense_id UUID PRIMARY KEY,
    moment_id UUID NOT NULL,
    domain_code TEXT NOT NULL DEFAULT 'PERSONAL',
    user_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_personal_expense_context__expense
        FOREIGN KEY (expense_id, moment_id, domain_code) REFERENCES finance.expense(expense_id, moment_id, domain_code) ON DELETE RESTRICT,
    CONSTRAINT fk_personal_expense_context__personal_moment
        FOREIGN KEY (moment_id, user_id) REFERENCES personal.personal_moment_context(moment_id, user_id) ON DELETE RESTRICT,
    CONSTRAINT ck_personal_expense_context__domain CHECK (domain_code = 'PERSONAL')
);

CREATE TABLE finance.group_expense_context (
    expense_id UUID PRIMARY KEY,
    moment_id UUID NOT NULL,
    domain_code TEXT NOT NULL DEFAULT 'GROUP',
    paid_by_participant_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_group_expense_context__expense
        FOREIGN KEY (expense_id, moment_id, domain_code) REFERENCES finance.expense(expense_id, moment_id, domain_code) ON DELETE RESTRICT,
    CONSTRAINT fk_group_expense_context__group_moment
        FOREIGN KEY (moment_id) REFERENCES collaboration.group_moment_context(moment_id) ON DELETE RESTRICT,
    CONSTRAINT fk_group_expense_context__payer_moment
        FOREIGN KEY (paid_by_participant_id, moment_id) REFERENCES collaboration.moment_participant(participant_id, moment_id) ON DELETE RESTRICT,
    CONSTRAINT uq_group_expense_context__expense_moment UNIQUE (expense_id, moment_id),
    CONSTRAINT ck_group_expense_context__domain CHECK (domain_code = 'GROUP')
);

CREATE TABLE finance.business_expense_context (
    expense_id UUID PRIMARY KEY,
    moment_id UUID NOT NULL,
    domain_code TEXT NOT NULL DEFAULT 'BUSINESS',
    company_id UUID NOT NULL,
    vendor_id UUID,
    vendor_contract_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_business_expense_context__expense
        FOREIGN KEY (expense_id, moment_id, domain_code) REFERENCES finance.expense(expense_id, moment_id, domain_code) ON DELETE RESTRICT,
    CONSTRAINT fk_business_expense_context__business_moment
        FOREIGN KEY (moment_id, company_id) REFERENCES business.business_moment_context(moment_id, company_id) ON DELETE RESTRICT,
    CONSTRAINT fk_business_expense_context__vendor_company
        FOREIGN KEY (vendor_id, company_id) REFERENCES business.vendor(vendor_id, company_id) ON DELETE RESTRICT,
    CONSTRAINT fk_business_expense_context__contract_vendor_company
        FOREIGN KEY (vendor_contract_id, company_id, vendor_id) REFERENCES business.vendor_contract(vendor_contract_id, company_id, vendor_id) ON DELETE RESTRICT,
    CONSTRAINT uq_business_expense_context__expense_company UNIQUE (expense_id, company_id),
    CONSTRAINT ck_business_expense_context__domain CHECK (domain_code = 'BUSINESS'),
    CONSTRAINT ck_business_expense_context__contract_requires_vendor CHECK (vendor_contract_id IS NULL OR vendor_id IS NOT NULL)
);

CREATE TABLE finance.expense_split (
    expense_split_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    expense_id UUID NOT NULL,
    split_type TEXT NOT NULL,
    label TEXT,
    amount NUMERIC(19,4) NOT NULL,
    percentage NUMERIC(9,6),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_expense_split__expense FOREIGN KEY (expense_id) REFERENCES finance.expense(expense_id) ON DELETE RESTRICT,
    CONSTRAINT ck_expense_split__type CHECK (split_type IN ('CATEGORY','ITEM','TAX','TIP','OTHER')),
    CONSTRAINT ck_expense_split__amount CHECK (amount >= 0),
    CONSTRAINT ck_expense_split__percentage CHECK (percentage IS NULL OR (percentage >= 0 AND percentage <= 100))
);

CREATE TABLE finance.expense_share (
    expense_share_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    expense_id UUID NOT NULL,
    moment_id UUID NOT NULL,
    participant_id UUID NOT NULL,
    share_amount NUMERIC(19,4) NOT NULL,
    share_percent NUMERIC(9,6),
    status TEXT NOT NULL DEFAULT 'ALLOCATED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_expense_share__group_expense
        FOREIGN KEY (expense_id, moment_id) REFERENCES finance.group_expense_context(expense_id, moment_id) ON DELETE RESTRICT,
    CONSTRAINT fk_expense_share__participant_moment
        FOREIGN KEY (participant_id, moment_id) REFERENCES collaboration.moment_participant(participant_id, moment_id) ON DELETE RESTRICT,
    CONSTRAINT uq_expense_share__expense_participant UNIQUE (expense_id, participant_id),
    CONSTRAINT ck_expense_share__amount CHECK (share_amount >= 0),
    CONSTRAINT ck_expense_share__percent CHECK (share_percent IS NULL OR (share_percent >= 0 AND share_percent <= 100)),
    CONSTRAINT ck_expense_share__status CHECK (status IN ('ALLOCATED','WAIVED','VOIDED'))
);

CREATE TABLE finance.expense_resource_link (
    expense_resource_link_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    expense_id UUID NOT NULL,
    resource_type TEXT NOT NULL,
    resource_id UUID NOT NULL,
    relation_type TEXT NOT NULL DEFAULT 'RELATED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_expense_resource_link__expense FOREIGN KEY (expense_id) REFERENCES finance.expense(expense_id) ON DELETE RESTRICT,
    CONSTRAINT uq_expense_resource_link__resource UNIQUE (expense_id, resource_type, resource_id, relation_type),
    CONSTRAINT ck_expense_resource_link__resource_type CHECK (resource_type IN ('BOOKING','PURCHASE_ITEM','TASK','VENDOR','INVOICE','ASSET','OTHER')),
    CONSTRAINT ck_expense_resource_link__relation CHECK (relation_type IN ('RELATED','FUNDS','REIMBURSES','SETTLES','OTHER'))
);

CREATE TABLE finance.budget (
    budget_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scope_type TEXT NOT NULL,
    scope_id UUID NOT NULL,
    moment_id UUID,
    owner_user_id UUID,
    company_id UUID,
    name TEXT NOT NULL,
    currency_code CHAR(3) NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    period_start DATE,
    period_end DATE,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_budget__moment FOREIGN KEY (moment_id) REFERENCES core.moment(moment_id) ON DELETE RESTRICT,
    CONSTRAINT fk_budget__user FOREIGN KEY (owner_user_id) REFERENCES core.user_profile(user_id) ON DELETE RESTRICT,
    CONSTRAINT fk_budget__company FOREIGN KEY (company_id) REFERENCES business.company(company_id) ON DELETE RESTRICT,
    CONSTRAINT ck_budget__scope CHECK (scope_type IN ('USER','MOMENT','COMPANY')),
    CONSTRAINT ck_budget__scope_anchor CHECK (
        (scope_type='USER' AND owner_user_id IS NOT NULL AND moment_id IS NULL AND company_id IS NULL AND scope_id=owner_user_id)
        OR (scope_type='MOMENT' AND moment_id IS NOT NULL AND owner_user_id IS NULL AND company_id IS NULL AND scope_id=moment_id)
        OR (scope_type='COMPANY' AND company_id IS NOT NULL AND owner_user_id IS NULL AND moment_id IS NULL AND scope_id=company_id)
    ),
    CONSTRAINT ck_budget__currency CHECK (currency_code ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_budget__amount CHECK (amount >= 0),
    CONSTRAINT ck_budget__period CHECK (period_end IS NULL OR period_start IS NULL OR period_end >= period_start),
    CONSTRAINT ck_budget__status CHECK (status IN ('DRAFT','ACTIVE','CLOSED','ARCHIVED')),
    CONSTRAINT ck_budget__version CHECK (version > 0)
);

CREATE TABLE finance.budget_revision (
    budget_revision_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    budget_id UUID NOT NULL,
    revision_number INTEGER NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    reason TEXT,
    effective_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by_user_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_budget_revision__budget FOREIGN KEY (budget_id) REFERENCES finance.budget(budget_id) ON DELETE RESTRICT,
    CONSTRAINT fk_budget_revision__created_by FOREIGN KEY (created_by_user_id) REFERENCES core.user_profile(user_id) ON DELETE RESTRICT,
    CONSTRAINT uq_budget_revision__number UNIQUE (budget_id, revision_number),
    CONSTRAINT ck_budget_revision__number CHECK (revision_number > 0),
    CONSTRAINT ck_budget_revision__amount CHECK (amount >= 0)
);

CREATE TABLE finance.contribution (
    contribution_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    moment_id UUID NOT NULL,
    participant_id UUID NOT NULL,
    financial_account_id UUID,
    amount NUMERIC(19,4) NOT NULL,
    currency_code CHAR(3) NOT NULL,
    contributed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    status TEXT NOT NULL DEFAULT 'RECORDED',
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_contribution__participant_moment FOREIGN KEY (participant_id, moment_id) REFERENCES collaboration.moment_participant(participant_id, moment_id) ON DELETE RESTRICT,
    CONSTRAINT fk_contribution__account FOREIGN KEY (financial_account_id) REFERENCES finance.financial_account(financial_account_id) ON DELETE RESTRICT,
    CONSTRAINT uq_contribution__id_moment UNIQUE (contribution_id, moment_id),
    CONSTRAINT ck_contribution__amount CHECK (amount > 0),
    CONSTRAINT ck_contribution__currency CHECK (currency_code ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_contribution__status CHECK (status IN ('RECORDED','REVERSED','VOIDED')),
    CONSTRAINT ck_contribution__version CHECK (version > 0)
);

CREATE TABLE finance.participant_obligation (
    participant_obligation_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    moment_id UUID NOT NULL,
    participant_id UUID NOT NULL,
    source_type TEXT NOT NULL,
    source_id UUID NOT NULL,
    currency_code CHAR(3) NOT NULL,
    original_amount NUMERIC(19,4) NOT NULL,
    settled_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    status TEXT NOT NULL DEFAULT 'OPEN',
    due_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_participant_obligation__participant_moment FOREIGN KEY (participant_id, moment_id) REFERENCES collaboration.moment_participant(participant_id, moment_id) ON DELETE RESTRICT,
    CONSTRAINT uq_participant_obligation__id_moment UNIQUE (participant_obligation_id, moment_id),
    CONSTRAINT ck_participant_obligation__source CHECK (source_type IN ('EXPENSE_SHARE','CONTRIBUTION','ADJUSTMENT','OTHER')),
    CONSTRAINT ck_participant_obligation__currency CHECK (currency_code ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_participant_obligation__amounts CHECK (original_amount >= 0 AND settled_amount >= 0 AND settled_amount <= original_amount),
    CONSTRAINT ck_participant_obligation__status CHECK (status IN ('OPEN','PARTIALLY_SETTLED','SETTLED','WAIVED','VOIDED')),
    CONSTRAINT ck_participant_obligation__version CHECK (version > 0)
);

CREATE TABLE finance.settlement (
    settlement_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    moment_id UUID NOT NULL,
    payer_participant_id UUID NOT NULL,
    payee_participant_id UUID NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    currency_code CHAR(3) NOT NULL,
    settled_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    status TEXT NOT NULL DEFAULT 'POSTED',
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_settlement__payer_moment FOREIGN KEY (payer_participant_id, moment_id) REFERENCES collaboration.moment_participant(participant_id, moment_id) ON DELETE RESTRICT,
    CONSTRAINT fk_settlement__payee_moment FOREIGN KEY (payee_participant_id, moment_id) REFERENCES collaboration.moment_participant(participant_id, moment_id) ON DELETE RESTRICT,
    CONSTRAINT uq_settlement__id_moment UNIQUE (settlement_id, moment_id),
    CONSTRAINT ck_settlement__different_parties CHECK (payer_participant_id <> payee_participant_id),
    CONSTRAINT ck_settlement__amount CHECK (amount > 0),
    CONSTRAINT ck_settlement__currency CHECK (currency_code ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_settlement__status CHECK (status IN ('DRAFT','POSTED','REVERSED','VOIDED')),
    CONSTRAINT ck_settlement__version CHECK (version > 0)
);

CREATE TABLE finance.settlement_allocation (
    settlement_allocation_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    settlement_id UUID NOT NULL,
    moment_id UUID NOT NULL,
    participant_obligation_id UUID NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_settlement_allocation__settlement_moment FOREIGN KEY (settlement_id, moment_id) REFERENCES finance.settlement(settlement_id, moment_id) ON DELETE RESTRICT,
    CONSTRAINT fk_settlement_allocation__obligation_moment FOREIGN KEY (participant_obligation_id, moment_id) REFERENCES finance.participant_obligation(participant_obligation_id, moment_id) ON DELETE RESTRICT,
    CONSTRAINT uq_settlement_allocation__pair UNIQUE (settlement_id, participant_obligation_id),
    CONSTRAINT ck_settlement_allocation__amount CHECK (amount > 0)
);

CREATE TABLE finance.revenue (
    revenue_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    moment_id UUID,
    financial_account_id UUID,
    description TEXT,
    category_code TEXT,
    amount NUMERIC(19,4) NOT NULL,
    currency_code CHAR(3) NOT NULL,
    effective_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    status TEXT NOT NULL DEFAULT 'POSTED',
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_revenue__company FOREIGN KEY (company_id) REFERENCES business.company(company_id) ON DELETE RESTRICT,
    CONSTRAINT fk_revenue__business_moment FOREIGN KEY (moment_id, company_id) REFERENCES business.business_moment_context(moment_id, company_id) ON DELETE RESTRICT,
    CONSTRAINT fk_revenue__company_account FOREIGN KEY (financial_account_id, company_id) REFERENCES finance.financial_account(financial_account_id, owner_company_id) ON DELETE RESTRICT,
    CONSTRAINT ck_revenue__amount CHECK (amount > 0),
    CONSTRAINT ck_revenue__currency CHECK (currency_code ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_revenue__status CHECK (status IN ('DRAFT','POSTED','REVERSED','VOIDED')),
    CONSTRAINT ck_revenue__version CHECK (version > 0)
);

CREATE TABLE finance.invoice (
    invoice_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    moment_id UUID,
    customer_external_party_id UUID,
    invoice_number TEXT NOT NULL,
    invoice_date DATE NOT NULL,
    due_date DATE,
    currency_code CHAR(3) NOT NULL,
    subtotal_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    tax_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    total_amount NUMERIC(19,4) NOT NULL,
    paid_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    status TEXT NOT NULL DEFAULT 'DRAFT',
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_invoice__company FOREIGN KEY (company_id) REFERENCES business.company(company_id) ON DELETE RESTRICT,
    CONSTRAINT fk_invoice__business_moment FOREIGN KEY (moment_id, company_id) REFERENCES business.business_moment_context(moment_id, company_id) ON DELETE RESTRICT,
    CONSTRAINT fk_invoice__customer FOREIGN KEY (customer_external_party_id) REFERENCES core.external_party(external_party_id) ON DELETE RESTRICT,
    CONSTRAINT uq_invoice__company_number UNIQUE (company_id, invoice_number),
    CONSTRAINT ck_invoice__dates CHECK (due_date IS NULL OR due_date >= invoice_date),
    CONSTRAINT ck_invoice__currency CHECK (currency_code ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_invoice__amounts CHECK (subtotal_amount >= 0 AND tax_amount >= 0 AND total_amount >= 0 AND paid_amount >= 0 AND paid_amount <= total_amount),
    CONSTRAINT ck_invoice__status CHECK (status IN ('DRAFT','ISSUED','PARTIALLY_PAID','PAID','OVERDUE','VOIDED','CANCELLED')),
    CONSTRAINT ck_invoice__version CHECK (version > 0)
);

CREATE TABLE finance.invoice_line (
    invoice_line_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL,
    line_number INTEGER NOT NULL,
    description TEXT NOT NULL,
    quantity NUMERIC(19,4) NOT NULL DEFAULT 1,
    unit_price NUMERIC(19,4) NOT NULL,
    tax_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    line_total NUMERIC(19,4) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_invoice_line__invoice FOREIGN KEY (invoice_id) REFERENCES finance.invoice(invoice_id) ON DELETE RESTRICT,
    CONSTRAINT uq_invoice_line__number UNIQUE (invoice_id, line_number),
    CONSTRAINT ck_invoice_line__number CHECK (line_number > 0),
    CONSTRAINT ck_invoice_line__amounts CHECK (quantity > 0 AND unit_price >= 0 AND tax_amount >= 0 AND line_total >= 0)
);

CREATE TABLE finance.invoice_payment (
    invoice_payment_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_id UUID NOT NULL,
    financial_account_id UUID,
    amount NUMERIC(19,4) NOT NULL,
    currency_code CHAR(3) NOT NULL,
    paid_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    status TEXT NOT NULL DEFAULT 'POSTED',
    reference_text TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_invoice_payment__invoice FOREIGN KEY (invoice_id) REFERENCES finance.invoice(invoice_id) ON DELETE RESTRICT,
    CONSTRAINT fk_invoice_payment__account FOREIGN KEY (financial_account_id) REFERENCES finance.financial_account(financial_account_id) ON DELETE RESTRICT,
    CONSTRAINT ck_invoice_payment__amount CHECK (amount > 0),
    CONSTRAINT ck_invoice_payment__currency CHECK (currency_code ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_invoice_payment__status CHECK (status IN ('POSTED','REVERSED','VOIDED'))
);

CREATE TABLE finance.financial_movement (
    financial_movement_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    financial_account_id UUID NOT NULL,
    movement_type TEXT NOT NULL,
    direction TEXT NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    currency_code CHAR(3) NOT NULL,
    effective_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    status TEXT NOT NULL DEFAULT 'POSTED',
    source_type TEXT,
    source_id UUID,
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_financial_movement__account FOREIGN KEY (financial_account_id) REFERENCES finance.financial_account(financial_account_id) ON DELETE RESTRICT,
    CONSTRAINT ck_financial_movement__type CHECK (movement_type IN ('EXPENSE','REVENUE','CONTRIBUTION','SETTLEMENT','INVOICE_PAYMENT','TRANSFER','ADJUSTMENT','OTHER')),
    CONSTRAINT ck_financial_movement__direction CHECK (direction IN ('DEBIT','CREDIT')),
    CONSTRAINT ck_financial_movement__amount CHECK (amount > 0),
    CONSTRAINT ck_financial_movement__currency CHECK (currency_code ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_financial_movement__status CHECK (status IN ('PENDING','POSTED','REVERSED','VOIDED')),
    CONSTRAINT ck_financial_movement__version CHECK (version > 0)
);

CREATE TABLE finance.financial_movement_link (
    financial_movement_link_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    financial_movement_id UUID NOT NULL,
    linked_movement_id UUID,
    resource_type TEXT,
    resource_id UUID,
    relation_type TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_financial_movement_link__movement FOREIGN KEY (financial_movement_id) REFERENCES finance.financial_movement(financial_movement_id) ON DELETE RESTRICT,
    CONSTRAINT fk_financial_movement_link__linked FOREIGN KEY (linked_movement_id) REFERENCES finance.financial_movement(financial_movement_id) ON DELETE RESTRICT,
    CONSTRAINT ck_financial_movement_link__target CHECK (linked_movement_id IS NOT NULL OR (resource_type IS NOT NULL AND resource_id IS NOT NULL)),
    CONSTRAINT ck_financial_movement_link__relation CHECK (relation_type IN ('REVERSAL_OF','TRANSFER_PAIR','SOURCE_OF','SETTLES','PAYS','OTHER'))
);

CREATE INDEX ix_financial_account__user_status ON finance.financial_account (owner_user_id, status) WHERE owner_user_id IS NOT NULL;
CREATE INDEX ix_financial_account__company_status ON finance.financial_account (owner_company_id, status) WHERE owner_company_id IS NOT NULL;
CREATE INDEX ix_expense__moment_time ON finance.expense (moment_id, effective_at DESC);
CREATE INDEX ix_expense__account_time ON finance.expense (financial_account_id, effective_at DESC) WHERE financial_account_id IS NOT NULL;
CREATE INDEX ix_expense__domain_status_time ON finance.expense (domain_code, status, effective_at DESC);
CREATE INDEX ix_personal_expense_context__user ON finance.personal_expense_context (user_id, moment_id);
CREATE INDEX ix_group_expense_context__moment ON finance.group_expense_context (moment_id, paid_by_participant_id);
CREATE INDEX ix_business_expense_context__company ON finance.business_expense_context (company_id, moment_id, vendor_id);
CREATE INDEX ix_expense_share__participant ON finance.expense_share (moment_id, participant_id, status);
CREATE INDEX ix_budget__scope_status ON finance.budget (scope_type, scope_id, status);
CREATE INDEX ix_contribution__moment_participant_time ON finance.contribution (moment_id, participant_id, contributed_at DESC);
CREATE INDEX ix_obligation__moment_participant_status ON finance.participant_obligation (moment_id, participant_id, status, due_at);
CREATE INDEX ix_settlement__moment_time ON finance.settlement (moment_id, settled_at DESC);
CREATE INDEX ix_revenue__company_time ON finance.revenue (company_id, effective_at DESC);
CREATE INDEX ix_invoice__company_status_due ON finance.invoice (company_id, status, due_date);
CREATE INDEX ix_invoice_payment__invoice_time ON finance.invoice_payment (invoice_id, paid_at DESC);
CREATE INDEX ix_financial_movement__account_time ON finance.financial_movement (financial_account_id, effective_at DESC);

COMMIT;
