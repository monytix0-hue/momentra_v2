# Trading Wholesale Device QA — Android results

**Date:** 2026-11-12  
**Platform:** Android phone (Nothing A059 / Asteroids, serial `00158357G000049`)  
**Build:** debug APK `resolvingpoint.momentra` (local `installDebug` with walk-blocker fixes)  
**API:** `https://api.momentra.tech/`  
**Account:** profile initials MS / owner `malla santosh`  
**Moment:** **Northstar Trading QA** · Trading / Wholesale · Channel/Route · Telangana · GSTIN REGULAR  

**Pass summary line:**
```text
[Android] PARTIAL — setup=PASS gst=36ADUFS9403L1Z9/Telangana masters=SKU+supplier pulse-budget=5L/target=10L money-posts=BLOCKED(save-cta) ops=NOT_RUN notes=Box masters overlay + throw-on-fail + CTA Box clickable landed; Maestro Save purchase still leaves sheet open; Pulse Sales/Purchases ₹0
```

Screenshots: `docs/qa/trading-device-walkthrough-2026-11-12/screenshots/`  
iOS command sheet: `IOS-ON-MAC.md` (no Xcode on this Linux host)  
Plan: `docs/superpowers/plans/2026-11-12-trading-device-qa-walkthrough.md`

---

## What ran

1. Device via `adb`; debug APK installed (Adoptium 21).
2. Created / activated **Northstar Trading QA** with Wholesale + B2B Direct, budget **₹5,00,000**, target **₹10,00,000**, GST Yes, GSTIN **`36ADUFS9403L1Z9`**, state **Telangana**.
3. Masters: SKUs **NS-SKU-01** / **NS-SKU-02** after `OperatingMastersSheet` **Box** overlay fix; supplier **Deccan Supplies QA** via Purchase picker `+ Add Supplier` (Name field, not Search).
4. QuickAdd money matrix driven via uiautomator/Maestro scripts; many early “PASS” rows were **form-closed heuristics** and must not be trusted as ledger posts.
5. Pulse after money attempts: Finance **Sales/Received/Outstanding/Payable ₹0**, Activity showed setup budget/target (**Budget 5,00,000** / **Target 10,00,000**) and supplier UPDATE noise; **Purchases ₹0**.
6. Walk-blocker fixes applied and reinstalled (see Bug log).

---

## Checklist (Android)

### 1. Prerequisites
| Step | Status | Notes |
|------|--------|-------|
| API reachable | **PASS** | Phone ping `api.momentra.tech` OK; Wi‑Fi validated |
| Auth / login | **PASS** | Already signed in |
| APK installed | **PASS** | Multiple `installDebug` cycles |
| Automated script | **SKIP** | Not re-run this session |

### Task A — Setup + Activate
| Step | Status | Notes |
|------|--------|-------|
| A1 Create Trading Wholesale | **PASS** | Live shell |
| A2 Name Northstar Trading QA | **PASS** | |
| A3 Channels Wholesale + B2B Direct | **PASS** | |
| A4 Budget 5L / Target 10L Revenue | **PASS** | Pulse Target & Budget sheet confirms |
| A5 GST Yes · GSTIN `36ADUFS9403L1Z9` · Telangana | **PASS** | Header: Telangana · GSTIN REGULAR |
| A6 Prices include GST | **PASS** | |
| A7 Activate | **PASS** | Books open · live |

### Task B — Masters
| Step | Status | Expected | Actual |
|------|--------|----------|--------|
| B1 NS-SKU-01 @ ₹450 | **PASS** | Listed after save | Created after Box overlay fix |
| B2 NS-SKU-02 @ ₹300 | **PASS** | Listed | Created |
| B3 Supplier Deccan Supplies QA | **PASS** | Persist + selectable | Works when **Name** filled (not Search); `%s` space encoding required for `adb input text` |
| B4 Buyer City Retail QA | **PARTIAL** | Persist | Created during Sale attempt; not re-verified after CTA fix |
| B5 Locations Main DC / Branch Hyd | **SKIP** | Listed | No reliable Masters/Locations hub tile in automation; Transfer blocked without them |

