package meteordevelopment.meteorclient.systems.modules.misc.carpet;

public final class CarpetStateTest {
    private static int count;
    private static void check(boolean pass, String name) { if (!pass) throw new AssertionError(name); count++; System.out.println("PASS " + name); }
    public static void main(String[] args) {
        var state = new CarpetState(); var key = new CarpetState.Key("carpet", "flippinCactus");
        check(state.value(key) == null, "Initial state unknown");
        state.observe(key, "false"); state.request(key, "true", 0);
        check(state.value(key).equals("false"), "Request does not claim success");
        check(state.pending(key), "Request pending");
        state.observe(key, "false"); check(state.pending(key), "Conflicting observation not success");
        state.observe(key, "true"); check(!state.pending(key), "Matching server observation confirms");
        state.request(key, "false", 100); state.expire(5099); check(!state.expired(key), "No early expiry");
        state.expire(5100); check(state.expired(key) && !state.pending(key), "Unconfirmed request expires");
        check(state.value(key).equals("true"), "Timeout retains observed state");
        state.observe(key, "false"); check(!state.expired(key), "Late update clears timeout");
        state.clear(); check(state.value(key) == null && !state.pending(key), "Disconnect clears session state");
        var other = new CarpetState.Key("gca", key.rule());
        state.observe(other, "true"); check(state.value(key) == null, "Manager namespaces isolated");
        check(CarpetState.playerCommand("AlecBot", "spawn").equals("player AlecBot spawn"), "Spawn serialization");
        for (String invalid : new String[]{"", "@a", "A B", "12345678901234567", "a\nkill"}) {
            boolean rejected = false; try { CarpetState.playerCommand(invalid, "spawn"); } catch (IllegalArgumentException e) { rejected = true; }
            check(rejected, "Invalid fake player name rejected");
        }
        boolean rejected = false; try { CarpetState.playerCommand("Alec", "op"); } catch (IllegalArgumentException e) { rejected = true; }
        check(rejected, "Unknown action rejected");
        check(!CarpetState.safe("a\nb") && !CarpetState.safe("") && !CarpetState.safe("x".repeat(257)), "Control characters and oversized input rejected");
        check(CarpetState.safe("carpet flippinCactus true"), "Valid rule command accepted");
        System.out.println("Carpet: " + count + " tests passed.");
    }
}
