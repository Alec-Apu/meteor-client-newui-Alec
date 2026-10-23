/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import it.unimi.dsi.fastutil.Stack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.renderer.operations.TextOperation;
import meteordevelopment.meteorclient.gui.renderer.packer.GuiTexture;
import meteordevelopment.meteorclient.gui.renderer.packer.TexturePacker;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.Texture;
import meteordevelopment.meteorclient.utils.PostInit;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;
import static meteordevelopment.meteorclient.utils.Utils.getWindowHeight;
import static meteordevelopment.meteorclient.utils.Utils.getWindowWidth;

public class GuiRenderer {
    private static final Color WHITE = new Color(255, 255, 255);

    private static final TexturePacker TEXTURE_PACKER = new TexturePacker();
    private static Texture TEXTURE;

    public static GuiTexture CIRCLE;
    public static GuiTexture TRIANGLE;
    public static GuiTexture EDIT;
    public static GuiTexture RESET;
    public static GuiTexture FAVORITE_NO, FAVORITE_YES;
    public static GuiTexture COPY, PASTE;

    public GuiTheme theme;

    private final Renderer2D r = new Renderer2D(false);
    private final Renderer2D rTex = new Renderer2D(true);

    private final Pool<Scissor> scissorPool = new Pool<>(Scissor::new);
    private final Stack<Scissor> scissorStack = new ObjectArrayList<>();

    private final Pool<TextOperation> textPool = new Pool<>(TextOperation::new);
    private final List<TextOperation> texts = new ObjectArrayList<>();

    private final List<Runnable> postTasks = new ObjectArrayList<>();

    public String tooltip, lastTooltip;
    public WWidget tooltipWidget;
    private double tooltipAnimProgress;

    private GuiGraphicsExtractor graphics;

    public static GuiTexture addTexture(Identifier id) {
        return TEXTURE_PACKER.add(id);
    }

    @PostInit
    public static void init() {
        CIRCLE = addTexture(MeteorClient.identifier("textures/icons/gui/circle.png"));
        TRIANGLE = addTexture(MeteorClient.identifier("textures/icons/gui/triangle.png"));
        EDIT = addTexture(MeteorClient.identifier("textures/icons/gui/edit.png"));
        RESET = addTexture(MeteorClient.identifier("textures/icons/gui/reset.png"));
        FAVORITE_NO = addTexture(MeteorClient.identifier("textures/icons/gui/favorite_no.png"));
        FAVORITE_YES = addTexture(MeteorClient.identifier("textures/icons/gui/favorite_yes.png"));

        COPY = addTexture(MeteorClient.identifier("textures/icons/gui/copy.png"));
        PASTE = addTexture(MeteorClient.identifier("textures/icons/gui/paste.png"));

        TEXTURE = TEXTURE_PACKER.pack();
    }

    public void begin(GuiGraphicsExtractor graphics) {
        this.graphics = graphics;
        this.graphics.nextStratum();

        var matrices = graphics.pose();
        matrices.pushMatrix();
        matrices.scale(1.0f / mc.getWindow().getGuiScale());

        scissorStart(0, 0, getWindowWidth(), getWindowHeight());
    }

    public void end() {
        scissorEnd();

        for (Runnable task : postTasks) task.run();
        postTasks.clear();

        graphics.pose().popMatrix();
        graphics.nextStratum();
    }

    public void beginRender() {
        r.begin();
        rTex.begin();
    }

    public void endRender() {
        endRender(null);
    }

    public void endRender(Scissor scissor) {
        if (scissor != null) scissor.push();

        r.end();
        rTex.end();

        r.render();
        rTex.render("u_Texture", TEXTURE.getTextureView(), TEXTURE.getSampler());

        // Normal text
        theme.textRenderer().begin(graphics, theme.scale(1));
        for (TextOperation text : texts) {
            if (!text.title) text.run(textPool);
        }
        theme.textRenderer().end();

        // Title text
        theme.textRenderer().begin(graphics, theme.scale(1.25));
        for (TextOperation text : texts) {
            if (text.title) text.run(textPool);
        }
        theme.textRenderer().end();

        texts.clear();

        if (scissor != null) scissor.pop();
    }

