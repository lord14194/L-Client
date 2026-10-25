package com.lclient.util;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Through-wall line / box renderer for 1.21.11.
 * Uses two custom render layers (no depth test) and writes into the world render context's own buffers,
 * so it goes through the same code path as vanilla debug rendering.
 * Call begin() -> line()/box() -> end() once per frame (LClient does this in WorldRenderEvents.END_MAIN).
 */
public final class Render {
    private static RenderLayer LINES, QUADS;
    private static boolean failed;

    private static VertexConsumerProvider provider;
    private static MatrixStack.Entry pose;
    private static Vec3d cam = Vec3d.ZERO;
    private static VertexConsumer lines, quads;
    private static float lineWidth = 2f;

    private Render() {}

    private static void layers() {
        if (LINES != null) return;
        RenderPipeline linePipe = RenderPipeline.builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
                .withLocation(Identifier.of("lclient", "pipeline/lines_no_depth"))
                .withVertexFormat(VertexFormats.POSITION_COLOR_NORMAL_LINE_WIDTH, VertexFormat.DrawMode.LINES)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(false)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withCull(false)
                .build();
        RenderPipeline quadPipe = RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
                .withLocation(Identifier.of("lclient", "pipeline/quads_no_depth"))
                .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(false)
                .withBlend(BlendFunction.TRANSLUCENT)
                .withCull(false)
                .build();
        LINES = RenderLayer.of("lclient_lines", RenderSetup.builder(linePipe).build());
        QUADS = RenderLayer.of("lclient_quads", RenderSetup.builder(quadPipe).translucent().build());
    }

    /** Prepare for one frame. {@code matrices} must be the world render context's stack (camera space). */
    public static void begin(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d camera) {
        if (failed) return;
        try {
            layers();
            provider = consumers;
            pose = matrices.peek();
            cam = camera;
            lines = consumers.getBuffer(LINES);
            quads = consumers.getBuffer(QUADS);
        } catch (Throwable t) {
            failed = true;
            provider = null;
            System.err.println("[L Client] ESP renderer disabled: " + t);
            t.printStackTrace();
        }
    }

    /** Flush both layers now so they are drawn on top of the finished frame. */
    public static void end() {
        if (provider instanceof VertexConsumerProvider.Immediate imm && LINES != null) {
            try {
                imm.draw(QUADS);
                imm.draw(LINES);
            } catch (Throwable t) {
                failed = true;
                System.err.println("[L Client] ESP renderer disabled: " + t);
                t.printStackTrace();
            }
        }
        provider = null; lines = null; quads = null;
    }

    private static int opaque(int c) { return (c >>> 24) == 0 ? (c | 0xFF000000) : c; }

    private static void lv(double x, double y, double z, int c, float nx, float ny, float nz) {
        lines.vertex(pose, (float) (x - cam.x), (float) (y - cam.y), (float) (z - cam.z))
                .color(c).normal(pose, nx, ny, nz).lineWidth(lineWidth);
    }

    private static void qv(double x, double y, double z, int c) {
        quads.vertex(pose, (float) (x - cam.x), (float) (y - cam.y), (float) (z - cam.z)).color(c);
    }

    private static void seg(double ax, double ay, double az, double bx, double by, double bz, int c) {
        double dx = bx - ax, dy = by - ay, dz = bz - az;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-6) return;
        float nx = (float) (dx / len), ny = (float) (dy / len), nz = (float) (dz / len);
        lv(ax, ay, az, c, nx, ny, nz);
        lv(bx, by, bz, c, nx, ny, nz);
    }

    public static void line(Vec3d a, Vec3d b, int color) {
        if (lines == null) return;
        seg(a.x, a.y, a.z, b.x, b.y, b.z, opaque(color));
    }

    /** Outline always; translucent fill when fillAlpha > 0 (0-255). */
    public static void box(Box b, int color, int fillAlpha) {
        if (lines == null) return;
        double x1 = b.minX, y1 = b.minY, z1 = b.minZ, x2 = b.maxX, y2 = b.maxY, z2 = b.maxZ;
        int o = color | 0xFF000000;
        // bottom and top rings + verticals
        seg(x1,y1,z1, x2,y1,z1, o); seg(x2,y1,z1, x2,y1,z2, o); seg(x2,y1,z2, x1,y1,z2, o); seg(x1,y1,z2, x1,y1,z1, o);
        seg(x1,y2,z1, x2,y2,z1, o); seg(x2,y2,z1, x2,y2,z2, o); seg(x2,y2,z2, x1,y2,z2, o); seg(x1,y2,z2, x1,y2,z1, o);
        seg(x1,y1,z1, x1,y2,z1, o); seg(x2,y1,z1, x2,y2,z1, o); seg(x2,y1,z2, x2,y2,z2, o); seg(x1,y1,z2, x1,y2,z2, o);
        if (fillAlpha > 0 && quads != null) {
            int f = UI.alpha(color, fillAlpha);
            quad(x1,y1,z1, x2,y1,z1, x2,y1,z2, x1,y1,z2, f);
            quad(x1,y2,z1, x1,y2,z2, x2,y2,z2, x2,y2,z1, f);
            quad(x1,y1,z1, x1,y2,z1, x2,y2,z1, x2,y1,z1, f);
            quad(x1,y1,z2, x2,y1,z2, x2,y2,z2, x1,y2,z2, f);
            quad(x1,y1,z1, x1,y1,z2, x1,y2,z2, x1,y2,z1, f);
            quad(x2,y1,z1, x2,y2,z1, x2,y2,z2, x2,y1,z2, f);
        }
    }

    private static void quad(double ax, double ay, double az, double bx, double by, double bz,
                             double cx, double cy, double cz, double dx, double dy, double dz, int c) {
        qv(ax, ay, az, c); qv(bx, by, bz, c); qv(cx, cy, cz, c); qv(dx, dy, dz, c);
    }
}
