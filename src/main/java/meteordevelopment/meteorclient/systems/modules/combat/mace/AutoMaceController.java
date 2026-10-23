/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */
package meteordevelopment.meteorclient.systems.modules.combat.mace;

/** Minecraft-independent decisions. An attack decision is a request, never evidence of a hit. */
public final class AutoMaceController {
    public enum Action { None, Select, Attack, WindCharge, Restore }
    public enum State { Disabled, WaitingJump, WaitingFall, Switching, Preparing, Attempted, Confirmed, Failed, Paused }
    public enum Reason {
        None, Disabled, NoTarget, TargetLost, OutOfRange, NotVisible, NotFalling, FallTooShort,
        NoMace, SwitchDisabled, Synchronizing, AttackCoolingDown, SlowFalling, FallFlying,
        InFluid, LowHealth, FatalFallRisk, UnknownLanding, LowDamageGain, UnsafeTiming,
        ItemInUse, ScreenOpen, CombatConflict, SlotChanged, AlreadyAttempted, AwaitingServer,
        ConfirmationTimeout, OrdinaryHit, NoWindCharge, WindCoolingDown, UnsafeLaunch,
        AwaitingLaunch, LaunchFailed, ServerSmashConfirmed
    }

    public record Settings(double minFall, double margin, double minGain, double minHealth,
                           double healthReserve, int syncTicks, int confirmationTicks,
                           boolean autoSwitch, boolean switchBack, boolean windCharges, boolean autoCombo) {}

    public record Input(boolean enabled, int tick, int targetId, boolean targetValid, boolean inSearch,
                        boolean inReach, boolean visible, boolean predictedHit, boolean vanillaSmash,
                        boolean onGround, double velocityY, double fallDistance, boolean slowFalling,
                        boolean fallFlying, boolean inFluid, boolean screenOpen, boolean usingItem,
                        boolean combatConflict, double health, double fallDamage, boolean landingKnown,
                        double damageGain, int selectedSlot, int maceSlot, boolean attackReady,
                        int windSlot, boolean windReady, boolean safeLaunch) {}

    public record Decision(Action action, int slot) {
        public static final Decision NONE = new Decision(Action.None, -1);
    }

    private boolean active, attempted, windUsed, waitingLaunch, airAfterLaunch, confirmedCombo;
    private int originalSlot = -1, ownedSlot = -1, switchTick, pendingTarget = -1, attemptTick, launchTick;
    private State state = State.Disabled, lastResult = State.Disabled;
    private Reason reason = Reason.Disabled;

    public State state() { return state; }
    public State lastResult() { return lastResult; }
    public Reason reason() { return reason; }
    public int pendingTarget() { return pendingTarget; }
    public int ownedSlot() { return ownedSlot; }

    public void activate() {
        active = true;
        attempted = windUsed = waitingLaunch = airAfterLaunch = confirmedCombo = false;
        originalSlot = ownedSlot = pendingTarget = -1;
        state = State.WaitingJump;
        lastResult = State.Disabled;
        reason = Reason.None;
    }

    public Decision deactivate(int selectedSlot) {
        active = false;
        pendingTarget = -1;
        waitingLaunch = false;
        state = State.Disabled;
        reason = Reason.Disabled;
        return restore(selectedSlot);
    }

    private Decision restore(int selectedSlot) {
        int slot = originalSlot;
        boolean restore = slot >= 0 && selectedSlot == ownedSlot && selectedSlot != slot;
        originalSlot = ownedSlot = -1;
        return restore ? new Decision(Action.Restore, slot) : Decision.NONE;
    }

    private Decision select(Input in, int slot) {
        if (originalSlot < 0) originalSlot = in.selectedSlot;
        ownedSlot = slot;
        switchTick = in.tick;
        state = State.Switching;
        reason = Reason.Synchronizing;
        return new Decision(Action.Select, slot);
    }

    private Decision pause(Input in, Settings settings, Reason why) {
        state = State.Paused;
        reason = why;
        return settings.switchBack ? restore(in.selectedSlot) : Decision.NONE;
    }

