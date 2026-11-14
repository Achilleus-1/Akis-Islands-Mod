package dev.aki.islands.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/** Runtime temperature input. The owning generator serializes its settings, not this wired graph. */
public record LatitudeTemperature(DensityFunction input, LatitudeClimate climate) implements DensityFunction {
    private static final Codec<LatitudeClimate> CLIMATE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            LatitudeClimateSettings.CODEC.fieldOf("settings").forGetter(LatitudeClimate::settings),
            Codec.doubleRange(Double.MIN_VALUE, Double.MAX_VALUE).fieldOf("north_extent").forGetter(LatitudeClimate::northExtent),
            Codec.doubleRange(Double.MIN_VALUE, Double.MAX_VALUE).fieldOf("south_extent").forGetter(LatitudeClimate::southExtent)
    ).apply(instance, LatitudeClimate::new));
    public static final MapCodec<LatitudeTemperature> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            DensityFunction.HOLDER_HELPER_CODEC.fieldOf("input").forGetter(LatitudeTemperature::input),
            CLIMATE_CODEC.fieldOf("climate").forGetter(LatitudeTemperature::climate)
    ).apply(instance, LatitudeTemperature::new));
    private static final KeyDispatchDataCodec<LatitudeTemperature> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);
    @Override
    public double compute(FunctionContext context) {
        return climate.temperature(input.compute(context), context.blockZ(), context.blockY());
    }

    @Override
    public void fillArray(double[] values, ContextProvider provider) { provider.fillAllDirectly(values, this); }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new LatitudeTemperature(input.mapAll(visitor), climate));
    }

    @Override
    public double minValue() { return Math.min(-1, input.minValue()); }

    @Override
    public double maxValue() { return Math.max(1, input.maxValue()); }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
