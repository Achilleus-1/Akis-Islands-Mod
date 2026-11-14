package dev.aki.islands.world;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import java.util.concurrent.atomic.AtomicLong;

/** A private mutable reference for startup terrain integrations such as Hybrid Aquatic. */
final class RuntimeNoiseSettings extends Holder.Reference<NoiseGeneratorSettings> {
    private static final AtomicLong NEXT_ID = new AtomicLong();
    private static final HolderOwner<NoiseGeneratorSettings> OWNER = new HolderOwner<>() {};

    RuntimeNoiseSettings(NoiseGeneratorSettings settings) {
        super(Type.STAND_ALONE, OWNER, ResourceKey.create(Registries.NOISE_SETTINGS,
                ResourceLocation.fromNamespaceAndPath("akis_islands", "runtime/" + NEXT_ID.incrementAndGet())), settings);
    }

    @Override
    public boolean canSerializeIn(HolderOwner<NoiseGeneratorSettings> owner) {
        // The generator codec persists baseSettings and its world-local options instead.
        return false;
    }
}
