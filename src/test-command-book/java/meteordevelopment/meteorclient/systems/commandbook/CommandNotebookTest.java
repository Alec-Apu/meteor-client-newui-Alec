/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */
package meteordevelopment.meteorclient.systems.commandbook;

import java.util.*;
import static meteordevelopment.meteorclient.systems.commandbook.CommandNotebook.*;

/** Tests the production notebook model and command routing without a game or network. */
public final class CommandNotebookTest {
    private static int passed;
    private static void check(boolean ok, String name) {
        if (!ok) throw new AssertionError(name);
        passed++;
        System.out.println("PASS " + name);
    }
    private static Entry favorite(String command, String scope) {
        return new Entry(UUID.randomUUID(), "返回家", command, "返回我在服务器设置的家", "生存", scope, 0, true);
    }
    public static void main(String[] args) {
        check(kind("/home", ".") == Kind.Server, "Slash command uses server route");
        check(kind(".help", ".") == Kind.Client, "Meteor command uses client route");
        check(kind("!help", "!") == Kind.Client, "Custom Meteor prefix is supported");
        check(kind("hello", ".") == Kind.Invalid, "Ordinary chat is not a command");
        check(kind("/", ".") == Kind.Invalid && kind(".", ".") == Kind.Invalid, "Empty command body is rejected");
        check(kind("/say one\n/kill", ".") == Kind.Invalid, "Newline cannot inject a second command");
        check(kind("/say\ttext", ".") == Kind.Invalid && kind("/say\0text", ".") == Kind.Invalid, "Control characters rejected");
        check(kind("/" + "x".repeat(256), ".") == Kind.Invalid, "Oversized command rejected");
        check(kind(".help", "!") == Kind.Invalid, "Old prefix cannot accidentally become chat");
        for (String command : List.of("/login secret", "/LOGIN secret", "/l secret", "/register secret secret", "/reg secret",
            "/authme:login secret", "/changepassword old new", "/2fa 123456", ".login token")) {
            check(!recordable(command, "."), "Sensitive command excluded: " + command.split(" ")[0]);
        }
        check(recordable("/home login", "."), "Non-sensitive root does not filter ordinary arguments");
        CommandNotebook book = new CommandNotebook();
        check(!book.record("hello", ".", "server:a", 1), "Ordinary chat is never stored");
        check(!book.record("/login secret", ".", "server:a", 1) && book.entries().isEmpty(), "Sensitive history is never stored");
        check(!book.record("/home", ".", "", 1) && !book.record("/home", ".", GLOBAL, 1), "No-world and global pseudo-context cannot create history");
        check(book.record("/home", ".", "server:a", 100), "Sent command enters contextual history");
        UUID historyId = book.entries().getFirst().id();
        book.record("/home", ".", "server:a", 200);
        check(book.entries().size() == 1 && book.get(historyId).lastUsed() == 200, "Repeated command merges and updates timestamp");
        book.record("/home", ".", "server:b", 150);
        check(book.entries().size() == 2, "Servers keep separate histories");
        check(book.query(View.History, "", "server:a", false).size() == 1, "Default view filters other servers");
        check(book.query(View.History, "", "server:a", true).size() == 2, "Other-server view is explicit");
        Entry saved = favorite("/home", GLOBAL);
        book.put(saved);
        check(book.query(View.Favorites, "设置的家", "server:b", false).size() == 1, "Search matches Chinese notes");
        check(book.query(View.Favorites, "HOME", "server:b", false).size() == 1, "Search is case insensitive and covers command text");
        check(book.query(View.Favorites, "生存", "server:b", false).size() == 1, "Category is searchable");
        check(book.query(View.Favorites, "返回家", "server:b", false).size() == 1, "Name is searchable");
        book.record("/home", ".", "server:a", 300);
        check(book.get(saved.id()).lastUsed() == 300 && book.get(saved.id()).note().equals(saved.note()), "Global bookmark gains use time without losing notes");
        check(book.query(View.Recent, "/home", "server:a", false).size() == 1, "Recent view merges global favorite with its sent history");
        check(book.query(View.Recent, "/home", "server:a", true).size() == 2, "Recent view keeps different server contexts separate");
        Entry promoted = new Entry(historyId, "我的家", "/home", "备注", "常用", "server:a", 300, true);
        book.put(promoted);
        check(book.get(historyId).favorite() && book.query(View.History, "", "server:a", false).isEmpty(), "Saving history promotes same entry without duplication");
        book.clearHistory();
        check(book.entries().size() == 2 && book.entries().stream().allMatch(Entry::favorite), "Clear history preserves saved commands");
        for (int i = 1; i <= 230; i++) book.record("/test " + i, ".", "server:a", 1000 + i);
        check(book.entries().size() == HISTORY_LIMIT + 2, "History is capped without trimming favorites");
        check(book.query(View.History, "/test 1", "server:a", false).stream().noneMatch(e -> e.command().equals("/test 1")), "Oldest history is evicted");
        check(book.query(View.Recent, "", "server:a", false).size() == 50, "Recently used view is capped at 50");
        List<Entry> snapshot = book.entries();
        CommandNotebook loaded = new CommandNotebook();
        loaded.replace(snapshot);
        check(loaded.entries().equals(snapshot), "Entry data survives a model round trip");
        loaded.remove(saved.id());
        check(loaded.get(saved.id()) == null && book.get(saved.id()) != null, "Deletion affects only selected notebook");
        List<String> sent = new ArrayList<>();
        Sender sender = (route, body) -> sent.add(route + ":" + body);
        check(execute(saved, false, "server:a", ".", sender) == Result.NotConnected && sent.isEmpty(), "No-world execute sends nothing");
        check(execute(promoted, true, "server:b", ".", sender) == Result.WrongScope && sent.isEmpty(), "Cross-server execution blocked");
        check(execute(saved, true, "server:b", ".", sender) == Result.Submitted && sent.equals(List.of("Server:home")), "Server execute strips exactly one slash and sends once");
        sent.clear();
        check(execute(favorite("!help", GLOBAL), true, "world:a", "!", sender) == Result.Submitted && sent.equals(List.of("Client:help")), "Client execute dispatches locally with custom prefix");
        sent.clear();
        check(execute(favorite("not-a-command", GLOBAL), true, "server:a", ".", sender) == Result.InvalidCommand && sent.isEmpty(), "Invalid saved text never sends ordinary chat");
        Entry original = favorite("/say {value} \"hello world\"", GLOBAL);
        check(original.command().equals("/say {value} \"hello world\""), "Arguments, quotes and braces remain literal");
        boolean rejectsOversizedNote = false;
        try { new Entry(UUID.randomUUID(), "x", "/home", "x".repeat(2049), "", GLOBAL, 0, true); }
        catch (IllegalArgumentException expected) { rejectsOversizedNote = true; }
        check(rejectsOversizedNote, "Bounded note length is enforced");
        boolean immutable = false;
        try { snapshot.clear(); } catch (UnsupportedOperationException expected) { immutable = true; }
        check(immutable, "Views cannot mutate saved entries");
        check(HISTORY_LIMIT == 50, "History retains at most 50 non-favorites");
        check(saved.favorited(false).id().equals(saved.id()) && !saved.favorited(false).favorite()
            && saved.favorited(false).note().equals(saved.note()), "Unfavorite preserves command identity and notes");
        check(saved.favorited(false).favorited(true).equals(saved), "Favorite toggle is reversible");
        CommandNotebook migration = new CommandNotebook();
        List<Entry> legacy = new ArrayList<>();
        for (int i = 1; i <= 200; i++) legacy.add(new Entry(UUID.randomUUID(), "legacy", "/test " + i, "", "", "server:a", i, false));
        legacy.add(saved); migration.replace(legacy);
        check(migration.entries().size() == 51 && migration.get(saved.id()) != null, "Old 200-entry data trims to 50 while keeping favorites");
        check(migration.query(View.History, "", "server:a", false).getLast().command().equals("/test 151"), "Migration retains the latest fifty commands");
        CommandHotkeyGate gate = new CommandHotkeyGate();
        check(gate.press(true, 65, true), "Initial eligible key press runs once");
        check(!gate.press(true, 65, true), "Held key cannot repeat an action");
        gate.release(true, 65);
        check(gate.press(true, 65, true), "Release permits a new deliberate press");
        check(!gate.press(true, 66, false), "Disabled or GUI input does not execute");
        check(!gate.press(true, 66, true), "Closing a GUI while holding a key does not execute");
        gate.release(true, 66);
        check(gate.press(true, 66, true), "New press after closing GUI executes");
        check(gate.press(false, 65, true), "Mouse and keyboard input have independent identities");
        check(!gate.press(false, 65, true), "Held mouse side button cannot repeat");
        gate.release(false, 65);
        check(gate.press(false, 65, true), "Mouse release permits the next press");
        System.out.println("Command notebook: " + passed + " tests passed.");
    }
}
