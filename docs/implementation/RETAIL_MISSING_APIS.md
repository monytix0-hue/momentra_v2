# Retail section — missing APIs

Date: 2026-10-31. Companion to the retail rebuild against Figma `2487:37758`
(`RTL-PULSE-v12`, `RTL-HUB-v5`, `RTL-MOMENTS-v12`, `RTL-LIFE-v12`, `RTL-MEMORY-v12`,
`RTL-MASTER · *`, `RTL-FORM · *`, `RTL-QA-*`, `RTL-SHEET · *`, `RTL-PICKER · *`, `RTL-STORY-*`).

Seventy frames were rebuilt. This file lists the frames that **cannot fully function on
today's backend**, the exact endpoint each one needs, and what the shipped screen does
instead. Nothing here is a bug in the client: each gap is a missing server capability, and
each fallback was chosen so the screen never invents a number (see the honesty rules at the
end).

Live surface today, from `backend/typescript/src/api/v1/router.ts`:

| Capability | Route | Used by |
|---|---|---|
| Operating projection | `GET /v1/business/moments/:momentId/operating` | Pulse, Moments, Life, Memory, Hub |
| Post activity | `POST /v1/business/moments/:momentId/operating-events` | all 10 Quick Add sheets |
| Revise activity | `PATCH /v1/business/moments/:momentId/operating-events/:eventId` | row edit |
| Approve / reject | `POST .../operating-events/:eventId/decision` | approval sheet |
| Facets | `GET .../pulse`, `.../life`, `.../memory`, `.../finance`, `.../actions` | tab bodies |
| Company list | `GET /v1/companies` | company scope |
| Company detail / patch | `GET` / `PATCH /v1/companies/:companyId` | business profile |
| Locations | `GET` / `POST /v1/companies/:companyId/locations`, `PATCH .../locations/:locationId` | company locations |
| Members | `GET` / `POST /v1/companies/:companyId/members` | People board |
| Vendors | `GET` / `POST /v1/companies/:companyId/vendors`, `PATCH .../vendors/:vendorId` | Suppliers tab |
| Invites | `POST /v1/company/invites`, `GET` / `POST .../invites/:code` | Invite & QR |
| Business moments | `GET /v1/business/moments` | Switch Moment, invite company resolution |
| Moment detail | `GET /v1/moments/:momentId` | moment identity |
| Story | `GET /v1/moments/:momentId/story`, `POST .../stories`, `GET .../booklet.pdf`, `GET .../share-pack` | Memory story CTA |

There is **no** product/SKU table, no stock ledger, no order header/line table, no POS
register, no receipt table and no purchase-order table anywhere under
`backend/typescript/src/modules/business`. Retail facts exist only as
`business.operating_event` rows that the projection folds into
`finance`, `boards`, `life`, `memory`, `attention` and `masters`
(`operating-catalog.ts`).

---

## 1. Unsupported screens

### 1.1 Catalogue master — `2506:29475`

| | |
|---|---|
| Needs | `GET /v1/companies/:companyId/products` (list with price, HSN, GST rate, category, UoM) |
| Today | Rows come from `operating.masters` filtered to `kind ∈ {PRODUCT, ITEM, SKU, MATERIAL, CONSUMABLE}`. That list is derived in `operating-catalog.ts:1343` from events that **already carried** a master payload, so a catalogue is empty until something has been sold, bought or stocked. |
| Behaviour | Tab lists real master rows with `UOM`, `GST n%`, `Scan` chip. Empty state says catalogue rows only appear on an explicit master record. `New product` opens the shared capture sheet in `MASTERS` mode, which posts an `UPDATE` event carrying the master payload — the documented way to introduce a product ahead of a transaction. |
| Gap | No way to page, search or edit a catalogue independent of activity; no MRP, no category tree, no variant list, no aisle/bin. |

### 1.2 Create Product / SKU — `2506:29782`

| | Needed | Present |
|---|---|---|
| Product name, category, variants, sell price, MRP, HSN, GST rate, GST-inclusive | `POST /v1/companies/:companyId/products` | no such route |
| Image / pack | media upload bound to a product | no such binding |

