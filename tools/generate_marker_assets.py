from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
VENDOR = ROOT / "tools" / "vendor"
if str(VENDOR) not in sys.path:
    sys.path.insert(0, str(VENDOR))

from PIL import Image, ImageDraw, ImageFilter, UnidentifiedImageError


SOURCE_DIR = ROOT / "dashboard" / "assets" / "markers" / "source"
OUTPUT_DIR = ROOT / "dashboard" / "assets" / "markers" / "generated"
ANDROID_OUTPUT_DIR = ROOT / "mobile" / "app" / "src" / "main" / "assets" / "marker_photos" / "generated"
MANIFEST_PATH = OUTPUT_DIR / "manifest.json"
CONTACT_SHEET_PATH = OUTPUT_DIR / "contact-sheet.png"

CANVAS_SIZE = (96, 120)
PHOTO_SIZE = 72
PHOTO_TOP = 10
POINTER_WIDTH = 24
POINTER_HEIGHT = 24


def slugify(name: str) -> str:
    base = re.sub(r"\.[^.]+$", "", name).strip().lower()
    base = re.sub(r"[^a-z0-9]+", "-", base).strip("-")
    return base or "marker"


def crop_square(image: Image.Image) -> Image.Image:
    width, height = image.size
    size = min(width, height)
    left = (width - size) // 2
    top = (height - size) // 2
    return image.crop((left, top, left + size, top + size))


def build_photo_circle(image: Image.Image) -> Image.Image:
    photo = crop_square(image).resize((PHOTO_SIZE, PHOTO_SIZE), Image.Resampling.LANCZOS)
    mask = Image.new("L", (PHOTO_SIZE, PHOTO_SIZE), 0)
    ImageDraw.Draw(mask).ellipse((0, 0, PHOTO_SIZE - 1, PHOTO_SIZE - 1), fill=255)
    circle = Image.new("RGBA", (PHOTO_SIZE, PHOTO_SIZE), (0, 0, 0, 0))
    circle.paste(photo, (0, 0), mask)
    return circle


def build_marker_asset(image_path: Path, output_path: Path) -> None:
    source = Image.open(image_path).convert("RGB")
    photo = build_photo_circle(source)

    canvas = Image.new("RGBA", CANVAS_SIZE, (0, 0, 0, 0))
    shadow = Image.new("RGBA", CANVAS_SIZE, (0, 0, 0, 0))
    shadow_draw = ImageDraw.Draw(shadow)

    center_x = CANVAS_SIZE[0] // 2
    circle_bounds = (
        center_x - (PHOTO_SIZE // 2) - 6,
        PHOTO_TOP - 6,
        center_x + (PHOTO_SIZE // 2) + 6,
        PHOTO_TOP + PHOTO_SIZE + 6,
    )
    pointer_top = circle_bounds[3] - 4
    pointer = [
        (center_x, pointer_top + POINTER_HEIGHT),
        (center_x - POINTER_WIDTH // 2, pointer_top),
        (center_x + POINTER_WIDTH // 2, pointer_top),
    ]

    shadow_draw.ellipse(circle_bounds, fill=(18, 47, 41, 80))
    shadow_draw.polygon(pointer, fill=(18, 47, 41, 80))
    shadow = shadow.filter(ImageFilter.GaussianBlur(radius=5))
    canvas.alpha_composite(shadow, dest=(0, 4))

    draw = ImageDraw.Draw(canvas)
    draw.ellipse(circle_bounds, fill=(255, 255, 255, 255), outline=(13, 122, 103, 255), width=4)
    draw.polygon(pointer, fill=(13, 122, 103, 255))
    draw.ellipse(
        (
            center_x - 5,
            pointer_top + POINTER_HEIGHT - 10,
            center_x + 5,
            pointer_top + POINTER_HEIGHT,
        ),
        fill=(255, 255, 255, 255),
    )

    canvas.alpha_composite(photo, dest=(center_x - PHOTO_SIZE // 2, PHOTO_TOP))
    output_path.parent.mkdir(parents=True, exist_ok=True)
    canvas.save(output_path, format="PNG")


def build_contact_sheet(entries: list[dict[str, str]]) -> None:
    if not entries:
        return

    thumbs = [Image.open(Path(entry["generated"])) for entry in entries]
    columns = 4
    rows = (len(thumbs) + columns - 1) // columns
    cell_w, cell_h = 180, 170
    sheet = Image.new("RGBA", (columns * cell_w, rows * cell_h), (246, 251, 249, 255))
    draw = ImageDraw.Draw(sheet)

    for index, (thumb, entry) in enumerate(zip(thumbs, entries)):
        row = index // columns
        col = index % columns
        x = col * cell_w
        y = row * cell_h
        preview = thumb.resize((96, 120), Image.Resampling.LANCZOS)
        sheet.alpha_composite(preview, dest=(x + 42, y + 10))
        label = entry["name"][:26]
        draw.text((x + 12, y + 136), label, fill=(28, 44, 43, 255))

    CONTACT_SHEET_PATH.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(CONTACT_SHEET_PATH, format="PNG")


def main() -> None:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    ANDROID_OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    entries: list[dict[str, str]] = []
    skipped: list[str] = []
    seen: dict[str, int] = {}

    for image_path in sorted(SOURCE_DIR.glob("*.jp*g")):
        slug = slugify(image_path.name)
        seen[slug] = seen.get(slug, 0) + 1
        if seen[slug] > 1:
            slug = f"{slug}-{seen[slug]}"
        output_path = OUTPUT_DIR / f"{slug}.png"
        android_output_path = ANDROID_OUTPUT_DIR / f"{slug}.png"
        try:
            build_marker_asset(image_path, output_path)
        except (UnidentifiedImageError, OSError):
            skipped.append(image_path.name)
            continue
        android_output_path.write_bytes(output_path.read_bytes())
        entries.append(
            {
                "name": image_path.name,
                "slug": slug,
                "source": str(image_path),
                "generated": str(output_path),
                "webPath": f"./assets/markers/generated/{slug}.png",
                "androidAssetPath": f"marker_photos/generated/{slug}.png",
            }
        )

    build_contact_sheet(entries)
    MANIFEST_PATH.write_text(json.dumps(entries, indent=2), encoding="utf-8")
    (ANDROID_OUTPUT_DIR / "manifest.json").write_text(json.dumps(entries, indent=2), encoding="utf-8")
    print(f"Generated {len(entries)} marker assets in {OUTPUT_DIR}")
    if skipped:
        print(f"Skipped {len(skipped)} unreadable files")


if __name__ == "__main__":
    main()
