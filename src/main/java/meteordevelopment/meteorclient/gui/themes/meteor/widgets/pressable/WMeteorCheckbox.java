/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.themes.meteor.widgets.pressable;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WCheckbox;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.util.Mth;

import static meteordevelopment.meteorclient.gui.themes.meteor.ModernWidgetStyle.isModuleDetails;
import static meteordevelopment.meteorclient.gui.themes.meteor.ModernWidgetStyle.renderPill;

public class WMeteorCheckbox extends WCheckbox implements MeteorWidget {
    private double animProgress;

    public WMeteorCheckbox(boolean checked) {
        super(checked);
        animProgress = checked ? 1 : 0;
    }

    @Override
    protected void onCalculateSize() {
        if (isModuleDetails()) {
            width = theme.scale(42);
            height = theme.scale(24);
            return;
        }

        super.onCalculateSize();
        height *= 1.10;
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        MeteorGuiTheme theme = theme();

        animProgress += (checked ? 1 : -1) * delta * 14;
        animProgress = Mth.clamp(animProgress, 0, 1);

        if (isModuleDetails()) {
            double knob = theme.scale(18);
            double inset = (height - knob) / 2;
            renderPill(renderer, x, y, width, height, checked ? theme.checkboxColor.get() : theme.sliderRight.get());
            renderer.roundedQuad(x + inset + (width - knob - inset * 2) * animProgress, y + inset, knob, knob,
                knob / 2, Color.WHITE);
            return;
        }

        renderBackground(renderer, this, pressed, mouseOver);

        if (animProgress > 0) {
            double cs = (width - theme.scale(2)) / 1.75 * animProgress;
            renderer.quad(x + (width - cs) / 2, y + (height - cs) / 2, cs, cs, theme.checkboxColor.get());
        }
    }
}
