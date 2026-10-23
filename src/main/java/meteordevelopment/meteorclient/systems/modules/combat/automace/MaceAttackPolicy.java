/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */
package meteordevelopment.meteorclient.systems.modules.combat.automace;

import java.util.function.IntPredicate;

/** Pure decision layer. It neither sends packets nor predicts a confirmed hit. */
public final class MaceAttackPolicy {
    public enum Status {
        Disabled, Conflict, NoTarget, NoMace, ManualOverride, WaitingForFall,
        UnsafeMovement, InsufficientFall, OutOfRange, Obstructed, LowHealth,
        WaitingForCooldown, Attempted, Confirmed, Unconfirmed, Ready
    }

    public record Conditions(boolean active, boolean conflict, boolean targetValid, boolean mace,
                             boolean falling, boolean unsafeMovement, double fallDistance, double minFall,
                             boolean inRange, boolean visible, boolean healthy, boolean cooled) {}

    private boolean active;
    private boolean consumedFall;

    public void activate() { active = true; consumedFall = false; }
    public void deactivate() { active = false; consumedFall = false; }
    public void landed() { consumedFall = false; }

    public Status evaluate(Conditions c) {
        if (!active || !c.active) return Status.Disabled;
        if (c.conflict) return Status.Conflict;
        if (!c.targetValid) return Status.NoTarget;
        if (!c.mace) return Status.NoMace;
        if (consumedFall) return Status.Attempted;
        if (c.unsafeMovement) return Status.UnsafeMovement;
        if (!c.falling) return Status.WaitingForFall;
        if (!Double.isFinite(c.fallDistance) || c.fallDistance <= Math.max(1.5, c.minFall)) return Status.InsufficientFall;
        if (!c.healthy) return Status.LowHealth;
        if (!c.inRange) return Status.OutOfRange;
        if (!c.visible) return Status.Obstructed;
        if (!c.cooled) return Status.WaitingForCooldown;
        return Status.Ready;
    }

    public boolean claimAttack(Conditions c) {
        if (evaluate(c) != Status.Ready) return false;
        consumedFall = true;
        return true;
    }

    /** Adapted from AutoMaceswap's CC0 MaceSwapMixin hotbar scan. See AUTO_MACE_SOURCES.md. */
    public static int findMace(IntPredicate isMace) {
        for (int i = 0; i < 9; i++) {
            if (isMace.test(i)) return i;
        }
        return -1;
    }

    /** Slot ownership avoids undoing a subsequent manual or other-module selection. */
    public static final class SlotLease {
        private int original = -1;
        private int equipped = -1;
        public void acquire(int current, int mace) {
            if (mace < 0 || mace > 8 || current < 0 || current > 8 || current == mace) return;
            if (original == -1) original = current;
            equipped = mace;
        }
        public boolean interrupted(int current) { return equipped != -1 && equipped != current; }
        public int restore(int current, boolean enabled, boolean stillMace) {
            int result = enabled && current == equipped && stillMace ? original : -1;
            clear();
            return result;
        }
        public void clear() { original = equipped = -1; }
    }
}
