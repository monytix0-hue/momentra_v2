# Business Authz Isolation Gaps Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close Business company-moment authorization gaps so company-bound writes require active `company_membership`, observers cannot mutate ops paths, leave/transfer is race-safe, share-links fail closed, and invite `operatingRole` is applied on redeem.

**Architecture:** Keep a single SQL truth in `assertCompanyMomentAccess` (company-bound → active membership only; company-less → creator or active `moment_participant`). Align `authorize()` BUSINESS branch and `security.can_access_moment` with that rule. Mirror Group leave locking on `leaveCompany` / `transferCompanyOwnership`, and reuse `assertNotCompanyObserver` + an expanded `OBSERVER_DENIED_ACTIONS` set instead of a new `canMutate` framework.

**Tech Stack:** TypeScript (Node/Express), `pg` + `PoolClient`, Zod, `node:test` + `tsx --test` + supertest, Postgres RLS (`frds/migrations/business`, `frds/migrations/shared`).

**Spec:** No formal product spec. Source of truth: this plan’s **Review Focus** + [`docs/architecture/BUSINESS_BACKEND.md`](../../architecture/BUSINESS_BACKEND.md) (Access section) + the code-review findings listed under Global Constraints.

## Global Constraints

- Company-bound Business moments (`business.business_moment_context.company_id IS NOT NULL`) require **active** `business.company_membership` for access and writes — creator and bare `moment_participant` must not bypass.
- Company-less Business moments (V095) remain reachable by creator or active `collaboration.moment_participant`; finance/khata/share still call `requireCompanyBoundMoment` (409).
- Do **not** invent a generic `canMutate` framework; extend `assertCompanyMomentAccess`, `assertNotCompanyObserver`, and `OBSERVER_DENIED_ACTIONS`.
- Mirror Group leave patterns in `backend/typescript/src/modules/collaboration/group-membership.ts` (`FOR UPDATE` on context + membership rows).
- Expense membership JOIN in `prepareBusinessExpenseGate` (`business-finance.ts`) is the write-gate reference; revenue/invoice/update must match.
- Migrations live under `frds/migrations/`; next Business numbers after `V101` are **`V102`** (operating_role) and **`V103`** (company-less RLS).
- Capability catalog: if `authorize` blocks MEMBER on a new `BUSINESS_UPDATE_CREATE` code, seed capability + moment_type maps inside **V102** (or the update governance call’s migration) — do not ship a write path that always 403s for members.
- Tests use `ALLOW_DEV_AUTH=1`, `tsx --test`, helpers from `business-s4-finance.test.ts` / `platform-foundation.test.ts` / `group-invite.test.ts`.
- Prefer small commits per task; do not change Android/iOS clients in this plan.
- API surface stays under `/v1` via `runCommand`; do not mount `router-product.ts`.

## Review Focus

1. **Former creator left company still writes revenue** — after `POST /v1/companies/:companyId/leave`, `POST .../revenues` on a company-bound moment must be **403** even if `moment_participant` stays ACTIVE until leave cleanup runs. **Pinned:** Task 2 + Task 8 (`left member cannot post revenue`).
2. **Company-less then link-company leaks prior participants** — after `linkBusinessMomentCompany`, non-members who were participants before the link must get **403** on company-bound writes. **Pinned:** Task 1 + Task 8 (`link-company isolates prior participants`).
3. **Company OBSERVER mutates ops writers** — OBSERVER must be denied for `ISSUE_CREATE`, `VENDOR_MANAGE`, `SLA_MANAGE`, `REVIEW_CREATE`, `DECISION_RECORD`, `MEETING_CREATE`, `RECOGNITION_CREATE`, `LOCATION_CREATE`, `COMPANY_UPDATE`, and `createBusinessUpdate`. **Pinned:** Task 4 (`OBSERVER denial matrix`).
4. **Concurrent leave/transfer races** — two simultaneous leaves or leave+transfer must not orphan OWNER-less companies or double-promote. **Pinned:** Task 2 (`concurrent leave serializes`).
5. **Share-link fails open / invite role ignored** — `createShareLink` must not return a token when insert fails or caller is non-OWNER/ADMIN / company-less; redeem must persist invite `operatingRole` onto membership and surface it via `viewerRole`. **Pinned:** Task 5 + Task 6.

