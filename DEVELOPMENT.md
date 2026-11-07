# Development

Use a **Java 21 JDK** and the committed Gradle wrapper. Target versions are pinned in `gradle.properties`: Minecraft 1.21.1, NeoForge 21.1.265, and ModDevGradle in `build.gradle`.

```sh
python tools/check_repository.py
./gradlew build runGameTestServer --no-daemon
```

On Windows use `.\gradlew.bat` in place of `./gradlew`. The first build downloads pinned tooling and Minecraft development artifacts. Python is required only for repository/branding helpers and optional compatibility downloads, not to run the mod.

The installable artifact is `build/libs/akis-islands-mod-1.21.1-0.5.0-alpha.jar`. GitHub releases attach only that runtime jar; GitHub supplies source archives automatically. No third-party compatibility jars, validation logs, or checksum sidecars are release assets.

## Project layout

| Path | Purpose |
| --- | --- |
| `src/main/java/dev/aki/islands/world/` | Shape, climate, codecs, and noise-generator integration |
| `src/main/java/dev/aki/islands/client/` | Native World Type preset editor and previews |
| `src/main/java/dev/aki/islands/testing/` | Development-only GameTests; excluded from runtime jars |
| `src/main/resources/` | World preset, translations, and supplied icons |
| `src/main/templates/` | Expanded NeoForge mod metadata |
| `src/test/java/` | Unit tests for layout, climate, and settings compatibility |
| `src/gameTest/` | Optional full-chunk test source and latitude-enabled preset |
| `scripts/fetch_compat_mods.py` | Explicit, hash-verified compatibility downloads |
| `tools/` | Repository checks and exact-copy branding importer |
| `docs/` | Cover, full guide, compatibility scope, and artifact provenance |

## Verification

`build` runs the 20 unit tests. `runGameTestServer` runs three in-game tests covering generator serialization, old-save defaults, native terrain and humidity, climate direction, actual biome selection, known external-controller delegation, and preview-ocean boundary enforcement against independent terrain branches. Check the log for `All 3 required tests passed`; some loader failures can end a development run without a failing process exit code.

`-PfullWorldgenTest` enables the optional full-chunk source and test-only World Type override, adding a fourth GameTest. Use isolated profiles and follow [the full guide](docs/GUIDE.md). The test classes, empty test template, and test-only flat preset never enter the distributable jar. After a full-generation run, close clients and use `./gradlew clean build` to restore ordinary development resources.

The compatibility record distinguishes native biome/terrain checks from unverified structure placement, long routes, and UI behavior. GameTest world options disable normal structure starts. See [docs/COMPATIBILITY.md](docs/COMPATIBILITY.md).

## Packaging and branding

`processResources` includes the project license/credits and retained NeoForged MDK notice. `logoFile` points to `icon.png`. Run `python tools/branding.py --source path/to/Akis-Islands.png` to synchronize the README cover and packaged icons byte for byte; this helper does not resize or re-encode the artwork.

Do not rename the `akis_islands` mod ID, generator codec IDs, or old settings formats without migration support. Missing latitude settings must continue to mean disabled. Keep climate configuration per world and preserve deterministic shapes and the active biome selector.

GitHub Actions repeats repository checks, a Java 21 build/unit-test run, and the vanilla GameTests on pushes and pull requests. CI does not download the optional compatibility modpack or establish universal compatibility.
