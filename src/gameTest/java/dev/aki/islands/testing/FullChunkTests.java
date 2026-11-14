package dev.aki.islands.testing;

import dev.aki.islands.IslandsMod;
import dev.aki.islands.world.ContinentChunkGenerator;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.fml.ModList;
import java.util.stream.Collectors;

@GameTestHolder(IslandsMod.ID)
@PrefixGameTestTemplate(false)
public final class FullChunkTests {
    @GameTest(template = "empty", timeoutTicks = 1200)
    public static void fullChunksUseContinentGenerator(GameTestHelper helper) {
        var level = helper.getLevel().getServer().overworld();
        helper.assertTrue(level.getChunkSource().getGenerator() instanceof ContinentChunkGenerator,
                "Full generation test requires the test-only continent overworld; found " + level.getChunkSource().getGenerator().getClass().getName());
        int sea = level.getSeaLevel();
        var random = level.getChunkSource().randomState();
        int bestX = 0, bestZ = 0, bestHeight = Integer.MIN_VALUE;
        var generator = (ContinentChunkGenerator) level.getChunkSource().getGenerator();
        if (ModList.get().isLoaded("hybrid_aquatic")) {
            // Hybrid Aquatic 1.7.4 appends its data-driven carver with a vanilla min node.
            // Its marker is replaced during installation, so no Hybrid Java node remains.
            var ops = net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE, level.registryAccess());
            var terrain = net.minecraft.world.level.levelgen.DensityFunction.HOLDER_HELPER_CODEC
                    .encodeStart(ops, generator.generatorSettings().value().noiseRouter().finalDensity()).getOrThrow().getAsJsonObject();
            helper.assertTrue(terrain.get("type").getAsString().equals("minecraft:min")
                            && terrain.getAsJsonObject("argument1").get("type").getAsString().equals("akis_islands:ocean_boundary"),
                    "Startup trench carver did not extend the continent terrain boundary");
            LogUtils.getLogger().info("Hybrid Aquatic full-chunk check: startup carver appended to the island terrain");
        }
        var biomeNamespaces = generator.getBiomeSource().possibleBiomes().stream()
                .flatMap(biome -> biome.unwrapKey().stream()).map(key -> key.location().getNamespace()).collect(Collectors.toSet());
        for (String mod : new String[]{"terralith", "biomesoplenty", "regions_unexplored"}) {
            if (ModList.get().isLoaded(mod)) helper.assertTrue(biomeNamespaces.contains(mod), "Missing biomes from " + mod);
        }
        var shape = new dev.aki.islands.world.ContinentShape(generator.islands(), generator.layoutSeed());
        for (int x = -256; x <= 256; x += 64) for (int z = -256; z <= 256; z += 64) {
            if (shape.landWeight(x, z) < 1) continue;
            int height = generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level, random);
            if (height > bestHeight) { bestX = x; bestZ = z; bestHeight = height; }
        }
        helper.assertTrue(bestHeight > sea + 2, "No inland terrain in the central planned landmass; maximum height " + bestHeight);
        // A river or lake can cross 0,0. Exercise an actual inland chunk inside the central plan.
        var inland = level.getChunk(Math.floorDiv(bestX, 16), Math.floorDiv(bestZ, 16));
        LogUtils.getLogger().info("Island full-chunk check: inland sample=({},{}), baseHeight={}, biome namespaces={}",
                bestX, bestZ, bestHeight, biomeNamespaces);
        int landColumns = 0;
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            if (inland.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z) > sea) landColumns++;
        }
        helper.assertTrue(landColumns > 128, "Expected majority land in the central chunk; got " + landColumns);
        var ocean = level.getChunk(512, 512);
        int waterColumns = 0;
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            if (!ocean.getFluidState(new BlockPos(8192 + x, sea - 5, 8192 + z)).isEmpty()) waterColumns++;
        }
        helper.assertTrue(waterColumns > 240, "Expected ocean water in distant full chunk; got " + waterColumns);
        // Probe the reported outskirts after all chunk stages, not just the raw density router.
        for (int[] point : new int[][]{{4700, -5000}, {6000, -6000}, {-6000, -6000}, {-6000, 6000}}) {
            int cx = Math.floorDiv(point[0], 16), cz = Math.floorDiv(point[1], 16);
            boolean plannedOcean = true;
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                if (shape.landWeight(cx * 16 + x, cz * 16 + z) != 0) plannedOcean = false;
            }
            if (!plannedOcean) continue;
            var completed = level.getChunk(cx, cz);
            int aboveWater = 0;
            int iceColumns = 0;
            int rawAboveWater = 0;
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                int height = completed.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z);
                if (height >= sea) {
                    // Frozen oceans retain native icebergs after terrain generation.
                    var top = completed.getBlockState(new BlockPos(cx * 16 + x, height, cz * 16 + z));
                    if (top.is(Blocks.PACKED_ICE) || top.is(Blocks.BLUE_ICE) || top.is(Blocks.ICE)
                            || top.is(Blocks.SNOW_BLOCK) || top.is(Blocks.SNOW)) iceColumns++;
                    else aboveWater++;
                }
                if (generator.getBaseHeight(cx * 16 + x, cz * 16 + z, Heightmap.Types.OCEAN_FLOOR_WG, level, random) > sea) rawAboveWater++;
            }
            LogUtils.getLogger().info("Outskirts ocean check: ({},{}), raw land columns={}, completed land columns={}, ice columns={}",
                    point[0], point[1], rawAboveWater, aboveWater, iceColumns);
            helper.assertTrue(rawAboveWater == 0 && aboveWater == 0, "Unplanned land after full generation at " + point[0] + "," + point[1]
                    + ": raw=" + rawAboveWater + ", completed=" + aboveWater);
        }
        for (var dimension : level.getServer().getAllLevels()) {
            if (dimension == level) continue;
            var otherGenerator = dimension.getChunkSource().getGenerator();
            helper.assertTrue(!(otherGenerator instanceof ContinentChunkGenerator), "Continent layout leaked into " + dimension.dimension().location());
            var chunk = dimension.getChunk(0, 0);
            LogUtils.getLogger().info("Dimension full-chunk check: {}, generator={}, status={}",
                    dimension.dimension().location(), otherGenerator.getClass().getName(), chunk.getPersistedStatus());
        }
        LogUtils.getLogger().info("Full-chunk terrain, biome, ocean and dimension checks passed");
        helper.succeed();
    }
}
