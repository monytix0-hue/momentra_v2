#!/usr/bin/env python3
"""Generate frds/by-product catalog from CREATE TABLE statements."""
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

CATALOG: dict[str, dict[str, list[str]]] = {
    "personal": {
        "pulse": [
            "projection.personal_pulse",
            "projection.attention_summary",
        ],
        "moments": [
            "core.moment",
            "personal.personal_moment_context",
            "projection.personal_moments",
            "projection.moment_summary",
        ],
        "life": [
            "personal.life_operation_observation",
            "personal.future_opportunity",
            "personal.future_pivot",
            "personal.future_learning_activity",
            "personal.future_progress_observation",
            "personal.lifestyle_activity",
            "personal.relationship_connection",
            "personal.relationship_activity",
            "projection.personal_life",
        ],
        "memory": [
            "memory.memory",
            "memory.memory_evidence",
            "memory.pattern",
            "memory.pattern_occurrence",
            "memory.learning",
            "memory.learning_evidence",
            "memory.playbook",
            "memory.playbook_version",
            "memory.playbook_evidence",
            "projection.personal_memory",
        ],
        "finance": [
            "finance.expense",
            "finance.personal_expense_context",
            "finance.financial_account",
            "finance.financial_movement",
            "finance.budget",
            "finance.budget_revision",
            "projection.personal_finance_snapshot",
        ],
    },
    "group": {
        "pulse": ["projection.group_pulse", "projection.group_finance_position"],
        "moments": [
            "collaboration.group_moment_context",
            "collaboration.moment_participant",
            "projection.group_moments",
        ],
        "life": [
            "collaboration.shared_experience_context",
            "collaboration.planning_item",
            "collaboration.booking",
            "collaboration.shared_purchase_context",
            "collaboration.shared_living_context",
            "collaboration.shared_goal_context",
            "collaboration.community_coordination_context",
            "projection.group_life",
        ],
        "memory": ["projection.group_memory"],
        "finance": [
            "finance.group_expense_context",
            "finance.expense_share",
            "finance.contribution",
            "finance.participant_obligation",
            "finance.settlement",
            "finance.settlement_allocation",
            "projection.group_finance_snapshot",
        ],
    },
    "business": {
        "pulse": ["projection.business_pulse"],
        "moments": [
            "business.business_moment_context",
            "business.company",
            "business.company_membership",
            "projection.business_moments",
        ],
        "life": [
            "business.team",
            "business.vendor",
            "business.issue",
            "business.risk",
            "business.decision",
            "projection.business_life",
        ],
        "memory": ["projection.business_memory"],
        "finance": [
            "finance.business_expense_context",
            "finance.revenue",
            "finance.invoice",
            "finance.invoice_line",
            "finance.invoice_payment",
            "projection.business_finance_snapshot",
        ],
    },
    "shared": {
        "work": [
            "work.goal",
            "work.milestone",
            "work.task",
            "work.assignment",
            "work.task_dependency",
        ],
        "governance": [
            "governance.permission",
            "governance.role",
            "governance.role_permission",
            "governance.role_assignment",
            "governance.consent_purpose",
            "governance.data_category",
            "governance.consent",
            "governance.policy",
            "governance.policy_version",
            "governance.approval_request",
            "governance.approval_step",
            "governance.approval_decision",
        ],
        "platform": [
            "events.domain_event",
            "events.outbox_event",
            "events.event_consumer_state",
            "events.event_delivery_attempt",
            "events.dead_letter_event",
            "audit.audit_record",
            "platform.idempotency_record",
            "platform.distributed_lock",
            "platform.job_execution",
            "platform.processing_checkpoint",
            "security.*",
        ],
        "ai": [
            "ai.context_session",
            "ai.context_item",
            "ai.inference_run",
            "ai.ai_insight",
            "ai.recommendation",
            "ai.action_proposal",
            "ai.action_proposal_parameter",
            "ai.provenance",
        ],
        "shell": [
            "projection.life360",
            "projection.available_action",
            "projection.pending_approval_summary",
            "projection.recent_activity",
            "projection.projection_state",
            "projection.user_company_access",
            "core.user_profile",
            "core.moment_category",
            "core.moment_type",
            "core.capability",
            "core.moment_type_capability",
            "analytics.metric_definition",
            "analytics.attention_item",
        ],
    },
}

MIGRATION_HINTS = {
    "core.": "V002",
    "personal.": "V003",
    "collaboration.": "V004",
    "business.": "V005",
    "work.": "V006",
    "finance.": "V007",
    "governance.": "V008",
    "analytics.": "V009",
    "memory.": "V010",
    "events.": "V011",
    "audit.": "V012",
    "platform.": "V012",
    "ai.": "V013",
    "projection.": "V014",
    "security.": "V024",
}


def migration_hint(table: str) -> str:
    for prefix, mig in MIGRATION_HINTS.items():
        if table.startswith(prefix) or table == prefix.rstrip("."):
            return mig
    return "V001-V030"


def main() -> None:
    out_dir = ROOT / "by-product"
    out_dir.mkdir(exist_ok=True)
    lines = [
        "# Momentra product-grouped SQL catalog",
        "",
        "Browse map for Supabase Table Editor, Node modules, and Prisma layout.",
        "Apply order remains V001–V030 from `manifest/MIGRATION_ORDER.txt`.",
        "",
    ]
    for domain, sections in CATALOG.items():
        lines.append(f"## {domain}")
        lines.append("")
        for section, tables in sections.items():
            lines.append(f"### {section}")
            lines.append("")
            for t in tables:
                lines.append(f"- `{t}` (source: {migration_hint(t)})")
            lines.append("")
    (out_dir / "README.md").write_text("\n".join(lines), encoding="utf-8")
    print(f"Wrote {out_dir / 'README.md'}")


if __name__ == "__main__":
    main()
