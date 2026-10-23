/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */
package meteordevelopment.meteorclient.mixin;

import com.mojang.renderpearl.frontend.FrontendCommandEncoder;
import com.mojang.renderpearl.backend.api.GpuDeviceBackend;
import com.mojang.renderpearl.api.commands.RenderPass;
import meteordevelopment.meteorclient.mixininterface.IGpuDevice;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FrontendCommandEncoder.class)
public abstract class FrontendCommandEncoderMixin {
    @Shadow @Final private GpuDeviceBackend device;

    @Inject(method = "createRenderPass(Lcom/mojang/renderpearl/api/commands/RenderPassDescriptor;)Lcom/mojang/renderpearl/api/commands/RenderPass;", at = @At("RETURN"))
    private void meteor$applyScissor(CallbackInfoReturnable<RenderPass> cir) {
        ((IGpuDevice) device).meteor$onCreateRenderPass(cir.getReturnValue());
    }
}
