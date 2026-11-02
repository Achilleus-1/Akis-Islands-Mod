package dev.aki.islands.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.Noises;
import java.util.Optional;

/** Retains the noise generator's normal surfaces, carvers, features and structure stages. */
public final class ContinentChunkGenerator extends NoiseBasedChunkGenerator {
    public static final MapCodec<ContinentChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(ContinentChunkGenerator::getBiomeSource),
            NoiseGeneratorSettings.CODEC.fieldOf("settings").forGetter(ContinentChunkGenerator::baseSettings),
            ContinentSettings.CODEC.optionalFieldOf("islands").forGetter(generator -> Optional.of(generator.islands())),
            Codec.LONG.optionalFieldOf("layout_seed", 0L).forGetter(ContinentChunkGenerator::layoutSeed),
            Codec.intRange(1, 2).optionalFieldOf("layout_version", 1).forGetter(generator -> generator.islands().format()),
            LatitudeClimateSettings.CODEC.optionalFieldOf("latitude_climate", LatitudeClimateSettings.DISABLED)
                    .forGetter(ContinentChunkGenerator::latitudeClimate)
    ).apply(instance, (source, settings, islands, seed, version, climate) -> new ContinentChunkGenerator(source, settings,
            islands.orElse(version == 1 ? ContinentSettings.LEGACY_DEFAULT : ContinentSettings.DEFAULT), seed, climate)));
    private final Holder<NoiseGeneratorSettings> baseSettings;
    private final ContinentSettings islands;
    private final long layoutSeed;
    private final LatitudeClimateSettings latitudeClimate;
    public ContinentChunkGenerator(BiomeSource source, Holder<NoiseGeneratorSettings> baseSettings,
                                   ContinentSettings islands, long layoutSeed) {
        this(source, baseSettings, islands, layoutSeed, LatitudeClimateSettings.DISABLED);
    }

    public ContinentChunkGenerator(BiomeSource source, Holder<NoiseGeneratorSettings> baseSettings,
                                   ContinentSettings islands, long layoutSeed, LatitudeClimateSettings latitudeClimate) {
        super(source, Holder.direct(adapt(baseSettings.value(), islands, layoutSeed, latitudeClimate)));
        this.baseSettings = baseSettings;
        this.islands = islands;
        this.layoutSeed = layoutSeed;
        this.latitudeClimate = latitudeClimate;
    }
    private static NoiseGeneratorSettings adapt(NoiseGeneratorSettings base, ContinentSettings options, long seed,
                                                 LatitudeClimateSettings latitude) {
        ContinentShape shape = new ContinentShape(options, seed);
        // Normalize holder references, then replace the active continentalness graph wherever
        // the pack uses it (including terrain splines).
        DensityFunction target = base.noiseRouter().continents();
        while (target instanceof DensityFunctions.HolderHolder holder) target = holder.function().value();
        DensityFunction canonical = target.mapAll(function -> function);
        NoiseRouter r = base.noiseRouter().mapAll(new DensityFunction.Visitor() {
            @Override
            public DensityFunction apply(DensityFunction function) {
                // Packs such as Terralith route biome continentalness and terrain continentalness
                // through different roots. Also adapt their shared standard continentalness noise.
                if (function instanceof DensityFunctions.ShiftedNoise noise
                        && (noise.noise().noiseData().is(Noises.CONTINENTALNESS)
                        || noise.noise().noiseData().is(Noises.CONTINENTALNESS_LARGE))) {
                    return new IslandDensity(function, shape, IslandDensity.Kind.CONTINENTS, base.seaLevel(), options.oceanDepth());
                }
                if (function.equals(canonical) && canonical.minValue() != canonical.maxValue()) {
                    return DensityFunctions.cache2d(new IslandDensity(function, shape,
                            IslandDensity.Kind.CONTINENTS, base.seaLevel(), options.oceanDepth()));
                }
                return function;
            }
        });
        DensityFunction continents = new IslandDensity(base.noiseRouter().continents(), shape,
                IslandDensity.Kind.CONTINENTS, base.seaLevel(), options.oceanDepth());
        DensityFunction temperature = !options.isLegacy() || options.climate() == ContinentSettings.Climate.UNCHANGED ? r.temperature()
                : new IslandDensity(r.temperature(), shape, IslandDensity.Kind.TEMPERATURE, base.seaLevel(), options.oceanDepth());
        if (ClimateCompatibility.shouldApply(latitude)) {
            temperature = new LatitudeTemperature(r.temperature(), new LatitudeClimate(latitude, shape));
        }
        NoiseRouter bounded = new NoiseRouter(r.barrierNoise(), r.fluidLevelFloodednessNoise(), r.fluidLevelSpreadNoise(),
                r.lavaNoise(), temperature, r.vegetation(), continents, r.erosion(), r.depth(), r.ridges(),
                options.isLegacy() ? new IslandDensity(r.initialDensityWithoutJaggedness(), shape, IslandDensity.Kind.FLOOR, base.seaLevel(), options.oceanDepth())
                        : new OceanBoundaryDensity(r.initialDensityWithoutJaggedness(), shape, base.seaLevel()),
                options.isLegacy() ? new IslandDensity(r.finalDensity(), shape, IslandDensity.Kind.FLOOR, base.seaLevel(), options.oceanDepth())
                        : new OceanBoundaryDensity(r.finalDensity(), shape, base.seaLevel()),
                r.veinToggle(), r.veinRidged(), r.veinGap());
        return new NoiseGeneratorSettings(base.noiseSettings(), base.defaultBlock(), base.defaultFluid(), bounded,
                base.surfaceRule(), base.spawnTarget(), base.seaLevel(), base.disableMobGeneration(),
                base.aquifersEnabled(), base.oreVeinsEnabled(), base.useLegacyRandomSource());
    }
    @Override
    protected MapCodec<? extends ChunkGenerator> codec() { return CODEC; }
    @Override
    public boolean stable(ResourceKey<NoiseGeneratorSettings> key) { return baseSettings.is(key); }
    public Holder<NoiseGeneratorSettings> baseSettings() { return baseSettings; }
    public ContinentSettings islands() { return islands; }
    public long layoutSeed() { return layoutSeed; }
    public LatitudeClimateSettings latitudeClimate() { return latitudeClimate; }
}
