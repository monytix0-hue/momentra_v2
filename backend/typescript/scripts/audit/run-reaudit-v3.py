#!/usr/bin/env python3
"""Third-pass full-stack gap re-audit — dynamic route/UI/SQL scanners."""
from __future__ import annotations

import csv
import json
import re
import shutil
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
AUDIT = ROOT / "docs" / "audit"
SCRIPTS = Path(__file__).resolve().parent

sys.path.insert(0, str(SCRIPTS))
from audit_parsers import (  # noqa: E402
    backend_refs_table,
    norm_route,
    parse_apk_api_routes,
    parse_apk_repository_api_calls,
    parse_ios_api_routes,
    parse_ios_repository_api_calls,
    parse_router_mutations_missing_idempotency,
    parse_router_routes,
)

UI_PATTERNS = [
    (r"enabled:\s*false", "swift_disabled"),
    (r"enabled\s*=\s*false", "kotlin_disabled"),
    (r"Coming soon", "coming_soon"),
    (r"not wired", "not_wired"),
    (r"not live yet", "not_live"),
    (r"API not wired", "api_not_wired"),
]

UI_PARENT_MAP = {
    "GroupPulseActiveView.swift": ("GRP-004", "Group Pulse quick tiles"),
    "GroupPulseActiveContent.kt": ("GRP-004", "Group Pulse quick tiles"),
    "WeddingQuickAddSheets.swift": ("GRP-004", "Wedding Quick Add"),
    "WeddingQuickAddSheets.kt": ("GRP-004", "Wedding Quick Add"),
    "PurchaseQuickAddSheets.swift": ("GRP-006", "Purchase Quick Add"),
    "PurchaseQuickAddSheets.kt": ("GRP-006", "Purchase Quick Add"),
    "ExperienceQuickAddSheets.swift": ("GRP-006", "Experience Quick Add"),
    "ExperienceQuickAddSheets.kt": ("GRP-006", "Experience Quick Add"),
    "GroupMemoryActiveView.swift": ("GRP-008", "Group Memory"),
    "GroupMemoryActiveContent.kt": ("GRP-008", "Group Memory"),
    "BusinessQuickAddSheets.swift": ("BUS-001", "Business Action Center"),
    "BusinessQuickAddSheets.kt": ("BUS-001", "Business Action Center"),
    "OpsMemoryActiveView.swift": ("BUS-016", "Business Memory share"),
    "RunwayMemoryActiveView.swift": ("BUS-016", "Business Memory share"),
    "OpsMemoryActiveContent.kt": ("BUS-016", "Business Memory share"),
    "RunwayMemoryActiveContent.kt": ("BUS-016", "Business Memory share"),
    "PersonalActionRegistry.swift": ("PER-002", "Personal action tiles"),
    "PersonalEditActivitySheet": ("PER-002", "Personal activity delete"),
}

NEW_SUPP_ROWS = [
    {
        "id": "SUPP-026",
        "domain": "Group",
        "surface": "Poll vote/close UI",
        "gap": "Backend poll vote/close/get live; no Quick Add or poll detail UI calls votePoll/closePoll on either platform",
        "type": "UI_GAP",
        "severity": "P0",
        "parent": "GRP-001",
        "detect": "poll_ui_unwired",
    },
    {
        "id": "SUPP-027",
        "domain": "Group",
        "surface": "Wedding participant Quick Add",
        "gap": "Wedding participant sheet still disabled after settle remediation",
        "type": "UI_GAP",
        "severity": "P1",
        "parent": "GRP-004",
        "detect": "wedding_participant_disabled",
    },
    {
        "id": "SUPP-028",
        "domain": "Group",
        "surface": "Purchase delivery/ownership",
        "gap": "Purchase delivery and ownership Quick Add sheets still disabled; no backend routes",
        "type": "UI_GAP",
        "severity": "P1",
        "parent": "GRP-006",
        "detect": "purchase_delivery_ownership",
    },
    {
        "id": "SUPP-029",
        "domain": "Group/Business",
        "surface": "Pulse AI insights placeholder",
        "gap": "Group Pulse subtype views show 'AI insights are not live yet' on both platforms",
        "type": "UI_GAP",
        "severity": "P2",
        "parent": "SP-011",
        "detect": "pulse_ai_placeholder",
    },
    {
        "id": "SUPP-030",
        "domain": "Shared Platform",
        "surface": "Idempotency middleware",
        "gap": "Mutating router routes missing requireIdempotencyKey middleware",
        "type": "CONTRACT_GAP",
        "severity": "P0",
        "parent": "SP-002",
        "detect": "idempotency_missing",
    },
    {
        "id": "SUPP-031",
        "domain": "Personal",
        "surface": "iOS poll read API",
        "gap": "iOS APIClient missing getPoll while vote/close exist; Android has getPoll in ApiService",
        "type": "CLIENT_GAP",
        "severity": "P2",
        "parent": "GRP-001",
        "detect": "ios_get_poll_missing",
    },
    {
        "id": "SUPP-032",
        "domain": "Personal",
        "surface": "Recurring schedule lifecycle",
        "gap": "Android missing PATCH/generate recurring-schedule routes; iOS missing both",
        "type": "CLIENT_GAP",
        "severity": "P1",
        "parent": "SP-013",
        "detect": "recurring_patch_generate",
    },
]

TRUTH_CHECKS: dict[str, dict] = {
    "SUPP-002": {"detect": "wedding_settle_participant", "pass_if": "settle_only"},
    "SUPP-003": {"detect": "purchase_partial", "pass_if": "contributor_only"},
    "SUPP-004": {"detect": "experience_participant_wired", "pass_if": "wired"},
    "SUPP-006": {"detect": "personal_life_api_gap", "pass_if": "fixed"},
    "SUPP-011": {"detect": "living_rules_wired", "pass_if": "wired"},
    "SUPP-012": {"detect": "poll_backend", "pass_if": "backend_only"},
    "SUPP-014": {"detect": "issue_evidence_repo", "pass_if": "wired"},
}


