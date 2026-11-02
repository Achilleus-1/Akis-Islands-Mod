# Aki's Islands guide

An alpha world-generation mod for **Minecraft 1.21.1 / NeoForge 21.1.265 / Java 21**. Plan one continent, several connected continents, an island group, or mixed continents, surrounded by an ocean that continues to Minecraft's normal world limits.

NeoForge 21.2 targets Minecraft 1.21.2. This project uses **21.1.265**, the matching build for the requested Minecraft version.

## Try it

1. Build with `./gradlew build` (`.\gradlew.bat build` in PowerShell), using Java 21.
2. Install `build/libs/akis-islands-mod-1.21.1-0.4.1-alpha.jar` in the client and server `mods` folders, replacing the previous version.
3. Open **Singleplayer → Create New World → World** and select **World Type: Aki's Islands**.
4. Use the native **Customize** button beside World Type. Choose **One continent**, **Attached continents**, **Islands**, or **Mixed continents**, then set size/count or click **Randomize Shape**.
5. Choose **Apply layout** and create the world. **Cancel** leaves the applied layout unchanged. Select Minecraft's ordinary world type to use ordinary world generation.

The editor belongs to the World Type panel; no extra controls are added to the Game or More tabs. Selecting another world type afterward replaces the applied layout. The preset uses the active pack's `minecraft:overworld` noise settings and biome preset. Packs with a separate custom world type may need a preset adapter.

## Settings and preview

| Setting | Meaning |
| --- | --- |
| Layout | One continent, attached continents, islands, or mixed continents. Mixed continents add randomized satellites, occasional land bridges, coves, and peninsulas around 2–6 large landmasses. |
| Radius | Overall layout scale, 1,000–100,000 blocks. This is approximate: irregular coastlines and blending can extend farther. |
| Landmasses | 2–6; ignored for a single continent. |
| Randomize Shape | Generate another coastline while keeping the layout, radius, and count. Minecraft's world seed stays unchanged. |
| Climate | Optional cold north, broad temperate middle, and hot south. Opens a separate editor. Disabled by default. |

The preview uses the same mathematical land mask as generation. It is a **coastline plan**, not a rendered biome, height, or structure map. North is -Z, and the cross marks 0,0. Rivers, lakes, mountains, modded features, and the final shoreline depend on the installed generators. A connected plan can still contain river crossings.

The origin lies inside the central planned landmass. Actual player spawn selection still uses Minecraft's and the pack's normal spawn rules.

Settings and the chosen random shape are saved with that world's chunk generator. Changing another world's settings does not change an existing coastline. Retain this mod when loading its worlds: their generator codec needs it.

Version 0.1.0 saves retain their original generation behavior, including previously saved advanced settings, so upgrading does not introduce seams. Newly created format-2 layouts use the simplified behavior. The legacy controls are not exposed in the editor.

## Compatibility design and limits

New layouts change **continentalness**, the large-scale land/ocean signal. Minecraft and the installed mods determine the terrain heights, ocean floor, coast surfaces, mountains, rivers, caves, ores, biomes, and structures using their normal generation rules. Climate stays native unless the optional latitude system is enabled.

Version 0.4.1 also enforces the coastline plan at the end of terrain density evaluation. This catches terrain branches that can add land independently of continentalness. The safeguard removes positive terrain density near the ocean surface outside the planned shore, easing in over the top 16 blocks below sea level. Inland density, existing negative density (water/air/caves), and all density more than 16 blocks below sea level remain unchanged. The native seabed keeps its deeper variation, and no ocean-depth control is added. Original format-1 worlds retain their original legacy generation.

This correction applies to newly generated chunks in worlds using the current generator. Already generated unexpected land remains stored in the save. The update does not delete chunks or retrofit existing terrain. Use a new test world to check the corrected boundary across the entire layout.

The generator extends Minecraft's `NoiseBasedChunkGenerator` and keeps the active **biome source, height range, surface rules, carvers, aquifers, ores, features, sea level, and structure stages**. It adapts continentalness references inside the pack's terrain splines as well as its biome router. It does not ship an overriding `minecraft:overworld` noise-settings JSON. There are no mixins; one access transformer exposes the standard shifted-noise node for inspection.

