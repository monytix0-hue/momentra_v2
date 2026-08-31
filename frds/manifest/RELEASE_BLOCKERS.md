# Momentra Database Release Blockers — Phase 11.9

The SQL handoff package is finalized, but it is **not a Production Release Candidate**.

## BLOCKER-001 — P0 — Analytics production semantics

The 14 Metric Definitions are seeded, but production Metric Versions remain DRAFT. Exact formula bodies, units/time windows/null handling and applicable thresholds must be approved and activated through reviewed SQL.

## BLOCKER-002 — P0 — Governance production semantics

The 9 Governance Policies are seeded, but Policy Versions remain DRAFT. Exact rule bodies, approval semantics, monetary cut-offs, lifecycle exceptions and consent conditions must be approved and activated.

## BLOCKER-003 — P1 — Group Quick Add subtype reconciliation

V019 contains deterministic capability mappings based on the compiled family matrices. The final per-Moment-Type subsets must be reconciled against the previously frozen product/Figma matrix before RC.

## BLOCKER-004 — P1 — Role/Permission reconciliation

The current V020 Role-Permission matrix is coherent and least-privilege, but it must be reconciled against the authoritative frozen Governance/Phase 6.6 matrix if that matrix differs.

## BLOCKER-005 — P0 — Phase 11.8 engine execution deferred

The exact V001–V030 checksummed package has not yet been executed end-to-end on a clean PostgreSQL engine or Development Supabase. Static QA cannot prove PL/pgSQL execution, PostgreSQL runtime semantics, Supabase `auth.uid()`, RLS helper behavior, grants, custom worker roles, service-role behavior, or V030 runtime success.

## Release rule

Production RC requires all P0 blockers closed, all required P1 blockers closed or formally dispositioned, and the exact release checksums to pass Phase 11.8 and subsequent verification gates.