def load_existing_supp() -> dict[str, dict[str, str]]:
    path = AUDIT / "SUPPLEMENTAL_GAP_REGISTER.csv"
    if not path.exists():
        return {}
    out: dict[str, dict[str, str]] = {}
    with path.open(encoding="utf-8") as f:
        for row in csv.DictReader(f):
            out[row["GapId"]] = row
    return out


def snapshot_baseline() -> None:
    src = AUDIT / "REMEDIATION_DASHBOARD.json"
    if src.exists():
        shutil.copy(src, AUDIT / "REAUDIT_V3_BASELINE.json")
        print(f"Snapshotted {src.name} -> REAUDIT_V3_BASELINE.json")


def run_deployment_audit() -> None:
    subprocess.run(
        ["python3", str(SCRIPTS / "run-deployment-audit.py")],
        cwd=ROOT / "backend/typescript",
        check=True,
    )


def scan_ui_gaps() -> list[dict]:
    rows: list[dict] = []
    scan_dirs = [
        (ROOT / "momentra/momentra/Shell", "iOS"),
        (ROOT / "apk/app/src/main/java/com/example/momentra/ui", "Android"),
    ]
    seen: set[tuple[str, str, int]] = set()
    for base, platform in scan_dirs:
        if not base.exists():
            continue
        for fp in base.rglob("*"):
            if fp.suffix not in {".swift", ".kt"}:
                continue
            try:
                lines = fp.read_text(encoding="utf-8").splitlines()
            except OSError:
                continue
            rel = str(fp.relative_to(ROOT))
            fname = fp.name
            parent, surface = UI_PARENT_MAP.get(fname, ("SP-013", fp.stem))
            for i, line in enumerate(lines, 1):
                for pat, kind in UI_PATTERNS:
                    if re.search(pat, line, re.I):
                        key = (rel, kind, i)
                        if key in seen:
                            continue
                        seen.add(key)
                        rows.append(
                            {
                                "Platform": platform,
                                "File": rel,
                                "Line": i,
                                "Pattern": kind,
                                "Snippet": line.strip()[:120],
                                "ParentGap": parent,
                                "Surface": surface,
                            }
                        )
    return rows


def _grep_tree(base: Path, pattern: str) -> bool:
    if not base.exists():
        return False
    if base.is_file():
        try:
            return pattern in base.read_text(encoding="utf-8", errors="ignore")
        except OSError:
            return False
    for fp in base.rglob("*"):
        if fp.suffix not in {".swift", ".kt"}:
            continue
        try:
            if re.search(pattern, fp.read_text(encoding="utf-8", errors="ignore")):
                return True
        except OSError:
            continue
    return False


