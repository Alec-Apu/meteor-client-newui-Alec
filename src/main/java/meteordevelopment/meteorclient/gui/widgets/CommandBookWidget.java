/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */
package meteordevelopment.meteorclient.gui.widgets;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import meteordevelopment.meteorclient.events.meteor.KeyInputEvent;
import meteordevelopment.meteorclient.events.meteor.MouseClickEvent;
import meteordevelopment.meteorclient.gui.renderer.GuiRenderer;
import meteordevelopment.meteorclient.gui.widgets.containers.*;
import meteordevelopment.meteorclient.gui.widgets.input.WDropdown;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.systems.commandbook.CommandBook;
import meteordevelopment.meteorclient.systems.commandbook.CommandNotebook;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.utils.misc.CommandErrors;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.Localization;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import net.minecraft.client.gui.screens.Screen;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

import static meteordevelopment.meteorclient.MeteorClient.mc;

/** Native module content: selection, editing and key binding never replace the owning ModuleScreen. */
public final class CommandBookWidget extends WVerticalList {
    public static final class State {
        public ListView view = ListView.Recent;
        public String search = "";
        public boolean showOtherScopes;
        public int page;
        public UUID selected;
        public Draft draft;
        public String notice = "";
    }

    public enum ListView {
        Recent, Favorites, History;
        CommandNotebook.View model() {
            return switch (this) { case Recent -> CommandNotebook.View.Recent; case Favorites -> CommandNotebook.View.Favorites; case History -> CommandNotebook.View.History; };
        }
        @Override public String toString() {
            return switch (this) { case Recent -> "Recently Used"; case Favorites -> "Saved Commands"; case History -> "Command History"; };
        }
    }

    public enum ScopeChoice {
        Global, CurrentContext, KeepExisting;
        @Override public String toString() {
            return switch (this) { case Global -> "Global"; case CurrentContext -> "Current server or world"; case KeepExisting -> "Keep existing scope"; };
        }
    }

    public static final class Draft {
        final UUID id;
        final String oldScope;
        String name, command, note, category;
        ScopeChoice scope;
        final Keybind binding;
        Draft(CommandNotebook.Entry entry) {
            id = entry == null ? UUID.randomUUID() : entry.id();
            oldScope = entry == null ? "" : entry.scope();
            name = entry == null ? "" : entry.name();
            command = entry == null ? "/" : entry.command();
            note = entry == null ? "" : entry.note();
            category = entry == null ? "" : entry.category();
            scope = entry == null ? (CommandBook.currentScope().isEmpty() ? ScopeChoice.Global : ScopeChoice.CurrentContext)
                : entry.scope().equals(CommandNotebook.GLOBAL) ? ScopeChoice.Global : ScopeChoice.KeepExisting;
            binding = entry == null ? Keybind.none() : CommandBook.get().binding(entry.id());
        }
    }

    private final State state;
    private final Screen owner;
    private WVerticalList content;
    private WKeybind hotkey;
    private WLabel notice;
    private long revision = -1;
    private String currentScope = "";
    private static final int PAGE_SIZE = 5;

    public CommandBookWidget(State state, Screen owner) { this.state = state; this.owner = owner; }
    private static String tr(String text) { return Localization.text(text); }
    private static String compact(String value, int length) { return value.length() <= length ? value : value.substring(0, length - 1) + "…"; }

    @Override public void init() {
        spacing = 12;
        var controls = add(theme.section(tr("Command Book"), true)).expandX().widget();
        var filters = controls.add(theme.table()).expandX().widget();
        filters.horizontalSpacing = 10; filters.verticalSpacing = 6;
        filters.add(theme.label(tr("Command View"), true));
        var view = filters.add(theme.dropdown(state.view)).expandX().widget();
        view.action = () -> { state.view = view.get(); state.page = 0; refresh(); };
        filters.row();
        filters.add(theme.label(tr("Search"), true));
        var search = filters.add(theme.textBox(state.search, "Search name, command or note...")).expandX().widget();
        search.action = () -> { state.search = search.get(); state.page = 0; refresh(); };
        filters.row();
        filters.add(theme.label(tr("Record command history"), true));
        var record = filters.add(theme.checkbox(CommandBook.get().recordHistory())).expandCellX().right().widget();
        record.action = () -> CommandBook.get().recordHistory(record.checked);
        filters.row();
        filters.add(theme.label(tr("Show other servers / worlds"), true));
        var other = filters.add(theme.checkbox(state.showOtherScopes)).expandCellX().right().widget();
        other.action = () -> { state.showOtherScopes = other.checked; state.page = 0; refresh(); };
        content = add(theme.verticalList()).expandX().widget();
        content.spacing = 12;
        refresh();
    }

