package dev.aki.islands.world;

import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/** Enforces the preview's ocean boundary after every terrain branch has been evaluated. */
record OceanBoundaryDensity(DensityFunction input, ContinentShape shape, int seaLevel) implements DensityFunction {
    @Override
    public double compute(FunctionContext context) {
        double original = input.compute(context);
        if (original <= 0 || context.blockY() <= seaLevel - 16) return original;
        return OceanBoundary.apply(original, shape.landWeight(context.blockX(), context.blockZ()), context.blockY(), seaLevel);
    }

    @Override
    public void fillArray(double[] values, ContextProvider provider) { provider.fillAllDirectly(values, this); }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        return visitor.apply(new OceanBoundaryDensity(input.mapAll(visitor), shape, seaLevel));
    }

    @Override
    public double minValue() { return Math.min(input.minValue(), -input.maxValue()); }

    @Override
    public double maxValue() { return input.maxValue(); }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        throw new UnsupportedOperationException("Ocean boundary is serialized through its owning chunk generator");
    }
}
