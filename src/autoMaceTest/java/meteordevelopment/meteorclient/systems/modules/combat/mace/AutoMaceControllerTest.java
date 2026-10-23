package meteordevelopment.meteorclient.systems.modules.combat.mace;

import static meteordevelopment.meteorclient.systems.modules.combat.mace.AutoMaceController.*;

/** Actual executable regression tests; inputs represent observations, not simulated server hits. */
public final class AutoMaceControllerTest {
    private static int passed;
    private static final class Fixture {
        final AutoMaceController c = new AutoMaceController();
        Settings s = new Settings(1.5, .15, 4, 8, 2, 2, 12, true, true, false, false);
        boolean enabled = true, valid = true, search = true, reach = true, visible = true, predicted = true,
            vanilla = true, known = true, ready = true, windReady = true, safe = true;
        boolean ground, slow, flying, fluid, screen, using, conflict;
        int tick, target = 42, selected, mace, wind = 2;
        double velocity = -.6, fall = 3, health = 20, damage = 1, bonus = 12;
        Fixture() { c.activate(); }
        Decision step() {
            Decision d = c.tick(new Input(enabled, tick++, target, valid, search, reach, visible, predicted,
                vanilla, ground, velocity, fall, slow, flying, fluid, screen, using, conflict, health,
                damage, known, bonus, selected, mace, ready, wind, windReady, safe), s);
            if (d.action() == Action.Select || d.action() == Action.Restore) selected = d.slot();
            return d;
        }
        void assist(boolean combo) { s = new Settings(1.5, .15, 4, 8, 2, 2, 12, true, true, true, combo); }
        void confirm(boolean smash) { c.serverDamage(tick, target, true, smash, 12); }
    }
    private static void check(boolean condition) { if (!condition) throw new AssertionError(); }
    private static void noAttack(Fixture f) { check(f.step().action() != Action.Attack); }
    private static void test(String name, Runnable body) {
        try { body.run(); passed++; System.out.println("PASS " + name); }
        catch (Throwable t) { throw new AssertionError("FAIL " + name, t); }
    }
    public static void main(String[] args) {
        test("01 insufficient fall distance", () -> {
            for (double distance : new double[]{0, 1, 1.5, 1.65}) { Fixture f = new Fixture(); f.fall = distance; noAttack(f); }
        });
        test("02 missing mace does not select", () -> {
            Fixture f = new Fixture(); f.mace = -1; check(f.step().action() == Action.None); check(f.selected == 0);
        });
        test("03 dead or disappeared target cancels pending attack", () -> {
            Fixture f = new Fixture(); check(f.step().action() == Action.Attack); f.valid = false; noAttack(f);
            check(f.c.pendingTarget() == -1); f.confirm(true); check(f.c.lastResult() == State.Failed);
            Fixture lost = new Fixture(); lost.target = -1; lost.valid = false; noAttack(lost);
        });
        test("04 search and attack ranges", () -> {
            Fixture f = new Fixture(); f.search = false; noAttack(f);
            f = new Fixture(); f.reach = false; noAttack(f);
            f = new Fixture(); f.step(); f.reach = false; noAttack(f); check(f.c.pendingTarget() == -1);
        });
        test("05 slow falling with stale fall distance", () -> { Fixture f = new Fixture(); f.slow = true; noAttack(f); check(f.c.reason() == Reason.SlowFalling); });
        test("06 fatal and unknown landing", () -> {
            Fixture f = new Fixture(); f.damage = 19; noAttack(f); check(f.c.reason() == Reason.FatalFallRisk);
            f = new Fixture(); f.known = false; noAttack(f); check(f.c.reason() == Reason.UnknownLanding);
        });
        test("07 disable restores and invalidates queued facts", () -> {
            Fixture f = new Fixture(); f.mace = 3; check(f.step().action() == Action.Select);
            Decision d = f.c.deactivate(f.selected); check(d.action() == Action.Restore && d.slot() == 0);
            for (int i = 0; i < 30; i++) check(f.step().action() != Action.Attack);
            f.confirm(true); check(f.c.state() == State.Disabled);
        });
        test("08 one attack over 100 airborne ticks", () -> {
            Fixture f = new Fixture(); int attacks = 0;
            for (int i = 0; i < 100; i++) if (f.step().action() == Action.Attack) attacks++;
            check(attacks == 1); check(f.c.lastResult() == State.Failed);
        });
        test("09 switch synchronization and restoration", () -> {
            Fixture f = new Fixture(); f.mace = 3; check(f.step().action() == Action.Select);
            noAttack(f); check(f.step().action() == Action.Attack); f.confirm(true);
            check(f.step().action() == Action.Restore); check(f.selected == 0);
        });
        test("10 missing cooling or failed wind charge", () -> {
            Fixture f = new Fixture(); f.assist(false); f.ground = true; f.wind = -1; check(f.step().action() == Action.None);
            f = new Fixture(); f.assist(false); f.ground = true; f.windReady = false; check(f.step().action() == Action.None);
            f = new Fixture(); f.assist(false); f.ground = true; f.wind = 0;
            check(f.step().action() == Action.WindCharge); f.c.windFailed();
            for (int i = 0; i < 30; i++) check(f.step().action() != Action.WindCharge);
        });
        test("11 wrong attacker target and ordinary hit", () -> {
            Fixture f = new Fixture(); f.step(); f.c.serverDamage(f.tick, 42, false, true, 12); check(f.c.pendingTarget() == 42);
            f.c.serverDamage(f.tick, 43, true, true, 12); check(f.c.pendingTarget() == 42);
            f.confirm(false); check(f.c.lastResult() == State.Failed); check(f.c.reason() == Reason.OrdinaryHit);
        });
        test("12 only confirmed smash arms combo", () -> {
            for (boolean smash : new boolean[]{false, true}) {
                Fixture f = new Fixture(); f.assist(true); f.wind = 0; f.ground = true; f.step();
                f.ground = false; check(f.step().action() == Action.Attack); f.confirm(smash); f.step();
                f.ground = true; check((f.step().action() == Action.WindCharge) == smash);
            }
        });
        test("13 manual slot changes retained", () -> {
            Fixture f = new Fixture(); f.mace = 3; f.step(); f.selected = 5;
            check(f.step().action() == Action.None); check(f.c.deactivate(5).action() == Action.None); check(f.selected == 5);
        });
        test("14 cooldown visibility prediction damage gates", () -> {
            Fixture f = new Fixture(); f.ready = false; noAttack(f);
            f = new Fixture(); f.visible = false; noAttack(f);
            f = new Fixture(); f.predicted = false; noAttack(f);
            f = new Fixture(); f.bonus = 3; noAttack(f);
        });
        test("15 flight fluid low health UI and conflicts", () -> {
            Fixture f = new Fixture(); f.flying = true; noAttack(f);
            f = new Fixture(); f.fluid = true; noAttack(f);
            f = new Fixture(); f.health = 7; noAttack(f);
            f = new Fixture(); f.screen = true; noAttack(f);
            f = new Fixture(); f.using = true; noAttack(f);
            f = new Fixture(); f.conflict = true; noAttack(f);
        });
        test("16 wind needs observed launch", () -> {
            Fixture f = new Fixture(); f.assist(false); f.ground = true; f.wind = 0;
            check(f.step().action() == Action.WindCharge);
            for (int i = 0; i < 22; i++) noAttack(f);
            check(f.c.lastResult() == State.Failed);
            for (int i = 0; i < 50; i++) check(f.step().action() != Action.WindCharge);
        });
        test("17 auto switch and switch back preferences", () -> {
            Fixture f = new Fixture(); f.mace = 3; f.s = new Settings(1.5,.15,4,8,2,2,12,false,false,false,false);
            check(f.step().action() == Action.None);
            f = new Fixture(); f.mace = 3; f.s = new Settings(1.5,.15,4,8,2,2,12,true,false,false,false);
            f.step(); f.step(); check(f.step().action() == Action.Attack); f.confirm(true);
            check(f.step().action() == Action.None); check(f.selected == 3);
            check(f.c.deactivate(3).action() == Action.Restore);
        });
        test("18 rising and vanilla smash predicate", () -> {
            Fixture f = new Fixture(); f.velocity = .3; noAttack(f);
            f = new Fixture(); f.vanilla = false; noAttack(f);
        });
        test("19 late and cancelled facts cannot confirm", () -> {
            Fixture f = new Fixture(); f.step(); f.c.serverDamage(30,42,true,true,12); check(f.c.lastResult() == State.Attempted);
            f.c.attackCancelled(); f.confirm(true); check(f.c.lastResult() == State.Failed); check(f.c.pendingTarget() == -1);
        });
        test("20 vanilla unenchanted smash bonus boundaries", () -> {
            check(smashBonus(1.5) == 0); check(smashBonus(3) == 12); check(smashBonus(8) == 22); check(smashBonus(10) == 24);
        });
        System.out.println("AUTO_MACE_TESTS: " + passed + "/20 PASS");
    }
}