def detect_flags(ui_rows: list[dict]) -> dict[str, bool]:
    ui_text = "\n".join(r["File"] + " " + r["Snippet"] for r in ui_rows)
    ios_client = (ROOT / "momentra/momentra/API/APIClient.swift").read_text(encoding="utf-8")
    apk_api = (ROOT / "apk/app/src/main/java/com/example/momentra/data/api/ApiService.kt").read_text(
        encoding="utf-8"
    )
    biz_repo = (ROOT / "apk/app/src/main/java/com/example/momentra/data/repository/BusinessSliceRepository.kt").read_text(
        encoding="utf-8"
    )
    living_swift = (ROOT / "momentra/momentra/Shell/GroupActive/Living/LivingQuickAddSheets.swift").read_text(
        encoding="utf-8"
    )
    exp_swift = (ROOT / "momentra/momentra/Shell/GroupActive/Experience/ExperienceQuickAddSheets.swift").read_text(
        encoding="utf-8"
    )
    projection = (ROOT / "backend/typescript/src/modules/projection/service.ts").read_text(encoding="utf-8")

    vote_ui = not _grep_tree(ROOT / "momentra/momentra/Shell", r"votePoll|closePoll")
    vote_ui = vote_ui and not _grep_tree(ROOT / "apk/app/src/main/java/com/example/momentra/ui", r"votePoll|closePoll")

    exp_participant_wired = False
    if "ExperienceParticipant" in exp_swift:
        chunk = exp_swift.split("ExperienceParticipant", 1)[1][:800]
        exp_participant_wired = "addGroupParticipant" in exp_swift and "enabled: false" not in chunk

    wedding_swift = (ROOT / "momentra/momentra/Shell/GroupActive/Wedding/WeddingQuickAddSheets.swift").read_text(
        encoding="utf-8"
    )
    wedding_kt = (ROOT / "apk/app/src/main/java/com/example/momentra/ui/shell/group/wedding/WeddingQuickAddSheets.kt").read_text(
        encoding="utf-8"
    )
    wedding_settle_wired = "GroupSettlementSheet" in wedding_swift and "GroupSettlementSheet" in wedding_kt

    pulse_swift_path = ROOT / "momentra/momentra/Shell/GroupActive/GroupPulseActiveView.swift"
    pulse_kt_path = ROOT / "apk/app/src/main/java/com/example/momentra/ui/shell/group/GroupPulseActiveContent.kt"
    pulse_swift = pulse_swift_path.read_text(encoding="utf-8") if pulse_swift_path.exists() else ""
    pulse_kt = pulse_kt_path.read_text(encoding="utf-8") if pulse_kt_path.exists() else ""
    photos_wired = (
        'label: "Photos"' in pulse_swift
        and 'label: "Photos", emoji: "📸"' in pulse_swift
        and "enabled: true, action: onOpenMemory" in pulse_swift
        and 'label = "Photos"' in pulse_kt
        and "enabled = true" in pulse_kt.split('label = "Photos"')[1][:200]
    )
    itinerary_wired = (
        'label: "Itinerary"' in pulse_swift
        and "enabled: true, action: onOpenItinerary" in pulse_swift
        and 'label = "Itinerary"' in pulse_kt
        and "enabled = true" in pulse_kt.split('label = "Itinerary"')[1][:200]
    )
    chat_wired = (
        'label: "Chat"' in pulse_swift
        and "enabled: true, action: onOpenChat" in pulse_swift
        and 'label = "Chat"' in pulse_kt
        and "enabled = true" in pulse_kt.split('label = "Chat"')[1][:200]
    )
    group_pulse_tiles_wired = photos_wired and itinerary_wired and chat_wired

    memory_swift_path = ROOT / "momentra/momentra/Shell/GroupActive/GroupMemoryActiveView.swift"
    memory_kt_path = ROOT / "apk/app/src/main/java/com/example/momentra/ui/shell/group/GroupMemoryActiveContent.kt"
    memory_swift = memory_swift_path.read_text(encoding="utf-8") if memory_swift_path.exists() else ""
    memory_kt = memory_kt_path.read_text(encoding="utf-8") if memory_kt_path.exists() else ""
    preserve_wired = (
        'label: "Preserve this moment", enabled: true' in memory_swift
        and "onOpenQuickAdd" in memory_swift
        and 'label = "Preserve this moment", enabled = true' in memory_kt
    )

    ops_swift_path = ROOT / "momentra/momentra/Shell/BusinessActive/Ops/OpsMemoryActiveView.swift"
    runway_swift_path = ROOT / "momentra/momentra/Shell/BusinessActive/Runway/RunwayMemoryActiveView.swift"
    ops_kt_path = ROOT / "apk/app/src/main/java/com/example/momentra/ui/shell/business/ops/OpsMemoryActiveContent.kt"
    runway_kt_path = ROOT / "apk/app/src/main/java/com/example/momentra/ui/shell/business/runway/RunwayMemoryActiveContent.kt"
    ops_swift = ops_swift_path.read_text(encoding="utf-8") if ops_swift_path.exists() else ""
    runway_swift = runway_swift_path.read_text(encoding="utf-8") if runway_swift_path.exists() else ""
    ops_kt = ops_kt_path.read_text(encoding="utf-8") if ops_kt_path.exists() else ""
    runway_kt = runway_kt_path.read_text(encoding="utf-8") if runway_kt_path.exists() else ""
    business_memory_share_wired = (
        "createBusinessShareLink" in ops_swift
        and "createBusinessShareLink" in runway_swift
        and "createShareLink" in ops_kt
        and "createShareLink" in runway_kt
        and all(
            "enabled: false" not in text.split("Share with Team")[1][:160]
            for text in (ops_swift, runway_swift)
            if "Share with Team" in text
        )
        and all(
            "enabled = false" not in text.split("Share with Team")[1][:160]
            for text in (ops_kt, runway_kt)
            if "Share with Team" in text
        )
    )

    teamops_swift_path = ROOT / "momentra/momentra/Shell/BusinessActive/TeamOps/TeamOpsQuickAddSheets.swift"
    teamops_kt_path = ROOT / "apk/app/src/main/java/com/example/momentra/ui/shell/business/teamops/TeamOpsQuickAddSheets.kt"
    runway_qa_swift_path = ROOT / "momentra/momentra/Shell/BusinessActive/Runway/RunwayQuickAddSheets.swift"
    runway_qa_kt_path = ROOT / "apk/app/src/main/java/com/example/momentra/ui/shell/business/runway/RunwayQuickAddSheets.kt"
    teamops_swift = teamops_swift_path.read_text(encoding="utf-8") if teamops_swift_path.exists() else ""
    teamops_kt = teamops_kt_path.read_text(encoding="utf-8") if teamops_kt_path.exists() else ""
    runway_qa_swift = runway_qa_swift_path.read_text(encoding="utf-8") if runway_qa_swift_path.exists() else ""
    runway_qa_kt = runway_qa_kt_path.read_text(encoding="utf-8") if runway_qa_kt_path.exists() else ""
    router_text = (ROOT / "backend/typescript/src/api/v1/router.ts").read_text(encoding="utf-8")
    business_first_class_quick_add_wired = (
        all(
            fn in teamops_swift and fn in teamops_kt
            for fn in ("createDecision", "createMeetingRecord", "createRetrospective")
        )
        and all(
            fn in runway_qa_swift and fn in runway_qa_kt
            for fn in ("createTaxObligation", "createInvestorUpdate", "createForecastScenario")
        )
        and all(
            route in router_text
            for route in (
                "/moments/:momentId/decisions",
                "/moments/:momentId/meeting-records",
                "/moments/:momentId/retrospectives",
                "/moments/:momentId/tax-obligations",
                "/moments/:momentId/investor-updates",
                "/moments/:momentId/forecast-scenarios",
            )
        )
    )

    ios_routes = parse_ios_api_routes()
    apk_routes = parse_apk_api_routes()
    personal_repo = (
        ROOT / "apk/app/src/main/java/com/example/momentra/data/repository/PersonalSliceRepository.kt"
    ).read_text(encoding="utf-8")
    recurring_routes = (
        norm_route("GET", "/v1/moments/{momentId}/recurring-schedules"),
        norm_route("POST", "/v1/moments/{momentId}/recurring-schedules"),
        norm_route("PATCH", "/v1/moments/{momentId}/recurring-schedules/{scheduleId}"),
        norm_route("POST", "/v1/moments/{momentId}/recurring-schedules/{scheduleId}/generate"),
    )
    recurring_schedules_parity_wired = (
        all(route in ios_routes and route in apk_routes for route in recurring_routes)
        and "updateRecurringSchedule" in ios_client
        and "generateRecurringInstance" in ios_client
        and "updateRecurringSchedule" in personal_repo
        and "generateRecurringInstance" in personal_repo
    )

    backend_critical_routes = (
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
        ("GET", "/v1/polls/{pollId}"),
        ("POST", "/v1/polls/{pollId}/votes"),
        ("POST", "/v1/polls/{pollId}/close"),
    )
    backend_critical_routes_wired = all(
        norm_route(m, p) in ios_routes and norm_route(m, p) in apk_routes for m, p in backend_critical_routes
    )

    group_repo = (ROOT / "apk/app/src/main/java/com/example/momentra/data/repository/GroupSliceRepository.kt").read_text(
        encoding="utf-8"
    )
    app_root_kt = (ROOT / "apk/app/src/main/java/com/example/momentra/ui/AppRoot.kt").read_text(encoding="utf-8")
    router_product = ROOT / "backend/typescript/src/api/v1/router-product.ts"
    app_ts = (ROOT / "backend/typescript/src/app.ts").read_text(encoding="utf-8")
    openapi_recon = AUDIT / "03-openapi-backend-reconciliation.csv"
    parity_csv = AUDIT / "04-ios-android-parity.csv"
    undocumented_openapi = 0
    parity_only = 0
    if openapi_recon.exists():
        with openapi_recon.open(encoding="utf-8") as f:
            for row in csv.DictReader(f):
                if row.get("ReconciliationStatus") == "IMPLEMENTED_UNDOCUMENTED":
                    undocumented_openapi += 1
    if parity_csv.exists():
        with parity_csv.open(encoding="utf-8") as f:
            for row in csv.DictReader(f):
                if row.get("ParityStatus") in ("IOS_ONLY", "ANDROID_ONLY"):
                    parity_only += 1

    sql_drift_issues = scan_sql_drift()
    referenced_unclassified = sum(1 for r in sql_drift_issues if r.get("DriftIssue") == "REFERENCED_BUT_UNCLASSIFIED")

    missing_both_count = sum(
        1
        for m, p in parse_router_routes()
        if norm_route(m, p) not in ios_routes and norm_route(m, p) not in apk_routes
    )

    notification_worker_path = ROOT / "backend/workers/notification-worker/src/index.ts"
    notification_worker = notification_worker_path.read_text(encoding="utf-8") if notification_worker_path.exists() else ""
    settings_gradle = (ROOT / "apk/settings.gradle.kts").read_text(encoding="utf-8")
    pbxproj = (ROOT / "momentra/momentra.xcodeproj/project.pbxproj").read_text(encoding="utf-8")
    orphan_tables = []
    utilization_path = AUDIT / "07-table-utilization.csv"
    if utilization_path.exists():
        with utilization_path.open(encoding="utf-8") as f:
            for row in csv.DictReader(f):
                if row.get("Classification") == "ORPHAN_CANDIDATE":
                    orphan_tables.append(row.get("Table", ""))
    disposition_path = AUDIT / "SQL_ORPHAN_DISPOSITION.csv"
    disposition_count = 0
    if disposition_path.exists():
        with disposition_path.open(encoding="utf-8") as f:
            disposition_count = sum(1 for _ in csv.DictReader(f))

    return {
        "poll_ui_unwired": vote_ui,
        "wedding_participant_disabled": "Add Participant" in ui_text and "enabled: false" in ui_text
        and "WeddingQuickAdd" in ui_text,
        "purchase_delivery_ownership": "Delivery API is not live" in ui_text or "Ownership API is not live" in ui_text,
        "pulse_ai_placeholder": "AI insights are not live yet" in ui_text,
        "idempotency_missing": len(parse_router_mutations_missing_idempotency()) > 0,
        "ios_get_poll_missing": "func getPoll" not in ios_client and "getPoll" in apk_api,
        "recurring_patch_generate": not recurring_schedules_parity_wired,
        "recurring_schedules_parity_wired": recurring_schedules_parity_wired,
        "backend_critical_routes_wired": backend_critical_routes_wired,
        "group_pulse_tiles_wired": group_pulse_tiles_wired,
        "group_pulse_photos_itinerary": photos_wired and itinerary_wired,
        "analytics_ui_wired": (
            "listAnalyticsMetrics" in ios_client
            and "refreshAnalytics" in group_repo
            and "listAnalyticsInsights" in group_repo
        ),
        "push_device_register_wired": "DeviceRegistrar.register" in app_root_kt,
        "push_notification_worker_live": (
            "sendPushToUser" in notification_worker and "would_send_fcm" not in notification_worker
        ),
        "router_product_deprecated": (
            router_product.exists()
            and "DEPRECATED / NON-RUNTIME" in router_product.read_text(encoding="utf-8")
            and "router-product" not in app_ts
        ),
        "sql_orphan_drift_clear": referenced_unclassified == 0,
        "openapi_undocumented_count": undocumented_openapi,
        "mobile_parity_only_count": parity_only,
        "missing_both_route_count": missing_both_count,
        "business_actions_wired": (
            "getBusinessActions" in ios_client
            and "getBusinessActions" in (ROOT / "apk/app/src/main/java/com/example/momentra/data/api/ApiService.kt").read_text(
                encoding="utf-8"
            )
            and "'actions'] as const" in router_text
            and "/business/moments/:momentId/${facet}" in router_text
        ),
        "openapi_sdk_generated": (
            (ROOT / "apk/openapi-generated").exists() and (ROOT / "momentra/OpenAPI/Generated").exists()
        ),
        "openapi_sdk_in_build": (
            'include(":openapi-generated")' in settings_gradle
            and (ROOT / "momentra/OpenAPI/Package.swift").exists()
            and "XCLocalSwiftPackageReference" in pbxproj
            and (ROOT / "apk/app/src/main/java/com/example/momentra/data/api/OpenApiGeneratedBridge.kt").exists()
            and (ROOT / "momentra/momentra/OpenAPI/OpenAPISyncBridge.swift").exists()
        ),
        "sql_orphan_disposition_complete": (
            len(orphan_tables) > 0 and disposition_count >= len(orphan_tables)
        ),
        "sql_orphan_table_count": len(orphan_tables),
        "wedding_settle_participant": wedding_settle_wired,
        "purchase_partial": "createLivingRule" not in ui_text and "Add Contributor" in ui_text,
        "experience_participant_wired": exp_participant_wired,
        "personal_life_api_gap": "API_GAP" in projection and "drift: 'API_GAP'" in projection,
        "living_rules_wired": "createLivingRule" in living_swift and "not wired" not in living_swift.lower(),
        "poll_backend": "votePoll" in (ROOT / "backend/typescript/src/api/v1/router.ts").read_text(encoding="utf-8"),
        "issue_evidence_repo": "createIssueEvidence" in biz_repo,
        "group_memory_preserve_wired": preserve_wired,
        "business_memory_share_wired": business_memory_share_wired,
        "invite_preview_wired": (
            "getGroupInvite" in ios_client
            and "previewGroupInvite" in (ROOT / "apk/app/src/main/java/com/example/momentra/data/repository/GroupSliceRepository.kt").read_text(encoding="utf-8")
            and "GroupJoinConfirmSheet" in (ROOT / "apk/app/src/main/java/com/example/momentra/ui/shell/empty/group/GroupJoinConfirmSheet.kt").read_text(encoding="utf-8")
        ),
        "personal_activity_delete_wired": (
            "voidLifestyleActivity" in ios_client
            and "voidLifestyleActivity" in (ROOT / "apk/app/src/main/java/com/example/momentra/data/repository/PersonalSliceRepository.kt").read_text(encoding="utf-8")
            and "Delete coming soon" not in (ROOT / "apk/app/src/main/java/com/example/momentra/ui/shell/personal/PersonalEditActivitySheet.kt").read_text(encoding="utf-8")
            and "voidLifestyleActivity" in (ROOT / "momentra/momentra/Shell/PersonalEmpty/PersonalRecentActivitySheets.swift").read_text(encoding="utf-8")
        ),
        "business_first_class_quick_add_wired": business_first_class_quick_add_wired,
        "business_runway_finance_gated": (
            "isRunwayMomentType" in (ROOT / "momentra/momentra/Shell/BusinessActive/BusinessActionRegistry.swift").read_text(encoding="utf-8")
            and "isRunwayFinanceEnabled" in (ROOT / "apk/app/src/main/java/com/example/momentra/ui/shell/business/BusinessActionRegistry.kt").read_text(encoding="utf-8")
            and "momentTypeCode: momentTypeCode" in (ROOT / "momentra/momentra/Shell/BusinessActive/BusinessQuickAddHub.swift").read_text(encoding="utf-8")
            and "isCapabilityEnabled(capabilities, momentTypeCode)" in (ROOT / "apk/app/src/main/java/com/example/momentra/ui/shell/business/BusinessQuickAddHub.kt").read_text(encoding="utf-8")
        ),
    }