### Task C — QuickAdds × 2
| # | QuickAdd | Post A | Post B | Status | Notes |
|---|----------|--------|--------|--------|-------|
| 1 | PURCHASE | ₹45,000 | ₹30,000 | **BLOCKED** | Form fills (amount + Deccan); **Save purchase** did not dismiss / post (Maestro + adb). Pulse Purchases ₹0 |
| 2 | SALE | ₹90,000 part | ₹60,000 paid | **BLOCKED** | Same CTA class of failure; do not trust earlier script PASS |
| 3 | COLLECTION | ₹40k / ₹25k | | **NOT RUN** | Needs open AR |
| 4 | PAYMENT | ₹20k / ₹15k | | **NOT RUN** | Needs open AP |
| 5 | SPEND | ₹8k / ₹5k | | **NOT RUN** | |
| 6–15 | STOCK…REPRINT | matrix | | **NOT RUN** | Stopped on money CTA blocker |

Honesty checks not fully exercised: HOLD ≠ SALE, REPRINT no post, Collection ≠ Sale — **deferred** until posts land.

---

## Bug log

| Step | Severity | Expected | Actual | Fix / follow-up |
|------|----------|----------|--------|-----------------|
| Masters create overlay | **Blocker** (fixed) | New SKU form visible | Form was Column sibling → 0 height | `OperatingMastersSheet.kt` wrap in `Box(Modifier.fillMaxSize())` |
| Trading save swallows API failure | **Blocker** (fixed) | Error stays on sheet | `onFailure` only set parent `error`; sheet still dismissed | `OperatingMomentScreens.kt` TRADING_WHOLESALE `onFailure` **throws** so capture sheet keeps open |
| Capture CTA click | **Blocker** (fixed locally; retest partial) | Save posts / shows error | UiAutomator/Maestro tap on label Text often no-op | `OperatingQuickSheet.kt` CTA: **Box + clickable**, label Text inside; add `lovTrade PURCHASE` enabled = party + amount |
| Purchase Save → Pulse | **Blocker** (open) | Purchases += amount | Pulse Purchases ₹0 after filled Save attempts | Re-verify after CTA Box install; capture OkHttp on failure |
| Duplicate `+ Add Supplier` | **Minor** | Single add action | Two identical links in picker | Cosmetic |
| Automation false PASS | **Process** | PASS = ledger | Scripts treated sheet-close as PASS | RESULTS uses Pulse/Activity as source of truth |

---

## Code changes from this walk

- `apk/.../OperatingMastersSheet.kt` — Box overlay (masters create)
- `apk/.../OperatingMomentScreens.kt` — throw on trading `recordOperatingEvent` failure
- `apk/.../OperatingQuickSheet.kt` — Box CTA clickable; PURCHASE enabled gate

---

## Artifacts

| Path | Purpose |
|------|---------|
| `screenshots/` | Setup, masters, purchase/sale attempts, Pulse |
| `MATRIX_RESULTS.tsv` / `MONEY_PARTIAL.txt` | Automation logs (**heuristic; superseded by this RESULTS**) |
| `IOS-ON-MAC.md` | Separate iOS pass command sheet |
| `maestro/` | Setup + save-purchase flows |
| `drive_*.py` | Device drivers used during the walk |

---

## Remaining for a green Android pass

1. Confirm **Save purchase** posts after CTA Box fix (Pulse Purchases / Activity row with ₹).
2. Complete Sale ×2 → Collection/Payment allocate → Spend ×2.
3. Seed **Main DC** / **Branch Hyd**; run STOCK → … → REPRINT matrix.
4. Record honest Pulse totals vs plan §2.4.

## iOS

Not run here (no Xcode). Follow `IOS-ON-MAC.md` with moment name **Northstar Trading QA iOS**.
