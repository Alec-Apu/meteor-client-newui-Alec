/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.themes.meteor.widgets.pressable;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.renderer.packer.GuiTexture;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WConfirmedButton;
import meteordevelopment.meteorclient.utils.render.color.Color;

import static meteordevelopment.meteorclient.gui.themes.meteor.ModernWidgetStyle.isModuleDetails;

public class WMeteorConfirmedButton extends WConfirmedButton implements MeteorWidget {
    public WMeteorConfirmedButton(String text, String confirmText, GuiTexture texture) {
        super(text, confirmText, texture);
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

        double textHeight = theme.textHeight(true);
        height = Math.max(theme.scale(34), textHeight + theme.scale(14));
        if (text != null) {
            double regularWidth = theme.textWidth(text, text.length(), true);
            double confirmWidth = theme.textWidth(confirmText, confirmText.length(), true);
            textWidth = Math.max(regularWidth, confirmWidth);
            width = textWidth + theme.scale(28);
        } else {
            width = height;
        }
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        MeteorGuiTheme theme = theme();
        double pad = pad();

        Color outline = theme.outlineColor.get(pressed, mouseOver);
        Color fg = pressedOnce ? theme.backgroundColor.get(pressed, mouseOver) : theme.textColor.get();
        Color bg = pressedOnce ? theme.textColor.get() : theme.backgroundColor.get(pressed, mouseOver);

        renderBackground(renderer, this, outline, bg);

        String text = getText();

        if (text != null) {
            boolean material = isModuleDetails();
            double width = theme.textWidth(text, text.length(), material);
            renderer.text(text, x + this.width / 2 - width / 2,
                y + height / 2 - theme.textHeight(material) / 2, fg, material);
        } else {
            double ts = theme.textHeight(isModuleDetails());
            renderer.quad(x + width / 2 - ts / 2, y + height / 2 - ts / 2, ts, ts, texture, fg);
        }
    }
}
