package dev.aki.islands.world;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OceanBoundaryTest {
    private static final int SEA = 63;
    private final ContinentShape shape = new ContinentShape(
            new ContinentSettings(ContinentSettings.Layout.MIXED, 5000, 5), 1234567);

    @Test void terrainIndependentOfContinentalnessCannotCreateUnplannedLand() {
        for (int x = -8000; x <= 8000; x += 128) for (int z = -8000; z <= 8000; z += 128) {
            double land = shape.landWeight(x, z);
            for (int y : new int[]{SEA, SEA + 1, 96, 200, 319}) {
                double density = OceanBoundary.apply(.8, land, y, SEA);
                if (land <= .5) assertTrue(density <= 0, "Unexpected terrain outside preview at " + x + "," + z);
                if (land == 1) assertEquals(.8, density, 0, "Intended inland terrain must stay native");
            }
        }
    }

    @Test void nativeOceanCavesAndDeeperSeabedKeepTheirExactDensities() {
        for (double input : new double[]{-.8, -.001, 0, .001, .8}) {
            for (int y = -64; y <= 319; y++) {
                double result = OceanBoundary.apply(input, shape.landWeight(4700, -5000), y, SEA);
                if (input <= 0 || y <= SEA - 16) assertEquals(input, result, 0);
                assertTrue(result <= input, "The boundary must never add terrain");
                assertTrue(result >= -Math.abs(input) && result <= input);
            }
        }
    }

    @Test void shallowSuppressionEasesInAndKeepsTheCoastContinuous() {
        double previous = .8;
        for (int y = SEA - 16; y <= SEA + 8; y++) {
            double current = OceanBoundary.apply(.8, 0, y, SEA);
            assertTrue(current <= previous);
            previous = current;
        }
        assertEquals(0, OceanBoundary.apply(.8, 0, SEA - 8, SEA), 0);
        double previousCoast = -.8;
        for (int i = 0; i <= 1000; i++) {
            double current = OceanBoundary.apply(.8, i / 1000.0, SEA, SEA);
            assertTrue(current >= previousCoast);
            assertTrue(current - previousCoast < .002);
            previousCoast = current;
        }
    }
}