Shipped as the capture sheet's `MASTERS` path (`masterKind = PRODUCT`), which does persist
name, unit, dimension, conversion, barcode and the full GST block
(`gstApplicable`, `gstClassification`, `gstRate`, `salePriceIncludesGst`,
`purchasePriceIncludesGst`, `gstEffectiveFrom`, `hsn`, `gstRegistration`,
`businessState`, `billingState`, `deliveryState`, `gstin`). Category, variant and MRP have
no field on the schema and are **not** captured — the sheet does not render inputs it
cannot save.

### 1.3 Create Stock Item — `2506:29907`

Same as 1.2 with `masterKind = MATERIAL | CONSUMABLE`. Opening stock, reorder point,
bin/aisle and "tracked" movement history need a stock ledger; only `tracked` (boolean) is
persisted today.

### 1.4 POS Sale — `2421:29655`, `2439:29655` (quick amount)

Needed: an open register/shift, a cart that holds lines, per-line SKU + quantity +
price, a tender step, a posted bill, receipt numbering and a stock movement per line.

Present: `POST .../operating-events` with `eventType = "SALE"`, `lines[]` (max 12, each
`itemName`, `quantity`, `unit`, `unitPrice`, `masterId`, `baseQuantity`, `discount`,
`taxMode`, `gstRate`). The Quick Add sheet rebuilds the Figma capture layout on that
contract: `Quick amount` posts an amount only (and by doctrine **never** moves stock),
`Scan / itemized` posts `lines`.

Not shipped on the client, and impossible server-side today: register/shift identity,
cart persistence, tender split (cash + UPI + card on one bill), receipt number allocation,
fiscal device, cash-drawer reconciliation. The Figma status strip
(`Register 2 · Shift open · Andheri`) is therefore rendered from the moment's
`preferences.location`, not from a register.

### 1.5 POS Hold / recall — `2438:29655`, Park bill — `2508:29333`

Needed: `POST /v1/business/moments/:momentId/pos/holds`, `GET .../pos/holds`,
`DELETE .../pos/holds/:holdId`.

Present: nothing. A parked bill is not an operating event — it is a draft with no
revenue and no stock, and the projection has no draft concept.

Behaviour: the `Sale` tile's `Hold` action is **absent** rather than a button that
discards a cart. If a cart must survive a shift today, the operator records the sale or
does not start it.

### 1.6 Stock transfer — `2508:29447`

Needed: `POST .../stock-transfers` with `fromLocationId`, `toLocationId`, `lines[]`,
`gstin`, and paired ledger movements.

Present: `eventType = "STOCK"`, `quantity`, `unit`, `itemName`, `note`. That records a
single-location count/damage adjustment. A transfer between locations has no `from`/`to`
field in `recordOperatingEventSchema`, so a transfer would silently read as an adjustment
at one place. Not offered.

### 1.7 Reprint receipt — `2508:29537`

Needed: `GET .../operating-events/:eventId/receipt.pdf` (or a stored bill number).

Present: events carry `title`, `amount`, `lines[]`, `gstRate`, `occurredAt` — enough to
render a tax invoice client-side, but there is no allocated bill number, no immutable
posted snapshot, and no PDF. The `Print`/`Reprint` action is not present; the row opens
the event's detail in the board sheet instead.

### 1.8 Purchase Order — `2443:29784`

Needed: `POST /v1/companies/:companyId/purchase-orders` with supplier, line items,
expected date, status (`DRAFT → SENT → PARTIAL → RECEIVED`), and goods-receipt linkage.

Present: `eventType = "ORDER"` (`parentEventId` links a child), `counterpartyName`,
`dueAt`, `quantity`, `unit`, `lines[]`. The purchase tile posts an `ORDER` with
`orderSide` semantics, which the projection treats as a **commitment**, never as a
payable (see `PAYABLE_TYPES` in `operating-catalog.ts`). Status transitions use the
`Status / Assignment` sheet on the parent order. There is no receive-against-PO step, so
stock only changes on an explicit `PURCHASE`/`STOCK` event.

### 1.9 Masters forms — `2506:29612` (Supplier), `2506:29703` (Location)

