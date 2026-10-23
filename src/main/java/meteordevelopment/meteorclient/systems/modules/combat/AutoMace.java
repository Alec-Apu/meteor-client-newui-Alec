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
import meteordevelopment.meteorclient.systems.modules.player.AutoClicker;
import meteordevelopment.meteorclient.systems.modules.combat.mace.AutoMaceController;
import meteordevelopment.meteorclient.utils.entity.SortPriority;
import meteordevelopment.meteorclient.utils.entity.TargetUtils;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.utils.misc.Localization;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.world.TickRate;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

import java.util.List;
import java.util.Set;

/** Does not move the player, disable protection modules, spoof fall distance or press attack/jump keys. */
public class AutoMace extends Module {
    private final SettingGroup sgTargets = settings.createGroup("Targeting");
    private final SettingGroup sgAttack = settings.getDefaultGroup();
    private final SettingGroup sgSafety = settings.createGroup("Safety");
    private final SettingGroup sgAssist = settings.createGroup("Assistance");

    private final Setting<Boolean> players = sgTargets.add(new BoolSetting.Builder().name("target-players").description("Target survival players.").defaultValue(true).build());
    private final Setting<Boolean> mobs = sgTargets.add(new BoolSetting.Builder().name("target-mobs").description("Target attackable mobs. Tamed mobs are excluded.").defaultValue(false).build());
    private final Setting<Double> targetRange = sgTargets.add(new DoubleSetting.Builder().name("target-range").description("Maximum distance from your eyes to a target's hitbox when searching.").defaultValue(6).range(1, 12).sliderMax(12).build());
    private final Setting<Double> aimRange = sgTargets.add(new DoubleSetting.Builder().name("aim-range").description("Full aiming field of view in degrees.").defaultValue(180).range(10, 360).sliderMax(360).build());
    private final Setting<SortPriority> priority = sgTargets.add(new EnumSetting.Builder<SortPriority>().name("target-priority").description("Reuse the client's target ordering, including distance and health.").defaultValue(SortPriority.LowestDistance).build());
    private final Setting<Boolean> friends = sgTargets.add(new BoolSetting.Builder().name("ignore-friends").description("Exclude players in the client's friends list.").defaultValue(true).build());
    private final Setting<Boolean> teams = sgTargets.add(new BoolSetting.Builder().name("ignore-teammates").description("Exclude players on your scoreboard team.").defaultValue(true).build());
    private final Setting<List<String>> excludedNames = sgTargets.add(new StringListSetting.Builder().name("excluded-names").description("Do not target these exact player or entity names.").build());
    private final Setting<Set<EntityType<?>>> excludedTypes = sgTargets.add(new EntityTypeListSetting.Builder().name("excluded-entities").description("Entity types never selected as targets.").onlyAttackable().build());

    private final Setting<Double> attackRange = sgAttack.add(new DoubleSetting.Builder().name("max-attack-distance").description("Attack distance limit, additionally capped by the game's actual interaction and weapon ranges.").defaultValue(3).range(1, 6).sliderMax(6).build());
    private final Setting<Double> minFall = sgAttack.add(new DoubleSetting.Builder().name("min-fall-distance").description("Minimum accumulated fall distance. Vanilla requires strictly more than 1.5 blocks.").defaultValue(1.5).range(1.5, 20).sliderMax(10).build());
    private final Setting<Double> margin = sgAttack.add(new DoubleSetting.Builder().name("safety-margin").description("Extra fall-distance margin above the minimum, in blocks.").defaultValue(0.15).range(0, 2).sliderMax(1).build());
    private final Setting<Boolean> autoSwitch = sgAttack.add(new BoolSetting.Builder().name("auto-switch").description("Select a hotbar mace before attacking.").defaultValue(true).build());
    private final Setting<Boolean> switchBack = sgAttack.add(new BoolSetting.Builder().name("switch-back").description("Restore your owned original slot after the attempt. Disabling always restores it unless you changed slots.").defaultValue(true).build());
    private final Setting<Integer> syncTicks = sgAttack.add(new IntSetting.Builder().name("switch-delay").description("Full ticks to wait after synchronizing a hotbar selection. This is not a server acknowledgement.").defaultValue(2).range(1, 10).sliderMax(10).build());
    private final Setting<Integer> confirmationTicks = sgAttack.add(new IntSetting.Builder().name("confirmation-timeout").description("Ticks to wait for an own-attacker server mace-smash damage packet.").defaultValue(12).range(4, 40).sliderMax(40).build());