---

## File Structure

| Path | Responsibility |
|------|----------------|
| `backend/typescript/src/modules/business/membership.ts` | Canonical `assertCompanyMomentAccess`; export `assertActiveCompanyMemberForMoment`; lock + leave/transfer; deactivate company-moment participants on leave |
| `backend/typescript/src/modules/governance/resolver.ts` | Align BUSINESS branch of `authorize()`; extend `OBSERVER_DENIED_ACTIONS` |
| `backend/typescript/src/modules/finance/business-finance.ts` | Route revenue/invoice (and expense gate) through membership helper |
| `backend/typescript/src/modules/business/operations-precision.ts` | `createBusinessUpdate` observer + governance; ensure issue/vendor/SLA writers rely on deny set |
| `backend/typescript/src/modules/business/business-closure-writers.ts` | Rely on extended deny set via existing `assertGovernanceAllowed` calls |
| `backend/typescript/src/modules/business/business-closure-reads.ts` | Fail-closed `createShareLink` (company-bound + OWNER/ADMIN) |
| `backend/typescript/src/modules/business/company-invite-service.ts` | Persist + redeem `operatingRole` |
| `backend/typescript/src/modules/business/operating-service.ts` / `operating-catalog.ts` | `viewerRole` / `resolveRole` honor membership `operating_role` |
| `frds/migrations/business/V102__company_operating_role.sql` | `operating_role` on invite + membership |
| `frds/migrations/business/V103__business_moment_company_less_rls.sql` | `can_access_moment` + `business_moment_context` SELECT for company-less |
| `docs/architecture/BUSINESS_BACKEND.md` | Access section matches new rules |
| `backend/typescript/tests/business-authz-isolation.test.ts` | Integration suite for leave/link/observer/share/invite/parity |
| `backend/typescript/tests/platform-foundation.test.ts` | Extend OBSERVER deny cases for new action codes |

---

### Task 1: Canonical company-moment access (membership vs company-less)

**Files:**
- Modify: `backend/typescript/src/modules/business/membership.ts` (`assertCompanyMomentAccess`)
- Modify: `backend/typescript/src/modules/governance/resolver.ts` (`authorize` BUSINESS EXISTS clause)
- Modify: `docs/architecture/BUSINESS_BACKEND.md` (Access bullets)
- Test: `backend/typescript/tests/business-authz-isolation.test.ts` (create)

**Interfaces:**
- Consumes: existing `BusinessMomentScope`, `RequestContext`, `PoolClient`, `AppError` / `ErrorCode.GOVERNANCE_DENIED`
- Produces:
  - `assertCompanyMomentAccess(client, ctx, momentId): Promise<BusinessMomentScope>` — when `company_id IS NOT NULL`, require `cm.status = 'ACTIVE'`; when `company_id IS NULL`, allow creator **or** active participant (not membership)
  - `authorize(...)` BUSINESS: same predicate as `assertCompanyMomentAccess` (no blanket `created_by_user_id` bypass for company-bound Business)
  - Note: `assertActiveCompanyMemberForMoment` lands in Task 3

- [ ] **Step 1: Write the failing tests**

In `business-authz-isolation.test.ts`, add:

```ts
it('company-bound: creator without membership is denied by assertCompanyMomentAccess', async () => { /* ... */ });
it('company-less: creator is allowed; stranger denied', async () => { /* ... */ });
it('authorize BUSINESS matches assertCompanyMomentAccess for company-bound non-member creator', async () => { /* ... */ });
```

Assertions:
- Non-member creator of a company-bound moment → `assertCompanyMomentAccess` throws `AppError` with status **403** / `GOVERNANCE_DENIED`
- Company-less creator → resolves with `companyId === null`
- `authorize({ actionCode: 'COMPANY_ACCESS', momentId })` for that non-member creator → `{ allowed: false }`
- Active member → `{ allowed: true }` and scope has `membershipId`

