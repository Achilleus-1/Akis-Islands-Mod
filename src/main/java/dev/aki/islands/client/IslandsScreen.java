package dev.aki.islands.client;

import com.mojang.blaze3d.platform.NativeImage;
import dev.aki.islands.IslandsMod;
import dev.aki.islands.world.ContinentChunkGenerator;
import dev.aki.islands.world.ContinentSettings;
import dev.aki.islands.world.ContinentShape;
import dev.aki.islands.world.LatitudeClimate;
import dev.aki.islands.world.LatitudeClimateSettings;
import dev.aki.islands.world.ClimateCompatibility;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class IslandsScreen extends Screen {
    private record Field(String key, EditBox input) {}
    private final CreateWorldScreen parent;
    private final List<Field> fields = new ArrayList<>();
    private ContinentSettings draft;
    private long shapeSeed;
    private ContinentSettings.Layout layout;
    private LatitudeClimateSettings latitudeClimate;
    private EditBox radius, count;
    private Button apply;
    private ResourceLocation preview;
    private ContinentShape shape;
    private int previewX, previewY, previewSize, formWidth;
    private String error = "";
    private final boolean supported;

    public IslandsScreen(CreateWorldScreen parent) {
        super(Component.translatable("akis_islands.title"));
        this.parent = parent;
        ChunkGenerator generator = parent.getUiState().getSettings().selectedDimensions().overworld();
        var saved = generator instanceof ContinentChunkGenerator islands ? islands.islands() : ContinentSettings.DEFAULT;
        draft = new ContinentSettings(saved.layout(), saved.radius(), saved.count());
        latitudeClimate = generator instanceof ContinentChunkGenerator islands ? islands.latitudeClimate() : LatitudeClimateSettings.DISABLED;
        shapeSeed = generator instanceof ContinentChunkGenerator islands ? islands.layoutSeed()
                : parent.getUiState().getSettings().options().seed();
        supported = generator.getClass() == NoiseBasedChunkGenerator.class || generator instanceof ContinentChunkGenerator;
        layout = draft.layout();
    }

    @Override
    protected void init() {
        fields.clear();
        formWidth = Math.max(142, Math.min(230, (int) (width * 0.42)));
        addRenderableWidget(Button.builder(layoutLabel(), button -> {
            layout = ContinentSettings.Layout.values()[(layout.ordinal() + 1) % ContinentSettings.Layout.values().length];
            button.setMessage(layoutLabel());
            refresh();
        }).bounds(12, 38, formWidth, 20).build());
        radius = field("radius", 76, Integer.toString(draft.radius()), 6);
        count = field("count", 102, Integer.toString(draft.count()), 1);
        addRenderableWidget(Button.builder(Component.translatable("akis_islands.randomize"), button -> {
            long previous = shapeSeed;
            var random = RandomSource.create();
            do { shapeSeed = random.nextLong(); } while (shapeSeed == previous);
            refresh();
        }).bounds(12, 130, formWidth, 20)
                .tooltip(Tooltip.create(Component.translatable("akis_islands.randomize.tip"))).build());
        addRenderableWidget(Button.builder(Component.translatable(ClimateCompatibility.hasExternalController()
                        ? "akis_islands.climate.external_button" : latitudeClimate.enabled() ? "akis_islands.climate.on" : "akis_islands.climate.off"),
                button -> minecraft.setScreen(new LatitudeClimateScreen(this, latitudeClimate, value -> latitudeClimate = value,
                        new ContinentShape(draft, shapeSeed))))
                .bounds(12, 156, formWidth, 20).build());
        previewX = formWidth + 28;
        previewY = 66;
        previewSize = Math.max(32, Math.min(width - previewX - 14, height - previewY - 64));
        int buttonWidth = Math.min(150, (width - 36) / 2);
        int start = (width - 2 * buttonWidth - 8) / 2;
        apply = addRenderableWidget(Button.builder(Component.translatable("akis_islands.apply"), button -> apply())
                .bounds(start, height - 28, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
                .bounds(start + buttonWidth + 8, height - 28, buttonWidth, 20).build());
        for (Field field : fields) field.input.setResponder(value -> refresh());
        refresh();
    }

    private EditBox field(String key, int y, String value, int maxLength) {
        int inputWidth = Math.min(92, formWidth / 2);
        EditBox box = new EditBox(font, 12 + formWidth - inputWidth, y, inputWidth, 18,
                Component.translatable("akis_islands." + key));
        box.setMaxLength(maxLength);
        box.setValue(value);
        box.setTooltip(Tooltip.create(Component.translatable("akis_islands." + key + ".tip")));
        fields.add(new Field(key, box));
        return addRenderableWidget(box);
    }

    private Component layoutLabel() {
        return Component.translatable("akis_islands.layout." + layout.name().toLowerCase(Locale.ROOT));
    }

    private void refresh() {
        count.active = layout != ContinentSettings.Layout.SINGLE;
        try {
            int landmasses = layout == ContinentSettings.Layout.SINGLE ? draft.count() : Integer.parseInt(count.getValue());
            draft = new ContinentSettings(layout, Integer.parseInt(radius.getValue()), landmasses);
            shape = new ContinentShape(draft, shapeSeed);
            error = supported ? "" : "akis_islands.unsupported";
            apply.active = supported;
            rebuildPreview();
        } catch (IllegalArgumentException exception) {
            error = "akis_islands.invalid";
            apply.active = false;
        }
    }

    private void rebuildPreview() {
        releasePreview();
        NativeImage image = new NativeImage(previewSize, previewSize, false);
        double extent = shape.extent() * 1.08;
        var climate = new LatitudeClimate(latitudeClimate, shape);
        boolean showClimate = ClimateCompatibility.shouldApply(latitudeClimate);
        for (int y = 0; y < previewSize; y++) for (int x = 0; x < previewSize; x++) {
            double worldX = ((x + 0.5) / previewSize * 2 - 1) * extent;
            double worldZ = ((y + 0.5) / previewSize * 2 - 1) * extent;
            double weight = shape.landWeight(worldX, worldZ);
            int rgb = weight < 0.15 ? 0x153f66 : weight < 0.5 ? 0x367b98 : weight < 0.65 ? 0xd9c491 : 0x659668;
            if (showClimate && weight >= 0.65) {
                double temperature = climate.temperature(0, worldZ, 64);
                rgb = ClimateColors.rgb(temperature);
            }
            image.setPixelRGBA(x, y, 0xff000000 | ((rgb & 255) << 16) | (rgb & 0xff00) | ((rgb >> 16) & 255));
        }
        preview = ResourceLocation.fromNamespaceAndPath(IslandsMod.ID, "preview");
        minecraft.getTextureManager().register(preview, new DynamicTexture(image));
    }

    private void apply() {
        if (!apply.active) return;
        parent.getUiState().updateDimensions((registries, dimensions) -> {
            NoiseBasedChunkGenerator original = (NoiseBasedChunkGenerator) dimensions.overworld();
            var base = original instanceof ContinentChunkGenerator islands ? islands.baseSettings() : original.generatorSettings();
            return dimensions.replaceOverworldGenerator(registries,
                    new ContinentChunkGenerator(original.getBiomeSource(), base, draft, shapeSeed, latitudeClimate));
        });
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 12, 0xffffff);
        for (Field field : fields) {
            graphics.drawString(font, Component.translatable("akis_islands." + field.key), 12, field.input.getY() + 5, 0xd0d7dd, false);
        }
        graphics.drawWordWrap(font, Component.translatable("akis_islands.terrain"), 12, 180, formWidth, 0xa4b7c9);
        graphics.drawString(font, Component.translatable("akis_islands.preview"), previewX, 36, 0xffffff, false);
        graphics.drawString(font, Component.translatable("akis_islands.north"), previewX, 50, 0xa4b7c9, false);
        if (preview != null) {
            graphics.blit(preview, previewX, previewY, 0, 0, previewSize, previewSize, previewSize, previewSize);
            int cx = previewX + previewSize / 2, cy = previewY + previewSize / 2;
            graphics.fill(cx - 3, cy, cx + 4, cy + 1, 0xffffffff);
            graphics.fill(cx, cy - 3, cx + 1, cy + 4, 0xffffffff);
        }
        graphics.drawWordWrap(font, Component.translatable("akis_islands.preview.tip"), previewX,
                previewY + previewSize + 5, width - previewX - 12, 0xa4b7c9);
        if (!error.isEmpty()) graphics.drawCenteredString(font, Component.translatable(error), width / 2, height - 39, 0xffa077);
    }

    private void releasePreview() {
        if (preview != null && minecraft != null) {
            minecraft.getTextureManager().release(preview);
            preview = null;
        }
    }
    @Override
    public void removed() { releasePreview(); }
    @Override
    public void onClose() { minecraft.setScreen(parent); }
}
