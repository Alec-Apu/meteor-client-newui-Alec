/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */
package meteordevelopment.meteorclient.mixin;
import net.minecraft.client.player.ItemActivation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.util.RandomSource;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.render.NoRender;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemActivation.class)
public abstract class ItemActivationMixin {
    @Inject(method = "activate", at = @At("HEAD"), cancellable = true)
    private void meteor$noTotemAnimation(ItemStack item, RandomSource random, CallbackInfo ci) {
        if (item.is(Items.TOTEM_OF_UNDYING) && Modules.get().get(NoRender.class).noTotemAnimation()) ci.cancel();
    }
}