| Form | Today |
|---|---|
| Supplier | `POST /v1/companies/:companyId/vendors` exists and is wired elsewhere in the app (Gap screens). The retail **Suppliers** tab uses the masters capture path instead, because the vendor record is company-scoped while this tab is moment-scoped (`masters` is derived from events). |
| Location | `POST /v1/companies/:companyId/locations` and `PATCH .../locations/:locationId` exist. The retail Locations tab lists `kind ∈ {LOCATION, STORE, CHANNEL}` masters from the projection; it does not create a `business.company_location` row, because a company location does not group stock in the projection. |

So both forms have a working endpoint but the **retail tab cannot use it without a
company→moment mapping decision**: creating a vendor or location from a moment needs the
company id, and the projection does not expose one. `GET /v1/business/moments` does
(`companyId`), and the Invite tab now resolves it that way; the masters tabs would need the
same lookup plus a rule for keeping `company_location` and `operating.masters` in sync.

### 1.10 Pickers — `2521:29585` (date), `2521:29709` (month), `2521:29766` (date & time)

The three Figma picker sheets (custom calendar, month grid, time chips) are not rebuilt as
separate sheets. The capture sheet already opens the platform date picker
(`DatePicker(... .graphical)` on iOS, `DatePickerDialog` with `rememberDatePickerState` on
Android) for `occurredAt`, `dueAt` and `goodsDue`. Month ranges have no consumer: the story
endpoint takes a fixed window and no report endpoint accepts a month parameter.

### 1.11 Switch Moment — `2524:29431`

Needs: nothing new. `GET /v1/business/moments` already returns `momentId`, `title`,
`momentTypeCode`/`businessFamily`, `companyId`.

Present: the shell's own moment switcher (driven by `BusinessActiveMomentsDirectory` /
`showMomentSwitcher`) covers this frame for both platforms; the retail section does not
re-implement it. If the frame's exact rows matter, it is a shell change, not a retail one.

### 1.12 Story states — `2670:33840` (composing), `34354` (saved), `34395` (share)

| Frame | Today |
|---|---|
| Create story `2670:33783` | Rebuilt as `RetailStoryConfigSheet` / `RetailStoryConfigSheet.kt`. `POST /v1/moments/:momentId/stories` takes **no body** and regenerates the moment's recorded window. The period, detail and GST controls are visible but disabled with the reason printed, because honouring them would mean pretending the request carries options. |
| Composing `2670:33840` | The sheet shows a spinner + "Composing…" while the POST is in flight. There is no progress stream ("5 of 8 slides"): the endpoint returns a `storyId` only after the whole generation finishes. |
| Saved `34354` / Share `34395` | Generation sets `OperatingChromeState.previewStoryId`, which the shell already turns into the existing story viewer. Save/Share exist there. Version badges and "changed facts" need `GET /v1/moments/:momentId/story` history, which returns one current story. |

`GET .../story/booklet.pdf` and `.../share-pack` are live, so the PDF/export actions in
`34395` have a real backing route.

### 1.13 Loading app states — `2678:33247`, `33395`, `33543`, `33691`

Four Figma loading frames exist for Pulse / Moments / Life / Memory. The retail section
paints `OperatingProjectionCache.peek(momentId)` immediately (last projection for that
moment) and otherwise shows the tab's own skeleton rows; there is no separate
full-screen loading frame. No API needed — this is a design choice to keep the previous
numbers visible instead of a blank canvas.

---

## 2. Exact endpoints requested

If the server grows these, the listed screens can stop degrading:

```
GET    /v1/companies/:companyId/products                     list (price, HSN, GST, category, UoM)
POST   /v1/companies/:companyId/products                     create ahead of any transaction
PATCH  /v1/companies/:companyId/products/:productId           edit / archive
GET    /v1/companies/:companyId/products/:productId/stock     on-hand by location

POST   /v1/business/moments/:momentId/registers               open shift / register
POST   .../registers/:registerId/holds                       park a cart (no revenue, no stock)
GET    .../registers/:registerId/holds                       recall list
DELETE .../registers/:registerId/holds/:holdId                discard a hold
POST   .../sales                                             post a bill: lines + tender split
GET    .../sales/:saleId/receipt.pdf                          reprint

POST   /v1/companies/:companyId/stock-transfers               from/to location + lines
GET    /v1/companies/:companyId/stock-ledger?itemId=          movements per item

POST   /v1/companies/:companyId/purchase-orders               supplier + lines + expected date
POST   .../purchase-orders/:poId/receive                       goods receipt → stock movement
PATCH  .../purchase-orders/:poId                               status transition

GET    /v1/business/moments/:momentId/operating
       → add `companyId`, `locationId`                         so masters tabs can use the company endpoints
POST   /v1/moments/:momentId/stories
       → accept `{ period, detail, includeGst }`              so the config sheet's controls stop being disabled
GET    /v1/moments/:momentId/stories                           version history for the saved/share states
GET    /v1/moments/:momentId/stories/:storyId/progress         slide-level progress for the composing state
```

