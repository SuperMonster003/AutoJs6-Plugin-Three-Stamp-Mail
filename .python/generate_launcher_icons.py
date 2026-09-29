"""Generate separate transparent UI icons and adaptive/legacy launcher icons.

The retained source geometry is the artwork. Colors and output geometry are generated,
never inferred from antialiased source RGB. Run with --check to verify without writes.
Dark is the default launcher mode. Explicit light and best-effort automatic modes
have independent resources; transparent launcher icons follow the launcher configuration; brand UI assets stay separate.
"""

from __future__ import annotations

import argparse
import io
import math
from pathlib import Path

from PIL import Image, ImageDraw
import generate_brand_icons as brand

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app/src/main/res"
SIZE = 432
SCALE = 4
UI_GLYPH = 0.66
ADAPTIVE_GLYPH = 0.44
DAY_GLYPH = (0x27, 0x27, 0x27)
NIGHT_GLYPH = (0xD8, 0xD8, 0xD8)
NIGHT_BACKGROUND = (0x21, 0x21, 0x21, 255)
DAY_BACKGROUND = (0xFA, 0xFA, 0xFA, 255)


def draw_glyph(draw: ImageDraw.ImageDraw, center: tuple[float, float], box: float, color: tuple[int, int, int, int]) -> None:
    cx, cy = center
    width = box
    height = box * 0.72
    left, right = cx - width / 2, cx + width / 2
    top, bottom = cy - height / 2, cy + height / 2
    stroke = int(round(box * 0.075))
    radius = box * 0.09

    # Envelope body.
    draw.rounded_rectangle((left, top, right, bottom), radius=radius, outline=color, width=stroke)

    # Open flap: two strokes meeting slightly below the vertical center.
    apex = (cx, cy + height * 0.08)
    inset = stroke * 0.55
    draw.line([(left + inset, top + inset), apex], fill=color, width=stroke)
    draw.line([(right - inset, top + inset), apex], fill=color, width=stroke)
    dot_radius = stroke * 0.5
    draw.ellipse((apex[0] - dot_radius, apex[1] - dot_radius, apex[0] + dot_radius, apex[1] + dot_radius), fill=color)

    # Lower folds: from the bottom corners toward the flap apex, stopping short of it.
    fold_end_y = cy + height * 0.16
    draw.line([(left + inset, bottom - inset), (cx - width * 0.16, fold_end_y)], fill=color, width=stroke)
    draw.line([(right - inset, bottom - inset), (cx + width * 0.16, fold_end_y)], fill=color, width=stroke)


def source_alpha() -> Image.Image:
    canvas = Image.new("RGBA", (1728, 1728), (0, 0, 0, 0))
    draw_glyph(ImageDraw.Draw(canvas), (864, 864), 1400, (0, 0, 0, 255))
    alpha = canvas.getchannel("A")
    bounds = alpha.getbbox()
    if bounds is None:
        raise ValueError("Icon source has no visible artwork")
    alpha = alpha.crop(bounds)
    radius = 0.5 * ADAPTIVE_GLYPH * 108 * math.hypot(1, alpha.height / alpha.width)
    if radius >= 33:
        raise ValueError(f"Adaptive artwork exceeds the 66 dp safe circle: radius {radius:.2f} dp")
    return alpha


