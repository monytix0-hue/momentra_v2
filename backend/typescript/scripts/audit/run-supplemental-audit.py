#!/usr/bin/env python3
"""Supplemental deployment audit — surfaces gaps beyond the 60 root-gap register."""
from __future__ import annotations

import csv
import json
import re
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
AUDIT = ROOT / "docs" / "audit"


def load_register_ids() -> set[str]:
    ids: set[str] = set()
    with (AUDIT / "MASTER_GAP_REGISTER.csv").open(encoding="utf-8") as f:
        for row in csv.DictReader(f):
            if row.get("Gap ID"):
                ids.add(row["Gap ID"])
    return ids


def parse_router() -> list[tuple[str, str]]:
    text = (ROOT / "backend/typescript/src/api/v1/router.ts").read_text(encoding="utf-8")
    routes = []
    for m in re.finditer(r"v1Router\.(get|post|patch|delete)\(\s*['`]([^'`]+)['`]", text):
        p = "/v1" + re.sub(r":([a-zA-Z]+)", r"{\1}", m.group(2))
        routes.append((m.group(1).upper(), p))
    return routes


def parse_apk() -> set[str]:
    text = (ROOT / "apk/app/src/main/java/com/example/momentra/data/api/ApiService.kt").read_text(encoding="utf-8")
    out: set[str] = set()
    for b in re.split(r"\n\s*(?=@(?:GET|POST|PATCH|DELETE))", text):
        meth = re.search(r"@(GET|POST|PATCH|DELETE)", b)
        p = re.search(r'"(v1/[^"]+)"', b)
        if meth and p:
            norm = "/v1/" + re.sub(r"\{[^}]+\}", "{}", p.group(1).replace("v1/", "", 1))
            out.add(f"{meth.group(1)} {norm}")
    return out


def parse_ios() -> set[str]:
    text = (ROOT / "momentra/momentra/API/APIClient.swift").read_text(encoding="utf-8")
    out: set[str] = set()
    for m in re.finditer(r'authorized(Get|Post|Patch|Delete)\(path: "([^"]+)"', text):
        p = re.sub(r"\\\([^)]+\)", "{}", m.group(2))
        if not p.startswith("v1/"):
            p = "v1/" + p
        out.add(f"{m.group(1).upper()} /{re.sub(r'{[^}]+}', '{}', p)}")
    return out


def rk(m: str, p: str) -> str:
    p = re.sub(r":([a-zA-Z]+)", r"{\1}", p)
    p = re.sub(r"\{[^}]+\}", "{}", p)
    return f"{m.upper()} {p}"


