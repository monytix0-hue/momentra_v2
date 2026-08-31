"""Download Personal empty Figma MCP assets into Android + iOS catalogs."""
from __future__ import annotations

import json
import re
import urllib.request
from pathlib import Path

ROOT = Path(r"g:\momentra_v2")
AGENT = Path(r"C:\Users\HI\.cursor\projects\g-momentra-v2\agent-tools")
ANDROID_NODPI = ROOT / "apk/app/src/main/res/drawable-nodpi"
ANDROID_DRAWABLE = ROOT / "apk/app/src/main/res/drawable"
IOS_ASSETS = ROOT / "momentra/momentra/Assets.xcassets/PersonalEmpty"

ANDROID_NODPI.mkdir(parents=True, exist_ok=True)
ANDROID_DRAWABLE.mkdir(parents=True, exist_ok=True)
IOS_ASSETS.mkdir(parents=True, exist_ok=True)

TARGETS = {
    "pulse": AGENT / "8b1a6abc-3f26-44af-920e-098e9f78fecb.txt",
    "life": AGENT / "8ebe0b0b-daa4-4c65-a167-303618f068d9.txt",
    "memory": AGENT / "67b44838-1038-46a6-ba2d-21ab1fac14e1.txt",
}

# Moments + Create from this session were not always written to agent-tools;
# scan for them, else use inline URLs from known session payloads if present.
for p in AGENT.glob("*.txt"):
    head = p.read_text(encoding="utf-8", errors="ignore")[:2000]
    if "353:391" in head or "Storytelling Empty" in head:
        TARGETS["moments"] = p
    if "353:452" in head or "Create / Redesigned" in head:
        TARGETS["create"] = p

FALLBACK: dict[str, list[tuple[str, str]]] = {}

CHROME_SKIP = (
    "Untitled",
    "Radar",
    "Plus",
    "StatusDot",
    "UploadedImage",
    "Add",
)

PAT = re.compile(
    r'const (img[A-Za-z0-9_]+)\s*=\s*"(https://www\.figma\.com/api/mcp/asset/[a-f0-9-]+(?:\.(?:png|svg|jpg|jpeg|webp))?)"'
)


def camel_to_snake(name: str) -> str:
    name = re.sub(r"^img", "", name)
    s1 = re.sub(r"(.)([A-Z][a-z]+)", r"\1_\2", name)
    return re.sub(r"([a-z0-9])([A-Z])", r"\1_\2", s1).lower()


def parse_file(path: Path) -> list[tuple[str, str]]:
    text = path.read_text(encoding="utf-8", errors="ignore")
    out: list[tuple[str, str]] = []
    seen: set[str] = set()
    for name, url in PAT.findall(text):
        if any(s in name for s in CHROME_SKIP):
            continue
        if url in seen:
            continue
        seen.add(url)
        out.append((camel_to_snake(name), url))
    return out


def sniff_ext(data: bytes) -> str:
    if data.startswith(b"\x89PNG"):
        return "png"
    if data.startswith(b"<svg") or data.startswith(b"<?xml") or b"<svg" in data[:200]:
        return "svg"
    if data.startswith(b"\xff\xd8"):
        return "jpg"
    return "bin"


def write_ios_imageset(name: str, data: bytes, ext: str) -> None:
    folder = IOS_ASSETS / name
    folder.mkdir(parents=True, exist_ok=True)
    filename = f"img.{ext}" if ext != "svg" else "icon.svg"
    (folder / filename).write_bytes(data)
    if ext == "svg":
        contents = {
            "images": [{"filename": filename, "idiom": "universal"}],
            "info": {"author": "xcode", "version": 1},
            "properties": {"preserves-vector-representation": True},
        }
    else:
        contents = {
            "images": [
                {"filename": filename, "idiom": "universal", "scale": "1x"},
                {"idiom": "universal", "scale": "2x"},
                {"idiom": "universal", "scale": "3x"},
            ],
            "info": {"author": "xcode", "version": 1},
        }
    (folder / "Contents.json").write_text(json.dumps(contents, indent=2), encoding="utf-8")


def download_one(screen: str, logical: str, url: str) -> dict:
    asset_name = f"personal_{screen}_{logical}"
    req = urllib.request.Request(url, headers={"User-Agent": "momentra-asset-sync/1.0"})
    with urllib.request.urlopen(req, timeout=60) as resp:
        data = resp.read()
    ext = sniff_ext(data)
    result = {"name": asset_name, "ext": ext, "bytes": len(data), "url": url}

    if ext == "svg":
        # Keep SVG in assets for reference; also copy to iOS. Android VectorDrawable
        # conversion is lossy without a converter — rasterize via storing PNG if possible.
        # For now write SVG bytes as .xml only if already Android vector; else save PNG
        # attempt: many MCP SVGs are small icons — save as raw file under drawable-nodpi
        # won't work for SVG. Save to assets + iOS; for Android write bytes to nodpi as
        # .png only when PNG. For SVG icons, write to assets/personal_empty and also
        # attempt simple copy for Coil later. Prefer converting tiny SVGs by keeping
        # existing VectorDrawables where we already have them.
        assets_dir = ROOT / "apk/app/src/main/assets/personal_empty"
        assets_dir.mkdir(parents=True, exist_ok=True)
        (assets_dir / f"{asset_name}.svg").write_bytes(data)
        write_ios_imageset(asset_name, data, "svg")
        # Also try to place a PNG sibling by requesting? MCP returns svg. Done.
        result["android"] = str(assets_dir / f"{asset_name}.svg")
        result["ios"] = asset_name
    else:
        dest = ANDROID_NODPI / f"{asset_name}.{ext}"
        dest.write_bytes(data)
        write_ios_imageset(asset_name, data, ext)
        result["android"] = str(dest)
        result["ios"] = asset_name
    return result


def main() -> None:
    manifest: dict[str, list[tuple[str, str]]] = {}
    for screen, path in TARGETS.items():
        if path.exists():
            manifest[screen] = parse_file(path)
            print(f"{screen}: {len(manifest[screen])} from {path.name}")
        else:
            print(f"{screen}: missing {path}")

    for screen, items in FALLBACK.items():
        existing_urls = {u for _, u in manifest.get(screen, [])}
        for logical, url in items:
            if url not in existing_urls:
                manifest.setdefault(screen, []).append((logical, url))
                print(f"{screen}: +fallback {logical}")

    results = []
    for screen, items in manifest.items():
        for logical, url in items:
            try:
                r = download_one(screen, logical, url)
                results.append(r)
                print(f"OK {r['name']}.{r['ext']} ({r['bytes']})")
            except Exception as e:
                print(f"FAIL {screen}/{logical}: {e}")
                results.append({"name": f"personal_{screen}_{logical}", "error": str(e), "url": url})

    report = ROOT / "apk/scripts/personal_asset_download_report.json"
    report.write_text(json.dumps(results, indent=2), encoding="utf-8")
    print("report", report)


if __name__ == "__main__":
    main()