    public Decision tick(Input in, Settings settings) {
        if (!active || !in.enabled) return deactivate(in.selectedSlot);
        if (ownedSlot >= 0 && in.selectedSlot != ownedSlot) {
            // Manual changes or protection modules own the slot now. Never undo their selection.
            originalSlot = ownedSlot = pendingTarget = -1;
            attempted = true;
            waitingLaunch = false;
            return pause(in, settings, Reason.SlotChanged);
        }
        if (pendingTarget >= 0) {
            if (!in.targetValid || in.targetId != pendingTarget || !in.inSearch || !in.inReach) {
                pendingTarget = -1;
                lastResult = State.Failed;
                reason = Reason.TargetLost;
            } else if (in.tick - attemptTick > settings.confirmationTicks) {
                pendingTarget = -1;
                lastResult = State.Failed;
                reason = Reason.ConfirmationTimeout;
            } else {
                state = State.Attempted;
                reason = Reason.AwaitingServer;
                return Decision.NONE;
            }
            state = State.Failed;
            return settings.switchBack ? restore(in.selectedSlot) : Decision.NONE;
        }
        if (in.screenOpen) return pause(in, settings, Reason.ScreenOpen);
        if (in.usingItem) return pause(in, settings, Reason.ItemInUse);
        if (in.combatConflict) return pause(in, settings, Reason.CombatConflict);
        if (!in.targetValid) return pause(in, settings, Reason.NoTarget);
        if (!in.inSearch) return pause(in, settings, Reason.OutOfRange);
        if (in.slowFalling) return pause(in, settings, Reason.SlowFalling);
        if (in.fallFlying) return pause(in, settings, Reason.FallFlying);
        if (in.inFluid) return pause(in, settings, Reason.InFluid);
        if (in.health < settings.minHealth) return pause(in, settings, Reason.LowHealth);
        if (!in.landingKnown) return pause(in, settings, Reason.UnknownLanding);
        if (in.fallDamage >= in.health - settings.healthReserve) return pause(in, settings, Reason.FatalFallRisk);
        if (in.maceSlot < 0 || in.maceSlot > 8) return pause(in, settings, Reason.NoMace);

        if (waitingLaunch) {
            if (!in.onGround) {
                airAfterLaunch = true;
                waitingLaunch = false;
            } else if (in.tick - launchTick > 20) {
                waitingLaunch = false;
                state = lastResult = State.Failed;
                reason = Reason.LaunchFailed;
                return restore(in.selectedSlot);
            } else {
                state = State.WaitingJump;
                reason = Reason.AwaitingLaunch;
                return Decision.NONE;
            }
        }
        if (in.onGround) {
            attempted = false;
            if (airAfterLaunch) {
                airAfterLaunch = false;
                if (settings.autoCombo && confirmedCombo) windUsed = false;
            }
            if (settings.windCharges && !windUsed) {
                if (!in.visible || !in.safeLaunch) return pause(in, settings, Reason.UnsafeLaunch);
                if (in.windSlot < 0 || in.windSlot > 8) return pause(in, settings, Reason.NoWindCharge);
                if (!in.windReady) return pause(in, settings, Reason.WindCoolingDown);
                if (in.selectedSlot != in.windSlot) return select(in, in.windSlot);
                if (ownedSlot >= 0 && in.tick - switchTick < Math.max(1, settings.syncTicks)) return Decision.NONE;
                windUsed = true;
                confirmedCombo = false;
                waitingLaunch = true;
                launchTick = in.tick;
                state = State.WaitingJump;
                reason = Reason.AwaitingLaunch;
                return new Decision(Action.WindCharge, -1);
            }
            state = State.WaitingJump;
            reason = Reason.NotFalling;
            return settings.switchBack ? restore(in.selectedSlot) : Decision.NONE;
        }
        airAfterLaunch = true;
        if (attempted) {
            state = lastResult == State.Confirmed ? State.Confirmed : State.WaitingFall;
            reason = lastResult == State.Confirmed ? Reason.ServerSmashConfirmed : Reason.AlreadyAttempted;
            return settings.switchBack ? restore(in.selectedSlot) : Decision.NONE;
        }
        if (in.velocityY >= -0.01) {
            state = State.WaitingFall;
            reason = Reason.NotFalling;
            return Decision.NONE;
        }
        if (!in.visible) return pause(in, settings, Reason.NotVisible);
        if (in.selectedSlot != in.maceSlot) {
            if (!settings.autoSwitch) return pause(in, settings, Reason.SwitchDisabled);
            return select(in, in.maceSlot);
        }
        if (ownedSlot >= 0 && in.tick - switchTick < Math.max(1, settings.syncTicks)) {
            state = State.Switching;
            reason = Reason.Synchronizing;
            return Decision.NONE;
        }
        state = State.Preparing;
        if (!in.vanillaSmash || in.fallDistance <= Math.max(1.5, settings.minFall) + settings.margin) {
            reason = Reason.FallTooShort;
            return Decision.NONE;
        }
        if (!in.inReach) { reason = Reason.OutOfRange; return Decision.NONE; }
        if (!in.predictedHit) { reason = Reason.UnsafeTiming; return Decision.NONE; }
        if (in.damageGain < settings.minGain) { reason = Reason.LowDamageGain; return Decision.NONE; }
        if (!in.attackReady) { reason = Reason.AttackCoolingDown; return Decision.NONE; }
        attempted = true;
        pendingTarget = in.targetId;
        attemptTick = in.tick;
        state = lastResult = State.Attempted;
        reason = Reason.AwaitingServer;
        return new Decision(Action.Attack, -1);
    }

    /** Only an own-attacker MACE_SMASH server damage packet confirms the special hit. */
    public void serverDamage(int tick, int targetId, boolean ownAttacker, boolean smash, int timeout) {
        if (!active || pendingTarget != targetId || !ownAttacker || tick - attemptTick > timeout) return;
        pendingTarget = -1;
        confirmedCombo = smash;
        state = lastResult = smash ? State.Confirmed : State.Failed;
        reason = smash ? Reason.ServerSmashConfirmed : Reason.OrdinaryHit;
    }

    public void attackCancelled() {
        pendingTarget = -1;
        confirmedCombo = false;
        state = lastResult = State.Failed;
        reason = Reason.UnsafeTiming;
    }

    public void windFailed() {
        waitingLaunch = false;
        state = lastResult = State.Failed;
        reason = Reason.LaunchFailed;
    }

    public static double smashBonus(double fall) {
        if (fall <= 1.5) return 0;
        return 4 * Math.min(fall, 3) + 2 * Math.min(Math.max(fall - 3, 0), 5) + Math.max(fall - 8, 0);
    }
}
