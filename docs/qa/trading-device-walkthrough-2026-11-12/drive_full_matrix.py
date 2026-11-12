#!/usr/bin/env python3
"""Trading QA: seed parties/locations if needed, then post QuickAdd matrix with keyboard dismiss."""
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
LOG = QA / "full_matrix.log"


def log(msg: str):
    line = msg if msg.endswith("\n") else msg + "\n"
    print(msg, flush=True)
    with LOG.open("a") as f:
        f.write(line)


def sh(*a, timeout=45):
    return subprocess.run(a, capture_output=True, text=True, timeout=timeout)


def adb(*a, timeout=45):
    return sh("adb", "-s", SERIAL, *a, timeout=timeout)


def is_launcher(j: str) -> bool:
    return "Edge Gallery" in j or ("Link to Windows" in j and "momentra" in j and "Quickadds" not in j)


def ensure_app():
    adb("shell", "am", "start", "-n", f"{PKG}/{ACT}")
    time.sleep(2)
    root = dump(force=True)
    j = joined(root)
    if is_launcher(j) or "Quickadds" not in j and "Northstar" not in j and "Record" not in j:
        adb("shell", "monkey", "-p", PKG, "-c", "android.intent.category.LAUNCHER", "1")
        time.sleep(3)
        adb("shell", "am", "start", "-n", f"{PKG}/{ACT}")
        time.sleep(2)


def dump(force: bool = False):
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


def joined(root=None):
    return " | ".join(texts(root))


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
    adb("shell", "input", "tap", str(int(x)), str(int(y)))
    time.sleep(0.35)


def tap_label(label, exact=False, root=None):
    b, _ = find(label, exact=exact, root=root)
    if b is None:
        return False
    x1, y1, x2, y2 = b
    tap_xy((x1 + x2) / 2, (y1 + y2) / 2)
    return True


def shot(name: str):
    p = subprocess.run(
        ["adb", "-s", SERIAL, "exec-out", "screencap", "-p"],
        capture_output=True,
        timeout=30,
    )
    (SHOT / name).write_bytes(p.stdout)


def hide_keyboard():
    # Prefer Enter / Back; also tap empty chrome above sheet
    adb("shell", "input", "keyevent", "KEYCODE_ENTER")
    time.sleep(0.2)
    adb("shell", "input", "keyevent", "KEYCODE_BACK")
    time.sleep(0.35)


def dismiss_to_hub(max_tries=12):
    for _ in range(max_tries):
        root = dump()
        j = joined(root)
        if is_launcher(j):
            ensure_app()
            time.sleep(1.0)
            tap_label("Quickadds", exact=True)
            time.sleep(1.0)
            continue
        hubish = ("Purchase" in j and "Sale" in j and "Collection" in j) or ("Quick Adds" in j)
        form = any(
            x in j
            for x in (
                "Record Purchase",
                "Record Sale",
                "Record Payment",
                "Record Collection",
                "Save purchase",
                "Save sale",
                "Post collection",
                "Save payment",
                "Save spend",
                "Select supplier",
                "Select buyer",
                "+ Add Supplier",
                "+ Add",
                "Search supplier",
                "Search buyer",
            )
        )
        if hubish and not form and "✕" not in j:
            return True
        if tap_label("✕", exact=True, root=root) or tap_label("×", exact=True, root=root):
            time.sleep(0.5)
            continue
        if "No matching" in j or "Search supplier" in j or "Search buyer" in j:
            if tap_label("×", exact=True, root=root) or tap_label("✕", exact=True, root=root):
                time.sleep(0.4)
                continue
        if tap_label("Quickadds", exact=True, root=root):
            time.sleep(0.9)
            continue
        # Prefer tab over BACK (BACK can exit app to launcher)
        if "Quickadds" in j:
            tap_label("Quickadds", exact=True, root=root)
            time.sleep(0.8)
            continue
        adb("shell", "input", "keyevent", "KEYCODE_BACK")
        time.sleep(0.55)
    ensure_app()
    time.sleep(1.5)
    tap_label("Quickadds", exact=True)
    time.sleep(1.0)
    return True


def scroll_hub_down():
    adb("shell", "input", "swipe", "540", "1600", "540", "700", "350")
    time.sleep(0.6)


