package dev.aki.islands.world;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;
import java.util.ArrayDeque;
import java.util.HexFormat;
import java.security.MessageDigest;
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

    @Test void existingLayoutsKeepTheirReleasedCoastlines() throws Exception {
        // Coastline fingerprints sampled from the published 0.3.0 jar, before adding MIXED.
        String[] expected = {"40226848792691887baa4f34b4ea6d933ec24efe8a08990440dadb138da32109",
                "958d5cc6d15123595df2be8a4051345641252052164d6b2ad939a1f6cbc72ae4",
                "d3e9cc4bf71038b84974ee82e742e6c456fb62c4d30fa1402ec077e037fa7b58"};
        var modes = new ContinentSettings.Layout[]{ContinentSettings.Layout.SINGLE,
                ContinentSettings.Layout.CONNECTED, ContinentSettings.Layout.ARCHIPELAGO};
        for (int i = 0; i < modes.length; i++) {
            var hash = MessageDigest.getInstance("SHA-256");
            for (int count : new int[]{2, 6}) for (long seed : new long[]{-13, 0, 8675309}) {
                var shape = new ContinentShape(settings(modes[i], 4000, count), seed);
                for (int x = -6400; x <= 6400; x += 80) for (int z = -6400; z <= 6400; z += 80) {
                    hash.update((byte) (shape.landWeight((double) x, (double) z) >= .5 ? 1 : 0));
                }
            }
            assertEquals(expected[i], HexFormat.of().formatHex(hash.digest()), modes[i] + " changed an existing coastline");
        }
    }

    @Test void mixedPlansCombineLargeLandmassesAndSmallOffshoreIslands() {
        for (long seed = 0; seed < 9; seed++) {
            var config = settings(ContinentSettings.Layout.MIXED, 8000, 3);
            assertEquals(config, ContinentSettings.CODEC.parse(JsonOps.INSTANCE,
                    ContinentSettings.CODEC.encodeStart(JsonOps.INSTANCE, config).getOrThrow()).getOrThrow());
            var shape = new ContinentShape(config, seed);
            var same = new ContinentShape(config, seed);
            int size = 201;
            double step = shape.extent() * 2.1 / (size - 1);
            boolean[][] land = new boolean[size][size];
            int large = 0, small = 0;
            for (int x = 0; x < size; x++) for (int z = 0; z < size; z++) {
                double wx = (x - 100) * step, wz = (z - 100) * step;
                double weight = shape.landWeight(wx, wz);
                assertEquals(weight, same.landWeight(wx, wz));
                land[x][z] = weight >= .5;
            }
            for (int x = 0; x < size; x++) for (int z = 0; z < size; z++) {
                if (!land[x][z]) continue;
                ArrayDeque<int[]> queue = new ArrayDeque<>();
                queue.add(new int[]{x, z}); land[x][z] = false;
                int area = 0;
                while (!queue.isEmpty()) {
                    int[] p = queue.remove(); area++;
                    for (int[] d : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
                        int nx = p[0] + d[0], nz = p[1] + d[1];
                        if (nx >= 0 && nz >= 0 && nx < size && nz < size && land[nx][nz]) {
                            land[nx][nz] = false; queue.add(new int[]{nx, nz});
                        }
                    }
                }
                if (area >= 300) large++;
                else if (area >= 4) small++;
            }
            assertTrue(large >= 2, "Mixed layout needs separate large landmasses at seed " + seed);
            assertTrue(small >= 1, "Mixed layout needs satellite islands at seed " + seed);
        }
    }
}