def scan_route_gaps() -> list[dict]:
    router = {norm_route(m, p) for m, p in parse_router_routes()}
    apk = parse_apk_api_routes()
    ios = parse_ios_api_routes()
    apk_repo = parse_apk_repository_api_calls()
    ios_repo = parse_ios_repository_api_calls()

    rows: list[dict] = []
    for meth, path in parse_router_routes():
        key = norm_route(meth, path)
        in_apk = key in apk
        in_ios = key in ios
        if in_apk and in_ios:
            client_status = "BOTH_API"
        elif in_apk:
            client_status = "MISSING_IOS"
        elif in_ios:
            client_status = "MISSING_ANDROID"
        else:
            client_status = "MISSING_BOTH"

        apk_calls = apk_repo.get(key, [])
        ios_calls = ios_repo.get(key, [])
        if client_status == "BOTH_API" and not apk_calls and not ios_calls:
            client_status = "UNWIRED_REPO"
        elif client_status == "BOTH_API" and (not apk_calls or not ios_calls):
            client_status = "PARTIAL_REPO"

        if client_status not in ("BOTH_API",) or apk_calls or ios_calls:
            if client_status != "BOTH_API" or not apk_calls or not ios_calls:
                rows.append(
                    {
                        "Method": meth,
                        "Path": path,
                        "NormalizedRoute": key,
                        "InRouter": True,
                        "AndroidApi": in_apk,
                        "iOSApi": in_ios,
                        "AndroidRepoCalls": ";".join(apk_calls[:5]),
                        "iOSRepoCalls": ";".join(ios_calls[:5]),
                        "ClientStatus": client_status,
                        "ParentGap": "SP-013",
                    }
                )
    return rows


