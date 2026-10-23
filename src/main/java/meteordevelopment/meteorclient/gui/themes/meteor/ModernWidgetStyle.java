/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.themes.meteor;

import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.screens.ModuleScreen;
import meteordevelopment.meteorclient.utils.render.color.Color;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public final class ModernWidgetStyle {
    private ModernWidgetStyle() {
    }

    public static boolean isModuleDetails() {
        return mc.gui.screen() instanceof ModuleScreen;
    }

    public static void renderPill(GuiRenderer renderer, double x, double y, double width, double height, Color color) {
        renderer.roundedQuad(x, y, width, height, height / 2, color);
    }
}
