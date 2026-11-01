package dev.aki.islands;

import com.mojang.serialization.MapCodec;
import dev.aki.islands.world.ContinentChunkGenerator;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(IslandsMod.ID)
public final class IslandsMod {
    public static final String ID = "akis_islands";
    private static final DeferredRegister<MapCodec<? extends ChunkGenerator>> GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, ID);
    static {
        GENERATORS.register("continents", () -> ContinentChunkGenerator.CODEC);
    }
    public IslandsMod(IEventBus bus) { GENERATORS.register(bus); }
}