    public void scissorStart(double x, double y, double width, double height) {
        if (!scissorStack.isEmpty()) {
            Scissor parent = scissorStack.top();

            if (x < parent.x) x = parent.x;
            else if (x + width > parent.x + parent.width) width -= (x + width) - (parent.x + parent.width);

            if (y < parent.y) y = parent.y;
            else if (y + height > parent.y + parent.height) height -= (y + height) - (parent.y + parent.height);

            endRender(parent);
        }

        scissorStack.push(scissorPool.get().set(x, y, width, height));
        graphics.enableScissor((int) x, (int) y, (int) (x + width), (int) (y + height));

        beginRender();
    }

    public void scissorEnd() {
        Scissor scissor = scissorStack.pop();

        endRender(scissor);

        scissor.push();
        for (Runnable task : scissor.postTasks) task.run();
        scissor.pop();

        graphics.disableScissor();
        if (!scissorStack.isEmpty()) beginRender();

        scissorPool.free(scissor);
    }

    public boolean renderTooltip(GuiGraphicsExtractor graphics, double mouseX, double mouseY, double delta) {
        tooltipAnimProgress += (tooltip != null ? 1 : -1) * delta * 14;
        tooltipAnimProgress = Mth.clamp(tooltipAnimProgress, 0, 1);

        boolean toReturn = false;

        if (tooltipAnimProgress > 0) {
            if (tooltip != null && !tooltip.equals(lastTooltip)) {
                tooltipWidget = theme.tooltip(tooltip);
                tooltipWidget.init();
            }

            double deltaX = -tooltipWidget.x + mouseX + 12;
            double deltaY = -tooltipWidget.y + mouseY + 12;

            if (mouseX + 12 + tooltipWidget.width > getWindowWidth())
                deltaX = -tooltipWidget.x + getWindowWidth() - tooltipWidget.width;
            if (mouseY + 12 + tooltipWidget.height > getWindowHeight())
                deltaY = -tooltipWidget.y + getWindowHeight() - tooltipWidget.height;

            tooltipWidget.move(deltaX, deltaY);

            setAlpha(tooltipAnimProgress);

            begin(graphics);
            tooltipWidget.render(this, mouseX, mouseY, delta);
            end();

            setAlpha(1);

            lastTooltip = tooltip;
            toReturn = true;
        }

        tooltip = null;
        return toReturn;
    }

    public void setAlpha(double a) {
        r.setAlpha(a);
        rTex.setAlpha(a);

        theme.textRenderer().setAlpha(a);
    }

    public void tooltip(String text) {
        tooltip = text;
    }

    public void quad(double x, double y, double width, double height, Color cTopLeft, Color cTopRight, Color cBottomRight, Color cBottomLeft) {
        r.quad(x, y, width, height, cTopLeft, cTopRight, cBottomRight, cBottomLeft);
    }

    public void quad(double x, double y, double width, double height, Color colorLeft, Color colorRight) {
        quad(x, y, width, height, colorLeft, colorRight, colorRight, colorLeft);
    }

    public void quad(double x, double y, double width, double height, Color color) {
        quad(x, y, width, height, color, color);
    }

    public void quad(WWidget widget, Color color) {
        quad(widget.x, widget.y, widget.width, widget.height, color);
    }

    public void line(double x1, double y1, double x2, double y2, Color color) {
        r.line(x1, y1, x2, y2, color);
    }

    public void quad(double x, double y, double width, double height, GuiTexture texture, Color color) {
        rTex.texQuad(x, y, width, height, texture.get(width, height), color);
    }

    /**
     * Draws an anti-aliased rounded rectangle using non-overlapping body quads and
     * quarter-circle texture samples. Unlike the old horizontal-band approximation,
     * the edge quality does not depend on a small fixed segment count.
     */
    public void roundedQuad(double x, double y, double width, double height, double radius, Color color) {
        radius = clampRadius(radius, width, height, true, true);
        if (radius <= 0) {
            quad(x, y, width, height, color);
            return;
        }

        quad(x + radius, y, width - radius * 2, height, color);
        quad(x, y + radius, radius, height - radius * 2, color);
        quad(x + width - radius, y + radius, radius, height - radius * 2, color);

        circleQuarter(x, y, radius, Corner.TopLeft, color);
        circleQuarter(x + width - radius, y, radius, Corner.TopRight, color);
        circleQuarter(x + width - radius, y + height - radius, radius, Corner.BottomRight, color);
        circleQuarter(x, y + height - radius, radius, Corner.BottomLeft, color);
    }