Seed pattern: copy `ensureUser` / `createCompany` / runway moment from `tests/business-s4-finance.test.ts`; for the “creator without membership” case insert moment + `business_moment_context` with `company_id` set while **omitting** `company_membership` for the actor (or leave company via raw SQL `status = 'LEFT'` before the assert).

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd backend/typescript && npx tsx --test tests/business-authz-isolation.test.ts`

Expected: FAIL — creator without membership still passes current OR-chain (`mp` / `created_by`).

- [ ] **Step 3: Implement access predicates**

In `membership.ts`, change the WHERE access OR to:

- `(bmc.company_id IS NOT NULL AND cm.company_membership_id IS NOT NULL)` **or**
- `(bmc.company_id IS NULL AND (mp.participant_id IS NOT NULL OR m.created_by_user_id = $2))`

Keep DRAFT allowance for creator as today (DRAFT + creator still loads even before ACTIVE membership when product requires setup ownership — only ACTIVE company-bound paths require membership).

In `resolver.ts` `authorize` moment access EXISTS, replace Business + trailing creator OR with the same company-bound / company-less split (Personal/Group branches unchanged). Do not allow `created_by_user_id` alone when `bmc.company_id IS NOT NULL`.

Update `BUSINESS_BACKEND.md` Access section to match.

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd backend/typescript && npx tsx --test tests/business-authz-isolation.test.ts`

Expected: PASS for the three cases above.

- [ ] **Step 5: Commit**

```bash
git add backend/typescript/src/modules/business/membership.ts \
  backend/typescript/src/modules/governance/resolver.ts \
  docs/architecture/BUSINESS_BACKEND.md \
  backend/typescript/tests/business-authz-isolation.test.ts
git commit -m "$(cat <<'EOF'
fix(business): require membership for company-bound moment access

Align assertCompanyMomentAccess and authorize() so company-bound moments
no longer admit creator/participant bypass without active membership.
EOF
)"
```

---

### Task 2: `leaveCompany` / transfer locking + participant cleanup

**Files:**
- Modify: `backend/typescript/src/modules/business/membership.ts` (`leaveCompany`, `transferCompanyOwnership`)
- Test: `backend/typescript/tests/business-authz-isolation.test.ts`

**Interfaces:**
- Consumes: Task 1 `assertActiveCompanyMember` / membership rows; Group pattern `lockGroupMomentContext` + `lockActiveParticipants` in `group-membership.ts`
- Produces:
  - `lockCompanyMemberships(client, companyId): Promise<Array<{ userId: string; membershipType: string; membershipId: string }>>` — `SELECT ... FROM business.company_membership WHERE company_id = $1 AND status = 'ACTIVE' ORDER BY company_membership_id FOR UPDATE`
  - Optional: `lockCompany(client, companyId): Promise<void>` — `SELECT company_id FROM business.company WHERE company_id = $1 FOR UPDATE`
  - `deactivateCompanyMomentParticipants(client, companyId, userId): Promise<void>` — set `collaboration.moment_participant.status` non-ACTIVE for moments joined to this `company_id` via `business_moment_context`
  - `leaveCompany` / `transferCompanyOwnership` call locks **before** promote/leave mutations; leave calls participant deactivation after membership `LEFT`

- [ ] **Step 1: Write the failing tests**

```ts
it('leaveCompany deactivates company-moment participants for the leaving user', async () => { /* ... */ });
it('left member cannot post revenue on company moment', async () => { /* ... */ });
it('concurrent leave serializes without orphaning ownership', async () => { /* ... */ });
```

Assertions:
- After successful leave: membership `status = 'LEFT'`; any `moment_participant` rows for that user on company-bound moments of this company are **not** `ACTIVE`
- `POST /v1/moments/:momentId/revenues` as left user → **403**
- Two concurrent `POST /v1/companies/:id/leave` (OWNER with transfer vs second ADMIN, or two MEMBERs) → exactly one success path that leaves a valid ACTIVE OWNER/ADMIN set (no zero-leader company); loser gets 403/400/409 as appropriate — assert final ACTIVE leader count ≥ 1 when company still has members

