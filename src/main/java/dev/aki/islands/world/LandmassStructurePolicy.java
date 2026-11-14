package dev.aki.islands.world;

import dev.aki.islands.IslandsMod;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;

/** Opt-in tag for structures that create land rather than buildings or ocean ruins. */
public final class LandmassStructurePolicy {
    public static final TagKey<Structure> LANDMASSES = TagKey.create(Registries.STRUCTURE,
            ResourceLocation.fromNamespaceAndPath(IslandsMod.ID, "landmass_structures"));

    private LandmassStructurePolicy() {}

    public static boolean createsLand(RegistryAccess registries, Structure structure) {
        return registries.registryOrThrow(Registries.STRUCTURE).wrapAsHolder(structure).is(LANDMASSES);
    }

    public static boolean fitsPlan(ContinentChunkGenerator generator, BoundingBox bounds) {
        // Include both edges and the interior, so coves and gaps between continents count too.
        for (int x = bounds.minX();; x = (int) Math.min((long) x + 8, bounds.maxX())) {
            for (int z = bounds.minZ();; z = (int) Math.min((long) z + 8, bounds.maxZ())) {
                if (!generator.isPlannedLand(x, z)) return false;
                if (z == bounds.maxZ()) break;
            }
            if (x == bounds.maxX()) break;
        }
        return true;
    }
}
