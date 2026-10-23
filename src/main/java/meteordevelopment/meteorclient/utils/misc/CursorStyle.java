/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */
package meteordevelopment.meteorclient.utils.misc;

import com.mojang.blaze3d.platform.cursor.CursorType;
import com.mojang.blaze3d.platform.cursor.CursorTypes;

public enum CursorStyle {
    Default, Click, Type;

    public CursorType getCursor() {
        return switch (this) {
            case Default -> CursorType.DEFAULT;
            case Click -> CursorTypes.POINTING_HAND;
            case Type -> CursorTypes.IBEAM;
        };
    }
}