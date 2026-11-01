package dev.aki.islands.world;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

/** Pure, thread-safe shape shared by generation and preview. */
public final class ContinentShape {
    private record Lobe(double x, double z, double rx, double rz, double angle, double phase) {}
    private final ContinentSettings settings;
    private final List<Lobe> lobes;
    private final double extent;
    private final double northExtent;
    private final double southExtent;
    private static final class ColumnCache {
        final long[] keys = new long[256];
        final double[] values = new double[256];
        final boolean[] filled = new boolean[256];
    }
    private final ThreadLocal<ColumnCache> columns = ThreadLocal.withInitial(ColumnCache::new);
    public ContinentShape(ContinentSettings settings, long seed) {
        this.settings = settings;
        SplittableRandom random = new SplittableRandom(seed);
        List<Lobe> generated = new ArrayList<>();
        int count = settings.layout() == ContinentSettings.Layout.SINGLE ? 1 : settings.count();
        double scale = settings.radius();
        double rotation = random.nextDouble(Math.PI * 2);
        for (int i = 0; i < count; i++) {
            double x = 0;
            double z = 0;
            double size = scale;
            if (count > 1) {
                size = scale * (settings.layout() == ContinentSettings.Layout.ARCHIPELAGO ? 0.19 : 0.30);
                if (i > 0) {
                    double angle = rotation + (i - 1) * Math.PI * 2 / (count - 1);
                    double distance = scale * (0.68 + random.nextDouble(0.12));
                    x = Math.cos(angle) * distance;
                    z = Math.sin(angle) * distance;
                }
            }
            generated.add(new Lobe(x, z, size * random.nextDouble(0.85, 1.10),
                    size * random.nextDouble(0.65, 0.95), random.nextDouble(Math.PI), random.nextDouble(Math.PI * 2)));
        }
        lobes = List.copyOf(generated);
        extent = lobes.stream().mapToDouble(l -> Math.hypot(l.x, l.z)
                + Math.max(l.rx, l.rz) * (1 + settings.roughness() * 0.64)).max().orElse(scale)
                + settings.effectiveCoastWidth() + settings.bridgeWidth();
        // Fit climate bands to the actual north/south footprint, even for a thin east/west island group.
        double minZ = 0, maxZ = 0;
        for (Lobe l : lobes) {
            for (int step = 0; step < 720; step++) {
                double theta = step * Math.PI / 360;
                double edge = 1 + settings.roughness() * (0.36 * Math.sin(3 * theta + l.phase)
                        + 0.18 * Math.sin(7 * theta - l.phase) + 0.09 * Math.sin(13 * theta + l.phase));
                double z = l.z + edge * (Math.cos(theta) * l.rx * Math.sin(l.angle)
                        + Math.sin(theta) * l.rz * Math.cos(l.angle));
                minZ = Math.min(minZ, z);
                maxZ = Math.max(maxZ, z);
            }
        }
        if (settings.layout() == ContinentSettings.Layout.CONNECTED) {
            double halfBridge = settings.bridgeWidth() / 2.0 + settings.effectiveCoastWidth();
            for (Lobe l : lobes) {
                minZ = Math.min(minZ, l.z - halfBridge);
                maxZ = Math.max(maxZ, l.z + halfBridge);
            }
        }
        northExtent = Math.max(1, -minZ);
        southExtent = Math.max(1, maxZ);
    }
    /** Positive on planned land, zero at its nominal shore, negative at sea. */
    public double signedDistance(double x, double z) {
        if (Math.abs(x) > extent || Math.abs(z) > extent) {
            return -Math.max(Math.abs(x), Math.abs(z)) + extent - settings.effectiveCoastWidth();
        }
        double result = -Double.MAX_VALUE;
        for (Lobe l : lobes) {
            double dx = x - l.x;
            double dz = z - l.z;
            double cosine = Math.cos(l.angle);
            double sine = Math.sin(l.angle);
            double u = (dx * cosine + dz * sine) / l.rx;
            double v = (-dx * sine + dz * cosine) / l.rz;
            double theta = Math.atan2(v, u);
            double edge = 1 + settings.roughness() * (0.36 * Math.sin(3 * theta + l.phase)
                    + 0.18 * Math.sin(7 * theta - l.phase) + 0.09 * Math.sin(13 * theta + l.phase));
            result = Math.max(result, (edge - Math.hypot(u, v)) * Math.min(l.rx, l.rz));
        }
        if (settings.layout() == ContinentSettings.Layout.CONNECTED) {
            for (int i = 1; i < lobes.size(); i++) {
                Lobe l = lobes.get(i);
                double t = clamp((x * l.x + z * l.z) / (l.x * l.x + l.z * l.z), 0, 1);
                result = Math.max(result, settings.bridgeWidth() / 2.0 + settings.effectiveCoastWidth()
                        - Math.hypot(x - t * l.x, z - t * l.z));
            }
        }
        return result;
    }
    /** Exactly 0 in the exterior ocean and 1 inland. */
    public double landWeight(double x, double z) {
        double t = clamp(0.5 + signedDistance(x, z) / (2 * settings.effectiveCoastWidth()), 0, 1);
        return t * t * (3 - 2 * t);
    }
    /** Reuse the horizontal mask through a column without sharing mutable state between generation workers. */
    public double landWeight(int x, int z) {
        ColumnCache cache = columns.get();
        long key = ((long) x << 32) | (z & 0xffffffffL);
        int slot = (x * 73428767 ^ z * 912931) & 255;
        if (cache.filled[slot] && cache.keys[slot] == key) return cache.values[slot];
        double value = landWeight((double) x, (double) z);
        cache.keys[slot] = key;
        cache.values[slot] = value;
        cache.filled[slot] = true;
        return value;
    }
    public double temperature(double z) {
        double latitude = z / settings.radius();
        return switch (settings.climate()) {
            case UNCHANGED -> 0;
            case NORTH_TO_SOUTH -> clamp(latitude, -1, 1);
            case EQUATOR -> 1 - 2 * clamp(Math.abs(latitude), 0, 1);
        };
    }
    public double extent() { return extent; }
    public double northExtent() { return northExtent; }
    public double southExtent() { return southExtent; }
    public static double clamp(double value, double min, double max) { return Math.max(min, Math.min(max, value)); }
}
