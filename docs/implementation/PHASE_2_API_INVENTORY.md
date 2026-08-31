# Phase 2 — API Endpoint Inventory

Authoritative contract: `backend/typescript/openapi/momentra-v1.yaml`  
Machine-readable: `backend/typescript/openapi/endpoint-inventory.json`  
Health (operational): `backend/typescript/openapi/health.yaml`

## Summary

| | |
|--|--|
| Total `/v1` operations | 61 |
| Commands | 34 |
| Reads | 27 |
| Implemented (router) | 58 |
| Contract only (Phase 3) | 3 (poll get/vote/close) |

## `/v1` product endpoints

| Method | Path | OperationId | Domain | Kind | Auth | Idempotency | OCC | Pagination | Impl |
|---|---|---|---|---|---|---|---|---|---|
| GET | /v1/me | getMe | Auth | read | yes | - | - | - | IMPLEMENTED |
| POST | /v1/me/devices | registerDevice | Devices | command | yes | - | - | - | IMPLEMENTED |
| DELETE | /v1/me/devices/{deviceId} | revokeDevice | Devices | command | yes | - | - | - | IMPLEMENTED |
| POST | /v1/moments | createMoment | Moments | command | yes | yes | - | - | IMPLEMENTED |
| GET | /v1/moments/{momentId} | getMoment | Moments | read | yes | - | - | - | IMPLEMENTED |
| PATCH | /v1/moments/{momentId} | updateMoment | Moments | command | yes | yes | yes | - | IMPLEMENTED |
| POST | /v1/moments/{momentId}/archive | archiveMoment | Moments | command | yes | yes | yes | - | IMPLEMENTED |
| POST | /v1/moments/{momentId}/cancel | cancelMoment | Moments | command | yes | yes | yes | - | IMPLEMENTED |
| GET | /v1/moments/{momentId}/activity | getMomentActivity | Activity | read | yes | - | - | yes | IMPLEMENTED |
| POST | /v1/moments/{momentId}/goals | createGoal | Work | command | yes | yes | - | - | IMPLEMENTED |
| POST | /v1/moments/{momentId}/milestones | createMilestone | Work | command | yes | yes | - | - | IMPLEMENTED |
| POST | /v1/moments/{momentId}/tasks | createTask | Work | command | yes | yes | - | - | IMPLEMENTED |
| POST | /v1/moments/{momentId}/expenses | createExpense | Finance | command | yes | yes | - | - | IMPLEMENTED |
| POST | /v1/moments/{momentId}/movements | createMovement | Finance | command | yes | yes | - | - | IMPLEMENTED |
| POST | /v1/moments/{momentId}/contributions | recordContribution | Finance | command | yes | yes | - | - | IMPLEMENTED |
| POST | /v1/moments/{momentId}/polls | createPoll | Poll | command | yes | yes | - | - | IMPLEMENTED |
| GET | /v1/polls/{pollId} | getPoll | Poll | read | yes | - | - | - | CONTRACT_ONLY |
| POST | /v1/polls/{pollId}/votes | votePoll | Poll | command | yes | yes | - | - | CONTRACT_ONLY |
| POST | /v1/polls/{pollId}/close | closePoll | Poll | command | yes | yes | yes | - | CONTRACT_ONLY |
| POST | /v1/moments/{momentId}/planning-items | createPlanningItem | Collaboration | command | yes | yes | - | - | IMPLEMENTED |
| POST | /v1/moments/{momentId}/bookings | createBooking | Collaboration | command | yes | yes | - | - | IMPLEMENTED |
| POST | /v1/moments/{momentId}/updates | postUpdate | Collaboration | command | yes | yes | - | - | IMPLEMENTED |
| POST | /v1/moments/{momentId}/participants | addParticipant | Collaboration | command | yes | yes | - | - | IMPLEMENTED |
| POST | /v1/moments/{momentId}/purchase-items | addPurchaseItem | Collaboration | command | yes | yes | - | - | IMPLEMENTED |
| POST | /v1/moments/{momentId}/residents | addResident | Collaboration | command | yes | yes | - | - | IMPLEMENTED |
| POST | /v1/moments/{momentId}/memories | createMemory | Collaboration | command | yes | yes | - | - | IMPLEMENTED |
| POST | /v1/moments/{momentId}/observations | recordObservation | Personal | command | yes | yes | - | - | IMPLEMENTED |
| POST | /v1/moments/{momentId}/future-items | createFutureItem | Personal | command | yes | yes | - | - | IMPLEMENTED |
| POST | /v1/moments/{momentId}/lifestyle-activities | createLifestyleActivity | Personal | command | yes | yes | - | - | IMPLEMENTED |
| POST | /v1/moments/{momentId}/relationship-activities | recordRelationshipActivity | Personal | command | yes | yes | - | - | IMPLEMENTED |
| GET | /v1/personal/pulse | getPersonalPulse | Personal | read | yes | - | - | - | IMPLEMENTED |
| GET | /v1/personal/moments | listPersonalMoments | Personal | read | yes | - | - | yes | IMPLEMENTED |
| GET | /v1/personal/life | getPersonalLife | Personal | read | yes | - | - | - | IMPLEMENTED |
| GET | /v1/personal/memory | getPersonalMemory | Personal | read | yes | - | - | - | IMPLEMENTED |
| GET | /v1/personal/attention | getPersonalAttention | Personal | read | yes | - | - | - | IMPLEMENTED |
| GET | /v1/personal/activity | getPersonalActivity | Personal | read | yes | - | - | yes | IMPLEMENTED |
| GET | /v1/group/moments | listGroupMoments | Group | read | yes | - | - | yes | IMPLEMENTED |
| GET | /v1/group/moments/{momentId}/pulse | getGroupMomentPulse | Group | read | yes | - | - | - | IMPLEMENTED |
| GET | /v1/group/moments/{momentId}/life | getGroupMomentLife | Group | read | yes | - | - | - | IMPLEMENTED |
| GET | /v1/group/moments/{momentId}/memory | getGroupMomentMemory | Group | read | yes | - | - | - | IMPLEMENTED |
| GET | /v1/group/moments/{momentId}/finance | getGroupMomentFinance | Group | read | yes | - | - | - | IMPLEMENTED |
| GET | /v1/group/moments/{momentId}/actions | getGroupMomentActions | Group | read | yes | - | - | - | IMPLEMENTED |
| GET | /v1/business/moments | listBusinessMoments | Business | read | yes | - | - | yes | IMPLEMENTED |
| GET | /v1/business/moments/{momentId}/pulse | getBusinessMomentPulse | Business | read | yes | - | - | - | IMPLEMENTED |
| GET | /v1/business/moments/{momentId}/life | getBusinessMomentLife | Business | read | yes | - | - | - | IMPLEMENTED |
| GET | /v1/business/moments/{momentId}/memory | getBusinessMomentMemory | Business | read | yes | - | - | - | IMPLEMENTED |
| GET | /v1/business/moments/{momentId}/finance | getBusinessMomentFinance | Business | read | yes | - | - | - | IMPLEMENTED |
| GET | /v1/business/moments/{momentId}/actions | getBusinessMomentActions | Business | read | yes | - | - | - | IMPLEMENTED |
| GET | /v1/companies | listCompanies | Business | read | yes | - | - | - | IMPLEMENTED |
| POST | /v1/companies | createCompany | Business | command | yes | yes | - | - | IMPLEMENTED |
| GET | /v1/companies/{companyId} | getCompany | Business | read | yes | - | - | - | IMPLEMENTED |
| PATCH | /v1/companies/{companyId} | updateCompany | Business | command | yes | yes | yes | - | IMPLEMENTED |
| GET | /v1/companies/{companyId}/locations | listCompanyLocations | Business | read | yes | - | - | - | IMPLEMENTED |
| POST | /v1/companies/{companyId}/locations | createCompanyLocation | Business | command | yes | yes | - | - | IMPLEMENTED |
| PATCH | /v1/companies/{companyId}/locations/{locationId} | updateCompanyLocation | Business | command | yes | yes | yes | - | IMPLEMENTED |
| GET | /v1/companies/{companyId}/teams | listCompanyTeams | Business | read | yes | - | - | - | IMPLEMENTED |
| POST | /v1/companies/{companyId}/teams | createCompanyTeam | Business | command | yes | yes | - | - | IMPLEMENTED |
| POST | /v1/media/uploads | createMediaUpload | Media | command | yes | yes | - | - | IMPLEMENTED |
| POST | /v1/media/uploads/{uploadId}/complete | completeMediaUpload | Media | command | yes | yes | - | - | IMPLEMENTED |
| GET | /v1/life360 | getLife360 | Circle | read | yes | - | - | - | IMPLEMENTED |
| POST | /v1/ai/action-proposals/{actionProposalId}/execute | executeActionProposal | AI | command | yes | yes | - | - | IMPLEMENTED |

## Health endpoints (not `/v1`)

| Method | Path | OperationId | Auth | Impl |
|---|---|---|---|---|
| GET | /health/live | healthLive | no | IMPLEMENTED |
| GET | /health/ready | healthReady | no | IMPLEMENTED |

## Legacy routes (not in authoritative spec)

| Route pattern | Classification | Notes |
|---|---|---|
| `openapi/v1.yaml` paths | DEPRECATE_LATER | Personal-scoped duplicate engines |
| `GET/POST /v1/telemetry/*` | KEEP | Client telemetry; out of product contract |
| Hand-maintained `ApiService.kt` | ADAPT | Migrate to generated client in Phase 3 |

## Primary consumers

| Domain | Android | iOS |
|--------|---------|-----|
| Auth/Me | Hand-maintained + generated baseline | Bootstrap `APIClient` + generated baseline |
| Projections | Shell ViewModels (Phase 4+) | Phase 5+ |
| Commands | Action Center chips | Phase 5+ |
