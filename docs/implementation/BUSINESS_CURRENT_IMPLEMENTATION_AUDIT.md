# Business current implementation audit (Phase B0)

Date: 2026-09-30. Forensic trace only. No UX changes.

Judged against the locked model: one company, three moment families (Money `BUSINESS_RUNWAY`, Daily Business `BUSINESS_OPERATIONS`, Team & Work `TEAM_OPERATIONS`), tabs Pulse / Moments / Create / Life / Memory, two audiences on one surface via config.

A file existing is not LIVE. Each row below was traced router → service → SQL → DTO, then client call → the composable or view the shell actually mounts. Backend status is separate from Android and iOS wiring.

Provenance tags: `SERVER_REAL`, `CLIENT_DERIVED_REAL`, `STATIC_COPY`, `PLACEHOLDER`, `UNKNOWN`.

## How to use this before B1

B1 can start a Pulse and Create layout pass for the signed-in owner on a single active moment. The write APIs, pulse GET, Life GET, Memory item list, and both Quick Add hubs are wired.

B1 must not treat the health ring, the company-level pulse row, or an empty capability list as the product source of truth. Those are the blockers in section 13. B0.5 device checks are required before any copy that says “this moment’s attention,” “this location’s cash,” or “viewers cannot write.”

Do not delete the three Pulse / Moments / Memory trees, Khata sheets, `CompanyLifeActiveContent`, `BusinessActiveTheme`, or `BusinessActionRegistry` until a replacement path is verified.

---

## 1. Backend feature matrix

Live router: `backend/typescript/src/api/v1/router.ts`, mounted from `backend/typescript/src/app.ts`. `router-product.ts` is not mounted.

Isolation shorthand:

- `member` — active `business.company_membership` for the path `companyId`.
- `moment-member` — `assertCompanyMomentAccess`: domain `BUSINESS`, context `ACTIVE`, caller is an active member of that moment’s company.
- Idempotency `yes+cmd` — `requireIdempotencyKey` and `runCommand` replay. `header` — header required, handler does not store a replay. `no` — GET.

Observer deny (`OBSERVER_DENIED_ACTIONS` in `backend/typescript/src/modules/governance/resolver.ts`) covers `EXPENSE_CREATE`, `MEMORY_CREATE`, `POLL_CREATE`, and the Group codes. It does **not** include `COMPANY_UPDATE`, `LOCATION_CREATE`, `ISSUE_CREATE`, `VENDOR_MANAGE`, `SLA_MANAGE`, `RISK_CREATE`, `DECISION_RECORD`, `REVIEW_CREATE`, `MEETING_CREATE`, `RECOGNITION_CREATE`, `MILESTONE_CREATE`. `POST .../business-updates` does not call governance at all.

