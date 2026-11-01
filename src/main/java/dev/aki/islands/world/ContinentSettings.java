package dev.aki.islands.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.datafixers.util.Either;
import java.util.function.Function;

/** Immutable world settings. Distances are in blocks. */
public record ContinentSettings(Layout layout, int radius, int count, int coastWidth,
                                double roughness, int bridgeWidth, int oceanDepth, Climate climate, int format) {
    public enum Layout { SINGLE, CONNECTED, ARCHIPELAGO }
    public enum Climate { UNCHANGED, NORTH_TO_SOUTH, EQUATOR }
    public static final ContinentSettings DEFAULT = new ContinentSettings(Layout.SINGLE, 8000, 3);
    public static final ContinentSettings LEGACY_DEFAULT = new ContinentSettings(
            Layout.SINGLE, 8000, 3, 384, 0.35, 512, 48, Climate.UNCHANGED);

    private static final Codec<ContinentSettings> CURRENT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(2, 2).fieldOf("format").forGetter(ContinentSettings::format),
            enumCodec(Layout.class).optionalFieldOf("layout", Layout.SINGLE).forGetter(ContinentSettings::layout),
            Codec.intRange(1000, 100000).optionalFieldOf("radius", 8000).forGetter(ContinentSettings::radius),
            Codec.intRange(2, 6).optionalFieldOf("count", 3).forGetter(ContinentSettings::count)
    ).apply(instance, (format, layout, radius, count) -> new ContinentSettings(layout, radius, count)));

    // Only old saves read/write these controls. New layouts serialize their three public choices.
    private static final Codec<ContinentSettings> LEGACY_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.intRange(1, 1).optionalFieldOf("format", 1).forGetter(s -> 1),
            enumCodec(Layout.class).optionalFieldOf("layout", Layout.SINGLE).forGetter(ContinentSettings::layout),
            Codec.intRange(1000, 100000).optionalFieldOf("radius", 8000).forGetter(ContinentSettings::radius),
            Codec.intRange(2, 6).optionalFieldOf("count", 3).forGetter(ContinentSettings::count),
            Codec.intRange(64, 4096).optionalFieldOf("coast_width", 384).forGetter(ContinentSettings::coastWidth),
            Codec.doubleRange(0, 1).optionalFieldOf("roughness", 0.35).forGetter(ContinentSettings::roughness),
            Codec.intRange(128, 4096).optionalFieldOf("bridge_width", 512).forGetter(ContinentSettings::bridgeWidth),
            Codec.intRange(24, 96).optionalFieldOf("ocean_depth", 48).forGetter(ContinentSettings::oceanDepth),
            enumCodec(Climate.class).optionalFieldOf("climate", Climate.UNCHANGED).forGetter(ContinentSettings::climate)
    ).apply(instance, (format, layout, radius, count, coast, roughness, bridge, depth, climate) ->
            new ContinentSettings(layout, radius, count, coast, roughness, bridge, depth, climate)));
    public static final Codec<ContinentSettings> CODEC = Codec.either(CURRENT_CODEC, LEGACY_CODEC)
            .xmap(either -> either.map(Function.identity(), Function.identity()),
                    settings -> settings.isLegacy() ? Either.right(settings) : Either.left(settings));

    public ContinentSettings(Layout layout, int radius, int count) {
        this(layout, radius, count, Math.clamp(radius / 32, 96, 512), 0.35,
                Math.clamp(radius / 25, 128, 2048), 48, Climate.UNCHANGED, 2);
    }

    /** Preserve the generation contract of worlds created by 0.1.0. */
    public ContinentSettings(Layout layout, int radius, int count, int coastWidth,
                             double roughness, int bridgeWidth, int oceanDepth, Climate climate) {
        this(layout, radius, count, coastWidth, roughness, bridgeWidth, oceanDepth, climate, 1);
    }
    public ContinentSettings {
        if ((format != 1 && format != 2) || layout == null || climate == null || radius < 1000 || radius > 100000 || count < 2 || count > 6
                || coastWidth < 64 || coastWidth > 4096 || !Double.isFinite(roughness) || roughness < 0 || roughness > 1
                || bridgeWidth < 128 || bridgeWidth > 4096 || oceanDepth < 24 || oceanDepth > 96) {
            throw new IllegalArgumentException("Continent settings are outside the supported ranges");
        }
    }
    public double effectiveCoastWidth() { return Math.min(coastWidth, radius * 0.15); }
    public boolean isLegacy() { return format == 1; }
    private static <E extends Enum<E>> Codec<E> enumCodec(Class<E> type) {
        return Codec.STRING.comapFlatMap(value -> {
            try { return DataResult.success(Enum.valueOf(type, value)); }
            catch (IllegalArgumentException exception) { return DataResult.error(() -> "Unknown " + type.getSimpleName() + ": " + value); }
        }, Enum::name);
    }
}
