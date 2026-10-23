/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.themes.meteor.widgets.pressable;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.renderer.packer.GuiTexture;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorGuiTheme;
import meteordevelopment.meteorclient.gui.themes.meteor.MeteorWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;

import static meteordevelopment.meteorclient.gui.themes.meteor.ModernWidgetStyle.isModuleDetails;

public class WMeteorButton extends WButton implements MeteorWidget {
    public WMeteorButton(String text, GuiTexture texture) {
        super(text, texture);
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
            textWidth = theme.textWidth(text, text.length(), true);
            width = textWidth + theme.scale(28);
        } else if (isResetButton()) {
            width = height = theme.scale(30);
        } else {
            width = height;
        }
    }

    @Override
    protected void onRender(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        MeteorGuiTheme theme = theme();

        if (isResetButton()) {
            if (mouseOver || pressed) {
                renderer.roundedQuad(x, y, width, height, theme.scale(9), theme.backgroundColor.get(pressed, mouseOver));
            }

            double size = theme.scale(14);
            renderer.quad(x + width / 2 - size / 2, y + height / 2 - size / 2, size, size, texture,
                mouseOver ? theme.textColor.get() : theme.textSecondaryColor.get());
            return;
        }

        renderBackground(renderer, this, pressed, mouseOver);

        if (text != null) {
            boolean material = isModuleDetails();
            double textHeight = theme.textHeight(material);
            renderer.text(text, x + width / 2 - textWidth / 2, y + height / 2 - textHeight / 2,
                theme.textColor.get(), material);
        }
        else {
            double ts = isModuleDetails() ? theme.textHeight(true) : theme.textHeight();
            renderer.quad(x + width / 2 - ts / 2, y + height / 2 - ts / 2, ts, ts, texture, theme.textColor.get());
        }
    }

    private boolean isResetButton() {
        return isModuleDetails() && texture == GuiRenderer.RESET;
    }
}
