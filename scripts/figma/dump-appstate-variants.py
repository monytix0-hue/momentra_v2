#!/usr/bin/env python3
"""Dump the AppState variant map from the checked-in Figma REST export.

The export lives at ``frame-2348-29188.json`` in the repo root (Figma
``GET /v1/files/:key/nodes?ids=2348:29188``). It is ~88 MB, so it is only ever
loaded with :mod:`json` — never streamed as text.

What this prints
----------------
For every variant of the ``AppState`` component set, the variant name, its node
id, and the *state-body* text layers inside it. Shell chrome is dropped:
the status bar, the top bar (QR / life 360 / New Moment / avatar), the
Personal/Group/Business/Clubs context switcher, the moment selector, and the
bottom nav are all part of the shared shell, not of the state body the app
implements in ``BusinessAppStateContent.{swift,kt}``.

Usage
-----
    python3 scripts/figma/dump-appstate-variants.py            # markdown table
    python3 scripts/figma/dump-appstate-variants.py --json     # JSON
    python3 scripts/figma/dump-appstate-variants.py --json --set MomentSwitcher

Exit codes
----------
0  success
2  the export (or the requested component set) could not be read
"""

from __future__ import annotations

import argparse
import json
import sys
import unicodedata
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_EXPORT = REPO_ROOT / "frame-2348-29188.json"

# Page 2348:29188 "Business Redesign" → section "App States · Shared (all Business verticals)".
PAGE_NODE_ID = "2348:29188"
APP_STATES_SECTION = "App States · Shared (all Business verticals)"

APPSTATE_SET_ID = "2666:32397"
SWITCHER_SET_ID = "2678:37791"

#: Direct children of a variant that hold the screen body. Everything else
#: (Header chrome, the moment selector, the bottom nav, the glow) is shell.
#:
#: `Story content` is the MomentSwitcher set's body frame; unlike the AppState
#: frames it keeps its own header/tabs/search inside, so those stay in the
#: output — they are genuinely part of that component's API.
BODY_FRAME_NAMES = ("State body", "Sheet", "Story content")

#: Frames that are unambiguously shell chrome, used by the fallback path.
CHROME_FRAME_NAMES = (
    "Header chrome",
    "Business / Active Moments Selector",
    "Biz · Bottom navbar · native",
    "Bottom nav",
    "glow",
    "scrim",
)

#: Shell copy that must never be reported as state-body copy. The body walk
#: already excludes these; the list is a safety net so a future Figma re-export
#: that moves chrome inside the body frame cannot silently pollute the map.
CHROME_STRINGS = frozenset(
    {
        "9:41",
        "●●●●",
        "▲",
        "■",
        "⚙",
        "QR",
        "life 360",
        "New Moment",
        "KR",
        "Personal",
        "Group",
        "Business",
        "Clubs",
        "Your business",
        "Switch Moment",
        "Active moment",
        "Pulse",
        "Moments",
        "Quick adds",
        "Life",
        "Memory",
        "status · time",
    }
)


def load_export(path: Path) -> dict:
    if not path.exists():
        raise SystemExit(f"export not found: {path}")
    with path.open("r", encoding="utf-8") as fh:
        return json.load(fh)


def find_node(node: dict, node_id: str) -> dict | None:
    if node.get("id") == node_id:
        return node
    for child in node.get("children", []) or []:
        hit = find_node(child, node_id)
        if hit is not None:
            return hit
    return None


def find_node_by_name(node: dict, name: str) -> dict | None:
    if node.get("name") == name:
        return node
    for child in node.get("children", []) or []:
        hit = find_node_by_name(child, name)
        if hit is not None:
            return hit
    return None


def parse_variant_props(name: str) -> dict[str, str]:
    """``"Screen=Pulse, State=Empty"`` → ``{"Screen": "Pulse", "State": "Empty"}``."""
    props: dict[str, str] = {}
    for part in name.split(","):
        part = part.strip()
        if "=" in part:
            key, _, value = part.partition("=")
            props[key.strip()] = value.strip()
    return props


#: Glyph-only strings that are real *copy* rather than badge decoration. Figma
#: renders a bare em dash as the "Last synced" value, so it must survive.
COPY_GLYPHS = frozenset({"—", "–"})


def is_decorative(text: str) -> bool:
    """True for glyph-only / emoji-only layers (badge dots, tile emoji, ✕, ✓, !).

    Punctuation counts as decoration unless it is one of :data:`COPY_GLYPHS` —
    the em dash is a genuine row value in the Offline and ViewAll frames.
    """
    stripped = text.strip()
    if not stripped:
        return True
    if stripped in COPY_GLYPHS:
        return False
    if any(ch.isalnum() for ch in stripped):
        return False
    return all(
        unicodedata.category(ch).startswith(("S", "Z", "P")) or ord(ch) >= 0x1F000
        for ch in stripped
    )


