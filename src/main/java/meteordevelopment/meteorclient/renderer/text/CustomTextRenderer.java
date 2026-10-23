/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer.text;

import meteordevelopment.meteorclient.renderer.MeshBuilder;
import meteordevelopment.meteorclient.renderer.MeshRenderer;
import meteordevelopment.meteorclient.renderer.MeteorRenderPipelines;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.io.IOException;
import java.nio.ByteBuffer;
import org.jspecify.annotations.Nullable;

public class CustomTextRenderer implements TextRenderer {
    public static final Color SHADOW_COLOR = new Color(60, 60, 60, 180);

    private final MeshBuilder mesh = new MeshBuilder(MeteorRenderPipelines.UI_TEXT);
    private final MeshBuilder fallbackMesh = new MeshBuilder(MeteorRenderPipelines.UI_TEXT);

    public final FontFace fontFace;

    private final Font[] fonts;
    private final DynamicFont @Nullable [] fallbackFonts;
    private Font font;
    private @Nullable DynamicFont fallbackFont;

    private boolean building;
    private boolean scaleOnly;
    private boolean vanillaOwned;
    private GuiGraphicsExtractor graphics;
    private double beginScale;
    private boolean beginBig;
    private double fontScale = 1;
    private double scale = 1;

    public CustomTextRenderer(FontFace fontFace) throws IOException {
        this(fontFace, null);
    }

    public CustomTextRenderer(FontFace fontFace, @Nullable FontFace fallbackFace) throws IOException {
        this.fontFace = fontFace;

        ByteBuffer buffer = fontFace.readToDirectByteBuffer();

        fonts = new Font[5];
        for (int i = 0; i < fonts.length; i++) {
            fonts[i] = new Font(buffer, (int) Math.round(27 * ((i * 0.5) + 1)));
        }

        if (fallbackFace != null) {
            ByteBuffer fallbackBuffer = fallbackFace.readToDirectByteBuffer();
            fallbackFonts = new DynamicFont[fonts.length];
            for (int i = 0; i < fallbackFonts.length; i++) {
                fallbackFonts[i] = new DynamicFont(fallbackBuffer, (int) Math.round(27 * ((i * 0.5) + 1)));
            }
        } else {
            fallbackFonts = null;
        }
    }

    @Override
    public void setAlpha(double a) {
        mesh.alpha = a;
        fallbackMesh.alpha = a;
        VanillaTextRenderer.INSTANCE.setAlpha(a);
    }

    @Override
    public void begin(GuiGraphicsExtractor graphics, double scale, boolean scaleOnly, boolean big) {
        if (building) throw new RuntimeException("CustomTextRenderer.begin() called twice");

        if (!scaleOnly) {
            mesh.begin();
            fallbackMesh.begin();
        }

        if (big) {
            this.font = fonts[fonts.length - 1];
            this.fallbackFont = fallbackFonts != null ? fallbackFonts[fallbackFonts.length - 1] : null;
        } else {
            double scaleA = Math.floor(scale * 10) / 10;

            int scaleI;
            if (scaleA >= 3) scaleI = 5;
            else if (scaleA >= 2.5) scaleI = 4;
            else if (scaleA >= 2) scaleI = 3;
            else if (scaleA >= 1.5) scaleI = 2;
            else scaleI = 1;

            font = fonts[scaleI - 1];
            fallbackFont = fallbackFonts != null ? fallbackFonts[scaleI - 1] : null;
        }

        this.building = true;
        this.scaleOnly = scaleOnly;
        this.graphics = graphics;
        this.beginScale = scale;
        this.beginBig = big;
        this.vanillaOwned = false;

        this.fontScale = font.getHeight() / 27.0;
        this.scale = 1 + (scale - fontScale) / fontScale;
    }