| Feature | Method + path | Service | Tables | Idempotency | Authz | Backend |
|---|---|---|---|---|---|---|
| Company list | GET `/v1/companies` | `listCompanies` | R `business.company`, `company_membership` | no | SQL filter on caller | LIVE |
| Company create | POST `/v1/companies` | `createCompany` | W company + OWNER membership | header | `COMPANY_CREATE` | LIVE |
| Company get | GET `/v1/companies/:companyId` | `getCompany` | R company | no | `COMPANY_READ` + member | LIVE |
| Company patch | PATCH `/v1/companies/:companyId` | `updateCompany` | W company | header | `COMPANY_UPDATE`. Status change OWNER-only. Observer not denied | LIVE |
| Locations | GET/POST `/v1/companies/:id/locations`, PATCH `.../locations/:locationId` | `listLocations`, `createLocation`, `updateLocation` | `business.company_location` | GET no / writes header | `LOCATION_*` + member. Observer gap | LIVE |
| Members list | GET `.../members` | `listCompanyMembers` | R membership, `core.user_profile` | no | active member | LIVE |
| Members add | POST `.../members` | `addCompanyMember` | W membership | yes+cmd | OWNER/ADMIN, cannot add OWNER | LIVE |
| Leave | POST `.../leave` | `leaveCompany` | W membership `LEFT` | yes+cmd | self; OWNER/ADMIN must name successor | LIVE |
| Transfer | POST `.../transfer-ownership` | `transferCompanyOwnership` | W membership | yes+cmd | OWNER | LIVE |
| Invite mint | POST `/v1/company/invites` | `mintCompanyInvite` | W `business.company_invite` | yes+cmd | OWNER/ADMIN | LIVE |
| Invite preview | GET `/v1/company/invites/:code` | `getCompanyInviteByCode` | R/W invite (expiry) | no | any authenticated user | LIVE |
| Invite redeem | POST `.../redeem` | `redeemCompanyInvite` | W membership, claim | yes+cmd | valid code | LIVE |
| Teams | GET/POST `.../teams` | `listTeams`, `createTeam` | `business.team` | GET no / POST header | `TEAM_*`. No patch/delete route | LIVE |
| Vendors | GET/POST `.../vendors`, PATCH `.../vendors/:vendorId` | reads + `createVendor` / `updateVendor` | `business.vendor` | writes yes+cmd | member; patch `VENDOR_MANAGE`. Observer gap | LIVE |
| Contracts | POST `.../vendors/:vendorId/contracts` | `createVendorContract` | W `business.vendor_contract` | yes+cmd | `VENDOR_MANAGE` | PARTIAL — write only, no list route |
| SLA | POST `.../sla-definitions`, POST `.../checks` | `createSlaDefinition`, `recordSlaCheck` | W `sla_definition`, `sla_check` | yes+cmd | `SLA_MANAGE` | PARTIAL — write only |
| Khata parties | GET/POST `.../khata/parties` | `listKhataParties`, `createKhataParty` | `business.vendor` (`vendor_type` = party kind) + R `khata_entry` | POST yes+cmd | member. Observer gap on create | LIVE |
| Khata entries | GET `.../parties/:partyId/entries`, POST `/v1/moments/:momentId/khata/entries` | `listKhataPartyEntries`, `createKhataEntry` | `business.khata_entry` | POST yes+cmd | moment-member; observer denied; party company must match | LIVE. No SSE |
| Setups catalog | GET `/v1/business/setups` | `getSetupCatalog`, `listUserSetups` | R `business.business_system_setup` + in-memory catalog | no | caller | LIVE |
| Setup activate | POST `/v1/business/setups/:familyCode/activate` | `activateBusinessSetup` → `createMoment` | W `core.moment`, `business_moment_context`, setup row | yes+cmd | active member of `body.companyId` | LIVE. **No client caller** |
| Moment create | POST `/v1/moments` `domainCode=BUSINESS` | `createMoment` | W moment + `business_moment_context` | yes+cmd | active company membership | LIVE. This is the path both apps use |
| Business moments list | GET `/v1/business/moments` | `listBusinessMoments` | R context + moment + membership | no | membership join. DRAFT only if creator | LIVE |
| Expenses | POST `/v1/moments/:id/business-expenses`, GET `/v1/business/moments/:id/expenses` | `createBusinessExpense`, `listBusinessExpenses` | W `finance.expense`, `business_expense_context`, snapshot; may W `governance.approval_*` | POST yes+cmd | moment-member; observer denied on create | LIVE |
| Revenues | POST `/v1/moments/:id/revenues`, GET list | `createBusinessRevenue`, `listBusinessRevenues` | `finance.revenue`, snapshot | POST yes+cmd | observer denied on create | LIVE |
| Invoices | POST `/v1/moments/:id/invoices`, GET list | `createBusinessInvoice`, `listBusinessInvoices` | `finance.invoice`, `invoice_line` | POST yes+cmd | observer denied on create | LIVE |
| Updates | POST `/v1/moments/:id/business-updates`, GET list | `createBusinessUpdate` | `business.business_update` | POST yes+cmd | moment-member only. No governance | LIVE |
| Reviews | POST `.../business-reviews` | `createBudgetReview` | `business.business_review` | yes+cmd | `REVIEW_CREATE`. No list route | LIVE |
| Approvals | POST `.../approval-requests`, GET list, POST `/v1/approvals/:id/decide` | `createApprovalRequest`, `listPendingApprovals`, `decideBusinessApproval` | `governance.approval_*`; decide may update expense | yes+cmd | create uses `EXPENSE_CREATE` (observer denied). Decide: OWNER/ADMIN + `COMPANY_UPDATE` | LIVE |
| Issues | POST/GET `.../issues`, POST evidence | `createIssue`, list, `addIssueEvidence` | `business.issue` | POST yes+cmd | `ISSUE_CREATE` on create. Observer gap | LIVE |
| Risks, decisions, meetings, recognition | POST `/v1/moments/:id/risks|decisions|meeting-records|recognitions` | closure writers | `business.risk`, `decision`, `meeting_record`, `recognition` | yes+cmd | action code. Observer gap | LIVE |
| Milestones | POST `/v1/moments/:id/milestones` | `workService.createMilestone` | `work.milestone`, `work.goal` | yes+cmd | `MILESTONE_CREATE`. Observer gap | LIVE |
| Polls create | POST `/v1/moments/:id/polls` | `createPoll` | `shared.poll` | yes+cmd | observer denied | LIVE |
| Polls list | GET `/v1/group/moments/:id/polls` | `listPolls` | R poll | no | `assertGroupMember` only | PARTIAL — company members are not authorized on this list |
| Memory write | POST `/v1/business/moments/:id/memories` | `createBusinessMemory` | W `memory.memory`, refresh `projection.business_memory` | yes+cmd | `MEMORY_CREATE` denies observers | LIVE |
| Facet pulse | GET `.../pulse` | `getBusinessPulseOrFinance` | R `projection.business_pulse` (PK company), snapshot, activity | no | membership in SQL | LIVE numbers, company-scoped row. See §11 |
| Facet finance | GET `.../finance` | same | R `projection.business_finance_snapshot` | no | moment-member | LIVE. EMPTY if no snapshot |
| Facet life | GET `.../life` | `getBusinessMomentProjection` | R `projection.business_life` (company), pulse, issues, activity, setups | no | moment-member | LIVE. One company row |
| Facet memory | GET `.../memory` | same | R `projection.business_memory` (company), fallback `memory.memory` | no | moment-member | PARTIAL. Items are real rows for the moment that last refreshed. `pattern_count` and `playbook_count` are written as 0 |
| Facet actions | GET `.../actions` | `getAvailableActions` | none | no | moment-member | PLACEHOLDER. Every action returned `enabled: true` |
| Activity | GET `.../activity` | `getBusinessMomentActivity` | R `projection.recent_activity` | no | moment-member | LIVE but **actor-only**: `user_id = caller` |
| Timeline | GET `.../moments` | `listBusinessMomentTimeline` | R `projection.business_moments`, contracts | no | moment-member | LIVE |
| Capacity | GET `.../capacity` | `getCapacity` | R membership, issues | no | moment-member | PARTIAL. Heuristic issues / (members × 5) |
| Workload | GET `.../workload` | `getWorkload` | R issues by severity | no | moment-member | PARTIAL. “Department” buckets are severities |
| MoM deltas | GET `.../mom-deltas` | `getMomDeltas` | R revenue, expense | no | moment-member | LIVE |
| Progress snapshot | GET `.../progress-snapshot` | `getProgressSnapshot` | R forecast, review | no | moment-member | LIVE |
| Roster | GET `.../roster` | `getRoster` | R membership, profile | no | moment-member | LIVE |
| Weekly report | GET `.../weekly-report` | `getWeeklyReport` | issues, decisions, updates, milestones, expenses | no | moment-member | LIVE |
| Share link | POST `.../share-link` | `createShareLink` | W `business.share_link` | header | moment-member. Observer not denied | LIVE. Insert failure still returns a token |
| Also mounted | POST tax-obligations, forecast-scenarios, investor-updates, budget-alerts, improvements, retrospectives, activity-log-entries | closure / operations-precision | matching business/finance tables | yes+cmd | moment-member + action code. Observer gap | LIVE |