HTTP: `POST /v1/companies/:companyId/leave` with `Idempotency-Key` (router already wired).

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd backend/typescript && npx tsx --test tests/business-authz-isolation.test.ts`

Expected: FAIL — leave only flips membership; revenue may still succeed via participant/creator; no serialization.

- [ ] **Step 3: Implement locks + cleanup**

Mirror Group: lock company row, then lock ACTIVE memberships `FOR UPDATE`, then transfer/leave updates. After setting membership `LEFT`, call `deactivateCompanyMomentParticipants`. Apply the same membership lock to `transferCompanyOwnership` before role swaps.

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd backend/typescript && npx tsx --test tests/business-authz-isolation.test.ts`

Expected: PASS for leave/revenue/concurrency cases.

- [ ] **Step 5: Commit**

```bash
git add backend/typescript/src/modules/business/membership.ts \
  backend/typescript/tests/business-authz-isolation.test.ts
git commit -m "$(cat <<'EOF'
fix(business): lock company leave and clear moment participants

Serialize leave/transfer with FOR UPDATE and revoke company-moment
participant rows so leavers lose write access immediately.
EOF
)"
```

---

### Task 3: Company-bound write gates (revenue, invoice, update, expense helper)

**Files:**
- Modify: `backend/typescript/src/modules/business/membership.ts` (implement `assertActiveCompanyMemberForMoment` if deferred from Task 1)
- Modify: `backend/typescript/src/modules/finance/business-finance.ts` (`createBusinessRevenue`, `createBusinessInvoice`; optionally thin `prepareBusinessExpenseGate`)
- Modify: `backend/typescript/src/modules/business/operations-precision.ts` (`createBusinessUpdate`)
- Test: `backend/typescript/tests/business-authz-isolation.test.ts`

**Interfaces:**
- Consumes: Task 1 `assertCompanyMomentAccess` / `requireCompanyBoundMoment`; `assertNotCompanyObserver`
- Produces: `assertActiveCompanyMemberForMoment(client, ctx, momentId): Promise<BusinessMomentScope & { companyId: string; membershipId: string }>` — throws **409** when company-less, **403** when membership missing; writers call this helper (not bare access + separate require)

- [ ] **Step 1: Write the failing tests**

```ts
it('assertActiveCompanyMemberForMoment rejects company-less with 409', async () => { /* ... */ });
it('non-member is denied revenue and invoice create', async () => { /* ... */ });
it('non-member is denied business-update create', async () => { /* ... */ });
```

Assertions:
- Import and call `assertActiveCompanyMemberForMoment` on a company-less moment → `AppError` status **409** (helper must exist; fails with “not exported” / ReferenceError until implemented)
- Participant without ACTIVE membership on company-bound moment → `POST .../revenues`, `POST .../invoices`, `POST .../business-updates` each **403** (may already hold after Task 1; keep as regression pins)
- Active MEMBER → revenue **201**

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd backend/typescript && npx tsx --test tests/business-authz-isolation.test.ts`

Expected: FAIL on `assertActiveCompanyMemberForMoment` missing or not enforcing 409 for company-less.

- [ ] **Step 3: Implement helper + writer gates**

Implement `assertActiveCompanyMemberForMoment` if not done in Task 1. Replace `assertCompanyMomentAccess` + `requireCompanyBoundMoment` at the top of `createBusinessRevenue`, `createBusinessInvoice`, and `createBusinessUpdate` with that helper. Keep `assertNotCompanyObserver` on finance writers. Do not weaken `prepareBusinessExpenseGate` JOIN; only dedupe onto the helper if the membership SQL becomes identical without losing approval-pref RTT bundling.

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd backend/typescript && npx tsx --test tests/business-authz-isolation.test.ts`

Expected: PASS.

Also run: `cd backend/typescript && npx tsx --test tests/business-s4-finance.test.ts`

Expected: existing S4 cases still PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/typescript/src/modules/business/membership.ts \
  backend/typescript/src/modules/finance/business-finance.ts \
  backend/typescript/src/modules/business/operations-precision.ts \
  backend/typescript/tests/business-authz-isolation.test.ts
git commit -m "$(cat <<'EOF'
fix(business): gate revenue invoice and updates on membership

