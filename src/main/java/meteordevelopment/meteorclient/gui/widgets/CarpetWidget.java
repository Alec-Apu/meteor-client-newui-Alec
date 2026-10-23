package meteordevelopment.meteorclient.gui.widgets;

import meteordevelopment.meteorclient.gui.widgets.containers.*;
import meteordevelopment.meteorclient.systems.modules.misc.Carpet;
import meteordevelopment.meteorclient.utils.misc.Localization;
import java.util.*;

/** Embedded in the standard module settings panel. */
public final class CarpetWidget extends WVerticalList {
    private final Carpet module;
    private WVerticalList content;
    private WLabel feedback;
    private String search = "";
    private int page, revision = -1;
    private List<Carpet.Rule> catalog = List.of();
    private final Map<String, String> drafts = new HashMap<>();
    public CarpetWidget(Carpet module) { this.module = module; }
    private String tr(String text) { return Localization.text(text); }
    private void button(WContainer parent, String text, Runnable action) {
        var b = parent.add(theme.button(tr(text))).widget(); b.action = action;
    }
    @Override public void init() {
        spacing = 12;
        add(theme.label(tr("Changes require server permission. Unknown state is never shown as enabled.")));
        feedback = add(theme.label("")).widget();
        var fake = add(theme.section(tr("Server Fake Player"), true)).expandX().widget();
        var name = fake.add(theme.textBox(module.playerName.get())).expandX().widget();
        name.action = () -> module.playerName.set(name.get());
        var spawn = fake.add(theme.horizontalList()).widget();
        button(spawn, "Spawn Fake Player", () -> module.player("spawn"));
        button(spawn, "Remove Fake Player", () -> module.player("kill"));
        button(spawn, "Stop All Actions", () -> module.player("stop"));
        var actions = fake.add(theme.table()).widget();
        int index = 0;
        for (String action : List.of("attack once", "attack continuous", "use once", "use continuous", "jump once", "jump continuous", "sneak", "unsneak", "sprint", "unsprint", "mount", "dismount")) {
            button(actions, "Player: " + action, () -> module.player(action));
            if (++index % 3 == 0) actions.row();
        }
        var filter = add(theme.horizontalList()).expandX().widget();
        var query = filter.add(theme.textBox(search, tr("Search rules..."))).expandX().widget();
        query.action = () -> { search = query.get(); page = 0; rebuild(); };
        button(filter, "Refresh Catalog", () -> { catalog = module.rules(); rebuild(); });
        content = add(theme.verticalList()).expandX().widget();
        var custom = add(theme.section(tr("Other Carpet Commands"), false)).expandX().widget();
        custom.add(theme.label(tr("Use exact commands exposed by this server; do not include the leading slash.")));
        var command = custom.add(theme.textBox("")).expandX().widget();
        var browser = custom.add(theme.verticalList()).expandX().widget();
        Runnable[] browse = new Runnable[1];
        browse[0] = () -> {
            browser.clear();
            var node = module.root();
            if (node == null) return;
            for (String token : command.get().strip().split(" +")) {
                if (token.isEmpty()) continue;
                var next = node.getChild(token);
                if (next == null) next = node.getChildren().stream().filter(n -> n instanceof com.mojang.brigadier.tree.ArgumentCommandNode<?, ?>).findFirst().orElse(null);
                if (next == null) return;
                node = next;
            }
            int count = 0;
            for (var child : node.getChildren()) {
                if (++count > 60) break;
                String namePart = child.getName();
                if (child instanceof com.mojang.brigadier.tree.LiteralCommandNode<?>) button(browser, namePart, () -> {
                    command.set((command.get().strip() + " " + namePart).strip()); browse[0].run();
                });
                else browser.add(theme.label("<" + namePart + ">"));
            }
        };
        command.action = browse[0];
        button(custom, "Browse Command Tree", browse[0]);
        button(custom, "Execute", () -> module.send(command.get()));
        button(custom, "List Available Commands", () -> {
            if (module.root() == null) return;
            module.info(String.join(", ", module.root().getChildren().stream().map(n -> "/" + n.getName()).sorted().toList()));
        });
        catalog = module.rules(); rebuild();
    }
    public void poll() {
        module.state.expire(System.currentTimeMillis());
        if (content != null && revision != module.state.revision) rebuild();
        if (feedback != null) feedback.set(tr(module.notice));
    }
    private void rebuild() {
        if (content == null) return;
        revision = module.state.revision; content.clear();
        var matches = catalog.stream().filter(r -> (module.source(r) + " " + r.key().manager() + " " + r.key().rule() + " " + module.ruleText(r, "name") + " " + module.ruleText(r, "desc")).toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT))).toList();
        int pages = Math.max(1, (matches.size() + 7) / 8); page = Math.min(page, pages - 1);
        var navigation = content.add(theme.horizontalList()).widget();
        button(navigation, "Previous", () -> { page = Math.max(0, page - 1); rebuild(); });
        navigation.add(theme.label((page + 1) + " / " + pages + " (" + matches.size() + ")"));
        button(navigation, "Next", () -> { page = Math.min(pages - 1, page + 1); rebuild(); });
        if (matches.isEmpty()) content.add(theme.label(tr("Join a Carpet server, then refresh the catalog.")));
        for (var rule : matches.subList(page * 8, Math.min(matches.size(), page * 8 + 8))) {
            var row = content.add(theme.section(module.source(rule) + " / " + module.ruleText(rule, "name"), true)).expandX().widget();
            row.tooltip = module.ruleText(rule, "desc");
            String value = module.state.value(rule.key());
            row.add(theme.label(tr("Server State") + ": " + (value == null ? tr("Unknown") : value) + (module.state.pending(rule.key()) ? " / " + tr("Awaiting confirmation") : module.state.expired(rule.key()) ? " / " + tr("Unconfirmed / timed out") : "")));
            String key = "carpet.rule." + rule.key().rule() + ".desc";
            String desc = net.minecraft.locale.Language.getInstance().getOrDefault(key);
            if (!key.equals(desc)) row.add(theme.label(desc));
            var controls = row.add(theme.horizontalList()).expandX().widget();
            if (rule.options().equals(List.of("true", "false")) || rule.options().equals(List.of("false", "true"))) {
                button(controls, "Enable Rule", () -> module.setRule(rule, "true"));
                button(controls, "Disable Rule", () -> module.setRule(rule, "false"));
            } else {
                var input = controls.add(theme.textBox(drafts.getOrDefault(rule.key().toString(), value == null ? "" : value))).expandX().widget();
                input.action = () -> drafts.put(rule.key().toString(), input.get());
                input.tooltip = String.join(", ", rule.options());
                button(controls, "Apply Rule", () -> module.setRule(rule, input.get()));
            }
            button(controls, "Query Rule", () -> module.send(rule.key().manager() + " " + rule.key().rule()));
        }
    }
}
