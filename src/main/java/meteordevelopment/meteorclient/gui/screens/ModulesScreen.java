/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens;

import com.mojang.blaze3d.platform.MacosUtil;
import com.mojang.blaze3d.textures.GpuTextureView;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.Tabs;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.utils.Cell;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WView;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WPressable;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.Blur;
import meteordevelopment.meteorclient.utils.misc.NbtUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.Mth;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static meteordevelopment.meteorclient.utils.Utils.getWindowHeight;
import static meteordevelopment.meteorclient.utils.Utils.getWindowWidth;
import static org.lwjgl.glfw.GLFW.*;

public class ModulesScreen extends TabScreen {
    private WModernPanel panel;

    public ModulesScreen(GuiTheme theme) {
        super(theme, Tabs.get().getFirst());
    }

    @Override
    public void initWidgets() {
        panel = add(new WModernPanel()).widget();
    }

    @Override
    public boolean keyPressed(@NonNull KeyEvent value) {
        if (locked) return false;

        boolean control = MacosUtil.IS_MACOS ? value.modifiers() == GLFW_MOD_SUPER : value.modifiers() == GLFW_MOD_CONTROL;
        if (control && value.key() == GLFW_KEY_F && panel != null) {
            panel.focusSearch();
            return true;
        }

        return super.keyPressed(value);
    }

    @Override
    public boolean toClipboard() {
        return NbtUtils.toClipboard(Modules.get());
    }

    @Override
    public boolean fromClipboard() {
        return NbtUtils.fromClipboard(Modules.get());
    }

    private final class WModernPanel extends WContainer {
        private static final Color ACCENT = new Color(0, 245, 255);
        private static final Color SELECTED_DARK = new Color(92, 96, 104, 92);
        private static final Color SELECTED_LIGHT = new Color(110, 116, 126, 54);

        private static final Color DARK_MICA = new Color(0, 0, 0, 255);
        private static final Color DARK_SIDEBAR = new Color(0, 0, 0, 179);
        private static final Color DARK_CARD = new Color(15, 15, 15, 255);
        private static final Color DARK_CARD_HOVER = new Color(28, 28, 28, 255);
        private static final Color DARK_CARD_ACTIVE = new Color(38, 40, 42, 255);
        private static final Color DARK_TEXT = new Color(242, 244, 252);
        private static final Color DARK_MUTED = new Color(149, 158, 183);
        private static final Color DARK_LINE = new Color(133, 145, 184, 42);
        private static final Color DARK_SWITCH = new Color(70, 77, 98);
        private static final Color DARK_SHADOW = new Color(0, 0, 0, 105);

        private static final Color LIGHT_MICA = new Color(238, 241, 248, 255);
        private static final Color LIGHT_SIDEBAR = new Color(246, 248, 252, 179);
        private static final Color LIGHT_CARD = new Color(250, 251, 254, 255);
        private static final Color LIGHT_CARD_HOVER = new Color(255, 255, 255, 255);
        private static final Color LIGHT_CARD_ACTIVE = new Color(237, 239, 242, 255);
        private static final Color LIGHT_TEXT = new Color(28, 31, 42);
        private static final Color LIGHT_MUTED = new Color(92, 99, 119);
        private static final Color LIGHT_LINE = new Color(69, 76, 102, 38);
        private static final Color LIGHT_SWITCH = new Color(175, 180, 194);
        private static final Color LIGHT_SHADOW = new Color(27, 32, 48, 72);

        private final List<NavigationItem> navigation = new ArrayList<>();
        private final List<WCategoryButton> categoryButtons = new ArrayList<>();

        private NavigationItem selected;
        private WTextBox search;
        private WModeToggle modeToggle;
        private WView moduleView;

        private double plannedWidth;
        private double plannedHeight;
        private double plannedSidebarWidth;
        private double savedX;
        private double savedY;
        private boolean positionInitialized;
        private boolean dragging;
        private double dragOffsetX;
        private double dragOffsetY;
        private boolean fallbackLightMode;

