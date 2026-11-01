package dev.aki.islands.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** World-local climate controls. Old worlds decode with the feature disabled. */
public record LatitudeClimateSettings(boolean enabled, int temperatePercent, int strengthPercent, int transitionPercent) {
    public static final LatitudeClimateSettings DISABLED = new LatitudeClimateSettings(false, 65, 85, 100);
    public static final LatitudeClimateSettings DEFAULT = DISABLED.withEnabled(true);
    public static final Codec<LatitudeClimateSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("enabled", false).forGetter(LatitudeClimateSettings::enabled),
            Codec.intRange(50, 90).optionalFieldOf("temperate_percent", 65).forGetter(LatitudeClimateSettings::temperatePercent),
            Codec.intRange(0, 100).optionalFieldOf("strength_percent", 85).forGetter(LatitudeClimateSettings::strengthPercent),
            Codec.intRange(50, 200).optionalFieldOf("transition_percent", 100).forGetter(LatitudeClimateSettings::transitionPercent)
    ).apply(instance, LatitudeClimateSettings::new));

    public LatitudeClimateSettings {
        if (temperatePercent < 50 || temperatePercent > 90 || strengthPercent < 0 || strengthPercent > 100
                || transitionPercent < 50 || transitionPercent > 200) {
            throw new IllegalArgumentException("Latitude climate settings are outside their supported ranges");
        }
    }

    public LatitudeClimateSettings withEnabled(boolean enabled) {
        return new LatitudeClimateSettings(enabled, temperatePercent, strengthPercent, transitionPercent);
    }
}
