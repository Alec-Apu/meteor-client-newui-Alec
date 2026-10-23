/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */
package meteordevelopment.meteorclient.systems.modules.combat;

import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixininterface.IMultiPlayerGameMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.automace.MaceAttackPolicy;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.utils.misc.Localization;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

import static meteordevelopment.meteorclient.systems.modules.combat.automace.MaceAttackPolicy.Status;

/** CC0 hotbar selector port with an independently implemented 26.2 combat adapter. */
public class AutoMace extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Boolean> targetPlayers = sgGeneral.add(new BoolSetting.Builder().name("target-players")
        .description("Allow attacking players, except friends and teammates.").defaultValue(true).build());
    private final Setting<Boolean> targetMobs = sgGeneral.add(new BoolSetting.Builder().name("target-mobs")
        .description("Allow attacking mobs, except owned animals.").defaultValue(false).build());
    private final Setting<List<String>> excludedNames = sgGeneral.add(new StringListSetting.Builder().name("excluded-names")
        .description("Never attack entities with these exact names.").defaultValue(List.of()).build());
    private final Setting<Double> targetRange = sgGeneral.add(new DoubleSetting.Builder().name("target-range")
        .description("Search distance. Equip the mace early enough to recover its attack cooldown.")
        .defaultValue(8).min(1).sliderRange(1, 16).build());
    private final Setting<Double> aimRange = sgGeneral.add(new DoubleSetting.Builder().name("aim-range")
        .description("Maximum horizontal field of view in degrees.").defaultValue(360).range(1, 360).sliderRange(1, 360).build());
    private final Setting<Double> attackRange = sgGeneral.add(new DoubleSetting.Builder().name("attack-range")
        .description("Maximum eye-to-hitbox distance, also limited by vanilla reach.")
        .defaultValue(3).min(0.1).sliderRange(0.1, 3).build());
    private final Setting<SortPriority> priority = sgGeneral.add(new EnumSetting.Builder<SortPriority>().name("target-priority")
        .description("How valid targets are sorted.").defaultValue(SortPriority.LowestDistance).build());
    private final Setting<Double> minFall = sgGeneral.add(new DoubleSetting.Builder().name("min-fall-distance")
        .description("Minimum fall distance. Vanilla smash attacks require strictly more than 1.5 blocks.")
        .defaultValue(1.5).min(1.5).sliderRange(1.5, 10).build());
    private final Setting<Double> safetyMargin = sgGeneral.add(new DoubleSetting.Builder().name("safety-margin")
        .description("Additional fall distance required before an attack attempt.")
        .defaultValue(0.1).min(0).sliderRange(0, 1).build());
    private final Setting<Double> healthThreshold = sgGeneral.add(new DoubleSetting.Builder().name("health-threshold")
        .description("Do not attempt attacks below this health. This module cannot prevent fall damage.")
        .defaultValue(6).min(0).sliderRange(0, 20).build());
    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder().name("auto-switch")
        .description("Equip a hotbar mace while a target is nearby, before falling.").defaultValue(true).build());
    private final Setting<Boolean> switchBack = sgGeneral.add(new BoolSetting.Builder().name("switch-back")
        .description("Restore the original hotbar selection after an attempt or cancellation, unless changed manually.")
        .defaultValue(true).build());
    private final Setting<Boolean> debug = sgGeneral.add(new BoolSetting.Builder().name("debug-info")
        .description("Show the target, falling state, mace slot and attack status on screen.").defaultValue(false).build());

    private final MaceAttackPolicy policy = new MaceAttackPolicy();
    private final MaceAttackPolicy.SlotLease slots = new MaceAttackPolicy.SlotLease();
    private LivingEntity target;
    private Status status = Status.Disabled;
    private int ticks, switchedTick, attemptTick, attemptedId = -1, generation;
    private boolean pending, manualOverride;
    private int maceSlot = -1;

    public AutoMace() {
        super(Categories.Combat, "auto-mace", "Automatically attempts vanilla mace smash attacks while falling. Does not generate jumps or prevent fall damage.");
    }

    @Override public void onActivate() {
        generation++;
        policy.activate();
        slots.clear();
        target = null;
        pending = manualOverride = false;
        attemptedId = -1;
        ticks = 0;
        switchedTick = -100;
        status = Status.NoTarget;
    }

    @Override public void onDeactivate() {
        generation++;
        policy.deactivate();
        restoreSlot();
        target = null;
        pending = manualOverride = false;
        attemptedId = -1;
        status = Status.Disabled;
    }

    private void restoreSlot() {
        if (mc.player == null || mc.gameMode == null) { slots.clear(); return; }
        int slot = slots.restore(mc.player.getInventory().getSelectedSlot(), switchBack.get(), mc.player.getMainHandItem().is(Items.MACE));
        if (slot != -1) select(slot);
    }

    private void select(int slot) {
        mc.player.getInventory().setSelectedSlot(slot);
        ((IMultiPlayerGameMode) mc.gameMode).meteor$syncSelected();
    }

    private boolean validTarget(Entity entity) {
        if (!(entity instanceof LivingEntity living) || entity == mc.player || entity instanceof FakePlayerEntity
            || entity.isRemoved() || !living.isAlive() || living.isDeadOrDying() || !entity.isPickable()) return false;
        if (mc.player.distanceTo(entity) > targetRange.get() || mc.player.isAlliedTo(entity)
            || excludedNames.get().contains(entity.getName().getString())) return false;
        if (entity instanceof Player player) {
            if (!targetPlayers.get() || player.isCreative() || player.isSpectator() || !Friends.get().shouldAttack(player)) return false;
        } else if (!(entity instanceof Mob) || !targetMobs.get()
            || entity instanceof OwnableEntity pet && pet.getOwnerReference() != null) return false;
        double angle = Math.abs(Mth.wrapDegrees(Rotations.getYaw(entity) - mc.player.getYRot()));
        return angle <= aimRange.get() / 2;
    }

    private static Vec3 closest(Vec3 point, AABB box) {
        return new Vec3(Mth.clamp(point.x, box.minX + 0.001, box.maxX - 0.001),
            Mth.clamp(point.y, box.minY + 0.001, box.maxY - 0.001),
            Mth.clamp(point.z, box.minZ + 0.001, box.maxZ - 0.001));
    }

    private boolean visible(Vec3 eye, Vec3 aim) {
        return mc.level.clip(new ClipContext(eye, aim, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player)).getType() == HitResult.Type.MISS;
    }

    @EventHandler private void onTick(TickEvent.Post event) {
        if (!isActive() || mc.player == null || mc.level == null || mc.gameMode == null) return;
        ticks++;
        if (mc.player.onGround()) policy.landed();
        if (slots.interrupted(mc.player.getInventory().getSelectedSlot())) {
            slots.clear();
            manualOverride = true;
        }
        // No attacks or item switching alongside competing combat / rotation operations.
        boolean conflict = Modules.get().isActive(KillAura.class) || Modules.get().isActive(AutoWeapon.class)
            || Modules.get().isActive(CrystalAura.class) || Rotations.rotating || mc.gui.screen() != null
            || mc.player.isUsingItem() || mc.gameMode.isDestroying();
        if (conflict || !mc.player.isAlive() || mc.player.isSpectator()) {
            restoreSlot();
            status = Status.Conflict;
            target = null;
            return;
        }
        if (pending) {
            if (mc.level.getEntity(attemptedId) == null || ticks - attemptTick >= 12) {
                pending = false;
                status = Status.Unconfirmed;
                restoreSlot();
            }
            return;
        }
        // Keep a confirmation/failure visible and avoid re-equipping after a completed fall.
        if (!mc.player.onGround() && (status == Status.Confirmed || status == Status.Unconfirmed || status == Status.Attempted)) return;
        target = (LivingEntity) TargetUtils.get(this::validTarget, priority.get());
        if (target == null) {
            restoreSlot();
            manualOverride = false;
            status = Status.NoTarget;
            return;
        }
        maceSlot = MaceAttackPolicy.findMace(i -> mc.player.getInventory().getItem(i).is(Items.MACE));
        if (maceSlot == -1) { restoreSlot(); status = Status.NoMace; return; }
        if (manualOverride) { status = Status.ManualOverride; return; }
        if (!mc.player.getMainHandItem().is(Items.MACE) && autoSwitch.get()) {
            slots.acquire(mc.player.getInventory().getSelectedSlot(), maceSlot);
            select(maceSlot);
            switchedTick = ticks;
            status = Status.WaitingForCooldown;
            return;
        }
        Vec3 eye = mc.player.getEyePosition();
        Vec3 aim = closest(eye, target.getBoundingBox());
        Vec3 nextEye = eye.add(mc.player.getDeltaMovement());
        AABB nextBox = target.getBoundingBox().move(target.getDeltaMovement());
        boolean reach = eye.distanceTo(aim) <= attackRange.get()
            && mc.player.isWithinAttackRange(mc.player.getMainHandItem(), target.getBoundingBox(), 0)
            && nextEye.distanceTo(closest(nextEye, nextBox)) <= Math.min(attackRange.get(), mc.player.entityInteractionRange());
        var conditions = new MaceAttackPolicy.Conditions(isActive(), false, validTarget(target),
            mc.player.getMainHandItem().is(Items.MACE), !mc.player.onGround() && mc.player.getDeltaMovement().y < -0.01,
            mc.player.isFallFlying() || mc.player.hasEffect(MobEffects.SLOW_FALLING) || mc.player.isInWater() || mc.player.isInLava()
                || mc.player.onClimbable() || mc.player.isPassenger(),
            mc.player.fallDistance, minFall.get() + safetyMargin.get(), reach, visible(eye, aim),
            mc.player.getHealth() >= healthThreshold.get(), ticks > switchedTick + 1 && mc.player.getAttackStrengthScale(0) >= 1);
        status = policy.evaluate(conditions);
        if (!MaceItem.canSmashAttack(mc.player) || !policy.claimAttack(conditions)) return;
        // Synchronous aim/attack/restore leaves no queued rotation callback after disable.
        Vec3 delta = aim.subtract(eye);
        float yaw = (float) Math.toDegrees(Math.atan2(delta.z, delta.x)) - 90;
        float pitch = (float) -Math.toDegrees(Math.atan2(delta.y, Math.sqrt(delta.x * delta.x + delta.z * delta.z)));
        attemptedId = target.getId();
        attemptTick = ticks;
        pending = true;
        status = Status.Attempted;
        try {
            mc.player.connection.send(new ServerboundMovePlayerPacket.Rot(yaw, pitch, mc.player.onGround(), mc.player.horizontalCollision));
            mc.gameMode.attack(mc.player, target);
            mc.player.swing(InteractionHand.MAIN_HAND);
        } finally {
            mc.player.connection.send(new ServerboundMovePlayerPacket.Rot(mc.player.getYRot(), mc.player.getXRot(), mc.player.onGround(), mc.player.horizontalCollision));
        }
    }

    @EventHandler private void onPacket(PacketEvent.Receive event) {
        if (!(event.packet instanceof ClientboundDamageEventPacket packet)) return;
        int token = generation;
        var level = mc.level;
        mc.execute(() -> {
            if (!isActive() || token != generation || mc.level != level || !pending || mc.player == null) return;
            if (packet.entityId() == attemptedId && packet.sourceType().is(DamageTypes.MACE_SMASH)
                && (packet.sourceCauseId() == mc.player.getId() || packet.sourceDirectId() == mc.player.getId())) {
                pending = false;
                status = Status.Confirmed;
                restoreSlot();
            }
        });
    }

    @Override public String getInfoString() { return Localization.text(status.name()); }

    @EventHandler private void onRender(Render2DEvent event) {
        if (!debug.get() || mc.player == null) return;
        String[] lines = {
            Localization.text("Auto Mace") + ": " + getInfoString(),
            Localization.text("Target") + ": " + (target == null ? "-" : target.getName().getString()),
            Localization.text("Falling") + ": " + (!mc.player.onGround() && mc.player.getDeltaMovement().y < -0.01)
                + " / " + String.format(java.util.Locale.ROOT, "%.2f", mc.player.fallDistance),
            Localization.text("Mace slot") + ": " + (maceSlot + 1) + " / " + mc.player.getMainHandItem().getHoverName().getString(),
            status == Status.NoMace && InvUtils.find(stack -> stack.is(Items.MACE), 9, 35).found()
                ? Localization.text("Move the backpack mace to your hotbar.") : ""
        };
        for (int i = 0; i < lines.length; i++) event.graphics.text(mc.font, lines[i], 6, 6 + 12 * i, 0xFFFFFFFF, true);
    }
}
