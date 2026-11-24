# Business V4 AppState smoke checklist (Obsidian Emerald)

**SoT:** Figma `rknfN9EikSCdqmqXogR29o` · section `2:67`  
**Tokens:** `design/tokens/business.tokens.json` v4.1 → `BusinessTokens` (canvas `#05070B`, accent `#10B981`, CTA `#059669→#34D399`, + `madeToOrder`)  
**Chrome:** Unified Personal/Group/Business topbar + bottom nav — AppState owns **body only**.

Mark each cell after a device/simulator pass. Missing figures must never render as `0`.

| # | Frame | Node | Android | iOS | Notes |
|---|---|---|---|---|---|
| 1 | Pulse Empty (Trading) | `2:221` | ☐ | ☐ | Title `No pulse yet`; amber `Not recorded` |
| 2 | Moments Empty (Distribution) | `2:319` | ☐ | ☐ | |
| 3 | Life Empty (Services) | `2:417` | ☐ | ☐ | |
| 4 | Memory Empty (Manufacturing) | `2:515` | ☐ | ☐ | |
| 5 | Hub Quick Adds (Marketing) | `2:613` | ☐ | ☐ | Tiles + footnote; **no** invented primary CTA |
| 6 | NoCompany Setup (Retail) | `2:721` | ☐ | ☐ | Create / Join company |
| 7 | Pulse Loading Skeleton | `2:822` | ☐ | ☐ | Title `Loading Pulse…` (U+2026) |
| 8 | Pulse Error | `2:907` | ☐ | ☐ | Chip `PULSE_FETCH_FAILED`; Retry |
| 9 | Offline | `2:996` | ☐ | ☐ | Banner + `Waiting for connection` |
| 10 | Retail Empty (Rose) | `2:1093` | ☐ | ☐ | Accent Rose `#F43F5E` |
| 11 | RST Food Empty (Flame) | `2:1190` | ☐ | ☐ | Accent Flame `#EA580C` |
| 12 | View All (Cyan) | `2:1287` | ☐ | ☐ | Accent Cyan `#06B6D4` |
| — | Toaster & Banner | `2:1375` | ☐ | ☐ | Success / offline banner / error code atoms |
| — | Guided create | `2:1514` | ☐ | ☐ | 2×2 cards + preview + Continue → **setup shell** (not immediate submit) |

## Business setup shell (nine subtypes)

Continue from guided create opens `BusinessSetupShell` with in-document chips. Activate → `submitBusinessSetup`.

| Family | Chip | Type code | Node | Android | iOS |
|---|---|---|---|---|---|
| Marketing | Client | `CLIENT_CAMPAIGN` | `5:33148` | ☐ | ☐ |
| Marketing | Own business | `OWN_BUSINESS_CAMPAIGN` | `5:38325` | ☐ | ☐ |
| Trading | Trading | `TRADING_WHOLESALE` | `5:18833` | ☐ | ☐ |
| Trading | Distribution | `DISTRIBUTION_DEALERSHIP` | `5:8637` | ☐ | ☐ |
| Retail | Retail | `RETAIL_STORE` | `5:80797` | ☐ | ☐ |
| Retail | Restaurant | `RESTAURANT_FOOD` | `5:90612` | ☐ | ☐ |
| Retail | Service | `CONSUMER_SERVICE` | `5:71162` | ☐ | ☐ |
| Manufacturing | Manufacturing | `STANDARD_PRODUCTION` | `5:50728` | ☐ | ☐ |
| Manufacturing | Job order | `MADE_TO_ORDER` | `5:55863` | ☐ | ☐ |

Setup smoke (Figma v6 parity):

