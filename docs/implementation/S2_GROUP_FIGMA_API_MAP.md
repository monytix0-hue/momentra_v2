# S2 Group — Figma ↔ API Map

**Figma:** [momentra / Group setup](https://www.figma.com/design/TzLvwVwlPbeVB8ug1zB3GM/momentra?node-id=169-487)  
**Primary Trip setup frame:** `575:9761` — Android / Group / Setup / Shared Experience (~402×3965 long-form: Basics → Dates/Budget/Split → People/Notifications → Summary + Activate). Preference rows beyond title/dates/people are **LOCAL_ONLY**.

### Experience long-form fields (`575:9761`)

| Section | Fields | Create API |
|---------|--------|------------|
| 01 Basics | Type chips, name, primary goal, destination, dates | `momentTypeCode`, `title`, `startAt`/`endAt` |
| 02 Budget & Split | Destination, dates, budget (presets + **Custom…**), currency, split, multi-currency, payment rhythm | `groupSetup.budgetAmount`, `groupSetup.budgetCurrencyCode`, `groupSetup.destinationText` on create; split/multi-currency prefs **LOCAL_ONLY** |
| 03 People | People list + invite, join approval, notify toggle, reminders, cadence | `participants[]`, invite mint; prefs **LOCAL_ONLY** |
| 04 Summary | Summary + Activate | `POST /v1/moments` |

Purchase (`575:11376`) and Living (`634:13345`) follow the same pattern with family-specific preference rows (all non-title/people prefs **LOCAL_ONLY**).

Do not invent fields. Gaps are tagged.

---

## S2G — Group Trip active tabs (post-setup)

| Figma node | Tab | UI files | Live API |
|------------|-----|----------|----------|
| `575:14165` | Pulse | `GroupPulseActiveContent.kt`, `GroupPulseActiveView.swift` | `GET /v1/group/moments/{id}/pulse`, `/finance`, `/activity`; budget from `budgetTotal` |
| `575:14327` | Moments | `GroupMomentsActiveContent.kt`, `GroupMomentsActiveView.swift` | People + budget metrics live; itinerary/gallery/events **API_GAP** |
| `575:14470` | Memory | `GroupMemoryActiveContent.kt`, `GroupMemoryActiveView.swift` | Budget reflection live; timeline/gallery **API_GAP** / empty |
| `575:14655` | Quick Add | `GroupQuickAddHub.kt`, `GroupQuickAddHubView.swift` | Expense, Contribution, Invite, Budget (PATCH); Planning/Booking/Poll/Memory/Update **API_GAP** |

Budget edit: `PATCH /v1/group/moments/{momentId}/budget` (idempotent).

---

## S2A — Trip setup → Create

| Figma field / step | Request property | Validation | Backend | Storage | Event / projection |
|--------------------|------------------|------------|---------|---------|-------------------|
| Experience Type = Trip | `momentTypeCode` = `TRIP` | Must exist in `core.moment_type` domain `GROUP` | `momentService.createMoment` | `core.moment` + `collaboration.group_moment_context.group_family` = Shared Experience family | `MomentCreated` + outbox |
| Domain | `domainCode` = `GROUP` | Enum | Same | Domain column | Projection hints for Group |
| Trip / Experience name | `title` | Required string | Same | `core.moment.title` | Inventory list title |
| Optional notes | `description` | Optional | Same | Moment description | — |
| Date range | `startAt`, `endAt` | ISO timestamps | Same | `collaboration.shared_experience_context` | — |
| Timezone | `timezone` | Client default | Same | Moment timezone | — |
| People (non-organizer) | `participants[]` `{displayName,roleCode,email,phone}` | Role ≠ inventing ORGANIZER for guests | Same | `moment_participant` + optional `external_party` | Membership rows |
| Invite link (Add People) | Pre-create: `POST /v1/group/invites` `{title,momentTypeCode}` → `inviteCode` | Mint schema strict | `inviteService.mintInvite` | `collaboration.moment_invite` | Audit via command |
| Bind invite on activate | `inviteCode` on create | GROUP-only | `bindInviteToMoment` | Invite → moment_id | Redeem can complete membership |
| Activate Experience CTA | `POST /v1/moments` Idempotency-Key | Command envelope | Live `router.ts` | Tx: moment + context + organizer + participants + audit + event + outbox | Client selects new moment; `GET /v1/group/moments` refresh |

**Post-create UX (required):** authoritative Moment returned → inventory scoped refresh → newly created Moment selected → Group shell → only visible tab loaded (no global shell rewrite).

---

## S2B — Participants / invitations

| Capability | Contract | Status |
|------------|----------|--------|
| Mint invite | `POST /v1/group/invites` | Live (runtime; **OpenAPI gap** — not in published GroupAPI docs) |
| Preview invite | `GET /v1/group/invites/:code` | Live |
| Redeem invite | `POST /v1/group/invites/:code/redeem` | Live |
| Organizer membership on create | Implicit in `createMoment` | Live |
| Participant inventory API | Not a dedicated OpenAPI list in live router | **API_GAP** / use moment get + SQL participants |
| Role / permission matrix | Governance resolver exists; full Group RBAC product table incomplete | **NEEDS_PRODUCT_DECISION** |
| Remove participant | Not mounted as live command | **API_GAP** |

Security boundary (architecture): User → Moment Membership → Role → Permission → Resource → Action. Never UI-only authz.

---

## S2C — Group Expense + Splits (Figma Quick Add)

Approximate Figma flow: Quick Add → Expense → Amount → Paid By → Participants → Split Method → Details → Save.

| Concept | OpenAPI | Live write | Tag |
|---------|---------|------------|-----|
| Expense amount / currency | `ExpenseCreateRequest` | PERSONAL-only service | **API_GAP** for GROUP domain |
| Split strategies EQUAL / PERCENTAGE / EXACT / SHARES | `ExpenseCreateRequest.splits[].strategy` | Explicitly deferred in `finance/service.ts` | **API_GAP** / **NOT_SUPPORTED** live |
| Paid by | Not fully mapped in live create | — | **API_GAP** |

Do **not** simulate splits only on Android/iOS.

---

## S2D — Obligations / Settlements

| Concept | FRD / SQL | Live API | Tag |
|---------|-----------|----------|-----|
| Obligation / settlement tables | Referenced in FRD finance mapping | No mounted settlement commands | **API_GAP** |
| Participant balances | Projection expected later | Not live | **API_GAP** |
| Simplified debts | Product later | — | **API_GAP** |

Backend remains source of truth; no local fake balances.

---

## S2E — Group Activity + Pulse

| Surface | Expected | Live | Tag |
|---------|----------|------|-----|
| Group Activity keyset | Mirror Personal `(occurred_at, id)` | Personal only on live router | **API_GAP** for Group timeline |
| Group Pulse GET | Projection-first | Facet stub payload via `getGroupMomentProjection` | **PARTIAL** |
| Mutation → bounded delta | Same pattern as Personal expense bump | Not for Group finance yet | Blocked on S2C |

---

## S2F — Remaining types (same engine)

Prefer type configuration over new services.

| Family (Figma) | Types (catalog) | Create path |
|----------------|-----------------|-------------|
| Shared Experience | Trip, Wedding, House Party, Office Outing | `POST /v1/moments` + type code |
| Shared Purchase | Gift Pool, Group Purchase, Shared Asset, Custom | Same |
| Shared Living | Flatmates, Family Household, Co-living, Custom | Same |

Each subtype must expose only supported Quick Adds once capability matrix is authoritative.
