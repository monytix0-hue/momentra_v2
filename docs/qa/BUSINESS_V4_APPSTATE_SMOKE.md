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
| Manufacturing | Made to order | `MADE_TO_ORDER` | `5:55863` | ☐ | ☐ |

Setup smoke (Figma v6 parity):

- [ ] Scroll through Basics → People & Roles → Network/Catalogue → Channels & Plan → GST → Review for subtypes that still include Network (not Retail Store / Distribution / Client Campaign)
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

### Client Campaign (`5:33148`) — five sections (no Network)

- [ ] Sections: Basics → People & Roles → Channels & Plan → GST → Review only
- [ ] Hero **Brightline Setup**; CTA **Start this business**; accent indigo
- [ ] Basics: campaign name, company, client, GSTIN, currency, dates, purpose chips
- [ ] Channels: Paid social / Search / Print / Influencer / Events + client fee + monthly budget + primary target / unit
- [ ] Soft plan status pill under Channels (green when budget+target parse)
- [ ] Roles: Owner / Admin / Campaign Lead / Finance / Approver
- [ ] **Save draft** / **Discard draft**; reopen restores prefs
- [ ] Activate missing budget or startDate highlights those fields; missing client highlights `clientName`; bad GSTIN with tracking Yes highlights GSTIN

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