def open_tile(name: str) -> bool:
    dismiss_to_hub()
    root = dump()
    if tap_label(name, exact=True, root=root) or tap_label(name, root=root):
        time.sleep(1.1)
        return True
    scroll_hub_down()
    root = dump()
    if tap_label(name, exact=True, root=root) or tap_label(name, root=root):
        time.sleep(1.1)
        return True
    scroll_hub_down()
    root = dump()
    ok = tap_label(name, exact=True, root=root) or tap_label(name, root=root)
    time.sleep(1.1)
    return ok


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
            cls.endswith("EditText")
            and (not t or t.replace(".", "").replace(",", "").isdigit())
        ):
            candidates.append((b[1], b, t))
    if candidates:
        candidates.sort()
        x1, y1, x2, y2 = candidates[0][1]
        tap_xy((x1 + x2) / 2, (y1 + y2) / 2)
    else:
        b, _ = find("0.00", root=root)
        if b is None:
            b, _ = find("INR", root=root)
        if b is None:
            return False
        x1, y1, x2, y2 = b
        tap_xy((x1 + x2) / 2, (y1 + y2) / 2)
    time.sleep(0.25)
    adb("shell", "input", "keyevent", "KEYCODE_MOVE_END")
    for _ in range(16):
        adb("shell", "input", "keyevent", "KEYCODE_DEL")
    adb("shell", "input", "text", str(amount).replace(",", ""))
    time.sleep(0.25)
    hide_keyboard()
    return True


def type_into_focused(text: str):
    adb("shell", "input", "text", text.replace(" ", "%s"))
    time.sleep(0.3)


def add_party_from_picker(add_label: str, name: str) -> bool:
    """On supplier/buyer picker: + Add → type name → save."""
    root = dump()
    j = joined(root)
    if name in j:
        return tap_label(name, exact=False, root=root)
    if not (
        tap_label(add_label, exact=False, root=root)
        or tap_label("+ Add Supplier", root=root)
        or tap_label("+ Add", exact=False, root=root)
        or tap_label("Add Supplier", root=root)
        or tap_label("Add buyer", root=root)
        or tap_label("Add customer", root=root)
    ):
        return False
    time.sleep(0.9)
    root = dump()
    # Find first EditText / Name field
    tapped = False
    for n in root.iter("node"):
        cls = n.attrib.get("class", "")
        t = (n.attrib.get("text") or "").strip()
        b = bounds(n)
        if b is None:
            continue
        if cls.endswith("EditText") or t in ("Name", "Supplier name", "Customer name", "Display name", ""):
            if "Search" in t:
                continue
            x1, y1, x2, y2 = b
            if y2 - y1 < 40:
                continue
            tap_xy((x1 + x2) / 2, (y1 + y2) / 2)
            tapped = True
            break
    if not tapped:
        # try label Name
        tap_label("Name", exact=False)
    time.sleep(0.3)
    for _ in range(20):
        adb("shell", "input", "keyevent", "KEYCODE_DEL")
    type_into_focused(name)
    hide_keyboard()
    # role chips if present
    tap_label("Supplier", exact=True) or True
    time.sleep(0.2)
    # save
    for cta in ("Save", "Create", "Add", "Done", "Confirm"):
        if tap_label(cta, exact=True) or tap_label(cta, exact=False):
            break
    time.sleep(1.2)
    root = dump()
    j = joined(root)
    if name in j:
        tap_label(name, exact=False, root=root)
        time.sleep(0.6)
        return True
    # maybe already selected and sheet closed
    return name in j or "Select supplier" not in j


def select_or_create_supplier(name: str) -> bool:
    if not (
        tap_label("Select supplier", exact=False)
        or tap_label("Supplier", exact=False)
    ):
        return False
    time.sleep(0.9)
    root = dump()
    j = joined(root)
    if name in j:
        ok = tap_label(name, exact=False, root=root)
        time.sleep(0.5)
        return ok
    return add_party_from_picker("+ Add Supplier", name)


