# WIP worldgen compatibility audit

**2026-11-13 · Aki's Islands 0.5.1-alpha · Minecraft 1.21.1 · NeoForge 21.1.265 · Java 21.**

The installed profile contained 314 active jars. Its generation resources, generation hooks, structure providers, relevant optimizers, and dependencies were inspected. An isolated normal dedicated server loaded 103 of those jars together with the development build. The original profile, configurations, and saves were not modified. Disabled files were excluded.

The [artifact manifest](wip-worldgen-mods.json) records the exact 103 jars, mod IDs, versions, SHA-512 hashes, release-note links, and test settings. Published release notes were reviewed for 81 of these artifacts, including 80 exact hash-matched Modrinth releases and Structure Essentials' CurseForge release. Local compatibility patches without published notes were inspected as installed artifacts. The remaining entries are supporting libraries without a reviewed release-note record. Third-party jars and configuration files are not distributed here.

## Compatibility corrections

**Hybrid Aquatic 1.7.4 trenches.** Its startup integration requires a mutable noise-settings reference and serializes the density graph to inspect the active terrain. Earlier Aki builds supplied a direct settings value and runtime wrappers without usable codecs. Hybrid Aquatic logged that trenches would not be carved. Aki now supplies a private, world-local reference and registered density codecs. The shared settings registry is not rebound. The test confirms Hybrid Aquatic appends its carver to the island terrain, and the normal server reports direct trench carving. The generator still persists its original base-settings reference and per-world options. [Installed release notes](https://modrinth.com/mod/hybrid-aquatic/version/jdOU7Pzu).

**Spawn 4.0.8 independent islands.** Spawn creates some islands as structures after noise generation. The baseline server produced 87 above-water columns at the ocean sample around (-6000, 6000), with a `spawn:sandy_island` structure reference and sand, grass, stone, and terracotta blocks. Aki's terrain density there was already ocean. The new structure policy prevents these independent landmasses from starting in planned ocean and checks their generated bounding footprint against the land plan at eight-block intervals, including the edges. A fresh world using the same settings has zero above-water land columns at that sample.

The default `akis_islands:landmass_structures` tag includes Spawn's cold, Dodo, sandy, tropical, and volcanic islands. They are eligible only where the planned land area can contain them. Their usual requirement for ocean biomes may leave few or no eligible locations in some layouts. Spawn's ordinary ocean structures, other mods' buildings, villages, ruins, and native icebergs are not in this tag. A data pack can extend the tag for another mod's land-creating structures. This policy applies only to the Aki continent generator. [Installed Spawn release notes](https://modrinth.com/mod/spawn/version/OgmUYgeL).

Existing saved islands remain in their chunks. These corrections affect newly generated terrain and structure starts.

## Streams Reflowing update review

The installed artifact is **2.17.3**, compared with **2.16.2** in the earlier compatibility profile. Its cumulative changelog describes terrain-fit controls, adaptive terrain reads, independent watershed sizing, and legacy shaping. The newer entries change stream/lake/sea banks, stream mouths, and dimension selection. In 2.17.3, `streamDimensions` defaults to the Overworld, stream ends stop closer to sea/lake shores, and End/Nether generation stalls are fixed. [Installed release notes](https://modrinth.com/mod/streams-reflowing/version/ktZSuv7E).

The normal-server test uses the profile's copied Streams configuration, including:

| Setting | Value |
| --- | --- |
| Terrain accuracy | `MEDIUM` |
| Watershed size | `BLOCKS_768` |
| Terrain fit | `-1`, following accuracy |
| Adaptive grid read | `true` |
| Fast terrain estimate preference | `true` |
| Legacy stream shaping | `false` |
| Stream dimensions | `minecraft:overworld` |
| Generate streams / lakes | Both enabled |

Streams reports **fast terrain mode OFF** for this combined generator even with the fast-estimate preference enabled. That is the observed runtime choice, not a configuration override by Aki. Its planner completes populated inland regions and empty ocean regions, and its chunk-generation stages finish in the test. No forced fast-estimator adapter was added. Changes to a world's stored watershed/stream data follow Streams' own migration rules.

All six terrain-accuracy choices, every terrain-fit combination, long river mouths, current direction, boats, and visual bank quality have not been exhaustively checked. The current evidence covers the installed profile's configuration.

## Installed generation stack

All rows below loaded together in the isolated server. Full chunks exercise the combined pipeline; this does not mean every feature or structure variant was located individually. The manifest includes the full dependency list and smaller feature/resource providers.

| Area | Installed providers |
| --- | --- |
| Overworld terrain and biomes | Terralith 2.6.2, Biolith 3.0.14, Biomes O' Plenty 21.1.0.14, TerraBlender 4.1.0.8, Lithostitched 1.8.1, More Density Functions 2.3.0 |
| Ocean, cave, and additional terrain | Streams Reflowing 2.17.3, Hybrid Aquatic 1.7.4, Spawn 4.0.8, YUNG's Cave Biomes 3.1.1, Quark 4.1-488, VanillaBackport 1.2.2 |
| Village and underwater structures | Integrated Villages 1.3.3, Integrated Stronghold 1.1.4, Integrated API 1.9.2, Underwater Village 1.0.2, Underground Village 2.0, Hopo Better Underwater Ruins 1.2.1b, Chef's Delight 1.0.6 |
| Other structures | Moog's Bountiful 1.0.10, End 2.1.1, Nether 3.1.1, Soaring 2.2.0, Structure Lib 3.4.2; Luminous Overworld 1.5.52, Nether 1.3.4, Beasts 1.2.8; Cataclysm 3.33, Mowzie's Mobs 1.8.2, Wan's Ancient Beasts 1.3.2; other manifest-listed providers |
| Rail generation and pack patches | Railways Untold 1.2.2, Create Infinite Railways 0.2.0, No Airship Villages 1.0.0, Rarer Spawn Islands 1.0.0, Dynamic Village 0.5.1 |
| Other dimensions | Amplified Nether 1.2.16, Nullscape 1.2.14, YUNG's Better End Island 3.1.2, The Aether 1.5.10, Deeper and Darker 1.4.1 |
| Generation and structure optimizers | C2ME 0.4.0-alpha.0.124, zfastnoise 1.0.13, Lithium 0.15.4, ModernFix 5.27.27, ServerCore 1.5.19, Structure Essentials 5.0, Structure Layout Optimizer 1.0.12, Async Locator 1.6.0, Redirected 1.0.0 |
| Features and environmental integrations | Farmer's Delight 1.3.4, Twigs 3.1.2, Upgrade Aquatic 7.0.2, Friends & Foes 5.0.4, Create Ore Excavation 1.6.8, Serene Seasons 10.1.0.9, Immersive Snow 1.4.0, other manifest-listed providers |
| Preview tools | World Preview 1.3.1 and Diorama 0.10.1 load; their client screens were not exercised in this dedicated-server audit |

Regions Unexplored and Natural Temperature are absent from this installed snapshot. Their earlier test results remain in the [historical compatibility record](COMPATIBILITY.md). When Natural Temperature is installed, Aki's optional latitude mode still defers automatically.

Notable reviewed upstream changes include Hybrid Aquatic's Terralith/Tectonic trench fixes, Spawn's biome compatibility and island fixes, Chef's Delight's village pool weights, Moog's Structure Lib's YUNG terrain-blending handling, and zfastnoise's BetterEnd crash fix. More Density Functions links to an incomplete version-history wiki; that page is not evidence of a complete 2.3.0 changelog. Installed artifacts were tested rather than inferred compatible from those notes alone. [More Density Functions history](https://github.com/klinbee/More-Density-Functions/wiki/Version-History), [Structure Essentials 5.0 notes](https://www.curseforge.com/minecraft/mc-mods/structure-essentials-forge-fabric/files/7962596).

## Verification

The normal server uses structures enabled, world seed **0**, shape state **1234567**, **Mixed continents**, radius **5,000**, **five main landmasses**, and latitude enabled at **65% middle / 85% strength / 100% transition**, with smooth easing. A separate fresh world tests the Spawn correction. Railways Untold remains installed because its GameTestServer-only generator replacement does not apply to an ordinary dedicated server.

- All six in-game tests complete without a reported failure. The five lightweight tests cover save compatibility, density inspection/round trips, climate behavior, structure classification and footprint rules, and 13,579 planned-ocean columns across all four layouts. The sixth uses the live Overworld and completed chunks.
- The retained biome source exposes Minecraft, Terralith, Spawn, TerraBlender, Hybrid Aquatic, Biomes O' Plenty, Quark, and YUNG's Cave Biomes namespaces. Climate queries select cold northern and warm southern biomes through that source.
- The completed inland sample at (-256, 256) has a raw height of 149. The distant sample at (8192, 8192) is water. Around (4700, -5000), (6000, -6000), (-6000, -6000), and (-6000, 6000), both raw and completed terrain checks find zero unplanned land columns. Four packed-ice columns in the frozen northern ocean are a native iceberg and remain allowed.
- Normal generation locates Integrated Villages' Minka village at (848, 1296), cabin village at (-3744, -2000), and Underwater Village's temple at (-1792, -1072). Completed saved chunks contain valid starts with 96, 21, and one structure pieces respectively, plus placed building blocks. These are placement checks for these examples, not every village variant or its visual appearance.
- Completed chunks at (0, 0) in the Nether, End, Aether, and Otherside finish with their own noise generators. They do not use the continent generator.
- The release build also runs the 20 unit tests and five vanilla GameTests. Test classes and the optional full-world preset are excluded from the runtime jar.
- All five lightweight GameTests also pass in the earlier 16-jar compatibility profile, retaining its Regions Unexplored, Natural Temperature, and Streams Reflowing 2.16.2 coverage.

This is a generation-focused server test, not a full 314-mod client playthrough. Every railway route, structure variant, seed, client preview, and visual biome/surface combination remains outside the completed coverage. Rare biomes can have little or no suitable space in a finite layout. Independent chunk generators or land-placing features outside the tagged structure pipeline may need further adapters.

## Upstream data issues observed

The installed stack also reports unrelated resource problems, including Underground Village's `stoneholm:cleric` loot table using the removed `minecraft:set_nbt` function, Spawn's anthill loot referring to an absent `spawn:roly_poly` item, and Luminous Nether's Supplementaries reverse-ash recipe using an invalid result format. These resources are owned by their providers and are not repaired by this release. The server still completes the generation checks. Optional compatibility resources can also report missing items or conditions when the corresponding content mod is absent from this isolated generation subset.

## Re-running this profile

Use the manifest's exact installed artifacts in an isolated development server and retain their required dependencies. Local pack patches must come from the owner's installation. Copy the relevant generation configuration into the isolated directory, including Streams' bank-style folders. Do not put a second Aki runtime jar alongside the development build.

Enable the test sources with `-PfullWorldgenTest`. Point `neoForge.runs.server.gameDirectory` to the isolated directory and forward `runServer.standardInput = System.in` in a local Gradle init script. Set the server's `level-type=minecraft:flat` so the test-only preset installs the continent generator; keep `generate-structures=true`. Run `runServer -PfullWorldgenTest -I <local-init-script>`, then use `test runall` after startup. Review both test errors and the full-chunk success log. This ordinary-server path retains Railways Untold and structure starts.

After stopping the server, use `clean build` to restore ordinary development resources. The release jar always excludes development tests and the test-only preset.