    private final Setting<Double> minGain = sgSafety.add(new DoubleSetting.Builder().name("min-damage-gain").description("Minimum estimated unenchanted smash bonus before armor and server modifiers.").defaultValue(4).range(0, 40).sliderMax(40).build());
    private final Setting<Double> minHealth = sgSafety.add(new DoubleSetting.Builder().name("health-threshold").description("Minimum health plus absorption, in health points, before acting.").defaultValue(8).range(1, 40).sliderMax(40).build());
    private final Setting<Double> healthReserve = sgSafety.add(new DoubleSetting.Builder().name("health-reserve").description("Pause if conservative projected fall damage would leave fewer than these health points.").defaultValue(2).range(1, 10).sliderMax(10).build());

    private final Setting<Boolean> wind = sgAssist.add(new BoolSetting.Builder().name("use-wind-charges").description("Optional one-shot ground launch using a hotbar wind charge. No automatic retries on failure.").defaultValue(false).build());
    private final Setting<Boolean> combo = sgAssist.add(new BoolSetting.Builder().name("auto-combo").description("Allow another wind launch after landing only following a server-confirmed mace smash.").defaultValue(false).visible(wind::get).build());
    private final Setting<Boolean> debug = sgAssist.add(new BoolSetting.Builder().name("debug-info").description("Show target, fall, slots, state and pause reason on screen. Does not spam chat or logs.").defaultValue(false).build());

    private final AutoMaceController controller = new AutoMaceController();
    private LivingEntity target;
    private int ticks, generation, maceSlot = -1;
    private boolean rotationOwned;
    private AutoMaceController.Input lastInput;

    public AutoMace() {
        super(Categories.Combat, "auto-mace", "Conservatively attempts falling mace attacks and waits for server smash confirmation.");
    }

    @Override
    public void onActivate() {
        generation++;
        ticks = 0;
        target = null;
        lastInput = null;
        rotationOwned = false;
        controller.activate();
    }

    @Override
    public void onDeactivate() {
        generation++;
        if (mc.player != null) apply(controller.deactivate(mc.player.getInventory().getSelectedSlot()));
        else controller.deactivate(-1);
        restoreRotation();
        target = null;
        lastInput = null;
    }

    private AutoMaceController.Settings options() {
        return new AutoMaceController.Settings(minFall.get(), margin.get(), minGain.get(), minHealth.get(), healthReserve.get(), syncTicks.get(), confirmationTicks.get(), autoSwitch.get(), switchBack.get(), wind.get(), combo.get());
    }

    private boolean targetValid(Entity entity) {
        if (!(entity instanceof LivingEntity living) || entity == mc.player || entity == mc.getCameraEntity()
            || !living.isAlive() || living.isDeadOrDying() || living.isRemoved() || !living.isAttackable()
            || excludedTypes.get().contains(entity.getType())) return false;
        if (excludedNames.get().stream().anyMatch(name -> name.equalsIgnoreCase(entity.getName().getString()))) return false;
        if (entity instanceof Player player) {
            if (!players.get() || player.isCreative() || player.isSpectator()) return false;
            if (friends.get() && !Friends.get().shouldAttack(player)) return false;
            if (teams.get() && mc.player.isAlliedTo(player)) return false;
            if (player instanceof FakePlayerEntity fake && fake.noHit) return false;
        } else if (!mobs.get() || !(entity instanceof Mob) || entity instanceof OwnableEntity ownable && ownable.getOwnerReference() != null) return false;
        Vec3 eye = mc.player.getEyePosition();
        Vec3 point = aimPoint(eye, entity.getBoundingBox());
        if (eye.distanceTo(point) > targetRange.get()) return false;
        Vec3 direction = point.subtract(eye).normalize();
        double angle = Math.toDegrees(Math.acos(Math.clamp(mc.player.getLookAngle().dot(direction), -1, 1)));
        return angle <= aimRange.get() / 2;
    }

    private static Vec3 aimPoint(Vec3 eye, AABB box) {
        Vec3 closest = new Vec3(Math.clamp(eye.x, box.minX, box.maxX), Math.clamp(eye.y, box.minY, box.maxY), Math.clamp(eye.z, box.minZ, box.maxZ));
        return closest.lerp(box.getCenter(), 0.01);
    }

    private boolean clearRay(Vec3 eye, Vec3 aim) {
        HitResult block = mc.level.clip(new ClipContext(eye, aim, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.player));
        if (block.getType() != HitResult.Type.MISS) return false;
        for (Entity other : mc.level.getEntities(mc.player, new AABB(eye, aim).inflate(0.1), Entity::isPickable)) {
            if (other != target && other.getBoundingBox().clip(eye, aim).isPresent()) return false;
        }
        return true;
    }

