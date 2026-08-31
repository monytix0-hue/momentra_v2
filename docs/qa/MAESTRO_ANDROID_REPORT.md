# Maestro Android Report — S9-QA-A

**Date:** 2026-08-27  
**App:** `com.example.momentra` (debug)  
**Host:** Windows  
**Device:** emulator-5554 (Pixel_10_Pro_XL) — physical preferred when available  
**Fixtures:** `npm run qa:prepare-fixtures` (seeded)

## Gate checklist

| Area | Status | Notes |
|------|--------|-------|
| SMOKE | **PASS** (physical `00158357G000049` A059) | Retry 2026-08-27 ~20:32 — 6/6 PASS |
| AUTH | **PASS** | smoke_email_login |
| PERSONAL | READY | Critical not run yet |
| GROUP | READY | Critical not run yet |
| BUSINESS | READY / PRODUCT_GAP | Critical not run yet |
| SETTINGS/SECURITY | PENDING | |
| ISOLATION | READY | Not run yet |
| REGRESSION | PENDING | |
| P0 | **0** | |
| P1 | **1 open** | QA-A-001 onboarding.skip id (mitigated by text) |

## A1 Fixtures

| Item | Status |
|------|--------|
| Deterministic QA accounts (9) | **PASS** — Firebase + Postgres via seed |
| `reset-maestro-fixtures.ts` | **PASS** — guarded (`QA_FIXTURES_ENABLED`, non-prod) |
| `seed-maestro-fixtures.ts` | **PASS** — Group A/B, Company A/B, multi-context |
| `.env.maestro.local` | Updated by seed (gitignored) |

## A2 Accessibility IDs

| Family | Android wired | iOS parity |
|--------|---------------|------------|
| Shell nav / context / bottom | Yes | Yes |
| `moment.switcher` / `company.switcher` | Yes | Yes |
| `personal.expense.*` | Yes | Partial |
| `group.expense.*` / `group.invite.code` | Yes | Pending |
| `business.expense.*` | Yes | Pending |
| revenue / invoice / approval | Constants only | PRODUCT_GAP |

## Evidence

- Fixture summary: `.maestro/reports/qa_fixtures_last.json` (if written) / seed stdout  
- Smoke probe: `.maestro/reports/qa_android_smoke_probe` — onboarding→login **PASS**  
- Full smoke: `.maestro/reports/qa_android_smoke_*`

## Blockers / defects

| ID | Sev | Title | Status |
|----|-----|-------|--------|
| QA-A-001 | P1 | `onboarding.skip` testTag not exposed as resource-id on TextButton (text "Skip" works) | Open — text fallback in smoke |
| QA-A-002 | P2 | Business revenue/invoice/approval UI absent on Android | PRODUCT_GAP |

## Stop line

S9-L→P remains **frozen**. iOS execution = **BLOCKED_ENVIRONMENT** on Windows.