    /** Draws a rectangle with only its top two corners rounded. */
    public void roundedTopQuad(double x, double y, double width, double height, double radius, Color color) {
        radius = clampRadius(radius, width, height, true, false);
        if (radius <= 0) {
            quad(x, y, width, height, color);
            return;
        }

        quad(x, y + radius, width, height - radius, color);
        quad(x + radius, y, width - radius * 2, radius, color);
        circleQuarter(x, y, radius, Corner.TopLeft, color);
        circleQuarter(x + width - radius, y, radius, Corner.TopRight, color);
    }

    /** Draws a rectangle with only its left two corners rounded. */
    public void roundedLeftQuad(double x, double y, double width, double height, double radius, Color color) {
        radius = clampRadius(radius, width, height, false, true);
        if (radius <= 0) {
            quad(x, y, width, height, color);
            return;
        }

        quad(x + radius, y, width - radius, height, color);
        quad(x, y + radius, radius, height - radius * 2, color);
        circleQuarter(x, y, radius, Corner.TopLeft, color);
        circleQuarter(x, y + height - radius, radius, Corner.BottomLeft, color);
    }

    private double clampRadius(double radius, double width, double height, boolean bothX, boolean bothY) {
        if (width <= 0 || height <= 0) return 0;
        double maxX = bothX ? width / 2 : width;
        double maxY = bothY ? height / 2 : height;
        return Math.max(0, Math.min(radius, Math.min(maxX, maxY)));
    }

    private void circleQuarter(double x, double y, double radius, Corner corner, Color color) {
        var region = CIRCLE.get(radius * 2 * mc.getWindow().getGuiScale(), radius * 2 * mc.getWindow().getGuiScale());
        double midX = (region.x1 + region.x2) / 2;
        double midY = (region.y1 + region.y2) / 2;

        double u1 = corner.left ? region.x1 : midX;
        double u2 = corner.left ? midX : region.x2;
        double v1 = corner.top ? region.y1 : midY;
        double v2 = corner.top ? midY : region.y2;
        rTex.texQuad(x, y, radius, radius, 0, u1, v1, u2, v2, color);
    }

    private enum Corner {
        TopLeft(true, true),
        TopRight(false, true),
        BottomRight(false, false),
        BottomLeft(true, false);

        private final boolean left;
        private final boolean top;

        Corner(boolean left, boolean top) {
            this.left = left;
            this.top = top;
        }
    }

    public void rotatedQuad(double x, double y, double width, double height, double rotation, GuiTexture texture, Color color) {
        rTex.texQuad(x, y, width, height, rotation, texture.get(width, height), color);
    }

    public void triangle(double x1, double y1, double x2, double y2, double x3, double y3, Color color) {
        r.triangle(x1, y1, x2, y2, x3, y3, color);
    }

    public void text(String text, double x, double y, Color color, boolean title) {
        texts.add(getOp(textPool, x, y, color).set(text, theme.textRenderer(), title));
    }

    public void texture(double x, double y, double width, double height, double rotation, Texture texture) {
        post(() -> {
            rTex.begin();
            rTex.texQuad(x, y, width, height, rotation, 0, 0, 1, 1, WHITE);
            rTex.end();

            rTex.render(texture.getTextureView(), texture.getSampler());
        });
    }

    public void texture(double x, double y, double width, double height, double rotation, GpuTextureView texture) {
        post(() -> {
            rTex.begin();
            // Framebuffer textures use the opposite vertical origin from GUI
            // assets, so flip V to keep sampled world content upright.
            rTex.texQuad(x, y, width, height, rotation, 0, 1, 1, 0, WHITE);
            rTex.end();

            rTex.render(texture, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
        });
    }

    public void post(Runnable task) {
        scissorStack.top().postTasks.add(task);
    }

    public void item(ItemStack itemStack, int x, int y, float scale, boolean overlay) {
        RenderUtils.drawItem(graphics, itemStack, x, y, scale, overlay, null, false);
    }

    public void absolutePost(Runnable task) {
        postTasks.add(task);
    }

    private <T extends GuiRenderOperation<T>> T getOp(Pool<T> pool, double x, double y, Color color) {
        T op = pool.get();
        op.set(x, y, color);
        return op;
    }
}
