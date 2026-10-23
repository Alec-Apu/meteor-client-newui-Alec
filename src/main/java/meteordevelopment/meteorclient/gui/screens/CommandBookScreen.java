/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */
package meteordevelopment.meteorclient.gui.screens;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WindowScreen;
import meteordevelopment.meteorclient.gui.widgets.WKeybind;
import meteordevelopment.meteorclient.events.meteor.KeyInputEvent;
import meteordevelopment.meteorclient.events.meteor.MouseClickEvent;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import org.lwjgl.glfw.GLFW;
import meteordevelopment.meteorclient.gui.widgets.WLabel;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.input.WDropdown;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WConfirmedButton;
import meteordevelopment.meteorclient.gui.widgets.pressable.WConfirmedMinus;
import meteordevelopment.meteorclient.systems.commandbook.CommandBook;
import meteordevelopment.meteorclient.systems.commandbook.CommandNotebook;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.utils.misc.CommandErrors;
import meteordevelopment.meteorclient.utils.misc.Localization;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public final class CommandBookScreen extends WindowScreen {
    private static String tr(String text) { return Localization.text(text); }
    private static String displayScope(String scope) {
        if (scope.equals(CommandNotebook.GLOBAL)) return tr("Global");
        if (scope.startsWith("server:")) return tr("Server") + ": " + scope.substring(7);
        if (scope.startsWith("world:")) {
            try { return tr("World") + ": " + java.nio.file.Path.of(scope.substring(6)).getFileName(); }
            catch (java.nio.file.InvalidPathException ignored) { return tr("World"); }
        }
        return scope;
    }

    private CommandNotebook.View view = CommandNotebook.View.Recent;
    private String search = "";
    private boolean showOtherScopes;
    private int page;
    private long revision = -1;
    private String scope = "";
    private WVerticalList entries;
    private WLabel notice;

    public CommandBookScreen(GuiTheme theme) { super(theme, "Command Book"); }

    @Override public void initWidgets() {
        var header = add(theme.horizontalList()).expandX().minWidth(460).widget();
        WTextBox input = header.add(theme.textBox(search, "Search name, command or note...")).expandX().widget();
        input.action = () -> { search = input.get(); page = 0; refresh(); };
        WButton add = header.add(theme.button(tr("Add Command"))).widget();
        add.action = () -> mc.gui.setScreen(new EditCommandScreen(theme, null, this::refresh));

        var views = add(theme.horizontalList()).expandX().widget();
        for (var value : CommandNotebook.View.values()) {
            WButton button = views.add(theme.button(tr(viewLabel(value)))).expandX().widget();
            button.action = () -> { view = value; page = 0; refresh(); };
        }
        var options = add(theme.horizontalList()).expandX().widget();
        options.add(theme.label(tr("Record command history")));
        var record = options.add(theme.checkbox(CommandBook.get().recordHistory())).widget();
        record.action = () -> CommandBook.get().recordHistory(record.checked);
        options.add(theme.label(tr("Show other servers / worlds")));
        var other = options.add(theme.checkbox(showOtherScopes)).widget();
        other.action = () -> { showOtherScopes = other.checked; page = 0; refresh(); };

        add(theme.label(tr("Recent commands: 50. Favorites are kept. Enable the Misc module for hotkeys."), 440)).expandX();
        notice = add(theme.label("")).expandX().widget();
        entries = add(theme.verticalList()).expandX().widget();
        refresh();
    }

    private static String viewLabel(CommandNotebook.View view) {
        return switch (view) { case Favorites -> "Saved Commands"; case Recent -> "Recently Used"; case History -> "Command History"; };
    }

    private void refresh() {
        if (entries == null) return;
        var book = CommandBook.get();
        revision = book.revision();
        scope = CommandBook.currentScope();
        var result = book.notebook().query(view, search, scope, showOtherScopes);
        entries.clear();
        int pages = Math.max(1, (result.size() + 9) / 10);
        page = Math.min(page, pages - 1);
        entries.add(theme.label(tr(viewLabel(view)) + " · " + result.size()));
        if (result.isEmpty()) entries.add(theme.label(tr("No matching commands. Add one or send a command in game."), 440));
        for (var entry : result.subList(page * 10, Math.min(result.size(), page * 10 + 10))) {
            entries.add(theme.horizontalSeparator()).expandX();
            var title = entries.add(theme.label(entry.name(), true, 440)).expandX().widget();
            title.tooltip = entry.name();
            var command = entries.add(theme.label(entry.command(), 440)).expandX().widget();
            command.tooltip = entry.command();
            if (!entry.note().isBlank()) {
                var note = entries.add(theme.label(tr("Note") + ": " + entry.note(), 440)).expandX().widget();
                note.tooltip = entry.note();
            }
            String last = entry.lastUsed() == 0 ? tr("Not used yet") : DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                .withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(entry.lastUsed()));
            var detail = entries.add(theme.label((entry.category().isBlank() ? "" : entry.category() + " · ")
                + displayScope(entry.scope()) + " · " + last, 440)).widget();
            detail.tooltip = entry.scope();
            WHorizontalList actions = entries.add(theme.horizontalList()).expandX().widget();
            WButton execute = actions.add(theme.button(tr("Execute"))).widget();
            execute.action = () -> {
                try {
                    var state = book.execute(entry.id());
                    notice.set(tr(switch (state) {
                        case Submitted -> "Command submitted. Check the game response.";
                        case NotConnected -> "Join a world before executing commands.";
                        case WrongScope -> "This command belongs to another server or world.";
                        case InvalidCommand -> "Enter a valid command with / or the current Meteor prefix.";
                    }));
                    refresh();
                } catch (CommandSyntaxException e) { notice.set(CommandErrors.message(e)); }
            };
            WButton fill = actions.add(theme.button(tr("Fill Chat"))).widget();
            fill.action = () -> { if (!book.fillChat(entry.id())) notice.set(tr("Join a world before executing commands.")); };
            WButton star = actions.add(theme.button(tr(entry.favorite() ? "Unfavorite Command" : "Favorite Command"))).widget();
            star.action = () -> { book.favorite(entry.id(), !entry.favorite()); refresh(); };
            if (entry.favorite()) {
                var binding = book.binding(entry.id());
                entries.add(theme.label(tr("Command Hotkey") + ": " + binding, 440));
            }
            WButton edit = actions.add(theme.button(tr("Edit"))).widget();
            edit.action = () -> mc.gui.setScreen(new EditCommandScreen(theme, entry, this::refresh));
            WButton copy = actions.add(theme.button(tr("Copy"))).widget();
            copy.action = () -> { mc.keyboardHandler.setClipboard(entry.command()); notice.set(tr("Command copied.")); };
            WConfirmedMinus remove = actions.add(theme.confirmedMinus()).widget();
            remove.tooltip = tr("Delete command");
            remove.action = () -> { book.remove(entry.id()); refresh(); };
        }
        var footer = entries.add(theme.horizontalList()).expandX().widget();
        if (page > 0) {
            WButton previous = footer.add(theme.button(tr("Previous"))).widget();
            previous.action = () -> { page--; refresh(); };
        }
        footer.add(theme.label((page + 1) + " / " + pages)).expandX();
        if (page + 1 < pages) {
            WButton next = footer.add(theme.button(tr("Next"))).widget();
            next.action = () -> { page++; refresh(); };
        }
        if (view == CommandNotebook.View.History) {
            WConfirmedButton clear = footer.add(theme.confirmedButton("Clear History", "Confirm")).widget();
            clear.action = () -> { book.clearHistory(); refresh(); };
        }
    }

    @Override public void tick() {
        if (revision != CommandBook.get().revision() || !scope.equals(CommandBook.currentScope())) refresh();
    }

    public enum ScopeChoice {
        Global, CurrentContext, KeepExisting;
        @Override public String toString() {
            return switch (this) { case Global -> "Global"; case CurrentContext -> "Current server or world"; case KeepExisting -> "Keep existing scope"; };
        }
    }

    public static final class EditCommandScreen extends WindowScreen {
        private final CommandNotebook.Entry original;
        private final Runnable reload;
        private WTextBox name, command, note, category;
        private WDropdown<ScopeChoice> scope;
        private WLabel error;
        private Keybind draftBinding;
        private WKeybind hotkey;

        public EditCommandScreen(GuiTheme theme, CommandNotebook.Entry original, Runnable reload) {
            super(theme, "Edit Saved Command");
            this.original = original;
            this.reload = reload;
        }

        private WTextBox field(String label, String value) {
            add(theme.label(tr(label)));
            return add(theme.textBox(value)).expandX().minWidth(440).widget();
        }

        @Override public void initWidgets() {
            name = field("Command Name", name != null ? name.get() : original == null ? "" : original.name());
            command = field("Command Text", command != null ? command.get() : original == null ? "/" : original.command());
            note = field("Note", note != null ? note.get() : original == null ? "" : original.note());
            category = field("Category", category != null ? category.get() : original == null ? "" : original.category());
            add(theme.label(tr("Command Scope")));
            ScopeChoice selected = scope != null ? scope.get() : original == null ? (CommandBook.currentScope().isEmpty() ? ScopeChoice.Global : ScopeChoice.CurrentContext)
                : original.scope().equals(CommandNotebook.GLOBAL) ? ScopeChoice.Global : ScopeChoice.KeepExisting;
            scope = add(theme.dropdown(original == null ? new ScopeChoice[] { ScopeChoice.Global, ScopeChoice.CurrentContext }
                : ScopeChoice.values(), selected)).expandX().widget();
            if (original != null) add(theme.label(displayScope(original.scope()), 440));
            add(theme.label(tr("Command Hotkey")));
            if (draftBinding == null) draftBinding = original == null ? Keybind.none() : CommandBook.get().binding(original.id());
            hotkey = add(theme.keybind(draftBinding)).expandX().widget();
            add(theme.label(tr("Click the key, then release a keyboard key or press a mouse side button. Esc clears it."), 440));
            error = add(theme.label("")).expandX().widget();
            WButton save = add(theme.button(tr("Save Command"))).expandX().widget();
            save.action = this::saveEntry;
            // Enter submits text edits normally; executing a command always requires its explicit button.
        }

        private void saveEntry() {
            String text = command.get().strip();
            if (name.get().isBlank() || CommandNotebook.kind(text, Config.get().prefix.get()) == CommandNotebook.Kind.Invalid) {
                error.set(tr("Enter a name and a valid command with / or the current Meteor prefix."));
                return;
            }
            String destination = switch (scope.get()) {
                case Global -> CommandNotebook.GLOBAL;
                case CurrentContext -> CommandBook.currentScope();
                case KeepExisting -> original == null ? "" : original.scope();
            };
            if (destination.isBlank()) { error.set(tr("Join a world to select its command scope.")); return; }
            try {
                UUID id = original == null ? UUID.randomUUID() : original.id();
                if (!CommandBook.get().canBind(id, destination, draftBinding)) {
                    error.set(tr("This hotkey conflicts with another command, module, macro or menu key.")); return;
                }
                CommandBook.get().put(new CommandNotebook.Entry(id,
                    name.get().strip(), text, note.get(), category.get().strip(), destination,
                    original == null ? 0 : original.lastUsed(), true));
                CommandBook.get().bind(id, draftBinding);
                onClose();
            } catch (IllegalArgumentException e) { error.set(tr("Name/category: 128 characters. Command: 256. Note: 2048.")); }
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        private void onBindingKey(KeyInputEvent event) {
            if (mc.gui.screen() != this || hotkey == null) return;
            if (event.action == KeyAction.Press && event.key() == GLFW.GLFW_KEY_ESCAPE && hotkey.onClear()) event.cancel();
            else if (event.action == KeyAction.Release && hotkey.onAction(true, event.key(), event.modifiers())) event.cancel();
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        private void onBindingMouse(MouseClickEvent event) {
            if (mc.gui.screen() == this && hotkey != null && event.action == KeyAction.Press && hotkey.onAction(false, event.button(), 0)) event.cancel();
        }

        @Override protected void onClosed() { if (reload != null) reload.run(); }
    }
}
