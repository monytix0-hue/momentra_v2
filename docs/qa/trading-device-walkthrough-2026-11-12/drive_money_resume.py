#!/usr/bin/env python3
"""Resume Trading QA money QuickAdds from hub (1B–5B then ops)."""
from __future__ import annotations

import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

SERIAL = "00158357G000049"
QA = Path("/mnt/Coding/momentra/docs/qa/trading-device-walkthrough-2026-11-12")
SHOT = QA / "screenshots"
PKG = "resolvingpoint.momentra"
ACT = "com.example.momentra.MainActivity"


def sh(*a, timeout=45):
    return subprocess.run(a, capture_output=True, text=True, timeout=timeout)


def adb(*a, timeout=45):
    return sh("adb", "-s", SERIAL, *a, timeout=timeout)


def ensure_app():
    adb("shell", "am", "start", "-n", f"{PKG}/{ACT}")
    time.sleep(2)


def dump():
    adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
    adb("pull", "/sdcard/ui.xml", "/tmp/trd-ui.xml")
    return ET.parse("/tmp/trd-ui.xml").getroot()


def texts(root=None):
    root = root if root is not None else dump()
    out = []
    for n in root.iter("node"):
        t = (n.attrib.get("text") or "").strip()
        if t:
            out.append(t)
    return out


def bounds(n):
    m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", n.attrib.get("bounds", ""))
    return tuple(map(int, m.groups())) if m else None


def find(label, exact=False, root=None):
    root = root if root is not None else dump()
    for n in root.iter("node"):
        t = (n.attrib.get("text") or "").strip()
        if (exact and t == label) or (not exact and label.lower() in t.lower()):
            b = bounds(n)
            if b:
                return b, t
    return None, None


def tap_xy(x, y):
    adb("shell", "input", "tap", str(x), str(y))
    time.sleep(0.4)


