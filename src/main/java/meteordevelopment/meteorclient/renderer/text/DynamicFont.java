/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.renderer.text;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.textures.FilterMode;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import meteordevelopment.meteorclient.renderer.MeshBuilder;
import meteordevelopment.meteorclient.renderer.Texture;
import meteordevelopment.meteorclient.utils.render.color.Color;
import org.lwjgl.BufferUtils;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.stb.STBTruetype;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

/** A lazily populated glyph atlas used for large CJK fonts. */
final class DynamicFont {
    private static final int ATLAS_SIZE = 2048;
    private static final int PADDING = 2;

    private final ByteBuffer fontBuffer;
    private final STBTTFontinfo fontInfo;
    private final int height;
    private final float fontScale;
    private final float ascent;
    private final Int2ObjectOpenHashMap<Glyph> glyphs = new Int2ObjectOpenHashMap<>();

    private ByteBuffer bitmap;
    private Texture texture;
    private int cursorX = PADDING;
    private int cursorY = PADDING;
    private int rowHeight;
    private boolean dirty;
    private boolean atlasFull;

    DynamicFont(ByteBuffer fontBuffer, int height) {
        this.fontBuffer = fontBuffer;
        this.height = height;

        fontInfo = STBTTFontinfo.create();
        if (!STBTruetype.stbtt_InitFont(fontInfo, fontBuffer)) {
            throw new IllegalArgumentException("Failed to initialize CJK fallback font.");
        }

        fontScale = STBTruetype.stbtt_ScaleForPixelHeight(fontInfo, height);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer ascentBuffer = stack.mallocInt(1);
            STBTruetype.stbtt_GetFontVMetrics(fontInfo, ascentBuffer, null, null);
            ascent = ascentBuffer.get(0);
        }
    }

    int getHeight() {
        return height;
    }

    boolean prepare(String text, int length) {
        int limit = Math.min(length, text.length());
        for (int offset = 0; offset < limit;) {
            int cp = text.codePointAt(offset);
            int count = Character.charCount(cp);
            if (offset + count > limit || glyph(cp) == null) return false;
            offset += count;
        }

        return true;
    }

    double getWidth(String text, int length) {
        double width = 0;
        int limit = Math.min(length, text.length());

        for (int offset = 0; offset < limit;) {
            int cp = text.codePointAt(offset);
            int count = Character.charCount(cp);
            if (offset + count > limit) break;

            Glyph glyph = glyph(cp);
            if (glyph == null) return -1;
            width += glyph.advance;
            offset += count;
        }

        return width;
    }

    double render(MeshBuilder mesh, String text, double x, double y, Color color, double scale) {
        y += ascent * fontScale * scale;
        mesh.ensureCapacity(text.codePointCount(0, text.length()) * 4, text.codePointCount(0, text.length()) * 6);

        for (int offset = 0; offset < text.length();) {
            int cp = text.codePointAt(offset);
            Glyph glyph = glyph(cp);
            offset += Character.charCount(cp);
            if (glyph == null) continue;

            if (glyph.hasBitmap()) {
                mesh.quad(
                    mesh.vec2(x + glyph.x0 * scale, y + glyph.y0 * scale).vec2(glyph.u0, glyph.v0).color(color).next(),
                    mesh.vec2(x + glyph.x0 * scale, y + glyph.y1 * scale).vec2(glyph.u0, glyph.v1).color(color).next(),
                    mesh.vec2(x + glyph.x1 * scale, y + glyph.y1 * scale).vec2(glyph.u1, glyph.v1).color(color).next(),
                    mesh.vec2(x + glyph.x1 * scale, y + glyph.y0 * scale).vec2(glyph.u1, glyph.v0).color(color).next()
                );
            }

            x += glyph.advance * scale;
        }

        return x;
    }

    boolean hasTexture() {
        return texture != null;
    }

    Texture texture() {
        return texture;
    }

    void uploadIfDirty() {
        if (!dirty || texture == null) return;
        texture.upload(bitmap);
        dirty = false;
    }

    void destroy() {
        if (texture != null) texture.close();
    }

    private Glyph glyph(int codePoint) {
        Glyph cached = glyphs.get(codePoint);
        if (cached != null) return cached;
        if (atlasFull) return null;
        if (STBTruetype.stbtt_FindGlyphIndex(fontInfo, codePoint) == 0) return null;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer advanceBuffer = stack.mallocInt(1);
            STBTruetype.stbtt_GetCodepointHMetrics(fontInfo, codePoint, advanceBuffer, null);
            float advance = advanceBuffer.get(0) * fontScale;

            IntBuffer widthBuffer = stack.mallocInt(1);
            IntBuffer heightBuffer = stack.mallocInt(1);
            IntBuffer xOffsetBuffer = stack.mallocInt(1);
            IntBuffer yOffsetBuffer = stack.mallocInt(1);
            ByteBuffer glyphBitmap = STBTruetype.stbtt_GetCodepointBitmap(
                fontInfo, fontScale, fontScale, codePoint,
                widthBuffer, heightBuffer, xOffsetBuffer, yOffsetBuffer
            );

            int width = widthBuffer.get(0);
            int glyphHeight = heightBuffer.get(0);
            int xOffset = xOffsetBuffer.get(0);
            int yOffset = yOffsetBuffer.get(0);

            if (width <= 0 || glyphHeight <= 0 || glyphBitmap == null) {
                Glyph glyph = new Glyph(0, 0, 0, 0, 0, 0, 0, 0, advance);
                glyphs.put(codePoint, glyph);
                if (glyphBitmap != null) STBTruetype.stbtt_FreeBitmap(glyphBitmap);
                return glyph;
            }

            ensureAtlas();
            if (cursorX + width + PADDING > ATLAS_SIZE) {
                cursorX = PADDING;
                cursorY += rowHeight + PADDING;
                rowHeight = 0;
            }
            if (cursorY + glyphHeight + PADDING > ATLAS_SIZE) {
                STBTruetype.stbtt_FreeBitmap(glyphBitmap);
                atlasFull = true;
                return null;
            }

            int atlasX = cursorX;
            int atlasY = cursorY;
            for (int row = 0; row < glyphHeight; row++) {
                int sourceOffset = row * width;
                int targetOffset = (atlasY + row) * ATLAS_SIZE + atlasX;
                for (int column = 0; column < width; column++) {
                    bitmap.put(targetOffset + column, glyphBitmap.get(sourceOffset + column));
                }
            }
            STBTruetype.stbtt_FreeBitmap(glyphBitmap);

            cursorX += width + PADDING;
            rowHeight = Math.max(rowHeight, glyphHeight);
            dirty = true;

            float inverseSize = 1f / ATLAS_SIZE;
            Glyph glyph = new Glyph(
                xOffset,
                yOffset,
                xOffset + width,
                yOffset + glyphHeight,
                atlasX * inverseSize,
                atlasY * inverseSize,
                (atlasX + width) * inverseSize,
                (atlasY + glyphHeight) * inverseSize,
                advance
            );
            glyphs.put(codePoint, glyph);
            return glyph;
        }
    }

    private void ensureAtlas() {
        if (texture != null) return;

        bitmap = BufferUtils.createByteBuffer(ATLAS_SIZE * ATLAS_SIZE);
        texture = new Texture(ATLAS_SIZE, ATLAS_SIZE, GpuFormat.R8_UNORM, FilterMode.LINEAR, FilterMode.LINEAR);
        dirty = true;
    }

    private record Glyph(
        float x0, float y0, float x1, float y1,
        float u0, float v0, float u1, float v1,
        float advance
    ) {
        boolean hasBitmap() {
            return x0 != x1 && y0 != y1;
        }
    }
}