Route company-bound finance and business-update writers through
assertActiveCompanyMemberForMoment like expenses.
EOF
)"
```

---

### Task 4: OBSERVER denial matrix for Business ops

**Files:**
- Modify: `backend/typescript/src/modules/governance/resolver.ts` (`OBSERVER_DENIED_ACTIONS`)
- Modify: `backend/typescript/src/modules/business/operations-precision.ts` (`createBusinessUpdate` — call `assertNotCompanyObserver` + `assertGovernanceAllowed`)
- Modify: `backend/typescript/tests/platform-foundation.test.ts` (extend OBSERVER cases)
- Test: `backend/typescript/tests/business-authz-isolation.test.ts`

**Interfaces:**
- Consumes: existing `assertGovernanceAllowed` on closure/ops writers; `assertNotCompanyObserver(membershipType: string): void`
- Produces: `OBSERVER_DENIED_ACTIONS` includes at least:
  - `ISSUE_CREATE`, `RISK_CREATE`, `VENDOR_MANAGE`, `SLA_MANAGE`, `REVIEW_CREATE`, `DECISION_RECORD`, `MEETING_CREATE`, `RECOGNITION_CREATE`, `RETRO_CREATE`, `ACTIVITY_LOG_CREATE`, `LOCATION_CREATE`, `LOCATION_UPDATE`, `TEAM_CREATE`, `COMPANY_UPDATE`, `TAX_OBLIGATION_CREATE`, `FORECAST_CREATE`, `INVESTOR_UPDATE_CREATE`, `BUDGET_ALERT_CREATE`, `BUSINESS_UPDATE_CREATE`
  - Keep existing Group/finance codes
- `createBusinessUpdate` calls `assertNotCompanyObserver(scope.membershipType)` and `assertGovernanceAllowed(..., { actionCode: 'BUSINESS_UPDATE_CREATE', resourceType: 'BUSINESS_UPDATE', companyId, momentId })`

- [ ] **Step 1: Write the failing tests**

```ts
it('OBSERVER denial matrix for Business ops actions', async () => { /* authorize() loop */ });
it('OBSERVER cannot POST business-updates', async () => { /* HTTP 403 */ });
```

In `platform-foundation.test.ts`, extend:

```ts
it('Business: OBSERVER cannot ISSUE_CREATE / VENDOR_MANAGE / BUSINESS_UPDATE_CREATE', async () => { /* ... */ });
```

Assertions:
- For each action in the matrix, `authorize` with `companyId` (and `momentId` where capability mapping requires it) as OBSERVER → `allowed: false`; as MEMBER → `allowed: true` for a representative write code already used by MEMBER today (`EXPENSE_CREATE` or `ISSUE_CREATE` if catalog-mapped)
- HTTP `POST /v1/moments/:momentId/business-updates` as OBSERVER → **403**

- [ ] **Step 2: Run tests to verify they fail**

Run:

```bash
cd backend/typescript && npx tsx --test \
  tests/business-authz-isolation.test.ts \
  tests/platform-foundation.test.ts
```

Expected: FAIL — OBSERVER currently allowed for missing codes; update path may return 201.

- [ ] **Step 3: Implement deny-set + update governance**

Add codes to `OBSERVER_DENIED_ACTIONS`. Wire `createBusinessUpdate` as specified. Do not add a parallel role system.

- [ ] **Step 4: Run tests to verify they pass**

Same command as Step 2. Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add backend/typescript/src/modules/governance/resolver.ts \
  backend/typescript/src/modules/business/operations-precision.ts \
  backend/typescript/tests/business-authz-isolation.test.ts \
  backend/typescript/tests/platform-foundation.test.ts
git commit -m "$(cat <<'EOF'
fix(governance): deny company observers on Business mutate actions

Extend OBSERVER_DENIED_ACTIONS and block business-update creates for
company OBSERVER membership.
EOF
)"
```

---

### Task 5: Fail-closed share-link mint

**Files:**
- Modify: `backend/typescript/src/modules/business/business-closure-reads.ts` (`createShareLink`)
- Test: `backend/typescript/tests/business-authz-isolation.test.ts`
- Modify if needed: `backend/typescript/tests/business-company-life.test.ts` (OWNER happy path still 201)

