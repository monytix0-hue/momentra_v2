# Maestro Defect Register — S9-QA Master Certification

## Severity

| Sev | Meaning |
|-----|---------|
| P0 | security / isolation / corruption / wrong money / authentication bypass |
| P1 | critical action broken / incorrect write / Quick Add unusable / major setup failure / approval wrong |
| P2 | secondary functionality / significant Figma mismatch |
| P3 | cosmetic/polish |

## Required evidence fields

`DEFECT-ID` · severity · platform · context · Moment · screen · Quick Add · expected · actual · Figma node · correlationId · runId · API response · backend log · screenshot · status · fix commit · rerun result

## Open defects

| ID | Sev | Platform | Context | Moment | Feature | Expected | Actual | Status |
|----|-----|----------|---------|--------|---------|----------|--------|--------|
| MQA-A-001 | P1 | Android | Auth | — | onboarding.skip | Maestro id visible | Fixed: ProductOnboardingScreen Skip now uses `testTag(MaestroIds.ONBOARDING_SKIP)` (S9-QA-A) | **FIXED** — pending device rerun |
| MQA-A-002 | P2 | Android | Business | B01–B03 | Revenue/Invoice/Approval UI | Sheets wired | Runway revenue/invoice IDs wired (S9-QA-A); Approval still PRODUCT_GAP | OPEN (partial) |
| MQA-A-003 | P2 | Android | Auth | — | skip wait | Clean wait | Optional WARN | Accepted |
| MQA-A-004 | P1 | Android | Group | G02 | Wedding Settle hub tile | Present on hub | Missing on Android Wedding hub (iOS has it) | OPEN — catalog `BROKEN` |
| MQA-A-005 | P2 | Both | Personal | P1 | Income/Expense label | Same label | Android Income / iOS Expense | OPEN — documented label drift |

**P0 = 0** · **P1 open = 1** (MQA-A-004) · MQA-A-001 pending device confirmation

## Q7 loop

execute → defect → classify → fix P0/P1 → targeted rerun → full affected Moment rerun