Company and moment writes that publish SSE call `publishProjectionUpdated`, which delivers only to the acting user (`backend/typescript/src/realtime/sse.ts`). Company, location, team, member, vendor create, and khata do not publish.

### Pulse score computation (server)

`refreshBusinessPulseProjection` in `backend/typescript/src/modules/business/business-projection.ts`:

- `financial_health_score` = `computeHealthScore`. If runway months exist, score is runway vs a target (default 6), capped 0–100. Else if snapshot totals exist, `revenue/expense * 50` capped at 50. Else null. Provenance of the number: `SERVER_REAL` (formula, not a constant).
- The pulse row is `ON CONFLICT (company_id)`. Finance fallback reads `projection.business_finance_snapshot` for the company `ORDER BY expense_total DESC LIMIT 1`, not the moment and not a location.
- `open_risk_count` is inserted as 0 and is not selected into the pulse API.
- `computeTeamScore` and `computeOpsScore` are stored on pulse history. The Team pulse screen does not display `team_score`. The Ops pulse screen displays SLA percent, not `ops_score`.
- Ops extras include `sectionQuality` (`REAL_DATA`, `EMPTY_SUPPORTED`, or `operationsIntelligence: DEFERRED`) from `loadOpsPulseExtras` in `operations-precision.ts`.

### Memory projection (server)

`refreshBusinessMemoryProjection` loads real `memory.memory` rows for that company+moment, then upserts **one** `projection.business_memory` row per company. `pattern_count = 0`, `playbook_count = 0`, `learning_count = item count`. Risk vs success is a keyword scan (`risk`, `issue`, `incident`) on title and body.

---

## 2. Android feature matrix

Shell: `apk/app/src/main/java/com/example/momentra/ui/shell/AppShellScreen.kt`. Family match on `selectedMomentTypeCode`: `RUNWAY`, else `TEAM_OPERATIONS`, else `OPERATIONS` without `TEAM`, else shared fallback.

