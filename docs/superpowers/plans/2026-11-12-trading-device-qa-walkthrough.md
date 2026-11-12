# Trading Wholesale Device QA Walkthrough Plan

> **For agentic workers:** This is a **manual / device-driven QA plan**, not a Figma fidelity rewrite. Execute on an attached Android phone and on iOS-on-Mac as **separate full passes**. Fix only walk blockers (setup persist, hub wire, field round-trip) — not chrome polish.

**Goal:** Walk Trading Wholesale (`TRADING_WHOLESALE`) from create → setup (name, channels & monthly plan, GST) → Activate → masters → **all QuickAdds × 2** with fixed ₹ amounts so pass/fail is checkable on Android phone and iOS (Mac) separately.

**Architecture:** Subtype `TRADING_WHOLESALE` under family `TRADING_DISTRIBUTION`. Setup document sections → Activate → Trading active shell → Quick Adds hub (`TradingQuickAdds` / iOS `OperatingQuickAddHubView`) → capture / HTR sheets → `POST …/operating-events` → Pulse projection. Automated script covers backend + Android JVM only.

**Tech Stack:** Android APK (`resolvingpoint.momentra` / `com.example.momentra`), iOS (`resolvingpoint.momentra`), shared moments + operating-events APIs.

**Spec:** User request 2026-11-12 (trading setup with GSTIN `36ADUFS9403L1Z9`, Telangana, GST on; QuickAdds all × 2 with amounts). Related: `docs/qa/BUSINESS_MOMENTS_AUTOMATED.md`, `docs/superpowers/plans/2026-11-10-trading-quick-add-figma-parity.md`.

## Global Constraints

- Use the **Sample Dataset** below verbatim so Android and iOS are comparable.
- Honesty: never invent GST rates; HOLD ≠ SALE/STOCK/money; REPRINT never posts; Collection ≠ Sale; Payment ≠ Purchase; Order = commitment only; Shipment = goods only.
- Run Android and iOS as **separate full passes** (different moment names).
- GSTIN must stay **uppercase** `36ADUFS9403L1Z9` with state **Telangana** (code **36**).

## Review Focus

1. **Setup activation** — named + ≥1 channel + budget > 0 + target > 0 + GST Yes path.
2. **GSTIN ↔ state** — prefix `36` matches Telangana; no false “mismatch” blocking Activate.
3. **Trading money flow** — Purchase → Sale → Collection/Payment allocate against real docs.
4. **HOLD / TRANSFER / REPRINT** — no new SALE; Transfer on-hand unchanged; Reprint read-only.
5. **Hub entry** — Android utilities may open via chips / hints; iOS hub appends HOLD/TRANSFER/REPRINT.

---

## 1. Prerequisites

### 1.1 Backend

- [ ] API reachable: `https://api.momentra.tech/` (or document alternate).
- [ ] Auth works for creating Business moments.
- [ ] Optional preflight:

```bash
./scripts/qa/run-business-moments-automated.sh
```

### 1.2 Android phone

- [ ] Debug APK installed, same API host.
- [ ] USB / wireless debugging for Logcat + screenshots.
- [ ] Fresh moment preferred.

### 1.3 iOS on Mac

- [ ] Xcode scheme `momentra`.
- [ ] Same API base URL as Android.
- [ ] Full separate pass with moment name **Northstar Trading QA iOS**.

### 1.4 Login

- [ ] Sign in on Android before Task A.
- [ ] Sign in on iOS before Task A (second pass).
- [ ] Record account email/id (no passwords in RESULTS).

---

## 2. Sample dataset (amounts)

### 2.1 Identity & setup

| Field | Android | iOS |
|-------|---------|-----|
| Moment type | Trading / Wholesale | same |
| Business name | **Northstar Trading QA** | **Northstar Trading QA iOS** |
| Channels | **Wholesale** + **B2B Direct** | same |
| Month | current calendar month | same |
| Monthly budget | **₹5,00,000** | same |
| Monthly sales target | **₹10,00,000** (Revenue) | same |
| Track GST | **Yes** | same |
| GSTIN | **`36ADUFS9403L1Z9`** | same |
| State | **Telangana** | same |
| Prices include GST | **Yes** | same |

### 2.2 Masters

| Master | Value |
|--------|--------|
| SKU 1 | **NS-SKU-01** · sell **₹450** |
| SKU 2 | **NS-SKU-02** · sell **₹300** |
| Supplier | **Deccan Supplies QA** |
| Buyer | **City Retail QA** |
| Location A | **Main DC** |
| Location B | **Branch Hyd** |

### 2.3 QuickAdd “2 each” matrix (execute in order)

| # | QuickAdd | Post A | Post B | Pass check |
|---|----------|--------|--------|------------|
| 1 | PURCHASE | ₹45,000 received now | ₹30,000 receive later | AP + stock / pending GRN |
| 2 | SALE | ₹90,000 part paid (₹40k now → AR ₹50k) | ₹60,000 paid in full | AR + channel Wholesale |
| 3 | COLLECTION | ₹40,000 vs Sale A | ₹25,000 vs Sale B | Allocate = amount; not a new sale |
| 4 | PAYMENT | ₹20,000 vs Purchase A | ₹15,000 vs Purchase B | AP only |
| 5 | SPEND | ₹8,000 paid | ₹5,000 pay later | Expense ≠ payment |
| 6 | STOCK | Set NS-SKU-01 → 200 | Add NS-SKU-02 +16 | Not a purchase |
| 7 | ORDER | Customer SO ₹1,20,000 | Supplier PO ₹50,000 | Commitment only |
| 8 | SHIPMENT | Outgoing −60 (vs Sale) | Incoming (vs Purchase B) | Goods only |
| 9 | RETURN | Credit ₹9,000 vs Sale | Credit ₹3,000 vs Purchase | Explicit goods if stock moves |
| 10 | BUDGET | Budget → ₹5,50,000 | Target → ₹11,00,000 | Versioned revision |
| 11 | UPDATE / ISSUE | Status update | Issue (blocker) | No money/stock |
| 12 | APPROVAL | Approve spend | Reject path | Reimburse = separate PAYMENT |
| 13 | HOLD | Hold order | Release / Hold dispatch | No SALE/STOCK |
| 14 | TRANSFER | Main DC → Branch Hyd 10 pc | Reverse 5 pc | On-hand unchanged |
| 15 | REPRINT | Sale A invoice | Second doc if offered | **No new event** |

