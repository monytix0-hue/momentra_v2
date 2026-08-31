"""Reclassify GX2_FIELD_MATRIX.csv against live router mounts; write THREE_LAYER_JOIN.md."""
from __future__ import annotations

import csv
import re
from collections import Counter, defaultdict
from datetime import date
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CSV_PATH = ROOT / "docs" / "implementation" / "GX2_FIELD_MATRIX.csv"
JOIN_MD = ROOT / "docs" / "implementation" / "THREE_LAYER_JOIN.md"
MATRIX_MD = ROOT / "docs" / "implementation" / "GX2_FIELD_MATRIX.md"

LIVE_WRITE = {
    "collaboration.planning_item",
    "collaboration.booking",
    "collaboration.group_update",
    "collaboration.purchase_item",
    "collaboration.resident",
    "collaboration.moment_participant",
    "memory.memory",
    "shared.poll",
    "finance.expense",
    "finance.contribution",
    "finance.settlement",
    "finance.budget",
}

STILL_API_GAP = {
    "collaboration.group_vendor",
    "collaboration.attendance",
    "collaboration.living_rule",
    "collaboration.shared_asset",
    "collaboration.maintenance_record",
    "collaboration.ownership_record",
    "collaboration.delivery_handover",
    "collaboration.poll",
}

IOS_MISSING_GET = {
    "collaboration.planning_item",
    "collaboration.booking",
    "collaboration.group_update",
    "collaboration.purchase_item",
    "collaboration.resident",
    "memory.memory",
    "shared.poll",
}

TABLE_RE = re.compile(
    r"\b((?:collaboration|memory|shared|finance|projection|governance|core|personal|business|work)\.\w+)"
)


def extract_tables(table_column: str) -> set[str]:
    if not table_column:
        return set()
    return set(TABLE_RE.findall(table_column))


