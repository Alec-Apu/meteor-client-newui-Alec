/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */
package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.CommandBookWidget;
import meteordevelopment.meteorclient.events.meteor.KeyInputEvent;
import meteordevelopment.meteorclient.events.meteor.MouseClickEvent;
import net.minecraft.client.gui.screens.Screen;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;

/** The notebook always records history; enabling this module additionally allows command hotkeys. */
public final class CommandBookModule extends Module {
    private Screen owner;
    private CommandBookWidget.State state;
    private CommandBookWidget widget;

    public CommandBookModule() {
        super(Categories.Misc, "command-book", "Manage saved commands and enable their individual hotkeys.");
    }

    @Override public WWidget getWidget(GuiTheme theme) {
        Screen screen = mc.gui.screen();
        if (state == null || owner != screen) state = new CommandBookWidget.State();
        owner = screen;
        widget = new CommandBookWidget(state, screen);
        return widget;
    }
    public boolean capture(KeyInputEvent event) { return widget != null && widget.capture(event); }
    public boolean capture(MouseClickEvent event) { return widget != null && widget.capture(event); }
}