---

## 3. What the shipped screens do instead

- **Never a fake zero.** Missing coverage prints `Unavailable` (23 sites on iOS, 33 on
  Android) and the health ring renders a bare track with no `0%` label.
- **Never a fake row.** Empty boards say "Nothing recorded yet" / "Not available"; the
  catalogue says rows only appear on an explicit master record.
- **Never a placeholder QR.** Invite & QR mints a real invite
  (`POST /v1/company/invites`), draws the QR from the returned `inviteCode` with the shared
  QR helper, and shows "QR code needs an invite token" until that succeeds. Contacts are
  the device's own; the `Send` action opens WhatsApp/Messages through the existing invite
  outbound helpers.
- **Role chips match the schema.** The Figma invite frame offers Owner / Manager /
  Cashier / Stock / Floor; `mintCompanyInviteSchema` accepts only
  `FINANCE | APPROVER | OPERATIONS_LEAD | CONTRIBUTOR`, so only those four are offered.
  Minting additionally requires `OWNER_ADMIN` — the screen says so and hides the actions.
- **No register fiction.** POS-register, shift, tender and bill-number chrome from
  `2421:29655`/`2508:*` is intentionally absent rather than mocked.
- **Commitments stay commitments.** Order rows show status only; nothing rolls an order
  into revenue or a payable.

---

## 4. Verification performed

- Android: `./gradlew :app:compileDebugKotlin --rerun-tasks` — clean, zero errors and
  zero new warnings (JDK 21). `./gradlew :app:assembleDebug` packages the APK
  successfully, and `:app:processDebugResources` passes, so all 27 `ic_retail_*`
  vectors parse.
- Android wiring: `OperatingMomentScreens.kt` routes `RETAIL_STORE` to
  `RetailStoreActive`, which forwards `momentId` into the Quick Add hub so Invite & QR
  can resolve the company.
- iOS: the retail sources are standard SwiftUI inside the target's
  `PBXFileSystemSynchronizedRootGroup`, so no project-file edit is needed. Every
  symbol they use exists in the target today — verified by declaration lookup:
  `APIClient.mintCompanyInvite` (`APIClient.swift:2024`), `CompanyJoinLink`,
  `GroupQRCode.image`, `GroupContactsLoader.Row` / `.fetchRowsIfAuthorized`,
  `InviteOutboundShare.inviteMessage`, `ActivityShareSheet`,
  `businessInviteRoleLabel` / `businessInviteMembership`, `OperatingChromeState`.
- iOS wiring: `OperatingMoment.swift:295` routes `RETAIL_STORE` to
  `RetailStoreActiveView`, which passes `momentId` to both the Quick Add hub and the
  story config sheet.
- Token completeness: every `RetailRose.*` reference in the retail folders resolves
  against `RetailRoseTokens.swift` / `RetailRoseTokens.kt` (script cross-check, empty
  missing set). Same for `RetailFormat.*` / `RetailFormat` on both platforms.
- Asset completeness: every `Image("Retail…")` resolves to a folder in
  `Assets.xcassets/RetailRose`, and every `R.drawable.ic_retail*` resolves to a file in
  `res/drawable` (script cross-check, empty missing sets). No Figma asset URL remains
  in code.
- Palette: every former `#0B1020` / `#34D399` reference in the retail folders is gone
  (only the two token files mention them, in the comment naming what they replaced).
- No dangling references to the deleted `BusinessPulseEmptyView` /
  `BusinessMomentsEmptyView` / `BusinessLifeEmptyView` / `BusinessMemoryEmptyView`
  (iOS) or `BusinessPulseEmptyContent` / `BusinessMomentsEmptyContent` /
  `BusinessLifeEmptyContent` / `BusinessMemoryEmptyContent` (Android).
