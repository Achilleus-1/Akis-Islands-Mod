package dev.aki.islands.client;

import dev.aki.islands.IslandsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterPresetEditorsEvent;

@Mod(value = IslandsMod.ID, dist = Dist.CLIENT)
public final class IslandsClient {
    public IslandsClient(IEventBus bus) {
        bus.addListener(IslandsClient::registerPresetEditor);
    }

    private static void registerPresetEditor(RegisterPresetEditorsEvent event) {
        event.register(ResourceKey.create(Registries.WORLD_PRESET,
                        ResourceLocation.fromNamespaceAndPath(IslandsMod.ID, "continents")),
                (parent, context) -> new IslandsScreen(parent));
    }
}
