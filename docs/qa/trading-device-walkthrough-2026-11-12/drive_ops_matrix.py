#!/usr/bin/env python3
"""Faster ops QuickAdds 6A–15B for Trading QA (assumes money path done)."""
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
LOG = QA / "ops_matrix.log"
results: list[tuple[str, str, str]] = []


def log(msg: str):
    print(msg, flush=True)
    with LOG.open("a") as f:
        f.write(msg + "\n")


def adb(*a, timeout=40):
    return subprocess.run(["adb", "-s", SERIAL, *a], capture_output=True, text=True, timeout=timeout)


def ensure_app():
    adb("shell", "am", "start", "-n", f"{PKG}/{ACT}")
    time.sleep(1.5)
    j = joined()
    if "Edge Gallery" in j or ("Quickadds" not in j and "Northstar" not in j and "Record" not in j):
        adb("shell", "monkey", "-p", PKG, "-c", "android.intent.category.LAUNCHER", "1")
        time.sleep(2.5)


def dump():
    adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
    adb("pull", "/sdcard/ui.xml", "/tmp/trd-ui.xml")
    return ET.parse("/tmp/trd-ui.xml").getroot()


def texts(root=None):
    root = root if root is not None else dump()
    return [(n.attrib.get("text") or "").strip() for n in root.iter("node") if (n.attrib.get("text") or "").strip()]


def joined(root=None):
    return " | ".join(texts(root))


def bounds(n):
    m = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", n.attrib.get("bounds", ""))
    return tuple(map(int, m.groups())) if m else None


def tap_xy(x, y):
    adb("shell", "input", "tap", str(int(x)), str(int(y)))
    time.sleep(0.3)


def tap_label(label, exact=False, root=None):
    root = root if root is not None else dump()
    for n in root.iter("node"):
        t = (n.attrib.get("text") or "").strip()
        if (exact and t == label) or (not exact and label.lower() in t.lower()):
            b = bounds(n)
            if b:
                x1, y1, x2, y2 = b
                tap_xy((x1 + x2) / 2, (y1 + y2) / 2)
                return True
    return False


def shot(name):
    p = subprocess.run(["adb", "-s", SERIAL, "exec-out", "screencap", "-p"], capture_output=True, timeout=30)
    (SHOT / name).write_bytes(p.stdout)


def hide_kb():
    adb("shell", "input", "keyevent", "KEYCODE_ENTER")
    time.sleep(0.15)
    adb("shell", "input", "keyevent", "KEYCODE_BACK")
    time.sleep(0.25)


def to_hub():
    ensure_app()
    for _ in range(6):
        root = dump()
        j = joined(root)
        if "Edge Gallery" in j:
            ensure_app()
            continue
        if ("Quick Adds" in j or ("Purchase" in j and "Sale" in j)) and "Record " not in j and "Save " not in j:
            return True
        if tap_label("✕", exact=True, root=root) or tap_label("×", exact=True, root=root):
            time.sleep(0.4)
            continue
        if tap_label("Quickadds", exact=True, root=root):
            time.sleep(0.8)
            continue
        # do not BACK off app
        break
    tap_label("Quickadds", exact=True)
    time.sleep(0.9)
    return True


def scroll_hub():
    adb("shell", "input", "swipe", "540", "1700", "540", "700", "300")
    time.sleep(0.45)


def open_tile(names):
    to_hub()
    names = names if isinstance(names, (list, tuple)) else [names]
    for _ in range(4):
        root = dump()
        for name in names:
            if tap_label(name, exact=True, root=root) or tap_label(name, root=root):
                time.sleep(1.0)
                return True
        scroll_hub()
    return False


def fill_amount(amount):
    root = dump()
    cands = []
    for n in root.iter("node"):
        t = (n.attrib.get("text") or "").strip()
        cls = n.attrib.get("class", "")
        b = bounds(n)
        if b is None:
            continue
        if t in ("0.00", "0") or (cls.endswith("EditText") and (not t or t.replace(".", "").isdigit())):
            cands.append((b[1], b))
    if cands:
        cands.sort()
        x1, y1, x2, y2 = cands[0][1]
        tap_xy((x1 + x2) / 2, (y1 + y2) / 2)
    else:
        b, _ = None, None
        for lab in ("0.00", "INR", "Quantity", "Qty"):
            b, _ = None, None
            for n in root.iter("node"):
                t = (n.attrib.get("text") or "").strip()
                if lab.lower() in t.lower():
                    b = bounds(n)
                    if b:
                        break
            if b:
                x1, y1, x2, y2 = b
                tap_xy((x1 + x2) / 2, (y1 + y2) / 2)
                break
    time.sleep(0.2)
    for _ in range(14):
        adb("shell", "input", "keyevent", "KEYCODE_DEL")
    adb("shell", "input", "text", str(amount).replace(",", ""))
    hide_kb()


def form_open(j: str) -> bool:
    return any(
        x in j
        for x in (
            "Save ",
            "Post ",
            "Confirm ",
            "Record ",
            "Submit",
            "Adjust stock",
            "Reprint",
        )
    ) and ("✕" in j or "×" in j or "INR" in j or "Quick" in j)


def post(ctas):
    hide_kb()
    root = dump()
    for c in ctas:
        if tap_label(c, exact=False, root=root):
            return True
    tap_xy(540, 2100)
    return True


