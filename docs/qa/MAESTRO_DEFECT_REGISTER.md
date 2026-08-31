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
| MQA-A-001 | P1 | Android | Auth | — | onboarding.skip | Maestro id visible | Text fallback only | OPEN (was QA-A-001) |
| MQA-A-002 | P2 | Android | Business | B01–B03 | Revenue/Invoice/Approval UI | Sheets wired | PRODUCT_GAP / ANDROID_MISSING on some surfaces | OPEN (was QA-A-002) |
| MQA-A-003 | P2 | Android | Auth | — | skip wait | Clean wait | Optional WARN | Accepted |

**P0 = 0** · **P1 = 1** (blocks Master Certification PASS)

## Q7 loop

execute → defect → classify → fix P0/P1 → targeted rerun → full affected Moment rerun
