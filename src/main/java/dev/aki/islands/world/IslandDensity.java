package dev.aki.islands.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringRepresentable;
import java.util.Locale;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/** Runtime wrapper; its owning generator serializes the original settings and options. */
public record IslandDensity(DensityFunction input, ContinentShape shape, Kind kind, int seaLevel, int oceanDepth)
        implements DensityFunction {
    public enum Kind implements StringRepresentable {
        CONTINENTS, FLOOR, TEMPERATURE;
        @Override public String getSerializedName() { return name().toLowerCase(Locale.ROOT); }
    }
    public static final MapCodec<IslandDensity> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("input").forGetter(IslandDensity::input),
            ContinentShape.CODEC.fieldOf("shape").forGetter(IslandDensity::shape),
            StringRepresentable.fromEnum(Kind::values).fieldOf("kind").forGetter(IslandDensity::kind),
            Codec.INT.fieldOf("sea_level").forGetter(IslandDensity::seaLevel),
            Codec.INT.fieldOf("ocean_depth").forGetter(IslandDensity::oceanDepth)
    ).apply(instance, IslandDensity::new));
    private static final KeyDispatchDataCodec<IslandDensity> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);
    @Override
    public double compute(FunctionContext context) {
        double original = input.compute(context);
        if (kind == Kind.TEMPERATURE) {
            if (context.blockY() < 0) return original;
            return ContinentShape.clamp(shape.temperature(context.blockZ()) * 0.85 + original * 0.15, -1, 1);
        }
        double weight = shape.landWeight(context.blockX(), context.blockZ());
        if (kind == Kind.CONTINENTS) {
            double inland = ContinentShape.clamp(original, 0.12, 1);
            return -0.8 + weight * (inland + 0.8);
        }
        if (weight >= 1) return original;
        double cap = (seaLevel - oceanDepth - context.blockY()) / 32.0;
        return Math.min(original, cap + weight * (Math.max(original, cap) - cap));
    }
    @Override
    public void fillArray(double[] values, ContextProvider provider) { provider.fillAllDirectly(values, this); }
    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new IslandDensity(input.mapAll(visitor), shape, kind, seaLevel, oceanDepth));
    }
    @Override
    public double minValue() {
        return switch (kind) {
            case CONTINENTS -> -0.8;
            case TEMPERATURE -> Math.min(-1, input.minValue());
            case FLOOR -> Math.min(-4096, input.minValue());
        };
    }
    @Override
    public double maxValue() {
        return switch (kind) {
            case CONTINENTS -> 1;
            case TEMPERATURE -> Math.max(1, input.maxValue());
            case FLOOR -> input.maxValue();
        };
    }
    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