        @Override
        public void init() {
            if (theme instanceof MeteorGuiTheme meteorTheme) meteorTheme.applyModernPalette();
            buildNavigation();

            search = add(theme.textBox("", "Search modules...")).widget();
            search.action = this::refreshModules;
            modeToggle = add(new WModeToggle()).widget();

            for (NavigationItem item : navigation) {
                WCategoryButton button = add(new WCategoryButton(item)).widget();
                categoryButtons.add(button);
            }

            moduleView = add(theme.view()).widget();
            moduleView.scrollOnlyWhenMouseOver = true;
            moduleView.hasScrollBar = true;
            moduleView.spacing = 10;
            refreshModules();

            // The first root layout is still being assembled while this widget is initialized.
            // Rebuild once after that frame so the default Combat page receives final view bounds.
            ModulesScreen.this.taskAfterRender = this::refreshModules;
        }

        private void buildNavigation() {
            String[] order = { "Combat", "Player", "Movement", "Render", "World", "Misc" };
            for (String name : order) {
                for (Category category : Modules.loopCategories()) {
                    if (category.name.equalsIgnoreCase(name)
                        && Modules.get().getGroup(category).stream().anyMatch(this::isVisible)) {
                        navigation.add(new NavigationItem(category.name, category, false));
                        break;
                    }
                }
            }

            navigation.add(new NavigationItem("All Modules", null, false));
            navigation.add(new NavigationItem("Favorites", null, true));

            selected = navigation.getFirst();
        }

        private boolean isVisible(Module module) {
            return !Config.get().hiddenModules.get().contains(module);
        }

        private boolean isLight() {
            if (theme instanceof MeteorGuiTheme meteorTheme) return meteorTheme.modernLightMode.get();
            return fallbackLightMode;
        }

        private void toggleMode() {
            if (theme instanceof MeteorGuiTheme meteorTheme) {
                meteorTheme.modernLightMode.set(!meteorTheme.modernLightMode.get());
                meteorTheme.applyModernPalette();
            } else fallbackLightMode = !fallbackLightMode;
        }

        private Color mica() { return isLight() ? LIGHT_MICA : DARK_MICA; }
        private Color sidebar() { return isLight() ? LIGHT_SIDEBAR : DARK_SIDEBAR; }
        private Color card() { return isLight() ? LIGHT_CARD : DARK_CARD; }
        private Color cardHover() { return isLight() ? LIGHT_CARD_HOVER : DARK_CARD_HOVER; }
        private Color cardActive() { return isLight() ? LIGHT_CARD_ACTIVE : DARK_CARD_ACTIVE; }
        private Color text() { return isLight() ? LIGHT_TEXT : DARK_TEXT; }
        private Color muted() { return isLight() ? LIGHT_MUTED : DARK_MUTED; }
        private Color line() { return isLight() ? LIGHT_LINE : DARK_LINE; }
        private Color switchOff() { return isLight() ? LIGHT_SWITCH : DARK_SWITCH; }
        private Color shadow() { return isLight() ? LIGHT_SHADOW : DARK_SHADOW; }
        private Color selectedBackground() { return isLight() ? SELECTED_LIGHT : SELECTED_DARK; }

        private void select(NavigationItem item) {
            if (selected == item) return;
            selected = item;
            refreshModules();
        }

        private void refreshModules() {
            if (moduleView == null) return;
            moduleView.clear();

            String filter = search == null ? "" : search.get().trim().toLowerCase(Locale.ROOT);
            List<Module> modules = new ArrayList<>();

            for (Module module : Modules.get().getAll()) {
                if (!isVisible(module)) continue;
                if (selected.favorite && !module.favorite) continue;
                if (selected.category != null && !selected.category.equals(module.category)) continue;
                if (!filter.isEmpty()
                    && !module.title.toLowerCase(Locale.ROOT).contains(filter)
                    && !module.description.toLowerCase(Locale.ROOT).contains(filter)) continue;
                modules.add(module);
            }

            modules.sort(Comparator.comparing(module -> module.title, String.CASE_INSENSITIVE_ORDER));
            for (int i = 0; i < modules.size(); i += 2) {
                WHorizontalList row = new WCardRow();
                row.theme = theme;
                row.spacing = 10;
                row.add(new WModuleCard(modules.get(i))).expandX();
                if (i + 1 < modules.size()) row.add(new WModuleCard(modules.get(i + 1))).expandX();
                moduleView.add(row).expandX();
            }
            invalidate();
        }