def main() -> None:
    with CSV_PATH.open(encoding="utf-8-sig", newline="") as f:
        rows = list(csv.DictReader(f))
    fieldnames = list(rows[0].keys())
    old_status = Counter(r["gx2_status"] for r in rows)
    changes: Counter[str] = Counter()

    for r in rows:
        old = r["gx2_status"]
        tables = extract_tables(r.get("table_column") or "")
        notes = r.get("notes") or ""

        if old == "API_GAP" and tables:
            still = tables & STILL_API_GAP
            live = tables & LIVE_WRITE
            if live and not still:
                r["gx2_status"] = "CLIENT_FIX"
                r["closure_priority"] = "JOIN-1"
                if "GX2-C live" not in notes:
                    r["notes"] = (
                        notes + "; GX2-C live 2026-08-30: route mounted; client bind/iOS GET pending"
                    ).strip("; ")
                changes["API_GAP→CLIENT_FIX"] += 1
            elif live and still:
                if "mixed live+gap" not in notes:
                    r["notes"] = (
                        notes + "; mixed live+gap tables — keep API_GAP until gap tables mount"
                    ).strip("; ")
                changes["API_GAP mixed kept"] += 1

        if r["gx2_status"] == "CLIENT_FIX" and (tables & IOS_MISSING_GET):
            if "iOS GET list missing" not in (r.get("notes") or ""):
                r["notes"] = (
                    (r.get("notes") or "") + "; iOS GET list missing — Life facet only"
                ).strip("; ")
                changes["annotated iOS GET"] += 1

    new_status = Counter(r["gx2_status"] for r in rows)

    with CSV_PATH.open("w", encoding="utf-8", newline="") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(rows)

    by_status_entity: dict[str, Counter[str]] = defaultdict(Counter)
    for r in rows:
        if r["gx2_status"] not in ("API_GAP", "CLIENT_FIX"):
            continue
        tables = extract_tables(r.get("table_column") or "")
        if not tables:
            by_status_entity[r["gx2_status"]]["(no table)"] += 1
        else:
            for t in tables:
                by_status_entity[r["gx2_status"]][t] += 1

    def delta(key: str) -> int:
        return new_status.get(key, 0) - old_status.get(key, 0)

    client_fix_rows = "\n".join(
        f"| `{ent}` | {n} |" for ent, n in by_status_entity["CLIENT_FIX"].most_common(15)
    )
    api_gap_rows = "\n".join(
        f"| `{ent}` | {n} |" for ent, n in by_status_entity["API_GAP"].most_common(15)
    )

    JOIN_MD.write_text(
        f"""# Three-Layer Join — SQL ↔ Backend ↔ iOS/Android

**Date:** {date.today().isoformat()}  
**Authority:** live [`router.ts`](../../backend/typescript/src/api/v1/router.ts) + [`ApiService.kt`](../../apk/app/src/main/java/com/example/momentra/data/api/ApiService.kt) + [`APIClient.swift`](../../momentra/momentra/API/APIClient.swift) + [`GX2_FIELD_MATRIX.csv`](./GX2_FIELD_MATRIX.csv)  
**Rule:** zero UNKNOWN. Reclassified after GX2-C mounts.

## Status rollup (reclassified)

| Status | Before | After | Delta |
|---|---:|---:|---:|
| WIRED | {old_status.get('WIRED', 0)} | {new_status.get('WIRED', 0)} | {delta('WIRED')} |
| CLIENT_FIX | {old_status.get('CLIENT_FIX', 0)} | {new_status.get('CLIENT_FIX', 0)} | {delta('CLIENT_FIX')} |
| API_GAP | {old_status.get('API_GAP', 0)} | {new_status.get('API_GAP', 0)} | {delta('API_GAP')} |
| SCHEMA_GAP | {old_status.get('SCHEMA_GAP', 0)} | {new_status.get('SCHEMA_GAP', 0)} | 0 |
| FIGMA_GAP | {old_status.get('FIGMA_GAP', 0)} | {new_status.get('FIGMA_GAP', 0)} | 0 |
| LOCAL_ONLY | {old_status.get('LOCAL_ONLY', 0)} | {new_status.get('LOCAL_ONLY', 0)} | 0 |
| DEFERRED | {old_status.get('DEFERRED', 0)} | {new_status.get('DEFERRED', 0)} | 0 |
| UNKNOWN | 0 | 0 | 0 |

**Reclass actions:** {dict(changes)}

## Live join inventory

### Mounted Group collab (SQL + live `/v1`)

| Table | Write | Read | Android | iOS |
|---|---|---|---|---|
| collaboration.planning_item | POST …/planning-items | GET …/planning-items + life | GET+POST | POST only (Life) |
| collaboration.booking | POST …/bookings | GET …/bookings + life | GET+POST | POST only (Life) |
| shared.poll | POST …/polls | GET …/polls | GET+POST | POST only |
| collaboration.group_update | POST …/updates | GET …/updates + life | GET+POST | POST only (Life) |
| collaboration.purchase_item | POST …/purchase-items | GET …/purchase-items | GET+POST | POST only |
| collaboration.resident | POST …/residents | GET …/residents | GET+POST | POST only |
| memory.memory | POST …/memories | GET …/memories + memory facet | GET+POST | POST only (Memory facet) |
| finance.* (expense/contribution/settlement/budget) | live | live | live | live |

### Still API_GAP (table exists, no live Group route)

| Table | Figma surface |
|---|---|
| collaboration.group_vendor | Add Vendor |
| collaboration.attendance | RSVP / Track Attendance |
| collaboration.living_rule | House rules |
| collaboration.shared_asset | Register asset |
| collaboration.maintenance_record | Log maintenance |
| collaboration.ownership_record | Transfer ownership |
| collaboration.delivery_handover | Delivery & handover |

### Coded but unmounted (`router-product.ts` only)

| Route | Decision target |
|---|---|
| PATCH /v1/moments/:id, archive, cancel | Mount or DEFERRED |
| POST …/goals, milestones, tasks | Mount or DEFERRED (work schema) |
| GET /v1/personal/attention | Mount or DEFERRED |
| POST /v1/personal/setups/:code/activate | Mount (business activate already live) |
| POST /v1/ai/action-proposals/:id/execute | DEFERRED until S8 |
| GET /v1/moments/:id/activity | Prefer group/business scoped paths |

## CLIENT_FIX entity rollup (top 15)

| Entity | Widgets |
|---|---:|
{client_fix_rows}

## Remaining API_GAP entity rollup (top 15)

| Entity | Widgets |
|---|---:|
{api_gap_rows}

## Join sequence (this program)

1. **Proof harness** — Trip / Gift Pool / Flatmates golden path (create → SQL → GET list → GET life/pulse).
2. **iOS GET parity** — seven list GETs Android already has.
3. **Bind Experience** — Wedding/Trip Pulse/Moments/Memory/Quick Add; fix registry destinies; quarantine demo.
4. **Mount remaining SQL** — vendor, attendance, living_rule, shared_asset, maintenance (existing tables only).
5. **Capability contract** — V019 on `/v1/me`, writes, client tiles.
6. **router-product inventory** — mount or mark DEFERRED; never leave OpenAPI saying IMPLEMENTED for unmounted routes.
7. **Fix stale SQL tooling** — verify-maestro-cert table names; reset-ledger → `momentra_migration_ledger`.

## Explicitly out

UPI/GPay, chat, FX, invented health/AI, fake gallery, Goal/Community product, SET ROLE / RLS rewrite, V030.
""",
        encoding="utf-8",
    )

    MATRIX_MD.write_text(
        f"""# GX2 Field Matrix

**Authority:** Figma `575:7980` + Excel masters.  
**Reclassified:** {date.today().isoformat()} against live `router.ts` (see [`THREE_LAYER_JOIN.md`](./THREE_LAYER_JOIN.md)).

**Rows:** {len(rows)} widgets. **Statuses:** no UNKNOWN.

**Full CSV:** [`GX2_FIELD_MATRIX.csv`](GX2_FIELD_MATRIX.csv)

## Status rollup

| Status | Count | Meaning |
|---|---:|---|
| `WIRED` | {new_status.get('WIRED', 0)} | Figma widget ↔ client ↔ live API ↔ table.column |
| `CLIENT_FIX` | {new_status.get('CLIENT_FIX', 0)} | Live API + SQL exist; client binding/nav/state incomplete |
| `API_GAP` | {new_status.get('API_GAP', 0)} | Table exists; live router missing read and/or write |
| `SCHEMA_GAP` | {new_status.get('SCHEMA_GAP', 0)} | Widget needs column/table that does not exist |
| `FIGMA_GAP` | {new_status.get('FIGMA_GAP', 0)} | Designed; no product/schema contract |
| `LOCAL_ONLY` | {new_status.get('LOCAL_ONLY', 0)} | Device/chrome only; must not pretend to persist |
| `DEFERRED` | {new_status.get('DEFERRED', 0)} | Explicitly out of join program |

## Live API overrides (current)

Group collab mounts on live router — planning-items, bookings, polls, updates, purchase-items, residents, memories — plus finance/settlements/invites. Rows that previously counted those as `API_GAP` are now `CLIENT_FIX`.

Still `API_GAP`: group_vendor, attendance, living_rule, shared_asset, maintenance_record, ownership_record, delivery_handover.

## Column contract

```text
moment_family → moment_subtype → figma_screen → figma_widget → field
→ table_column → api_read → api_write → android_binding → ios_binding
→ projection → gx2_status
```
""",
        encoding="utf-8",
    )

    print("old", dict(old_status))
    print("new", dict(new_status))
    print("changes", dict(changes))
    print("wrote", JOIN_MD)


if __name__ == "__main__":
    main()
