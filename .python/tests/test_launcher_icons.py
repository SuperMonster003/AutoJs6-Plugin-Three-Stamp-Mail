from pathlib import Path
import importlib.util
import io
import math
import unittest
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location('launcher_icons', ROOT / '.python/generate_launcher_icons.py')
ICONS = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(ICONS)


class LauncherArtworkTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.outputs = ICONS.generated_files()

    def image(self, name):
        return Image.open(io.BytesIO(self.outputs[ICONS.RES / name])).convert('RGBA')

    def test_ui_modes_share_the_source_shape_and_preserve_tonal_folds(self):
        day = self.image('mipmap/ic_launcher.png')
        night = self.image('mipmap-night/ic_launcher.png')
        self.assertEqual(day.getchannel('A').tobytes(), night.getchannel('A').tobytes())
        self.assertEqual((432, 432), day.size)
        for image, light in ((day, True), (night, False)):
            pixels = list(image.get_flattened_data())
            self.assertGreater(sum(pixel[3] == 0 for pixel in pixels), len(pixels) // 2)
            opaque = [pixel[:3] for pixel in pixels if pixel[3] == 255]
            self.assertGreater(len(set(opaque)), 8, 'The original envelope folds must not become a solid rectangle')
            luminance = sum(sum(pixel) / 3 for pixel in opaque) / len(opaque)
            self.assertTrue(luminance < 96 if light else luminance > 160)

    def test_final_adaptive_ink_fits_the_safe_circle_including_antialiasing(self):
        image = self.image('mipmap/ic_launcher_monochrome.png')
        ink = [(x, y) for y in range(image.height) for x in range(image.width) if image.getpixel((x, y))[3]]
        self.assertTrue(ink)
        self.assertLessEqual(max(math.hypot(x + .5 - 216, y + .5 - 216) for x, y in ink), 132)
        self.assertEqual(self.image('mipmap/ic_launcher_system_foreground.png').getchannel('A').tobytes(), image.getchannel('A').tobytes())

    def test_regeneration_is_deterministic_and_has_no_ui_adaptive_override(self):
        self.assertEqual(self.outputs, ICONS.generated_files())
        self.assertFalse(list(ICONS.RES.glob('mipmap*/ic_launcher.xml')))
        self.assertFalse(ICONS.obsolete_files())


if __name__ == '__main__':
    unittest.main()
