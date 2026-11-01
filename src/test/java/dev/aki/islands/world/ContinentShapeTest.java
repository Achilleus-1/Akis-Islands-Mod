package dev.aki.islands.world;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;
import java.util.ArrayDeque;
import static org.junit.jupiter.api.Assertions.*;

class ContinentShapeTest {
    private ContinentSettings settings(ContinentSettings.Layout layout, int radius, int count) {
        return new ContinentSettings(layout, radius, count);
    }

    @Test void exteriorIsOceanForEveryLayoutAndManySeeds() {
        for (var mode : ContinentSettings.Layout.values()) {
            for (long seed = -20; seed < 20; seed++) {
                var shape = new ContinentShape(settings(mode, 8000, 6), seed);
                assertEquals(1, shape.landWeight(0, 0), 0, "Origin must be in the core landmass");
                for (int angle = 0; angle < 360; angle += 7) {
                    double r = shape.extent() * 1.1;
                    assertEquals(0, shape.landWeight(Math.cos(Math.toRadians(angle)) * r,
                            Math.sin(Math.toRadians(angle)) * r), 0, "Unbounded land at seed " + seed);
                }
                assertEquals(0, shape.landWeight(29_999_984, -29_999_984), 0);
            }
        }
    }

    @Test void layoutIsDeterministicAndSeedChangesCoastline() {
        var a = new ContinentShape(ContinentSettings.DEFAULT, 12345);
        var b = new ContinentShape(ContinentSettings.DEFAULT, 12345);
        var c = new ContinentShape(ContinentSettings.DEFAULT, 54321);
        int differences = 0;
        for (int x = -10000; x <= 10000; x += 250) {
            for (int z = -10000; z <= 10000; z += 250) {
                assertEquals(a.landWeight(x, z), b.landWeight(x, z));
                if (Math.abs(a.landWeight(x, z) - c.landWeight(x, z)) > 0.1) differences++;
            }
        }
        assertTrue(differences > 100);
    }

    @Test void singleAndConnectedPlansHaveOneConnectedLandComponent() {
        for (var mode : new ContinentSettings.Layout[]{ContinentSettings.Layout.SINGLE, ContinentSettings.Layout.CONNECTED}) {
            for (int seed = 0; seed < 12; seed++) {
                var shape = new ContinentShape(settings(mode, 4000, 6), seed);
                int size = 201;
                boolean[][] land = new boolean[size][size];
                int total = 0;
                for (int x = 0; x < size; x++) for (int z = 0; z < size; z++) {
                    land[x][z] = shape.landWeight((x - 100) * 60, (z - 100) * 60) >= 0.5;
                    if (land[x][z]) total++;
                }
                ArrayDeque<int[]> queue = new ArrayDeque<>();
                queue.add(new int[]{100, 100}); land[100][100] = false;
                int reached = 0;
                while (!queue.isEmpty()) {
                    int[] p = queue.remove(); reached++;
                    for (int[] d : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
                        int x = p[0] + d[0], z = p[1] + d[1];
                        if (x >= 0 && z >= 0 && x < size && z < size && land[x][z]) {
                            land[x][z] = false; queue.add(new int[]{x,z});
                        }
                    }
                }
                assertEquals(total, reached, mode + " disconnected at seed " + seed);
            }
        }
    }

    @Test void legacyClimateAndSettingsStillRoundTrip() {
        var base = ContinentSettings.DEFAULT;
        for (var climate : new ContinentSettings.Climate[]{ContinentSettings.Climate.NORTH_TO_SOUTH, ContinentSettings.Climate.EQUATOR}) {
            var legacy = new ContinentSettings(base.layout(), 8000, 3, 384, .35, 512, 48, climate);
            var shape = new ContinentShape(legacy, 1);
            assertTrue(legacy.isLegacy());
            assertEquals(legacy, ContinentSettings.CODEC.parse(JsonOps.INSTANCE,
                    ContinentSettings.CODEC.encodeStart(JsonOps.INSTANCE, legacy).getOrThrow()).getOrThrow());
            assertEquals(-1, shape.temperature(-80000));
            assertEquals(climate == ContinentSettings.Climate.EQUATOR ? -1 : 1, shape.temperature(80000));
            assertEquals(climate == ContinentSettings.Climate.EQUATOR ? 1 : 0, shape.temperature(0));
        }
    }

    @Test void settingsRoundTripAndRejectInvalidInput() {
        var value = settings(ContinentSettings.Layout.CONNECTED, 1000, 6);
        var json = ContinentSettings.CODEC.encodeStart(JsonOps.INSTANCE, value).getOrThrow();
        assertEquals(value, ContinentSettings.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
        assertFalse(value.isLegacy());
        for (String removed : new String[]{"coast_width", "roughness", "bridge_width", "ocean_depth", "climate", "seed"}) {
            assertFalse(json.getAsJsonObject().has(removed), "New settings must not contain " + removed);
        }
        for (String invalid : new String[]{"{\"format\":2,\"radius\":0}", "{\"format\":2,\"count\":7}", "{\"layout\":\"BROKEN\"}", "{\"format\":3}"}) {
            assertTrue(ContinentSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(invalid)).error().isPresent());
        }
        assertEquals(ContinentSettings.LEGACY_DEFAULT,
                ContinentSettings.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString("{}")).getOrThrow());
        assertThrows(IllegalArgumentException.class, () -> settings(ContinentSettings.Layout.SINGLE, 999, 3));
    }
}
