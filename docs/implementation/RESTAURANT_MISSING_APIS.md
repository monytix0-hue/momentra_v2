# Restaurant section — missing APIs

Date: 2026-11-01. Companion to the restaurant rebuild against Figma `2487:37611`
(`resuarant`: `RST-PULSE-v12`, `RST-HUB-v5`, `RST-MOMENTS-v12`, `RST-LIFE-v12`,
`RST-MEMORY-v12`, `RST-SETUP-v6`, `RST-MASTER · *`, `RST-FORM · *`, `RST-QA-*`,
`RST-SHEET · *`, `RST-PICKER · *`, `RST-STORY-CONFIG`).

Seventy-two frames were rebuilt on the new Ember/Stone palette. This file lists the frames
that **cannot fully function on today's backend**, the exact endpoint each one needs, and
what the shipped screen does instead. Nothing here is a client bug: each gap is a missing
server capability, and each fallback was chosen so a screen never invents a number (see the
honesty rules at the end).

Live surface today, from `backend/typescript/src/api/v1/router.ts`:

| Capability | Route | Used by |
|---|---|---|
| Operating projection | `GET /v1/business/moments/:momentId/operating` | Pulse, Moments, Life, Memory, Hub |
| Post activity | `POST /v1/business/moments/:momentId/operating-events` | POS, KOT, all 10 Quick Add sheets |
| Revise activity | `PATCH /v1/business/moments/:momentId/operating-events/:eventId` | row edit, KOT Fire |
| Approve / reject | `POST .../operating-events/:eventId/decision` | approval sheet |
| Facets | `GET .../pulse`, `.../life`, `.../memory`, `.../finance`, `.../actions` | tab bodies |
| Company list | `GET /v1/companies` | company scope |
| Company detail / patch | `GET` / `PATCH /v1/companies/:companyId` | business profile |
| Locations | `GET` / `POST /v1/companies/:companyId/locations`, `PATCH .../locations/:locationId` | setup location |
| Members | `GET` / `POST /v1/companies/:companyId/members` | People board |
| Vendors | `GET` / `POST /v1/companies/:companyId/vendors`, `PATCH .../vendors/:vendorId` | Vendors tab |
| Invites | `POST /v1/company/invites`, `GET` / `POST .../invites/:code` | Invite & QR |
| Business moments | `GET /v1/business/moments` | Switch Moment, invite company resolution |
| Moment detail | `GET /v1/moments/:momentId` | moment identity |
| Story | `GET /v1/moments/:momentId/story`, `POST .../stories`, `GET .../booklet.pdf`, `GET .../share-pack` | Memory story CTA |

There is **no** menu/dish table, no station table, no table/section layout, no covers
field, no POS register, no KOT ticket table, no kitchen printer binding and no bill-number
allocation anywhere under `backend/typescript/src/modules/business`. Restaurant facts exist
only as `business.operating_event` rows that the projection folds into `finance`, `boards`,
`life`, `memory`, `attention` and `masters` (`operating-catalog.ts`, subtype
`RESTAURANT_FOOD`).

---

## 1. Unsupported screens

### 1.1 The strict operating contract has no covers, table or section

| | |
|---|---|
| Frames | POS header `2446:29655`, `2446:29802`, `2448:29655`; KOT `2449:29655`; Pulse hero `2400:29655` (`Covers 86`) |
| Needs | `covers`, `tableId`, `section` on `recordOperatingEventSchema` / the sale line; a `tables` master with occupancy and a merge/split model |
| Today | `validateSale` in `operating-retail-contract.ts` accepts `entryMode`, `paymentState`, `receivedAmount`, `quantityReceived`, `stockAction`, `stockReason`, `expenseCategory`, `orderStatus`, `taxMode`, `taxAmount` and the FX/return/production block. There is no covers, table or section field anywhere on `.strict()`. |
| Behaviour | The `Table`, `Covers` and `KOT` chips render **disabled** with the reason printed under them (`The strict contract has no table, section or covers field`). The bill still posts — the field simply is not part of the record. |

### 1.2 Tender split across cash + UPI + card