    private double landingDistance(Vec3 motion) {
        AABB box = mc.player.getBoundingBox().move(motion.x, 0, motion.z);
        double best = Double.POSITIVE_INFINITY;
        for (double x : new double[]{box.minX + 0.02, box.maxX - 0.02}) {
            for (double z : new double[]{box.minZ + 0.02, box.maxZ - 0.02}) {
                Vec3 from = new Vec3(x, box.minY + 0.05, z);
                HitResult hit = mc.level.clip(new ClipContext(from, from.add(0, -48, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, mc.player));
                if (hit.getType() != HitResult.Type.MISS) best = Math.min(best, Math.max(0, box.minY - hit.getLocation().y));
            }
        }
        return best;
    }

    private boolean conflict() {
        Modules modules = Modules.get();
        return modules.isActive(KillAura.class) || modules.isActive(AutoWeapon.class) || modules.isActive(CrystalAura.class)
            || modules.isActive(BedAura.class) || modules.isActive(AnchorAura.class)
            || modules.isActive(Criticals.class) || modules.isActive(AutoClicker.class)
            || Rotations.rotating || mc.options.keyAttack.isDown()
            || modules.isActive(meteordevelopment.meteorclient.systems.modules.movement.NoFall.class)
            || TickRate.INSTANCE.getTimeSinceLastTick() >= 1;
    }

    private AutoMaceController.Input snapshot() {
        maceSlot = InvUtils.find(stack -> stack.is(Items.MACE), 0, 8).slot();
        int windSlot = InvUtils.find(stack -> stack.is(Items.WIND_CHARGE), 0, 8).slot();
        boolean valid = target != null && targetValid(target) && mc.level.getEntity(target.getId()) == target;
        Vec3 eye = mc.player.getEyePosition();
        Vec3 motion = mc.player.getDeltaMovement();
        Vec3 aim = valid ? aimPoint(eye, target.getBoundingBox()) : eye;
        double range = Math.min(attackRange.get(), mc.player.entityInteractionRange());
        boolean reach = valid && eye.distanceTo(aim) <= range && maceSlot >= 0
            && mc.player.isWithinAttackRange(mc.player.getInventory().getItem(maceSlot), target.getBoundingBox(), 0);
        boolean visible = valid && mc.player.hasLineOfSight(target) && clearRay(eye, aim);
        Vec3 nextEye = eye.add(motion);
        AABB nextBox = valid ? target.getBoundingBox().move(target.getDeltaMovement()) : mc.player.getBoundingBox();
        Vec3 nextAim = aimPoint(nextEye, nextBox);
        boolean predicted = reach && visible && nextEye.distanceTo(nextAim) <= range && clearRay(nextEye, nextAim)
            && mc.level.noCollision(mc.player, mc.player.getBoundingBox().expandTowards(motion));
        double remaining = landingDistance(motion.scale(2));
        boolean groundKnown = Double.isFinite(remaining);
        double projectedDamage = groundKnown ? Math.ceil(Math.max(0, mc.player.fallDistance + remaining - mc.player.getAttributeValue(Attributes.SAFE_FALL_DISTANCE)) * mc.player.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER)) : Double.POSITIVE_INFINITY;
        boolean safeLaunch = valid && mc.player.onGround() && groundKnown && remaining <= 0.25
            && aim.subtract(eye).multiply(1, 0, 1).lengthSqr() <= range * range
            && mc.level.noCollision(mc.player, mc.player.getBoundingBox().expandTowards(0, 3, 0));
        return new AutoMaceController.Input(isActive(), ticks, valid ? target.getId() : -1, valid, valid, reach, visible, predicted,
            MaceItem.canSmashAttack(mc.player), mc.player.onGround(), motion.y, mc.player.fallDistance,
            mc.player.hasEffect(MobEffects.SLOW_FALLING), mc.player.isFallFlying(), mc.player.isInWater() || mc.player.isInLava(),
            mc.gui.screen() != null, mc.player.isUsingItem(), conflict(), mc.player.getHealth() + mc.player.getAbsorptionAmount(),
            projectedDamage, groundKnown, AutoMaceController.smashBonus(mc.player.fallDistance), mc.player.getInventory().getSelectedSlot(), maceSlot,
            mc.player.getAttackStrengthScale(0) >= 1, windSlot, windSlot >= 0 && !mc.player.getCooldowns().isOnCooldown(mc.player.getInventory().getItem(windSlot)), safeLaunch);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        ticks++;
        restoreRotation();
        if (controller.pendingTarget() >= 0) {
            Entity pending = mc.level.getEntity(controller.pendingTarget());
            target = pending instanceof LivingEntity living ? living : null;
        } else {
            Entity chosen = TargetUtils.get(this::targetValid, priority.get());
            target = chosen instanceof LivingEntity living ? living : null;
        }
        lastInput = snapshot();
        apply(controller.tick(lastInput, options()));
    }