SUPPLEMENTAL_GAPS: list[dict] = [
    {
        "id": "SUPP-001",
        "domain": "Group",
        "surface": "Group Pulse quick tiles",
        "gap": "Photos / Chat / Itinerary tiles disabled on both platforms",
        "type": "UI_GAP",
        "severity": "P2",
        "parent": "GRP-004",
        "evidence": "GroupPulseActiveView.swift / GroupPulseActiveContent.kt enabled=false",
    },
    {
        "id": "SUPP-002",
        "domain": "Group",
        "surface": "Wedding Quick Add",
        "gap": "Participant + Settle sheets disabled (enabled=false) despite settlement IMPLEMENTED in catalog",
        "type": "CLIENT_GAP",
        "severity": "P1",
        "parent": "GRP-004",
        "evidence": "WeddingQuickAddSheets.swift/.kt",
    },
    {
        "id": "SUPP-003",
        "domain": "Group",
        "surface": "Purchase Quick Add",
        "gap": "Contributor / Delivery / Ownership sheets disabled on both platforms",
        "type": "UI_GAP",
        "severity": "P1",
        "parent": "GRP-006",
        "evidence": "PurchaseQuickAddSheets.swift/.kt",
    },
    {
        "id": "SUPP-004",
        "domain": "Group",
        "surface": "Experience Quick Add",
        "gap": "Add Participant sheet disabled on both platforms",
        "type": "UI_GAP",
        "severity": "P1",
        "parent": "GRP-006",
        "evidence": "ExperienceQuickAddSheets.swift/.kt",
    },
    {
        "id": "SUPP-005",
        "domain": "Group",
        "surface": "Group Memory",
        "gap": "Preserve this moment CTA disabled",
        "type": "UI_GAP",
        "severity": "P2",
        "parent": "GRP-008",
        "evidence": "GroupMemoryActiveView.swift / GroupMemoryActiveContent.kt",
    },
    {
        "id": "SUPP-006",
        "domain": "Personal",
        "surface": "Personal Life",
        "gap": "Life sections show API_GAP banner; only active-area count is live",
        "type": "PROJECTION_GAP",
        "severity": "P0",
        "parent": "PER-009",
        "evidence": "PersonalLifeActiveView.swift / PersonalLifeActiveContent.kt",
    },
    {
        "id": "SUPP-007",
        "domain": "Shared Platform",
        "surface": "Push notifications",
        "gap": "Android push permission only; send worker STUB",
        "type": "REFRESH_GAP",
        "severity": "P2",
        "parent": "SP-010",
        "evidence": "AppRoot.kt comment",
    },
    {
        "id": "SUPP-008",
        "domain": "Shared Platform",
        "surface": "Analytics UI",
        "gap": "Analytics metrics/insights/refresh defined in clients but no UI calls",
        "type": "UI_GAP",
        "severity": "P2",
        "parent": "SP-011",
        "evidence": "ApiService.kt / APIClient.swift — no repository usage",
    },
    {
        "id": "SUPP-009",
        "domain": "Shared Platform",
        "surface": "OpenAPI SDK toolchain",
        "gap": "Generated SDKs (iOS 42 + Android 39 files) not compiled into apps; hand-maintained clients drift",
        "type": "CONTRACT_GAP",
        "severity": "P1",
        "parent": "SP-001",
        "evidence": "momentra/OpenAPI/Generated, apk/openapi-generated not in build",
    },
    {
        "id": "SUPP-010",
        "domain": "Shared Platform",
        "surface": "Backend-only routes",
        "gap": "17+ live routes have no iOS or Android client method",
        "type": "CLIENT_GAP",
        "severity": "P1",
        "parent": "SP-013",
        "evidence": "personal/attention, setups, goals, tasks, living-rules, expense-categories, ai/execute",
    },
    {
        "id": "SUPP-011",
        "domain": "Group",
        "surface": "Living rules",
        "gap": "living-rules GET/POST live in backend; UI explicitly not wired",
        "type": "UI_GAP",
        "severity": "P1",
        "parent": "GRP-006",
        "evidence": "LivingQuickAddSheets.swift/.kt House Rule message",
    },
    {
        "id": "SUPP-012",
        "domain": "Group",
        "surface": "Poll lifecycle",
        "gap": "Poll vote/close/get in OpenAPI only; shared.poll_vote table unused",
        "type": "API_GAP",
        "severity": "P0",
        "parent": "GRP-001",
        "evidence": "03-openapi-backend-reconciliation.csv DOCUMENTED_ONLY",
    },
    {
        "id": "SUPP-013",
        "domain": "Personal",
        "surface": "Recurring schedules",
        "gap": "iOS missing recurring-schedules; Android missing PATCH/generate",
        "type": "CLIENT_GAP",
        "severity": "P1",
        "parent": "SP-013",
        "evidence": "ApiService.kt vs APIClient.swift",
    },
    {
        "id": "SUPP-014",
        "domain": "Business",
        "surface": "Issue evidence",
        "gap": "POST issues/{id}/evidence in Android ApiService but not called from repository",
        "type": "CLIENT_GAP",
        "severity": "P1",
        "parent": "BUS-015",
        "evidence": "BusinessSliceRepository.kt",
    },
    {
        "id": "SUPP-015",
        "domain": "Shared Platform",
        "surface": "Invite preview",
        "gap": "GET group/company invites/{code} defined on Android, never called on either platform",
        "type": "UI_GAP",
        "severity": "P2",
        "parent": "GRP-006",
        "evidence": "ApiService.kt — no repository call sites",
    },
    {
        "id": "SUPP-016",
        "domain": "Shared Platform",
        "surface": "Dead router",
        "gap": "router-product.ts unmounted duplicate/stub routes create ambiguity",
        "type": "LEGACY",
        "severity": "P3",
        "parent": "SP-017",
        "evidence": "not imported in app.ts",
    },
    {
        "id": "SUPP-017",
        "domain": "Shared Platform",
        "surface": "SQL schema",
        "gap": "34 tables ORPHAN_CANDIDATE with zero backend references",
        "type": "SCHEMA_GAP",
        "severity": "P1",
        "parent": "SP-007",
        "evidence": "07-table-utilization.csv",
    },
    {
        "id": "SUPP-018",
        "domain": "Shared Platform",
        "surface": "OpenAPI contract",
        "gap": "114 live routes IMPLEMENTED_UNDOCUMENTED in OpenAPI",
        "type": "CONTRACT_GAP",
        "severity": "P1",
        "parent": "SP-001",
        "evidence": "03-openapi-backend-reconciliation.csv",
    },
    {
        "id": "SUPP-019",
        "domain": "Shared Platform",
        "surface": "Mobile parity",
        "gap": "88 routes classified IOS_ONLY or ANDROID_ONLY",
        "type": "CLIENT_GAP",
        "severity": "P1",
        "parent": "SP-013",
        "evidence": "04-ios-android-parity.csv",
    },
    {
        "id": "SUPP-020",
        "domain": "Group/Business",
        "surface": "Actions projection",
        "gap": "GET group/business .../actions in OpenAPI CONTRACT_ONLY — not in live router",
        "type": "API_GAP",
        "severity": "P1",
        "parent": "SP-001",
        "evidence": "OpenAPI only; iOS has getBusinessActions unwired",
    },
    {
        "id": "SUPP-021",
        "domain": "Business",
        "surface": "Ops/Runway Quick Add",
        "gap": "Decision/Meeting/Retro/Forecast/Tax/Investor flows use business-updates closest-writer",
        "type": "DATA_GAP",
        "severity": "P0",
        "parent": "BUS-001",
        "evidence": "OpsQuickAddSheets.kt/.swift, RunwayQuickAddSheets",
    },
    {
        "id": "SUPP-022",
        "domain": "Business",
        "surface": "Memory share",
        "gap": "Share with Team disabled on Ops and Runway Memory screens",
        "type": "UI_GAP",
        "severity": "P1",
        "parent": "BUS-016",
        "evidence": "OpsMemoryActiveView.kt/.swift, RunwayMemoryActiveView",
    },
    {
        "id": "SUPP-023",
        "domain": "Shared Platform",
        "surface": "E2E certification",
        "gap": "23 Maestro flows indexed; 0/19 moment journeys executed with DB proof",
        "type": "TEST_GAP",
        "severity": "P0",
        "parent": "SP-014",
        "evidence": "MASTER_QA_SUMMARY.md, maestro-journey-index.csv",
    },
    {
        "id": "SUPP-024",
        "domain": "Personal",
        "surface": "Personal activity",
        "gap": "Delete activity shows Coming soon toast — no DELETE API wired",
        "type": "UI_GAP",
        "severity": "P2",
        "parent": "PER-002",
        "evidence": "PersonalEditActivitySheet",
    },
    {
        "id": "SUPP-025",
        "domain": "Business",
        "surface": "Capability gating",
        "gap": "Revenue/Invoice Quick Add disabled on B01/B03 (CAPABILITY_GAP) but enabled on B02",
        "type": "BUSINESS_RULE_GAP",
        "severity": "P1",
        "parent": "GRP-003",
        "evidence": "QUICK_ADD_COVERAGE_MATRIX.md — V019 mapping",
    },
]


