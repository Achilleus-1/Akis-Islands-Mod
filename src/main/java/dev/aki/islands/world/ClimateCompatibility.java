package dev.aki.islands.world;

import net.neoforged.fml.ModList;

/** One temperature controller at a time. No foreign config changes or mixin-order assumptions. */
public final class ClimateCompatibility {
    private ClimateCompatibility() {}

    public static boolean hasExternalController() {
        var mods = ModList.get();
        return mods != null && (mods.isLoaded("naturaltemperature") || mods.isLoaded("natural_temperature"));
    }

    public static boolean shouldApply(LatitudeClimateSettings settings) {
        return settings.enabled() && settings.strengthPercent() > 0 && !hasExternalController();
    }
}
