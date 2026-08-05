/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens;

import com.mojang.blaze3d.textures.GpuTextureView;
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

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static meteordevelopment.meteorclient.utils.Utils.getWindowHeight;
import static meteordevelopment.meteorclient.utils.Utils.getWindowWidth;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;

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

    private final class WModulePanel extends WContainer {
        private WView view;
        private WBackButton back;
        private WSmallButton favorite;
        private WSmallButton mode;
        private WActiveButton active;

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
            favorite = add(new WSmallButton("Fav", () -> module.favorite = !module.favorite)).widget();
            mode = add(new WSmallButton("Mode", this::toggleMode)).widget();
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

            WHorizontalList columns = theme.horizontalList();
            columns.spacing = 12;
            WVerticalList left = theme.verticalList();
            WVerticalList right = theme.verticalList();
            left.spacing = 10;
            right.spacing = 10;
            columns.add(left).expandX();
            columns.add(right).expandX();

            int index = 0;
            for (SettingGroup group : module.settings.groups) {
                Settings singleGroup = new Settings();
                singleGroup.groups.add(group);
                WVerticalList target = (index++ & 1) == 0 ? left : right;
                target.add(theme.settings(singleGroup)).expandX();
            }

            WSection moduleSection = theme.section("Module", true);
            WHorizontalList bind = moduleSection.add(theme.horizontalList()).expandX().widget();
            bind.add(theme.label("Bind"));
            keybind = bind.add(theme.keybind(module.keybind)).expandX().widget();
            keybind.actionOnSet = () -> Modules.get().setModuleToBind(module);
            WButton reset = bind.add(theme.button(GuiRenderer.RESET)).widget();
            reset.action = keybind::resetBind;

            WHorizontalList release = moduleSection.add(theme.horizontalList()).expandX().widget();
            release.add(theme.label("Toggle on release")).expandCellX();
            WCheckbox releaseToggle = release.add(theme.checkbox(module.toggleOnBindRelease)).widget();
            releaseToggle.action = () -> module.toggleOnBindRelease = releaseToggle.checked;

            WHorizontalList feedback = moduleSection.add(theme.horizontalList()).expandX().widget();
            feedback.add(theme.label("Chat feedback")).expandCellX();
            WCheckbox feedbackToggle = feedback.add(theme.checkbox(module.chatFeedback)).widget();
            feedbackToggle.action = () -> module.chatFeedback = feedbackToggle.checked;

            WHorizontalList sharing = moduleSection.add(theme.horizontalList()).expandX().widget();
            WButton copy = sharing.add(theme.button("Copy config")).expandX().widget();
            copy.action = ModuleScreen.this::toClipboard;
            WButton paste = sharing.add(theme.button("Paste config")).expandX().widget();
            paste.action = ModuleScreen.this::fromClipboard;
            right.add(moduleSection).expandX();

            WWidget custom = module.getWidget(theme);
            if (custom != null) left.add(custom).expandX();

            view.add(columns).expandX();
            invalidate();
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
            double maxWidth = getWindowWidth() * 0.56;
            double maxHeight = getWindowHeight() * 0.62;
            plannedWidth = Math.min(maxWidth, maxHeight * 4.0 / 3.0);
            plannedHeight = plannedWidth * 3.0 / 4.0;
            if (view != null) view.maxHeight = plannedHeight - theme.scale(80);
            super.calculateSize();
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

            double pad = theme.scale(15);
            double top = y + theme.scale(16);
            back.x = x + pad;
            back.y = top;
            favorite.x = x + width - pad - theme.scale(42);
            favorite.y = top;
            mode.x = favorite.x - theme.scale(50);
            mode.y = top;
            active.x = mode.x - theme.scale(89);
            active.y = top;
            view.x = x + pad;
            view.y = y + theme.scale(66);
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

            renderRoundedTop(renderer, x, y, width, theme.scale(62), radius, header());

            renderer.text(module.title, x + theme.scale(80), y + theme.scale(15), text(), true);
            String description = fit(module.description, width * 0.42);
            renderer.text(description, x + theme.scale(80), y + theme.scale(41), muted(), false);
        }

        private String fit(String value, double maxWidth) {
            if (theme.textWidth(value) <= maxWidth) return value;
            String result = value;
            while (!result.isEmpty() && theme.textWidth(result + "...") > maxWidth) result = result.substring(0, result.length() - 1);
            return result + "...";
        }

        private void renderRoundedTopBlur(GuiRenderer renderer, GpuTextureView texture, double radius) {
            int bands = 5;
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

            renderer.scissorStart(x, y + radius, width, theme.scale(62) - radius);
            renderer.texture(0, 0, getWindowWidth(), getWindowHeight(), 0, texture);
            renderer.scissorEnd();
        }

        private void renderRounded(GuiRenderer renderer, double rx, double ry, double rw, double rh, double radius, Color color) {
            renderBands((sx, sy, sw, sh) -> renderer.quad(sx, sy, sw, sh, color), rx, ry, rw, rh, radius);
        }

        private void renderRoundedTop(GuiRenderer renderer, double rx, double ry, double rw, double rh, double radius, Color color) {
            int bands = 5;
            double bandHeight = radius / bands;
            for (int i = 0; i < bands; i++) {
                double midpoint = (i + 0.5) * bandHeight;
                double circleY = radius - midpoint;
                double inset = radius - Math.sqrt(Math.max(0, radius * radius - circleY * circleY));
                renderer.quad(rx + inset, ry + i * bandHeight, rw - inset * 2, bandHeight, color);
            }
            renderer.quad(rx, ry + radius, rw, rh - radius, color);
        }

        private void renderBands(BandRenderer renderer, double rx, double ry, double rw, double rh, double radius) {
            int bands = 5;
            double bandHeight = radius / bands;
            for (int i = 0; i < bands; i++) {
                double midpoint = (i + 0.5) * bandHeight;
                double circleY = radius - midpoint;
                double inset = radius - Math.sqrt(Math.max(0, radius * radius - circleY * circleY));
                renderer.render(rx + inset, ry + i * bandHeight, rw - inset * 2, bandHeight);
                renderer.render(rx + inset, ry + rh - (i + 1) * bandHeight, rw - inset * 2, bandHeight);
            }
            renderer.render(rx, ry + radius, rw, Math.max(0, rh - radius * 2));
        }

        @Override
        public boolean onMouseClicked(MouseButtonEvent click, boolean doubled) {
            if (click.button() == GLFW_MOUSE_BUTTON_LEFT && click.x() >= x && click.x() <= x + width
                && click.y() >= y && click.y() <= y + theme.scale(62)) {
                dragging = true;
                dragOffsetX = click.x() - x;
                dragOffsetY = click.y() - y;
                return true;
            }
            return false;
        }

        @Override
        public boolean onMouseReleased(MouseButtonEvent click) {
            if (dragging && click.button() == GLFW_MOUSE_BUTTON_LEFT) {
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

        @FunctionalInterface
        private interface BandRenderer {
            void render(double x, double y, double width, double height);
        }
    }

    private final class WBackButton extends WPressable {
        @Override
        protected void onCalculateSize() {
            width = theme.scale(54);
            height = theme.scale(28);
        }

        @Override
        protected void onPressed(int button) {
            if (button == GLFW_MOUSE_BUTTON_LEFT) mc.gui.setScreen(ModuleScreen.this.parent);
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            renderControl(renderer, this, buttonColor());
            renderer.text("< Back", x + theme.scale(8), y + height / 2 - theme.textHeight() / 2, text(), false);
        }
    }

    private final class WSmallButton extends WPressable {
        private final String label;
        private final Runnable action;
        private WSmallButton(String label, Runnable action) {
            this.label = label;
            this.action = action;
        }

        @Override
        protected void onCalculateSize() {
            width = theme.scale(42);
            height = theme.scale(28);
        }

        @Override
        protected void onPressed(int button) {
            if (button == GLFW_MOUSE_BUTTON_LEFT) action.run();
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            renderControl(renderer, this, buttonColor());
            renderer.text(label, x + width / 2 - theme.textWidth(label) / 2, y + height / 2 - theme.textHeight() / 2, text(), false);
        }
    }

    private final class WActiveButton extends WPressable {
        @Override
        protected void onCalculateSize() {
            width = theme.scale(81);
            height = theme.scale(28);
        }

        @Override
        protected void onPressed(int button) {
            if (button == GLFW_MOUSE_BUTTON_LEFT) module.toggle();
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            renderControl(renderer, this, module.isActive() ? ACCENT : buttonColor());
            String label = module.isActive() ? "Enabled" : "Disabled";
            renderer.text(label, x + width / 2 - theme.textWidth(label) / 2, y + height / 2 - theme.textHeight() / 2,
                module.isActive() ? Color.WHITE : text(), false);
        }
    }

    private void renderControl(GuiRenderer renderer, WWidget widget, Color color) {
        double radius = theme.scale(7);
        int bands = 5;
        double bandHeight = radius / bands;
        for (int i = 0; i < bands; i++) {
            double midpoint = (i + 0.5) * bandHeight;
            double circleY = radius - midpoint;
            double inset = radius - Math.sqrt(Math.max(0, radius * radius - circleY * circleY));
            renderer.quad(widget.x + inset, widget.y + i * bandHeight, widget.width - inset * 2, bandHeight, color);
            renderer.quad(widget.x + inset, widget.y + widget.height - (i + 1) * bandHeight, widget.width - inset * 2, bandHeight, color);
        }
        renderer.quad(widget.x, widget.y + radius, widget.width, Math.max(0, widget.height - radius * 2), color);
    }
}
