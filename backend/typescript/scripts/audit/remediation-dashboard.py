#!/usr/bin/env python3
"""Remediation progress dashboard — playbook §9 metrics from audit artifacts."""
from __future__ import annotations

import csv
import json
import sys
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
AUDIT = ROOT / "docs" / "audit"
REGISTER = AUDIT / "MASTER_GAP_REGISTER_RECONCILED.csv"
SUPP = AUDIT / "SUPPLEMENTAL_GAP_REGISTER.csv"
OPENAPI_RECON = AUDIT / "03-openapi-backend-reconciliation.csv"
PARITY = AUDIT / "04-ios-android-parity.csv"
TABLES = AUDIT / "07-table-utilization.csv"
MAESTRO = AUDIT / "14-e2e-flow-evidence" / "maestro-journey-status.json"
OUT_JSON = AUDIT / "REMEDIATION_DASHBOARD.json"
OUT_MD = AUDIT / "REMEDIATION_DASHBOARD.md"

DEV_STATUSES = ("OPEN", "IN DEVELOPMENT", "IMPLEMENTED", "VALIDATING", "CLOSED", "DEFERRED")


def read_csv(path: Path) -> list[dict[str, str]]:
    if not path.exists():
        return []
    with path.open(encoding="utf-8") as f:
        return list(csv.DictReader(f))


def count_register(rows: list[dict[str, str]]) -> dict[str, int]:
    counts = {s: 0 for s in DEV_STATUSES}
    for r in rows:
        status = (r.get("DevStatus") or r.get("RegisterStatus") or "OPEN").strip().upper()
        if status == "PARTIAL":
            status = "IN DEVELOPMENT"
        if status not in counts:
            status = "OPEN"
        counts[status] += 1
    return counts


def count_supplemental(rows: list[dict[str, str]]) -> dict[str, int]:
    pass_n = sum(1 for r in rows if r.get("Status", "").upper() in ("PASS", "CLOSED"))
    defer_n = sum(1 for r in rows if r.get("Status", "").upper() == "DEFERRED")
    open_n = len(rows) - pass_n - defer_n
    return {"PASS": pass_n, "OPEN": open_n, "DEFERRED": defer_n, "TOTAL": len(rows)}


def count_openapi_drift(rows: list[dict[str, str]]) -> int:
    unexplained = 0
    for r in rows:
        classification = (r.get("Classification") or r.get("Reconciliation") or "").upper()
        if classification in ("", "IMPLEMENTED_UNDOCUMENTED", "UNEXPLAINED", "OPEN"):
            unexplained += 1
    return unexplained


def count_parity_gaps(rows: list[dict[str, str]]) -> int:
    unexplained = 0
    for r in rows:
        classification = (r.get("Classification") or r.get("ParityClass") or "").upper()
        if classification in ("", "IOS_ONLY", "ANDROID_ONLY", "UNEXPLAINED"):
            unexplained += 1
    return unexplained


def count_unclassified_tables(rows: list[dict[str, str]]) -> int:
    return sum(
        1
        for r in rows
        if (r.get("Classification") or "").upper() in ("ORPHAN_CANDIDATE", "REVIEW", "")
    )


def count_maestro(rows: list[dict]) -> dict[str, int]:
    if not rows:
        return {"PASS": 0, "TOTAL": 19, "FAIL": 0, "PENDING": 19}
    pass_n = sum(1 for r in rows if r.get("status") == "PASS")
    total = len(rows) or 19
    return {"PASS": pass_n, "TOTAL": total, "FAIL": sum(1 for r in rows if r.get("status") == "FAIL"), "PENDING": total - pass_n}


def count_severity(rows: list[dict[str, str]]) -> dict[str, int]:
    p0 = p1 = 0
    for r in rows:
        if (r.get("DevStatus") or r.get("RegisterStatus") or "").upper() in ("CLOSED", "DEFERRED"):
            continue
        sev = (r.get("Severity") or "").upper()
        if sev == "P0":
            p0 += 1
        elif sev == "P1":
            p1 += 1
    return {"P0": p0, "P1": p1}


def main() -> int:
    register = read_csv(REGISTER)
    supplemental = read_csv(SUPP)
    openapi = read_csv(OPENAPI_RECON)
    parity = read_csv(PARITY)
    tables = read_csv(TABLES)

    maestro_rows: list[dict] = []
    if MAESTRO.exists():
        maestro_rows = json.loads(MAESTRO.read_text(encoding="utf-8")).get("journeys", [])

    reg_counts = count_register(register)
    supp_counts = count_supplemental(supplemental)
    maestro_counts = count_maestro(maestro_rows)
    sev = count_severity(register)

    dashboard = {
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        "rootGaps": {
            "total": len(register),
            "closed": reg_counts.get("CLOSED", 0),
            "validating": reg_counts.get("VALIDATING", 0),
            "implemented": reg_counts.get("IMPLEMENTED", 0),
            "inDevelopment": reg_counts.get("IN DEVELOPMENT", 0),
            "open": reg_counts.get("OPEN", 0),
            "deferred": reg_counts.get("DEFERRED", 0),
        },
        "supplemental": supp_counts,
        "maestro": maestro_counts,
        "openapiDriftUnexplained": count_openapi_drift(openapi),
        "parityGapsUnexplained": count_parity_gaps(parity),
        "sqlUnclassified": count_unclassified_tables(tables),
        "p0Remaining": sev["P0"],
        "p1Remaining": sev["P1"],
    }

    OUT_JSON.write_text(json.dumps(dashboard, indent=2) + "\n", encoding="utf-8")

    md = f"""# Remediation Dashboard

Generated: {dashboard["generatedAt"]}

```
Root gaps:     CLOSED {dashboard["rootGaps"]["closed"]}/{dashboard["rootGaps"]["total"]} | VALIDATING {dashboard["rootGaps"]["validating"]} | IMPLEMENTED {dashboard["rootGaps"]["implemented"]} | OPEN {dashboard["rootGaps"]["open"]} | DEFERRED {dashboard["rootGaps"]["deferred"]}
Supplemental:  PASS {dashboard["supplemental"]["PASS"]}/{dashboard["supplemental"]["TOTAL"]}
Maestro:       PASS {dashboard["maestro"]["PASS"]}/{dashboard["maestro"]["TOTAL"]}
OpenAPI drift: {dashboard["openapiDriftUnexplained"]} unexplained
Parity gaps:   {dashboard["parityGapsUnexplained"]} unexplained
SQL unclassified: {dashboard["sqlUnclassified"]}
P0 remaining:  {dashboard["p0Remaining"]} | P1 remaining: {dashboard["p1Remaining"]}
```
"""
    OUT_MD.write_text(md, encoding="utf-8")
    print(md)
    return 0


if __name__ == "__main__":
    sys.exit(main())
