/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */
package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.screens.CommandBookScreen;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Localization;

/** The notebook always records history; enabling this module additionally allows command hotkeys. */
public final class CommandBookModule extends Module {
    public CommandBookModule() {
        super(Categories.Misc, "command-book", "Manage saved commands and enable their individual hotkeys.");
    }

    @Override public WWidget getWidget(GuiTheme theme) {
        var list = theme.verticalList();
        list.add(theme.label(Localization.text("Enable this module to use command hotkeys. Recording works while disabled."), 400));
        var open = list.add(theme.button(Localization.text("Open Command Book"))).expandX().widget();
        open.action = () -> mc.gui.setScreen(new CommandBookScreen(theme));
        return list;
    }
}