def tap_label(label, exact=False, root=None):
    b, _ = find(label, exact=exact, root=root)
    if b is None:
        return False
    x1, y1, x2, y2 = b
    tap_xy((x1 + x2) // 2, (y1 + y2) // 2)
    return True


def shot(name: str):
    p = subprocess.run(
        ["adb", "-s", SERIAL, "exec-out", "screencap", "-p"],
        capture_output=True,
        timeout=30,
    )
    (SHOT / name).write_bytes(p.stdout)


def dismiss_to_hub(max_tries=10):
    for _ in range(max_tries):
        root = dump()
        ts = texts(root)
        joined = " | ".join(ts)
        on_hub = ("Purchase" in joined and "Sale" in joined and "Collection" in joined) or (
            "Quick Adds" in joined
        )
        form_open = any(
            x in joined
            for x in (
                "Save sale",
                "Save purchase",
                "Post collection",
                "Post payment",
                "Save spend",
                "Record Sale",
                "Record Purchase",
                "Post stock",
                "Save order",
                "Confirm shipment",
                "Post return",
                "Save budget",
                "Post update",
                "Submit approval",
                "Hold",
                "Transfer",
                "Reprint",
            )
        ) and ("✕" in joined or "INR" in joined or "Quick amount" in joined)
        if on_hub and not form_open:
            return True
        if tap_label("✕", exact=True, root=root):
            time.sleep(0.5)
            continue
        if tap_label("Quickadds", exact=True, root=root):
            time.sleep(1.0)
            continue
        adb("shell", "input", "keyevent", "KEYCODE_BACK")
        time.sleep(0.6)
    ensure_app()
    time.sleep(1.5)
    tap_label("Quickadds", exact=True)
    time.sleep(1.0)
    return True


def fill_amount(amount):
    root = dump()
    candidates = []
    for n in root.iter("node"):
        t = (n.attrib.get("text") or "").strip()
        cls = n.attrib.get("class", "")
        b = bounds(n)
        if b is None:
            continue
        if t in ("0.00", "0", "₹0.00") or (
            cls.endswith("EditText") and (not t or t.replace(".", "").replace(",", "").isdigit())
        ):
            candidates.append((b[1], b))
    if not candidates:
        b, _ = find("0.00", root=root)
        if b is None:
            b, _ = find("INR", root=root)
        if b is None:
            return False
        x1, y1, x2, y2 = b
        tap_xy((x1 + x2) // 2, (y1 + y2) // 2)
    else:
        candidates.sort()
        x1, y1, x2, y2 = candidates[0][1]
        tap_xy((x1 + x2) // 2, (y1 + y2) // 2)
    time.sleep(0.25)
    adb("shell", "input", "keyevent", "KEYCODE_MOVE_END")
    for _ in range(14):
        adb("shell", "input", "keyevent", "KEYCODE_DEL")
    adb("shell", "input", "text", str(amount).replace(",", ""))
    time.sleep(0.25)
    return True


def post_cta(patterns):
    root = dump()
    for p in patterns:
        if tap_label(p, exact=False, root=root):
            return True
    tap_xy(540, 2050)
    return True


results: list[tuple[str, str, str]] = []


def open_tile(name: str) -> bool:
    dismiss_to_hub()
    root = dump()
    return tap_label(name, exact=True, root=root) or tap_label(name, root=root)


def still_on_form(joined: str) -> bool:
    return any(
        x in joined
        for x in (
            "Save sale",
            "Save purchase",
            "Post collection",
            "Post payment",
            "Save spend",
            "Post spend",
            "Record Sale",
            "Record Purchase",
        )
    )


def do_money(step, tile, amount, chips, cta, shotname):
    print(f"== {step} {tile} {amount} ==", flush=True)
    ensure_app()
    if not open_tile(tile):
        results.append((step, "FAIL", f"tile {tile} not found"))
        shot(shotname)
        print(results[-1], flush=True)
        return
    time.sleep(1.0)
    for c in chips:
        tap_label(c, exact=True) or tap_label(c)
        time.sleep(0.3)
    if amount is not None:
        fill_amount(amount)
    shot(shotname.replace(".png", "-pre.png"))
    post_cta(cta)
    time.sleep(1.5)
    joined = " | ".join(texts()[:80])
    if still_on_form(joined):
        post_cta(cta)
        time.sleep(1.2)
        joined = " | ".join(texts()[:80])
    status = "FAIL" if still_on_form(joined) else "PASS"
    results.append((step, status, f"{tile} {amount} chips={chips}"))
    print(results[-1], flush=True)
    shot(shotname)
    dismiss_to_hub()


def main():
    SHOT.mkdir(parents=True, exist_ok=True)
    ensure_app()
    dismiss_to_hub()
    shot("45-hub-resume.png")

    do_money(
        "1B",
        "Purchase",
        30000,
        ["Pay later", "Receive later"],
        ["Save purchase", "Post purchase", "Confirm"],
        "46-purchase-1b.png",
    )
    do_money(
        "2A",
        "Sale",
        90000,
        ["Part paid"],
        ["Save sale", "Post sale", "Confirm"],
        "47-sale-2a.png",
    )
    do_money(
        "2B",
        "Sale",
        60000,
        ["Received", "Paid"],
        ["Save sale", "Post sale", "Confirm"],
        "48-sale-2b.png",
    )
    do_money(
        "3A",
        "Collection",
        40000,
        [],
        ["Post collection", "Save collection", "Confirm", "Allocate"],
        "49-collection-3a.png",
    )
    do_money(
        "3B",
        "Collection",
        25000,
        [],
        ["Post collection", "Save collection", "Confirm", "Allocate"],
        "50-collection-3b.png",
    )
    do_money(
        "4A",
        "Payment",
        20000,
        [],
        ["Post payment", "Save payment", "Confirm"],
        "51-payment-4a.png",
    )
    do_money(
        "4B",
        "Payment",
        15000,
        [],
        ["Post payment", "Save payment", "Confirm"],
        "52-payment-4b.png",
    )
    do_money(
        "5A",
        "Expense",
        8000,
        ["Paid"],
        ["Save spend", "Post spend", "Confirm"],
        "53-expense-5a.png",
    )
    do_money(
        "5B",
        "Expense",
        5000,
        ["Pay later"],
        ["Save spend", "Post spend", "Confirm"],
        "54-expense-5b.png",
    )

    out = QA / "MONEY_PARTIAL.txt"
    out.write_text("\n".join(f"{a}\t{b}\t{c}" for a, b, c in results) + "\n")
    print("WROTE", out, flush=True)
    for r in results:
        print(r)


if __name__ == "__main__":
    main()