    private WButton button(WContainer parent, String label, Runnable action) {
        var button = parent.add(theme.button(tr(label))).widget();
        button.action = action;
        return button;
    }

    private void refresh() {
        if (content == null) return;
        revision = CommandBook.get().revision(); currentScope = CommandBook.currentScope();
        content.clear(); hotkey = null;
        var book = CommandBook.get();
        var result = book.notebook().query(state.view.model(), state.search, currentScope, state.showOtherScopes);
        var list = content.add(theme.section(tr(state.view.toString()) + " (" + result.size() + ")", true)).expandX().widget();
        var rows = list.add(theme.table()).expandX().widget();
        rows.horizontalSpacing = 8; rows.verticalSpacing = 6;
        int pages = Math.max(1, (result.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        state.page = Math.min(state.page, pages - 1);
        if (result.isEmpty()) list.add(theme.label(tr("No matching commands.")));
        for (var entry : result.subList(state.page * PAGE_SIZE, Math.min(result.size(), (state.page + 1) * PAGE_SIZE))) {
            var select = rows.add(theme.button(compact(entry.name(), 24))).expandX().widget();
            select.tooltip = entry.command() + (entry.note().isBlank() ? "" : "\n" + entry.note());
            select.action = () -> { state.selected = entry.id(); state.draft = null; state.notice = ""; refresh(); };
            var star = rows.add(theme.button(entry.favorite() ? "★" : "☆")).widget();
            star.tooltip = tr(entry.favorite() ? "Unfavorite Command" : "Favorite Command");
            star.action = () -> { book.favorite(entry.id(), !entry.favorite()); refresh(); };
            var edit = rows.add(theme.button(GuiRenderer.EDIT)).widget();
            edit.tooltip = tr("Edit"); edit.action = () -> edit(entry);
            rows.row();
        }
        var footer = list.add(theme.horizontalList()).expandX().widget();
        if (state.page > 0) button(footer, "Previous", () -> { state.page--; refresh(); });
        footer.add(theme.label((state.page + 1) + " / " + pages)).expandX();
        if (state.page + 1 < pages) button(footer, "Next", () -> { state.page++; refresh(); });
        button(footer, "Add Command", () -> edit(null));
        if (state.view == ListView.History) {
            var clear = list.add(theme.confirmedButton("Clear History", "Confirm")).expandX().widget();
            clear.action = () -> { book.clearHistory(); refresh(); };
        }
        if (state.draft != null) editor();
        else if (state.selected != null) {
            var entry = book.notebook().get(state.selected);
            if (entry != null) details(entry);
        }
        notice = content.add(theme.label(state.notice, 330)).expandX().widget();
    }

    private void edit(CommandNotebook.Entry entry) {
        state.draft = new Draft(entry); state.notice = ""; refresh();
    }

    private void details(CommandNotebook.Entry entry) {
        var details = content.add(theme.section(tr("Command Details"), true)).expandX().widget();
        var table = details.add(theme.table()).expandX().widget();
        table.horizontalSpacing = 10; table.verticalSpacing = 6;
        detail(table, "Command Name", entry.name()); detail(table, "Command Text", entry.command());
        detail(table, "Note", entry.note()); detail(table, "Category", entry.category());
        detail(table, "Command Scope", displayScope(entry.scope()));
        detail(table, "Command Hotkey", CommandBook.get().binding(entry.id()).toString());
        var actions = details.add(theme.horizontalList()).expandX().widget();
        button(actions, "Execute", () -> {
            try {
                var result = CommandBook.get().execute(entry.id());
                state.notice = tr(switch (result) {
                    case Submitted -> "Command submitted. Check the game response.";
                    case NotConnected -> "Join a world before executing commands.";
                    case WrongScope -> "This command belongs to another server or world.";
                    case InvalidCommand -> "Enter a valid command with / or the current Meteor prefix.";
                });
            } catch (CommandSyntaxException e) { state.notice = CommandErrors.message(e); }
            refresh();
        });
        button(actions, "Fill Chat", () -> {
            if (!CommandBook.get().fillChat(entry.id())) { state.notice = tr("Join a world before executing commands."); refresh(); }
        });
        button(actions, "Copy", () -> { mc.keyboardHandler.setClipboard(entry.command()); state.notice = tr("Command copied."); refresh(); });
        button(actions, "Edit", () -> edit(entry));
        var remove = actions.add(theme.confirmedMinus()).widget(); remove.tooltip = tr("Delete command");
        remove.action = () -> { CommandBook.get().remove(entry.id()); state.selected = null; refresh(); };
    }

    private void detail(WTable table, String label, String text) {
        table.add(theme.label(tr(label), true));
        var value = table.add(theme.label(compact(text, 29))).expandX().widget(); value.tooltip = text;
        table.row();
    }

    private void editor() {
        Draft draft = state.draft;
        var section = content.add(theme.section(tr("Edit Saved Command"), true)).expandX().widget();
        var table = section.add(theme.table()).expandX().widget();
        table.horizontalSpacing = 10; table.verticalSpacing = 6;
        field(table, "Command Name", draft.name, value -> draft.name = value);
        field(table, "Command Text", draft.command, value -> draft.command = value);
        field(table, "Note", draft.note, value -> draft.note = value);
        field(table, "Category", draft.category, value -> draft.category = value);
        table.add(theme.label(tr("Command Scope"), true));
        var scope = table.add(theme.dropdown(draft.oldScope.isEmpty() ? new ScopeChoice[] {ScopeChoice.Global, ScopeChoice.CurrentContext} : ScopeChoice.values(), draft.scope)).expandX().widget();
        scope.action = () -> draft.scope = scope.get(); table.row();
        table.add(theme.label(tr("Command Hotkey"), true));
        hotkey = table.add(theme.keybind(draft.binding)).expandX().widget();
        hotkey.tooltip = tr("Click the key, then release a keyboard key or press a mouse side button. Esc clears it.");
        var actions = section.add(theme.horizontalList()).expandX().widget();
        button(actions, "Save Command", this::saveEntry);
        button(actions, "Cancel", () -> { state.draft = null; state.notice = ""; refresh(); });
    }

    private void field(WTable table, String label, String value, java.util.function.Consumer<String> change) {
        table.add(theme.label(tr(label), true));
        WTextBox input = table.add(theme.textBox(value)).expandX().minWidth(160).widget();
        input.action = () -> change.accept(input.get()); table.row();
    }

    private void saveEntry() {
        Draft draft = state.draft;
        if (draft == null) return;
        String text = draft.command.strip();
        if (draft.name.isBlank() || CommandNotebook.kind(text, Config.get().prefix.get()) == CommandNotebook.Kind.Invalid) {
            message("Enter a name and a valid command with / or the current Meteor prefix."); return;
        }
        String scope = switch (draft.scope) {
            case Global -> CommandNotebook.GLOBAL; case CurrentContext -> CommandBook.currentScope(); case KeepExisting -> draft.oldScope;
        };
        if (scope.isBlank()) { message("Join a world to select its command scope."); return; }
        if (!CommandBook.get().canBind(draft.id, scope, draft.binding)) {
            message("This hotkey conflicts with another command, module, macro or menu key."); return;
        }
        try {
            var previous = CommandBook.get().notebook().get(draft.id);
            CommandBook.get().put(new CommandNotebook.Entry(draft.id, draft.name.strip(), text, draft.note, draft.category.strip(), scope,
                previous == null ? 0 : previous.lastUsed(), true));
            CommandBook.get().bind(draft.id, draft.binding);
            state.selected = draft.id; state.draft = null; state.notice = ""; refresh();
        } catch (IllegalArgumentException e) { message("Name/category: 128 characters. Command: 256. Note: 2048."); }
    }

    private void message(String text) { state.notice = tr(text); if (notice != null) notice.set(state.notice); }
    public boolean ownsCurrentScreen() { return mc.gui.screen() == owner && getRoot() != null; }
    public boolean capture(KeyInputEvent event) {
        if (!ownsCurrentScreen() || hotkey == null) return false;
        return event.action == KeyAction.Press && event.key() == GLFW.GLFW_KEY_ESCAPE && hotkey.onClear()
            || event.action == KeyAction.Release && hotkey.onAction(true, event.key(), event.modifiers());
    }
    public boolean capture(MouseClickEvent event) {
        return ownsCurrentScreen() && hotkey != null && event.action == KeyAction.Press && hotkey.onAction(false, event.button(), 0);
    }

    @Override public boolean render(GuiRenderer renderer, double mouseX, double mouseY, double delta) {
        if (revision != CommandBook.get().revision() || !currentScope.equals(CommandBook.currentScope())) refresh();
        return super.render(renderer, mouseX, mouseY, delta);
    }

    private static String displayScope(String scope) {
        if (scope.equals(CommandNotebook.GLOBAL)) return tr("Global");
        if (scope.startsWith("server:")) return tr("Server") + ": " + scope.substring(7);
        if (scope.startsWith("world:")) {
            try { return tr("World") + ": " + java.nio.file.Path.of(scope.substring(6)).getFileName(); }
            catch (java.nio.file.InvalidPathException ignored) { return tr("World"); }
        }
        return scope;
    }
}
