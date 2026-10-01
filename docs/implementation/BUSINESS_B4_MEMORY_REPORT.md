# Business B4 — Memory UI

Date: 2026-10-01.

Classification: **PASS CANDIDATE — runtime verification pending**

B3 stays **PASS CANDIDATE — runtime verification pending**. No device smoke was run. iOS sources and tests were written and were not compiled on this Windows host.

## What the screen shows

One company Memory surface answers “What has this business learned?” from `GET /v1/business/moments/:id/memory`. The opening moment is the membership handle and the initial lens. Money, Daily, and Team, and the fallback type, all open this screen. Small shop and growing use the same screen.

The company payload builds the hero, the one pattern, what worked / what didn’t, and Then → Now. The lens is not an input to that mapper. Overview, Money, Daily, and Team filter Worth remembering only. Changing the chip does not refetch. A company change clears the list. A moment change inside the company keeps the last company payload and moves the chip. A response for a different company is ignored.

1. Memory hero — the local period from the earliest and latest `occurredAt` (`d MMM yyyy – d MMM yyyy` in the device zone; tests use Asia/Kolkata). One local date uses that date. No dates use “Company memory”. The sentence is the item count, “N saved memories.”, or “Nothing saved yet.”
2. Worth remembering — title, family label (Money, Daily, Team), and the local date when `occurredAt` parses. Empty copy is “Nothing saved yet.”
3. Pattern worth knowing — one pattern, and only when at least two items share evidence. `businessMemoryIsRisk` matches only `risk`, `issue`, and `incident`. A repeated `memoryType` other than blank or `GENERAL` is the other candidate. The larger group wins. A tie keeps the risk group. See evidence lists those titles. Nothing repeats, so the card is omitted.
4. What worked / What didn’t — worked is everything that is not risk evidence. Empty lines stay “No success memories yet.” and “No risk memories yet.” These lists are not filtered by the chip.
5. Then → Now — the earliest and latest local calendar dates. It is omitted when every parsed timestamp falls on the same local date, including two instants that cross UTC midnight and stay on one Asia/Kolkata day. A missing date does not draw an empty card.

Record learning still opens the existing Quick Add path. The empty Create Memory chooser is not wired. Pattern network, playbook, wisdom, knowledge journey, accuracy, substring chips, and `items.size / 3` are off this screen.

Swift `BusinessMemoryItem` now decodes optional `body`, `occurredAt`, `memoryType`, and `businessFamily`.

## Tests

`apk\gradlew.bat :app:testDebugUnitTest --tests com.example.momentra.ui.shell.business.shared.BusinessMemoryPresentationTest --offline` passed, and debug Kotlin compiled.

Covered: the mapper is called without a lens and Money / Daily chips change only Worth remembering; `delay` and `overdue` are not risk; a repeated type is the one pattern and a single memory is not; Then → Now appears for two local dates; `2026-09-29T20:30:00Z` and `2026-09-30T02:00:00Z` are both 30 Sep 2026 in Asia/Kolkata and omit Then → Now; zero items omit the pattern and Then → Now; an empty body is not a learning sentence.

`momentra/momentraTests/BusinessMemoryPresentationTests.swift` was not run.

## Still untouched

Realtime, location-scoped totals, and new Memory intelligence. This pass does not change the Memory projection.
