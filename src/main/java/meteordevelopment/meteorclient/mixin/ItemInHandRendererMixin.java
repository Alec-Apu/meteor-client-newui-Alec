/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.vertex.PoseStack;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.ArmRenderEvent;
import meteordevelopment.meteorclient.events.render.HeldItemRendererEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.HandView;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

import static meteordevelopment.meteorclient.MeteorClient.mc;

@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class ItemInHandRendererMixin {
    @org.spongepowered.asm.mixin.injection.ModifyVariable(method = "submitArmWithItem", at = @At("HEAD"), argsOnly = true, ordinal = 2)
    private float modifySwing(float attackValue) {
        HandView module = Modules.get().get(HandView.class);
        InteractionHand hand = Objects.requireNonNullElse((mc.player.getCurrentSwing() == null ? null : mc.player.getCurrentSwing().hand()), InteractionHand.MAIN_HAND);

        if (module.isActive()) {
            if (module.swordSlash() && hand == InteractionHand.MAIN_HAND && mc.player.getMainHandItem().is(ItemTags.SWORDS)) {
                return 0f;
            }
            if (hand == InteractionHand.OFF_HAND && !mc.player.getOffhandItem().isEmpty()) {
                return attackValue + module.offSwing.get().floatValue();
            }
            if (hand == InteractionHand.MAIN_HAND && !mc.player.getMainHandItem().isEmpty()) {
                return attackValue + module.mainSwing.get().floatValue();
            }
        }

        return attackValue;
    }

    @Inject(method = "submitArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V", shift = At.Shift.BEFORE))
    private void onRenderItem(net.minecraft.client.renderer.state.level.PlayerRenderState player, net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState state, float frameInterp, float xRot, InteractionHand hand, float attack, ItemStack itemStack, float inverseArmHeight, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, CallbackInfo ci) {
        MeteorClient.EVENT_BUS.post(HeldItemRendererEvent.get(hand, poseStack));
    }

    @Inject(method = "submitArmWithItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/FirstPersonHandsAndItemsRenderer;renderPlayerArm(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IFFLnet/minecraft/world/entity/HumanoidArm;Lnet/minecraft/client/renderer/state/level/PlayerRenderState;)V"))
    private void onRenderArm(net.minecraft.client.renderer.state.level.PlayerRenderState player, net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState state, float frameInterp, float xRot, InteractionHand hand, float attack, ItemStack itemStack, float inverseArmHeight, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, CallbackInfo ci) {
        MeteorClient.EVENT_BUS.post(ArmRenderEvent.get(hand, poseStack));
    }

    @Inject(method = "applyEatTransform", at = @At(value = "INVOKE", target = "Ljava/lang/Math;pow(DD)D", shift = At.Shift.BEFORE), cancellable = true)
    private void cancelTransformations(PoseStack poseStack, float frameInterp, HumanoidArm arm, float remaining, int duration, CallbackInfo ci) {
        if (Modules.get().get(HandView.class).disableFoodAnimation()) ci.cancel();
    }
}
