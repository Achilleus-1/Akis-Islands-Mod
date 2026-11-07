# Compatibility test record

**2026-11-07 — 0.5.0-alpha, Minecraft 1.21.1, NeoForge 21.1.265, Java 21.** This is an alpha test record, not a universal compatibility guarantee.

The 0.5.0 update rechecks unit tests and lightweight vanilla/modded climate checks. The full-chunk terrain checks below were last run for 0.4.1; this update adds an optional climate preset and widens its accepted middle-width range.

The full artifact list, download URLs, and SHA-512 hashes are recorded in [tested-mods.json](tested-mods.json). `python scripts/fetch_compat_mods.py --locked` reproduces those downloads.

| Requested mod | Tested version | All-mod loading, codec and noise-height check | Full-chunk terrain profile |
| --- | --- | --- | --- |
| Terralith | 2.6.2 | Passed | Passed |
| Biolith | 3.0.14 | Passed | Passed |
| Biomes O' Plenty | 21.1.0.14 | Passed | Passed |
| Integrated Villages | 1.3.3+1.21.1-neoforge | Passed | Loaded during check |
| Underwater Village | 1.0.2 | Passed | Loaded during check |
| Streams Reflowing | 2.16.2 | Passed | Passed |
| Railways Untold | 1.2.2 | Passed | See harness limitation below |
| Regions Unexplored | 0.6.3-neoforge-21.1 | Passed | Passed |
| Natural Temperature | 1.2.1 | Passed; Aki's bands defer automatically | Tested as the external climate owner in the all-mod profile |

Required dependencies also loaded: Lithostitched 1.8.1, TerraBlender 4.1.0.8, GlitchCore 2.1.0.2, Integrated API 1.9.1, Create 6.0.10, Supplementaries 4.0.2, and Moonlight 3.7.2. Some mods include additional libraries inside their jars.

## What the checks establish

- Twenty unit tests cover deterministic layouts, finite oceans, connected land plans, settings compatibility, north/south direction, broad middle coverage, smooth and adjustable transitions, underground preservation, local variation, and climate settings persistence. Checks also cover separate large landmasses plus satellite islands, original-layout coastline fingerprints sampled from the published 0.3 jar, old climate curve preservation, smoother joins, presets, reset behavior, and the final ocean-boundary rule. The narrow-middle checks round-trip 10%, 25%, and 49% along with existing values, verify continuity and direction for both saved curve types, and confirm that Wide extremes materially expands strong-temperature coverage.
- A NeoForge GameTest encodes and decodes the actual registered chunk-generator codec, checks old saves whose default settings were omitted, verifies retained biome-source/surface-rule references, and samples terrain heights around the origin and in distant oceans. It also compares ocean density against the active pack's native generator with ocean continentalness, including below sea level, and checks that temperature and humidity remain unchanged.
- A second GameTest enables latitude climate, round-trips its settings, checks missing settings in old saves, compares enabled/disabled terrain density and humidity, checks underground climate preservation, and queries the retained biome source with the new climate. The vanilla profile selected deep frozen ocean north, temperate forests/plains in the middle, and warm ocean south. The modded profile also selected Terralith's deep warm ocean in the south. Gameplay biome temperature is not used as a proxy for the separate placement coordinate.
- The 0.5.0 climate GameTest also round-trips Wide extremes (25% middle / 90% strength / 75% transition), verifies that strong cold/hot temperatures extend inward at 75% latitude, and checks exact preservation of terrain density, humidity, underground temperature, and external-controller delegation. Native biome selection at those sampled latitudes returned snowy plains / warm ocean in vanilla and ice spikes / Terralith deep warm ocean in the active-climate modded profile. These sample IDs do not measure total desert or frozen-biome area. Visual click-through of the editor remains unverified.
- A third GameTest checks the radius-5,000 outskirts across all four layouts, using five main landmasses for Mixed continents. It samples 13,579 planned-ocean columns in the active modded profile near the reported quadrant. It also deliberately adds a positive terrain branch independent of continentalness, confirms that this branch creates land before the safeguard, and checks that the safeguarded real noise-column path stays underwater.
- The full-generation profile installs the continent generator as the real Overworld generator. It locates inland terrain inside the central planned landmass, generates a completed chunk there, checks its retained heightmap, and generates a distant completed chunk to check ocean water. Rivers or lakes may cross the exact origin.
- Full-profile assertions check that Terralith, Biomes O' Plenty, and Regions Unexplored remain in the biome source's available biome namespaces.
- The 0.2.0 native client check confirmed that Game has no floating mod controls, World Type exposes the native Customize button for Aki's Islands, all three layout modes remain available, Randomize Shape changes the preview, and Apply preserves the selected shape when Customize is reopened.
- The 0.3.0 development client launches successfully. The added climate screen has not received a visual click-through: the Windows UI helper's native pipe was unavailable after retries and a kernel reset. Build, model tests, actual biome-selection tests, saved-settings checks, and the active-climate full-chunk profile pass; the new sliders/preview still need a manual visual check.
- The 0.4.0 update passes the vanilla profile's two GameTests, the active-climate seven-mod profile's three GameTests including real inland/ocean chunks, and the nine-mod profile's two GameTests including Natural Temperature delegation. The climate probe now uses Mixed continents and checks the new easing flag through the actual registered generator codec. Nine generated Mixed coastline plans were visually inspected. The editor's new preset/reset controls remain unverified by visual click-through: the Windows helper still reports `failed to connect native pipe` (`os error 2`).
- The 0.4.1 boundary regression uses a fresh modded world with five Mixed main landmasses at radius 5,000. Full chunks around (4700, -5000), (6000, -6000), (-6000, -6000), and (-6000, 6000) contain zero above-water terrain columns in the checked ocean locations. Both raw height queries and completed chunks are checked. These checks include Terralith and Streams Reflowing, along with the other mods in the active-climate profile.

