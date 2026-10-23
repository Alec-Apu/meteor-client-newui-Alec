package meteordevelopment.meteorclient.systems.modules.combat.automace;

import static meteordevelopment.meteorclient.systems.modules.combat.automace.MaceAttackPolicy.*;

/** Dependency-free executable regression tests, run by Gradle check. */
public final class MaceAttackPolicyTest {
    private static int passed;
    private static void check(boolean ok, String name) {
        if (!ok) throw new AssertionError(name);
        passed++;
        System.out.println("PASS " + name);
    }
    private static final class Fall {
        boolean active = true, conflict, target = true, mace = true, falling = true, unsafe;
        double distance = 4, minimum = 1.6;
        boolean range = true, visible = true, healthy = true, cooled = true;
        Conditions conditions() {
            return new Conditions(active, conflict, target, mace, falling, unsafe, distance, minimum,
                range, visible, healthy, cooled);
        }
    }
    public static void main(String[] args) {
        MaceAttackPolicy p = new MaceAttackPolicy();
        Fall f = new Fall();
        check(!p.claimAttack(f.conditions()), "Inactive policy sends no attack");
        p.activate();
        f.distance = 1.5;
        check(!p.claimAttack(f.conditions()), "Vanilla 1.5 boundary is insufficient");
        f.distance = 1.6;
        check(!p.claimAttack(f.conditions()), "Safety margin boundary is insufficient");
        f.distance = Double.NaN;
        check(!p.claimAttack(f.conditions()), "Non-finite fall distance rejected");
        f.distance = 4; f.mace = false;
        check(!p.claimAttack(f.conditions()), "Missing mace prevents attack");
        f.mace = true; f.target = false;
        check(!p.claimAttack(f.conditions()), "Dead or disappeared target cancels attack");
        f.target = true; f.range = false;
        check(!p.claimAttack(f.conditions()), "Out-of-range target cancels attack");
        f.range = true; f.visible = false;
        check(!p.claimAttack(f.conditions()), "Blocked hitbox cancels attack");
        f.visible = true; f.unsafe = true;
        check(!p.claimAttack(f.conditions()), "Slow falling / elytra / water prevents smash attempt");
        f.unsafe = false; f.falling = false;
        check(!p.claimAttack(f.conditions()), "Grounded or ascending player cannot attack");
        f.falling = true; f.healthy = false;
        check(!p.claimAttack(f.conditions()), "Low health safety gate prevents attack");
        f.healthy = true; f.cooled = false;
        check(!p.claimAttack(f.conditions()), "Switch sync and attack cooldown must finish");
        f.cooled = true; f.conflict = true;
        check(!p.claimAttack(f.conditions()), "Competing combat module pauses attacks");
        f.conflict = false;
        check(p.claimAttack(f.conditions()), "Valid cooled vanilla fall permits one attempt");
        for (int i = 0; i < 200; i++) if (p.claimAttack(f.conditions())) throw new AssertionError("Repeated attack");
        check(true, "200 subsequent ticks cannot repeat attack in one fall");
        f.target = false; p.evaluate(f.conditions()); f.target = true;
        check(!p.claimAttack(f.conditions()), "Target loss and reacquisition cannot rearm same fall");
        p.landed();
        check(p.claimAttack(f.conditions()), "Landing rearms next fall");
        p.deactivate(); p.landed();
        check(!p.claimAttack(f.conditions()), "Disable clears state and blocks later actions");
        p.activate(); f.active = false;
        check(!p.claimAttack(f.conditions()), "Module-active guard invalidates late work");
        check(findMace(i -> false) == -1, "No mace produces no slot");
        check(findMace(i -> i == 5) == 5, "CC0 selector finds hotbar mace");
        check(findMace(i -> i == 9) == -1, "Hotbar selector cannot select backpack as hotbar");
        SlotLease lease = new SlotLease();
        lease.acquire(2, -1);
        check(lease.restore(2, true, true) == -1, "Missing mace cannot acquire weapon switch");
        lease.acquire(2, 5);
        check(lease.restore(5, true, true) == 2, "Owned switch restores original slot");
        lease.acquire(2, 5);
        check(lease.restore(5, false, true) == -1, "Switch Back disabled leaves equipped slot");
        lease.acquire(2, 5);
        check(lease.interrupted(3) && lease.restore(3, true, true) == -1, "Manual selection is respected");
        lease.acquire(2, 5);
        check(lease.restore(5, true, false) == -1, "Changed hotbar contents cancel restoration");
        check(lease.restore(5, true, true) == -1, "Restoration clears lease and cannot run twice");
        System.out.println("Auto Mace policy: " + passed + " tests passed.");
    }
}
