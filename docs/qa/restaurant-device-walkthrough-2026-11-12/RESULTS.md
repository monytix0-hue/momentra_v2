# Restaurant Device QA — Android results

**Date:** 2026-11-12  
**Platform:** Android phone (Nothing A059 / Asteroids, serial `00158357G000049`)  
**Build:** debug APK `resolvingpoint.momentra` v2.0 (local assembleDebug)  
**API env:** `https://api.momentra.tech/` (default; confirmed `/v1` → 401)  
**Account:** profile initials MS / owner label `malla santosh` (email not captured)  
**Moment:** **Spice Route QA** · Restaurant / Food · Outlet  

**Pass summary line:**
```text
[Android] PARTIAL — dishes=5(names) purchase=blocked kot=no-revenue(tickets=1) sale=blocked(price-needed) notes=vendor/raw/sellPrice gaps; masters form overlay needed Box fix
```

Screenshots: `docs/qa/restaurant-device-walkthrough-2026-11-12/screenshots/`

---

## What ran

1. Device detected via `~/Android/Sdk/platform-tools/adb`
2. Debug APK built (`JAVA_HOME=~/.gradle/jdks/eclipse_adoptium-21*`) and installed
3. Maestro 2.12.1 installed; used for hub navigation; form fill mostly via `adb`+uiautomator
4. Created Retail → **Restaurant** moment **Spice Route QA**, activated (live Ember shell)
5. **Uncommitted local fix** in `RestaurantMastersSheet.kt`: wrap browser+form in `Box` so create/edit overlay works (was zero-height sibling under Column)
6. Saved station **Tandoor**; created **5 menu dishes** by name
7. Sent KOT Table **12** lines (Butter×1, Dal×1, Garlic×2, Jeera×1); Pulse stayed **Sales ₹0**, **Open tickets 1**
8. POS **Bill from KOT** loaded same lines; charge blocked — **Price needed / ₹0**

---

## Checklist (Android)

### 1. Prerequisites
| Step | Status | Notes |
|------|--------|-------|
| API reachable | **PASS** | `api.momentra.tech` `/v1` HTTP 401 (auth-gated, up) |
| Auth / login | **PASS** | Already signed in on device |
| APK installed | **PASS** | Debug install Success |
| Maestro | **PASS** | Installed; partial use |
| Automated script | **SKIP** | Not re-run this session |

### Task A — Create + Activate / Start
| Step | Status | Notes |
|------|--------|-------|
| A1 Create retail moment | **PASS** | Retail & Consumer create path |
| A2 Name Spice Route QA · Outlet | **PASS** | Location field `Outlet` |
| A3 Subtype Restaurant / Food | **PASS** | Live pulse: Restaurant / Food |
| A4 Dine-in channel | **PASS** | Selected in §03 |
| A5 §04 Menu, Stations & Vendors | **PASS** | Wording confirmed on setup |
| A6 Minimal remaining setup | **PASS** | Budget/target filled (Pulse shows ₹12L target) |
| A7 Start → Business is live | **PASS** | Live shell after setup |
| A8 Ember · Pulse/Moments/Life/Memory/Quick Adds | **PASS** | Tabs present |

### Task B — Masters
| Step | Status | Expected | Actual |
|------|--------|----------|--------|
| B1 Station Tandoor | **PASS** | Save + real station | Real list: Tandoor · 0 open tickets (demo cleared) |
| B2 Vendor Fresh Farm QA | **FAIL** | Persist Fresh Farm QA | Demo vendors only after Save; name did not stick |
| B3 Five dishes | **PARTIAL** | Names + sell ₹ | Names OK; **sellPrice not shown on list** (no ₹) |
| B4 Menu shows exactly 5 | **PASS** | 5 real dishes | Butter Chicken, Dal Makhani, Garlic Naan, Jeera Rice, Paneer Tikka Masala |
| B5 Five raw materials | **BLOCKED** | Create raws | Purchase needs rates; no reliable raw create completed |

**Sell prices expected vs list:** expected 480/420/340/90/180 — **actual: blank / Price needed** (form has no editable sell-price field; `priceHeroCard` is display-only defaulting to 480 that did not round-trip onto list).

### Task C — Purchase ₹4,650
| Step | Status | Expected | Actual |
|------|--------|----------|--------|
| C1 Ingredients · Fresh Farm QA | **FAIL** | Counterparty Fresh Farm QA | Invoice chrome `Fresh Farm` / Select supplier empty; vendor QA missing |
| C2 Lines total ₹4,650 | **BLOCKED** | 4650 | ₹0; GST **Unavailable** (honest) |
| C3 Post purchase | **BLOCKED** | Post | No priced lines |
| C4 No SALE bump | **N/A** | — | Not posted |