        private void focusSearch() {
            search.setFocused(true);
            search.setCursorMax();
        }

        @Override
        public void calculateSize() {
            updatePlannedSize();
            if (moduleView != null) moduleView.maxHeight = contentHeight();
            super.calculateSize();
        }

        private void updatePlannedSize() {
            double maxWidth = getWindowWidth() * 0.52;
            double maxHeight = getWindowHeight() * 0.56;
            double baseWidth = Math.min(maxWidth, maxHeight * 4.0 / 3.0);
            double baseHeight = baseWidth * 3.0 / 4.0;
            double baseSidebar = Math.max(theme.scale(135), baseWidth * 0.236);
            double baseContent = baseWidth - baseSidebar - theme.scale(32);
            plannedWidth = Math.min(getWindowWidth() * 0.92, baseWidth + baseContent * 0.10);
            plannedHeight = baseHeight;
            plannedSidebarWidth = baseSidebar;
        }

        @Override
        protected void onCalculateSize() {
            width = plannedWidth;
            height = plannedHeight;
        }

        private double sidebarWidth() { return plannedSidebarWidth; }
        private double outerPad() { return theme.scale(16); }
        private double headerHeight() { return theme.scale(63); }
        private double contentWidth() { return width - sidebarWidth() - outerPad() * 2; }
        private double contentHeight() { return height - headerHeight(); }
        private double cardWidth() { return (contentWidth() - theme.scale(18)) / 2.0; }
        private double cardHeight() { return theme.scale(54); }

        @Override
        protected void onCalculateWidgetPositions() {
            if (!positionInitialized) {
                savedX = getWindowWidth() / 2.0 - width / 2.0 - width * 0.14;
                savedY = getWindowHeight() / 2.0 - height / 2.0 - height * 0.13;
                positionInitialized = true;
            }

            savedX = Mth.clamp(savedX, 0, Math.max(0, getWindowWidth() - width));
            savedY = Mth.clamp(savedY, 0, Math.max(0, getWindowHeight() - height));
            x = savedX;
            y = savedY;

            double sidebar = sidebarWidth();
            double pad = outerPad();
            double toggleWidth = theme.scale(55);

            modeToggle.x = x + width - pad - toggleWidth;
            modeToggle.y = y + theme.scale(17);
            modeToggle.width = toggleWidth;
            search.x = modeToggle.x - theme.scale(10) - theme.scale(156);
            search.y = y + theme.scale(17);
            search.width = theme.scale(156);

            double buttonX = x + theme.scale(12);
            double buttonY = y + theme.scale(76);
            double buttonWidth = sidebar - theme.scale(24);
            for (WCategoryButton button : categoryButtons) {
                if (button.item.category == null && !button.item.favorite) buttonY += theme.scale(15);
                button.x = buttonX;
                button.y = buttonY;
                button.width = buttonWidth;
                buttonY += button.height + theme.scale(3);
            }

            moduleView.x = x + sidebar + pad;
            moduleView.y = y + headerHeight();
            moduleView.width = contentWidth();
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            double radius = theme.scale(13);
            renderRounded(renderer, x, y, width, height, radius, mica());

            Blur blur = Modules.get().get(Blur.class);
            if (blur != null) {
                GpuTextureView texture = blur.getGuiBlurTexture();
                if (texture != null) renderRoundedLeftBlur(renderer, texture, radius);
            }

            renderRoundedLeft(renderer, x, y, sidebarWidth(), height, radius, sidebar());
            renderer.quad(x + sidebarWidth(), y + radius, theme.scale(1), height - radius * 2, line());

            renderer.text("Alec", x + theme.scale(17), y + theme.scale(17), text(), true);
            renderer.text("Minecraft", x + theme.scale(17), y + theme.scale(42), ACCENT, false);
            double titleX = x + sidebarWidth() + outerPad();
            renderer.text(selected.title, titleX, y + theme.scale(17), text(), true);
            renderer.text(moduleCountText(), titleX, y + theme.scale(42), muted(), false);
        }