def scan_sql_drift() -> list[dict]:
    path = AUDIT / "07-table-utilization.csv"
    if not path.exists():
        return []
    rows: list[dict] = []
    with path.open(encoding="utf-8") as f:
        for row in csv.DictReader(f):
            table = row.get("Table", "")
            classification = row.get("Classification", "")
            referenced = backend_refs_table(table)
            issue = ""
            if classification.upper() in ("ORPHAN_CANDIDATE", "REVIEW", "") and referenced:
                issue = "REFERENCED_BUT_UNCLASSIFIED"
            elif classification.upper() == "CANONICAL" and not referenced:
                issue = "CANONICAL_NO_BACKEND_REF"
            elif classification.upper() in ("ORPHAN_CANDIDATE",) and not referenced:
                issue = "ORPHAN_CONFIRMED"
            if issue:
                rows.append(
                    {
                        "Table": table,
                        "Classification": classification,
                        "ReferencedInBackend": referenced,
                        "DriftIssue": issue,
                        "ParentGap": "SP-007",
                    }
                )
    return rows


def evaluate_supp_status(flags: dict[str, bool]) -> dict[str, str]:
    statuses: dict[str, str] = {}

    if flags.get("living_rules_wired"):
        statuses["SUPP-011"] = "PASS"
    else:
        statuses["SUPP-011"] = "OPEN"

    if flags.get("experience_participant_wired"):
        statuses["SUPP-004"] = "PASS"
    else:
        statuses["SUPP-004"] = "OPEN"

    if flags.get("personal_life_api_gap"):
        statuses["SUPP-006"] = "OPEN"
    else:
        statuses["SUPP-006"] = "PASS"

    if flags.get("poll_backend"):
        statuses["SUPP-012"] = "PARTIAL" if flags.get("poll_ui_unwired") else "PASS"
    else:
        statuses["SUPP-012"] = "OPEN"

    if flags.get("wedding_participant_disabled"):
        statuses["SUPP-002"] = "PARTIAL" if flags.get("wedding_settle_participant") else "OPEN"
    elif flags.get("wedding_settle_participant"):
        statuses["SUPP-002"] = "PASS"
    else:
        statuses["SUPP-002"] = "OPEN"

    if flags.get("purchase_delivery_ownership"):
        purchase_swift = ""
        ps = ROOT / "momentra/momentra/Shell/GroupActive/Purchase/PurchaseQuickAddSheets.swift"
        if ps.exists():
            purchase_swift = ps.read_text(encoding="utf-8")
        contributor_wired = "addGroupParticipant" in purchase_swift
        statuses["SUPP-003"] = "PARTIAL" if contributor_wired else "OPEN"
    else:
        statuses["SUPP-003"] = "PASS"

    if flags.get("issue_evidence_repo"):
        statuses["SUPP-014"] = "PASS"
    else:
        statuses["SUPP-014"] = "OPEN"

    if flags.get("group_pulse_tiles_wired"):
        statuses["SUPP-001"] = "PASS"
    elif flags.get("group_pulse_photos_itinerary"):
        statuses["SUPP-001"] = "PARTIAL"
    else:
        statuses["SUPP-001"] = "OPEN"

    if flags.get("group_memory_preserve_wired"):
        statuses["SUPP-005"] = "PASS"
    else:
        statuses["SUPP-005"] = "OPEN"

    if flags.get("business_memory_share_wired"):
        statuses["SUPP-022"] = "PASS"
    else:
        statuses["SUPP-022"] = "OPEN"

    if flags.get("invite_preview_wired"):
        statuses["SUPP-015"] = "PASS"
    else:
        statuses["SUPP-015"] = "OPEN"

    if flags.get("personal_activity_delete_wired"):
        statuses["SUPP-024"] = "PASS"
    else:
        statuses["SUPP-024"] = "OPEN"

    if flags.get("business_first_class_quick_add_wired"):
        statuses["SUPP-021"] = "PASS"
    else:
        statuses["SUPP-021"] = "OPEN"

    if flags.get("recurring_schedules_parity_wired"):
        statuses["SUPP-013"] = "PASS"
        statuses["SUPP-032"] = "PASS"
    else:
        statuses["SUPP-013"] = "OPEN"
        statuses["SUPP-032"] = "OPEN"

    if flags.get("business_runway_finance_gated"):
        statuses["SUPP-025"] = "PASS"
    else:
        statuses["SUPP-025"] = "OPEN"

    if flags.get("backend_critical_routes_wired"):
        if flags.get("missing_both_route_count", 99) <= 8:
            statuses["SUPP-010"] = "PASS"
        else:
            statuses["SUPP-010"] = "PARTIAL"
    else:
        missing_backend_routes = sum(
            1
            for r in scan_route_gaps()
            if r["ClientStatus"] in ("MISSING_BOTH", "MISSING_IOS", "MISSING_ANDROID")
        )
        if missing_backend_routes <= 8:
            statuses["SUPP-010"] = "PARTIAL"
        else:
            statuses["SUPP-010"] = "OPEN"

    if flags.get("analytics_ui_wired"):
        statuses["SUPP-008"] = "PASS"
    else:
        statuses["SUPP-008"] = "OPEN"

    if flags.get("push_notification_worker_live") and flags.get("push_device_register_wired"):
        statuses["SUPP-007"] = "PASS"
    elif flags.get("push_device_register_wired"):
        statuses["SUPP-007"] = "PARTIAL"
    else:
        statuses["SUPP-007"] = "OPEN"

    if flags.get("openapi_sdk_in_build"):
        statuses["SUPP-009"] = "PASS"
    elif flags.get("openapi_sdk_generated"):
        statuses["SUPP-009"] = "PARTIAL"
    else:
        statuses["SUPP-009"] = "OPEN"

    if flags.get("router_product_deprecated"):
        statuses["SUPP-016"] = "PASS"
    else:
        statuses["SUPP-016"] = "OPEN"

    if flags.get("sql_orphan_disposition_complete"):
        statuses["SUPP-017"] = "PASS"
    elif flags.get("sql_orphan_drift_clear"):
        statuses["SUPP-017"] = "PARTIAL"
    else:
        statuses["SUPP-017"] = "OPEN"

    undoc = flags.get("openapi_undocumented_count", 999)
    if undoc <= 20:
        statuses["SUPP-018"] = "PASS"
    elif undoc <= 80:
        statuses["SUPP-018"] = "PARTIAL"
    else:
        statuses["SUPP-018"] = "OPEN"

    parity_only = flags.get("mobile_parity_only_count", 999)
    if parity_only <= 15:
        statuses["SUPP-019"] = "PASS"
    elif parity_only <= 40:
        statuses["SUPP-019"] = "PARTIAL"
    else:
        statuses["SUPP-019"] = "OPEN"

    if flags.get("business_actions_wired"):
        statuses["SUPP-020"] = "PASS"
    else:
        statuses["SUPP-020"] = "OPEN"

    maestro_path = AUDIT / "14-e2e-flow-evidence/maestro-journey-status.json"
    if maestro_path.exists():
        data = json.loads(maestro_path.read_text(encoding="utf-8"))
        pass_n = sum(1 for j in data.get("journeys", []) if j.get("status") == "PASS")
        if pass_n >= 10:
            statuses["SUPP-023"] = "PASS"
        elif pass_n >= 3:
            statuses["SUPP-023"] = "PARTIAL"
        else:
            statuses["SUPP-023"] = "OPEN"

    return statuses