    @Override
    public double getWidth(String text, int length, boolean shadow) {
        if (text.isEmpty()) return 0;

        Font font = building ? this.font : fonts[0];
        if (font.supports(text, length)) {
            return (font.getWidth(text, length) + (shadow ? 1 : 0)) * scale / 1.5;
        }

        DynamicFont fallback = building ? fallbackFont : fallbackFonts != null ? fallbackFonts[0] : null;
        if (fallback != null && fallback.prepare(text, length)) {
            return (fallback.getWidth(text, length) + (shadow ? 1 : 0)) * scale / 1.5;
        }

        if (building) ensureVanillaFallback();
        return VanillaTextRenderer.INSTANCE.getWidth(text, length, shadow);
    }

    @Override
    public double getHeight(boolean shadow) {
        Font font = building ? this.font : fonts[0];
        return (font.getHeight() + 1 + (shadow ? 1 : 0)) * scale / 1.5;
    }

    @Override
    public double render(String text, double x, double y, Color color, boolean shadow) {
        if (!building) throw new RuntimeException("CustomTextRenderer.render() called without calling begin()");

        if (!font.supports(text, text.length())) {
            if (fallbackFont != null && fallbackFont.prepare(text, text.length())) {
                return renderFallback(text, x, y, color, shadow);
            }

            ensureVanillaFallback();
            return VanillaTextRenderer.INSTANCE.render(text, x, y, color, shadow);
        }

        double width;
        if (shadow) {
            int preShadowA = SHADOW_COLOR.a;
            SHADOW_COLOR.a = (int) (color.a / 255.0 * preShadowA);

            width = font.render(mesh, text, x + fontScale * scale / 1.5, y + fontScale * scale / 1.5, SHADOW_COLOR, scale / 1.5);
            font.render(mesh, text, x, y, color, scale / 1.5);

            SHADOW_COLOR.a = preShadowA;
        } else {
            width = font.render(mesh, text, x, y, color, scale / 1.5);
        }

        return width;
    }

    private void ensureVanillaFallback() {
        if (VanillaTextRenderer.INSTANCE.isBuilding()) return;

        VanillaTextRenderer.INSTANCE.begin(graphics, beginScale, scaleOnly, beginBig);
        vanillaOwned = true;
    }

    private double renderFallback(String text, double x, double y, Color color, boolean shadow) {
        double width;
        if (shadow) {
            int preShadowA = SHADOW_COLOR.a;
            SHADOW_COLOR.a = (int) (color.a / 255.0 * preShadowA);

            width = fallbackFont.render(fallbackMesh, text, x + fontScale * scale / 1.5,
                y + fontScale * scale / 1.5, SHADOW_COLOR, scale / 1.5);
            fallbackFont.render(fallbackMesh, text, x, y, color, scale / 1.5);

            SHADOW_COLOR.a = preShadowA;
        } else {
            width = fallbackFont.render(fallbackMesh, text, x, y, color, scale / 1.5);
        }

        return width;
    }

    @Override
    public boolean isBuilding() {
        return building;
    }

    @Override
    public void end() {
        if (!building) throw new RuntimeException("CustomTextRenderer.end() called without calling begin()");

        if (!scaleOnly) {
            mesh.end();
            fallbackMesh.end();

            MeshRenderer.begin()
                .attachments(Minecraft.getInstance().gameRenderer.mainRenderTarget())
                .pipeline(MeteorRenderPipelines.UI_TEXT)
                .mesh(mesh)
                .sampler("u_Texture", font.texture.getTextureView(), font.texture.getSampler())
                .end();

            if (fallbackFont != null && fallbackFont.hasTexture()) {
                fallbackFont.uploadIfDirty();
                MeshRenderer.begin()
                    .attachments(Minecraft.getInstance().gameRenderer.mainRenderTarget())
                    .pipeline(MeteorRenderPipelines.UI_TEXT)
                    .mesh(fallbackMesh)
                    .sampler("u_Texture", fallbackFont.texture().getTextureView(), fallbackFont.texture().getSampler())
                    .end();
            }
        }

        if (vanillaOwned) VanillaTextRenderer.INSTANCE.end();

        building = false;
        graphics = null;
        vanillaOwned = false;
        scale = 1;
    }

    public void destroy() {
        for (Font font : this.fonts) {
            font.texture.close();
        }

        if (fallbackFonts != null) {
            for (DynamicFont fallbackFont : fallbackFonts) fallbackFont.destroy();
        }
    }
}