def select_or_create_buyer(name: str) -> bool:
    if not (
        tap_label("Select buyer", exact=False)
        or tap_label("Select customer", exact=False)
        or tap_label("Buyer", exact=False)
        or tap_label("Customer", exact=False)
    ):
        # maybe channel-only sale
        return False
    time.sleep(0.9)
    root = dump()
    j = joined(root)
    if name in j:
        ok = tap_label(name, exact=False, root=root)
        time.sleep(0.5)
        return ok
    for add in ("+ Add Buyer", "+ Add Customer", "+ Add", "Add buyer", "Add customer"):
        if tap_label(add, exact=False):
            break
    else:
        return False
    time.sleep(0.9)
    # reuse add flow with customer role
    root = dump()
    for n in root.iter("node"):
        cls = n.attrib.get("class", "")
        t = (n.attrib.get("text") or "").strip()
        b = bounds(n)
        if b is None:
            continue
        if cls.endswith("EditText") and "Search" not in t:
            x1, y1, x2, y2 = b
            tap_xy((x1 + x2) / 2, (y1 + y2) / 2)
            break
    for _ in range(20):
        adb("shell", "input", "keyevent", "KEYCODE_DEL")
    type_into_focused(name)
    hide_keyboard()
    tap_label("Customer", exact=True) or tap_label("Buyer", exact=True)
    for cta in ("Save", "Create", "Add", "Done", "Confirm"):
        if tap_label(cta, exact=True) or tap_label(cta, exact=False):
            break
    time.sleep(1.2)
    root = dump()
    if name in joined(root):
        tap_label(name, exact=False)
        time.sleep(0.5)
        return True
    return True


def post_cta(patterns):
    hide_keyboard()
    root = dump()
    for p in patterns:
        if tap_label(p, exact=False, root=root):
            return True
    # bottom primary
    tap_xy(540, 2100)
    return True


def form_still_open(j: str) -> bool:
    return any(
        x in j
        for x in (
            "Save purchase",
            "Save sale",
            "Post collection",
            "Save payment",
            "Save spend",
            "Record Purchase",
            "Record Sale",
            "Record Payment",
            "Record Collection",
            "Post stock",
            "Save order",
            "Confirm shipment",
            "Post return",
            "Save budget",
            "Submit",
        )
    )


results: list[tuple[str, str, str]] = []


def record(step, status, note):
    results.append((step, status, note))
    log(f"{step}\t{status}\t{note}")


def do_purchase(step, amount, chips, shotname, supplier="Deccan Supplies QA"):
    log(f"== {step} Purchase {amount} ==")
    ensure_app()
    if not open_tile("Purchase"):
        record(step, "FAIL", "tile not found")
        shot(shotname)
        return
    for c in chips:
        tap_label(c, exact=True) or tap_label(c)
        time.sleep(0.25)
    fill_amount(amount)
    select_or_create_supplier(supplier)
    hide_keyboard()
    shot(shotname.replace(".png", "-pre.png"))
    post_cta(["Save purchase", "Post purchase", "Confirm"])
    time.sleep(1.6)
    j = joined()
    if form_still_open(j):
        post_cta(["Save purchase", "Post purchase", "Confirm"])
        time.sleep(1.3)
        j = joined()
    status = "FAIL" if form_still_open(j) else "PASS"
    record(step, status, f"Purchase {amount} chips={chips} supplier={supplier}")
    shot(shotname)
    dismiss_to_hub()


def do_sale(step, amount, chips, shotname, buyer="City Retail QA", received=None):
    log(f"== {step} Sale {amount} ==")
    ensure_app()
    if not open_tile("Sale"):
        record(step, "FAIL", "tile not found")
        shot(shotname)
        return
    for c in chips:
        tap_label(c, exact=True) or tap_label(c)
        time.sleep(0.25)
    fill_amount(amount)
    select_or_create_buyer(buyer)
    if received is not None:
        # try fill received amount field
        root = dump()
        for label in ("Received now", "Received amount", "Paid now", "Amount received"):
            if tap_label(label, exact=False, root=root):
                time.sleep(0.2)
                adb("shell", "input", "text", str(received))
                hide_keyboard()
                break
    hide_keyboard()
    # channel Wholesale if visible
    tap_label("Wholesale", exact=True)
    shot(shotname.replace(".png", "-pre.png"))
    post_cta(["Save sale", "Post sale", "Confirm"])
    time.sleep(1.6)
    j = joined()
    if form_still_open(j):
        post_cta(["Save sale", "Post sale", "Confirm"])
        time.sleep(1.3)
        j = joined()
    status = "FAIL" if form_still_open(j) else "PASS"
    record(step, status, f"Sale {amount} chips={chips} buyer={buyer} received={received}")
    shot(shotname)
    dismiss_to_hub()


