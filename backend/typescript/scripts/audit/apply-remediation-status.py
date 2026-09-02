#!/usr/bin/env python3
"""Apply remediation DevStatus columns to MASTER_GAP_REGISTER_RECONCILED.csv."""
from __future__ import annotations

import csv
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
REGISTER = ROOT / "docs" / "audit" / "MASTER_GAP_REGISTER_RECONCILED.csv"
SUPP = ROOT / "docs" / "audit" / "SUPPLEMENTAL_GAP_REGISTER.csv"

# Wave closures from remediation execution (evidence under docs/audit/evidence/{GapId}/)
STATUS_OVERRIDES: dict[str, dict[str, str]] = {
    "GRP-001": {"DevStatus": "CLOSED", "EvidenceRef": "tests/group-gx2-collab.test.ts", "ClosedAt": "2026-09-01"},
    "PER-009": {"DevStatus": "CLOSED", "EvidenceRef": "tests/personal-three-layer-join.test.ts", "ClosedAt": "2026-09-01"},
    "BUS-003": {"DevStatus": "IN DEVELOPMENT", "EvidenceRef": "TeamOpsQuickAddSheets — milestone wired; Ops general update uses business-update by design"},
    "GRP-006": {"DevStatus": "IN DEVELOPMENT", "EvidenceRef": "living-rules + participant Quick Adds wired both platforms"},
    "GRP-007": {"DevStatus": "IN DEVELOPMENT", "EvidenceRef": "Wedding settle wired Android+iOS"},
    "PER-002": {"DevStatus": "IN DEVELOPMENT", "EvidenceRef": "V046 precision routes; widget freeze partial"},
    "SP-001": {"DevStatus": "IN DEVELOPMENT", "EvidenceRef": "Poll routes documented IMPLEMENTED in build-openapi.ts"},
}

SUPP_PASS = {"SUPP-006", "SUPP-011", "SUPP-012", "SUPP-002", "SUPP-003", "SUPP-004"}


def main() -> None:
    now = datetime.now(timezone.utc).strftime("%Y-%m-%d")
    rows: list[dict[str, str]] = []
    with REGISTER.open(encoding="utf-8") as f:
        reader = csv.DictReader(f)
        fieldnames = list(reader.fieldnames or [])
        for col in ("DevStatus", "EvidenceRef", "ClosedAt"):
            if col not in fieldnames:
                fieldnames.append(col)
        for row in reader:
            gap = row["GapId"]
            override = STATUS_OVERRIDES.get(gap, {})
            row["DevStatus"] = override.get("DevStatus", row.get("RegisterStatus", "OPEN"))
            if row["DevStatus"] == "PARTIAL":
                row["DevStatus"] = "IN DEVELOPMENT"
            row["EvidenceRef"] = override.get("EvidenceRef", row.get("EvidenceLocation", "docs/audit/"))
            row["ClosedAt"] = override.get("ClosedAt", "")
            rows.append(row)

    with REGISTER.open("w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=fieldnames)
        w.writeheader()
        w.writerows(rows)
    print(f"Updated {REGISTER} ({len(rows)} rows)")

    supp_rows: list[dict[str, str]] = []
    with SUPP.open(encoding="utf-8") as f:
        reader = csv.DictReader(f)
        supp_fields = list(reader.fieldnames or [])
        for row in reader:
            if row["GapId"] in SUPP_PASS:
                row["Status"] = "PASS"
                row["Evidence"] = (row.get("Evidence") or "") + f"; remediated {now}"
            supp_rows.append(row)
    with SUPP.open("w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=supp_fields)
        w.writeheader()
        w.writerows(supp_rows)
    print(f"Updated {SUPP} ({len(supp_rows)} rows, {len(SUPP_PASS)} PASS)")


if __name__ == "__main__":
    main()
