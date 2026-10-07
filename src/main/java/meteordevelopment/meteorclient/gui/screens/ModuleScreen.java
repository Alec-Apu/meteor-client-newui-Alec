/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens;

import com.mojang.renderpearl.api.textures.GpuTextureView;
import meteordevelopment.meteorclient.events.meteor.ModuleBindChangedEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WKeybind;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WView;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Blur;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

import java.util.Optional;
import java.util.function.Supplier;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static meteordevelopment.meteorclient.utils.Utils.getWindowHeight;
import static meteordevelopment.meteorclient.utils.Utils.getWindowWidth;
import static com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT;

public class ModuleScreen extends WidgetScreen {
    private static final Color ACCENT = new Color(0, 245, 255);
    private static final Color DARK_MICA = new Color(0, 0, 0, 255);
    private static final Color DARK_HEADER = new Color(0, 0, 0, 118);
    private static final Color DARK_TEXT = new Color(242, 244, 252);
    private static final Color DARK_MUTED = new Color(149, 158, 183);
    private static final Color DARK_BUTTON = new Color(18, 18, 18, 255);
    private static final Color LIGHT_MICA = new Color(238, 241, 248, 255);
    private static final Color LIGHT_HEADER = new Color(250, 251, 254, 112);
    private static final Color LIGHT_TEXT = new Color(28, 31, 42);
    private static final Color LIGHT_MUTED = new Color(92, 99, 119);
    private static final Color LIGHT_BUTTON = new Color(255, 255, 255, 255);
    private static final Color DARK_ACCENT_SOFT = new Color(0, 245, 255, 42);
    private static final Color LIGHT_ACCENT_SOFT = new Color(0, 155, 170, 30);

    private final Module module;
    private WModulePanel panel;
    private WKeybind keybind;

    public ModuleScreen(GuiTheme theme, Module module) {
        super(theme, module.title);
        this.module = module;
    }

    @Override
    public void initWidgets() {
        if (theme instanceof MeteorGuiTheme meteorTheme) meteorTheme.applyModernPalette();
        panel = add(new WModulePanel()).widget();
    }

    @Override
    public void tick() {
        super.tick();
        if (panel != null && panel.visibilityChanged()) panel.refreshContent();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return !Modules.get().isBinding();
    }

    @EventHandler
    private void onModuleBindChanged(ModuleBindChangedEvent event) {
        if (keybind != null) keybind.reset();
    }

    @Override
    public boolean toClipboard() {
        CompoundTag tag = new CompoundTag();
        tag.putString("name", module.name);
        CompoundTag settingsTag = module.settings.toTag();
        if (!settingsTag.isEmpty()) tag.put("settings", settingsTag);
        return NbtUtils.toClipboard(tag);
    }

    @Override
    public boolean fromClipboard() {
        CompoundTag tag = NbtUtils.fromClipboard();
        if (tag == null || !tag.getStringOr("name", "").equals(module.name)) return false;

        Optional<CompoundTag> settings = tag.getCompound("settings");
        if (settings.isPresent()) module.settings.fromTag(settings.get());
        else module.settings.reset();

        if (parent instanceof WidgetScreen widgetScreen) widgetScreen.reload();
        if (panel != null) panel.refreshContent();
        return true;
    }

    private boolean isLight() {
        return theme instanceof MeteorGuiTheme meteorTheme && meteorTheme.modernLightMode.get();
    }

    private Color mica() { return isLight() ? LIGHT_MICA : DARK_MICA; }
    private Color header() { return isLight() ? LIGHT_HEADER : DARK_HEADER; }
    private Color text() { return isLight() ? LIGHT_TEXT : DARK_TEXT; }
    private Color muted() { return isLight() ? LIGHT_MUTED : DARK_MUTED; }
    private Color buttonColor() { return isLight() ? LIGHT_BUTTON : DARK_BUTTON; }
    private Color accentSoft() { return isLight() ? LIGHT_ACCENT_SOFT : DARK_ACCENT_SOFT; }

    public double materialSettingLabelWidth() {
        return panel != null && panel.twoColumns ? 128 : 170;
    }

    private final class WModulePanel extends WContainer {
        private WView view;
        private WBackButton back;
        private WIconButton favorite;
        private WIconButton mode;
        private WActiveButton active;
        private boolean twoColumns;

        private double plannedWidth;
        private double plannedHeight;
        private double savedX;
        private double savedY;
        private boolean positionInitialized;
        private boolean dragging;
        private double dragOffsetX;
        private double dragOffsetY;