def body_frame(variant: dict) -> dict | None:
    """The frame holding the screen body, or ``None`` when the shell-only frame."""
    children = variant.get("children", []) or []
    for child in children:
        if child.get("name") in BODY_FRAME_NAMES:
            return child
    # Fallback: anything that is not explicitly chrome.
    remainder = [c for c in children if c.get("name") not in CHROME_FRAME_NAMES]
    if len(remainder) == 1:
        return remainder[0]
    return None


def collect_texts(node: dict, out: list[dict], *, include_chrome: bool = False) -> None:
    if node.get("type") == "TEXT":
        raw = node.get("characters") or ""
        text = raw.strip()
        if text and not is_decorative(text):
            if include_chrome or text not in CHROME_STRINGS:
                style = node.get("style") or {}
                box = node.get("absoluteBoundingBox") or {}
                out.append(
                    {
                        "nodeId": node.get("id"),
                        "text": raw,
                        "fontSize": style.get("fontSize"),
                        "fontWeight": style.get("fontWeight"),
                        "y": box.get("y"),
                    }
                )
    for child in node.get("children", []) or []:
        collect_texts(child, out, include_chrome=include_chrome)


def build_map(document: dict, set_id: str) -> dict:
    component_set = find_node(document, set_id)
    if component_set is None:
        raise SystemExit(f"component set {set_id} not found in export")
    if component_set.get("type") != "COMPONENT_SET":
        raise SystemExit(f"node {set_id} is {component_set.get('type')}, expected COMPONENT_SET")

    props = component_set.get("componentPropertyDefinitions") or {}
    variants = []
    for child in component_set.get("children", []) or []:
        if child.get("type") != "COMPONENT":
            continue
        frame = body_frame(child)
        texts: list[dict] = []
        if frame is not None:
            collect_texts(frame, texts)
        variants.append(
            {
                "name": child.get("name"),
                "nodeId": child.get("id"),
                "properties": parse_variant_props(child.get("name") or ""),
                "bodyFrame": frame.get("name") if frame else None,
                "bodyFrameId": frame.get("id") if frame else None,
                "texts": texts,
                "strings": [t["text"] for t in texts],
            }
        )

    variants.sort(key=lambda v: (list(props.keys()), v["name"] or ""))
    return {
        "page": PAGE_NODE_ID,
        "section": APP_STATES_SECTION,
        "componentSet": {
            "id": component_set.get("id"),
            "name": component_set.get("name"),
            "variantProperties": {
                key: {
                    "type": value.get("type"),
                    "defaultValue": value.get("defaultValue"),
                    "variantOptions": value.get("variantOptions", []),
                }
                for key, value in props.items()
            },
        },
        "variantCount": len(variants),
        "variants": variants,
    }


def render_markdown(data: dict) -> str:
    lines: list[str] = []
    set_info = data["componentSet"]
    lines.append(f"### `{set_info['name']}` — {set_info['id']}")
    lines.append("")
    for key, prop in set_info["variantProperties"].items():
        options = ", ".join(f"`{o}`" for o in prop["variantOptions"])
        lines.append(f"- **{key}**: {options}")
    lines.append("")
    lines.append(f"{data['variantCount']} variants.")
    lines.append("")
    lines.append("| Variant | Node ID | State-body strings |")
    lines.append("|---------|---------|--------------------|")
    for variant in data["variants"]:
        strings = " · ".join(f"`{s}`" for s in variant["strings"])
        lines.append(f"| `{variant['name']}` | `{variant['nodeId']}` | {strings} |")
    lines.append("")
    return "\n".join(lines)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--export", type=Path, default=DEFAULT_EXPORT, help="path to the Figma REST export")
    parser.add_argument("--json", action="store_true", help="emit JSON instead of a markdown table")
    parser.add_argument(
        "--set",
        default="AppState",
        choices=("AppState", "MomentSwitcher"),
        help="which component set to dump (default: AppState)",
    )
    args = parser.parse_args(argv)

    payload = load_export(args.export)
    nodes = payload.get("nodes") or {}
    page = nodes.get(PAGE_NODE_ID)
    if page is None:
        raise SystemExit(f"page node {PAGE_NODE_ID} missing from export")
    document = page.get("document")
    if document is None:
        raise SystemExit("page node has no document")

    set_id = APPSTATE_SET_ID if args.set == "AppState" else SWITCHER_SET_ID
    data = build_map(document, set_id)

    if args.json:
        json.dump(data, sys.stdout, indent=2, ensure_ascii=False)
        sys.stdout.write("\n")
    else:
        sys.stdout.write(render_markdown(data))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