    private void select(int slot) {
        if (slot < 0 || slot > 8 || mc.player == null || mc.gameMode == null) return;
        mc.player.getInventory().setSelectedSlot(slot);
        ((IMultiPlayerGameMode) mc.gameMode).meteor$syncSelected();
    }

    private void look(float yaw, float pitch) {
        // Synchronous ordinary look packet: no queued callback or persistent client camera rotation.
        mc.player.connection.send(new ServerboundMovePlayerPacket.Rot(yaw, pitch, mc.player.onGround(), mc.player.horizontalCollision));
        rotationOwned = true;
    }

    private void restoreRotation() {
        if (rotationOwned && mc.player != null && !Rotations.rotating) {
            mc.player.connection.send(new ServerboundMovePlayerPacket.Rot(mc.player.getYRot(), mc.player.getXRot(), mc.player.onGround(), mc.player.horizontalCollision));
        }
        rotationOwned = false;
    }

    private void apply(AutoMaceController.Decision decision) {
        switch (decision.action()) {
            case Select, Restore -> select(decision.slot());
            case Attack -> {
                // Revalidate immediately at dispatch; a decision is not a permission to attack stale state.
                AutoMaceController.Input now = snapshot();
                if (!isActive() || !now.targetValid() || !now.inReach() || !now.visible() || !now.predictedHit()
                    || !now.vanillaSmash() || now.slowFalling() || now.fallFlying() || now.combatConflict()
                    || now.inFluid() || now.screenOpen() || now.usingItem() || now.onGround() || now.velocityY() >= -0.01
                    || !now.landingKnown() || now.health() < minHealth.get() || now.fallDamage() >= now.health() - healthReserve.get()
                    || now.fallDistance() <= Math.max(1.5, minFall.get()) + margin.get() || now.damageGain() < minGain.get()
                    || !mc.player.getMainHandItem().is(Items.MACE) || !now.attackReady()) {
                    controller.attackCancelled();
                    return;
                }
                Vec3 aim = aimPoint(mc.player.getEyePosition(), target.getBoundingBox());
                look((float) Rotations.getYaw(aim), (float) Rotations.getPitch(aim));
                mc.gameMode.attack(mc.player, target);
                mc.player.swing(InteractionHand.MAIN_HAND);
            }
            case WindCharge -> {
                if (!isActive() || !mc.player.getMainHandItem().is(Items.WIND_CHARGE) || !snapshot().safeLaunch()) {
                    controller.windFailed();
                    return;
                }
                look(mc.player.getYRot(), 90);
                float pitch = mc.player.getXRot();
                try {
                    // The use-item packet carries its own angles in 26.2.
                    mc.player.setXRot(90);
                    if (!mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND).consumesAction()) controller.windFailed();
                    else mc.player.swing(InteractionHand.MAIN_HAND);
                } finally {
                    mc.player.setXRot(pitch);
                }
            }
            case None -> {}
        }
    }

    @EventHandler
    private void onPacket(PacketEvent.Receive event) {
        if (!(event.packet instanceof ClientboundDamageEventPacket packet)) return;
        int token = generation;
        mc.execute(() -> {
            if (!isActive() || generation != token || mc.player == null || mc.level == null) return;
            controller.serverDamage(ticks, packet.entityId(), packet.sourceCauseId() == mc.player.getId(),
                packet.sourceType().is(DamageTypes.MACE_SMASH), confirmationTicks.get());
        });
    }

    @EventHandler
    private void onRender(Render2DEvent event) {
        if (!debug.get() || mc.player == null || lastInput == null) return;
        String[] lines = {
            Localization.text("Auto Mace") + ": " + (target == null ? "-" : target.getName().getString()),
            Localization.text("Falling") + ": " + (lastInput.velocityY() < -0.01) + " | " + Localization.text("Fall Distance") + ": " + String.format(java.util.Locale.ROOT, "%.2f", lastInput.fallDistance()),
            Localization.text("Held Item") + ": " + mc.player.getMainHandItem().getHoverName().getString() + " | " + Localization.text("Mace Slot") + ": " + (maceSlot < 0 ? "-" : maceSlot + 1),
            Localization.text("Attack State") + ": " + Localization.text(controller.state().name()) + " | " + Localization.text(controller.lastResult().name()),
            Localization.text("Reason") + ": " + Localization.text(controller.reason().name())
        };
        for (int i = 0; i < lines.length; i++) event.graphics.text(mc.font, lines[i], 6, 6 + i * 12, 0xFFFFFFFF, true);
    }

    @Override
    public String getInfoString() { return Localization.text(controller.state().name()); }
}
