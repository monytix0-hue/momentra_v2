# DPDP Act gap audit (Momentra)

**Statute:** [Digital Personal Data Protection Act, 2023 (No. 22 of 2023)](https://www.meity.gov.in/static/uploads/2024/06/2bf1f0e9f04e6fb4f8fef35e82c42aa5.pdf) (MeitY gazette PDF).

**Not legal advice.** Status values: **EXISTS** · **PARTIAL** · **MISSING** · **LOCKED** · **COUNSEL** · **N/A**. Rules under the Act (“as may be prescribed”) and Board notifications may change operational detail.

Updated: 2026-11-13 (Asia/Calcutta)

## Product decisions (locked)

- **Age gate 18+** (no under-18 accounts; no parental-consent product path).
- **Principal-owned erasure** on `DELETE /v1/me`: erase/anonymize principal data; leave shared Group/Business graphs for other principals. Still forbid DROP/TRUNCATE of shared tables / other users’ data.

## Section map

| Act | Obligation | Evidence | Status |
|-----|------------|----------|--------|
| s.4 | Consent or legitimate use | Consent catalogue + core service use named in Privacy Policy | **EXISTS** (product) |
| s.5 | Notice | [`web/legal/privacy.html`](../../web/legal/privacy.html); consent gate; login Privacy/Terms links | **EXISTS** (product; DRAFT until counsel) |
| s.6 | Consent / easy withdraw / prove notice | Grant/withdraw APIs + audits; Account toggles | **EXISTS** |
| s.7 | Legitimate uses | Named in Privacy Policy | **EXISTS** (product) |
| s.8(1)–(5) | Fiduciary / processors / security | [`SUBPROCESSORS.md`](./SUBPROCESSORS.md); security docs | **PARTIAL** — DPAs are counsel/ops |
| s.8(6) | Breach intimation | [`DPDP_ERASURE_RUNBOOK.md`](./DPDP_ERASURE_RUNBOOK.md) | **PARTIAL** (Board forms TBD) |
| s.8(7)–(8) | Erase on withdraw / purpose ended | `eraseMe` + preflight; shared retention | **EXISTS** (principal-owned) |
| s.8(9)–(10) | Contact + grievance | `hello@momentra.app`; Account mailto; 72h/14d target | **EXISTS** (product) |
| s.9 | Child = under 18 | Gate **18+** Android/iOS; Privacy/Terms | **EXISTS** |
| s.10 | Significant Data Fiduciary | Not notified | **N/A** |
| s.11 | Access summary | `GET /v1/me/export` + Download my data | **EXISTS** |
| s.12 | Correction / erasure | `PATCH /v1/me`; `DELETE /v1/me` eraseMe | **EXISTS** (product) |
| s.13 | Grievance before Board | Email + Account; SLA target documented | **EXISTS** (product; prescribed period TBD) |
| s.14 | Nominate | Deferred until manner prescribed | **MISSING** (rules TBD) |
| s.16 | Cross-border restrictions | Policy: honour GoI restricted list when notified | **PARTIAL** |

## Counsel / ops still required for public “compliant” claim

- Lawyer-reviewed Privacy + Terms; remove DRAFT banner.
- Written processor contracts / DPAs.
- Board breach/grievance templates when prescribed.
- Optional: nomination UI when rules prescribe manner.

## Related docs

- [DPDP_COMPLIANCE_NOTE.md](./DPDP_COMPLIANCE_NOTE.md)
- [DPDP_ERASURE_RUNBOOK.md](./DPDP_ERASURE_RUNBOOK.md)
- [SUBPROCESSORS.md](./SUBPROCESSORS.md)
- [LAUNCH_DECISIONS.md](./LAUNCH_DECISIONS.md)