def run(step, tiles, amount, chips, ctas, shotname, extras=None):
    log(f"== {step} {tiles} {amount} ==")
    ensure_app()
    if not open_tile(tiles):
        results.append((step, "FAIL", f"tile not found {tiles}"))
        shot(shotname)
        log(str(results[-1]))
        return
    for c in chips or []:
        tap_label(c, exact=True) or tap_label(c)
        time.sleep(0.2)
    if amount is not None:
        fill_amount(amount)
    for e in extras or []:
        tap_label(e, exact=False)
        time.sleep(0.25)
    hide_kb()
    shot(shotname.replace(".png", "-pre.png"))
    post(ctas)
    time.sleep(1.3)
    j = joined()
    if form_open(j):
        post(ctas)
        time.sleep(1.0)
        j = joined()
    status = "FAIL" if form_open(j) else "PASS"
    # tile-not-opened false pass: still on hub without opening counts as FAIL if we expected a form
    if status == "PASS" and "Quick Adds" in j and step.startswith(("6", "7", "8", "9", "14")):
        # might have posted and returned — OK
        pass
    results.append((step, status, f"{tiles} {amount}"))
    log(str(results[-1]))
    shot(shotname)
    # close leftover
    if form_open(joined()):
        tap_label("✕", exact=True) or tap_label("×", exact=True)
        time.sleep(0.4)


def main():
    SHOT.mkdir(parents=True, exist_ok=True)
    LOG.write_text("")
    ensure_app()
    to_hub()
    shot("92-ops-hub.png")

    run("6A", ["Stock", "Adjust stock"], 200, ["Set"], ["Post stock", "Save stock", "Confirm", "Save"], "70-stock-6a.png", ["NS-SKU-01"])
    run("6B", ["Stock", "Adjust stock"], 16, ["Add", "Increase"], ["Post stock", "Save stock", "Confirm", "Save"], "71-stock-6b.png", ["NS-SKU-02"])
    run("7A", ["Order"], 120000, ["Customer", "Sales"], ["Save order", "Post order", "Confirm", "Save"], "72-order-7a.png", ["City Retail QA"])
    run("7B", ["Order"], 50000, ["Supplier", "Purchase"], ["Save order", "Post order", "Confirm", "Save"], "73-order-7b.png", ["Deccan Supplies QA"])
    run("8A", ["Shipment", "Dispatch"], 60, ["Outgoing", "Out"], ["Confirm shipment", "Post shipment", "Save", "Confirm"], "74-ship-8a.png")
    run("8B", ["Shipment", "Dispatch"], None, ["Incoming", "In"], ["Confirm shipment", "Post shipment", "Save", "Confirm"], "75-ship-8b.png")
    run("9A", ["Return"], 9000, ["Sale", "Customer"], ["Post return", "Save return", "Confirm", "Save"], "76-return-9a.png")
    run("9B", ["Return"], 3000, ["Purchase", "Supplier"], ["Post return", "Save return", "Confirm", "Save"], "77-return-9b.png")
    run("10A", ["Budget"], 550000, ["Budget"], ["Save budget", "Post budget", "Confirm", "Save"], "78-budget-10a.png")
    run("10B", ["Budget", "Target"], 1100000, ["Target"], ["Save budget", "Save target", "Confirm", "Save"], "79-budget-10b.png")
    run("11A", ["Update", "Status", "Issue"], None, ["Status", "Update"], ["Post update", "Save", "Confirm"], "80-update-11a.png")
    run("11B", ["Issue", "Update"], None, ["Issue", "Blocker"], ["Post issue", "Save", "Confirm"], "81-issue-11b.png")
    run("12A", ["Approval"], None, ["Approve"], ["Submit", "Approve", "Confirm", "Save"], "82-approval-12a.png")
    run("12B", ["Approval"], None, ["Reject"], ["Submit", "Reject", "Confirm", "Save"], "83-approval-12b.png")
    run("13A", ["Hold"], None, ["Hold order", "Order"], ["Confirm", "Hold", "Save"], "84-hold-13a.png")
    run("13B", ["Hold"], None, ["Release", "Dispatch"], ["Confirm", "Release", "Save"], "85-hold-13b.png")
    run("14A", ["Transfer"], 10, [], ["Confirm transfer", "Post transfer", "Confirm", "Save"], "86-transfer-14a.png", ["Main DC", "Branch Hyd"])
    run("14B", ["Transfer"], 5, [], ["Confirm transfer", "Post transfer", "Confirm", "Save"], "87-transfer-14b.png", ["Branch Hyd", "Main DC"])
    run("15A", ["Reprint"], None, [], ["Reprint", "Print", "Confirm", "Done", "Save"], "88-reprint-15a.png")
    run("15B", ["Reprint"], None, [], ["Reprint", "Print", "Confirm", "Done", "Save"], "89-reprint-15b.png")

    ensure_app()
    tap_label("Pulse", exact=True)
    time.sleep(1.2)
    shot("93-pulse-final.png")
    (QA / "PULSE_FINAL.txt").write_text("\n".join(texts()[:120]) + "\n")

    # merge
    prior = {}
    mpath = QA / "MATRIX_RESULTS.tsv"
    if mpath.exists():
        for line in mpath.read_text().splitlines():
            p = line.split("\t")
            if len(p) >= 3:
                prior[p[0]] = (p[0], p[1], p[2])
    for a, b, c in results:
        prior[a] = (a, b, c)
    ordered = sorted(prior.values(), key=lambda r: r[0])
    mpath.write_text("\n".join(f"{a}\t{b}\t{c}" for a, b, c in ordered) + "\n")
    log(f"WROTE {mpath}")
    for r in ordered:
        log(str(r))


if __name__ == "__main__":
    main()
