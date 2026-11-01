package dev.aki.islands.world;

import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/** Runtime temperature input. The owning generator serializes its settings, not this wired graph. */
record LatitudeTemperature(DensityFunction input, LatitudeClimate climate) implements DensityFunction {
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
        throw new UnsupportedOperationException("Latitude temperature is serialized through its owning generator");
    }
}
