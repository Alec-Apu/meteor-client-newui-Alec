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

import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public final class CommandBook extends System<CommandBook> {
    private final CommandNotebook notebook = new CommandNotebook();
    private boolean recordHistory = true;
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
    public void remove(UUID id) { notebook.remove(id); changed(true); }
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
        tag.putInt("schema", 1);
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
            list.add(value);
        }
        tag.put("entries", list);
        return tag;
    }

    @Override public CommandBook fromTag(CompoundTag tag) {
        var entries = new ArrayList<CommandNotebook.Entry>();
        for (var raw : tag.getListOrEmpty("entries")) {
            if (!(raw instanceof CompoundTag value)) continue;
            try {
                entries.add(new CommandNotebook.Entry(UUID.fromString(value.getStringOr("id", "")),
                    value.getStringOr("name", ""), value.getStringOr("command", ""), value.getStringOr("note", ""),
                    value.getStringOr("category", ""), value.getStringOr("scope", CommandNotebook.GLOBAL),
                    value.getLongOr("last-used", 0), value.getBooleanOr("favorite", false)));
            } catch (IllegalArgumentException ignored) {
                // Skip malformed entries, without logging their potentially sensitive command bodies.
                MeteorClient.LOG.warn("Skipped an invalid command notebook entry.");
            }
        }
        notebook.replace(entries);
        recordHistory = tag.getBooleanOr("record-history", true);
        dirty = false;
        saveTicks = 0;
        revision++;
        return this;
    }
}
