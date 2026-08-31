#!/usr/bin/env python3
"""Assemble checksummed V001-V030 migration pack from phase folders."""
from __future__ import annotations

import hashlib
import json
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

SOURCES = {
    "V001__extensions.sql": "Phase1/V001__extensions.sql",
    "V002__core.sql": "Phase1/V002__core.sql",
    "V003__personal.sql": "Phase1/V003__personal.sql",
    "V004__collaboration.sql": "Phase1/V004__collaboration.sql",
    "V005__business.sql": "Phase1/V005__business.sql",
    "V006__work.sql": "phase2/V006__work.sql",
    "V007__finance.sql": "phase2/V007__finance.sql",
    "V008__governance.sql": "phase2/V008__governance.sql",
    "V009__analytics.sql": "phase2/V009__analytics.sql",
    "V010__memory.sql": "phase2/V010__memory.sql",
    "V011__events.sql": "phase3/V011__events.sql",
    "V012__audit_platform.sql": "phase3/V012__audit_platform.sql",
    "V013__ai.sql": "phase3/V013__ai.sql",
    "V014__projection.sql": "phase3/V014__projection.sql",
    "V015__integration_constraints.sql": "phase3/V015__integration_constraints.sql",
    "V016__technical_functions_triggers.sql": "phase3/V016__technical_functions_triggers.sql",
    "V017__indexes_finalize.sql": "phase3/V017__indexes_finalize.sql",
    "V018__taxonomy_seed.sql": "phase4/V018__taxonomy_seed.sql",
    "V019__capability_seed.sql": "phase4/V019__capability_seed.sql",
    "V020__governance_seed.sql": "phase4/V020__governance_seed.sql",
    "V021__consent_catalogue_seed.sql": "phase4/V021__consent_catalogue_seed.sql",
    "V022__analytics_metric_seed.sql": "phase4/V022__analytics_metric_seed.sql",
    "V023__policy_seed.sql": "phase4/V023__policy_seed.sql",
    "V024__rls_enable.sql": "phase5/V024__rls_enable.sql",
    "V025__rls_personal.sql": "phase5/V025__rls_personal.sql",
    "V026__rls_group.sql": "phase5/V026__rls_group.sql",
    "V027__rls_business.sql": "phase5/V027__rls_business.sql",
    "V028__rls_shared_domains.sql": "phase5/V028__rls_shared_domains.sql",
    "V029__grants_revokes.sql": "phase5/V029__grants_revokes.sql",
    "V030__production_validation.sql": "phase6/V030__production_validation.sql",
}


def main() -> None:
    order = [
        line.strip()
        for line in (ROOT / "phase9" / "MIGRATION_ORDER.txt").read_text().splitlines()
        if line.strip()
    ]
    migrations_dir = ROOT / "migrations"
    manifest_dir = ROOT / "manifest"
    migrations_dir.mkdir(exist_ok=True)
    manifest_dir.mkdir(exist_ok=True)

    sums: list[str] = []
    for filename in order:
        src = ROOT / SOURCES[filename]
        if not src.exists():
            raise FileNotFoundError(src)
        dst = migrations_dir / filename
        shutil.copy2(src, dst)
        digest = hashlib.sha256(dst.read_bytes()).hexdigest()
        sums.append(f"{digest}  {filename}")
        print(f"OK {filename} {digest[:12]}...")

    (manifest_dir / "MIGRATION_ORDER.txt").write_text("\n".join(order) + "\n", encoding="utf-8")
    (manifest_dir / "SHA256SUMS.txt").write_text("\n".join(sums) + "\n", encoding="utf-8")
    for name in ("manifest.json", "RELEASE_BLOCKERS.md", "RELEASE_STATUS.md"):
        src = ROOT / "phase9" / name
        if src.exists():
            shutil.copy2(src, manifest_dir / name)

    print(f"Assembled {len(order)} migrations into {migrations_dir}")


if __name__ == "__main__":
    main()
