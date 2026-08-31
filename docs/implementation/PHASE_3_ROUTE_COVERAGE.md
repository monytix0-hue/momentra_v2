# Phase 3 — Route Coverage

Compares authoritative OpenAPI (`momentra-v1.yaml`) to runtime router implementation.

**Generated:** 2026-08-20

## Summary

| Metric | Count |
|--------|------:|
| OpenAPI /v1 operations | 61 |
| IMPLEMENTED | 58 |
| CONTRACT_ONLY | 3 |
| Deferred / GAP (not in OpenAPI) | 4 |

Phase 3 does **not** claim all 61 APIs are production-hardened. Platform foundation is proven via health, `/v1/me`, and device registration (transactional proof). Remaining routes retain prior router wiring but are not Phase 3 vertical slices.

## /v1 coverage

| Method | Path | OperationId | Domain | Status |
|---|---|---|---|---|
| GET | /v1/me | getMe | Auth | IMPLEMENTED |
| POST | /v1/me/devices | registerDevice | Devices | IMPLEMENTED |
| DELETE | /v1/me/devices/{deviceId} | revokeDevice | Devices | IMPLEMENTED |
| POST | /v1/moments | createMoment | Moments | IMPLEMENTED |
| GET | /v1/moments/{momentId} | getMoment | Moments | IMPLEMENTED |
| PATCH | /v1/moments/{momentId} | updateMoment | Moments | IMPLEMENTED |
| POST | /v1/moments/{momentId}/archive | archiveMoment | Moments | IMPLEMENTED |
| POST | /v1/moments/{momentId}/cancel | cancelMoment | Moments | IMPLEMENTED |
| GET | /v1/moments/{momentId}/activity | getMomentActivity | Activity | IMPLEMENTED |
| POST | /v1/moments/{momentId}/goals | createGoal | Work | IMPLEMENTED |
| POST | /v1/moments/{momentId}/milestones | createMilestone | Work | IMPLEMENTED |
| POST | /v1/moments/{momentId}/tasks | createTask | Work | IMPLEMENTED |
| POST | /v1/moments/{momentId}/expenses | createExpense | Finance | IMPLEMENTED |
| POST | /v1/moments/{momentId}/movements | createMovement | Finance | IMPLEMENTED |
| POST | /v1/moments/{momentId}/contributions | recordContribution | Finance | IMPLEMENTED |
| POST | /v1/moments/{momentId}/polls | createPoll | Poll | IMPLEMENTED |
| GET | /v1/polls/{pollId} | getPoll | Poll | CONTRACT_ONLY |
| POST | /v1/polls/{pollId}/votes | votePoll | Poll | CONTRACT_ONLY |
| POST | /v1/polls/{pollId}/close | closePoll | Poll | CONTRACT_ONLY |
| POST | /v1/moments/{momentId}/planning-items | createPlanningItem | Collaboration | IMPLEMENTED |
| POST | /v1/moments/{momentId}/bookings | createBooking | Collaboration | IMPLEMENTED |
| POST | /v1/moments/{momentId}/updates | postUpdate | Collaboration | IMPLEMENTED |
| POST | /v1/moments/{momentId}/participants | addParticipant | Collaboration | IMPLEMENTED |
| POST | /v1/moments/{momentId}/purchase-items | addPurchaseItem | Collaboration | IMPLEMENTED |
| POST | /v1/moments/{momentId}/residents | addResident | Collaboration | IMPLEMENTED |
| POST | /v1/moments/{momentId}/memories | createMemory | Collaboration | IMPLEMENTED |
| POST | /v1/moments/{momentId}/observations | recordObservation | Personal | IMPLEMENTED |
| POST | /v1/moments/{momentId}/future-items | createFutureItem | Personal | IMPLEMENTED |
| POST | /v1/moments/{momentId}/lifestyle-activities | createLifestyleActivity | Personal | IMPLEMENTED |
| POST | /v1/moments/{momentId}/relationship-activities | recordRelationshipActivity | Personal | IMPLEMENTED |
| GET | /v1/personal/pulse | getPersonalPulse | Personal | IMPLEMENTED |
| GET | /v1/personal/moments | listPersonalMoments | Personal | IMPLEMENTED |
| GET | /v1/personal/life | getPersonalLife | Personal | IMPLEMENTED |
| GET | /v1/personal/memory | getPersonalMemory | Personal | IMPLEMENTED |
| GET | /v1/personal/attention | getPersonalAttention | Personal | IMPLEMENTED |
| GET | /v1/personal/activity | getPersonalActivity | Personal | IMPLEMENTED |
| GET | /v1/group/moments | listGroupMoments | Group | IMPLEMENTED |
| GET | /v1/group/moments/{momentId}/pulse | getGroupMomentPulse | Group | IMPLEMENTED |
| GET | /v1/group/moments/{momentId}/life | getGroupMomentLife | Group | IMPLEMENTED |
| GET | /v1/group/moments/{momentId}/memory | getGroupMomentMemory | Group | IMPLEMENTED |
| GET | /v1/group/moments/{momentId}/finance | getGroupMomentFinance | Group | IMPLEMENTED |
| GET | /v1/group/moments/{momentId}/actions | getGroupMomentActions | Group | IMPLEMENTED |
| GET | /v1/business/moments | listBusinessMoments | Business | IMPLEMENTED |
| GET | /v1/business/moments/{momentId}/pulse | getBusinessMomentPulse | Business | IMPLEMENTED |
| GET | /v1/business/moments/{momentId}/life | getBusinessMomentLife | Business | IMPLEMENTED |
| GET | /v1/business/moments/{momentId}/memory | getBusinessMomentMemory | Business | IMPLEMENTED |
| GET | /v1/business/moments/{momentId}/finance | getBusinessMomentFinance | Business | IMPLEMENTED |
| GET | /v1/business/moments/{momentId}/actions | getBusinessMomentActions | Business | IMPLEMENTED |
| GET | /v1/companies | listCompanies | Business | IMPLEMENTED |
| POST | /v1/companies | createCompany | Business | IMPLEMENTED |
| GET | /v1/companies/{companyId} | getCompany | Business | IMPLEMENTED |
| PATCH | /v1/companies/{companyId} | updateCompany | Business | IMPLEMENTED |
| GET | /v1/companies/{companyId}/locations | listCompanyLocations | Business | IMPLEMENTED |
| POST | /v1/companies/{companyId}/locations | createCompanyLocation | Business | IMPLEMENTED |
| PATCH | /v1/companies/{companyId}/locations/{locationId} | updateCompanyLocation | Business | IMPLEMENTED |
| GET | /v1/companies/{companyId}/teams | listCompanyTeams | Business | IMPLEMENTED |
| POST | /v1/companies/{companyId}/teams | createCompanyTeam | Business | IMPLEMENTED |
| POST | /v1/media/uploads | createMediaUpload | Media | IMPLEMENTED |
| POST | /v1/media/uploads/{uploadId}/complete | completeMediaUpload | Media | IMPLEMENTED |
| GET | /v1/life360 | getLife360 | Circle | IMPLEMENTED |
| POST | /v1/ai/action-proposals/{actionProposalId}/execute | executeActionProposal | AI | IMPLEMENTED |

## Deferred / GAP (not invented in Phase 3)

| Item | Status |
|------|--------|
| Circle CRUD | DEFERRED — `GET /v1/life360` only |
| Settlement command | GAP |
| Budget command | GAP |
| Vendor command | GAP |
| Poll vote/close | CONTRACT_ONLY (OpenAPI frozen; router not implemented) |

## Health (separate spec)

| Method | Path | Status |
|--------|------|--------|
| GET | /health/live | IMPLEMENTED |
| GET | /health/ready | IMPLEMENTED |

## Notes

- `POST /v1/me/devices` is the Phase 3 transactional proof command (idempotency + audit + event + outbox).
- `projectionHints` runtime now emits typed `{ projection, action }` objects.
