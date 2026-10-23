/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */
package meteordevelopment.meteorclient.systems.commandbook;

import java.util.HashSet;
import java.util.Set;

/** Edge tracking only: no queued sends, timers or actions on release. */
public final class CommandHotkeyGate {
    private final Set<Long> held = new HashSet<>();

    public boolean press(boolean keyboard, int value, boolean eligible) {
        return held.add(code(keyboard, value)) && eligible;
    }

    public void release(boolean keyboard, int value) { held.remove(code(keyboard, value)); }
    private static long code(boolean keyboard, int value) { return ((keyboard ? 1L : 0L) << 32) | (value & 0xffffffffL); }
}