### Task D — KOT
| Step | Status | Expected | Actual |
|------|--------|----------|--------|
| D1 Send Table 12 lines | **PASS** | Lines per §2.5 | Bill-from-KOT shows Butter×1, Dal×1, Garlic×2, Jeera×1; units wrongly **kg** |
| D2 No revenue | **PASS** | Sales unchanged | Pulse: **Sales ₹0**, **Open tickets 1** |
| D3 Fire | **NOT RUN** | — | Time / focus on Send |
| D4 Reprint optional | **NOT RUN** | — | |

### Task E — Sale / POS
| Step | Status | Expected | Actual |
|------|--------|----------|--------|
| E1 Open POS · prefer Bill from KOT | **PASS** | KOT live | KOT-20183 Sent; Scenario B row visible |
| E2 Total ₹1,180 (B) or ₹1,080 (A) | **FAIL** | 1180 / 1080 | Lines **Price needed**, line ₹0, GST Unavailable |
| E3 SALE posts | **BLOCKED** | Revenue += posted | Not charged |
| E4 Record actual ₹ | **N/A** | — | |

### Task F — Honesty
| Step | Status | Notes |
|------|--------|-------|
| F1 Split tender | **NOT RUN** | |
| F2 Covers Unavailable honesty | **PASS** | Covers showed 4 on KOT (enabled) |
| F3 Empty Pulse before SALE | **PASS** | Pre-KOT empty; after KOT still Sales ₹0 |

---

## Bug log

| Step | Severity | Expected | Actual | Screenshot |
|------|----------|----------|--------|------------|
| Masters form overlay | **Blocker** (fixed locally, uncommitted) | + Add / New opens form | Form emitted as Column sibling → 0 height | `22`/`23` vs after Box fix |
| B2 Vendor save | **Major** | Fresh Farm QA in list | Demo vendors remain | `26`/`27` |
| B3 sellPrice | **Major** | Menu shows ₹ rates | No ₹ on list; POS Price needed | `28`/`29`/`41` |
| Dish form sell price UX | **Major** | Editable sell price | Only display hero; cannot set 420/340/90/180 | — |
| C Purchase | **Major** | ₹4,650 post | ₹0; no raw masters | `30`/`31` |
| E Charge | **Blocker** for ₹ check | Charge KOT bill | Cannot charge without prices | `40`/`41` |
| KOT unit | **Minor** | plate/pcs | Shows **kg** on ticket/POS | `36`/`40` |

---

## Blockers for remaining steps (user)

1. **Vendor Fresh Farm QA** — re-try New vendor after Box fix; confirm toast + list replaces demo. If still demo-only, capture Logcat for UPDATE failure.
2. **Sell prices on dishes** — product gap: no sell-price editor; need fix or edit path so masters carry 480/420/340/90/180 before Purchase/POS math can pass.
3. **Raw materials + Purchase ₹4,650** — create 5 raws (§2.4) then post Ingredients purchase to Fresh Farm QA.
4. **POS Charge** — after prices exist, Bill from KOT (Scenario B ₹1,180) or Scenario A ₹1,080; confirm Pulse sales bump.
5. **Fire tab** — optional after Send; confirm still no SALE.
6. **Do not invent GST** — UI correctly shows Unavailable when rate missing; leave it.

Local uncommitted fix to keep: `apk/app/src/main/java/com/example/momentra/ui/shell/business/restaurant/RestaurantMastersSheet.kt` (Box wrapper). Not committed.

---

## iOS-on-Mac (this host cannot run iOS)

Linux host: no Xcode / simctl. Run a **separate full pass** on Mac.

### Short command sheet

```bash
# 1) Same API as Android
# Info.plist MomentraAPIBaseURL or env:
export MOMENTRA_API_BASE_URL='https://api.momentra.tech/'

# 2) Build / run
cd /path/to/momentra/momentra   # or repo root with momentra.xcodeproj
xcodebuild -scheme momentra -destination 'platform=iOS Simulator,name=iPhone 16' build
# or open Xcode → scheme momentra → Simulator or device
# Bundle: resolvingpoint.momentra

# 3) Sign in with SAME test account as Android
# 4) Create NEW moment name: Spice Route QA iOS  (do not reuse Android half-state)
# 5) Walk Tasks A–F from:
#    docs/superpowers/plans/2026-11-12-restaurant-device-qa-walkthrough.md
#    Sample ₹: dishes 480/420/340/90/180; Purchase 4650; KOT 1180 no revenue; Sale A 1080 or B from KOT
# 6) iOS KOT entry: Status → Send KOT › AND/OR Pulse → KOT
# 7) Pass summary:
#    [iOS] PASS/FAIL — dishes=5 purchase=4650 kot=no-revenue sale=<amount> notes=
```

Watch for Android-known gaps: masters form overlay, vendor persist, sellPrice round-trip, Purchase without raws.
