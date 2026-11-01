package dev.aki.islands.testing;

import dev.aki.islands.IslandsMod;
import dev.aki.islands.world.ContinentChunkGenerator;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.levelgen.Heightmap;
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
        helper.succeed();
    }
}