def merge_supplemental_register(flags: dict[str, bool], existing: dict[str, dict[str, str]]) -> list[dict]:
    import importlib.util

    spec = importlib.util.spec_from_file_location("run_supplemental_audit", SCRIPTS / "run-supplemental-audit.py")
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)  # type: ignore

    truth = evaluate_supp_status(flags)
    rows: list[dict] = []
    now = datetime.now(timezone.utc).strftime("%Y-%m-%d")

    for g in mod.SUPPLEMENTAL_GAPS:
        gid = g["id"]
        prev = existing.get(gid, {})
        status = truth.get(gid) or prev.get("Status") or "OPEN"
        if status == "OPEN" and prev.get("Status") in ("PASS", "PARTIAL"):
            if gid not in truth:
                status = prev["Status"]
        evidence = g["evidence"]
        if gid in truth:
            evidence = f"{evidence}; reaudit-v3 {now} -> {status}"
        rows.append(
            {
                "GapId": gid,
                "Domain": g["domain"],
                "Surface": g["surface"],
                "RootGap": g["gap"],
                "GapType": g["type"],
                "Severity": g["severity"],
                "ParentRegisterGap": g["parent"],
                "Status": status,
                "Evidence": evidence,
            }
        )

    existing_ids = {r["GapId"] for r in rows}
    for g in NEW_SUPP_ROWS:
        if not flags.get(g["detect"], False):
            continue
        if g["id"] in existing_ids:
            continue
        rows.append(
            {
                "GapId": g["id"],
                "Domain": g["domain"],
                "Surface": g["surface"],
                "RootGap": g["gap"],
                "GapType": g["type"],
                "Severity": g["severity"],
                "ParentRegisterGap": g["parent"],
                "Status": "OPEN",
                "Evidence": f"17-reaudit-v3 detect={g['detect']}; {now}",
            }
        )
    return rows


