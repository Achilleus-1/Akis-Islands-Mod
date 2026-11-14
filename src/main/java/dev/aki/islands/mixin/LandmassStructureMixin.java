package dev.aki.islands.mixin;

import dev.aki.islands.world.ContinentChunkGenerator;
import dev.aki.islands.world.LandmassStructurePolicy;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.function.Predicate;

@Mixin(Structure.class)
public abstract class LandmassStructureMixin {
    @Inject(method = "generate", at = @At("HEAD"), cancellable = true)
    private void akisIslands$checkLandmassStart(RegistryAccess registries, ChunkGenerator generator, BiomeSource biomes,
            RandomState random, StructureTemplateManager templates, long seed, ChunkPos chunk, int references,
            LevelHeightAccessor height, Predicate<Holder<Biome>> validBiome, CallbackInfoReturnable<StructureStart> callback) {
        if (generator instanceof ContinentChunkGenerator islands
                && !islands.isPlannedLand(chunk.getMiddleBlockX(), chunk.getMiddleBlockZ())
                && LandmassStructurePolicy.createsLand(registries, (Structure) (Object) this)) {
            callback.setReturnValue(StructureStart.INVALID_START);
        }
    }

    @Inject(method = "generate", at = @At("RETURN"), cancellable = true)
    private void akisIslands$checkLandmassFootprint(RegistryAccess registries, ChunkGenerator generator, BiomeSource biomes,
            RandomState random, StructureTemplateManager templates, long seed, ChunkPos chunk, int references,
            LevelHeightAccessor height, Predicate<Holder<Biome>> validBiome, CallbackInfoReturnable<StructureStart> callback) {
        var start = callback.getReturnValue();
        if (generator instanceof ContinentChunkGenerator islands && start.isValid()
                && LandmassStructurePolicy.createsLand(registries, (Structure) (Object) this)
                && !LandmassStructurePolicy.fitsPlan(islands, start.getBoundingBox())) {
            callback.setReturnValue(StructureStart.INVALID_START);
        }
    }
}
