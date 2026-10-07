/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.utils.misc;

import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.nbt.CompoundTag;

import java.util.Objects;

import static com.mojang.blaze3d.platform.InputConstants.*;

public class Keybind implements ISerializable<Keybind>, ICopyable<Keybind> {
    private boolean isKey;
    private int value;
    private int modifiers;

    private Keybind(boolean isKey, int value, int modifiers) {
        set(isKey, value, modifiers);
    }

    public static Keybind none() {
        return new Keybind(true, com.mojang.blaze3d.platform.InputConstants.UNKNOWN.getValue(), 0);
    }

    public static Keybind fromKey(int key) {
        return new Keybind(true, key, 0);
    }

    public static Keybind fromKeys(int key, int modifiers) {
        return new Keybind(true, key, modifiers);
    }

    public static Keybind fromButton(int button) {
        return new Keybind(false, button, 0);
    }

    public int getValue() {
        return value;
    }

    public boolean isSet() {
        return value != com.mojang.blaze3d.platform.InputConstants.UNKNOWN.getValue();
    }

    public boolean isKey() {
        return isKey;
    }

    public boolean hasMods() {
        return isKey && modifiers != 0;
    }

    public void set(boolean isKey, int value, int modifiers) {
        this.isKey = isKey;
        this.value = value;
        this.modifiers = modifiers;
    }

    @Override
    public Keybind set(Keybind value) {
        this.isKey = value.isKey;
        this.value = value.value;
        this.modifiers = value.modifiers;

        return this;
    }

    public void reset() {
        set(true, com.mojang.blaze3d.platform.InputConstants.UNKNOWN.getValue(), 0);
    }

    public boolean canBindTo(boolean isKey, int value, int modifiers) {
        if (isKey) {
            if (modifiers != 0 && isKeyMod(value)) return false;
            return value != com.mojang.blaze3d.platform.InputConstants.UNKNOWN.getValue() && value != KEY_ESCAPE;
        }
        return value != MOUSE_BUTTON_LEFT && value != MOUSE_BUTTON_RIGHT;
    }

    public boolean matches(boolean isKey, int value, int modifiers) {
        if (!this.isSet() || this.isKey != isKey) return false;
        if (!hasMods()) return this.value == value;
        return this.value == value && normalizeModifiers(this.modifiers) == normalizeModifiers(modifiers);
    }

    public boolean matches(KeyEvent input) {
        return matches(true, input.key(), input.modifiers());
    }

    public boolean matches(MouseButtonInfo input) {
        return matches(false, input.button(), 0);
    }

    public boolean isPressed() {
        return isKey ? modifiersPressed() && Input.isKeyPressed(value) : Input.isButtonPressed(value);
    }

    private boolean modifiersPressed() {
        if (!hasMods()) return true;

        if (!isModPressed(MOD_CONTROL, KEY_LCONTROL, KEY_RCONTROL)) return false;
        if (!isModPressed(MOD_SUPER, KEY_LGUI, KEY_RGUI)) return false;
        if (!isModPressed(MOD_ALT, KEY_LALT, KEY_RALT)) return false;
        if (!isModPressed(MOD_SHIFT, KEY_LSHIFT, KEY_RSHIFT)) return false;

        return true;
    }

    private boolean isModPressed(int value, int... keys) {
        if ((modifiers & value) == 0) return true;

        for (int key : keys) {
            if (Input.isKeyPressed(key)) return true;
        }

        return false;
    }

    private boolean isKeyMod(int key) {
        return key >= KEY_LCONTROL && key <= KEY_RGUI;
    }

    @Override
    public Keybind copy() {
        return new Keybind(isKey, value, modifiers);
    }

    @Override
    public String toString() {
        if (!isSet()) return "None";
        if (!isKey) return Utils.getButtonName(value);
        if (modifiers == 0) return Utils.getKeyName(value);

        StringBuilder label = new StringBuilder();
        if ((modifiers & MOD_CONTROL) != 0) label.append("Ctrl + ");
        if ((modifiers & MOD_SUPER) != 0) label.append("Cmd + ");
        if ((modifiers & MOD_ALT) != 0) label.append("Alt + ");
        if ((modifiers & MOD_SHIFT) != 0) label.append("Shift + ");
        if ((modifiers & MOD_CAPS_LOCK) != 0) label.append("Caps Lock + ");
        if ((modifiers & MOD_NUM_LOCK) != 0) label.append("Num Lock + ");
        label.append(Utils.getKeyName(value));

        return label.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Keybind keybind = (Keybind) o;
        return isKey == keybind.isKey && value == keybind.value && modifiers == keybind.modifiers;
    }

