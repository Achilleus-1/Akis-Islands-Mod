package dev.aki.islands.world;

/** Pure boundary rule shared by the density wrapper and its regression checks. */
final class OceanBoundary {
    private OceanBoundary() {}

    static double apply(double original, double land, int y, int seaLevel) {
        // Keep native air/water/caves and the seabed below the shallow surface band exactly intact.
        if (original <= 0 || y <= seaLevel - 16 || land >= 1) return original;
        double t = ContinentShape.clamp((y - (seaLevel - 16)) / 16.0, 0, 1);
        double fade = t * t * (3 - 2 * t);
        // Only remove positive terrain. At/above sea level, preview ocean (land <= .5)
        // cannot become solid, including terrain added independently of continentalness.
        return original * (1 - 2 * (1 - land) * fade);
    }
}