The editor accepts the standard noise generator and this mod's generator. It declines other generator classes so their custom behavior is not silently discarded. Completely independent generators need explicit adapters. Custom density functions with unusual traversal or equality behavior may also need adapters.

**Universal compatibility is not established.** Mods can replace generators, override climate decisions, place structures without biome restrictions, or add terrain after noise generation. Preserving those stages also allows their intended bridges, platforms, icebergs, and structures to appear above water. A strict prohibition on every above-water block outside the continent would conflict with that behavior.

See [the compatibility test record](COMPATIBILITY.md) for exact versions and verification scope. These checks do not certify every village, railway, river, biome, seed, or interaction in an arbitrary pack. Structure mods retain their own biome tags, spacing, dependencies, and placement rules.

## Optional latitude climate

Open **Customize → Climate**, enable **North to South**, adjust the controls, and choose **Done**. Then choose **Apply layout** on the continent screen. Canceling either editor discards its uncommitted changes.

| Control | Default | Range and effect |
| --- | --- | --- |
| Normal middle | 65% | 50-90% of the planned north-south span. A broad temperate center around Z=0, larger than either extreme. |
| Band strength | 85% | 0-100%. Blends between native temperature and the latitude profile, retaining local variation. Zero is exactly native temperature. |
| Transition length | 100% | 50-200%. Controls how far beyond the normal middle the gradual change to cold/hot takes. It does not shrink the middle. |

Click **Preset** to cycle through the starting values below. You can then adjust any slider; the label becomes **Custom** when the values no longer match a preset. **Reset to default** restores the default values. Both controls keep climate on/off unchanged, work while climate is off, and remain unavailable when Natural Temperature owns climate. Choose **Done** to keep your edits or **Cancel** to discard them.

| Preset | Moderate middle | Approximate cold / hot span | Band strength | Transition length |
| --- | --- | --- | --- | --- |
| Default | 65% | 17.5% / 17.5% | 85% | 100% |
| Balanced | 50% | 25% / 25% | 85% | 100% |
| Temperate | 80% | 10% / 10% | 85% | 100% |
| Gentle | 65% | 17.5% / 17.5% | 65% | 150% |

These are planned latitude proportions, not measured biome or land-area percentages. Each hemisphere fits its own coastline extent; asymmetric shapes, gradual transitions, native variation, and biome rules affect the final distribution.

North (-Z) becomes colder and south (+Z) becomes hotter. The middle remains temperate with gentle variation; it is not a constant temperature. Smooth transitions replace hard climate boundaries. The profile fits each hemisphere's actual planned land extent, so thin or rotated island groups still receive useful bands. Longer transitions can place the strongest extremes beyond the shore. The preview colors indicate the planned climate, not exact biomes.

The system changes only the temperature input to the **existing biome selector**. Native humidity remains active, allowing wet and dry variants within each band. Modded biomes keep their authored climate ranges, terrain/depth requirements, regional selection, surface rules, and features. The mod does not classify biomes by name or replace biome IDs after selection. Individual biome coverage still depends on the pack's rules and on how much suitable climate/terrain exists in the finite land area.

Underground temperature is unchanged at and below Y=0, with a smooth fade to surface band strength over Y=0-48. Other dimensions keep their generators. The feature does not introduce a wind/rain-shadow or physical weather simulation.

**Natural Temperature owns climate when installed.** The editor identifies it and disables Aki's latitude controls; generator loading also defers automatically, including for data-pack settings that request bands. Use Natural Temperature's settings, or omit that redundant climate mod to use Aki's adjustable bands. No external config is changed. Other mods that override the final temperature sample or use independent biome selectors may need adapters; universal compatibility is not established.

Latitude settings are saved per world. Missing settings in older saves mean **disabled**, so upgrading does not turn this feature on in existing worlds. As with other worldgen mods, adding/removing a climate controller in an existing save can change newly generated biomes.

