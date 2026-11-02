package dev.aki.islands.client;

import dev.aki.islands.world.ClimateCompatibility;
import dev.aki.islands.world.ContinentShape;
import dev.aki.islands.world.LatitudeClimate;
import dev.aki.islands.world.LatitudeClimateSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** A separate editor keeps the continent screen small and the climate feature explicitly optional. */
final class LatitudeClimateScreen extends Screen {
    private final Screen parent;
    private final Consumer<LatitudeClimateSettings> save;
    private final ContinentShape shape;
    private LatitudeClimateSettings draft;
    private final List<ClimateSlider> sliders = new ArrayList<>();
    private Button presets, reset;
    private final boolean external = ClimateCompatibility.hasExternalController();

    LatitudeClimateScreen(Screen parent, LatitudeClimateSettings settings, Consumer<LatitudeClimateSettings> save, ContinentShape shape) {
        super(Component.translatable("akis_islands.climate.title"));
        this.parent = parent;
        this.draft = settings;
        this.save = save;
        this.shape = shape;
    }

    @Override
    protected void init() {
        sliders.clear();
        int w = Math.min(300, width - 32);
        int x = (width - w) / 2;
        Button toggle = addRenderableWidget(Button.builder(modeLabel(), button -> {
            draft = draft.withEnabled(!draft.enabled());
            button.setMessage(modeLabel());
            updateActive();
        }).bounds(x, 34, w, 20).tooltip(Tooltip.create(Component.translatable(external
                ? "akis_islands.climate.external.tip" : "akis_islands.climate.mode.tip"))).build());
        toggle.active = !external;
        int half = (w - 8) / 2;
        presets = addRenderableWidget(Button.builder(presetLabel(), button -> {
            var choices = LatitudeClimateSettings.Preset.values();
            var current = selectedPreset();
            var next = choices[current == null ? 0 : (current.ordinal() + 1) % choices.length];
            draft = next.apply(draft.enabled());
            syncValues();
        }).bounds(x, 60, half, 20).tooltip(Tooltip.create(Component.translatable("akis_islands.climate.preset.tip"))).build());
        reset = addRenderableWidget(Button.builder(Component.translatable("akis_islands.climate.reset"), button -> {
            draft = draft.resetValues();
            syncValues();
        }).bounds(x + half + 8, 60, w - half - 8, 20)
                .tooltip(Tooltip.create(Component.translatable("akis_islands.climate.reset.tip"))).build());
        slider("middle", x, 90, w, 50, 90, () -> draft.temperatePercent(),
                value -> draft = new LatitudeClimateSettings(draft.enabled(), value, draft.strengthPercent(), draft.transitionPercent()));
        slider("strength", x, 114, w, 0, 100, () -> draft.strengthPercent(),
                value -> draft = new LatitudeClimateSettings(draft.enabled(), draft.temperatePercent(), value, draft.transitionPercent()));
        slider("transition", x, 138, w, 50, 200, () -> draft.transitionPercent(),
                value -> draft = new LatitudeClimateSettings(draft.enabled(), draft.temperatePercent(), draft.strengthPercent(), value));
        int buttonW = Math.min(150, (width - 36) / 2);
        int start = (width - 2 * buttonW - 8) / 2;
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> {
            save.accept(external ? draft.withEnabled(false) : draft);
            onClose();
        }).bounds(start, height - 28, buttonW, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
                .bounds(start + buttonW + 8, height - 28, buttonW, 20).build());
        updateActive();
    }

    private Component modeLabel() {
        return Component.translatable(external ? "akis_islands.climate.external_button"
                : draft.enabled() ? "akis_islands.climate.on" : "akis_islands.climate.off");
    }

    private LatitudeClimateSettings.Preset selectedPreset() {
        for (var preset : LatitudeClimateSettings.Preset.values()) if (preset.matches(draft)) return preset;
        return null;
    }

    private Component presetLabel() {
        var selected = selectedPreset();
        return Component.translatable("akis_islands.climate.preset", Component.translatable("akis_islands.climate.preset."
                + (selected == null ? "custom" : selected.name().toLowerCase(Locale.ROOT))));
    }

    private void syncValues() {
        for (var slider : sliders) slider.sync();
        presets.setMessage(presetLabel());
    }

    private void slider(String key, int x, int y, int width, int min, int max, IntSupplier read, IntConsumer write) {
        var slider = new ClimateSlider(key, x, y, width, min, max, read, write);
        sliders.add(addRenderableWidget(slider));
    }

    private void updateActive() {
        for (var slider : sliders) slider.active = draft.enabled() && !external;
        presets.active = !external;
        reset.active = !external;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 12, 0xffffff);
        int w = Math.min(300, width - 32), x = (width - w) / 2, y = height - 74;
        if (external) {
            graphics.drawCenteredString(font, Component.translatable("akis_islands.climate.external"), width / 2, y + 14, 0xe6c39a);
            return;
        }
        var climate = new LatitudeClimate(draft, shape);
        for (int i = 0; i < w; i++) {
            double fraction = i / (double) Math.max(1, w - 1);
            double z = fraction < 0.5 ? (fraction * 2 - 1) * shape.northExtent()
                    : (fraction * 2 - 1) * shape.southExtent();
            double temperature = draft.enabled() && !external ? climate.temperature(0, z, 64) : 0;
            int rgb = ClimateColors.rgb(temperature);
            graphics.fill(x + i, y, x + i + 1, y + 10, 0xff000000 | rgb);
        }
        graphics.drawString(font, Component.translatable("akis_islands.climate.north"), x, y + 14, 0xc5d9e8, false);
        graphics.drawCenteredString(font, Component.translatable("akis_islands.climate.center"), width / 2, y + 14, 0xb8d5b5);
        Component south = Component.translatable("akis_islands.climate.south");
        graphics.drawString(font, south, x + w - font.width(south), y + 14, 0xe6c39a, false);
    }

    @Override
    public void onClose() { minecraft.setScreen(parent); }

    private final class ClimateSlider extends AbstractSliderButton {
        private final String key;
        private final int min, max;
        private final IntConsumer write;
        private final IntSupplier read;

        ClimateSlider(String key, int x, int y, int width, int min, int max, IntSupplier read, IntConsumer write) {
            super(x, y, width, 20, Component.empty(), (read.getAsInt() - min) / (double) (max - min));
            this.key = key;
            this.min = min;
            this.max = max;
            this.write = write;
            this.read = read;
            setTooltip(Tooltip.create(Component.translatable("akis_islands.climate." + key + ".tip")));
            updateMessage();
        }

        private int number() { return (int) Math.round(min + value * (max - min)); }

        private void sync() {
            value = (read.getAsInt() - min) / (double) (max - min);
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("akis_islands.climate." + key, number()));
        }

        @Override
        protected void applyValue() {
            write.accept(number());
            presets.setMessage(presetLabel());
        }
    }
}
