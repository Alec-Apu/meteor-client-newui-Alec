/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */
package meteordevelopment.meteorclient.mixin;

import com.mojang.renderpearl.backend.opengl.GlCommandEncoder;
import com.mojang.renderpearl.backend.opengl.GlRenderPipeline;
import meteordevelopment.meteorclient.renderer.MeteorRenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import static org.lwjgl.opengl.GL11C.*;

@Mixin(GlCommandEncoder.class)
public abstract class GlCommandEncoderMixin {
    @Shadow private GlRenderPipeline lastPipeline;

    @Inject(method = "setupDraw", at = @At("RETURN"))
    private void meteor$lineSmooth(CallbackInfo ci) {
        if (MeteorRenderPipelines.lineSmooth(lastPipeline)) {
            glEnable(GL_LINE_SMOOTH);
            glLineWidth(1);
        } else {
            glDisable(GL_LINE_SMOOTH);
        }
    }
}