New climate settings use quintic easing: both the rate of temperature change and its rate of acceleration ease to zero at the joins. This softens the transition into and out of the bands; biome selection still follows Minecraft's and your mods' rules. Version 0.3 saves keep their original cubic curve. Editing a climate value, choosing a preset, or resetting values opts that edited configuration into the new easing. Merely loading a save does not change its curve. Existing three-layout coastlines remain unchanged.

The Nether, End, and other dimensions retain their selected generators. This mod controls the Overworld's land footprint; it does not change the world border or Minecraft's coordinate limits.

## Dedicated servers and data packs

For default settings in a **new** world, set:

```properties
level-type=akis_islands:continents
```

For custom server settings, create the world in the client editor and copy its save to the server, or provide a data pack overriding `data/akis_islands/worldgen/world_preset/continents.json`. The Overworld generator accepts:

```json
{
  "type": "akis_islands:continents",
  "layout_version": 2,
  "settings": "minecraft:overworld",
  "biome_source": {
    "type": "minecraft:multi_noise",
    "preset": "minecraft:overworld"
  },
  "islands": {
    "format": 2,
    "layout": "CONNECTED",
    "radius": 8000,
    "count": 3
  },
  "latitude_climate": {
    "enabled": true,
    "temperate_percent": 65,
    "strength_percent": 85,
    "transition_percent": 100,
    "smooth_transitions": true
  }
}
```

`layout` accepts `SINGLE`, `CONNECTED`, `ARCHIPELAGO`, or `MIXED`. Mixed layout's count controls its large landmasses; satellite count is randomized. Use **Randomize Shape** in the editor to choose another shape; its internal random state is persisted automatically. Include `smooth_transitions: true` for the newer easing in data packs; omitting it retains the 0.3 curve for save compatibility.

Server `generator-settings` is not wired to these continent controls. Use the world editor or a world-preset data pack. Adding the mod to an existing ordinary save does not convert that save's generator.

## Development checks

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
.\gradlew.bat build
.\gradlew.bat runClient
.\gradlew.bat runGameTestServer
```

Unit tests cover shape determinism, bounded oceans, connected land plans, settings compatibility, latitude direction, broad middle coverage, continuous transitions, underground preservation, and all climate controls. The lightweight GameTests check generator codec round trips, old-save defaults, retained biome/surface settings, native ocean density, real biome selection, unchanged terrain/humidity, and external climate-controller delegation.

To download the tested public mods and required dependencies into an isolated development folder:

```powershell
python scripts\fetch_compat_mods.py --locked
.\gradlew.bat runGameTestServer -PgameTestDir=run-compat
```

Downloads use Modrinth's API, verify SHA-512 hashes, and record exact artifacts in `run-compat/downloads.json`. They do not change an existing Minecraft installation or enter the distributable jar. Integrated Villages' extra required dependencies are explicitly included because its Modrinth metadata did not list all dependencies declared inside its jar.

Full chunk checks use `-PfullWorldgenTest`, which adds test-only sources and a latitude-enabled Overworld preset. Railways Untold 1.2.2 unconditionally swaps a GameTest server's generator for its own terrain harness. Natural Temperature owns climate while installed. To test Aki's active climate with the remaining seven requested mods, use a separate profile:

```powershell
$destination = Join-Path $PWD 'run-climate-terrain-v3\mods'
New-Item -ItemType Directory -Path $destination -Force
Get-ChildItem run-compat\mods\*.jar |
    Where-Object { $_.Name -notlike 'railwaysuntold-*' -and $_.Name -notlike 'naturaltemperature-*' } |
    Copy-Item -Destination $destination
.\gradlew.bat runGameTestServer -PgameTestDir=run-climate-terrain-v3 -PfullWorldgenTest
```

Close development clients before cleaning the build on Windows. After a full-generation test run, use `./gradlew clean build` to return the development resources to their normal state. The jar task always excludes the test classes, empty test structure, and test-only flat-preset override.

Built from the [official NeoForge 1.21.1 ModDevGradle starter](https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle). See also [NeoForge documentation](https://docs.neoforged.net/docs/1.21.1/) and the [official version repository](https://maven.neoforged.net/releases/net/neoforged/neoforge/).