        @Override
        public void init() {
            back = add(new WBackButton()).widget();
            favorite = add(new WIconButton(() -> module.favorite ? "★" : "☆", "Toggle favorite",
                () -> module.favorite = !module.favorite, () -> module.favorite)).widget();
            mode = add(new WIconButton(() -> isLight() ? "☀" : "☾", "Switch light and dark theme",
                this::toggleMode, () -> false)).widget();
            active = add(new WActiveButton()).widget();
            view = add(theme.view()).widget();
            view.scrollOnlyWhenMouseOver = true;
            view.hasScrollBar = true;
            view.spacing = 0;
            refreshContent();
        }

        private void toggleMode() {
            if (theme instanceof MeteorGuiTheme meteorTheme) {
                meteorTheme.modernLightMode.set(!meteorTheme.modernLightMode.get());
                meteorTheme.applyModernPalette();
                refreshContent();
            }
        }

        private void refreshContent() {
            if (view == null) return;
            view.clear();

            WWidget custom = module.getWidget(theme);
            int contentWeight = moduleContentWeight(custom != null);
            twoColumns = contentWeight > singleColumnCapacity();

            WHorizontalList columns = theme.horizontalList();
            columns.spacing = 18;
            WVerticalList left = theme.verticalList();
            WVerticalList right = theme.verticalList();
            left.spacing = 14;
            right.spacing = 14;
            columns.add(left).expandX();
            if (twoColumns) columns.add(right).expandX();

            int leftWeight = 0;
            int rightWeight = 0;
            for (SettingGroup group : module.settings.groups) {
                int visibleSettings = 0;
                for (Setting<?> setting : group) {
                    if (setting.isVisible()) visibleSettings++;
                }
                if (visibleSettings == 0) continue;

                Settings singleGroup = new Settings();
                singleGroup.groups.add(group);
                int groupWeight = visibleSettings + 1;

                boolean addLeft = !twoColumns || leftWeight <= rightWeight;
                WVerticalList target = addLeft ? left : right;
                target.add(theme.settings(singleGroup)).expandX();
                if (addLeft) leftWeight += groupWeight;
                else rightWeight += groupWeight;
            }

            boolean moduleOnLeft = !twoColumns || leftWeight <= rightWeight;
            WVerticalList moduleColumn = moduleOnLeft ? left : right;
            WSection moduleSection = moduleColumn.add(theme.section("Module", true)).expandX().widget();
            WTable moduleTable = moduleSection.add(theme.table()).expandX().widget();
            moduleTable.horizontalSpacing = 10;
            moduleTable.verticalSpacing = 6;

            moduleTable.add(theme.label("Bind", true)).minWidth(materialSettingLabelWidth()).centerY().padVertical(6);
            keybind = moduleTable.add(theme.keybind(module.keybind)).expandX().widget();
            keybind.actionOnSet = () -> Modules.get().setModuleToBind(module);
            WButton reset = moduleTable.add(theme.button(GuiRenderer.RESET)).centerY().widget();
            reset.action = keybind::resetBind;
            moduleTable.row();

            moduleTable.add(theme.label("Toggle on release", true)).minWidth(materialSettingLabelWidth()).centerY().padVertical(6);
            WCheckbox releaseToggle = moduleTable.add(theme.checkbox(module.toggleOnBindRelease)).expandCellX().right().widget();
            releaseToggle.action = () -> module.toggleOnBindRelease = releaseToggle.checked;
            moduleTable.add(theme.label("")).minWidth(30);
            moduleTable.row();

            moduleTable.add(theme.label("Chat feedback", true)).minWidth(materialSettingLabelWidth()).centerY().padVertical(6);
            WCheckbox feedbackToggle = moduleTable.add(theme.checkbox(module.chatFeedback)).expandCellX().right().widget();
            feedbackToggle.action = () -> module.chatFeedback = feedbackToggle.checked;
            moduleTable.add(theme.label("")).minWidth(30);
            moduleTable.row();

            moduleTable.add(theme.label("Configuration", true)).minWidth(materialSettingLabelWidth()).centerY().padVertical(6);
            WHorizontalList sharing = moduleTable.add(theme.horizontalList()).expandCellX().right().widget();
            sharing.spacing = 8;
            WButton copy = sharing.add(theme.button("Copy")).widget();
            copy.action = ModuleScreen.this::toClipboard;
            WButton paste = sharing.add(theme.button("Paste")).widget();
            paste.action = ModuleScreen.this::fromClipboard;
            moduleTable.add(theme.label("")).minWidth(30);

            if (moduleOnLeft) leftWeight += 5;
            else rightWeight += 5;

            if (custom != null) {
                WVerticalList customColumn = !twoColumns || leftWeight <= rightWeight ? left : right;
                customColumn.add(custom).expandX();
            }

            view.add(columns).expandX();
            invalidate();
        }

