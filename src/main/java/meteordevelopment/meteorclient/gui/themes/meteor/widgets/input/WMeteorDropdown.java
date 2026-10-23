/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.themes.meteor.widgets.input;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.input.WDropdown;
import meteordevelopment.meteorclient.utils.render.color.Color;

import static meteordevelopment.meteorclient.gui.themes.meteor.ModernWidgetStyle.isModuleDetails;

public class WMeteorDropdown<T> extends WDropdown<T> implements MeteorWidget {
    public WMeteorDropdown(T[] values, T value) {
        super(values, value);
    }

    @Override
    public double pad() {
        return isModuleDetails() ? theme.scale(8) : super.pad();
    }

    @Override
    protected void onCalculateSize() {
        if (!isModuleDetails()) {
            super.onCalculateSize();
            return;
        }

        double pad = pad();
        maxValueWidth = 0;
        for (T value : values) {
            String text = meteordevelopment.meteorclient.utils.misc.Localization.value(value);
            maxValueWidth = Math.max(maxValueWidth, theme.textWidth(text, text.length(), true));
        }

        root.calculateSize();
        double textHeight = theme.textHeight(true);
        width = pad + maxValueWidth + pad + textHeight + pad;
        height = Math.max(theme.scale(34), pad + textHeight + pad);
        root.width = width;
    }

    @Override
    protected WDropdownRoot createRootWidget() {
        return new WRoot();
    }

    @Override
    protected WDropdownValue createValueWidget() {
        return new WValue();
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        MeteorGuiTheme theme = theme();
        double pad = pad();
        boolean material = isModuleDetails();
        double s = theme.textHeight(material);

        renderBackground(renderer, this, pressed, mouseOver);

        String text = meteordevelopment.meteorclient.utils.misc.Localization.value(get());
        double w = theme.textWidth(text, text.length(), material);
        renderer.text(text, x + pad + maxValueWidth / 2 - w / 2, y + height / 2 - s / 2,
            theme.textColor.get(), material);

        if (material) {
            double centerX = x + width - pad - theme.scale(5);
            double centerY = y + height / 2;
            double chevron = theme.scale(5);
            renderer.line(centerX - chevron, centerY - chevron / 2, centerX, centerY + chevron / 2,
                theme.textSecondaryColor.get());
            renderer.line(centerX, centerY + chevron / 2, centerX + chevron, centerY - chevron / 2,
                theme.textSecondaryColor.get());
        } else {
            renderer.rotatedQuad(x + pad + maxValueWidth + pad, y + height / 2 - s / 2, s, s, 0,
                GuiRenderer.TRIANGLE, theme.textColor.get());
        }
    }

    private static class WRoot extends WDropdownRoot implements MeteorWidget {
        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            MeteorGuiTheme theme = theme();
            renderBackground(renderer, this, theme.outlineColor.get(), theme.backgroundColor.get());
        }
    }

    private class WValue extends WDropdownValue implements MeteorWidget {
        @Override
        protected void onCalculateSize() {
            double pad = pad();
            boolean material = isModuleDetails();
            String text = meteordevelopment.meteorclient.utils.misc.Localization.value(value);

            width = pad + theme.textWidth(text, text.length(), material) + pad;
            height = material ? Math.max(theme.scale(34), pad + theme.textHeight(true) + pad)
                : pad + theme.textHeight() + pad;
        }

        @Override
        protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
            MeteorGuiTheme theme = theme();

            Color color = theme.backgroundColor.get(pressed, mouseOver, true);
            int preA = color.a;
            color.a += color.a / 2;
            color.validate();

            renderBackground(renderer, this, theme.outlineColor.get(false, mouseOver), color);

            color.a = preA;

            String text = meteordevelopment.meteorclient.utils.misc.Localization.value(value);
            boolean material = isModuleDetails();
            double textWidth = theme.textWidth(text, text.length(), material);
            double textHeight = theme.textHeight(material);
            renderer.text(text, x + width / 2 - textWidth / 2, y + height / 2 - textHeight / 2,
                theme.textColor.get(), material);
        }
    }
}
