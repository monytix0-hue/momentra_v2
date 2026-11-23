# Restaurant Setup Figma + Draft

**Goal:** Make `RESTAURANT_FOOD` setup match Figma `5:90612` (RST-SETUP-v6) on iOS and Android, with Group-style Save draft / Schedule later, field-level activate errors, and backend preference aliases.

**Out of scope:** §04 Menu / Stations / Vendors masters sheets and operating-masters CRUD from setup.

**In scope for §04:** Static Figma-labelled module list (Menu, Stations, Vendors, Team invite & QR) as display-only rows.

## Tasks

1. Rewrite `RESTAURANT_FOOD` in `BusinessSetupDocuments` (Swift + Kotlin) to Figma field model.
2. StickyActivateBar: Save draft | Schedule later (dismiss) parity with Group.
3. Field errors via existing `OperatingSetupPayloadBuilder.fieldErrors` + DocumentKit chrome.
4. Backend/client aliases: `restaurantName` → `outletName`, `monthlySalesTarget` → `monthlyTarget`, `waiter` role; rebuild `backend/dist`.
5. Smoke: draft → resume → activate (no §04 masters taps).

## Smoke checklist

- [ ] Open Restaurant chip → Spice Route Setup hero, CTA **Start this restaurant**
- [ ] Fill name + company + at least one channel → Save draft succeeds
- [ ] Schedule later dismisses without requiring activate
- [ ] Resume draft prefills values
- [ ] Activate with empty channels shows field error on Channels
- [ ] §04 Menu/Stations/Vendors are visible but not interactive sheets