| Surface | Composable | API | Wiring |
|---|---|---|---|
| No company, Create | `CompanyFlowSheet` | — | LIVE gate |
| No company, other tabs | `BusinessPulseEmptyContent`, `BusinessMomentsEmptyContent`, `BusinessLifeEmptyContent`, `BusinessMemoryEmptyContent` | none | STATIC_COPY |
| Company, first moment, non-Create | same empty contents | none | STATIC_COPY |
| Company, between moments | `MomentEmptyState` | none | STATIC_COPY |
| Company Create (no moment selected) | `BusinessCreateFlow` | see setup | LIVE |
| Active Create | `BusinessQuickAddHub` | tile → sheets below | LIVE |
| Runway Pulse / Moments / Memory | `RunwayPulseActiveContent`, `RunwayMomentsActiveContent`, `RunwayMemoryActiveContent` | pulse, activity, finance, mom-deltas, progress-snapshot, timeline, memory | LIVE reads |
| Ops Pulse / Moments / Memory | `OpsPulseActiveContent`, `OpsMomentsActiveContent`, `OpsMemoryActiveContent` | pulse, issues, timeline, activity, memory | LIVE reads |
| Team Pulse / Moments / Memory | `TeamOpsPulseActiveContent`, `TeamOpsMomentsActiveContent`, `TeamOpsMemoryActiveContent` | pulse, capacity, workload, expenses, approvals, timeline, memory | LIVE reads |
| Life (all families) | `BusinessLifeActiveContent` → `CompanyLifeActiveContent` | GET `.../life`, GET `.../weekly-report` | LIVE. One body, focus filter |
| Other type code | `BusinessPulseActiveContent`, `BusinessMomentsActiveContent`, `BusinessMemoryActiveContent` | pulse / activity / memory | LIVE, thinner |
| Company activate | `CompanySetupContent.onActivate` | POST `/v1/companies`, POST locations, optional PATCH, then small-shop loop `POST /v1/moments` | LIVE. Does not call setup activate |
| Moment wizard | `BusinessSetupWizardContent` | `POST /v1/moments` via `MomentCreateRepository.createBusinessMoment` | LIVE |
| Khata | `BusinessKhataHomeSheet` | parties + entries | LIVE. Save does not refresh shell tabs |
| Expense | `BusinessExpenseSheet` | POST `business-expenses` | LIVE |
| Revenue / invoice | `RunwayQuickAddSheet` or `BusinessRevenueSheet` / `BusinessInvoiceSheet` | POST revenues / invoices | LIVE |
| Tax, investor, budget alert, forecast, general update | `RunwayQuickAddSheet` | matching POSTs | LIVE |
| Vendor, issue, improvement, SLA, approval, review | `OpsGapQuickAddSheet` | matching POSTs | LIVE |
| Team kinds + poll | `TeamOpsGapQuickAddSheet` | matching POSTs; poll via `GroupSliceRepository.createPoll` | LIVE |
| Memory quick add | family sheet | POST `.../memories` | LIVE |
| Locations manager | `BusinessLocationFlow` from company settings | GET/POST/PATCH locations | LIVE |
| Vendor operations gap | `VendorOperationsScreen` via `BusinessGapPage.Vendor` | vendor/contract/SLA writes | PARTIAL (no contract/SLA list API) |
| Milestone / visibility gap | moments directory settings | local `remember` only | PLACEHOLDER |
| Create chooser Memory tab | `BusinessCreateMomentContent` | `memories = emptyList()` | PLACEHOLDER. No API |
| Project / Event / Vendor cards | same chooser, growing only | not clickable | PLACEHOLDER |
| Members sheet | `BusinessMembersSheet` | GET members | LIVE API, hub `onMembers` is never called |

---

## 3. iOS feature matrix

Shell: `momentra/momentra/Shell/AppShellView.swift`. Same family split as Android. Paths under `momentra/momentra/Shell/`.

| Surface | View | APIClient | Wiring |
|---|---|---|---|
| Empty / first moment | `BusinessPulseEmptyView`, moments/life/memory empty views | none | STATIC_COPY |
| Create with company | `BusinessCreateFlowView` | — | LIVE |
| Active hubs | `BusinessQuickAddHub` | same POST family as Android | LIVE |
| Runway / Ops / Team pulse, moments, memory | `RunwayPulseActiveView`, `OpsPulseActiveView`, `TeamOpsPulseActiveView` and matching Moments/Memory views | pulse, life (if previously loaded), activity, timeline, capacity, workload, memory | LIVE |
| Life | `BusinessLifeActiveView` + `CompanyLifeComponents.swift` | GET life, weekly-report, POST share-link | LIVE |
| Fallback type | `BusinessPulseActiveView`, `BusinessMomentsActiveView`, `BusinessMemoryActiveView` | thinner lists | LIVE |
| Company setup | `CompanySetupFlowView` | POST companies, locations, members, invites, media | LIVE |
| Moment wizard | `BusinessSetupWizardView` → `createMoment` | POST `/v1/moments` | LIVE |
| Khata | `BusinessKhataHomeView` | parties, entries; cash sale uses `createBusinessRevenue` | LIVE |
| Locations | `BusinessLocationFlow` in `BusinessGapScreens.swift` | list/create/patch | LIVE |
| Milestone + visibility gap | `BusinessGapScreens.swift` | none (`@State` only) | PLACEHOLDER |
| `getBusinessSetups` / `activateBusinessSetup` | no callers | GET/POST exist on client | CLIENT_NOT_WIRED |
| `getBusinessActions`, mom-deltas, progress-snapshot, roster, revenue/invoice/issue list GETs | no view callers | client methods exist | CLIENT_NOT_WIRED (Android Runway moments does call mom-deltas, progress-snapshot, finance) |

---

## 4. Endpoint to client mapping

| Endpoint | Android | iOS |
|---|---|---|
| POST/GET/PATCH companies, locations, members, invites | wired | wired |
| POST `/v1/moments` business setup | wired (activate and wizard) | wired (wizard and template create) |
| POST `/v1/business/setups/:family/activate` | not called | not called |
| GET pulse, life, memory, finance, activity, timeline | wired | wired (finance on runway path) |
| GET capacity, workload | Team pulse | Team pulse |
| GET mom-deltas, progress-snapshot | Runway moments | not called |
| GET roster, actions | not called from these screens | not called |
| POST expenses, revenues, invoices, updates, issues, approvals, memories, khata, vendors | wired | wired |
| POST tax, investor, budget alert, forecast, SLA, improvement, review, decision, risk, meeting, recognition, milestone, retro, activity log, poll | wired from growing (and some team) hubs | wired |
| GET expenses | Team pulse | Team pulse |
| GET revenues, invoices, issues (list) | issues on Ops pulse; expenses on Team | issues path on ops; list GETs otherwise unused |
| POST decide approval | Team pulse | Team pulse |
| GET weekly-report, POST share-link | Life | Life |
| GET group polls | not used as the business list | not used as the business list |

