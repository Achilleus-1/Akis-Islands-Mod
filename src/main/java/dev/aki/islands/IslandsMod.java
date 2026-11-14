package dev.aki.islands;

import com.mojang.serialization.MapCodec;
import dev.aki.islands.world.ContinentChunkGenerator;
import dev.aki.islands.world.IslandDensity;
import dev.aki.islands.world.OceanBoundaryDensity;
import dev.aki.islands.world.LatitudeTemperature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(IslandsMod.ID)
public final class IslandsMod {
    public static final String ID = "akis_islands";
    private static final DeferredRegister<MapCodec<? extends ChunkGenerator>> GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, ID);
    private static final DeferredRegister<MapCodec<? extends DensityFunction>> DENSITIES =
            DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE, ID);
    static {
        GENERATORS.register("continents", () -> ContinentChunkGenerator.CODEC);
        DENSITIES.register("island_density", () -> IslandDensity.MAP_CODEC);
        DENSITIES.register("ocean_boundary", () -> OceanBoundaryDensity.MAP_CODEC);
        DENSITIES.register("latitude_temperature", () -> LatitudeTemperature.MAP_CODEC);
    }
    public IslandsMod(IEventBus bus) {
        GENERATORS.register(bus);
        DENSITIES.register(bus);
    }
}