- [ ] Scroll through Basics → People & Roles → Network/Catalogue → Channels & Plan → GST → Review for subtypes that still include Network (not Retail Store / Distribution / Client Campaign / Own Business Campaign)
- [ ] CTA copy matches frame (`Start this business` for trading / distribution / client campaign / `Start this store` / `Start this restaurant`)
- [ ] Chip swap reseeds fields + rethemes without losing typed title when keys match
- [ ] Kind of business chips match Figma tiles (`◈`, border select, retail order Restaurant / Food → Consumer Service → Retail Store) — not capsule pills
- [ ] GST “No” / “Not now” hides registration type / state / GSTIN / pricing fields
- [ ] Review summary reflects current values (not seed placeholders only)
- [ ] Long scroll + sticky activate bar does not clip CTA glow
- [ ] Invite link + QR + share lives inside **02 People & Roles** (not a bottom SectionCard after Review)
- [ ] Activate creates moment (implicit company when needed)
- [ ] Edit / Manage moment opens same shell preselected to subtype
- [ ] Personal / Group create & chrome unchanged
- [ ] Date fields open calendar / month picker (not free-text only)
- [ ] Currency is a dropdown with INR default across all 9 setups
- [ ] GSTIN types in capitals (keyboard + paste)
- [ ] Business bottom sheets on **iOS and APK** (DateRow, Manage when Business, View-all) show glass + dim scrim; setup fullscreen covers stay opaque gradient

### Brand name label, live hero title, logo, keyboard (all 9 setups)

- [ ] Trading, Distribution, Consumer Service, Manufacturing, Job order show **Brand name** (not “Business name”)
- [ ] Retail Store still says **Store name**, Restaurant still says **Restaurant name**, Marketing still says **Campaign name**
- [ ] Marketing’s second field reads **Promoted brand / product**
- [ ] Typing in Brand name updates the hero to `{name} Setup` on every keystroke
- [ ] Clearing Brand name restores the Figma placeholder hero title
- [ ] Editing Company name alone leaves the hero title unchanged
- [ ] Tapping the hero circle opens the image picker; after upload the circle shows the logo
- [ ] Keyboard has a Done affordance and closes on scroll; sticky CTAs stay tappable while it is open
- [ ] Reopening a draft saved before this change still shows the saved name under **Brand name**

### Job order (`MADE_TO_ORDER` / `5:55863`)

- [ ] Scroll Basics → People & Roles → Job order · Quotes & order approval → GST → Activate (no Plan · Monthly section)
- [ ] Chip / eyebrow label is **Job order** (not Make to order)
- [ ] CTA is **Activate Moment**; secondary **Save draft** works
- [ ] Budget/target/channels live under Business Basics; invalid GSTIN highlights `gstin`
- [ ] Discard draft removes DRAFT moment

### Hero logo + live title (all 9 setups)
- [ ] Changing Business Basics primary name updates hero title immediately to `{name} Setup`
- [ ] Clearing the name restores the Figma placeholder hero title
- [ ] Tapping hero logo opens image picker; after successful upload the circle shows the logo (not buildings/`▣`)
- [ ] Save draft persists `logoMediaId`; reopen still has logo when URL/preview available

### Retail Store (`5:80797`) — five sections (no Catalogue/Network)

- [ ] Sections: Basics → People & Roles → Channels & Plan → GST → Review only (no Catalogue / Stock / Network rows)
- [ ] Hero **Harbor Mart Setup**; CTA **Start this store**
- [ ] Basics: store name, company name, GSTIN, currency, start/end, description, location
- [ ] Channels: Counter / Home / Online / B2B / Market + budget + sales target + measure
- [ ] Soft plan status pill under Channels (green when budget+target parse)
- [ ] **Save draft** creates or patches DRAFT; pill shows `Setup in progress`; reopen restores prefs
- [ ] **Discard draft** in header when status is DRAFT; leaves setup and removes draft
- [ ] Activate with empty channels highlights channels (danger border + message), form values kept
- [ ] Invalid GSTIN with tracking Yes highlights GSTIN; Save draft still succeeds

### Restaurant (`5:90612`) — six sections; §04 display-only

- [ ] Hero **Spice Route Setup**; CTA **Start this restaurant**
- [ ] Basics: restaurant name (`outletName`), company, GSTIN, currency, start/end, description, location
- [ ] Channels: Dine-in / Direct / Zomato / Takeaway / Swiggy + budget + sales target + measure
- [ ] Roles: Owner / Manager / Cashier / Kitchen / Waiter
- [ ] §04 Kitchen, Menu & Network rows (Menu / Stations / Vendors / Team invite) are visible, not masters sheets
- [ ] **Save draft** | **Schedule later**; reopen draft restores prefs
- [ ] Activate with empty channels highlights channels; invalid GSTIN with tracking Yes highlights GSTIN

