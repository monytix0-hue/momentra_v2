# Restaurant Device QA Walkthrough Plan

> **For agentic workers:** This is a **manual device QA plan**, not a code-implementation plan. Execute as a human (or device-driven) checklist on Android phone and iOS-on-Mac separately. Do **not** treat as subagent-driven development unless the user later asks to automate gaps.

**Goal:** Walk Restaurant (`RESTAURANT_FOOD`) from create → setup → Activate/Start → 5-dish menu + raw materials → Quick Adds (Purchase, Sale/POS, KOT) with fixed ₹ amounts so pass/fail is checkable on an attached Android phone and on iOS (Mac simulator or device) separately.

**Architecture:** Primary vertical is **Restaurant / Food** under retail family `RETAIL_CONSUMER` (dish menu + KOT). Ember chrome on active shell. Masters persist as `UPDATE` + master body; money/ops post as operating events; Pulse refreshes from projection. Automated script covers backend + Android JVM only — this plan covers device toasts, Pulse round-trips, and platform entry-path differences.

**Tech Stack:** Android APK (`com.example.momentra`), iOS app (`resolvingpoint.momentra`), shared `POST /v1/moments`, `POST …/activate`, `POST …/operating-events`, projection + Pulse.

**Spec:** User request 2026-11-12 (retail setup → activate → 5 dishes + raw materials → QuickAdds Purchase/Sale/KOT). Related: `docs/qa/BUSINESS_MOMENTS_AUTOMATED.md`, `docs/implementation/RESTAURANT_MISSING_APIS.md`, `docs/superpowers/plans/2026-11-11-restaurant-flow-24-screens.md`.

## Global Constraints

- **Primary kit = Restaurant** (`RESTAURANT_FOOD`), not pure Retail Store — user asked for dishes + KOT.
- Use the **Sample Dataset** amounts below verbatim so Android and iOS results are comparable.
- Honesty: KOT Send / Fire / Status **never** post SALE revenue; Purchase posts AP/stock; POS Charge posts SALE.
- Do not invent GST splits, printer PDFs, or recipe deduction if UI shows Unavailable (`RESTAURANT_MISSING_APIS.md`).
- Run Android phone and iOS Mac **as separate full passes** (do not mix half-done states across platforms).
- Demo empty-state menu in UI is **4 dishes only** — this plan **creates 5 real masters** (demo rows disappear once masters exist).

## Review Focus

1. **KOT ≠ revenue** — after Send/Fire, Pulse/activity may show ORDER/PREPARING; finance/sales must **not** increase.
2. **Masters empty until saved** — empty Menu shows demo; after first Save, list must show real dishes (5 names).
3. **Android KOT entry** — Quick Adds → **Status** → **Send KOT ›** (no hub KOT tile); iOS also has Pulse → **KOT**.
4. **Purchase math** — posted purchase total must match line × rate (₹4,650 in this dataset).
5. **POS after KOT** — Bill from KOT / Charge posts SALE; expected bill line total for scripted ticket = ₹1,050 (see Sale script).

---

## Scope note: Retail Store variant

If the user meant **Retail Store** (`RETAIL_STORE`) instead: same create → Activate retail moment → Start path, but subtype chip **Retail Store**, §04 **Catalogue, Locations & Network**, Rose theme, **no KOT**. Replace dishes with 5 **products**, skip Status→KOT, use Retail Quick Adds Sale/Purchase/Stock. Keep this Restaurant plan as the default for dish + KOT.

---

## 1. Prerequisites

### 1.1 Backend

- [ ] API reachable from both devices (same env: staging or local tunnel).
- [ ] Auth works: login with a test account that can create Business moments.
- [ ] Optional preflight (laptop, not a substitute for device):

```bash
./scripts/qa/run-business-moments-automated.sh
```

Expected: backend `operating-*.test.ts` pass; capabilities test if `DATABASE_URL` set; Android `ui.shell.business.*` JVM tests pass. **Does not** prove device toasts or Pulse refresh.

### 1.2 Android phone (attached)

- [ ] Debug/release APK installed (`com.example.momentra`), pointing at the same API.
- [ ] USB debugging or wireless debug on if capturing Logcat.
- [ ] Device time/timezone sane; network allows API host.
- [ ] Fresh moment preferred (or note existing moment id in bug log).

### 1.3 iOS on Mac (simulator or device)