**Interfaces:**
- Consumes: `assertActiveCompanyMemberForMoment`, `requireCompanyBoundMoment` (implied), membership type
- Produces: `createShareLink(client, ctx, momentId): Promise<{ shareUrl; shareToken; expiresAt; note }>` — requires company-bound + `OWNER`|`ADMIN`; on INSERT failure **throw** `AppError` (no bare `catch` returning token); never mint when `companyId` null

- [ ] **Step 1: Write the failing tests**

```ts
it('share-link requires OWNER or ADMIN and company binding', async () => { /* ... */ });
it('share-link fails closed when persist fails', async () => { /* ... */ });
```

Assertions:
- MEMBER or OBSERVER → **403**
- Company-less moment → **409** (validation) or **403** — pick one and assert consistently (`requireCompanyBoundMoment` → 409)
- Force persist failure (e.g. invalid FK / revoked table via test double **or** call service with `companyId` null after stubbing access — prefer asserting no token in body when status ≥ 400; if using unit-level client mock is too heavy, assert MEMBER 403 and company-less 409, and delete the bare `catch` that returns token so any DB error propagates as 5xx/AppError)

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd backend/typescript && npx tsx --test tests/business-authz-isolation.test.ts`

Expected: FAIL — MEMBER may still get 201; fail-open catch still returns token.

- [ ] **Step 3: Implement fail-closed mint**

```ts
export async function createShareLink(
  client: PoolClient,
  ctx: RequestContext,
  momentId: string
): Promise<{ shareUrl: string; shareToken: string; expiresAt: string; note: string }>
```

Flow: `assertActiveCompanyMemberForMoment` → membershipType in `OWNER`|`ADMIN` else 403 → INSERT without swallowing errors → return persisted token.

- [ ] **Step 4: Run tests to verify they pass**

Run:

```bash
cd backend/typescript && npx tsx --test \
  tests/business-authz-isolation.test.ts \
  tests/business-company-life.test.ts
```

Expected: PASS (life share test uses company owner).

- [ ] **Step 5: Commit**

```bash
git add backend/typescript/src/modules/business/business-closure-reads.ts \
  backend/typescript/tests/business-authz-isolation.test.ts
git commit -m "$(cat <<'EOF'
fix(business): require admin and fail closed on share-link mint

Stop returning share tokens when persistence fails or the caller is not
an OWNER/ADMIN on a company-bound moment.
EOF
)"
```

---

### Task 6: Company invite `operatingRole` persisted and applied on redeem

**Files:**
- Create: `frds/migrations/business/V102__company_operating_role.sql`
- Modify: `backend/typescript/src/modules/business/company-invite-service.ts`
- Modify: `backend/typescript/src/modules/business/operating-catalog.ts` (`resolveRole`)
- Modify: `backend/typescript/src/modules/business/operating-service.ts` (`viewerRole` — pass operating role when loaded)
- Modify: `backend/typescript/src/modules/business/membership.ts` (`BusinessMomentScope` / access SELECT to include `operating_role` if needed)
- Test: `backend/typescript/tests/business-authz-isolation.test.ts`

**Interfaces:**
- Consumes: `OPERATING_INVITE_ROLES`, `mintCompanyInviteSchema.operatingRole`, `resolveRole(participants, userId, membershipType)`
- Produces:
  - Columns: `business.company_invite.operating_role TEXT NULL`, `business.company_membership.operating_role TEXT NULL` with CHECKs matching invite enum (`FINANCE|APPROVER|OPERATIONS_LEAD|CONTRIBUTOR`)
  - Mint writes column (stop encoding role only in `title_snapshot`; keep snapshot title as display name only for new invites; still parse legacy `||` snapshots in `splitInviteSnapshot` for old rows)
  - `redeemCompanyInvite` copies `operating_role` into membership INSERT/UPDATE
  - `RedeemCompanyInviteResult` gains `operatingRole: string | null`
  - `resolveRole(..., membershipType, operatingRole?: string | null): OperatingRole` — if `operatingRole` set and valid, return it before MEMBER→`TEAM_MEMBER` default (OWNER/ADMIN still map to `OWNER_ADMIN` unless you explicitly decide invite role overrides — **pin:** OWNER/ADMIN membership keeps `OWNER_ADMIN`; operating_role applies for MEMBER/CONTRACTOR)

- [ ] **Step 1: Write the failing tests**

```ts
it('company invite redeem applies operatingRole onto membership', async () => { /* ... */ });
```

Assertions:
- `POST /v1/company/invites` with `{ companyId, membershipType: 'MEMBER', operatingRole: 'FINANCE' }` → body includes `operatingRole: 'FINANCE'`
- Redeem as second user → membership row `operating_role = 'FINANCE'`; redeem response `operatingRole === 'FINANCE'`
- `viewerRole` / operating projection role for that user resolves to `FINANCE` (call `viewerRole` in-transaction or assert via operating GET if moment+setup exist)

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd backend/typescript && npx tsx --test tests/business-authz-isolation.test.ts`

