package com.lclient.module.impl;

import com.lclient.module.*;
import com.lclient.module.Module;
import com.lclient.setting.*;
import com.lclient.util.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.world.World;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class Hud extends Module {
    private final BoolSetting watermark = bool("Watermark", true);
    private final BoolSetting coords = bool("Coordinates", true);
    private final BoolSetting list = bool("Module list", true);
    private final NumSetting hue = num("Accent hue", 75, 0, 360, 1);

    public Hud() {
        super("HUD", "Overlay and theme", Category.VISUAL);
        enabled = true;
    }

    @Override public void onTick() {
        Theme.accent = 0xFF000000 | Color.HSBtoRGB(hue.i() / 360f, 0.8f, 1f);
    }

    public void render(DrawContext ctx) {
        if (mc.player == null || mc.world == null || mc.options.hudHidden) return;
        var tr = mc.textRenderer;
        int accent = Theme.accent;

        if (watermark.value) {
            String a = "L Client", b = " | " + mc.getCurrentFps() + " fps";
            int w = tr.getWidth(a) + tr.getWidth(b) + 12;
            UI.rrect(ctx, 4, 4, w, 16, 0, 0xC00A0A0B);
            UI.rrect(ctx, 4, 4, 2, 16, 0, accent);
            ctx.drawText(tr, a, 10, 8, accent, true);
            ctx.drawText(tr, b, 10 + tr.getWidth(a), 8, Theme.MUTED, true);
        }

        if (coords.value) {
            int x = mc.player.getBlockX(), y = mc.player.getBlockY(), z = mc.player.getBlockZ();
            boolean nether = mc.world.getRegistryKey() == World.NETHER;
            String other = nether ? "Overworld " + (x * 8) + ", " + (z * 8) : "Nether " + (x / 8) + ", " + (z / 8);
            String l1 = "XYZ  " + x + " " + y + " " + z + "  " + mc.player.getHorizontalFacing().asString();
            int w = Math.max(tr.getWidth(l1), tr.getWidth(other)) + 12;
            int h = mc.getWindow().getScaledHeight();
            UI.rrect(ctx, 4, h - 30, w, 26, 0, 0xC00A0A0B);
            UI.rrect(ctx, 4, h - 30, 2, 26, 0, accent);
            ctx.drawText(tr, l1, 10, h - 25, Theme.TEXT, true);
            ctx.drawText(tr, other, 10, h - 14, Theme.MUTED, true);
        }

        if (list.value) {
            List<Module> shown = new ArrayList<>();
            for (Module m : ModuleManager.ALL) {
                m.hudAnim += ((m.enabled ? 1f : 0f) - m.hudAnim) * 0.2f;
                if (m != this && m.hudAnim > 0.02f) shown.add(m);
            }
            shown.sort((p, q) -> tr.getWidth(q.name) - tr.getWidth(p.name));
            int sw = mc.getWindow().getScaledWidth(), y = 4;
            for (Module m : shown) {
                int w = tr.getWidth(m.name) + 10;
                int x = sw - 4 - (int) (w * m.hudAnim);
                UI.rrect(ctx, x, y, w, 12, 0, UI.alpha(0x0A0A0B, (int) (176 * m.hudAnim)));
                UI.rrect(ctx, x + w - 2, y, 2, 12, 0, UI.alpha(accent, (int) (255 * m.hudAnim)));
                ctx.drawText(tr, m.name, x + 4, y + 2, UI.alpha(accent, (int) (255 * m.hudAnim)), true);
                y += 13;
            }
        }
    }
}