        private int moduleContentWeight(boolean hasCustomWidget) {
            int weight = 5; // Permanent Module card: header plus four rows.

            for (SettingGroup group : module.settings.groups) {
                int visibleSettings = 0;
                for (Setting<?> setting : group) {
                    if (setting.isVisible()) visibleSettings++;
                }

                if (visibleSettings > 0) weight += visibleSettings + 1;
            }

            if (hasCustomWidget) weight += 4;
            return weight;
        }

        private int singleColumnCapacity() {
            double availableHeight = getWindowHeight() * 0.52;
            return Math.max(9, (int) Math.floor(availableHeight / theme.scale(42)));
        }

        private boolean visibilityChanged() {
            for (SettingGroup group : module.settings.groups) {
                for (Setting<?> setting : group) {
                    if (setting.isVisible() != setting.lastWasVisible) return true;
                }
            }
            return false;
        }

        @Override
        public void calculateSize() {
            double maxHeight = getWindowHeight() * 0.70;
            double widthRatio = twoColumns ? 0.62 : 0.42;
            double aspectLimit = maxHeight * (twoColumns ? 4.0 / 3.0 : 1.10);
            plannedWidth = Math.min(getWindowWidth() * widthRatio, aspectLimit);
            plannedHeight = maxHeight;

            if (view != null) view.maxHeight = maxHeight - theme.scale(102);
            super.calculateSize();

            if (view != null) {
                double desiredHeight = view.height + theme.scale(102);
                plannedHeight = Mth.clamp(desiredHeight, theme.scale(210), maxHeight);
                height = plannedHeight;
            }
        }

        @Override
        protected void onCalculateSize() {
            width = plannedWidth;
            height = plannedHeight;
        }

        @Override
        protected void onCalculateWidgetPositions() {
            if (!positionInitialized) {
                savedX = getWindowWidth() / 2.0 - width / 2.0 - width * 0.10;
                savedY = getWindowHeight() / 2.0 - height / 2.0 - height * 0.10;
                positionInitialized = true;
            }
            savedX = Mth.clamp(savedX, 0, Math.max(0, getWindowWidth() - width));
            savedY = Mth.clamp(savedY, 0, Math.max(0, getWindowHeight() - height));
            x = savedX;
            y = savedY;

            double pad = theme.scale(18);
            double top = y + theme.scale(20);
            back.x = x + pad;
            back.y = top;
            favorite.x = x + width - pad - favorite.width;
            favorite.y = top;
            mode.x = favorite.x - theme.scale(8) - mode.width;
            mode.y = top;
            active.x = mode.x - theme.scale(8) - active.width;
            active.y = top;
            view.x = x + pad;
            view.y = y + theme.scale(82);
            view.width = width - pad * 2;
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            double radius = theme.scale(13);
            renderRounded(renderer, x, y, width, height, radius, mica());

            Blur blur = Modules.get().get(Blur.class);
            if (blur != null) {
                GpuTextureView texture = blur.getGuiBlurTexture();
                if (texture != null) renderRoundedTopBlur(renderer, texture, radius);
            }

            renderRoundedTop(renderer, x, y, width, theme.scale(76), radius, header());

            renderer.text(module.title, x + theme.scale(104), y + theme.scale(17), text(), true);
            double descriptionWidth = Math.max(theme.scale(90), active.x - theme.scale(14) - (x + theme.scale(104)));
            String description = fit(module.description, descriptionWidth);
            renderer.text(description, x + theme.scale(104), y + theme.scale(47), muted(), false);
        }

        private String fit(String value, double maxWidth) {
            if (theme.textWidth(value) <= maxWidth) return value;
            String result = value;
            while (!result.isEmpty() && theme.textWidth(result + "...") > maxWidth) result = result.substring(0, result.length() - 1);
            return result + "...";
        }

