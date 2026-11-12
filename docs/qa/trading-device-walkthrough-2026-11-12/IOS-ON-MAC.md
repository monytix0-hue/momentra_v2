# iOS-on-Mac — Trading Wholesale QA (separate pass)

Android phone pass runs on Linux; this host has **no Xcode / `xcodebuild`**. Run the iOS pass on a Mac only. Do **not** reuse the Android moment.

## Commands

```bash
export MOMENTRA_API_BASE_URL='https://api.momentra.tech/'
cd momentra   # Xcode project root
xcodebuild -scheme momentra -destination 'platform=iOS Simulator,name=iPhone 16' build
# Or: open momentra.xcodeproj → scheme momentra → Simulator / device
```

Bundle: `resolvingpoint.momentra`  
API: same as Android (`https://api.momentra.tech/`)  
Moment name: **Northstar Trading QA iOS** (new; never mix with Android moment)

Plan doc: `docs/superpowers/plans/2026-11-12-trading-device-qa-walkthrough.md`

## Locked sample (iOS)

| Field | Value |
|-------|--------|
| Type | Trading / Wholesale (`TRADING_WHOLESALE`) |
| Name | **Northstar Trading QA iOS** |
| Channels | Wholesale + B2B Direct |
| Budget / Target | **₹5,00,000** / **₹10,00,000** (Revenue) |
| Track GST | Yes |
| GSTIN | **`36ADUFS9403L1Z9`** |
| State | **Telangana** (code 36) |
| Prices include GST | Yes |

## Masters

| Master | Value |
|--------|--------|
| SKU 1 | NS-SKU-01 · sell ₹450 |
| SKU 2 | NS-SKU-02 · sell ₹300 |
| Supplier | Deccan Supplies QA |
| Buyer | City Retail QA |
| Locations | Main DC · Branch Hyd |

## QuickAdd matrix (2 each — same ₹ as Android)

Execute in order so Collection/Payment have docs to allocate:

| # | QuickAdd | Post A | Post B |
|---|----------|--------|--------|
| 1 | PURCHASE | ₹45,000 received now | ₹30,000 receive later |
| 2 | SALE | ₹90,000 part paid (₹40k now) | ₹60,000 paid in full |
| 3 | COLLECTION | ₹40,000 vs Sale A | ₹25,000 vs Sale B |
| 4 | PAYMENT | ₹20,000 vs Purchase A | ₹15,000 vs Purchase B |
| 5 | SPEND | ₹8,000 paid | ₹5,000 pay later |
| 6 | STOCK | Set NS-SKU-01 → 200 | Add NS-SKU-02 +16 |
| 7 | ORDER | Customer SO ₹1,20,000 | Supplier PO ₹50,000 |
| 8 | SHIPMENT | Outgoing −60 | Incoming (vs Purchase B) |
| 9 | RETURN | Credit ₹9,000 vs Sale | Credit ₹3,000 vs Purchase |
| 10 | BUDGET | Budget → ₹5,50,000 | Target → ₹11,00,000 |
| 11 | UPDATE / ISSUE | Status update | Issue (blocker) |
| 12 | APPROVAL | Approve spend | Reject path |
| 13 | HOLD | Hold order | Release / Hold dispatch |
| 14 | TRANSFER | Main DC → Branch Hyd 10 | Reverse 5 |
| 15 | REPRINT | Sale A invoice | Second doc if offered (**no new event**) |

iOS hub appends **HOLD / TRANSFER / REPRINT** as utilities — use those tiles; do not invent SALE/STOCK from Hold/Reprint.

## Walk checklist

1. Create Trading Wholesale → Setup Basics → Channels & plan → GST (GSTIN + Telangana) → Activate.
2. Seed all six masters before QuickAdds.
3. Run matrix Posts A/B; screenshot each CTA.
4. Capture Pulse totals after money posts.
5. Log line:

```text
[iOS] PASS/FAIL/PARTIAL — purchases=… sales=… collections=… payments=… notes=
```

Results folder (create on Mac): `docs/qa/trading-device-walkthrough-2026-11-12/ios/` (`RESULTS-ios.md` + screenshots).

## Carry-over from Android

- Masters form overlay needed `Box(Modifier.fillMaxSize())` parent (`OperatingMastersSheet`) — confirm iOS masters create sheet is visible.
- Purchase/Sale require **supplier/buyer** selected; amount alone will not post.
- Dismiss keyboard before Save CTA (IME can cover primary button).
- Collection/Payment need open AR/AP; posting without allocation is not a valid pass.
- Honesty: no invented GST rates; HOLD ≠ revenue; REPRINT never posts; Collection ≠ Sale; Payment ≠ Purchase.

## JVM / backend gate (optional, Linux OK)

```bash
./scripts/qa/run-business-moments-automated.sh
```

See `docs/qa/BUSINESS_MOMENTS_AUTOMATED.md`.