---

## 5. Active route map

1. No company: Create opens company setup. Pulse, Moments, Life, Memory show marketing empty states.
2. Company, no selected moment: Create opens `BusinessCreateFlow` (chooser → wizard, or template moments already created at activate). Other tabs stay empty or “between moments” copy.
3. Selected moment: bottom tab plus type code picks one of three trees, or the shared fallback. Life is always `CompanyLifeActiveContent` / `BusinessLifeActiveView` with focus `RUNWAY`, `TEAM`, `OPS`, or unlocked chips.
4. Create with a selected moment: Quick Add hub for that family and audience. It does not open the moment wizard.
5. New-moment overlay: company setup if no company, otherwise `BusinessCreateFlow`.
6. Company menu: switch company, settings (profile, modules, locations, owner-only transfer/archive), gap host for the moment directory.

---

## 6. Dead or orphan routes

Do not delete these in B1.

| Item | Reachable? | Status |
|---|---|---|
| Create cards Project, Event, Vendor | Yes, growing chooser only. `comingSoon`, not clickable. Hidden for small shop | PLACEHOLDER |
| Create Memory tab | Yes. Hardcoded empty list. No memory API on that tab | PLACEHOLDER |
| Shared `BusinessPulseActiveContent` / `BusinessMomentsActiveContent` / `BusinessMemoryActiveContent` | Only when type code is not runway, ops, or team | LIVE fallback, not the three-family path |
| `BusinessGapHost` Moments / Finance / Vendor / Company pulse | Yes, from the moment switcher and Life report failure | LIVE shell, mixed data |
| Milestone tracking and Visibility settings | Yes, from the moments directory | PLACEHOLDER local state |
| `BusinessGapQuickAddSheet` generic body (“API not wired”) | Only if type code matches no family sheet | DEAD for the three live families |
| Backend `POST /business/setups/:family/activate` | No UI caller. Clients use `POST /moments` | LIVE backend, CLIENT_NOT_WIRED |
| `GET .../actions` | No screen | PLACEHOLDER backend |
| Poll list under `/group/moments/.../polls` | Business create posts to `/moments/:id/polls` | PARTIAL. List is Group-member gated |

---

## 7. Small shop behavior

Audience is config, not a second app. Android `BusinessAudience.SMALL_SHOP`. iOS `BusinessAudience` plus UserDefaults `momentra.business.audience.{companyId}`.

- Setup shows industry templates (`KIRANA`, `PET_RETAIL`, `WORKSHOP`, `SERVICE`, `CUSTOM`) via `IndustryTemplateCatalog`. Growing passes `template = null`.
- On activate, small shop loops `setupKindsFor` and `POST /v1/moments` status `ACTIVE`. Kirana, pet, and service create Money + Daily Business. Workshop also creates Team. Custom uses the toggles. A failed moment create warns and does not roll back the company. Growing copy tells the user to open Create. It does not auto-create moments.
- Location step locks the structure chip to Single Location. The “add another location” row is still on screen, and the list can still POST multiple `company_location` rows. Finance and pulse queries do not filter by location either way.
- Quick Add (`businessHubTiles` / `BusinessQuickAddKind.hubTiles`): Money = Khata, Revenue, Expense, Invoice, Memory. Daily = Khata, Spend, Update Vendor, Report Issue, Memory. Team = Khata, Staff update, Expense, Approval, Memory.
- Moments filter chips are hidden and the filter is forced to All.
- Memory playbook, pattern network, wisdom, and knowledge-journey cards are hidden. Success and risk lists stay.
- Pulse “intelligence” cards are hidden.

---

## 8. Growing business behavior

- No starter templates. Location step offers Single, Multi-Location, and Multi-Unit.
- Create chooser adds Project, Event, and Vendor operations as Coming Soon.
- Quick Add is the long catalog: Money adds Tax, Investor, Budget alert, Forecast, General update, and does not include Khata. Daily adds Request approval, Improvement, Budget review, SLA, General update, and does not include Khata. Team adds update, decision, blocker, meeting, recognition, approval, milestone, retro, risk, activity log, poll, memory, expense.
- Moments chips: Runway All / Revenue / Expenses. Ops Budget / Vendors / Issues / Updates. Team All / Milestones / Decisions / Deliveries. Filters are client string matches on timeline and activity rows.
- Pulse intelligence sections render as “Coming soon.”
- Memory pattern, playbook, wisdom, and knowledge-journey sections render. Runway and Team hero pattern/accuracy are the string `"—"`. Ops hero `patternCount = items.size / 3` is client-invented. Those sections are `PLACEHOLDER`, not a model.

---

## 9. Android / iOS parity gaps

Shared and aligned: three trees plus one Life, Khata, audience tile sets, fail-open registry, health-band copy, small-shop hiding of intelligence and memory shells, refresh token without cache invalidate, no Business `viewerReadOnly`.