The active climate terrain profile omits Railways Untold (its GameTest harness replaces the generator) and Natural Temperature (it owns the temperature controller). It retains the other seven requested mods and all required dependencies. All four GameTests pass with a real latitude-enabled continent Overworld. The separate nine-mod profile passes all three lightweight GameTests and confirms automatic external-controller delegation.

The 0.4.1 terrain test uses world seed 0, internal shape state 1234567, Mixed continents, a 5,000-block layout radius, five main landmasses, and latitude enabled with a 65% middle, 85% strength, 100% transition length, and smooth easing. The lightweight tests additionally use terrain seed 8675309, the default 8,000-block single continent for native terrain checks, and an 8,000-block Mixed plan for latitude checks. These are representative checks, not an exhaustive seed search. The exact reported save and randomized shape were unavailable; the original test seed did not reproduce the unwanted island before the safeguard. The independent-terrain regression demonstrates the previously unenforced boundary and verifies its correction.

## What remains unverified

**Every individual structure placement and railway/stream route is unverified.** Minecraft's GameTest server disables normal structure starts in its world options. Loading Integrated Villages and Underwater Village during a terrain check proves coexistence/loading, not that every structure type has spawned correctly. Long-distance railway generation, stream mouths at custom shores, giant structures crossing a shore, and every custom surface/biome combination still need normal gameplay testing.

Railways Untold 1.2.2 contains `GameTestTerrainMixin`, which unconditionally replaces a `GameTestServer` generator with its `TestTerrainChunkGenerator`. The combined nine-mod profile can therefore check loading and directly instantiated noise generators, but it cannot exercise the continent generator as its live Overworld generator. The separate active-climate full-chunk profile omits Railways Untold and Natural Temperature while retaining the other seven requested mods and dependencies. This is a test-harness/controller limitation; it is not evidence of an ordinary-world conflict.

Independent chunk generators and unusual custom density-function graphs need adapters. Mods that place terrain, platforms, icebergs, structures, or other blocks after the noise stage can still add above-water blocks outside the planned land boundary. Aki's Islands intentionally retains their generation stages.

Natural Temperature remains responsible for its own climate behavior and configuration while installed. Aki's optional latitude mode automatically defers, including for explicitly enabled data-pack settings. Its UI explains that choice and never writes another mod's config. Without that external controller, only the temperature input is changed; humidity, terrain, cave temperatures at/below Y=0, and the native biome selector are retained. Unknown climate mods or independent selectors may need adapters.

Biome categories and physical climate accuracy are limited by the authors' placement rules. The system is not a rain-shadow/weather simulation and cannot ensure every rare or specially restricted modded biome has suitable territory in every finite layout. Original settings are retained for old saves, and missing optional latitude settings remain disabled.

## Re-run

Follow the commands in [README.md](../README.md). Close development clients before cleaning, and return to a normal `clean build` after using the test-only Overworld preset.

Check the log for **“All N required tests passed”**, as well as Gradle's exit status. Some NeoForge startup errors can stop mod loading without making the Gradle run task fail.
