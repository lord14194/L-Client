package com.lclient.util;

import net.minecraft.client.gui.DrawContext;

public final class UI {
    private UI() {}

    /** Filled rounded rectangle, drawn row by row so translucent colors never double-blend. */
    public static void rrect(DrawContext c, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0) return;
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        if (r == 0) { c.fill(x, y, x + w, y + h, color); return; }
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = r - (int) Math.round(Math.sqrt(r * r - dy * dy));
            c.fill(x + inset, y + i, x + w - inset, y + i + 1, color);
            c.fill(x + inset, y + h - i - 1, x + w - inset, y + h - i, color);
        }
        c.fill(x, y + r, x + w, y + h - r, color);
    }

    /** Rounded rectangle with a 1px border (both opaque colours work best). */
    public static void rrectBorder(DrawContext c, int x, int y, int w, int h, int r, int fill, int border) {
        rrect(c, x - 1, y - 1, w + 2, h + 2, r + 1, border);
        rrect(c, x, y, w, h, r, fill);
    }

    /** Soft drop shadow: a few growing translucent rounded rectangles behind a window. */
    public static void shadow(DrawContext c, int x, int y, int w, int h, int r) {
        for (int i = 8; i >= 1; i--)
            rrect(c, x - i, y - i + 2, w + i * 2, h + i * 2, r + i, alpha(0x000000, 10));
    }

    public static int mix(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa = (a >>> 24), ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int ba = (b >>> 24), br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16)
             | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    public static int alpha(int color, int a) {
        return (Math.max(0, Math.min(255, a)) << 24) | (color & 0xFFFFFF);
    }

    /** Opaque version of a colour, for text (1.21.6+ treats alpha 0 text as invisible). */
    public static int solid(int color) { return color | 0xFF000000; }
}
