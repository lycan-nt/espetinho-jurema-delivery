"""Gera favicons circulares a partir de public/assets/logo-alfatec.jpeg."""
from __future__ import annotations

import base64
from io import BytesIO
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
SRC = ROOT / "public" / "assets" / "logo-alfatec.jpeg"
OUT = ROOT / "public"


def square_crop(img: Image.Image) -> Image.Image:
    w, h = img.size
    side = min(w, h)
    left = (w - side) // 2
    top = (h - side) // 2
    return img.crop((left, top, left + side, top + side))


def round_icon(src: Image.Image, px: int) -> Image.Image:
    img = src.resize((px, px), Image.Resampling.LANCZOS)
    mask = Image.new("L", (px, px), 0)
    ImageDraw.Draw(mask).ellipse((0, 0, px, px), fill=255)
    out = Image.new("RGBA", (px, px), (0, 0, 0, 0))
    out.paste(img, (0, 0), mask)
    return out


def main() -> None:
    base = square_crop(Image.open(SRC).convert("RGBA"))
    icons = {s: round_icon(base, s) for s in (16, 32, 48, 180)}

    icons[32].save(OUT / "favicon-32.png", optimize=True)
    icons[16].save(OUT / "favicon-16.png", optimize=True)
    icons[180].save(OUT / "apple-touch-icon.png", optimize=True)
    icons[16].save(
        OUT / "favicon.ico",
        format="ICO",
        sizes=[(16, 16), (32, 32), (48, 48)],
        append_images=[icons[32], icons[48]],
    )

    buf = BytesIO()
    icons[32].save(buf, format="PNG")
    b64 = base64.b64encode(buf.getvalue()).decode("ascii")
    svg = (
        '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32">\n'
        f'  <image href="data:image/png;base64,{b64}" width="32" height="32"/>\n'
        "</svg>\n"
    )
    (OUT / "favicon.svg").write_text(svg, encoding="utf-8")
    print("Favicons gerados em", OUT)


if __name__ == "__main__":
    main()