### 2.4 Expected Pulse / ledger after money posts (reference)

After Purchase A+B, Sale A+B, Collection A+B, Payment A+B (ideal happy path):

| Metric | Rough expected |
|--------|----------------|
| Purchase posted | 45,000 + 30,000 = **₹75,000** |
| Sale posted | 90,000 + 60,000 = **₹1,50,000** |
| Collection | 40,000 + 25,000 = **₹65,000** |
| Payment | 20,000 + 15,000 = **₹35,000** |
| Open AR after collections | Sale A remaining ≈ **₹10,000** if 40k of 50k AR collected after 40k tender on sale… *(document actuals; part-paid semantics may vary by UI)* |
| HOLD / REPRINT | **0** new SALE events |

Record **actual** Pulse numbers in RESULTS — UI allocation fields may alter AR remainder.

---

## 3. Task A — Create + Setup + Activate

### A1 Create Trading moment

1. Business → create / add Trading Wholesale moment.
2. Open setup document.

### A2 § Business Basics

- [ ] Business name = Northstar Trading QA (or iOS variant).
- [ ] Optional company / location as available.
- [ ] Skip or fill dates if required by UI.

### A3 § Channels & Monthly Plan

- [ ] Select **Wholesale** and **B2B Direct**.
- [ ] Budget **500000** / Target **1000000**.
- [ ] Target metric **Revenue**.

### A4 § GST & Tax

- [ ] Track GST = **Yes**.
- [ ] GSTIN = **`36ADUFS9403L1Z9`**.
- [ ] State = **Telangana**.
- [ ] Prices include GST = Yes.
- [ ] Confirm no blocking GSTIN/state mismatch.

### A5 Activate

- [ ] Activate / Start trading moment.
- [ ] Land on Trading Pulse / active shell with Quick Adds unlocked.

**PASS:** Activated trading shell. **FAIL:** Capability denied / incomplete readiness.

---

## 4. Task B — Masters

Via Quick Adds → Masters / Catalogue & SKU / Network / Godowns (labels may vary):

- [ ] Create **NS-SKU-01** @ ₹450 and **NS-SKU-02** @ ₹300 (sell price visible after save).
- [ ] Create supplier **Deccan Supplies QA**.
- [ ] Create buyer **City Retail QA**.
- [ ] Create locations **Main DC** and **Branch Hyd**.

**PASS:** All six entities listed after save (not demo-only). **FAIL:** names saved but prices/parties missing (block Purchase/Sale).

---

## 5. Task C — QuickAdds × 2 (matrix)

Open each tile from Trading Quick Adds hub (HOLD/TRANSFER/REPRINT via hub utilities on iOS or chips/hints on Android). For each Post A then Post B:

1. Fill fields per matrix amounts.
2. Post / Save.
3. Confirm toast or success; note sheet `error` if any.
4. Screenshot sticky / confirmation when FAIL/BLOCKED.

Checklist row template (copy into RESULTS):

| # | Mode | Post | ₹ expected | ₹/result actual | Status |
|---|------|------|------------|-----------------|--------|
| 1A | PURCHASE | received now | 45000 | | |
| … | | | | | |

---

## 6. Task D — Pulse spot-check

- [ ] Activity / finance cards reflect purchases & sales (non-zero after C).
- [ ] After HOLD: no new SALE.
- [ ] After REPRINT: event count for SALE unchanged.

---

## 7. Platform notes

| Step | Android phone | iOS on Mac |
|------|---------------|------------|
| Package | APK debug install | Xcode run Simulator/device |
| Hub | `TradingQuickAdds` | `OperatingQuickAddHubView` |
| HTR | May be chip / sheet enum (utilities row often empty on V12 trading) | Hub appends HOLD/TRANSFER/REPRINT |
| Screenshots | `docs/qa/trading-device-walkthrough-2026-11-12/screenshots/` | same folder with `ios-` prefix |

---

## 8. Bug log template

```text
vertical: TRADING_WHOLESALE
platform: android|ios
sheet: PURCHASE|SALE|…
action: post A|B
expected: ₹…
actual: UI text / toast / HTTP
moment: Northstar Trading QA…
build: …
```

---

## 9. Results location

- Plan (this file): `docs/superpowers/plans/2026-11-12-trading-device-qa-walkthrough.md`
- Results: `docs/qa/trading-device-walkthrough-2026-11-12/RESULTS.md`
- iOS sheet: `docs/qa/trading-device-walkthrough-2026-11-12/IOS-ON-MAC.md`
- Screenshots: `docs/qa/trading-device-walkthrough-2026-11-12/screenshots/`

## 10. Automated vs this pass

`./scripts/qa/run-business-moments-automated.sh` = backend operating tests + Android JVM hub smokes. **Not** a substitute for this device pass (toasts, Pulse, GSTIN UI, allocation pickers).
