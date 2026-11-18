# QuickAdd wiring honesty notes (2026-11-18)

Program: **Moments QuickAdds / sheets / toasts** (Tasks 1–17 wiring + this note). Audience: QA and reviewers verifying behavior without expecting out-of-scope fixes.

## Known divergences and intentional limits

1. **iOS hub-as-sheet vs Android CREATE tab** — Presentation differs by platform (iOS Quick Add hub as a sheet; Android hub on the CREATE tab). Toast and deep-link parity rely on the shared operating toast host and Personal/operating CREATE hubs wired in Tasks 1–6 and 15. **No hub UI unification in this program.**

2. **Marketing HOLD / TRANSFER / REPRINT (non-post)** — Own-hub and chrome paths open **chrome-only** previews/sheets. Saves do **not** post inventory, revenue, or print jobs. Intentional UX honesty (Task 16); no new Marketing APIs.

3. **Retail POS local hold / missing APIs** — Park/hold and some utility flows may use local/session state where backend routes are absent or partial. UI must state limits; **no new Retail POS APIs** in this program.

4. **Future / Lifestyle / Relationships stub families** — Tiles outside P1 Life Operations on Personal CREATE remain **hidden or non-tappable** stubs. No new sheets or precision flows here (YAGNI).

5. **Figma node ID remaps** — Implementation follows current code and QA copy contracts. **Figma file node IDs were not re-audited or remapped** in this program; treat stale node references as documentation-only.

6. **Personal Transfer API (single `accountId`)** — `createMovement` accepts one `accountId` (source). Transfer UI requires a distinct TO account for Save, but the counterparty is recorded in **description** (`to=<accountName>`), not a separate `toAccountId`. Do not treat missing dual-leg API as a wiring bug.

## Self-review vs audit “Out of scope (honesty-only)”

| Audit out-of-scope item | Covered in this doc |
|-------------------------|---------------------|
| Marketing HOLD/TRANSFER/REPRINT intentional non-post | §2 |
| Retail POS local hold / missing APIs | §3 |
| Future/Lifestyle/Relationships stub families | §4 |
| Figma node ID remaps — note only | §5 |
| iOS Hub-as-sheet vs Android CREATE tab — document | §1 |
| Transfer TO / single-account API (P1 honesty) | §6 |