def do_money(step, tile, amount, chips, cta, shotname, allocate_hint=None):
    log(f"== {step} {tile} {amount} ==")
    ensure_app()
    if not open_tile(tile):
        record(step, "FAIL", f"tile {tile} not found")
        shot(shotname)
        return
    for c in chips:
        tap_label(c, exact=True) or tap_label(c)
        time.sleep(0.25)
    fill_amount(amount)
    if allocate_hint:
        tap_label(allocate_hint, exact=False) or tap_label("Select", exact=False)
        time.sleep(0.7)
        root = dump()
        # pick first non-empty row that looks like an open doc
        for n in root.iter("node"):
            t = (n.attrib.get("text") or "").strip()
            b = bounds(n)
            if not t or b is None:
                continue
            if any(k in t for k in ("₹", "Sale", "Purchase", "AR", "AP", "open", "City", "Deccan")):
                if t in ("Select", "Select Nothing is open  ›", "Nothing is open"):
                    continue
                x1, y1, x2, y2 = b
                tap_xy((x1 + x2) / 2, (y1 + y2) / 2)
                time.sleep(0.4)
                break
    hide_keyboard()
    shot(shotname.replace(".png", "-pre.png"))
    post_cta(cta)
    time.sleep(1.5)
    j = joined()
    if form_still_open(j):
        post_cta(cta)
        time.sleep(1.2)
        j = joined()
    # honest FAIL if nothing open / still form
    if "Nothing is open" in j or "Nothing is waiting" in j and form_still_open(j):
        status = "FAIL"
    else:
        status = "FAIL" if form_still_open(j) else "PASS"
    record(step, status, f"{tile} {amount} chips={chips}")
    shot(shotname)
    dismiss_to_hub()


def do_simple(step, tile_names, amount, chips, cta, shotname, extra_taps=None):
    log(f"== {step} {tile_names} {amount} ==")
    ensure_app()
    opened = False
    for name in tile_names if isinstance(tile_names, (list, tuple)) else [tile_names]:
        if open_tile(name):
            opened = True
            break
    if not opened:
        record(step, "FAIL", f"tile {tile_names} not found")
        shot(shotname)
        return
    for c in chips or []:
        tap_label(c, exact=True) or tap_label(c)
        time.sleep(0.25)
    if amount is not None:
        fill_amount(amount)
    if extra_taps:
        for t in extra_taps:
            tap_label(t, exact=False)
            time.sleep(0.3)
    hide_keyboard()
    shot(shotname.replace(".png", "-pre.png"))
    post_cta(cta)
    time.sleep(1.5)
    j = joined()
    if form_still_open(j):
        post_cta(cta)
        time.sleep(1.2)
        j = joined()
    status = "FAIL" if form_still_open(j) else "PASS"
    record(step, status, f"{tile_names} {amount}")
    shot(shotname)
    dismiss_to_hub()


def seed_locations_via_masters():
    log("== seed locations ==")
    dismiss_to_hub()
    # try Masters / Catalogue / Network tiles
    for label in ("Masters", "Catalogue", "Network", "Godowns", "Locations"):
        if open_tile(label):
            break
    else:
        # open from Pulse Needs / menu — skip if not found
        record("LOC", "SKIP", "no masters tile on hub")
        return
    shot("56-masters-open.png")
    # Locations tab
    tap_label("Locations", exact=False) or tap_label("Godowns", exact=False)
    time.sleep(0.8)
    for loc in ("Main DC", "Branch Hyd"):
        j = joined()
        if loc in j:
            continue
        if not (tap_label("+ Add", exact=False) or tap_label("New", exact=False) or tap_label("Add location", exact=False)):
            continue
        time.sleep(0.8)
        for n in dump().iter("node"):
            if n.attrib.get("class", "").endswith("EditText"):
                b = bounds(n)
                if b:
                    x1, y1, x2, y2 = b
                    tap_xy((x1 + x2) / 2, (y1 + y2) / 2)
                    break
        type_into_focused(loc)
        hide_keyboard()
        tap_label("Save", exact=True) or tap_label("Create", exact=False)
        time.sleep(1.0)
    shot("57-locations.png")
    dismiss_to_hub()


