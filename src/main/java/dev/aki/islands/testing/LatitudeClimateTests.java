package dev.aki.islands.testing;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import dev.aki.islands.IslandsMod;
import dev.aki.islands.world.ClimateCompatibility;
import dev.aki.islands.world.ContinentChunkGenerator;
import dev.aki.islands.world.ContinentSettings;
import dev.aki.islands.world.ContinentShape;
import dev.aki.islands.world.LatitudeClimateSettings;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.HashSet;
import java.util.Set;

@GameTestHolder(IslandsMod.ID)
@PrefixGameTestTemplate(false)
public final class LatitudeClimateTests {
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void climateUsesNativeBiomeSelectionAndKeepsTerrain(GameTestHelper helper) {
        var registries = helper.getLevel().registryAccess();
        var preset = registries.registryOrThrow(Registries.WORLD_PRESET).get(ResourceLocation.withDefaultNamespace("normal"));
        var base = (NoiseBasedChunkGenerator) preset.createWorldDimensions().overworld();
        var disabled = new ContinentChunkGenerator(base.getBiomeSource(), base.generatorSettings(), ContinentSettings.DEFAULT, 1234567);
        var configured = new LatitudeClimateSettings(true, 65, 85, 100);
        var enabled = new ContinentChunkGenerator(base.getBiomeSource(), base.generatorSettings(), ContinentSettings.DEFAULT, 1234567, configured);
        var ops = RegistryOps.create(JsonOps.INSTANCE, registries);
        var encoded = ChunkGenerator.CODEC.encodeStart(ops, enabled).getOrThrow();
        var decoded = (ContinentChunkGenerator) ChunkGenerator.CODEC.parse(ops, encoded).getOrThrow();
        helper.assertTrue(decoded.latitudeClimate().equals(configured), "Latitude settings did not survive world serialization");
        helper.assertTrue(decoded.latitudeClimate().temperatePercent() > 50, "Default middle should be larger than the combined ends");
        var old = ChunkGenerator.CODEC.encodeStart(ops, disabled).getOrThrow().getAsJsonObject().deepCopy();
        old.remove("latitude_climate");
        helper.assertTrue(!((ContinentChunkGenerator) ChunkGenerator.CODEC.parse(ops, old).getOrThrow()).latitudeClimate().enabled(),
                "Loading an old world enabled latitude climate");
        helper.assertTrue(enabled.getBiomeSource() == base.getBiomeSource(), "Latitude climate replaced the biome source");
        var a = RandomState.create(disabled.generatorSettings().value(), registries.lookupOrThrow(Registries.NOISE), 8675309);
        var b = RandomState.create(decoded.generatorSettings().value(), registries.lookupOrThrow(Registries.NOISE), 8675309);
        var shape = new ContinentShape(ContinentSettings.DEFAULT, 1234567);
        int north = -(int) Math.ceil(shape.northExtent() * 1.2), south = (int) Math.ceil(shape.southExtent() * 1.2);
        for (int x : new int[]{-512, 0, 512}) for (int z : new int[]{north, 0, south}) {
            for (int y : new int[]{-32, 0, 32, 64, 128}) {
                var context = new DensityFunction.SinglePointContext(x, y, z);
                helper.assertTrue(a.router().finalDensity().compute(context) == b.router().finalDensity().compute(context), "Latitude changed terrain density");
                helper.assertTrue(a.router().vegetation().compute(context) == b.router().vegetation().compute(context), "Latitude changed humidity");
                if (y <= 0 || ClimateCompatibility.hasExternalController()) {
                    helper.assertTrue(a.router().temperature().compute(context) == b.router().temperature().compute(context),
                            "Latitude changed underground or externally controlled climate");
                }
            }
        }
        if (!ClimateCompatibility.hasExternalController()) {
            Set<ResourceLocation> cold = new HashSet<>(), middle = new HashSet<>(), hot = new HashSet<>();
            for (int x = -1024; x <= 1024; x += 64) {
                cold.add(biome(enabled, b, x, north));
                middle.add(biome(enabled, b, x, 0));
                hot.add(biome(enabled, b, x, south));
                helper.assertTrue(b.router().temperature().compute(new DensityFunction.SinglePointContext(x, 64, north)) < -.4, "North climate is not cold");
                helper.assertTrue(b.router().temperature().compute(new DensityFunction.SinglePointContext(x, 64, south)) > .4, "South climate is not hot");
            }
            // Gameplay biome temperature is not the placement coordinate: warm/cold ocean biomes
            // can share that property. Verify the native resolver actually selects different biomes.
            LogUtils.getLogger().info("Latitude biome selection: north={}, middle={}, south={}", cold, middle, hot);
            helper.assertTrue(!cold.equals(hot), "Native biome selection ignored the north/south temperature difference");
        } else {
            LogUtils.getLogger().info("Latitude compatibility: deferred to the installed external temperature controller");
        }
        helper.succeed();
    }

    private static ResourceLocation biome(ContinentChunkGenerator generator, RandomState state, int x, int z) {
        return generator.getBiomeSource().getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(64), QuartPos.fromBlock(z),
                state.sampler()).unwrapKey().orElseThrow().location();
    }
}
