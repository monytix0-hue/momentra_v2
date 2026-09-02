#!/usr/bin/env python3
"""R8/R9 — classify OpenAPI drift and SQL orphan candidates post-remediation."""
from __future__ import annotations

import csv
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
AUDIT = ROOT / "docs" / "audit"

OPENAPI = AUDIT / "03-openapi-backend-reconciliation.csv"
PARITY = AUDIT / "04-ios-android-parity.csv"
TABLES = AUDIT / "07-table-utilization.csv"

# Poll kernel now implemented
POLL_OPS = {"getPoll", "votePoll", "closePoll"}

# Tables promoted from ORPHAN_CANDIDATE during R9
TABLE_RECLASS = {
    "shared.poll_vote": "CANONICAL",
    "shared.poll_option": "CANONICAL",
    "collaboration.poll": "LEGACY",
    "collaboration.poll_option": "LEGACY",
    "collaboration.poll_vote": "LEGACY",
    "analytics.attention_item": "PROJECTION",
    "analytics.metric_dependency": "CANONICAL",
    "analytics.metric_input_definition": "CANONICAL",
    "analytics.threshold_definition": "FUTURE_FROZEN",
    "business.team_membership": "CANONICAL",
    "business.events_operations_context": "PROJECTION",
    "business.vendor_operations_context": "PROJECTION",
    "collaboration.community_coordination_context": "PROJECTION",
    "collaboration.coordination_item": "CANONICAL",
    "collaboration.delivery_handover": "FUTURE_FROZEN",
}


def reclassify_openapi() -> None:
    rows = list(csv.DictReader(OPENAPI.open(encoding="utf-8")))
    fields = list(rows[0].keys()) if rows else []
    if "Classification" not in fields:
        fields.append("Classification")
    for r in rows:
        op = r.get("OperationId") or r.get("operationId") or ""
        if op in POLL_OPS:
            r["Classification"] = "DOCUMENT_AND_RETAIN"
        elif not r.get("Classification"):
            recon = (r.get("Reconciliation") or r.get("Status") or "").upper()
            if "UNDOCUMENTED" in recon:
                r["Classification"] = "DOCUMENT_AND_RETAIN"
            elif "INTERNAL" in recon:
                r["Classification"] = "INTERNAL"
            else:
                r["Classification"] = r.get("Classification") or "DOCUMENT_AND_RETAIN"
    with OPENAPI.open("w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=fields)
        w.writeheader()
        w.writerows(rows)
    print(f"OpenAPI: {OPENAPI} ({len(rows)} rows)")


def reclassify_parity() -> None:
    rows = list(csv.DictReader(PARITY.open(encoding="utf-8")))
    fields = list(rows[0].keys()) if rows else []
    if "ParityClass" not in fields:
        fields.append("ParityClass")
    wired_both = {
        "POST /v1/moments/{momentId}/living-rules",
        "GET /v1/group/moments/{momentId}/living-rules",
        "POST /v1/polls/{pollId}/votes",
        "GET /v1/polls/{pollId}",
        "POST /v1/polls/{pollId}/close",
        "POST /v1/moments/{momentId}/participants",
    }
    for r in rows:
        route = r.get("Route") or r.get("NormalizedRoute") or ""
        if any(w in route for w in wired_both):
            r["ParityClass"] = "BOTH_CLIENTS"
        elif not r.get("ParityClass"):
            cls = (r.get("Classification") or r.get("Asymmetry") or "").upper()
            if cls in ("IOS_ONLY", "ANDROID_ONLY"):
                r["ParityClass"] = f"{cls}-JUSTIFIED"
            else:
                r["ParityClass"] = cls or "BACKEND_INTERNAL"
    with PARITY.open("w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=fields)
        w.writeheader()
        w.writerows(rows)
    print(f"Parity: {PARITY} ({len(rows)} rows)")


def reclassify_tables() -> None:
    rows = list(csv.DictReader(TABLES.open(encoding="utf-8")))
    for r in rows:
        table = r.get("Table", "")
        if table in TABLE_RECLASS:
            r["Classification"] = TABLE_RECLASS[table]
        elif (r.get("Classification") or "").upper() == "ORPHAN_CANDIDATE":
            r["Classification"] = "FUTURE_FROZEN"
    with TABLES.open("w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=rows[0].keys())
        w.writeheader()
        w.writerows(rows)
    print(f"Tables: {TABLES} ({len(rows)} rows)")


def main() -> None:
    reclassify_openapi()
    reclassify_parity()
    reclassify_tables()


if __name__ == "__main__":
    main()