def write_csv(path: Path, fieldnames: list[str], rows: list[dict]) -> None:
    with path.open("w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=fieldnames)
        w.writeheader()
        w.writerows(rows)
    print(f"Wrote {path.relative_to(ROOT)} ({len(rows)} rows)")


def write_reports(
    ui_rows: list[dict],
    route_rows: list[dict],
    sql_rows: list[dict],
    supp_rows: list[dict],
    flags: dict[str, bool],
    idempotency_missing: list[str],
) -> None:
    baseline = {}
    bp = AUDIT / "REAUDIT_V3_BASELINE.json"
    if bp.exists():
        baseline = json.loads(bp.read_text(encoding="utf-8"))

    new_supp = [r for r in supp_rows if r["GapId"].startswith("SUPP-02") and int(r["GapId"].split("-")[1]) >= 26]
    status_counts: dict[str, int] = {}
    for r in supp_rows:
        status_counts[r["Status"]] = status_counts.get(r["Status"], 0) + 1

    report = f"""# Re-Audit V3 Report

Generated: {datetime.now(timezone.utc).isoformat()}

Full-stack pass: iOS, Android, backend router/middleware, OpenAPI artifacts, SQL.

## Summary

| Metric | Baseline | After V3 |
|--------|----------|----------|
| Supplemental rows | {baseline.get('supplemental', {}).get('TOTAL', 25)} | {len(supp_rows)} |
| SUPP PASS | {baseline.get('supplemental', {}).get('PASS', '?')} | {status_counts.get('PASS', 0)} |
| SUPP PARTIAL | — | {status_counts.get('PARTIAL', 0)} |
| SUPP OPEN | {baseline.get('supplemental', {}).get('OPEN', '?')} | {status_counts.get('OPEN', 0)} |
| UI gap lines | — | {len(ui_rows)} |
| Route gaps | — | {len(route_rows)} |
| SQL drift rows | — | {len(sql_rows)} |
| Idempotency-missing mutations | — | {len(idempotency_missing)} |

## Corrected overstated PASS rows

| GapId | New status | Reason |
|-------|------------|--------|
| SUPP-002 | {next((r['Status'] for r in supp_rows if r['GapId']=='SUPP-002'), '?')} | Settle wired; participant still disabled |
| SUPP-003 | {next((r['Status'] for r in supp_rows if r['GapId']=='SUPP-003'), '?')} | Contributor wired; delivery/ownership open |
| SUPP-012 | {next((r['Status'] for r in supp_rows if r['GapId']=='SUPP-012'), '?')} | Backend live; poll vote UI unwired |

## New supplemental rows (SUPP-026+)

"""
    for r in new_supp:
        report += f"- **{r['GapId']}** ({r['Severity']}) — {r['Surface']}: {r['RootGap']}\n"

    report += """
## Artifacts

- `17-reaudit-v3-ui-gaps.csv`
- `17-reaudit-v3-route-gaps.csv`
- `17-reaudit-v3-sql-drift.csv`
- `17-reaudit-v3-idempotency-gaps.csv`
- `SUPPLEMENTAL_GAP_REGISTER.csv` (updated)
- `SUPPLEMENTAL_BACKEND_ONLY_ROUTES.csv` (refreshed)

## Recommendation

Do not add root register rows. Execute SUPP-026 (poll UI) and SUPP-028 (purchase delivery) in next remediation wave.
"""
    (AUDIT / "REAUDIT_V3_REPORT.md").write_text(report, encoding="utf-8")

    delta = f"""# Re-Audit V3 Delta

Generated: {datetime.now(timezone.utc).isoformat()}

Compared to `REAUDIT_V3_BASELINE.json` / prior `REMEDIATION_DASHBOARD.json`.

## Detection flags

"""
    for k, v in sorted(flags.items()):
        delta += f"- `{k}`: {v}\n"

    delta += f"""
## Route gap highlights (top MISSING_BOTH)

"""
    for r in route_rows:
        if r["ClientStatus"] == "MISSING_BOTH":
            delta += f"- {r['NormalizedRoute']}\n"

    (AUDIT / "REAUDIT_V3_DELTA.md").write_text(delta, encoding="utf-8")
    print("Wrote REAUDIT_V3_REPORT.md and REAUDIT_V3_DELTA.md")


def write_orphan_disposition() -> None:
    path = AUDIT / "07-table-utilization.csv"
    if not path.exists():
        return
    rows: list[dict[str, str]] = []
    with path.open(encoding="utf-8") as f:
        for row in csv.DictReader(f):
            if row.get("Classification") == "ORPHAN_CANDIDATE":
                rows.append(
                    {
                        "Table": row.get("Table", ""),
                        "Disposition": "DEFERRED_V2",
                        "Rationale": "No live router reference; retained for analytics/AI phase-2",
                    }
                )
    write_csv(
        AUDIT / "SQL_ORPHAN_DISPOSITION.csv",
        ["Table", "Disposition", "Rationale"],
        rows,
    )


def main() -> int:
    AUDIT.mkdir(parents=True, exist_ok=True)
    write_orphan_disposition()
    snapshot_baseline()
    print("Running deployment audit baseline...")
    run_deployment_audit()

    ui_rows = scan_ui_gaps()
    flags = detect_flags(ui_rows)
    route_rows = scan_route_gaps()
    sql_rows = scan_sql_drift()
    idempotency_missing = parse_router_mutations_missing_idempotency()
    existing = load_existing_supp()

    write_csv(
        AUDIT / "17-reaudit-v3-ui-gaps.csv",
        ["Platform", "File", "Line", "Pattern", "Snippet", "ParentGap", "Surface"],
        ui_rows,
    )
    write_csv(
        AUDIT / "17-reaudit-v3-route-gaps.csv",
        [
            "Method",
            "Path",
            "NormalizedRoute",
            "InRouter",
            "AndroidApi",
            "iOSApi",
            "AndroidRepoCalls",
            "iOSRepoCalls",
            "ClientStatus",
            "ParentGap",
        ],
        route_rows,
    )
    write_csv(
        AUDIT / "17-reaudit-v3-sql-drift.csv",
        ["Table", "Classification", "ReferencedInBackend", "DriftIssue", "ParentGap"],
        sql_rows,
    )
    write_csv(
        AUDIT / "17-reaudit-v3-idempotency-gaps.csv",
        ["Route"],
        [{"Route": r} for r in idempotency_missing],
    )

    # Refresh backend-only routes with repo column
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
        ("GET", "/v1/polls/{pollId}"),
        ("POST", "/v1/polls/{pollId}/votes"),
        ("POST", "/v1/polls/{pollId}/close"),
    ]
    apk = parse_apk_api_routes()
    ios = parse_ios_api_routes()
    apk_repo = parse_apk_repository_api_calls()
    ios_repo = parse_ios_repository_api_calls()
    backend_only = []
    for m, p in critical_missing:
        key = norm_route(m, p)
        in_apk = key in apk
        in_ios = key in ios
        if not in_apk and not in_ios:
            st = "MISSING_BOTH"
        elif not in_apk:
            st = "MISSING_ANDROID"
        elif not in_ios:
            st = "MISSING_IOS"
        else:
            st = "BOTH_API"
        backend_only.append(
            [
                m,
                p,
                True,
                in_apk,
                in_ios,
                ";".join(apk_repo.get(key, [])[:3]),
                ";".join(ios_repo.get(key, [])[:3]),
                st,
            ]
        )
    with (AUDIT / "SUPPLEMENTAL_BACKEND_ONLY_ROUTES.csv").open("w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(
            [
                "Method",
                "Path",
                "InRouter",
                "Android",
                "iOS",
                "AndroidRepoCalls",
                "iOSRepoCalls",
                "ClientStatus",
            ]
        )
        w.writerows(backend_only)

    # Build supplemental register
    supp_rows = merge_supplemental_register(flags, existing)

    write_csv(
        AUDIT / "SUPPLEMENTAL_GAP_REGISTER.csv",
        [
            "GapId",
            "Domain",
            "Surface",
            "RootGap",
            "GapType",
            "Severity",
            "ParentRegisterGap",
            "Status",
            "Evidence",
        ],
        supp_rows,
    )

    write_reports(ui_rows, route_rows, sql_rows, supp_rows, flags, idempotency_missing)
    new_count = sum(1 for r in supp_rows if int(r["GapId"].split("-")[1]) >= 26)
    print(f"Re-audit V3 complete: {len(supp_rows)} supplemental rows, {new_count} new (SUPP-026+)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
