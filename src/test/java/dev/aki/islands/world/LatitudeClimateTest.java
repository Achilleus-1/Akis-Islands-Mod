package dev.aki.islands.world;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LatitudeClimateTest {
    private LatitudeClimate climate(LatitudeClimateSettings settings) {
        return new LatitudeClimate(settings, 1000, 1000);
    }

    @Test void disabledZeroStrengthAndUndergroundPreserveNativeTemperatureExactly() {
        for (var settings : new LatitudeClimateSettings[]{LatitudeClimateSettings.DISABLED,
                new LatitudeClimateSettings(true, 65, 0, 100)}) {
            for (int z : new int[]{-100000, 0, 100000}) for (double input : new double[]{-2.3, -.5, 0, .9, 2.1}) {
                assertEquals(input, climate(settings).temperature(input, z, 100), 0);
            }
        }
        var active = climate(LatitudeClimateSettings.DEFAULT);
        for (int y : new int[]{-64, -1, 0}) for (int z : new int[]{-5000, 0, 5000}) {
            assertEquals(.75, active.temperature(.75, z, y), 0);
        }
    }

    @Test void northIsColdSouthIsHotAndTheMiddleStaysBroad() {
        var profile = climate(LatitudeClimateSettings.DEFAULT);
        assertEquals(0, profile.temperature(0, 0, 64), 0);
        assertTrue(profile.temperature(0, -1000, 64) < -.7);
        assertTrue(profile.temperature(0, 1000, 64) > .7);
        for (int z = -650; z <= 650; z++) assertTrue(Math.abs(profile.baseTemperature(z)) <= .1200001);
        double previous = -Double.MAX_VALUE;
        for (int z = -3000; z <= 3000; z++) {
            double current = profile.temperature(.33, z, 64);
            assertTrue(current >= previous, "Temperature reversed direction at " + z);
            previous = current;
        }
        assertEquals(profile.baseTemperature(1000), profile.baseTemperature(1_000_000), 0, "Bands should not loop");
    }

    @Test void widthTransitionAndStrengthHaveIndependentEffects() {
        var narrow = climate(new LatitudeClimateSettings(true, 50, 100, 100));
        var wide = climate(new LatitudeClimateSettings(true, 90, 100, 100));
        assertTrue(wide.baseTemperature(700) < narrow.baseTemperature(700));
        var shortTransition = climate(new LatitudeClimateSettings(true, 65, 100, 50));
        var longTransition = climate(new LatitudeClimateSettings(true, 65, 100, 200));
        assertTrue(shortTransition.baseTemperature(850) > longTransition.baseTemperature(850));
        assertEquals(shortTransition.baseTemperature(500), longTransition.baseTemperature(500), 0);
        var weak = climate(new LatitudeClimateSettings(true, 65, 25, 100));
        var strong = climate(new LatitudeClimateSettings(true, 65, 100, 100));
        assertTrue(weak.temperature(0, 1000, 64) < strong.temperature(0, 1000, 64));
    }

    @Test void boundariesAndUndergroundTransitionAreContinuousAndLocalVariationSurvives() {
        var profile = climate(LatitudeClimateSettings.DEFAULT);
        for (double z : new double[]{-1000, -650, 0, 650, 1000}) {
            assertEquals(profile.baseTemperature(z - 1e-5), profile.baseTemperature(z + 1e-5), 1e-6);
        }
        assertTrue(profile.temperature(.5, 200, 64) > profile.temperature(-.5, 200, 64));
        assertTrue(Math.abs(profile.temperature(.5, 1000, 1) - .5) < .001);
        assertEquals(profile.temperature(.5, 1000, 48), profile.temperature(.5, 1000, 100), 0);
    }

    @Test void climateFitsBothHemispheresOfEveryLayoutWithoutChangingItsSeed() {
        for (var layout : ContinentSettings.Layout.values()) for (long seed = -4; seed <= 4; seed++) {
            var shape = new ContinentShape(new ContinentSettings(layout, 4000, 5), seed);
            var other = new ContinentShape(new ContinentSettings(layout, 4000, 5), seed);
            var profile = new LatitudeClimate(LatitudeClimateSettings.DEFAULT, shape);
            assertTrue(shape.northExtent() > 0 && shape.northExtent() < shape.extent());
            assertTrue(shape.southExtent() > 0 && shape.southExtent() < shape.extent());
            assertEquals(-.9, profile.baseTemperature(-shape.northExtent()), 1e-12);
            assertEquals(.9, profile.baseTemperature(shape.southExtent()), 1e-12);
            assertEquals(shape.northExtent(), other.northExtent(), 0);
            assertEquals(shape.southExtent(), other.southExtent(), 0);
        }
    }

    @Test void settingsRoundTripAndRejectInvalidRanges() {
        var settings = new LatitudeClimateSettings(true, 80, 92, 150);
        var json = LatitudeClimateSettings.CODEC.encodeStart(JsonOps.INSTANCE, settings).getOrThrow();
        assertEquals(settings, LatitudeClimateSettings.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
        assertEquals(LatitudeClimateSettings.DISABLED, LatitudeClimateSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{}")).getOrThrow());
        for (String invalid : new String[]{"{\"temperate_percent\":49}", "{\"temperate_percent\":91}",
                "{\"strength_percent\":101}", "{\"transition_percent\":49}", "{\"transition_percent\":201}"}) {
            assertTrue(LatitudeClimateSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(invalid)).error().isPresent());
        }
    }
}