| Gap | Android | iOS |
|---|---|---|
| Runway moments extras | calls finance, mom-deltas, progress-snapshot | timeline + finance + activity. No mom-deltas or progress-snapshot |
| Ops SLA empty state | shows the SLA number the server sent | hides the number unless `sectionQuality.slaCompliance == REAL_DATA`, otherwise `"—"` |
| Runway attention severity | from activity payload when present | first row hardcoded HIGH, rest MED (`STATIC_COPY`) |
| Fallback pulse/moments/memory | shared composables | thinner list shells |
| Setup activate endpoint | unused | unused |
| Hub filter chips | audience-specific tiles only | `hubFilterChips` returns `[]` |
| Cash sale | Khata entry path | Khata cash sale also calls `createBusinessRevenue` |

---

## 10. UX duplication versus one config, one body

What already matches the rule:

- One company Life body with focus filters (`CompanyLifeActiveContent` / `BusinessLifeActiveView`). Filters drop server rows. They do not mount a second dashboard.
- One Quick Add registry and one theme object (`BusinessActiveTheme`, `BusinessActionRegistry`) with audience catalogs.
- One company setup. Audience changes copy, templates, location chips, and hub tiles.

What does not:

- Pulse, Moments, and Memory are three composable trees (Runway, Ops, TeamOps) plus a fourth fallback. The same health-band `when` is copied in each tree on both platforms.
- Growing vs small shop is config at the hub and at a few `if (!smallShop)` wrappers. The trees themselves are not config-driven.
- Create moment chooser, gap host, and family sheets are separate entry bodies. The generic gap sheet is a third Create implementation that the three live families do not use.

B1 should add a family config in front of one Pulse body and one Create body. It should not delete the trees in the same change.

---

## 11. Backend gaps versus the five questions

### Pulse — what needs attention now

| UI | Provenance | Evidence |
|---|---|---|
| Ring number | `SERVER_REAL` | Runway and Team: `financialHealthScore`. Ops: `operations.slaCompliancePct` (iOS only if `sectionQuality` is `REAL_DATA`) |
| “Strong & Growing” / “Strong & Stable” / “Stable & Optimizing” / “Needs focus” / “At risk” | `CLIENT_DERIVED_REAL` | Local thresholds. Team uses the finance score, not `team_score` |
| “Awaiting live health signal”, “Cash balance not added” | `STATIC_COPY` | Null score, or Life cache miss. Pulse load does not fetch Life. Cash appears only if Life was opened earlier (`putLife`) |
| Burn | `SERVER_REAL` | Life `monthlySpending`, else snapshot `expenseTotal` |
| Attention rows | `SERVER_REAL` titles from actor activity, else issues | Activity GET is `user_id = caller`, so other members’ events are absent |
| Intelligence cards | `PLACEHOLDER` | “Coming soon”. Hidden for small shop |
| Ops `operationsIntelligence` | `PLACEHOLDER` | Server `sectionQuality` value `DEFERRED` |
| `open_risk_count` | not shown | Server always writes 0 |

The score is a formula over runway and snapshot totals. It is not a fabricated constant. It is also not “what needs attention now.” The row is one per company, so the last moment refresh wins `financial_health_score` (null does not clear it: `COALESCE`).

### Moments — what happened

Timeline GET is moment-scoped and `SERVER_REAL`. Activity mixed into the same lists is actor-only. Filters are client-side. Small shop forces All. This tab does not render the health ring.

### Life — how the business is doing

KPIs, signals, journey, trends, and module cards come from GET life (`SERVER_REAL` fields, empty copy when rows are missing). Narrative Healthy / Watch / Needs focus is `CLIENT_DERIVED_REAL` on `kpis.financialHealthScore`. Trends empty line is `STATIC_COPY`. Life activity SQL is company-wide across active business moments, not actor-filtered. Life projection is still one row per company. No location filter.

### Memory — what the business learned

Success and risk lists are real memory items (`SERVER_REAL` rows) split by the same keyword rule the server uses (`CLIENT_DERIVED_REAL` labels). Pattern count, playbook count, wisdom, and knowledge journey are `PLACEHOLDER`. The projection row is per company, so a refresh for moment B replaces the payload that moment A’s screen reads if the client trusts the company row. The Create tab’s memory chooser does not call this API.

### Create — what to record

Hubs call the live POSTs in sections 2 and 3. Registry behavior is in section 13. Revenue and invoice tiles also require a type code containing `RUNWAY` (`isRunwayFinanceEnabled`). Khata, memory, and most team tiles are not mapped to a capability, so they stay enabled even when the capability list is non-empty.

### Refresh after a successful save

`BusinessTabDataCache.invalidateMoment` has no call sites on Android or iOS.

`refreshVisibleBusinessTab` (`AppShellViewModel.kt`, `AppShellModel.swift`): if the pulse cache is warm and `forcePrefetch` is false, it skips prefetch and only increments `businessTabRefreshToken`. The mounted tab’s load effect refetches. Hidden tabs keep the old cache until opened, and the old cache paints first. An in-flight prefetch started before the write can write the pre-write payload back.

