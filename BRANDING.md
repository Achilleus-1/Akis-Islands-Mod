# Aki's Islands branding

**[Product of Achilleus](https://linktr.ee/achilleus_)**.

Artwork supplied by Achilleus as `Akis-Islands.png`. The complete original composition, PNG data, and any transparency are preserved byte for byte.

| Asset | Use |
| --- | --- |
| [Cover](docs/cover.png) | 1024 × 1024 cover displayed in the README |
| [Mod icon](src/main/resources/icon.png) | The same artwork in NeoForge's Mods listing |
| [Pack icon](src/main/resources/pack.png) | Matching packaged resource icon |
| [Artwork importer](tools/branding.py) | Copies the supplied cover into all three locations without re-encoding |

Run `python tools/branding.py --source path/to/Akis-Islands.png` to import replacement artwork, or `python tools/branding.py` to synchronize packaged icons from the committed cover. No cropping, recoloring, background, or additional text is introduced.

The image is promotional artwork. No gameplay footage is fabricated. See [LEGAL.md](LEGAL.md) for ownership and attribution. GitHub's social-sharing preview is a separate repository setting; the README cover and mod icon do not configure it automatically.