| | |
|---|---|
| Frames | POS tender row `2446:29655` (`UPI | Cash | Card | Split`), `2446:29802` |
| Needs | `tenders[]: { mode, amount }` on the sale, summed to the bill amount, each with its own settlement reference |
| Today | One bill carries exactly one `paymentState` (`RECEIVED | PENDING | PARTIAL`). `receivedAmount` is a single scalar. |
| Behaviour | The three tender chips write the single `paymentState`; `Split` stays visible and **disabled** with `One bill carries one payment state — a split tender has nowhere to land.` Recording two settlements still works the honest way: post the bill, then post a `COLLECTION` against it. |

### 1.3 GST and service charge on the ticket

| | |
|---|---|
| Frames | POS totals block `2446:29655` (`Subtotal | GST 5% | Service | Total`) |
| Needs | A per-bill service charge and a client-computed tax preview |
| Today | `stampPostedGst` (`operating-gst.ts`) stamps GST server-side from each line's dish rate; the client cannot set the bill's tax total. There is no service-charge field at all. |
| Behaviour | `GST` and `Service` rows print `Unavailable` in the warning colour with the reason (`Stamped server-side from each dish's rate`, `No service-charge field on the contract`). `Payable now` shows the real subtotal the lines add up to — never `₹0` and never a fabricated tax figure. |

### 1.4 Dish price on the master

| | |
|---|---|
| Frames | `RST-MASTER · Menu` `2465:30143`, `RST-FORM · Menu Dish` `2461:29655` (`Price`) |
| Needs | `price` (and `mrp` / variant pricing) on the master payload, plus a rate-card read path |
| Today | The master payload persists `name`, `unit`, `dimension`, `conversionToBase`, `barcode` and the full GST block (`gstApplicable`, `gstClassification`, `gstRate`, `salePriceIncludesGst`, `purchasePriceIncludesGst`, `gstEffectiveFrom`, `hsn`, `gstin`, states). There is no price column. |
| Behaviour | Menu rows show the GST rate badge and the reason `Dish price is typed on each bill line — the master carries a GST rate, not a rate card.` The POS and KOT sheets therefore let the operator type the unit price on the line; the backend derives the bill amount from `lines[].unitPrice`, which is a real field. |

### 1.5 Menu / Station master CRUD independent of an event

| | |
|---|---|
| Frames | `RST-MASTER · Menu` `2465:30143`, `· Stations` `2465:30265`, `· Vendors` `2465:30021`; forms `2461:29655`, `2461:29777`, `2472:29659`, `2472:29750` |
| Needs | `GET/POST/PATCH /v1/companies/:companyId/menu-dishes`, `.../kitchen-stations` (or the same on the moment), with edit and archive |
| Today | Rows come from `operating.masters` filtered to `kind ∈ {MENU_DISH, DISH, MENU_ITEM}`, `{STATION, KITCHEN_STATION}` and `{VENDOR, SUPPLIER, PRINCIPAL, PARTNER}`. That list is **derived** in `operating-catalog.ts` from events that already carried a master payload, so a menu is empty until the first bill or ticket. |
| Behaviour | `New dish` / `New station` / `New vendor` open the shared capture sheet in `MASTERS` mode, which posts an `UPDATE` event carrying `master` — the documented way to introduce a master ahead of a transaction. There is no edit or archive action, because a derived row cannot be mutated independently of the event that produced it. |

### 1.6 KOT ticket identity, station-per-line routing and reprint

| | |
|---|---|
| Frames | `RST-QA-01-ALT · KOT · Send` `2449:29655`, `· Fire` `2449:29769`, `· Reprint` `2449:29859` |
| Needs | A ticket entity (`kotId`, monotonically numbered per station), `stationId` on each line, and `GET .../kots/:kotId/ticket.pdf` |
| Today | An `ORDER` event exists (`orderStatus`, `lines[]`, `note`, `channelName`), so **Send** is real: it posts `ORDER` with `orderStatus = PLACED` and the itemized lines. **Fire** is real too: it PATCHes the same event to `PREPARING` via `revisesEventId`, so the kitchen never sees two tickets. |
| Behaviour | Station routing persists as a `STATION` master and is echoed into the ticket note (`Station: Tandoor`); the stations list derives each station's open-ticket count by matching that note (or the channel) against `ORDER` rows whose status is not `COMPLETED`/`CANCELLED` — genuinely computable, not estimated. `Reprint` is **absent**, not mocked: there is no ticket PDF endpoint, and inventing a client-side ticket image would not match what the kitchen printed. |