Expected: FAIL — redeem ignores role; column missing until migration.

- [ ] **Step 3: Migration + redeem/mint + resolveRole**

Apply via project migrate path used locally (`npm run migrate:install` / full migrate per engineer env). Implement service + `resolveRole` signature change; update all `resolveRole` call sites.

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd backend/typescript && npx tsx --test tests/business-authz-isolation.test.ts`

Expected: PASS. Also `npx tsx --test tests/group-invite.test.ts` still PASS (untouched).

- [ ] **Step 5: Commit**

```bash
git add frds/migrations/business/V102__company_operating_role.sql \
  backend/typescript/src/modules/business/company-invite-service.ts \
  backend/typescript/src/modules/business/operating-catalog.ts \
  backend/typescript/src/modules/business/operating-service.ts \
  backend/typescript/src/modules/business/membership.ts \
  backend/typescript/tests/business-authz-isolation.test.ts
git commit -m "$(cat <<'EOF'
feat(business): persist invite operatingRole on membership redeem

Add operating_role columns and apply them on redeem so viewerRole
honors finance/approver invite intent.
EOF
)"
```

---

### Task 7: RLS / `can_access_moment` company-less alignment

**Files:**
- Create: `frds/migrations/business/V103__business_moment_company_less_rls.sql`
- Note: `frds/migrations/shared/V024__rls_enable.sql` is historical — **do not rewrite**; replace via V103 `CREATE OR REPLACE FUNCTION`
- Test: `backend/typescript/tests/business-authz-isolation.test.ts`

**Interfaces:**
- Consumes: `security.is_active_company_member`, `security.current_user_id`, `collaboration.moment_participant`, `core.moment`
- Produces:
  - `security.can_access_moment(p_moment_id)` BUSINESS branch:
    - if `company_id IS NOT NULL` → `is_active_company_member(company_id)`
    - if `company_id IS NULL` → creator **or** active participant
  - `business.business_moment_context` SELECT policy: member **or** (`company_id IS NULL` and (creator or active participant)) **or** backend/worker roles (preserve existing worker ORs from V027)

- [ ] **Step 1: Write the failing tests**

```ts
it('RLS can_access_moment allows company-less creator and denies stranger', async () => { /* SET LOCAL role + select */ });
```

Follow patterns in `scripts/db-phase1-tests.ts` / pool session GUCs if the suite already sets `security.current_user_id`; otherwise assert via API reads that already rely on `can_access_moment` (stranger 403 on company-less pulse/operating if exposed). Prefer SQL function assert when test harness can `set_config` the user id.

Assertions:
- Company-less creator → `can_access_moment` true
- Stranger → false
- Company-bound non-member → false even if creator (after Task 1 data shape)

- [ ] **Step 2: Run tests to verify they fail**

Run: `cd backend/typescript && npx tsx --test tests/business-authz-isolation.test.ts`

Expected: FAIL — current function returns false whenever `company_id IS NULL`.

- [ ] **Step 3: Ship V103 function + policy updates**

`CREATE OR REPLACE FUNCTION security.can_access_moment...` and `DROP POLICY` / `CREATE POLICY` for `rls_business_moment_context__select_member` only. Do not broaden finance RLS to null company (finance stays company-bound).

- [ ] **Step 4: Run tests to verify they pass**

Run: `cd backend/typescript && npx tsx --test tests/business-authz-isolation.test.ts`

Expected: PASS. Run `npm run migrate:validate` if available in env.

- [ ] **Step 5: Commit**

```bash
git add frds/migrations/business/V103__business_moment_company_less_rls.sql \
  backend/typescript/tests/business-authz-isolation.test.ts