    @Override
    public int hashCode() {
        return Objects.hash(isKey, value, modifiers);
    }

    // Serialization

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();

        tag.putBoolean("isKey", isKey);
        tag.putInt("value", value);
        tag.putInt("modifiers", modifiers);
        tag.putString("inputFormat", "sdl");

        return tag;
    }

    @Override
    public Keybind fromTag(CompoundTag tag) {
        isKey = tag.getBooleanOr("isKey", false);
        value = tag.getIntOr("value", 0);
        modifiers = tag.getIntOr("modifiers", 0);

        if (!"sdl".equals(tag.getStringOr("inputFormat", ""))) {
            value = isKey ? legacyKey(value) : switch (value) {
                case -1 -> 0;
                case 0 -> MOUSE_BUTTON_LEFT;
                case 1 -> MOUSE_BUTTON_RIGHT;
                case 2 -> MOUSE_BUTTON_MIDDLE;
                default -> value + 1;
            };
            int old = modifiers;
            modifiers = ((old & 1) != 0 ? MOD_SHIFT : 0)
                | ((old & 2) != 0 ? MOD_CONTROL : 0)
                | ((old & 4) != 0 ? MOD_ALT : 0)
                | ((old & 8) != 0 ? MOD_SUPER : 0)
                | ((old & 16) != 0 ? MOD_CAPS_LOCK : 0)
                | ((old & 32) != 0 ? MOD_NUM_LOCK : 0);
        }

        return this;
    }

    private static int normalizeModifiers(int mods) {
        return ((mods & MOD_SHIFT) != 0 ? MOD_SHIFT : 0)
            | ((mods & MOD_CONTROL) != 0 ? MOD_CONTROL : 0)
            | ((mods & MOD_ALT) != 0 ? MOD_ALT : 0)
            | ((mods & MOD_SUPER) != 0 ? MOD_SUPER : 0)
            | (mods & (MOD_CAPS_LOCK | MOD_NUM_LOCK));
    }

    private static int legacyKey(int key) {
        if (key >= 65 && key <= 90) return KEY_A + key - 65;
        if (key >= 49 && key <= 57) return KEY_1 + key - 49;
        if (key >= 290 && key <= 301) return KEY_F1 + key - 290;
        if (key >= 302 && key <= 313) return KEY_F13 + key - 302;
        if (key >= 321 && key <= 329) return 89 + key - 321; // SDL keypad 1 through 9
        return switch (key) {
            case 32 -> KEY_SPACE;
            case 39 -> KEY_APOSTROPHE;
            case 44 -> KEY_COMMA;
            case 45 -> KEY_MINUS;
            case 46 -> KEY_PERIOD;
            case 47 -> KEY_SLASH;
            case 48 -> KEY_0;
            case 59 -> KEY_SEMICOLON;
            case 61 -> KEY_EQUALS;
            case 91 -> KEY_LBRACKET;
            case 92 -> KEY_BACKSLASH;
            case 93 -> KEY_RBRACKET;
            case 96 -> KEY_GRAVE;
            case 161 -> 135; // SDL international 1
            case 162 -> 136; // SDL international 2
            case 256 -> KEY_ESCAPE;
            case 257 -> KEY_RETURN;
            case 258 -> KEY_TAB;
            case 259 -> KEY_BACKSPACE;
            case 260 -> KEY_INSERT;
            case 261 -> KEY_DELETE;
            case 262 -> KEY_RIGHT;
            case 263 -> KEY_LEFT;
            case 264 -> KEY_DOWN;
            case 265 -> KEY_UP;
            case 266 -> KEY_PAGEUP;
            case 267 -> KEY_PAGEDOWN;
            case 268 -> KEY_HOME;
            case 269 -> KEY_END;
            case 280 -> KEY_CAPSLOCK;
            case 281 -> KEY_SCROLLLOCK;
            case 282 -> 83; // SDL num lock
            case 283 -> KEY_PRINTSCREEN;
            case 284 -> KEY_PAUSE;
            case 320 -> 98; // SDL keypad 0
            case 330 -> 99;
            case 331 -> 84;
            case 332 -> 85;
            case 333 -> 86;
            case 334 -> 87;
            case 335 -> 88;
            case 336 -> 103;
            case 340 -> KEY_LSHIFT;
            case 341 -> KEY_LCONTROL;
            case 342 -> KEY_LALT;
            case 343 -> KEY_LGUI;
            case 344 -> KEY_RSHIFT;
            case 345 -> KEY_RCONTROL;
            case 346 -> KEY_RALT;
            case 347 -> KEY_RGUI;
            case 348 -> 101; // SDL application
            default -> 0;
        };
    }
}
