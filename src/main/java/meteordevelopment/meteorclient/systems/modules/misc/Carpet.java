package meteordevelopment.meteorclient.systems.modules.misc;

import com.mojang.brigadier.tree.CommandNode;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.*;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.CarpetWidget;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.misc.carpet.CarpetState;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import java.util.*;

/** Optional reflection bridge avoids a mandatory Carpet dependency. */
public final class Carpet extends Module {
    public record Rule(CarpetState.Key key, List<String> options) {}
    public String source(Rule rule) {
        for (var entry : Map.of("AMS", "carpetamsaddition.CarpetAMSAdditionSettings", "TIS", "carpettisaddition.CarpetTISAdditionSettings", "GCA", "dev.dubhe.gugle.carpet.GcaSetting").entrySet()) {
            try { Class.forName(entry.getValue()).getDeclaredField(rule.key().rule()); return entry.getKey(); }
            catch (ReflectiveOperationException | LinkageError ignored) {}
        }
        return "Carpet";
    }
    private final Map<String, Map<String, String>> translations = new HashMap<>();
    public String ruleText(Rule rule, String suffix) {
        String language = meteordevelopment.meteorclient.utils.misc.Localization.language() == meteordevelopment.meteorclient.utils.misc.Localization.Language.English ? "en_us" : "zh_cn";
        Map<String, String> map = translations.computeIfAbsent(language, lang -> {
            Map<String, String> result = new HashMap<>();
            for (String namespace : List.of("carpet", "carpettisaddition", "gca")) {
                try (var input = getClass().getResourceAsStream("/assets/" + namespace + "/lang/" + lang + ".json")) {
                    if (input != null) {
                        var json = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(input, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                        json.entrySet().forEach(e -> { if (e.getValue().isJsonPrimitive()) result.put(e.getKey(), e.getValue().getAsString()); });
                    }
                } catch (java.io.IOException | RuntimeException ignored) {}
            }
            return result;
        });
        for (String prefix : List.of("carpet.rule.", "carpettisaddition.rule.", "gca.rule.")) {
            String found = map.get(prefix + rule.key().rule() + "." + suffix);
            if (found != null) return found;
        }
        return suffix.equals("name") ? rule.key().rule() : "/" + rule.key().manager() + " " + rule.key().rule();
    }
    public final CarpetState state = new CarpetState();
    private final SettingGroup general = settings.getDefaultGroup();
    public final Setting<String> playerName = general.add(new StringSetting.Builder().name("fake-player-name").description("Server-side fake player name.").defaultValue("AlecBot").build());
    public final Setting<Boolean> sensitive = general.add(new BoolSetting.Builder().name("allow-sensitive-rules").description("Allow changing command permissions, scripts and explosion-related rules.").defaultValue(false).build());
    private CarpetWidget widget;
    private Object connection;
    private long lastSend;
    private long lastLocalSync;
    public String notice = "";
    public Carpet() {
        super(Categories.Misc, "carpet", "Control Carpet rules and server-side fake players.");
        autoSubscribe = false;
        runInMainMenu = true;
        MeteorClient.EVENT_BUS.subscribe(this);
    }
    @Override public WWidget getWidget(GuiTheme theme) { return widget = new CarpetWidget(this); }
    public void tickWidget() {
        if (widget != null) widget.poll();
        var server = mc.getSingleplayerServer();
        if (server == null || System.currentTimeMillis() - lastLocalSync < 1000) return;
        lastLocalSync = System.currentTimeMillis();
        var current = mc.getConnection();
        var catalog = rules();
        server.execute(() -> {
            Map<CarpetState.Key, String> observed = new HashMap<>();
            try {
                Class<?> api = Class.forName("carpet.api.settings.SettingsManager");
                Class<?> ruleApi = Class.forName("carpet.api.settings.CarpetRule");
                Object manager = Class.forName("carpet.CarpetServer").getField("settingsManager").get(null);
                for (var r : catalog) if (r.key().manager().equals("carpet")) {
                    Object rule = api.getMethod("getCarpetRule", String.class).invoke(manager, r.key().rule());
                    if (rule != null) observed.put(r.key(), String.valueOf(ruleApi.getMethod("value").invoke(rule)));
                }
            } catch (ReflectiveOperationException | LinkageError ignored) {}
            mc.execute(() -> { if (mc.getConnection() == current && mc.getSingleplayerServer() == server) observed.forEach((k,v) -> { if (!Objects.equals(state.value(k),v) || state.pending(k)) state.observe(k,v); }); });
        });
    }
    @Override public void onDeactivate() { notice = "Controls disabled. Server rules are unchanged."; }
    @EventHandler private void joined(GameJoinedEvent event) { reset(); }
    @EventHandler private void left(GameLeftEvent event) { reset(); }
    private void reset() { state.clear(); connection = mc.getConnection(); notice = ""; lastSend = 0; }
    @EventHandler private void received(PacketEvent.Receive event) {
        if (!(event.packet instanceof ClientboundCustomPayloadPacket packet)) return;
        var payload = packet.payload();
        if (!payload.type().id().toString().equals("carpet:hello")) return;
        try {
            Object raw = payload.getClass().getMethod("data").invoke(payload);
            if (!(raw instanceof CompoundTag tag)) return;
            var rules = tag.getCompound("Rules");
            if (rules.isEmpty()) return;
            CompoundTag snapshot = rules.get().copy();
            var source = event.connection;
            mc.execute(() -> {
                if (mc.getConnection() == null || mc.getConnection().getConnection() != source) return;
                if (connection != mc.getConnection()) reset();
                acceptRules(snapshot);
            });
        } catch (ReflectiveOperationException ignored) { /* Unknown optional payload implementation. */ }
    }
    public void acceptRules(CompoundTag rules) {
        for (String id : rules.keySet()) rules.getCompound(id).ifPresent(rule -> {
            var key = new CarpetState.Key(rule.getStringOr("Manager", "carpet"), rule.getStringOr("Rule", id));
            rule.getString("Value").ifPresent(value -> state.observe(key, value));
        });
    }
    public CommandNode<?> root() { return mc.getConnection() == null ? null : mc.getConnection().getCommands().getRoot(); }
    public boolean available(String command) { return root() != null && root().getChild(command) != null; }
    public List<Rule> rules() {
        List<Rule> result = new ArrayList<>();
        try {
            Class<?> server = Class.forName("carpet.CarpetServer");
            List<Object> managers = new ArrayList<>();
            managers.add(server.getField("settingsManager").get(null));
            Class<?> extension = Class.forName("carpet.CarpetExtension");
            for (Object ext : (Iterable<?>) server.getField("extensions").get(null)) {
                Object manager = extension.getMethod("extensionSettingsManager").invoke(ext);
                if (manager != null) managers.add(manager);
            }
            Class<?> api = Class.forName("carpet.api.settings.SettingsManager");
            Class<?> ruleApi = Class.forName("carpet.api.settings.CarpetRule");
            for (Object manager : managers) {
                if (manager == null) continue;
                String identifier = (String) api.getMethod("identifier").invoke(manager);
                if (!available(identifier)) continue;
                for (Object rule : (Iterable<?>) api.getMethod("getCarpetRules").invoke(manager)) {
                    String name = (String) ruleApi.getMethod("name").invoke(rule);
                    List<String> options = new ArrayList<>();
                    for (Object option : (Iterable<?>) ruleApi.getMethod("suggestions").invoke(rule)) options.add(option.toString());
                    result.add(new Rule(new CarpetState.Key(identifier, name), options));
                }
            }
        } catch (ReflectiveOperationException | LinkageError ex) { notice = "Install matching Carpet client mods for the complete rule catalog and synchronization."; }
        result.sort(Comparator.comparing(r -> r.key().manager() + ":" + r.key().rule()));
        return result;
    }
    public static boolean sensitive(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        return n.contains("command") || n.contains("permission") || n.contains("explos") || n.contains("tnt") || n.contains("scarpet") || n.contains("script");
    }
    public void setRule(Rule rule, String value) {
        if (sensitive(rule.key().rule()) && !sensitive.get()) { notice = "Enable sensitive rules before changing this rule."; return; }
        if (!CarpetState.safe(value) || value.length() > 128) { notice = "Invalid command."; return; }
        if (send(rule.key().manager() + " " + rule.key().rule() + " " + value)) state.request(rule.key(), value, System.currentTimeMillis());
    }
    public void player(String action) {
        try { send(CarpetState.playerCommand(playerName.get(), action)); }
        catch (IllegalArgumentException ex) { notice = ex.getMessage(); }
    }
    public boolean send(String command) {
        if (!isActive()) { notice = "Enable Carpet before sending commands."; return false; }
        if (mc.player == null || mc.getConnection() == null) { notice = "Join a server first."; return false; }
        if (!CarpetState.safe(command) || command.startsWith("/")) { notice = "Invalid command."; return false; }
        if (!available(command.split(" ", 2)[0])) { notice = "Command unavailable or permission denied."; return false; }
        long now = System.currentTimeMillis();
        if (now - lastSend < 400) { notice = "Please wait before sending another command."; return false; }
        lastSend = now;
        mc.player.connection.sendCommand(command);
        notice = "Command sent; check server feedback.";
        return true;
    }
}