git commit -m "$(cat <<'EOF'
fix(rls): allow company-less business moment access for participants

Align can_access_moment and business_moment_context SELECT with V095
nullable company_id creator/participant rules.
EOF
)"
```

---

### Task 8: Isolation suite — link-company, governance parity, regression sweep

**Files:**
- Modify: `backend/typescript/tests/business-authz-isolation.test.ts`
- Modify: `docs/architecture/BUSINESS_BACKEND.md` if any Access drift remains

**Interfaces:**
- Consumes: Task 1–7 behaviors; `linkBusinessMomentCompany` in `modules/moment/service.ts`; HTTP routes already in `api/v1/router.ts`
- Produces: complete Review Focus coverage in one file; no new production APIs

- [ ] **Step 1: Write the failing tests (any still missing)**

```ts
it('link-company isolates prior participants from writes', async () => { /* ... */ });
it('governance authorize and assertCompanyMomentAccess agree for member and left user', async () => { /* ... */ });
```

Assertions:
- Create company-less moment as A; add B as ACTIVE `moment_participant`; A links company where A is OWNER; B posts revenue → **403**; A posts revenue → **201**
- For same momentId/userId pairs, `authorize` allowed iff `assertCompanyMomentAccess` succeeds (wrap assert in try/catch → boolean)

- [ ] **Step 2: Run full isolation + related suites**

```bash
cd backend/typescript && npx tsx --test \
  tests/business-authz-isolation.test.ts \
  tests/business-s4-finance.test.ts \
  tests/business-company-life.test.ts \
  tests/platform-foundation.test.ts \
  tests/security-gate-idor.test.ts
```

Expected: FAIL only on missing link-company/parity cases before Step 3; after implementation all PASS.

- [ ] **Step 3: Fix any remaining drift**

If link-company still leaves B authorized, verify Task 1 predicate (membership required when `company_id` set) — do not special-case link. If docs still claim participant OR for company-bound, fix Access section.

- [ ] **Step 4: Re-run suites — expect PASS**

Same command as Step 2.

- [ ] **Step 5: Commit**

```bash
git add backend/typescript/tests/business-authz-isolation.test.ts \
  docs/architecture/BUSINESS_BACKEND.md
git commit -m "$(cat <<'EOF'
test(business): pin authz isolation for leave link and parity

Cover link-company participant isolation and governance vs moment-access
agreement after membership hardening.
EOF
)"
```

---

## Intentional scope cuts

- **No new `canMutate` framework** — folded into Tasks 1/3/4 (YAGNI).
- **No client (Android/iOS) invite UX changes** — backend redeem + role resolution only.
- **No rewrite of all `business.*` RLS policies** for null `company_id` — only `can_access_moment` + `business_moment_context` SELECT; finance/khata remain company-bound.
- **No change to Group invite flows** — company invites only.
- **Capability catalog seeding** for brand-new action codes (`BUSINESS_UPDATE_CREATE`) — only if `authorize` capability mapping blocks MEMBER writes; if blocked, add a minimal capability row in V102 or reuse an existing mapped write code for the update governance call (prefer existing mapped code only if semantically honest; otherwise seed capability + moment_type maps in V102).

---

## Self-review (plan author)

| Check | Result |
|-------|--------|
| Spec coverage | Findings 1–7 mapped to Tasks 1–7; finding 8 deferred explicitly; Review Focus pinned to Tasks 2/4/5/6/8 |
| Step scan | Each task is fail→pass→commit; no TBD steps; signatures without full SQL bodies except WHERE predicate in Task 1 |
| Type consistency | `assertActiveCompanyMemberForMoment`, `BusinessMomentScope`, `operating_role`, `BUSINESS_UPDATE_CREATE`, `leaveCompany` locks used consistently |
| Proportion | One sequential authz plan (not split); invite=V102, RLS=V103 |
| Test runner | `npx tsx --test …` matching `package.json` `"test": "tsx --test tests/**/*.test.ts"` |