### 1.7 Menu mix aggregate for story page 3

| | |
|---|---|
| Frames | Story page `P3` (menu mix) inside `RST-STORY-CONFIG` `2671:32352` |
| Needs | `GET /v1/moments/:momentId/story/menu-mix` — per-dish revenue and covers over the window |
| Today | The story projection folds `finance`, `boards`, `life` and `memory`; no board exposes a per-dish aggregate, and `lines[]` is stored per event without a roll-up endpoint. |
| Behaviour | Every other story page is served by the existing viewer. Page 3 prints its own empty state ("Nothing recorded yet") instead of guessing a top dish. |

### 1.8 Story config controls

| | |
|---|---|
| Frame | `RST-STORY-CONFIG` `2671:32352` (period chips, `Summary | Detailed`, `Include GST snapshot`) |
| Needs | `POST /v1/moments/:momentId/stories` accepting `{ period, detail, includeGst }` |
| Today | The route takes **no body** and regenerates the moment's recorded window. |
| Behaviour | The controls stay visible (the frame shows them) but disabled with the reason printed: `Period follows the moment's recorded window — regenerate covers that range only.` and `Recorded GST is not part of this story build`. Generation routes through `OperatingChromeState.previewStoryId`, which the shell already turns into the story viewer. |

### 1.9 The three pickers

| | |
|---|---|
| Frames | `RST-PICKER · Date` `2521:29334`, `· Month` `2521:29458`, `· Due date & time` `2521:29515` |
| Needs | nothing new — the pickers are pure client surfaces |
| Behaviour | Rebuilt as `EmberPickerSheet` / `EmberPickerSheet.kt` with three modes. Every chip is real: `Today / Tomorrow / +7 days` resolve against the device calendar, the month grid follows the real calendar (Monday-first, leading blanks), and the time slots plus `Now / End of shift` resolve to actual clock times. The `Custom range ›` chip stays **disabled** with the reason `Not a field`, because no report endpoint accepts a range. |

### 1.10 Switch Moment

| | |
|---|---|
| Frame | `2524:29370` |
| Needs | nothing new. `GET /v1/business/moments` already returns `momentId`, `title`, `momentTypeCode`, `companyId`. |
| Behaviour | The shell's own switcher (`BusinessActiveMomentsDirectory` / `showMomentSwitcher`) covers this frame for both platforms, exactly as retail decided. The restaurant section does not re-implement it. |

### 1.11 Setup steps 02 and 04

| | |
|---|---|
| Frames | `RST-SETUP-v6` `2470:29187` — section `04 Kitchen, Menu & Network`, section `02 People & Roles` |
| Needs | Menu/station creation during setup, and per-invite role selection backed by five roles |
| Behaviour | Setup itself is fully re-themed to the Ember palette (root `#1C1917`, cards `#292524`, ember `#EA580C`, gold `#FCD34D`). The channel list matches the frame (`Dine-in · Direct · Zomato · Takeaway · Swiggy`). The two master sections open the same real masters sheets described in 1.5, and `People & Roles` routes into `BusinessInviteQRView`, which offers the four schema-valid roles. |

### 1.12 Loading app states

| | |
|---|---|
| Frames | `RST-APP-STATE · Pulse / Moments / Life / Memory · Loading` |
| Needs | nothing new |
| Behaviour | `OperatingProjectionCache.peek(momentId)` paints first, else the tab's own skeleton rows. No separate full-screen loader, matching retail. |

---

## 2. Exact endpoints requested

If the server grows these, the listed screens can stop degrading:

```
PATCH  .../operating-events schema
       → add `covers`, `tableId`, `section`, `serviceCharge`          clears 1.1, 1.3
       → accept `tenders[]: [{ mode, amount }]` on a SALE              clears 1.2

GET    /v1/companies/:companyId/menu-dishes                          list with price, HSN, GST, station
POST   /v1/companies/:companyId/menu-dishes                          create ahead of any transaction
PATCH  /v1/companies/:companyId/menu-dishes/:dishId                  edit / archive / price change
GET    /v1/companies/:companyId/kitchen-stations                     stations with a live open-ticket count
POST   /v1/companies/:companyId/kitchen-stations                     create station
                                                                     clears 1.4, 1.5

POST   /v1/business/moments/:momentId/kots                           allocate a ticket number per station
PATCH  .../kots/:kotId                                               fire / ready / served
GET    .../kots/:kotId/ticket.pdf                                    reprint
POST   .../kots/:kotId/lines/:lineId/station                          per-line routing
                                                                     clears 1.6

GET    /v1/moments/:momentId/story/menu-mix                          per-dish revenue + covers
                                                                     clears 1.7

POST   /v1/moments/:momentId/stories
       → accept `{ period, detail, includeGst }`                     clears 1.8
GET    /v1/moments/:momentId/stories                                  version history
GET    /v1/moments/:momentId/stories/:storyId/progress               slide-level progress
```

---

## 3. What the shipped screens do instead

- **Never a fake zero.** Missing coverage prints `Unavailable`; the 92pt health ring in
  `Target & Budget` renders a bare track with no `0%` label when there is no configured
  whole. GST and Service on the POS totals block print `Unavailable` with the reason.
- **Never a fake row.** Empty boards say "Nothing recorded yet" / "Not available"; the menu
  says rows only appear on an explicit master record; the pass says "Nothing in the pass"
  and tells the operator to send a ticket first.
- **Never a placeholder QR.** Invite & QR mints a real invite
  (`POST /v1/company/invites`), draws the QR from the returned `inviteCode` with the shared
  QR helper, and shows "QR code needs an invite token" until that succeeds. Contacts are
  the device's own; `Send` opens WhatsApp/Messages through the existing invite helpers.
- **Role chips match the schema.** The Figma invite frame offers Owner / Manager / Cashier /
  Kitchen / Waiter; `mintCompanyInviteSchema` accepts only
  `FINANCE | APPROVER | OPERATIONS_LEAD | CONTRIBUTOR`, so only those four are offered.
  Minting additionally requires `OWNER_ADMIN` — the screen says so and hides the actions.
- **Orders are never revenue.** `Send` posts an `ORDER` (`PLACED`); `Fire` patches it to
  `PREPARING`. Only a posted `SALE` moves `finance.sales`, which is exactly what the frames'
  own annotation says.
- **Disabled, with the reason printed.** Table / covers / KOT, `Split`, `Service`, the
  story period / detail / GST controls, `Custom range` and `Reprint` all render disabled
  and state why. None of them silently does nothing.

---

## 4. Verification performed

- Android: `./gradlew :app:compileDebugKotlin` — clean, zero errors.
- iOS: the restaurant sources live inside the target's
  `PBXFileSystemSynchronizedRootGroup`, so no project-file edit is needed. Every symbol they
  use exists in the target today — verified by declaration lookup:
  `OperatingProjectionPayload` / `OperatingMasterPayload` / `OperatingActivityPayload` /
  `OperatingContractInput` (`APIClient.swift:89-217`), `recordOperatingEvent` with
  `lineItems` and `revisesEventId` (`APIClient.swift:5362`), `createMomentStory`
  (`APIClient.swift:1622`), `APIClient.mintCompanyInvite`, `CompanyJoinLink`,
  `GroupQRCode.image`, `GroupContactsLoader`, `InviteOutboundShare`, `ActivityShareSheet`,
  `businessInviteRoleLabel` / `businessInviteMembership`, `OperatingChromeState`.
- Token completeness: every `RestaurantEmber.*` reference resolves against
  `RestaurantEmberTokens.swift` / `RestaurantEmberTokens.kt`; every `RestaurantFormat.*`
  reference resolves against `RestaurantFormat.swift` / `RestaurantFormat.kt`.
- Asset completeness: 27 vectors on each platform — `Assets.xcassets/RestaurantEmber/*`
  (14 tile icons + 5 tab icons + 8 chrome icons, including `RestaurantRingTrack` /
  `RestaurantRingProgress`) and `res/drawable/ic_rest_*` (same 27).
- Palette: no `#101412`, `#F59E0B` or `#F2B544` survives in the restaurant folders or in
  the `RESTAURANT_FOOD` setup spec.
- Picker correctness: the month grid is derived from `Calendar.range(of:)` (iOS) /
  `YearMonth.lengthOfMonth()` (Android) with Monday as the first column, so no fabricated
  day ever appears.