### Trading Wholesale (`5:18833`) — five sections (no Catalogue/Network)

- [ ] After **Start this business**: animated ✓ toast **Moment activated** (~2.5s) then Pulse (`5:15203`)
- [ ] Trading hub uses TRD-v12 amber `#F59E0B` / flame `#EA580C` on canvas `#090D16` (Pulse / Moments / Life / Memory / Quick Adds)
- [ ] Pulse shows Target & budget from setup prefs; other groups honest (`Not recorded` / zeros) until Quick Adds
- [ ] Moments / Life / Memory boards render section cards; Quick Adds shows catalog tile grid (≥12)
- [ ] Hero **Northstar Trading Setup**; CTA **Start this business**
- [ ] Sections: Basics → People & Roles → Channels & Plan → GST → Review only
- [ ] Basics: business name, company, GSTIN, currency, start/end, description, location
- [ ] Channels: Retail buyers / Wholesale / Modern trade / HoReCa / Online + budget + sales target + measure
- [ ] Roles: Owner / Manager / Sales exec / Warehouse / Sales rep
- [ ] **Save draft** | **Schedule later**; reopen draft restores prefs
- [ ] Activate missing budget/target highlights those fields; invalid GSTIN with tracking Yes highlights GSTIN

### Distribution (`5:8637`) — five sections (no Catalogue/Network)

- [ ] Sections: Basics → People & Roles → Channels & Plan → GST → Review only
- [ ] Hero **Metro Distribution Setup**; CTA **Start this business**; accent Flame/orange
- [ ] Basics: business name, company name, GSTIN, currency, start/end, description, DC location
- [ ] Channels: Retail dealers / Wholesale / Modern trade / HoReCa / Van sales + budget + sales target + measure
- [ ] Soft plan status pill under Channels (green when budget+target parse)
- [ ] Roles: Owner / Manager / Route lead / Warehouse / Salesman
- [ ] **Save draft** creates or patches DRAFT; reopen restores prefs
- [ ] **Discard draft** in header when status is DRAFT
- [ ] Activate missing budget/target highlights those fields; invalid GSTIN with tracking Yes highlights GSTIN; Save draft still succeeds
- [ ] Activate → toast **Distribution is live** (~2.5s) then Pulse (DST-PULSE-v12)
- [ ] Pulse hero: eyebrow DISTRIBUTION, moment title, entity pills, FY chip, **Books open** when applicable; flame canvas `#0F172A` / accent `#EA580C`
- [ ] Pulse quick strip: Sale · Purchase · Collection · Payment → CREATE with code selected
- [ ] Pulse cards: `pulseMetrics` groups + `DD-ATTENTION` / `DD-GST`; **View all ›** on Attention; missing ≠ `0` / `₹0` (Not recorded / Unavailable)
- [ ] Moments: Routes live / People chips; `DD-COMMITMENTS`, `DD-ORDERS`, `DD-STOCK`, `DD-PEOPLE` + board sections
- [ ] Life: Today header; L01–L04; due/overdue chips only from `paymentReadiness`
- [ ] Memory: closed-period chips; sticky **Generate Business Story** (stub toast, no second ledger)
- [ ] Hub CREATE: **Source. Move. Settle.** search, Core / Support / Utilities groups; Dispatch / Record Sale CTAs

### Client Campaign (`5:33148`) — five sections (no Network)