- [ ] Xcode scheme `momentra`, bundle `resolvingpoint.momentra`.
- [ ] Simulator **or** physical iPhone separately — pick one target and stick to it for the full pass.
- [ ] Same API base URL / env as Android pass (document which).
- [ ] Build reference: `cd momentra && xcodebuild -scheme momentra -destination 'generic/platform=iOS' build` (adjust destination for simulator).

### 1.4 Login

- [ ] Sign in on **Android** before Task A.
- [ ] Sign in on **iOS** before Task A (second pass).
- [ ] Record account email/id (no passwords in the bug log).

---

## 2. Sample dataset (amounts)

Use outlet display name **Spice Route QA** (Figma demo brand; fine for device QA).

### 2.1 Vendor

| Field | Value |
|-------|--------|
| Name | Fresh Farm QA |
| Use for | Purchase counterparty |

### 2.2 Station

| Field | Value |
|-------|--------|
| Name | Tandoor |
| Notes | Default route for mains; UI may show printer/lead placeholders |

### 2.3 Five dishes (create via Menu → New dish)

| # | Dish | Sell price (₹) | Unit | Category / notes |
|---|------|----------------|------|------------------|
| 1 | Butter Chicken | **480** | plate | Mains → Tandoor |
| 2 | Paneer Tikka Masala | **420** | plate | Mains |
| 3 | Dal Makhani | **340** | plate | Mains |
| 4 | Garlic Naan | **90** | pcs | Breads |
| 5 | Jeera Rice | **180** | plate | Mains (5th dish — not in UI demo) |

GST field if shown: **5%** (stamp honesty — do not invent line tax if Unavailable).

**Menu list sell subtotal (reference):** 480+420+340+90+180 = **₹1,510** (catalogue only; not a posted sale).

### 2.4 Raw materials (create via Raw material form / Purchase → + New raw material)

| # | Material | Unit | Purchase rate (₹) | Buy qty (this Purchase) | Line total |
|---|----------|------|-------------------|-------------------------|------------|
| 1 | Chicken (bone-in) | kg | **280** | **10** | **2,800** |
| 2 | Paneer | kg | **360** | **3** | **1,080** |
| 3 | Black urad dal | kg | **140** | **2** | **280** |
| 4 | Maida (flour) | kg | **45** | **5** | **225** |
| 5 | Basmati rice | kg | **95** | **3** | **285** |

**Purchase document total:** 2800+1080+280+225+285 = **₹4,650**  
**Vendor:** Fresh Farm QA  
**Mode:** Ingredients (if chip present)

### 2.5 Scripted KOT ticket (Send)

| Field | Value |
|-------|--------|
| Channel | In-dining |
| Table | **12** |
| Covers | **4** (only if field enabled; else leave Unavailable — do not invent) |
| Lines | Butter Chicken ×1 (₹480) + Dal Makhani ×1 (₹340) + Garlic Naan ×2 (₹180) + Jeera Rice ×1 (₹180) |
| **Line sell total** | **₹1,180** (kitchen ticket; **not** revenue until POS Charge) |
| Client key / ticket hint | e.g. `KOT-QA-12` if field accepts free text |

### 2.6 Scripted POS / Sale (after or with KOT)

**Scenario A — Charge without bill-from-KOT (simple):**

| Lines | Qty | Rate | Amount |
|-------|-----|------|--------|
| Butter Chicken | 1 | 480 | 480 |
| Paneer Tikka Masala | 1 | 420 | 420 |
| Garlic Naan | 2 | 90 | 180 |
| **Sale total** | | | **₹1,080** |

**Scenario B — Prefer Bill from KOT if open ticket exists:** charge the KOT lines (**₹1,180**) or the sheet’s linked total; record actual posted amount in bug log if UI differs.

Payment: single tender **Paid** / cash or UPI as UI allows. Skip Split tender if locked.

### 2.7 Expected outcomes (₹ math checklist)

| After action | Stock / ops | Finance / Pulse |
|--------------|-------------|-----------------|
| Save 5 dishes + station + vendor | Masters list shows 5 dishes, Tandoor, Fresh Farm QA | Toast `✓ Saved · sheet closes and returns back` |
| Post Purchase ₹4,650 | Purchase / stock facets show ingredients in (if role allows) | AP/purchase activity; **not** SALE revenue |
| Send KOT (Table 12) | ORDER / prep activity | **No** sales revenue increase |
| Fire course | Same ORDER revised (PREPARING/FIRE) | Still **no** SALE |
| POS Charge ₹1,080 (A) or KOT bill (B) | Sale activity | Sales / revenue increases by posted bill total |

