package meteordevelopment.meteorclient.systems.modules.misc.carpet;

import java.util.*;

/** Observed server state is separate from unconfirmed requests. */
public final class CarpetState {
    public record Key(String manager, String rule) {}
    public record Pending(String value, long deadline) {}
    private final Map<Key, String> values = new HashMap<>();
    private final Map<Key, Pending> pending = new HashMap<>();
    private final Set<Key> expired = new HashSet<>();
    public int revision;
    public void clear() { values.clear(); pending.clear(); expired.clear(); revision++; }
    public String value(Key key) { return values.get(key); }
    public boolean pending(Key key) { return pending.containsKey(key); }
    public boolean expired(Key key) { return expired.contains(key); }
    public void request(Key key, String value, long now) { pending.put(key, new Pending(value, now + 5000)); expired.remove(key); revision++; }
    public void observe(Key key, String value) {
        values.put(key, value);
        if (pending.containsKey(key) && pending.get(key).value().equals(value)) pending.remove(key);
        expired.remove(key); revision++;
    }
    public void expire(long now) {
        pending.entrySet().removeIf(e -> { if (now < e.getValue().deadline()) return false; expired.add(e.getKey()); revision++; return true; });
    }
    public static boolean safe(String value) { return !value.isBlank() && value.length() <= 256 && value.chars().noneMatch(c -> c < 32 || c == 127); }
    public static String playerCommand(String name, String action) {
        if (!name.matches("[A-Za-z0-9_]{1,16}")) throw new IllegalArgumentException("Invalid player name.");
        if (!Set.of("spawn", "kill", "stop", "attack once", "attack continuous", "use once", "use continuous", "jump once", "jump continuous", "sneak", "unsneak", "sprint", "unsprint", "mount", "dismount").contains(action)) throw new IllegalArgumentException("Invalid player action.");
        return "player " + name + " " + action;
    }
}