def render(alpha: Image.Image, ratio: float, color: tuple[int, int, int], background=None) -> Image.Image:
    size = SIZE * SCALE
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    if background is not None:
        ImageDraw.Draw(canvas).ellipse((0, 0, size - 1, size - 1), fill=background)
    width = round(size * ratio)
    height = max(1, round(width * alpha.height / alpha.width))
    scaled_alpha = alpha.resize((width, height), Image.Resampling.LANCZOS)
    glyph = Image.new("RGBA", (width, height), (*color, 255))
    glyph.putalpha(scaled_alpha)
    canvas.alpha_composite(glyph, ((size - width) // 2, (size - height) // 2))
    if background is not None:
        return canvas.resize((SIZE, SIZE), Image.Resampling.LANCZOS)
    # Resize only alpha for transparent artwork: premultiplied RGBA resampling can
    # change foreground RGB by one level, including at fully opaque pixels.
    result = Image.new("RGBA", (SIZE, SIZE), (*color, 255))
    result.putalpha(canvas.getchannel("A").resize((SIZE, SIZE), Image.Resampling.LANCZOS))
    return result


def generated_files() -> dict[Path, bytes]:
    alpha = source_alpha()
    images = {
        "mipmap/ic_launcher_transparent.png": render(alpha, UI_GLYPH, DAY_GLYPH),
        "mipmap-night/ic_launcher_transparent.png": render(alpha, UI_GLYPH, NIGHT_GLYPH),
        "mipmap/ic_launcher_system.png": render(alpha, UI_GLYPH, NIGHT_GLYPH, NIGHT_BACKGROUND),
        "mipmap/ic_launcher_system_foreground.png": render(alpha, ADAPTIVE_GLYPH, NIGHT_GLYPH),
        "mipmap/ic_launcher_system_light.png": render(alpha, UI_GLYPH, DAY_GLYPH, DAY_BACKGROUND),
        "mipmap/ic_launcher_system_light_foreground.png": render(alpha, ADAPTIVE_GLYPH, DAY_GLYPH),
        "mipmap/ic_launcher_system_monochrome.png": render(alpha, ADAPTIVE_GLYPH, (0, 0, 0)),
    }
    for directory, background in (("mipmap", brand.DAY_BACKGROUND), ("mipmap-night", brand.NIGHT_BACKGROUND)):
        images[f"{directory}/ic_launcher.png"] = brand.render(brand.SIZE, background, "rounded", brand.GLYPH_WHITE, 0.68)
        images[f"{directory}/ic_launcher_round.png"] = brand.render(brand.ROUND_SIZE, background, "circle", brand.GLYPH_WHITE, 0.62)
        images[f"{directory}/ic_launcher_foreground.png"] = brand.render(brand.SIZE, None, None, brand.GLYPH_WHITE, 0.54)
        images[f"{directory}/ic_launcher_monochrome.png"] = brand.render(brand.SIZE, None, None, brand.GLYPH_BLACK, 0.54)
    result = {}
    for name, image in images.items():
        output = io.BytesIO()
        image.save(output, format="PNG", optimize=True)
        result[RES / name] = output.getvalue()
    def adaptive(foreground: str, background: str) -> bytes:
        return f'''<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/{background}"/>
    <foreground android:drawable="@mipmap/{foreground}"/>
    <monochrome android:drawable="@mipmap/ic_launcher_system_monochrome"/>
</adaptive-icon>
'''.encode("utf-8")
    for name, background in (("ic_launcher_system", "ic_launcher_system_background"), ("ic_launcher_system_light", "ic_launcher_system_background_light")):
        result[RES / "mipmap-anydpi-v26" / f"{name}.xml"] = adaptive(f"{name}_foreground", background)
    for name, color in (("ic_launcher_system_background", "#212121"), ("ic_launcher_system_background_light", "#FAFAFA")):
        result[RES / "values" / f"{name}.xml"] = (
            '<?xml version="1.0" encoding="utf-8"?>\n<resources>\n'
            f'    <color name="{name}">{color}</color>\n</resources>\n'
        ).encode("utf-8")
    # Keep Auto's own resource ID in the compiled manifest. Resource aliases in
    # values/ are eagerly resolved by PackageManager and freeze one theme's ID.
    for qualifier, target, background in (("", "ic_launcher_system", "ic_launcher_system_background"), ("-notnight", "ic_launcher_system_light", "ic_launcher_system_background_light")):
        result[RES / f"mipmap{qualifier}" / "ic_launcher_system_auto.xml"] = (
            '<?xml version="1.0" encoding="utf-8"?>\n'
            f'<bitmap xmlns:android="http://schemas.android.com/apk/res/android" android:src="@mipmap/{target}"/>\n'
        ).encode("utf-8")
        result[RES / f"mipmap{qualifier}-anydpi-v26" / "ic_launcher_system_auto.xml"] = adaptive(f"{target}_foreground", background)
    return result


def obsolete_files() -> list[Path]:
    # The pre-existing brand assets belong to application/README/About, independently
    # of the selectable launcher aliases, and must remain intact.
    return [path for path in (RES / "values/ic_launcher_system_auto.xml", RES / "values-notnight/ic_launcher_system_auto.xml") if path.is_file()]


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="Check all generated resources without changing files")
    options = parser.parse_args()
    outputs = generated_files()
    stale = [path for path, expected in outputs.items() if not path.is_file() or path.read_bytes() != expected]
    obsolete = obsolete_files()
    if options.check:
        if stale or obsolete:
            raise SystemExit("Stale icon resources: " + ", ".join(str(p.relative_to(ROOT)) for p in stale + obsolete))
        print(f"Verified {len(outputs)} icon resources")
        return
    for path in obsolete:
        if not path.resolve().is_relative_to(RES.resolve()):
            raise ValueError("Icon output escaped resource directory")
        path.unlink()
    for path, data in outputs.items():
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(data)
        print(f"Generated {path.relative_to(ROOT).as_posix()}")


if __name__ == "__main__":
    main()