        private void renderRoundedTopBlur(GuiRenderer renderer, GpuTextureView texture, double radius) {
            int bands = Math.max(8, (int) Math.ceil(radius));
            double bandHeight = radius / bands;
            for (int i = 0; i < bands; i++) {
                double midpoint = (i + 0.5) * bandHeight;
                double circleY = radius - midpoint;
                double inset = radius - Math.sqrt(Math.max(0, radius * radius - circleY * circleY));
                double sy = y + i * bandHeight;
                renderer.scissorStart(x + inset, sy, width - inset * 2, bandHeight);
                renderer.texture(0, 0, getWindowWidth(), getWindowHeight(), 0, texture);
                renderer.scissorEnd();
            }

            renderer.scissorStart(x, y + radius, width, theme.scale(76) - radius);
            renderer.texture(0, 0, getWindowWidth(), getWindowHeight(), 0, texture);
            renderer.scissorEnd();
        }

        private void renderRounded(GuiRenderer renderer, double rx, double ry, double rw, double rh, double radius, Color color) {
            renderer.roundedQuad(rx, ry, rw, rh, radius, color);
        }

        private void renderRoundedTop(GuiRenderer renderer, double rx, double ry, double rw, double rh, double radius, Color color) {
            renderer.roundedTopQuad(rx, ry, rw, rh, radius, color);
        }

        @Override
        public boolean onMouseClicked(MouseButtonEvent click, boolean doubled) {
            if (click.button() == MOUSE_BUTTON_LEFT && click.x() >= x && click.x() <= x + width
                && click.y() >= y && click.y() <= y + theme.scale(76)) {
                dragging = true;
                dragOffsetX = click.x() - x;
                dragOffsetY = click.y() - y;
                return true;
            }
            return false;
        }

        @Override
        public boolean onMouseReleased(MouseButtonEvent click) {
            if (dragging && click.button() == MOUSE_BUTTON_LEFT) {
                dragging = false;
                return true;
            }
            return false;
        }

        @Override
        public void onMouseMoved(double mouseX, double mouseY, double lastMouseX, double lastMouseY) {
            if (!dragging) return;
            double nextX = Mth.clamp(mouseX - dragOffsetX, 0, Math.max(0, getWindowWidth() - width));
            double nextY = Mth.clamp(mouseY - dragOffsetY, 0, Math.max(0, getWindowHeight() - height));
            move(nextX - x, nextY - y);
            savedX = x;
            savedY = y;
        }

    }

    private final class WBackButton extends WPressable {
        @Override
        protected void onCalculateSize() {
            width = theme.scale(72);
            height = theme.scale(36);
        }

        @Override
        protected void onPressed(int button) {
            if (button == MOUSE_BUTTON_LEFT) mc.gui.setScreen(ModuleScreen.this.parent);
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            renderControl(renderer, this, buttonColor());
            String label = "← Back";
            renderer.text(label, x + width / 2 - theme.textWidth(label, label.length(), true) / 2,
                y + height / 2 - theme.textHeight(true) / 2, text(), true);
        }
    }

    private final class WIconButton extends WPressable {
        private final Supplier<String> label;
        private final String hint;
        private final Runnable action;
        private final Supplier<Boolean> highlighted;

        private WIconButton(Supplier<String> label, String hint, Runnable action, Supplier<Boolean> highlighted) {
            this.label = label;
            this.hint = hint;
            this.action = action;
            this.highlighted = highlighted;
            tooltip = hint;
        }

        @Override
        protected void onCalculateSize() {
            width = theme.scale(40);
            height = theme.scale(36);
        }

        @Override
        protected void onPressed(int button) {
            if (button == MOUSE_BUTTON_LEFT) action.run();
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            renderControl(renderer, this, highlighted.get() ? accentSoft() : buttonColor());
            String text = label.get();
            renderer.text(text, x + width / 2 - theme.textWidth(text, text.length(), true) / 2,
                y + height / 2 - theme.textHeight(true) / 2, text(), true);
        }
    }

    private final class WActiveButton extends WPressable {
        @Override
        protected void onCalculateSize() {
            width = theme.scale(104);
            height = theme.scale(36);
        }

        @Override
        protected void onPressed(int button) {
            if (button == MOUSE_BUTTON_LEFT) module.toggle();
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            renderControl(renderer, this, module.isActive() ? ACCENT : buttonColor());
            String label = module.isActive() ? "Enabled" : "Disabled";
            renderer.text(label, x + width / 2 - theme.textWidth(label, label.length(), true) / 2,
                y + height / 2 - theme.textHeight(true) / 2, module.isActive() ? Color.WHITE : text(), true);
        }
    }

    private void renderControl(GuiRenderer renderer, WWidget widget, Color color) {
        double radius = theme.scale(12);
        renderer.roundedQuad(widget.x, widget.y, widget.width, widget.height, radius, color);
    }
}
