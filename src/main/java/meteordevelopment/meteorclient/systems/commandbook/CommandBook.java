/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */
package meteordevelopment.meteorclient.systems.commandbook;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandSignedPacket;
import net.minecraft.world.level.storage.LevelResource;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import meteordevelopment.meteorclient.events.meteor.KeyInputEvent;
import meteordevelopment.meteorclient.systems.macros.Macros;
import meteordevelopment.meteorclient.mixin.KeyMappingAccessor;
import meteordevelopment.meteorclient.utils.misc.input.KeyBinds;
import com.mojang.blaze3d.platform.InputConstants;
import meteordevelopment.meteorclient.events.meteor.MouseClickEvent;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.Localization;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.CommandBookModule;
import meteordevelopment.orbit.EventPriority;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public final class CommandBook extends System<CommandBook> {
    private final CommandNotebook notebook = new CommandNotebook();
    private boolean recordHistory = true;
    private final Map<UUID, Keybind> bindings = new HashMap<>();
    private final CommandHotkeyGate hotkeys = new CommandHotkeyGate();
    private boolean dirty;
    private int saveTicks;
    private long revision;

    public CommandBook() { super("command-book"); }
    public static CommandBook get() { return Systems.get(CommandBook.class); }
    public CommandNotebook notebook() { return notebook; }
    public long revision() { return revision; }
    public boolean recordHistory() { return recordHistory; }
    public void recordHistory(boolean value) { recordHistory = value; changed(true); }
    private void changed(boolean saveNow) { revision++; dirty = true; if (saveNow) { save(); dirty = false; saveTicks = 0; } }

    public static String currentScope() {
        if (mc.player == null || mc.level == null || mc.getConnection() == null) return "";
        if (mc.getSingleplayerServer() != null) {
            return "world:" + mc.getSingleplayerServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
        }
        if (mc.getCurrentServer() != null) return "server:" + mc.getCurrentServer().ip.strip().toLowerCase(Locale.ROOT);
        return "";
    }

    public void put(CommandNotebook.Entry entry) {
        var previous = notebook.get(entry.id());
        notebook.put(previous == null ? entry : entry.used(previous.lastUsed()));
        changed(true);
    }
    public void remove(UUID id) { notebook.remove(id); bindings.remove(id); changed(true); }
    public void favorite(UUID id, boolean value) {
        var entry = notebook.get(id);
        if (entry == null) return;
        notebook.put(entry.favorited(value));
        if (!value) bindings.remove(id);
        changed(true);
    }
    public Keybind binding(UUID id) { return bindings.getOrDefault(id, Keybind.none()).copy(); }
    public boolean canBind(UUID id, String scope, Keybind binding) {
        if (!binding.isSet()) return true;
        if (!binding.canBindTo(binding.isKey(), binding.getValue(), 0)) return false;
        for (var mapping : java.util.List.of(KeyBinds.OPEN_GUI, KeyBinds.OPEN_COMMANDS)) {
            var key = ((KeyMappingAccessor) mapping).meteor$getKey();
            if ((key.getType() != InputConstants.Type.MOUSE) == binding.isKey() && key.getValue() == binding.getValue()) return false;
        }
        for (var module : Modules.get().getAll()) if (overlaps(module.keybind, binding)) return false;
        for (var macro : Macros.get()) if (overlaps(macro.keybind.get(), binding)) return false;
        for (var entry : notebook.entries()) {
            if (entry.id().equals(id) || !entry.favorite()) continue;
            if (!(entry.scope().equals(scope) || entry.scope().equals(CommandNotebook.GLOBAL) || scope.equals(CommandNotebook.GLOBAL))) continue;
            Keybind other = bindings.get(entry.id());
            if (other != null && overlaps(other, binding)) return false;
        }
        return true;
    }
    private static boolean overlaps(Keybind a, Keybind b) {
        return a.matches(b.isKey(), b.getValue(), b.toTag().getIntOr("modifiers", 0))
            || b.matches(a.isKey(), a.getValue(), a.toTag().getIntOr("modifiers", 0));
    }
    public void bind(UUID id, Keybind binding) {
        var entry = notebook.get(id);
        if (entry == null || !entry.favorite() || !canBind(id, entry.scope(), binding)) throw new IllegalArgumentException("Invalid command hotkey");
        if (binding.isSet()) bindings.put(id, binding.copy()); else bindings.remove(id);
        changed(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    private void onBindingKey(KeyInputEvent event) {
        if (Modules.get().get(CommandBookModule.class).capture(event)) event.cancel();
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    private void onBindingMouse(MouseClickEvent event) {
        if (Modules.get().get(CommandBookModule.class).capture(event)) event.cancel();
    }

    @EventHandler(priority = EventPriority.HIGH)
    private void onKey(KeyInputEvent event) {
        if (event.action == KeyAction.Release) hotkeys.release(true, event.key());
        else if (event.action == KeyAction.Press && hotkeys.press(true, event.key(), !event.isCancelled() && hotkeysAvailable()))
            useHotkey(true, event.key(), event.modifiers());
    }
    @EventHandler(priority = EventPriority.HIGH)
    private void onMouse(MouseClickEvent event) {
        if (event.action == KeyAction.Release) hotkeys.release(false, event.button());
        else if (event.action == KeyAction.Press && hotkeys.press(false, event.button(), !event.isCancelled() && hotkeysAvailable()))
            useHotkey(false, event.button(), 0);
    }
    private boolean hotkeysAvailable() {
        return mc.player != null && mc.player.isAlive() && mc.level != null && mc.getConnection() != null && mc.gui.screen() == null
            && Modules.get().get(CommandBookModule.class).isActive();
    }
    private void useHotkey(boolean keyboard, int value, int modifiers) {
        String scope = currentScope();
        var matches = notebook.entries().stream().filter(e -> e.favorite() && e.appliesTo(scope))
            .filter(e -> bindings.containsKey(e.id()) && bindings.get(e.id()).matches(keyboard, value, modifiers)).toList();
        if (matches.size() > 1) { ChatUtils.warning(Localization.text("Multiple commands share this hotkey. Rebind them before use.")); return; }
        if (matches.isEmpty()) return;
        var selected = matches.getFirst();
        if (!canBind(selected.id(), selected.scope(), bindings.get(selected.id()))) {
            ChatUtils.warning(Localization.text("This command hotkey conflicts with a module, macro or menu key.")); return;
        }
        try {
            var result = execute(matches.getFirst().id());
            if (result != CommandNotebook.Result.Submitted) ChatUtils.warning(Localization.text("Command hotkey could not run. Check its scope and command text."));
        } catch (CommandSyntaxException e) { ChatUtils.error(meteordevelopment.meteorclient.utils.misc.CommandErrors.message(e)); }
    }
    public void clearHistory() { notebook.clearHistory(); changed(true); }

    public void record(String command) {
        if (!recordHistory) return;
        String scope = currentScope();
        if (notebook.record(command, Config.get().prefix.get(), scope, java.lang.System.currentTimeMillis())) changed(false);
    }

    @EventHandler private void onSent(PacketEvent.Sent event) {
        String body;
        if (event.packet instanceof ServerboundChatCommandPacket command) body = command.command();
        else if (event.packet instanceof ServerboundChatCommandSignedPacket command) body = command.command();
        else return;
        var connection = event.connection;
        // Network-originated events must not mutate the notebook or resolve worlds off the client thread.
        mc.execute(() -> {
            if (mc.getConnection() != null && mc.getConnection().getConnection() == connection) record("/" + body);
        });
    }

    @EventHandler private void onTick(TickEvent.Post event) {
        if (dirty && ++saveTicks >= 20) { save(); dirty = false; saveTicks = 0; }
    }

    public CommandNotebook.Result execute(UUID id) throws CommandSyntaxException {
        var entry = notebook.get(id);
        if (entry == null) return CommandNotebook.Result.InvalidCommand;
        // Commands.dispatch throws a checked exception; transport remains synchronous, one click / one send.
        final CommandSyntaxException[] syntaxError = { null };
        String context = currentScope();
        var result = CommandNotebook.execute(entry, mc.player != null && mc.getConnection() != null && !context.isEmpty(), context,
            Config.get().prefix.get(), (kind, body) -> {
                if (kind == CommandNotebook.Kind.Server) mc.player.connection.sendCommand(body);
                else {
                    try { Commands.dispatch(body); }
                    catch (CommandSyntaxException e) { syntaxError[0] = e; }
                }
            });
        if (syntaxError[0] != null) throw syntaxError[0];
        if (result == CommandNotebook.Result.Submitted) {
            mc.gui.hud.getChat().addRecentChat(entry.command());
            // Explicit notebook usage updates the selected bookmark even if automatic history is disabled.
            var current = notebook.get(id);
            if (current != null) notebook.put(current.used(java.lang.System.currentTimeMillis()));
            changed(true);
        }
        return result;
    }

    public boolean fillChat(UUID id) {
        var entry = notebook.get(id);
        if (entry == null || mc.player == null || mc.getConnection() == null || mc.level == null) return false;
        mc.gui.setScreen(new ChatScreen(entry.command(), true));
        return true;
    }

    @Override public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("schema", 2);
        tag.putBoolean("record-history", recordHistory);
        ListTag list = new ListTag();
        for (var entry : notebook.entries()) {
            CompoundTag value = new CompoundTag();
            value.putString("id", entry.id().toString());
            value.putString("name", entry.name());
            value.putString("command", entry.command());
            value.putString("note", entry.note());
            value.putString("category", entry.category());
            value.putString("scope", entry.scope());
            value.putLong("last-used", entry.lastUsed());
            value.putBoolean("favorite", entry.favorite());
            if (bindings.containsKey(entry.id()) && entry.favorite()) value.put("hotkey", bindings.get(entry.id()).toTag());
            list.add(value);
        }
        tag.put("entries", list);
        return tag;
    }

    @Override public CommandBook fromTag(CompoundTag tag) {
        var entries = new ArrayList<CommandNotebook.Entry>();
        bindings.clear();
        for (var raw : tag.getListOrEmpty("entries")) {
            if (!(raw instanceof CompoundTag value)) continue;
            try {
                entries.add(new CommandNotebook.Entry(UUID.fromString(value.getStringOr("id", "")),
                    value.getStringOr("name", ""), value.getStringOr("command", ""), value.getStringOr("note", ""),
                    value.getStringOr("category", ""), value.getStringOr("scope", CommandNotebook.GLOBAL),
                    value.getLongOr("last-used", 0), value.getBooleanOr("favorite", false)));
                UUID id = UUID.fromString(value.getStringOr("id", ""));
                if (value.getBooleanOr("favorite", false) && value.contains("hotkey")) {
                    Keybind binding = Keybind.none().fromTag(value.getCompoundOrEmpty("hotkey"));
                    if (binding.isSet() && binding.canBindTo(binding.isKey(), binding.getValue(), 0)) bindings.put(id, binding);
                }
            } catch (IllegalArgumentException ignored) {
                // Skip malformed entries, without logging their potentially sensitive command bodies.
                MeteorClient.LOG.warn("Skipped an invalid command notebook entry.");
            }
        }
        notebook.replace(entries);
        bindings.keySet().removeIf(id -> notebook.get(id) == null || !notebook.get(id).favorite());
        recordHistory = tag.getBooleanOr("record-history", true);
        dirty = false;
        saveTicks = 0;
        revision++;
        return this;
    }
}
