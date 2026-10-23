/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.themes.meteor.widgets.input;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.input.WSlider;

import static meteordevelopment.meteorclient.gui.themes.meteor.ModernWidgetStyle.renderPill;

public class WMeteorSlider extends WSlider implements MeteorWidget {
    public WMeteorSlider(double value, double min, double max) {
        super(value, min, max);
    }

    @Override
    protected double handleSize() {
        return 0;
    }

    @Override
    protected void onCalculateSize() {
        width = theme.scale(20);
        height = theme.scale(20);
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        double valueWidth = valueWidth();

        renderBar(renderer, valueWidth);
    }

    private void renderBar(GuiRenderer renderer, double valueWidth) {
        MeteorGuiTheme theme = theme();

        double s = theme.scale(dragging || mouseOver ? 7 : 6);
        double x = this.x;
        double y = this.y + height / 2 - s / 2;

        renderPill(renderer, x, y, width, s, theme.sliderRight.get());
        if (valueWidth <= 0) return;

        renderer.scissorStart(x, y, Math.min(valueWidth, width), s);
        renderPill(renderer, x, y, width, s, theme.sliderLeft.get());
        renderer.scissorEnd();
    }
}