---

## 3. Numbered walkthrough (one platform at a time)

Copy this section twice mentally: **Pass Android**, then **Pass iOS**. Check boxes per platform in the bug log.

### Task A: Create + setup + Activate / Start

**Nav:** Business empty → create retail moment → setup document → subtype Restaurant.

- [ ] **A1.** Create Business moment; family retail / shop & counter path; headline **YOUR RETAIL MOMENT** (or current copy).
- [ ] **A2.** Setup §01: name **Spice Route QA**; party label **Outlet**.
- [ ] **A3.** Subtype chip **Restaurant / Food** (`RESTAURANT_FOOD`) — not Retail Store.
- [ ] **A4.** Channels §03: enable at least **Dine-in** (optional Takeaway).
- [ ] **A5.** §04 title reads **Menu, Stations & Vendors** (restaurant wording).
- [ ] **A6.** Complete remaining setup sections with minimal valid data.
- [ ] **A7.** §06: while draft, CTA **Start this business**; after success, **Business is live** (pill / copy).
- [ ] **A8.** Land on active shell with Ember theme; tabs include **Pulse · Moments · Life · Memory · Quick Adds**.

**Pass:** Business is live, subtype restaurant, Ember active.  
**Fail:** Wrong subtype (Retail Store / rose), stuck on Setup in progress, activate API error.

### Task B: Masters — Station, Vendor, 5 dishes, raw materials

**Nav:** Quick Adds → **Menu · Stations · Vendors** (masters hub).

- [ ] **B1.** Stations → Add → name **Tandoor** → **Save station** → toast ✓ Saved.
- [ ] **B2.** Vendors → New → **Fresh Farm QA** → **Save vendor** → toast ✓ Saved.
- [ ] **B3.** Menu → New dish ×5 using §2.3 names and sell prices → each **Save menu dish** → toast ✓ Saved.
- [ ] **B4.** Menu list shows **exactly these 5** (demo Butter Chicken… four-pack gone once real masters exist).
- [ ] **B5.** Create 5 raw materials (§2.4 names, unit **kg**, purchase rates) via Raw form or Purchase browser **+ New raw material**.

**Pass:** Projection lists masters; Pulse/Moments may show UPDATE activity.  
**Fail:** Demo-only list after save; save with blank name allowed; no toast / red error stuck.

### Task C: Quick Add — Purchase

**Nav:** Quick Adds → **Purchase** (caption Ingredients).

- [ ] **C1.** Mode Ingredients; counterparty **Fresh Farm QA**.
- [ ] **C2.** Add five lines (§2.4 qty × rate); confirm UI total **₹4,650** (or note deviation).
- [ ] **C3.** CTA **Post purchase** / **Post purchase · ₹4,650**.
- [ ] **C4.** Sheet closes; hub/Pulse shows purchase activity; **no** SALE revenue bump from this alone.

**Pass:** Purchase posted; amount matches.  
**Fail:** Post fails silently; total wrong; treated as SALE.

### Task D: Quick Add — KOT (Send → Fire)

**Android nav:** Quick Adds → **Status** → **Send KOT ›** → Send tab.  
**iOS nav:** Same Status path **and/or** Pulse quick action **KOT**.

- [ ] **D1.** Send KOT: In-dining, Table **12**, lines per §2.5 → Send to kitchen.
- [ ] **D2.** Pulse / Moments: ORDER (or prep) visible; finance sales **unchanged**.
- [ ] **D3.** Fire tab: fire held/now courses as UI allows (`revisesEventId` path); still **no** SALE.
- [ ] **D4.** (Optional) Reprint: does **not** allocate a new KOT id / new revenue.

**Pass:** Kitchen ticket lifecycle without revenue.  
**Fail:** Revenue increases on Send/Fire; Android cannot open KOT from Status; iOS Pulse KOT missing.

### Task E: Quick Add — Sale / POS

**Nav:** Quick Adds → **Sale** (+ Record Sale) **or** Pulse **+ Record Sale**.

- [ ] **E1.** Open POS; prefer In-dining; if **Bill from KOT** / **KOT live** offers Table 12 ticket, use Scenario B; else Scenario A (§2.6).
- [ ] **E2.** Confirm line total (**₹1,080** or **₹1,180** / linked); Charge / Post.
- [ ] **E3.** Sheet closes; Pulse/finance shows SALE ≈ posted amount.
- [ ] **E4.** Record actual posted ₹ in bug log if UI GST/packaging changes total.