- [ ] Sections: Basics → People & Roles → Channels & Plan → GST → Review only
- [ ] Hero **Brightline Setup**; CTA **Start this business**; accent indigo
- [ ] Basics: campaign name, company, client, GSTIN, currency, dates, purpose chips
- [ ] Channels: Paid social / Search / Print / Influencer / Events + client fee + monthly budget + primary target / unit
- [ ] Soft plan status pill under Channels (green when budget+target parse)
- [ ] Roles: Owner / Admin / Campaign Lead / Finance / Approver
- [ ] **Save draft** / **Discard draft**; reopen restores prefs
- [ ] Activate missing budget or startDate highlights those fields; missing client highlights `clientName`; bad GSTIN with tracking Yes highlights GSTIN
- [ ] Activate → toast **Campaign is live** with animated ✓ (~2.75s) then Pulse (`5:29386`)
- [ ] Pulse indigo chrome; tagline **Plan. Launch. Measure.**; widgets Needs Attention → Target & Budget → Payment Readiness → Finance → Activity
- [ ] Fresh activate (prefs only) shows budget/target identity; empty Attention honesty
- [ ] Moments (`5:29681`): Campaign Overview / People / Channels & Network / Open Commitments / Campaign Results
- [ ] Life (`5:29935`): Current Campaign / Channel Activity / Money Moving / People Today
- [ ] Memory (`5:30195`): Monthly History / Money History / People & Activity / Patterns & Decisions
- [ ] CREATE Quick Adds hub (`64:16631`): 10 Client tiles incl. Invoice + Collection; Saving → Saved confirm; indigo capture chrome

### Own Business Campaign (`5:38325`) — five sections (no Network)

- [ ] Sections: Basics → People & Roles → Channels & Monthly Plan → GST → Review only (no Network / Counterparties)
- [ ] Hero **Harbor & Loom Setup**; eyebrow **Own Business Campaign**; CTA **Start this business**
- [ ] Basics: campaign name, promoted brand (`brandName`), GSTIN, currency, start/end dates, purpose chips, structure (One-time / Ongoing), promoted products, location, description
- [ ] People & Roles: team members; roles Owner / Admin / Campaign Lead / Finance / Approver
- [ ] Channels: Paid social / Search / Print / Influencer / Events / Store window + own marketing budget + plan month + monthly budget + primary target (`conversionGoal`) + target unit
- [ ] Soft plan status pill under Channels (green when budget+target parse)
- [ ] GST v7 panels: tracking, registration, state, sale price includes GST, effective from; hide registration/pricing when Not now
- [ ] **Save draft** | **Schedule later** (shared sticky bar); reopen draft restores prefs
- [ ] Activate missing `monthlyBudget` or `startDate` highlights those fields; missing promoted brand highlights `brandName`; bad GSTIN with tracking Yes highlights GSTIN; Save draft still succeeds with half-filled form

### Consumer Service (`5:71162`) extras

- [ ] Basics include company, currency, start/end, description, location (+ Identity / Dates & more panels)
- [ ] Channels are Studio / Home / Online / Corporate / Events (not Walk-in/WhatsApp)
- [ ] Roles are Owner / Manager / Stylist / Front desk / Therapist
- [ ] Five sections only (no Services / Rooms / Clients network strip)
- [ ] **Save draft** creates or patches DRAFT; pill shows `Setup in progress`; reopen restores prefs
- [ ] Activate with empty channels highlights channels (danger border + message), form values kept
- [ ] Invalid GSTIN with tracking Yes highlights GSTIN; Save draft still succeeds

## Shell wire checks

- [ ] BUSINESS `ShellContentState.Loading|Idle` → AppState loading body (not spinner / placeholder)
- [ ] BUSINESS `Offline` → Offline AppState body (not generic EmptyPanel)
- [ ] BUSINESS `Error` → Error AppState body with Retry / Open Quick adds
- [ ] BUSINESS `Empty` + no company → NoCompany Setup
- [ ] BUSINESS `Empty` + CREATE → Guided business selection (`2:1514`) → setup shell
- [ ] BUSINESS Ready CREATE (later) → Hub tiles
- [ ] Personal / Group chrome unchanged

## Unit gates

```bash
cd backend/typescript && npx tsx --test tests/business-setup-draft.test.ts
cd apk && ./gradlew :app:testDebugUnitTest --tests '*.BusinessTokensObsidianTest' --tests '*.BusinessSetupThemeTest' --tests '*.BusinessSetupFieldModelTest' --tests '*.OperatingSetupPayloadBuilderTest' --tests '*.BusinessAppStateSpecsTest' -q
cd tools/design-pipeline && npm run business-tokens -- --check
# iOS: BusinessSetupThemeTests + BusinessSetupFieldModelTests + OperatingSetupPayloadBuilderTests
```

