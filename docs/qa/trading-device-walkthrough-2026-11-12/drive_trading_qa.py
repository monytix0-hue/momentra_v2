#!/usr/bin/env python3
"""ADB/uiautomator helpers for Trading device QA walk."""
from __future__ import annotations

import re
import subprocess
import time
from pathlib import Path

SERIAL = "00158357G000049"
QA = Path("/mnt/Coding/momentra/docs/qa/trading-device-walkthrough-2026-11-12")
SHOTS = QA / "screenshots"
UI = Path("/tmp/trd-ui.xml")


def adb(*args: str, check: bool = True) -> subprocess.CompletedProcess:
    return subprocess.run(
        ["adb", "-s", SERIAL, *args],
        check=check,
        capture_output=True,
        text=True,
    )


def dump() -> None:
    adb("shell", "uiautomator", "dump", "/sdcard/ui.xml")
    adb("pull", "/sdcard/ui.xml", str(UI))


def texts() -> list[str]:
    import xml.etree.ElementTree as ET

    root = ET.parse(UI).getroot()
    out = []
    for n in root.iter("node"):
        t = (n.attrib.get("text") or "").strip()
        if t:
            out.append(t)
    return out


def find_bounds(match: str, exact: bool = False) -> tuple[int, int] | None:
    import xml.etree.ElementTree as ET

    root = ET.parse(UI).getroot()
    for n in root.iter("node"):
        t = (n.attrib.get("text") or "").strip()
        d = (n.attrib.get("content-desc") or "").strip()
        hay = t or d
        ok = hay == match if exact else (match.lower() in hay.lower())
        if ok and hay:
            b = list(map(int, re.findall(r"\d+", n.attrib["bounds"])))
            return (b[0] + b[2]) // 2, (b[1] + b[3]) // 2
    return None


def find_edittexts() -> list[tuple[int, int, str]]:
    import xml.etree.ElementTree as ET

    root = ET.parse(UI).getroot()
    out = []
    for n in root.iter("node"):
        if not n.attrib.get("class", "").endswith("EditText"):
            continue
        b = list(map(int, re.findall(r"\d+", n.attrib["bounds"])))
        out.append(((b[0] + b[2]) // 2, (b[1] + b[3]) // 2, (n.attrib.get("text") or "")))
    return out


def tap(x: int, y: int) -> None:
    adb("shell", "input", "tap", str(x), str(y))


def tap_text(match: str, exact: bool = False) -> bool:
    dump()
    xy = find_bounds(match, exact=exact)
    if not xy:
        return False
    tap(*xy)
    time.sleep(0.6)
    return True


def type_text(s: str) -> None:
    # Escape spaces for adb input text
    escaped = s.replace(" ", "%s").replace("'", "")
    adb("shell", "input", "text", escaped)


def hide_kb() -> None:
    adb("shell", "input", "keyevent", "KEYCODE_BACK")
    time.sleep(0.3)


def swipe_up(dy: int = 900) -> None:
    adb("shell", "input", "swipe", "540", "1900", "540", str(1900 - dy), "350")
    time.sleep(0.5)


def swipe_down(dy: int = 900) -> None:
    adb("shell", "input", "swipe", "540", "900", "540", str(900 + dy), "350")
    time.sleep(0.5)


def shot(name: str) -> None:
    p = SHOTS / name
    subprocess.run(
        f"adb -s {SERIAL} exec-out screencap -p > {p}",
        shell=True,
        check=True,
    )


def blob() -> str:
    dump()
    return "\n".join(texts())


def wait_text(match: str, timeout: float = 15.0) -> bool:
    end = time.time() + timeout
    while time.time() < end:
        if match.lower() in blob().lower():
            return True
        time.sleep(0.6)
    return False


def open_quickadds() -> None:
    dump()
    xy = find_bounds("Quickadds", exact=True) or find_bounds("Quick Adds")
    if xy:
        tap(*xy)
        time.sleep(1.5)


def open_tile(title: str) -> bool:
    open_quickadds()
    time.sleep(0.5)
    for _ in range(8):
        dump()
        xy = find_bounds(title, exact=True)
        if xy:
            tap(*xy)
            time.sleep(1.2)
            return True
        swipe_up(700)
    # try scroll to top and again
    for _ in range(6):
        swipe_down(700)
    for _ in range(8):
        dump()
        xy = find_bounds(title, exact=True)
        if xy:
            tap(*xy)
            time.sleep(1.2)
            return True
        swipe_up(700)
    return False


def fill_first_empty_amount(amount: str) -> bool:
    dump()
    edits = find_edittexts()
    # Prefer empty or zero-looking amount fields
    target = None
    for x, y, t in edits:
        if t.strip() in ("", "0", "0.0", "₹0"):
            target = (x, y)
            break
    if not target and edits:
        target = edits[0][:2]
    if not target:
        return False
    tap(*target)
    time.sleep(0.2)
    # clear
    for _ in range(12):
        adb("shell", "input", "keyevent", "KEYCODE_DEL")
    type_text(amount)
    hide_kb()
    return True


def tap_post_cta() -> bool:
    dump()
    for label in (
        "Post purchase",
        "Post sale",
        "Post collection",
        "Post payment",
        "Post expense",
        "Post stock",
        "Post stock update",
        "Post order",
        "Post delivery",
        "Post shipment",
        "Post return",
        "Save revision",
        "Post update",
        "Post issue",
        "Decide",
        "Approve",
        "Hold order",
        "Hold dispatch",
        "Confirm transfer",
        "Print",
        "Share",
        "Post",
        "Save",
        "Charge",
        "Record",
    ):
        xy = find_bounds(label)
        if xy:
            tap(*xy)
            time.sleep(1.5)
            return True
    # sticky bottom area
    tap(540, 2200)
    time.sleep(1.2)
    return False


def dismiss_sheet() -> None:
    dump()
    for label in ("Close", "‹", "Cancel", "Done"):
        if tap_text(label):
            time.sleep(0.8)
            return
    adb("shell", "input", "keyevent", "KEYCODE_BACK")
    time.sleep(0.8)


if __name__ == "__main__":
    print(blob()[:500])
