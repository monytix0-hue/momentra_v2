# Gap 3 — Business Life projection integrity

Date: 2026-09-30.

Classification: **PASS**

One Life row per company now keeps Money, Daily, Team, and vendor slices independently. Company runway and health move only when Money refreshes. Opening Life through a different moment in the same company returns the same family payloads, runway months, and health score. B3 Life UI was not started.

## 1. Product lock

One Life per company. Several Business moments contribute. Switching company replaces Life. Switching moment inside the company does not.

That is the contract a later B3 screen can sit on: Overview, Money, Daily, and Team are lenses on this company row, not separate Life records.

## 2. Column mapping

No new table and no column rename. `projection.business_life` stays keyed by `company_id`.

- `runway_payload` — Money slice
- `business_operations_payload` — Daily slice
- `team_operations_payload` — Team slice
- `vendor_operations_payload` — vendor slice, owned by Daily
- company finance — `projection.business_finance_snapshot`, plus `business_pulse.runway_months` and `financial_health_score`, updated only by a Money refresh
- company metadata — active moment count and journey, already company-scoped

## 3. What changed

`refreshBusinessLifeProjection` already kept Money, Daily, and Team JSON with `CASE WHEN` on the family flags. Vendor did not. The payload was always a filled object, and the conflict clause wrote it whenever that object was not `{}`, so every family refresh replaced the vendor slice. Vendor is now written only when the refresh is Daily (`BUSINESS_OPERATIONS`, not Team). Money and Team leave the stored vendor JSON untouched, including on first insert.

`refreshBusinessPulseProjection` used to replace `runway_months`, `financial_health_score`, and the whole `widget_payload` from whichever moment refreshed, and it appended that moment’s health into `business_pulse_history`. Only a Money refresh may set those three pulse fields, the money category breakdown on the finance snapshot, and the health and runway history scores. Daily may update the ops history score. Team may update the team history score. A non-Money refresh does not replace a stored runway or health score with null or with that family’s defaults.

`open_issue_count` on `business_pulse` is still the refreshing moment’s count. Life does not display it. Daily Pulse still reads live issue counts. It was left as-is.

The Life read still calls `assertCompanyMomentAccess`. The moment is only the membership handle. Scores and signals no longer use that caller moment, and the read no longer stamps the caller moment’s capacity or month deltas onto the stored family JSON.

## 4. Canonical family moment

The domain does not guarantee one active moment per family. `business.business_moment_context` is unique on `(moment_id, business_family)` only. The index on `(company_id, business_family, status)` is not unique.

When Life needs a family’s setup, capacity, budget, or SLA, it uses the canonical moment:

- status `ACTIVE`
- matching family (`BUSINESS_RUNWAY`, `BUSINESS_OPERATIONS`, or `TEAM_OPERATIONS`)
- earliest `created_at`
- `moment_id` ascending as the tie-break

A second moment of the same family does not move that pick. This pass does not add a uniqueness migration. If that family has no active moment, the slice stays empty. A missing amount is not coerced to `0`. A stored `0` stays `0`.

## 5. Tests

`npx tsx --test tests/business-gap3-life.test.ts` — 4 passed.

- Money, then Daily, then Team matches Team, then Daily, then Money for the three family payloads, the vendor slice, runway months, and health.
- A later Daily refresh does not clear Money cash, Team capacity, vendor count, runway months, or health. A later Team refresh does not clear Money or Daily. A later Money refresh does not clear Daily, Team, or vendor.
- `GET` Life through the Money moment and through the Daily moment returns the same family payloads, runway months, health score, signals, and module scores.
- Company B’s Life does not contain Company A’s cash, issue title, or expense title.
- Two active Money moments with the same `created_at`: Life uses the lower `moment_id`. After the other Money moment refreshes last, the runway signal still uses the canonical 9-month threshold, not the other moment’s 24-month threshold. The three Life reads match.
- A Money slice with available cash `0` stays `0`. A removed monthly revenue stays null. Team and Daily, never refreshed, stay `{}` and `EMPTY_SUPPORTED`.

`npx tsx --test tests/business-company-life.test.ts` — 3 passed.

No Android or iOS compile. This pass does not change clients.

## 6. Still untouched

- realtime
- location-scoped totals
- Memory last-writer
- device runtime smoke

B3 Life UI starts only after this report. It was not started here.