**Pass:** SALE equals charged total; prior Purchase still ₹4,650 side.  
**Fail:** Silent fail on POS; double revenue; KOT billed twice as two full sales without intent.

### Task F: Smoke negatives (honesty)

- [ ] **F1.** Split tender (if shown locked) — stay locked; do not force.
- [ ] **F2.** Covers / table — if Unavailable, leave; do not type fake persisted covers for “pass”.
- [ ] **F3.** Empty Pulse before any SALE shows empty CTA, not fabricated ₹0 KPIs pretending to be live metrics.

---

## 4. Platform notes — Android phone vs iOS Mac

| Topic | Android phone | iOS on Mac |
|-------|---------------|------------|
| Theme | Ember `RestaurantActive` | `RestaurantActiveView` / Restaurant kit |
| KOT entry | **Status → Send KOT ›** (no hub KOT tile) | Status path + **Pulse → KOT** |
| Sale entry | Hub **Sale** → `RestaurantPosSheet` | Hub Sale → POS / capture `POS` |
| Masters toast | `✓ Saved · sheet closes and returns back` | Same copy expected |
| Build | APK on USB device | Simulator **or** device; document which |
| Capture | Logcat on fail | Console / device logs on fail |
| Known risk | POS error surfacing weaker than hub | Business runtime gate / fixtures historically flaky — use same API user as Android |
| Visual | Safe area / nav gestures | Notch / home indicator; same content chrome |

**Rule:** Finish Tasks A–F on Android, log results, then wipe assumptions and repeat A–F on iOS (new moment or clearly labeled second outlet name e.g. **Spice Route QA iOS**).

---

## 5. Bug log template

Copy one row per defect. Attach screenshot paths if captured.

| Field | Fill |
|-------|------|
| Date | 2026-11-12 |
| Platform | Android phone / iOS Simulator / iOS device |
| Build | APK version or git SHA / Xcode build |
| API env | |
| Account | (email only) |
| Moment id / name | Spice Route QA … |
| Step id | e.g. D2, E3 |
| Expected | e.g. no SALE after KOT Send; Purchase ₹4,650 |
| Actual | |
| Toast / error text | |
| Pulse after | |
| Screenshot / Logcat | |
| Severity | Blocker / Major / Minor |
| Honesty? | Y if Missing-API Unavailable violated or invented |

**Pass summary line (per platform):**

```text
[Android|iOS] PASS/FAIL — dishes=5 purchase=4650 kot=no-revenue sale=<amount> notes=
```

---

## 6. Automated script vs this device pass

| | `./scripts/qa/run-business-moments-automated.sh` | This device walkthrough |
|--|--------------------------------------------------|-------------------------|
| Backend projection / KOT semantics / retail analytics | ✅ | Indirect (live API) |
| Capabilities incl. `RESTAURANT_FOOD` (needs DB) | ✅ | ❌ |
| Android JVM hub/KOT fidelity smokes | ✅ | ❌ |
| Device UI, toasts, Logcat | ❌ | ✅ |
| Create → Activate → masters → Purchase → KOT → POS | ❌ | ✅ |
| iOS UI | ❌ | ✅ |
| Maestro cert ledger | PENDING / not this script | Optional later |

**Order of work:** run automated script on laptop for green baseline → execute this plan on Android phone → execute again on iOS Mac → file bugs with template.

---

## 7. Execution checklist (meta)

- [ ] Automated script green (or failures noted as known).
- [ ] Android full pass A–F + pass summary line.
- [ ] iOS full pass A–F + pass summary line.
- [ ] Bug log filed for every FAIL.
- [ ] If user actually wanted Retail Store: run parallel product-only pass (no KOT) with same Purchase/Sale discipline.

---

## Self-review (plan author)

1. **Spec coverage:** Setup, Activate/Start, 5 dishes, raw materials, Purchase, Sale/POS, KOT, dual-platform, amounts, bug log, automation gap — all have tasks.
2. **Amounts locked:** Purchase **₹4,650**; Sale A **₹1,080**; KOT lines **₹1,180** kitchen; menu catalogue **₹1,510**.
3. **Restaurant vs Retail:** Primary Restaurant; Retail Store variant called out once.
4. **No code/commit** in this deliverable — QA doc only.