| Save | Cache invalidate | Pulse | Moments | Life |
|---|---|---|---|---|
| Expense, revenue, invoice, runway/ops/team quick add | no | visible tab refetches | only if that tab is mounted | only if that tab is mounted |
| Khata sheet | no | no shell refresh | no | no |
| Other members | n/a | SSE is actor-only | same | same |

Server projection refresh on expense/revenue does run inside the write. The client gap is cache and fan-out, not a missing SQL update for the actor’s own next GET.

### Permission

| Control | UI | Server |
|---|---|---|
| Business viewer / observer | No `viewerReadOnly` on Business screens. Group has it. Business does not | Observer denied for expense, revenue, invoice, memory, poll, approval create. Not denied for company patch, locations, vendors, issues, decisions, risks, meetings, updates, SLA |
| Quick Add tiles | `BusinessActionRegistry`: empty capability list fails open to seven default codes. Non-empty list filters spend, vendor, issue, SLA, revenue, invoice | Governance catalog can still 403. Client hide is not enforcement |
| Company settings | Profile, modules, alerts, memory toggle call PATCH with no role check in the UI. Transfer, archive, deactivate hidden unless `membershipType == OWNER` | Status change is OWNER-only. Other patches are `COMPANY_UPDATE` and do not check observer |
| Approval decide | Team pulse shows the action | `assertCanApproveCompanyFinance`: OWNER/ADMIN. Test proves MEMBER receives 403 |

---

## 12. B1 readiness

Reuse, do not rewrite from scratch:

- `BusinessActiveTheme` / `BusinessActiveTheme.swift` — family labels and `businessHubTiles` / `hubTiles` already encode small shop vs growing.
- `BusinessActionRegistry` — destination mapping and runway-only revenue/invoice. Change fail-open only with a test, not by deleting the object.
- `CompanyLifeActiveContent` / `BusinessLifeActiveView` — the one Life body. Leave it out of the Pulse/Create pass.
- Khata sheets — live parties and entries. Wire their save into the same refresh token if Create starts from Khata. Do not replace the sheet.
- Setup wizard and `IndustryTemplateCatalog` — live company and moment create. Create redesign is the in-moment hub, not the empty-state wizard.
- Server writers for expense, revenue, invoice, issue, update, approval, memory, khata — keep the contracts.

Do not delete yet:

- The three Pulse, Moments, and Memory trees, or the shared fallbacks.
- Gap host, vendor operations screen, location flow.
- Coming Soon cards and the empty Memory chooser (they are the only growing-path placeholders; removing them without a replacement changes the chooser).
- `POST /business/setups/:family/activate` — live and tested indirectly via `createMoment`, unused by clients.

B1 Pulse should be an attention list fed by issues, approvals, and activity, with the health ring demoted or removed from the hero. B1 Create should be one hub reading the existing tile catalog. Neither pass should invent a new score API.

---

## 13. B1 risk register

