package dev.aki.islands.world;

/** Deterministic temperature profile. Uses the planned coast, never generated chunks or global settings. */
public record LatitudeClimate(LatitudeClimateSettings settings, double northExtent, double southExtent) {
    public LatitudeClimate {
        if (settings == null || !Double.isFinite(northExtent) || !Double.isFinite(southExtent)
                || northExtent <= 0 || southExtent <= 0) throw new IllegalArgumentException("Invalid climate extents");
    }

    public LatitudeClimate(LatitudeClimateSettings settings, ContinentShape shape) {
        this(settings, shape.northExtent(), shape.southExtent());
    }

    public double baseTemperature(double z) {
        double span = z < 0 ? northExtent : southExtent;
        double latitude = Math.abs(z) / span;
        double middle = settings.temperatePercent() / 100.0;
        double magnitude;
        if (latitude <= middle) {
            magnitude = 0.12 * ease(latitude / middle);
        } else {
            double transition = (1 - middle) * settings.transitionPercent() / 100.0;
            magnitude = 0.12 + 0.78 * ease((latitude - middle) / transition);
        }
        return Math.copySign(magnitude, z);
    }

    public double temperature(double original, double z, int y) {
        if (!settings.enabled() || settings.strengthPercent() == 0 || y <= 0) return original;
        // Retain local variation within bands. Humidity and all other climate coordinates stay native.
        double target = ContinentShape.clamp(baseTemperature(z) + 0.18 * Math.tanh(original), -1, 1);
        double blend = settings.strengthPercent() / 100.0 * ease(y / 48.0);
        return original + blend * (target - original);
    }

    private double ease(double value) {
        double t = ContinentShape.clamp(value, 0, 1);
        // Quintic easing has zero first AND second derivatives at the joins and coast extremes.
        return settings.smoothTransitions() ? t * t * t * (t * (t * 6 - 15) + 10) : t * t * (3 - 2 * t);
    }
}
