/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.themes.meteor;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.utils.BaseWidget;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.utils.render.color.Color;

public interface MeteorWidget extends BaseWidget {
    default MeteorGuiTheme theme() {
        return (MeteorGuiTheme) getTheme();
    }

    default void renderBackground(GuiRenderer renderer, WWidget widget, Color outlineColor, Color backgroundColor) {
        MeteorGuiTheme theme = theme();
        double s = theme.scale(2);
        double radius = Math.min(theme.scale(7), Math.min(widget.width, widget.height) / 2);

        renderRounded(renderer, widget.x, widget.y, widget.width, widget.height, radius, outlineColor);
        renderRounded(renderer, widget.x + s, widget.y + s, Math.max(0, widget.width - s * 2),
            Math.max(0, widget.height - s * 2), Math.max(0, radius - s), backgroundColor);
    }

    default void renderBackground(GuiRenderer renderer, WWidget widget, boolean pressed, boolean mouseOver) {
        MeteorGuiTheme theme = theme();
        renderBackground(renderer, widget, theme.outlineColor.get(pressed, mouseOver), theme.backgroundColor.get(pressed, mouseOver));
    }

    private void renderRounded(GuiRenderer renderer, double x, double y, double width, double height, double radius, Color color) {
        if (width <= 0 || height <= 0 || radius <= 0) {
            renderer.quad(x, y, Math.max(0, width), Math.max(0, height), color);
            return;
        }

        int bands = 5;
        double bandHeight = radius / bands;
        for (int i = 0; i < bands; i++) {
            double midpoint = (i + 0.5) * bandHeight;
            double circleY = radius - midpoint;
            double inset = radius - Math.sqrt(Math.max(0, radius * radius - circleY * circleY));
            renderer.quad(x + inset, y + i * bandHeight, width - inset * 2, bandHeight, color);
            renderer.quad(x + inset, y + height - (i + 1) * bandHeight, width - inset * 2, bandHeight, color);
        }
        renderer.quad(x, y + radius, width, Math.max(0, height - radius * 2), color);
    }
}
