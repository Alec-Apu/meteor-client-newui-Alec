/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.widgets;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;

public abstract class WTooltip extends WContainer implements WRoot {
    private boolean valid;

    protected String text;

    public WTooltip(String text) {
        this.text = text;
    }

    @Override
    public void init() {
        // Wrap by code point and measured width, including text without spaces.
        var lines = add(theme.verticalList()).pad(4).widget();
        double maxWidth = Math.max(80, Math.min(theme.scale(420),
            meteordevelopment.meteorclient.utils.Utils.getWindowWidth() * 0.6));
        for (String paragraph : text.split("\n", -1)) {
            StringBuilder line = new StringBuilder();
            for (int offset = 0; offset < paragraph.length();) {
                int cp = paragraph.codePointAt(offset);
                String character = new String(Character.toChars(cp));
                if (!line.isEmpty() && theme.textWidth(line + character) > maxWidth) {
                    lines.add(theme.label(line.toString()));
                    line.setLength(0);
                }
                line.append(character);
                offset += Character.charCount(cp);
            }
            lines.add(theme.label(line.toString()));
        }
    }

    @Override
    public void invalidate() {
        valid = false;
    }

    @Override
    public boolean render(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (!valid) {
            calculateSize();
            calculateWidgetPositions();

            valid = true;
        }

        return super.render(renderer, mouseX, mouseY, delta);
    }
}