| # | Finding | Evidence | Severity | Platform | B1 without a fix? | Phase |
|---|---|---|---|---|---|---|
| R1 | Pulse, Life, and Memory projections are one row per company. The last moment refresh overwrites `financial_health_score` and the memory payload. A Pulse redesign that labels the ring “this moment” will be wrong whenever two families exist | `business-projection.ts` `ON CONFLICT (company_id)` for `projection.business_pulse` and `projection.business_memory`. Life read is `WHERE company_id = $1` in `projection/service.ts` | High | Both, from server | Layout-only B1 can proceed. Any copy that says this moment’s health or this moment’s memory cannot | B0.5 to confirm with two moments on device. SQL key change is later, not inside the visual pass |
| R2 | Health labels are client thresholds on a finance or SLA number. Team Pulse uses `financialHealthScore` and the words “Strong & Stable”. That is score theater, not “what needs attention now” | `RunwayPulseActiveContent`, `OpsPulseActiveContent`, `TeamOpsPulseActiveContent` and the Swift twins. Bands ≥80 / ≥50 (runway, team) and ≥90 / ≥70 (ops) | High | Both | B1 must change the hero. Do not wait for a new score API. The current number can stay as a secondary fact | B1 |
| R3 | Runway cash and burn depend on Life having been opened. Pulse load does not fetch Life. First paint is “Cash balance not added” even when setup prefs contain cash | Android `RunwayPulseActiveContent` `mapStr` / life cache. iOS `RunwayPulseActiveView` same. `BusinessTabLoad.fetchPulseTab` keeps previous life | High | Both | B1 can proceed only if the new Pulse either fetches the runway payload itself or does not show cash until that GET returns | B1 load fix. B0.5 if the visit-order bug needs a screenshot before design |
| R4 | Empty capabilities fail open to expense, revenue, invoice, members, vendor, issue, SLA. Group fails closed. A Create redesign that copies this will show Money tiles when bootstrap sent no capabilities. Server may still 403 | `BusinessActionRegistry.effectiveCapabilities`. Same in `BusinessActionRegistry.swift`. Comment: “fail open” | High | Both | B1 Create can proceed if it keeps today’s rule on purpose and does not describe hidden tiles as authorization. Changing fail-open is a product decision inside B1, with the existing unit test updated | B1 decision. Not a silent refactor |
| R5 | Client gating is not enforcement. Business has no viewer lock. Observers are denied for expense, revenue, invoice, memory, poll, and approval create. They are not denied for issues, vendors, decisions, risks, meetings, SLA, locations, or business updates | `resolver.ts` `OBSERVER_DENIED_ACTIONS`. `createBusinessUpdate` has no governance call. Shell `viewerReadOnly` is Group-only | High for a role-aware Create. Medium for an owner-only visual pass | Server, both clients | Owner-path B1 can proceed. Do not ship “viewers cannot record” as a feature until the deny list matches the hub | Later for the deny list. B1 must not claim the UI hide is the control |
| R6 | After save, the business cache is not invalidated. Only the visible tab refetches, and it paints the stale cache first. Khata save does not bump the token. SSE does not reach other members | `invalidateMoment` has no call sites. `refreshVisibleBusinessTab` skips prefetch when pulse is warm. `publishProjectionUpdated` filters `userId === actor`. Khata `onSaved` reloads the sheet only | Medium | Both | Single-user B1 layout can proceed. Do not add new Pulse cards that assume instant cross-tab or cross-user freshness | B0.5 to watch the stale flash. Cache invalidation can ride along with B1 only if it stays a bugfix and does not expand scope. Fan-out is later |
| R7 | Activity used for Pulse attention and parts of Moments is the caller’s rows only. Life’s activity query is company-wide. The two tabs will disagree | `getBusinessMomentActivity` `WHERE user_id = $1`. Life activity query in `getBusinessMomentProjection` joins company moments with no user filter | Medium | Server, both clients | B1 can list issues and approvals (those GETs are company/moment scoped). Do not build “what happened” solely from `.../activity` | B1 choice of data source. Activity fan-in is later |
| R8 | No finance, pulse, or life query filters by `location_id`. Small shop can still POST more than one location. Multi-store totals are company-wide | `business-finance.ts` has no `location_id`. `CoLocationsForm` still shows add-location for small shop | Medium | Server and setup UI | B1 must not add a location switcher on Pulse. Setup copy can stay | B0.5 to see the extra-location row. Location-scoped totals are later |
| R9 | Memory intelligence on the growing path is empty on purpose (pattern 0, playbook 0, “Coming soon”, `"—"`). Ops also invents `items.size / 3` | `refreshBusinessMemoryProjection` writes `pattern_count` 0, `playbook_count` 0. Family memory composables `if (!smallShop)` | Medium for Memory. Low for Pulse/Create | Both | Pulse/Create B1 can proceed. Do not delete the placeholder cards in that pass | Later (Memory phase) |
| R10 | Create Memory tab is an empty list with no API, while the in-moment Memory tab and the Memory quick add are live | `BusinessCreateMomentContent` `memories = emptyList()` | Low | Both | Leave the chooser tab alone in B1, or point it at GET memory in a later pass. Do not treat it as the memory system | Later |
| R11 | `GET .../actions` marks every action enabled. The hub does not call it. Wiring Create to this facet would ignore real capabilities | `getBusinessMomentProjection` facet `actions`: `enabled: true` | Medium if someone “hooks up actions” during B1 | Server | Do not call this facet from the new Create. Keep `BusinessActionRegistry` plus server 403 | B1 constraint. Facet fix is later |
| R12 | Poll create is business-authorized. Poll list is `assertGroupMember` only | router GET `/group/moments/:momentId/polls` | Low | Server | Leave poll as a team tile. Do not build a business poll inbox on that GET | Later |
| R13 | iOS Runway attention severity is hardcoded HIGH then MED. Android reads payload when present | `RunwayPulseActiveView` | Low | iOS | Fix only if B1 keeps a severity chip. Otherwise drop the chip | B1 if the chip stays. Otherwise later |
| R14 | Share-link insert failure still returns a token and says persist after V055 | `createShareLink` in `business-closure-reads.ts` | Low | Server | Out of Pulse/Create scope | Later |

---

## Tests

Recorded 2026-09-30 on this Windows host. Local Postgres was available. Nothing was skipped.

| Suite | Result |
|---|---|
| `backend/typescript/tests/business-s4-finance.test.ts` | Pass. Expense projection, approval MEMBER 403 / OWNER 200, revenue+invoice isolation, non-member pulse 403 |
| `backend/typescript/tests/business-company-life.test.ts` | Pass. Life KPIs, share-link, weekly-report |
| `backend/typescript/tests/business-ops-three-layer-join.test.ts` | Pass. Capability map, spend+vendor+issue+SLA pulse |
| Combined `tsx --test` of those three files | 9 passed, 0 failed, 0 skipped |
| `apk` `BusinessActionRegistryTest` (`com.example.momentra.ui.shell.business.shared`) | Pass. 5 tests, 0 failures |
| iOS Business tests | Not run. This host has no iOS toolchain |

A passing suite does not cover R1 (two moments, one pulse row), R3 (cash before Life), R6 (Khata and hidden tabs), or R5 (observer on issue/update). Those need B0.5 or a new test later. They are not reasons to start a redesign of the three screen trees inside B0.

## Stop

B0 ends here. Next decision is whether B0.5 runs for every flow or only for R1, R3, R5, R6, and R8, and whether B1 Pulse/Create starts against the owner single-moment path with the constraints in section 12.