        private void renderRoundedLeftBlur(GuiRenderer renderer, GpuTextureView texture, double radius) {
            renderRoundedBands((sx, sy, sw, sh) -> {
                double clipWidth = x + sidebarWidth() - sx;
                renderer.scissorStart(sx, sy, clipWidth, sh);
                renderer.texture(0, 0, getWindowWidth(), getWindowHeight(), 0, texture);
                renderer.scissorEnd();
            }, x, y, sidebarWidth(), height, radius);
        }

        private void renderRounded(GuiRenderer renderer, double rx, double ry, double rw, double rh, double radius, Color color) {
            renderRoundedBands((sx, sy, sw, sh) -> renderer.quad(sx, sy, sw, sh, color), rx, ry, rw, rh, radius);
        }

        private void renderRoundedLeft(GuiRenderer renderer, double rx, double ry, double rw, double rh, double radius, Color color) {
            renderRoundedBands((sx, sy, sw, sh) -> renderer.quad(sx, sy, Math.max(0, rx + rw - sx), sh, color), rx, ry, rw, rh, radius);
        }

        private void renderRoundedBands(BandRenderer renderer, double rx, double ry, double rw, double rh, double radius) {
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

        private String moduleCountText() {
            int count = 0;
            String filter = search == null ? "" : search.get().trim().toLowerCase(Locale.ROOT);
            for (Module module : Modules.get().getAll()) {
                if (!isVisible(module)) continue;
                if (selected.favorite && !module.favorite) continue;
                if (selected.category != null && !selected.category.equals(module.category)) continue;
                if (!filter.isEmpty()
                    && !module.title.toLowerCase(Locale.ROOT).contains(filter)
                    && !module.description.toLowerCase(Locale.ROOT).contains(filter)) continue;
                count++;
            }
            return count + (count == 1 ? " module" : " modules");
        }

        @Override
        public boolean onMouseClicked(MouseButtonEvent click, boolean doubled) {
            if (click.button() == GLFW_MOUSE_BUTTON_LEFT && click.x() >= x && click.x() <= x + width
                && click.y() >= y && click.y() <= y + headerHeight()) {
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

        private final class WModeToggle extends WPressable {
            @Override
            protected void onCalculateSize() {
                width = theme.scale(55);
                height = theme.scale(28);
                tooltip = "Switch between light and dark Mica";
            }

            @Override
            protected void onPressed(int button) {
                if (button == GLFW_MOUSE_BUTTON_LEFT) toggleMode();
            }

            @Override
            protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
                renderRounded(renderer, x, y, width, height, theme.scale(8), isLight() ? LIGHT_CARD : DARK_CARD);
                String label = isLight() ? "Light" : "Dark";
                renderer.text(label, x + width / 2 - theme.textWidth(label) / 2, y + height / 2 - theme.textHeight() / 2, text(), false);
            }
        }

        private final class WCategoryButton extends WPressable {
            private final NavigationItem item;
            private WCategoryButton(NavigationItem item) { this.item = item; }

            @Override
            protected void onCalculateSize() {
                width = theme.scale(105);
                height = theme.scale(34);
            }

            @Override
            protected void onPressed(int button) {
                if (button == GLFW_MOUSE_BUTTON_LEFT) select(item);
            }

            @Override
            protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
                boolean active = selected == item;
                if (item.category == null && !item.favorite) {
                    renderer.quad(x, y - theme.scale(9), width, theme.scale(1), line());
                }
                if (active || mouseOver) renderRounded(renderer, x, y, width, height, theme.scale(6), active ? selectedBackground() : line());
                if (active) renderer.quad(x, y + theme.scale(6), theme.scale(2), height - theme.scale(12), ACCENT);
                renderer.text(item.title, x + theme.scale(10), y + height / 2 - theme.textHeight() / 2, active ? text() : muted(), false);
            }
        }

        private final class WCardRow extends WHorizontalList {
            @Override
            protected void onCalculateSize() {
                width = contentWidth() - theme.scale(8);
                height = cardHeight();
                calculatedWidth = 0;
                fillXCount = cells.size();
            }

            @Override
            protected void onCalculateWidgetPositions() {
                double gap = theme.scale(10);
                double available = width - gap * Math.max(0, cells.size() - 1);
                double cellWidth = cells.size() == 1 ? cardWidth() : available / cells.size();
                double cellX = x;
                for (Cell<?> cell : cells) {
                    cell.x = cellX;
                    cell.y = y;
                    cell.width = cellWidth;
                    cell.height = height;
                    cell.alignWidget();
                    cellX += cellWidth + gap;
                }
            }
        }

        private final class WModuleCard extends WPressable {
            private final Module module;
            private WModuleCard(Module module) {
                this.module = module;
                tooltip = module.description;
            }

            @Override
            protected void onCalculateSize() {
                width = cardWidth();
                height = cardHeight();
            }

            @Override
            protected void onPressed(int button) {
                if (button == GLFW_MOUSE_BUTTON_LEFT) module.toggle();
                else if (button == GLFW_MOUSE_BUTTON_RIGHT) mc.gui.setScreen(theme.moduleScreen(module));
            }

            @Override
            protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
                renderRounded(renderer, x, y, width, height, theme.scale(9), module.isActive() ? cardActive() : mouseOver ? cardHover() : card());
                double pad = theme.scale(10);
                renderer.text(module.title, x + pad, y + theme.scale(8), text(), false);
                String description = fit(module.description, width - pad * 2 - theme.scale(42));
                renderer.text(description, x + pad, y + theme.scale(31), muted(), false);

                double switchWidth = theme.scale(31);
                double switchHeight = theme.scale(15.4);
                double switchX = x + width - pad - switchWidth;
                double switchY = y + height / 2 - switchHeight / 2;
                renderRounded(renderer, switchX, switchY, switchWidth, switchHeight, switchHeight / 2, module.isActive() ? ACCENT : switchOff());
                double knob = switchHeight - theme.scale(4);
                double knobX = module.isActive() ? switchX + switchWidth - knob - theme.scale(2) : switchX + theme.scale(2);
                renderRounded(renderer, knobX, switchY + theme.scale(2), knob, knob, knob / 2, isLight() ? Color.WHITE : DARK_TEXT);
            }

            private String fit(String value, double maxWidth) {
                if (theme.textWidth(value) <= maxWidth) return value;
                String result = value;
                while (!result.isEmpty() && theme.textWidth(result + "...") > maxWidth) {
                    result = result.substring(0, result.length() - 1);
                }
                return result + "...";
            }

            private List<String> wrap(String value, double maxWidth, int maxLines) {
                List<String> lines = new ArrayList<>();
                StringBuilder line = new StringBuilder();
                for (String word : value.split("\\s+")) {
                    String candidate = line.isEmpty() ? word : line + " " + word;
                    if (theme.textWidth(candidate) <= maxWidth) {
                        line.setLength(0);
                        line.append(candidate);
                    } else {
                        if (!line.isEmpty()) lines.add(line.toString());
                        line.setLength(0);
                        line.append(word);
                        if (lines.size() == maxLines) break;
                    }
                }
                if (!line.isEmpty() && lines.size() < maxLines) lines.add(line.toString());
                if (lines.size() == maxLines && theme.textWidth(lines.getLast() + "...") <= maxWidth) {
                    lines.set(maxLines - 1, lines.getLast() + "...");
                }
                return lines;
            }
        }

        @FunctionalInterface
        private interface BandRenderer {
            void render(double x, double y, double width, double height);
        }
    }

    private record NavigationItem(String title, Category category, boolean favorite) {
    }
}
