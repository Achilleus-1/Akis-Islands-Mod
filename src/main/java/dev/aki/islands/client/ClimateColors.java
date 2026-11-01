package dev.aki.islands.client;

import dev.aki.islands.world.ContinentShape;

/** A continuous schematic climate palette, not a biome or terrain map. */
final class ClimateColors {
    private ClimateColors() {}

    static int rgb(double temperature) {
        int middle = 0x659668;
        int edge = temperature < 0 ? 0xb9d9ec : 0xd6a064;
        double weight = ContinentShape.clamp((Math.abs(temperature) - 0.12) / 0.78, 0, 1);
        int r = (int) (((middle >> 16) & 255) * (1 - weight) + ((edge >> 16) & 255) * weight);
        int g = (int) (((middle >> 8) & 255) * (1 - weight) + ((edge >> 8) & 255) * weight);
        int b = (int) ((middle & 255) * (1 - weight) + (edge & 255) * weight);
        return r << 16 | g << 8 | b;
    }
}