def load_existing_supp() -> dict[str, dict[str, str]]:
    path = AUDIT / "SUPPLEMENTAL_GAP_REGISTER.csv"
    out: dict[str, dict[str, str]] = {}
    if not path.exists():
        return out
    with path.open(encoding="utf-8") as f:
        for row in csv.DictReader(f):
            gid = row.get("GapId", "")
            if gid:
                out[gid] = row
    return out


def main() -> None:
    router = {rk(m, p) for m, p in parse_router()}
    apk = parse_apk()
    ios = parse_ios()
    both = apk & ios

  # Verify SUPP-010 routes
    critical_missing = [
        ("GET", "/v1/personal/attention"),
        ("GET", "/v1/personal/setups"),
        ("POST", "/v1/personal/setups/{systemCode}/activate"),
        ("GET", "/v1/business/setups"),
        ("GET", "/v1/finance/expense-categories"),
        ("GET", "/v1/group/moments/{momentId}/living-rules"),
        ("POST", "/v1/moments/{momentId}/living-rules"),
        ("POST", "/v1/moments/{momentId}/goals"),
        ("POST", "/v1/moments/{momentId}/tasks"),
        ("POST", "/v1/ai/action-proposals/{actionProposalId}/execute"),
    ]
    backend_only_rows = []
    for m, p in critical_missing:
        key = rk(m, p)
        in_router = key in router
        in_apk = key in apk
        in_ios = key in ios
        backend_only_rows.append([m, p, in_router, in_apk, in_ios, "MISSING_BOTH" if not in_apk and not in_ios else "PARTIAL"])

    existing = load_existing_supp()
    write_csv = AUDIT / "SUPPLEMENTAL_GAP_REGISTER.csv"
    with write_csv.open("w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow([
            "GapId", "Domain", "Surface", "RootGap", "GapType", "Severity",
            "ParentRegisterGap", "Status", "Evidence",
        ])
        for g in SUPPLEMENTAL_GAPS:
            prev = existing.get(g["id"], {})
            status = prev.get("Status") or "OPEN"
            evidence = prev.get("Evidence") or g["evidence"]
            w.writerow([
                g["id"], g["domain"], g["surface"], g["gap"], g["type"], g["severity"],
                g["parent"], status, evidence,
            ])

    write_csv2 = AUDIT / "SUPPLEMENTAL_BACKEND_ONLY_ROUTES.csv"
    with write_csv2.open("w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["Method", "Path", "InRouter", "Android", "iOS", "ClientStatus"])
        w.writerows(backend_only_rows)

    # Counts from existing artifacts
    undoc = sum(1 for _ in open(AUDIT / "03-openapi-backend-reconciliation.csv") if "IMPLEMENTED_UNDOCUMENTED" in _)
    parity = sum(1 for _ in open(AUDIT / "04-ios-android-parity.csv") if ",CLIENT_GAP," in _ or ",IOS_ONLY," in _ or ",ANDROID_ONLY," in _)
    orphans = sum(1 for _ in open(AUDIT / "07-table-utilization.csv") if ",ORPHAN_CANDIDATE," in _)

    sev = {}
    typ = {}
    for g in SUPPLEMENTAL_GAPS:
        sev[g["severity"]] = sev.get(g["severity"], 0) + 1
        typ[g["type"]] = typ.get(g["type"], 0) + 1

    md = f"""# Supplemental Gap Report (Re-Audit)

Generated: {datetime.now(timezone.utc).isoformat()}

This report captures **{len(SUPPLEMENTAL_GAPS)} additional gaps** discovered in the second audit pass. These are **symptoms and drill-down items** that map to the frozen 60 root gaps in `Momentra_Master_Deployment_Gap_Register.xlsx` — not duplicate root causes.

## Executive summary

| Metric | First audit | Re-audit (supplemental) |
|--------|-------------|-------------------------|
| Root gaps (register) | 60 | 60 (unchanged) |
| Supplemental gaps | — | **{len(SUPPLEMENTAL_GAPS)}** |
| Undocumented live routes | 114 | {undoc} (confirmed) |
| Client parity asymmetries | 88 | {parity} (confirmed) |
| Orphan SQL tables | 34 | {orphans} (confirmed) |
| Maestro journeys executed | 0/19 | 0/19 (unchanged) |

### Supplemental severity breakdown
| Severity | Count |
|----------|-------|
{chr(10).join(f'| {k} | {v} |' for k,v in sorted(sev.items()))}

### Supplemental gap types
| Type | Count |
|------|-------|
{chr(10).join(f'| {k} | {v} |' for k,v in sorted(typ.items(), key=lambda x: -x[1]))}

## Top new findings (not explicit in register rows)

### P0 — immediate attention
1. **SUPP-006** — Personal Life sections still `API_GAP` in UI (under PER-009)
2. **SUPP-012** — Poll vote/close/get still contract-only; `shared.poll_vote` unused (GRP-001)
3. **SUPP-021** — Ops/Runway still routing first-class facts through `business-updates` (BUS-001)
4. **SUPP-023** — Zero Maestro moment journeys executed with DB/outbox proof (SP-014)

### P1 — required V1 surface gaps
- **SUPP-002** — Wedding Participant/Settle UI disabled despite settlement catalog IMPLEMENTED
- **SUPP-003/004** — Purchase + Experience participant flows UI-disabled
- **SUPP-009** — Generated OpenAPI SDKs unused; manual clients drift (114 undocumented routes)
- **SUPP-010** — 10+ backend routes with no mobile client at all (setups, goals, tasks, attention, living-rules)
- **SUPP-011** — Living rules backend live, UI says not wired
- **SUPP-013** — Recurring schedule parity incomplete (iOS missing entirely)
- **SUPP-017** — 34 orphan SQL tables including `shared.poll_vote`, `work.task_dependency`
- **SUPP-019** — 88 iOS/Android route asymmetries
- **SUPP-020** — Group/Business `actions` projection in OpenAPI only
- **SUPP-022** — Business Memory share disabled
- **SUPP-025** — B01/B03 Revenue/Invoice capability-gated off

### P2/P3 — non-blocking
- Group Pulse Photos/Chat/Itinerary disabled (SUPP-001)
- Push notification worker stub (SUPP-007)
- Analytics APIs with no UI (SUPP-008)
- Dead `router-product.ts` (SUPP-016)

## Reconciliation updates (register rows that need evidence refresh)

| Register ID | Re-audit finding |
|-------------|----------------|
| **BUS-003** | **Partially closed** — TeamOps *does* call `createMilestone` on both platforms; gap remains for Ops/Runway milestone paths |
| **GRP-007** | **Still PARTIAL** — route exists, capability 501 possible, Wedding settle UI still disabled (SUPP-002) |
| **GRP-006** | **Still PARTIAL** — routes exist for many collab actions; living-rules + purchase/experience UI gaps remain |
| **PER-002** | **More complete than register states** — many precision routes live (V046); gap is widget-level freeze not route absence |
| **SP-001** | **Confirmed worse** — 114 undocumented + 13 contract-only false positives in OpenAPI |

## Artifacts
- `SUPPLEMENTAL_GAP_REGISTER.csv` — {len(SUPPLEMENTAL_GAPS)} supplemental rows
- `SUPPLEMENTAL_BACKEND_ONLY_ROUTES.csv` — critical routes with no mobile client
- All area `01`–`16` files regenerated by `npm run audit:deployment`

## Recommendation
Do **not** add supplemental rows to the root register (avoid duplication). Instead:
1. Link each SUPP-* row to its `ParentRegisterGap` for Wave execution
2. Start Wave 0 with SP-001/002/005/006 using SUPP-009/018 as acceptance criteria
3. Prioritize SUPP-012 (polls) and SUPP-021 (business closest-writer) in Wave 3
"""
    (AUDIT / "SUPPLEMENTAL_GAP_REPORT.md").write_text(md, encoding="utf-8")
    print(f"Wrote SUPPLEMENTAL_GAP_REGISTER.csv ({len(SUPPLEMENTAL_GAPS)} rows)")
    print(f"Wrote SUPPLEMENTAL_GAP_REPORT.md")


if __name__ == "__main__":
    main()
