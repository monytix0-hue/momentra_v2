# Business B3 — Life UI

Date: 2026-09-30.

Classification: **PASS CANDIDATE — runtime verification pending**

Life now renders one company state. Money, Daily, and Team change only the selected lens. No device smoke was run. iOS sources and tests were written and were not compiled on this Windows host.

## What the screen shows

The dashboard is five blocks on the Gap 3 company payload:

1. Company overview — Money, Daily Business, and Team as states. An empty slice is “Not set up”. A stored status is used when it is not the generic family title.
2. This week — a count of recent company events whose time falls in the last seven days.
3. Where the business moved — Activity and Money. A Money lens opens Money. The lists themselves stay the company set.
4. Needs attention / What’s working — company signals. Action and Watch are attention. Healthy is working.
5. Financial position — labeled Company totals. A stored zero stays. A missing amount is omitted.

Opening Life from another moment in the same company keeps the last company payload and moves the lens. Switching company clears that payload. Share and the weekly report stay as actions under the blocks.

## Tests

`apk\gradlew.bat :app:testDebugUnitTest --tests com.example.momentra.ui.shell.business.shared.BusinessLifePresentationTest --offline` passed, and debug Kotlin compiled.

`momentra/momentraTests/BusinessLifePresentationTests.swift` was not run.

## Still untouched

Realtime, location-scoped totals, Memory last-writer, and device runtime smoke. Memory last-writer is Gap 4, not this screen.
