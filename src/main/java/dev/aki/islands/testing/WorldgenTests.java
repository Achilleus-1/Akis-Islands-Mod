package dev.aki.islands.testing;

import com.mojang.serialization.JsonOps;
import dev.aki.islands.IslandsMod;
import dev.aki.islands.world.ContinentChunkGenerator;
import dev.aki.islands.world.ContinentSettings;
import dev.aki.islands.world.ContinentShape;
import dev.aki.islands.world.IslandDensity;
import dev.aki.islands.world.LatitudeClimate;
import dev.aki.islands.world.LatitudeClimateSettings;
import dev.aki.islands.world.LatitudeTemperature;
import dev.aki.islands.world.OceanBoundaryDensity;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.Noises;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Development GameTests; NeoForge only discovers these in a development run. */
@GameTestHolder(IslandsMod.ID)
@PrefixGameTestTemplate(false)
public final class WorldgenTests {
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void islandStructuresRespectPlanWithoutBlockingOceanBuildings(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        var structures = registries.registryOrThrow(Registries.STRUCTURE);
        var policy = dev.aki.islands.world.LandmassStructurePolicy.LANDMASSES;
        for (String id : new String[]{"minecraft:shipwreck", "minecraft:ocean_ruin_cold", "minecraft:village_plains"}) {
            var holder = structures.getHolder(net.minecraft.resources.ResourceLocation.parse(id)).orElseThrow();
            helper.assertTrue(!holder.is(policy), "Ordinary structure was incorrectly classified as a landmass: " + id);
        }
        for (String id : new String[]{"cold_island", "dodo_island", "sandy_island", "tropical_island", "volcanic_island"}) {
            structures.getHolder(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("spawn", id))
                    .ifPresent(holder -> helper.assertTrue(holder.is(policy), "Spawn island missing from landmass policy: " + id));
        }
        var preset = registries.registryOrThrow(Registries.WORLD_PRESET)
                .get(net.minecraft.resources.ResourceLocation.withDefaultNamespace("normal"));
        var original = (NoiseBasedChunkGenerator) preset.createWorldDimensions().overworld();
        var generator = new ContinentChunkGenerator(original.getBiomeSource(), original.generatorSettings(),
                new ContinentSettings(ContinentSettings.Layout.MIXED, 5000, 5), 1234567);
        helper.assertTrue(dev.aki.islands.world.LandmassStructurePolicy.fitsPlan(generator,
                new net.minecraft.world.level.levelgen.structure.BoundingBox(-16, 0, -16, 16, 100, 16)), "Central landmass was rejected");
        helper.assertTrue(!dev.aki.islands.world.LandmassStructurePolicy.fitsPlan(generator,
                new net.minecraft.world.level.levelgen.structure.BoundingBox(-6100, 0, 5900, -5900, 100, 6100)), "Exterior island was accepted");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void runtimeTerrainFunctionsSupportInspectionAndRoundTrip(GameTestHelper helper) {
        var ops = RegistryOps.create(JsonOps.INSTANCE, helper.getLevel().registryAccess());
        var shape = new ContinentShape(new ContinentSettings(ContinentSettings.Layout.MIXED, 5000, 5), 1234567);
        DensityFunction[] functions = {
                new IslandDensity(DensityFunctions.constant(.33), shape, IslandDensity.Kind.CONTINENTS, 63, 48),
                new IslandDensity(DensityFunctions.constant(.33), shape, IslandDensity.Kind.FLOOR, 63, 48),
                new IslandDensity(DensityFunctions.constant(.33), shape, IslandDensity.Kind.TEMPERATURE, 63, 48),
                new OceanBoundaryDensity(DensityFunctions.constant(.25), shape, 63),
                new LatitudeTemperature(DensityFunctions.constant(.42), new LatitudeClimate(LatitudeClimateSettings.DEFAULT, shape))
        };
        for (DensityFunction function : functions) {
            var encoded = DensityFunction.HOLDER_HELPER_CODEC.encodeStart(ops, function).getOrThrow();
            var decoded = DensityFunction.HOLDER_HELPER_CODEC.parse(ops, encoded).getOrThrow();
            for (int x : new int[]{-10000, 0, 4700}) for (int z : new int[]{-5000, 0, 10000}) {
                for (int y : new int[]{-32, 0, 48, 64, 128}) {
                    var context = new DensityFunction.SinglePointContext(x, y, z);
                    helper.assertTrue(function.compute(context) == decoded.compute(context), "Density inspection changed terrain or climate");
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 1200)
    public static void previewOceanStaysOceanNearFiveThousandBlockBoundary(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        var preset = registries.registryOrThrow(Registries.WORLD_PRESET)
                .get(net.minecraft.resources.ResourceLocation.withDefaultNamespace("normal"));
        var original = (NoiseBasedChunkGenerator) preset.createWorldDimensions().overworld();
        int samples = 0;
        for (var layout : ContinentSettings.Layout.values()) {
            var settings = new ContinentSettings(layout, 5000, layout == ContinentSettings.Layout.MIXED ? 5 : 3);
            var generator = new ContinentChunkGenerator(original.getBiomeSource(), original.generatorSettings(), settings, 1234567);
            var shape = new dev.aki.islands.world.ContinentShape(settings, 1234567);
            var random = RandomState.create(generator.generatorSettings().value(), registries.lookupOrThrow(Registries.NOISE), 8675309);
            for (int x = 3200; x <= 7000; x += 64) for (int z = -7000; z <= -3200; z += 64) {
                if (shape.landWeight(x, z) != 0) continue;
                for (int y : new int[]{generator.getSeaLevel(), 96, 160}) {
                    double density = random.router().finalDensity().compute(new DensityFunction.SinglePointContext(x, y, z));
                    helper.assertTrue(density <= 0, "Unplanned terrain: " + layout + " at " + x + "," + y + "," + z + " density=" + density);
                }
                samples++;
            }
        }
        // Model a terrain pack that adds a land branch after continentalness has been resolved.
        // Continentalness-only masking cannot suppress this branch; test the real noise-column path too.
        var source = original.generatorSettings().value();
        var r = source.noiseRouter();
        var plateau = DensityFunctions.max(r.finalDensity(), DensityFunctions.yClampedGradient(80, 96, .3, -.3));
        var independent = withRouter(source, new NoiseRouter(r.barrierNoise(), r.fluidLevelFloodednessNoise(),
                r.fluidLevelSpreadNoise(), r.lavaNoise(), r.temperature(), r.vegetation(), r.continents(), r.erosion(),
                r.depth(), r.ridges(), r.initialDensityWithoutJaggedness(), plateau, r.veinToggle(), r.veinRidged(), r.veinGap()));
        var mixed = new ContinentSettings(ContinentSettings.Layout.MIXED, 5000, 5);
        var guarded = new ContinentChunkGenerator(original.getBiomeSource(), net.minecraft.core.Holder.direct(independent), mixed, 1234567);
        var state = RandomState.create(guarded.generatorSettings().value(), registries.lookupOrThrow(Registries.NOISE), 8675309);
        int oceanHeight = guarded.getBaseHeight(4700, -5000, Heightmap.Types.OCEAN_FLOOR_WG, helper.getLevel(), state);
        helper.assertTrue(oceanHeight < guarded.getSeaLevel(), "Independent terrain escaped the boundary in a real noise column: " + oceanHeight);
        var nativeState = RandomState.create(independent, registries.lookupOrThrow(Registries.NOISE), 8675309);
        helper.assertTrue(nativeState.router().finalDensity().compute(new DensityFunction.SinglePointContext(4700, 64, -5000)) > 0,
                "Independent-terrain regression must actually add land before the boundary is applied");
        com.mojang.logging.LogUtils.getLogger().info("Preview ocean boundary: checked {} ocean columns across all layouts", samples);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void generatorPersistenceAndTerrain(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        var preset = registries.registryOrThrow(Registries.WORLD_PRESET)
                .get(net.minecraft.resources.ResourceLocation.withDefaultNamespace("normal"));
        var original = (NoiseBasedChunkGenerator) preset.createWorldDimensions().overworld();
        var generator = new ContinentChunkGenerator(original.getBiomeSource(), original.generatorSettings(), ContinentSettings.DEFAULT, 1234567);
        var ops = RegistryOps.create(JsonOps.INSTANCE, registries);
        var encoded = ChunkGenerator.CODEC.encodeStart(ops, generator).getOrThrow();
        var decoded = (ContinentChunkGenerator) ChunkGenerator.CODEC.parse(ops, encoded).getOrThrow();
        helper.assertTrue(generator.generatorSettings() instanceof net.minecraft.core.Holder.Reference<?>,
                "Startup terrain integrations require a mutable settings reference");
        helper.assertTrue(generator.generatorSettings().value() != original.generatorSettings().value(),
                "Runtime terrain settings must be isolated from the shared registry");
        var another = new ContinentChunkGenerator(original.getBiomeSource(), original.generatorSettings(), ContinentSettings.DEFAULT, 7654321);
        helper.assertTrue(!generator.generatorSettings().equals(another.generatorSettings()), "Runtime settings identities leaked across worlds");
        helper.assertTrue(!decoded.islands().isLegacy(), "New worlds must use native terrain and climate behavior");
        var oldSave = encoded.getAsJsonObject().deepCopy();
        oldSave.remove("islands");
        oldSave.remove("layout_version");
        var oldDecoded = (ContinentChunkGenerator) ChunkGenerator.CODEC.parse(ops, oldSave).getOrThrow();
        helper.assertTrue(oldDecoded.islands().equals(ContinentSettings.LEGACY_DEFAULT), "Old saves with omitted defaults changed generation settings");
        helper.assertTrue(decoded.islands().equals(generator.islands()) && decoded.layoutSeed() == generator.layoutSeed(), "World settings did not persist");
        helper.assertTrue(generator.getBiomeSource() == original.getBiomeSource(), "Biome source was replaced");
        helper.assertTrue(generator.generatorSettings().value().surfaceRule() == original.generatorSettings().value().surfaceRule(), "Surface rules were replaced");
        var random = RandomState.create(decoded.generatorSettings().value(), registries.lookupOrThrow(Registries.NOISE), 8675309);
        var source = original.generatorSettings().value();
        var oceanRouter = source.noiseRouter().mapAll(function -> {
            if (function instanceof DensityFunctions.ShiftedNoise noise && (noise.noise().noiseData().is(Noises.CONTINENTALNESS)
                    || noise.noise().noiseData().is(Noises.CONTINENTALNESS_LARGE))) return DensityFunctions.constant(-0.8);
            return function;
        });
        var nativeOcean = RandomState.create(withRouter(source, oceanRouter), registries.lookupOrThrow(Registries.NOISE), 8675309);
        var nativeClimate = RandomState.create(source, registries.lookupOrThrow(Registries.NOISE), 8675309);
        int landSamples = 0;
        for (int x = -256; x <= 256; x += 64) for (int z = -256; z <= 256; z += 64) {
            int height = decoded.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, helper.getLevel(), random);
            if (height > decoded.getSeaLevel()) landSamples++;
        }
        helper.assertTrue(landSamples > 40, "Expected majority land around origin; got " + landSamples + "/81");
        for (int x : new int[]{-100000, 100000}) for (int z : new int[]{-100000, 0, 100000}) {
            for (int y = decoded.getSeaLevel(); y < 256; y += 16) {
                var context = new DensityFunction.SinglePointContext(x, y, z);
                double density = random.router().finalDensity().compute(context);
                helper.assertTrue(density < 0,
                        "Exterior has above-water density at " + x + "," + y + "," + z);
                helper.assertTrue(Math.abs(density - nativeOcean.router().finalDensity().compute(context)) < 1.0e-8,
                        "Exterior density no longer follows the active pack's native ocean terrain");
                helper.assertTrue(random.router().temperature().compute(context) == nativeClimate.router().temperature().compute(context),
                        "The continent generator changed temperature");
                helper.assertTrue(random.router().vegetation().compute(context) == nativeClimate.router().vegetation().compute(context),
                        "The continent generator changed humidity");
            }
            for (int y = -32; y < decoded.getSeaLevel(); y += 8) {
                var context = new DensityFunction.SinglePointContext(x, y, z);
                helper.assertTrue(Math.abs(random.router().finalDensity().compute(context)
                        - nativeOcean.router().finalDensity().compute(context)) < 1.0e-8,
                        "The outer ocean floor was flattened instead of following native terrain");
            }
            int height = decoded.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, helper.getLevel(), random);
            helper.assertTrue(height < decoded.getSeaLevel() - 16, "Exterior is not deep ocean: " + height);
        }
        helper.succeed();
    }

    private static NoiseGeneratorSettings withRouter(NoiseGeneratorSettings base, NoiseRouter router) {
        return new NoiseGeneratorSettings(base.noiseSettings(), base.defaultBlock(), base.defaultFluid(), router,
                base.surfaceRule(), base.spawnTarget(), base.seaLevel(), base.disableMobGeneration(),
                base.aquifersEnabled(), base.oreVeinsEnabled(), base.useLegacyRandomSource());
    }
}
