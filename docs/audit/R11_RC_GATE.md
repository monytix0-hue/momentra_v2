# R11 Release Candidate Gate

Checklist derived from remediation playbook §11. Run `npm run audit:remediation-dashboard` before sign-off.

## Root register

- [ ] 60/60 root gaps `CLOSED` or `DEFERRED` (SP-017, SP-018 pre-deferred)
- [ ] 0 P0 open without approved deferral
- [ ] 0 required-V1 P1 open without approved deferral

## Supplemental acceptance

- [ ] 25/25 SUPP-* criteria `PASS` or `DEFERRED`

## Vertical certification

- [ ] 19/19 Maestro moment journeys `PASS` with DB/outbox/projection evidence in `docs/audit/14-e2e-flow-evidence/`

## Contract & parity

- [ ] 0 unexplained OpenAPI drift (`03-openapi-backend-reconciliation.csv`)
- [ ] 0 unexplained iOS/Android parity gaps (`04-ios-android-parity.csv`)
- [ ] 0 unclassified SQL tables (`07-table-utilization.csv`)

## Platform NFR

- [ ] SP-015 performance gate PASS
- [ ] SP-016 observability/release gate PASS
- [ ] Fresh + upgrade migration PASS on clean PostgreSQL

## Sign-off

| Role | Name | Date |
|------|------|------|
| Backend lead | | |
| Mobile lead | | |
| QA lead | | |

Upon approval: tag `rc/v1.0.0`, freeze register, begin store release path.
