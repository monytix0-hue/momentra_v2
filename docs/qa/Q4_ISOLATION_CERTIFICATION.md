# Q4 Isolation Certification

**Release-blocking.** Any tenant/user leak = **P0**.

## Matrices

### Personal
- U1 × P1/P2/P3/P4
- U2 × P1/P2/P3/P4
- No cross-user cache/data leakage

### Group
- U1 = owner, U2 = member, U3 = outsider
- Moment A vs Moment B
- U2 can access A; U2 cannot access B if not member; U3 cannot access either
- Write into A → nothing appears in B

### Business
- Company A (Moment A1, A2) vs Company B (Moment B1)
- Switch A → B: **no temporary flash** of Company A Pulse / Activity / balances / Moment / Quick Add data

## Journey

`.maestro/cert/android/isolation/q4_isolation.yaml`  
`.maestro/cert/ios/isolation/q4_isolation.yaml`

## Backend

`qa:verify --sibling-moment-id …` asserts **No cross-Moment write**.

## Status

Journey **IMPLEMENTED**. Device execution **PENDING**. Isolation gate checkbox remains ✗ until green run + evidence under `.maestro/reports/<RUN_ID>/isolation/`.
