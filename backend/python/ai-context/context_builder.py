"""AI Context Builder — governed minimum context assembly (Architecture v4 D13)."""
from __future__ import annotations

# Placeholder for FastAPI integration; workers call this module to assemble context.

def build_minimum_context(user_id: str, scope_type: str, scope_id: str, purpose: str) -> dict:
    return {
        "user_id": user_id,
        "scope_type": scope_type,
        "scope_id": scope_id,
        "purpose": purpose,
        "items": [],
    }
