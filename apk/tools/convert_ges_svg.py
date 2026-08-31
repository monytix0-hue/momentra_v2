import re
from pathlib import Path

def svg_to_android_vector(svg_path: Path) -> str:
    text = svg_path.read_text(encoding="utf-8")
    viewbox = re.search(r'viewBox="([^"]+)"', text)
    vb = viewbox.group(1).split() if viewbox else ["0", "0", "24", "24"]
    vw, vh = float(vb[2]), float(vb[3])
    width = re.search(r'width="(\d+)"', text)
    height = re.search(r'height="(\d+)"', text)
    wd = int(width.group(1)) if width else int(vw)
    ht = int(height.group(1)) if height else int(vh)
    paths = re.findall(r'<path[^>]*d="([^"]+)"[^>]*/>', text)
    stroke_width = re.findall(r'stroke-width="([^"]+)"', text)
    strokes = re.findall(r'stroke="([^"]+)"', text)
    sw = stroke_width[0] if stroke_width else "2"
    color = strokes[0] if strokes else "#FFFFFF"
    lines = [
        '<?xml version="1.0" encoding="utf-8"?>',
        f'<vector xmlns:android="http://schemas.android.com/apk/res/android" '
        f'android:width="{wd}dp" android:height="{ht}dp" '
        f'android:viewportWidth="{vw}" android:viewportHeight="{vh}">',
    ]
    for d in paths:
        lines.append(
            f'  <path android:pathData="{d}" android:strokeColor="{color}" '
            f'android:strokeWidth="{sw}" android:fillColor="#00000000" '
            f'android:strokeLineCap="round" android:strokeLineJoin="round"/>'
        )
    lines.append("</vector>")
    return "\n".join(lines)

base = Path(__file__).resolve().parents[1] / "app" / "src" / "main" / "res" / "drawable"
for f in sorted(base.glob("ges_*.xml")):
    content = f.read_text(encoding="utf-8")
    if content.startswith("<svg"):
        f.write_text(svg_to_android_vector(f), encoding="utf-8")
        print("converted", f.name)