def main():
    import os

    SHOT.mkdir(parents=True, exist_ok=True)
    resume = os.environ.get("RESUME_FROM", "").strip()
    if not resume:
        LOG.write_text("")
    else:
        with LOG.open("a") as f:
            f.write(f"\n# resume from {resume}\n")
    ensure_app()
    dismiss_to_hub()
    shot("58-hub-start-full.png")

    steps_done = resume  # empty = run all; else skip until this id

    def due(step: str) -> bool:
        nonlocal steps_done
        if not steps_done:
            return True
        if step == steps_done:
            steps_done = ""
            return True
        return False

    # Money path with suppliers/buyers
    if due("1A"):
        do_purchase("1A", 45000, [], "60-purchase-1a.png")
    if due("1B"):
        do_purchase(
            "1B",
            30000,
            ["Receive later", "Pay later"],
            "61-purchase-1b.png",
        )
    if due("2A"):
        do_sale("2A", 90000, ["Part paid"], "62-sale-2a.png", received=40000)
    if due("2B"):
        do_sale("2B", 60000, ["Received", "Paid"], "63-sale-2b.png")
    if due("3A"):
        do_money(
            "3A",
            "Collection",
            40000,
            [],
            ["Post collection", "Save collection", "Confirm", "Allocate"],
            "64-collection-3a.png",
            allocate_hint="Select",
        )
    if due("3B"):
        do_money(
            "3B",
            "Collection",
            25000,
            [],
            ["Post collection", "Save collection", "Confirm", "Allocate"],
            "65-collection-3b.png",
            allocate_hint="Select",
        )
    if due("4A"):
        do_money(
            "4A",
            "Payment",
            20000,
            [],
            ["Save payment", "Post payment", "Confirm"],
            "66-payment-4a.png",
            allocate_hint="Select",
        )
    if due("4B"):
        do_money(
            "4B",
            "Payment",
            15000,
            [],
            ["Save payment", "Post payment", "Confirm"],
            "67-payment-4b.png",
            allocate_hint="Select",
        )
    if due("5A"):
        do_money(
            "5A",
            "Expense",
            8000,
            ["Paid"],
            ["Save spend", "Post spend", "Confirm"],
            "68-expense-5a.png",
        )
    if due("5B"):
        do_money(
            "5B",
            "Expense",
            5000,
            ["Pay later"],
            ["Save spend", "Post spend", "Confirm"],
            "69-expense-5b.png",
        )

    if due("LOC"):
        seed_locations_via_masters()

    # Ops
    if due("6A"):
        do_simple(
            "6A",
            ["Stock", "Adjust stock", "STOCK"],
            200,
            ["Set", "Set to"],
            ["Post stock", "Save stock", "Confirm"],
            "70-stock-6a.png",
            extra_taps=["NS-SKU-01"],
        )
    if due("6B"):
        do_simple(
            "6B",
            ["Stock", "Adjust stock", "STOCK"],
            16,
            ["Add", "Increase"],
            ["Post stock", "Save stock", "Confirm"],
            "71-stock-6b.png",
            extra_taps=["NS-SKU-02"],
        )
    if due("7A"):
        do_simple(
            "7A",
            ["Order", "Sales order", "ORDER"],
            120000,
            ["Customer", "Sales order", "SO"],
            ["Save order", "Post order", "Confirm"],
            "72-order-7a.png",
            extra_taps=["City Retail QA"],
        )
    if due("7B"):
        do_simple(
            "7B",
            ["Order", "Purchase order", "ORDER"],
            50000,
            ["Supplier", "Purchase order", "PO"],
            ["Save order", "Post order", "Confirm"],
            "73-order-7b.png",
            extra_taps=["Deccan Supplies QA"],
        )
    if due("8A"):
        do_simple(
            "8A",
            ["Shipment", "Dispatch", "SHIPMENT"],
            60,
            ["Outgoing", "Out"],
            ["Confirm shipment", "Post shipment", "Save shipment", "Confirm"],
            "74-ship-8a.png",
        )
    if due("8B"):
        do_simple(
            "8B",
            ["Shipment", "Dispatch", "SHIPMENT"],
            None,
            ["Incoming", "In"],
            ["Confirm shipment", "Post shipment", "Save shipment", "Confirm"],
            "75-ship-8b.png",
        )
    if due("9A"):
        do_simple(
            "9A",
            ["Return", "RETURN"],
            9000,
            ["Sale", "Customer"],
            ["Post return", "Save return", "Confirm"],
            "76-return-9a.png",
        )
    if due("9B"):
        do_simple(
            "9B",
            ["Return", "RETURN"],
            3000,
            ["Purchase", "Supplier"],
            ["Post return", "Save return", "Confirm"],
            "77-return-9b.png",
        )
    if due("10A"):
        do_simple(
            "10A",
            ["Budget", "BUDGET"],
            550000,
            ["Budget"],
            ["Save budget", "Post budget", "Confirm"],
            "78-budget-10a.png",
        )
    if due("10B"):
        do_simple(
            "10B",
            ["Budget", "Target", "BUDGET"],
            1100000,
            ["Target"],
            ["Save budget", "Save target", "Post", "Confirm"],
            "79-budget-10b.png",
        )
    if due("11A"):
        do_simple(
            "11A",
            ["Update", "Status", "ISSUE", "Issue"],
            None,
            ["Status", "Update"],
            ["Post update", "Save", "Confirm"],
            "80-update-11a.png",
            extra_taps=[],
        )
    if due("11B"):
        do_simple(
            "11B",
            ["Issue", "Update", "ISSUE"],
            None,
            ["Issue", "Blocker"],
            ["Post issue", "Save", "Confirm"],
            "81-issue-11b.png",
        )
    if due("12A"):
        do_simple(
            "12A",
            ["Approval", "APPROVAL"],
            None,
            ["Approve"],
            ["Submit", "Approve", "Confirm"],
            "82-approval-12a.png",
        )
    if due("12B"):
        do_simple(
            "12B",
            ["Approval", "APPROVAL"],
            None,
            ["Reject"],
            ["Submit", "Reject", "Confirm"],
            "83-approval-12b.png",
        )
    if due("13A"):
        do_simple(
            "13A",
            ["Hold", "HOLD"],
            None,
            ["Hold order", "Order"],
            ["Confirm", "Hold", "Save"],
            "84-hold-13a.png",
        )
    if due("13B"):
        do_simple(
            "13B",
            ["Hold", "HOLD"],
            None,
            ["Release", "Hold dispatch", "Dispatch"],
            ["Confirm", "Release", "Save"],
            "85-hold-13b.png",
        )
    if due("14A"):
        do_simple(
            "14A",
            ["Transfer", "TRANSFER"],
            10,
            [],
            ["Confirm transfer", "Post transfer", "Confirm", "Save"],
            "86-transfer-14a.png",
            extra_taps=["Main DC", "Branch Hyd"],
        )
    if due("14B"):
        do_simple(
            "14B",
            ["Transfer", "TRANSFER"],
            5,
            [],
            ["Confirm transfer", "Post transfer", "Confirm", "Save"],
            "87-transfer-14b.png",
            extra_taps=["Branch Hyd", "Main DC"],
        )
    if due("15A"):
        do_simple(
            "15A",
            ["Reprint", "REPRINT"],
            None,
            [],
            ["Reprint", "Print", "Confirm", "Done"],
            "88-reprint-15a.png",
        )
    if due("15B"):
        do_simple(
            "15B",
            ["Reprint", "REPRINT"],
            None,
            [],
            ["Reprint", "Print", "Confirm", "Done"],
            "89-reprint-15b.png",
        )

    # Pulse snapshot
    ensure_app()
    tap_label("Pulse", exact=True)
    time.sleep(1.2)
    shot("90-pulse-final.png")
    pulse_txt = "\n".join(texts()[:80])
    (QA / "PULSE_FINAL.txt").write_text(pulse_txt + "\n")

    # Merge prior PASS rows when resuming
    out = QA / "MATRIX_RESULTS.tsv"
    prior = {}
    if resume and out.exists():
        for line in out.read_text().splitlines():
            parts = line.split("\t")
            if len(parts) >= 3:
                prior[parts[0]] = (parts[0], parts[1], parts[2])
    for a, b, c in results:
        prior[a] = (a, b, c)
    # seed known PASS from earlier run if missing
    for seed in (
        ("1A", "PASS", "Purchase 45000 chips=[] supplier=Deccan Supplies QA"),
        ("1B", "PASS", "Purchase 30000 chips=['Receive later', 'Pay later'] supplier=Deccan Supplies QA"),
        ("2A", "PASS", "Sale 90000 chips=['Part paid'] buyer=City Retail QA received=40000"),
        ("2B", "PASS", "Sale 60000 chips=['Received', 'Paid'] buyer=City Retail QA received=None"),
        ("3A", "PASS", "Collection 40000 chips=[]"),
        ("3B", "PASS", "Collection 25000 chips=[]"),
    ):
        if resume and seed[0] not in prior:
            prior[seed[0]] = seed
    ordered = sorted(prior.values(), key=lambda r: r[0])
    out.write_text("\n".join(f"{a}\t{b}\t{c}" for a, b, c in ordered) + "\n")
    log(f"WROTE {out}")
    for r in ordered:
        log(str(r))


if __name__ == "__main__":
    main()
