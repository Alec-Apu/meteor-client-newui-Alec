/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */
package meteordevelopment.meteorclient.systems.commandbook;

import java.util.*;

/** Immutable entries and Minecraft-independent notebook operations. No command bodies are logged. */
public final class CommandNotebook {
    public static final String GLOBAL = "*";
    public static final int HISTORY_LIMIT = 200;
    public static final int MAX_COMMAND_LENGTH = 256;
    public enum View { Favorites, Recent, History }
    public enum Kind { Server, Client, Invalid }
    public enum Result { Submitted, NotConnected, WrongScope, InvalidCommand }

    public record Entry(UUID id, String name, String command, String note, String category,
                        String scope, long lastUsed, boolean favorite) {
        public Entry {
            Objects.requireNonNull(id);
            Objects.requireNonNull(name);
            Objects.requireNonNull(command);
            Objects.requireNonNull(note);
            Objects.requireNonNull(category);
            Objects.requireNonNull(scope);
            if (name.length() > 128 || command.length() > MAX_COMMAND_LENGTH || note.length() > 2048
                || category.length() > 128 || scope.isBlank() || scope.length() > 2048 || lastUsed < 0)
                throw new IllegalArgumentException("Invalid command notebook entry");
        }
        public Entry used(long now) { return new Entry(id, name, command, note, category, scope, Math.max(lastUsed, now), favorite); }
        public boolean appliesTo(String current) { return scope.equals(GLOBAL) || scope.equals(current); }
    }

    @FunctionalInterface public interface Sender { void send(Kind kind, String body); }
    private final LinkedHashMap<UUID, Entry> entries = new LinkedHashMap<>();

    public static Kind kind(String command, String prefix) {
        if (command == null || command.isBlank() || command.length() > MAX_COMMAND_LENGTH
            || command.chars().anyMatch(Character::isISOControl)) return Kind.Invalid;
        if (command.startsWith("/") && !command.substring(1).isBlank()) return Kind.Server;
        if (prefix != null && !prefix.isEmpty() && command.startsWith(prefix)
            && !command.substring(prefix.length()).isBlank()) return Kind.Client;
        return Kind.Invalid;
    }

    public static boolean recordable(String command, String prefix) {
        Kind kind = kind(command, prefix);
        if (kind == Kind.Invalid) return false;
        String body = command.substring(kind == Kind.Server ? 1 : prefix.length()).stripLeading();
        String root = body.split("\\s+", 2)[0].toLowerCase(Locale.ROOT);
        root = root.substring(root.lastIndexOf(':') + 1);
        return !Set.of("login", "l", "register", "reg", "changepassword", "changepass", "password",
            "passwd", "auth", "authenticate", "2fa", "totp", "pin").contains(root);
    }

    public List<Entry> entries() { return List.copyOf(entries.values()); }
    public Entry get(UUID id) { return entries.get(id); }
    public void put(Entry entry) { entries.put(entry.id, entry); trim(); }
    public void remove(UUID id) { entries.remove(id); }
    public void replace(Collection<Entry> values) {
        entries.clear();
        for (Entry entry : values) entries.put(entry.id, entry);
        trim();
    }

    public boolean record(String command, String prefix, String scope, long now) {
        if (scope == null || scope.isBlank() || scope.equals(GLOBAL) || now <= 0 || !recordable(command, prefix)) return false;
        Entry match = entries.values().stream().filter(e -> e.command.equals(command) && e.scope.equals(scope)).findFirst().orElse(null);
        if (match == null) {
            String title = command.length() > 64 ? command.substring(0, 64) + "…" : command;
            match = new Entry(UUID.randomUUID(), title, command, "", "", scope, now, false);
        }
        entries.put(match.id, match.used(now));
        // Global favorites retain their own identity and notes while gaining a last-use timestamp.
        for (Entry entry : List.copyOf(entries.values())) {
            if (entry.favorite && entry.scope.equals(GLOBAL) && entry.command.equals(command)) entries.put(entry.id, entry.used(now));
        }
        trim();
        return true;
    }

    private void trim() {
        List<Entry> history = entries.values().stream().filter(e -> !e.favorite)
            .sorted(Comparator.comparingLong(Entry::lastUsed).reversed()).toList();
        for (int i = HISTORY_LIMIT; i < history.size(); i++) entries.remove(history.get(i).id);
    }

    public void clearHistory() { entries.values().removeIf(e -> !e.favorite); }

    public List<Entry> query(View view, String text, String currentScope, boolean showOtherScopes) {
        String search = text.strip().toLowerCase(Locale.ROOT);
        return entries.values().stream()
            .filter(e -> showOtherScopes || e.appliesTo(currentScope))
            .filter(e -> switch (view) {
                case Favorites -> e.favorite;
                case Recent -> e.lastUsed > 0;
                case History -> !e.favorite && e.lastUsed > 0;
            })
            .filter(e -> (e.name + "\n" + e.command + "\n" + e.note + "\n" + e.category).toLowerCase(Locale.ROOT).contains(search))
            .sorted(Comparator.comparingLong(Entry::lastUsed).reversed().thenComparing(Entry::name).thenComparing(Entry::id))
            .limit(view == View.Recent ? 20 : Long.MAX_VALUE).toList();
    }

    public static Result execute(Entry entry, boolean connected, String currentScope, String prefix, Sender sender) {
        if (!connected) return Result.NotConnected;
        if (!entry.appliesTo(currentScope)) return Result.WrongScope;
        Kind kind = kind(entry.command, prefix);
        if (kind == Kind.Invalid) return Result.InvalidCommand;
        sender.send(kind, entry.command.substring(kind == Kind.Server ? 1 : prefix.length()));
        return Result.Submitted;
    }
}
