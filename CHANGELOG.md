# Changelog

## 0.5.0-alpha — 2026-11-07

- Expanded Normal middle from 50–90% to 10–90%, allowing a much narrower temperate center.
- Added Wide extremes: 25% middle, 90% band strength, and 75% transition length, giving strong cold/hot temperatures more room before the coast.
- Updated climate tooltips and documentation to explain middle width, earlier transitions, and biome coverage.
- Preserved default values, existing presets, saved climate curves, and Natural Temperature delegation.

## 0.4.1-alpha — 2026-11-02

- Enforced the preview's ocean boundary after final terrain density evaluation, suppressing unplanned above-water noise terrain even when a pack adds terrain independently of continentalness.
- Preserved inland terrain, negative density, deeper seabed variation, saved shape choices, climate settings, and the normal feature/structure stages.
- Added boundary regressions for all four layouts, Mixed continents with five landmasses at radius 5,000, an independent terrain branch, and complete modded chunks around the reported outskirts.
- Applied the correction to newly generated chunks; existing saved terrain is not erased.

## 0.4.0-alpha — 2026-11-01

- Added Mixed continents: 2–6 large landmasses with randomized satellite islands, occasional land bridges, coves, and peninsulas.
- Added Default, Balanced (roughly 50% moderate / 25% cold / 25% hot), Temperate, and Gentle climate presets.
- Added Reset to default for climate values, preserving the enabled/disabled choice.
- Added smoother quintic easing for new climate settings, while retaining the original curve when loading 0.3 saves.
- Kept the existing three layouts' generation and the native biome, terrain, and structure stages unchanged.

## 0.3.0-alpha — 2026-11-01

- Added opt-in north-to-south latitude climate, with a broad temperate middle and gradual cold/hot ends.
- Added adjustable middle coverage, band strength, and transition length, with per-world persistence.
- Retained native humidity, biome selection, terrain generation, and underground climate at/below Y=0.
- Deferred automatically to Natural Temperature when installed, without changing another mod's configuration.
- Added matching cover art, packaged icons, credits, repository documentation, and the first GitHub alpha release.

## 0.2.0-alpha — 2026-11-01

- Moved customization into the native World Type panel.
- Simplified controls to layout, radius, landmass count, and Randomize Shape.
- Removed the custom ocean-floor cap and separate climate controls for new format-2 layouts.
- Retained original settings when loading older worlds.

## 0.1.0-alpha — 2026-11-01

- Added the initial single-continent, connected-continent, and island-group generator with a coastline preview.
- Added generator serialization and initial vanilla/modded world-generation checks.

Earlier versions were development builds. The 0.3.0 alpha is the first GitHub release.
