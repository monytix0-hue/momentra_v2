# DPDP compliance note (Momentra engineering)

**Not legal advice.** Records Act-aligned product controls vs [Digital Personal Data Protection Act, 2023](https://www.meity.gov.in/static/uploads/2024/06/2bf1f0e9f04e6fb4f8fef35e82c42aa5.pdf). Gap table: [DPDP_GAP_AUDIT.md](./DPDP_GAP_AUDIT.md).

Updated: 2026-11-13 (Asia/Calcutta)

## Claim rule

- In-app / marketing may say the product is **designed to meet DPDP Act obligations** only after counsel clears the Privacy DRAFT banner.
- Do **not** claim “certified compliant” or “fully compliant” without counsel sign-off.

## Shipped in product

| Theme | Act | What shipped |
|-------|-----|--------------|
| Age gate 18+ | s.9 | Consent gate + prefs `momentra_age_gate_accepted_18plus` (legacy 13+ insufficient) |
| Notice | s.5 | Privacy draft; consent gate; login Privacy/Terms links |
| Consent | s.6 | Catalogue, grant/withdraw + audits, Account toggles |
| Access | s.11 | `GET /v1/me/export` + Download my data |
| Correction | s.12 | `PATCH /v1/me` |
| Principal-owned erasure | s.8(7), s.12 | `GET /v1/me/erasure-preflight` + `DELETE /v1/me` (`eraseMe`) |
| Grievance contact | s.8(9), s.13 | `hello@momentra.app`; Account mailto; 72h ack / 14d resolve target |
| Processors register | s.8(2) | [`SUBPROCESSORS.md`](./SUBPROCESSORS.md) |
| Ops erasure / breach stub | s.8(6)–(7) | [`DPDP_ERASURE_RUNBOOK.md`](./DPDP_ERASURE_RUNBOOK.md) |

## Erasure semantics (2A)

- **Erased / anonymized:** profile PII (tombstone UUID kept), devices/tokens, consents, notifications, personal moments, sole-member groups, USER-scoped media.
- **Left shared:** Group/Business content for other members after leave; organizer/owner with other members must transfer first (preflight blockers).
- **Retained:** audit/security events; shared MOMENT/COMPANY media still referenced.

## Still counsel / GoI

- Lawyer-final Privacy/Terms (remove DRAFT).
- Processor DPAs.
- s.14 nomination when prescribed.
- s.10 SDF only if notified.
- Board “as prescribed” forms.
