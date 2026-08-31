from pathlib import Path
import urllib.request
from PIL import Image

out_android = Path(r"g:/momentra_v2/apk/app/src/main/res/drawable-nodpi")
out_ios = Path(r"g:/momentra_v2/momentra/momentra/Assets.xcassets/PersonalSetup")
out_ios.mkdir(parents=True, exist_ok=True)
(out_ios / "Contents.json").write_text(
    '{"info":{"author":"xcode","version":1}}\n', encoding="utf-8"
)

assets = {
    "personal_setup_life_ops_scroll": "https://www.figma.com/api/mcp/asset/26833590-1477-417f-8aff-9fd8f6de623d.png",
    "personal_setup_future_scroll": "https://www.figma.com/api/mcp/asset/138c7193-b672-45b0-9d80-4523797cf52c.png",
    "personal_setup_lifestyle_scroll": "https://www.figma.com/api/mcp/asset/665ca04a-3089-405e-aa20-0383d486067c.png",
    "personal_setup_relationships_scroll": "https://www.figma.com/api/mcp/asset/b9923a01-42c2-448a-9f4f-0eddc40a3a0a.png",
}

contents = """{
  "images" : [
    { "filename" : "img.png", "idiom" : "universal", "scale" : "1x" },
    { "idiom" : "universal", "scale" : "2x" },
    { "idiom" : "universal", "scale" : "3x" }
  ],
  "info" : { "author" : "xcode", "version" : 1 }
}
"""

for name, url in assets.items():
    raw = out_android / f"{name}_full.png"
    print("download", name)
    urllib.request.urlretrieve(url, raw)
    im = Image.open(raw).convert("RGBA")
    # Figma chrome: topbar 56 + context switcher 44 at design width 402
    chrome = int(round(im.width * 100 / 402))
    cropped = im.crop((0, chrome, im.width, im.height))
    png = out_android / f"{name}.png"
    cropped.save(png, "PNG")
    raw.unlink(missing_ok=True)
    print(" ", im.size, "->", cropped.size, png.stat().st_size)
    iset = out_ios / f"{name}.imageset"
    iset.mkdir(exist_ok=True)
    cropped.save(iset / "img.png", "PNG")
    (iset / "Contents.json").write_text(contents, encoding="utf-8")

print("done")
