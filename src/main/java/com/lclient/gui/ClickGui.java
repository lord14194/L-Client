package com.lclient.gui;

import com.lclient.LClient;
import com.lclient.integration.ExternalModule;
import com.lclient.integration.Integrations;
import com.lclient.module.Category;
import com.lclient.module.Config;
import com.lclient.module.Module;
import com.lclient.module.ModuleManager;
import com.lclient.setting.BoolSetting;
import com.lclient.setting.NumSetting;
import com.lclient.setting.Setting;
import com.lclient.util.Theme;
import com.lclient.util.UI;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * L Client GUI: sidebar with tabs (L's own categories + one tab per bundled mod),
 * search, scrollable module list. Left click toggles, right click / "+" expands settings.
 */
public class ClickGui extends Screen {
    private static final int HEAD = 28, SIDE = 92, ROW = 20, SUBROW = 16, STATUS = 16;

    /** group == null -> one of L's own categories, otherwise a bundled mod's tab. */
    private record Tab(String label, Category cat, String group) {}

    private static class Hit {
        int x, y, w, h;
        boolean content, search;
        Runnable left, right;
        NumSetting slider;
        boolean in(double mx, double my) { return mx >= x && mx < x + w && my >= y && my < y + h; }
    }

    private final List<Hit> hits = new ArrayList<>();
    private final List<Tab> tabs = new ArrayList<>();
    private int tab;
    private Hit dragging;
    private Module binding;
    private String query = "";
    private boolean searchFocus;
    private float open, scrollAnim;
    private int scrollTarget, contentH, viewTop, viewBottom;
    private String hoverDesc = "";

    public ClickGui() { super(Text.literal("L Client")); }

    @Override public boolean shouldPause() { return false; }

    @Override protected void init() {
        Integrations.refresh();
        tabs.clear();
        for (Category c : Category.values()) if (c != Category.EXTERNAL) tabs.add(new Tab(c.label, c, null));
        for (String g : Integrations.groups()) tabs.add(new Tab(g, null, g));
        if (tab >= tabs.size()) tab = 0;
    }

    // ------------------------------------------------------------------ helpers

    private Hit hit(int x, int y, int w, int h, boolean content, Runnable l, Runnable r) {
        Hit t = new Hit();
        t.x = x; t.y = y; t.w = w; t.h = h; t.content = content; t.left = l; t.right = r;
        hits.add(t);
        return t;
    }

    private List<Module> modulesOf(Tab t) {
        if (t.group() == null) return ModuleManager.in(t.cat());
        return new ArrayList<Module>(Integrations.group(t.group()));
    }

    private static void sync(List<Module> mods) {
        for (Module m : mods) if (m instanceof ExternalModule e) e.sync();
    }

    private int enabledCount(Tab t) {
        int n = 0;
        List<Module> mods = modulesOf(t);
        sync(mods);
        for (Module m : mods) if (m.enabled) n++;
        return n;
    }

    /** Rows to show: modules, with String entries as section headers. */
    private List<Object> entries() {
        List<Object> out = new ArrayList<>();
        String q = query.trim().toLowerCase();
        if (!q.isEmpty()) {
            for (Tab t : tabs) {
                List<Module> match = new ArrayList<>();
                for (Module m : modulesOf(t)) if (m.name.toLowerCase().contains(q)) match.add(m);
                if (match.isEmpty()) continue;
                sync(match);
                out.add(t.label());
                out.addAll(match);
            }
            return out;
        }
        List<Module> mods = modulesOf(tabs.get(tab));
        sync(mods);
        String lastPage = null;
        for (Module m : mods) {
            if (m instanceof ExternalModule e && !e.page.equals(lastPage)) {
                lastPage = e.page;
                out.add(e.page);
            }
            out.add(m);
        }
        return out;
    }

    private static String keyName(int key) {
        String n = GLFW.glfwGetKeyName(key, 0);
        if (n != null) return n.toUpperCase();
        return switch (key) {
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "R-SHIFT";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "L-SHIFT";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "R-CTRL";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "L-CTRL";
            case GLFW.GLFW_KEY_LEFT_ALT -> "L-ALT";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "R-ALT";
            case GLFW.GLFW_KEY_SPACE -> "SPACE";
            case GLFW.GLFW_KEY_TAB -> "TAB";
            default -> "KEY " + key;
        };
    }

    private String fit(String s, int maxW) {
        if (textRenderer.getWidth(s) <= maxW) return s;
        String t = s;
        while (t.length() > 1 && textRenderer.getWidth(t + "..") > maxW) t = t.substring(0, t.length() - 1);
        return t + "..";
    }

    private void text(DrawContext c, String s, int x, int y, int color) {
        c.drawText(textRenderer, s, x, y, UI.solid(color), true);
    }

    // hard-edged primitives: this GUI is all straight lines, no rounding
    private static void box(DrawContext c, int x, int y, int w, int h, int color) {
        if (w > 0 && h > 0) c.fill(x, y, x + w, y + h, color);
    }

    private static void frame(DrawContext c, int x, int y, int w, int h, int color) {
        box(c, x, y, w, 1, color);
        box(c, x, y + h - 1, w, 1, color);
        box(c, x, y + 1, 1, h - 2, color);
        box(c, x + w - 1, y + 1, 1, h - 2, color);
    }

    private static void brackets(DrawContext c, int x, int y, int w, int h, int len, int color) {
        box(c, x, y, len, 1, color);               box(c, x, y, 1, len, color);
        box(c, x + w - len, y, len, 1, color);     box(c, x + w - 1, y, 1, len, color);
        box(c, x, y + h - 1, len, 1, color);       box(c, x, y + h - len, 1, len, color);
        box(c, x + w - len, y + h - 1, len, 1, color); box(c, x + w - 1, y + h - len, 1, len, color);
    }

    /** Small square tick box. t = 0..1 fills it from the centre. */
    private static void tick(DrawContext c, int x, int y, float t, int accent) {
        frame(c, x, y, 7, 7, UI.mix(Theme.FAINT, accent, t));
        int sz = Math.round(5 * t);
        int off = (5 - sz) / 2;
        box(c, x + 1 + off, y + 1 + off, sz, sz, accent);
    }

    // ------------------------------------------------------------------ render

    @Override public void render(DrawContext ctx, int mx, int my, float delta) {
        hits.clear();
        hoverDesc = "";
        open += (1f - open) * 0.22f;
        if (open > 0.995f) open = 1f;
        scrollAnim += (scrollTarget - scrollAnim) * 0.3f;
        if (Math.abs(scrollTarget - scrollAnim) < 0.3f) scrollAnim = scrollTarget;
        int accent = Theme.accent;

        // dim the world behind the window
        box(ctx, 0, 0, width, height, UI.alpha(0x000000, (int) (150 * open)));

        int W = Math.min(width - 16, 470), H = Math.min(height - 16, 286);
        int x0 = (width - W) / 2, y0 = (height - H) / 2 + (int) ((1f - open) * 14);

        // hard offset shadow, window body, faint dot grid, border
        box(ctx, x0 + 4, y0 + 4, W, H, UI.alpha(0x000000, (int) (170 * open)));
        box(ctx, x0, y0, W, H, Theme.WINDOW);
        int dot = UI.alpha(0xFFFFFF, 9);
        for (int gy = y0 + HEAD + 6; gy < y0 + H - STATUS - 2; gy += 12)
            for (int gx = x0 + SIDE + 14; gx < x0 + W - 4; gx += 12) box(ctx, gx, gy, 1, 1, dot);
        frame(ctx, x0, y0, W, H, Theme.BORDER);

        // header
        box(ctx, x0 + 1, y0 + 1, W - 2, HEAD - 1, Theme.HEADER);
        box(ctx, x0, y0 + HEAD, W, 1, Theme.DIVIDER);
        box(ctx, x0 + 1, y0 + 1, (int) ((W - 2) * 0.3f * open), 2, accent);
        frame(ctx, x0 + 10, y0 + 8, 15, 15, accent);
        text(ctx, "L", x0 + 17 - textRenderer.getWidth("L") / 2, y0 + 12, accent);
        text(ctx, "CLIENT", x0 + 32, y0 + 7, Theme.TEXT);
        text(ctx, "v" + LClient.MC_VERSION, x0 + 32, y0 + 17, Theme.FAINT);

        // search box
        int sw = Math.min(150, W - 150), sx = x0 + W - sw - 10, sy = y0 + 8;
        Hit sh = hit(sx, sy, sw, 15, false, () -> searchFocus = true, () -> { query = ""; searchFocus = true; });
        sh.search = true;
        box(ctx, sx, sy, sw, 15, Theme.FIELD);
        frame(ctx, sx, sy, sw, 15, searchFocus ? accent : Theme.BORDER);
        text(ctx, ">", sx + 5, sy + 4, accent);
        boolean caret = searchFocus && (System.currentTimeMillis() / 500) % 2 == 0;
        if (query.isEmpty() && !searchFocus) text(ctx, "search", sx + 15, sy + 4, Theme.FAINT);
        else text(ctx, fit(query, sw - 32) + (caret ? "_" : ""), sx + 15, sy + 4, Theme.TEXT);

        // sidebar
        int sbY = y0 + HEAD + 1, sbH = H - HEAD - 2 - STATUS;
        box(ctx, x0 + 1, sbY, SIDE + 7, sbH, Theme.SIDEBAR);
        box(ctx, x0 + SIDE + 8, sbY, 1, sbH, Theme.DIVIDER);
        int ty = sbY + 6;
        boolean searching = !query.trim().isEmpty();
        boolean divided = false;
        for (int i = 0; i < tabs.size(); i++) {
            Tab t = tabs.get(i);
            if (t.group() != null && !divided) {
                divided = true;
                box(ctx, x0 + 8, ty + 3, SIDE - 8, 1, Theme.DIVIDER);
                text(ctx, "// BUNDLED", x0 + 8, ty + 9, Theme.FAINT);
                ty += 22;
            }
            final int idx = i;
            Hit th = hit(x0 + 1, ty, SIDE + 7, 18, false, () -> { tab = idx; scrollTarget = 0; scrollAnim = 0; query = ""; }, null);
            boolean sel = i == tab && !searching;
            boolean hov = th.in(mx, my);
            if (sel) {
                box(ctx, th.x, th.y, th.w, th.h, Theme.ROW);
                box(ctx, th.x, th.y, 2, th.h, accent);
            } else if (hov) {
                box(ctx, th.x, th.y, th.w, th.h, UI.mix(Theme.SIDEBAR, Theme.ROW, 0.6f));
            }
            text(ctx, String.format("%02d", i + 1), th.x + 8, th.y + 5, sel ? accent : Theme.FAINT);
            int n = enabledCount(t);
            String b = n > 0 ? String.valueOf(n) : "";
            int bw = b.isEmpty() ? 0 : textRenderer.getWidth(b) + 8;
            text(ctx, fit(t.label().toUpperCase(), th.w - 24 - bw - 4), th.x + 24, th.y + 5, (sel || hov) ? Theme.TEXT : Theme.MUTED);
            if (n > 0) text(ctx, b, th.x + th.w - 8 - textRenderer.getWidth(b), th.y + 5, accent);
            ty += 20;
        }

        // content area
        int cx = x0 + SIDE + 17, cw = W - SIDE - 17 - 11;
        viewTop = y0 + HEAD + 7;
        viewBottom = y0 + H - STATUS - 3;

        ctx.enableScissor(x0 + SIDE + 10, viewTop - 3, x0 + W - 3, viewBottom);
        int y = viewTop - (int) scrollAnim;
        int startY = y;
        List<Object> entries = entries();
        if (entries.isEmpty()) {
            text(ctx, searching ? "no match for \"" + query.trim() + "\"" : "nothing here yet", cx + 4, y + 6, Theme.MUTED);
            y += 24;
        }
        for (Object o : entries) {
            if (o instanceof String hdr) {
                String up = "// " + hdr.toUpperCase();
                text(ctx, up, cx + 2, y + 4, UI.mix(Theme.MUTED, accent, 0.55f));
                box(ctx, cx + 8 + textRenderer.getWidth(up), y + 8, cw - 8 - textRenderer.getWidth(up), 1, Theme.DIVIDER);
                y += 15;
            } else {
                y = drawModule(ctx, (Module) o, cx, y, cw, mx, my);
            }
        }
        ctx.disableScissor();
        contentH = y - startY + 6;
        int maxScroll = Math.max(0, contentH - (viewBottom - viewTop));
        if (scrollTarget > maxScroll) scrollTarget = maxScroll;
        if (scrollTarget < 0) scrollTarget = 0;

        // scrollbar
        if (maxScroll > 0) {
            int trackH = viewBottom - viewTop;
            int barH = Math.max(14, trackH * trackH / contentH);
            int barY = viewTop + (int) ((trackH - barH) * (scrollAnim / maxScroll));
            box(ctx, x0 + W - 6, viewTop, 1, trackH, Theme.DIVIDER);
            box(ctx, x0 + W - 7, barY, 3, barH, accent);
        }

        // status bar
        int stY = y0 + H - STATUS;
        box(ctx, x0 + 1, stY, W - 2, STATUS - 1, Theme.HEADER);
        box(ctx, x0 + 1, stY, W - 2, 1, Theme.DIVIDER);
        String hint = "LMB toggle   RMB settings   scroll   type to search";
        text(ctx, fit(hoverDesc.isEmpty() ? hint : hoverDesc, W - 90), x0 + 10, stY + 5, hoverDesc.isEmpty() ? Theme.FAINT : Theme.MUTED);
        String right = "L CLIENT";
        text(ctx, right, x0 + W - 10 - textRenderer.getWidth(right), stY + 5, UI.alpha(accent, 200));

        // corner brackets just outside the window
        brackets(ctx, x0 - 3, y0 - 3, W + 6, H + 6, 9, UI.alpha(accent, (int) (255 * open)));
    }

    private int drawModule(DrawContext ctx, Module m, int cx, int y, int cw, int mx, int my) {
        int accent = Theme.accent;
        ExternalModule ext = m instanceof ExternalModule e ? e : null;
        boolean launcher = ext != null && ext.isAction();
        boolean hasSettings = !m.settings.isEmpty();
        float target = m.enabled ? 1f : 0f;
        m.anim += (target - m.anim) * 0.25f;
        if (Math.abs(target - m.anim) < 0.01f) m.anim = target;

        Runnable left = launcher ? () -> ext.action.accept(this) : m::toggle;
        Runnable right = hasSettings ? () -> m.expanded = !m.expanded : null;
        Hit h = hit(cx, y, cw, ROW - 2, true, left, right);
        boolean visible = y + ROW > viewTop - 4 && y < viewBottom;
        boolean hover = visible && h.in(mx, my) && my >= viewTop - 3 && my < viewBottom;
        if (hover) hoverDesc = m.description;

        if (visible) {
            int base = hover ? Theme.ROW_HOVER : Theme.ROW;
            box(ctx, h.x, h.y, h.w, h.h, UI.mix(base, accent, m.anim * 0.12f));
            if (m.anim > 0.02f) box(ctx, h.x, h.y, 2, h.h, UI.alpha(accent, (int) (255 * m.anim)));
            int nameColor = UI.mix(Theme.MUTED, Theme.TEXT, Math.max(m.anim, hover ? 0.8f : 0f));
            if (launcher) text(ctx, ">", h.x + 8, h.y + 5, hover ? accent : Theme.FAINT);
            else tick(ctx, h.x + 8, h.y + (h.h - 7) / 2, m.anim, accent);
            text(ctx, fit(m.name, cw - 21 - (hasSettings ? 56 : 44)), h.x + 21, h.y + 5, nameColor);
            String st = launcher ? "OPEN >" : (m.anim > 0.5f ? "ON" : "OFF");
            int stColor = launcher ? (hover ? accent : Theme.MUTED) : UI.mix(Theme.FAINT, accent, m.anim);
            text(ctx, st, h.x + h.w - 8 - textRenderer.getWidth(st), h.y + 5, stColor);
        }
        if (hasSettings) {
            Hit eh = hit(cx + cw - 50, y, 18, ROW - 2, true, () -> m.expanded = !m.expanded, () -> m.expanded = !m.expanded);
            if (visible) text(ctx, m.expanded ? "-" : "+", eh.x + 6, eh.y + 5, eh.in(mx, my) ? accent : Theme.FAINT);
        }
        y += ROW;

        if (m.expanded && (hasSettings || ext == null)) {
            int sx = cx + 8, sw = cw - 8;
            int rows = m.settings.size() + (ext == null ? 1 : 0);
            int blockH = rows * SUBROW + 4;
            if (y + blockH > viewTop - 4 && y < viewBottom) {
                box(ctx, sx - 2, y - 1, sw + 2, blockH, Theme.SUB);
                box(ctx, sx - 2, y - 1, 1, blockH, UI.alpha(accent, 150));
            }
            int sy = y + 1;
            for (Setting<?> s : m.settings) {
                boolean vis = sy + SUBROW > viewTop - 4 && sy < viewBottom;
                if (s instanceof BoolSetting b) {
                    Hit bh = hit(sx + 2, sy, sw - 4, SUBROW - 1, true, b::toggle, null);
                    if (vis) {
                        if (bh.in(mx, my)) box(ctx, bh.x, bh.y, bh.w, bh.h, Theme.ROW);
                        text(ctx, fit(b.name, bh.w - 40), bh.x + 6, bh.y + 4, b.value ? Theme.TEXT : Theme.MUTED);
                        tick(ctx, bh.x + bh.w - 14, bh.y + 4, b.value ? 1f : 0f, accent);
                    }
                } else if (s instanceof NumSetting n) {
                    Hit nh = hit(sx + 2, sy, sw - 4, SUBROW - 1, true, null, null);
                    nh.slider = n;
                    if (vis) {
                        box(ctx, nh.x, nh.y, nh.w, nh.h, Theme.ROW);
                        float f = (float) ((n.value - n.min) / (n.max - n.min));
                        f = Math.max(0f, Math.min(1f, f));
                        box(ctx, nh.x, nh.y, (int) (nh.w * f), nh.h, UI.alpha(accent, 48));
                        box(ctx, nh.x, nh.y + nh.h - 1, nh.w, 1, Theme.DIVIDER);
                        box(ctx, nh.x, nh.y + nh.h - 1, (int) (nh.w * f), 1, accent);
                        text(ctx, fit(n.name, nh.w - 40), nh.x + 6, nh.y + 3, Theme.TEXT);
                        String v = n.display();
                        text(ctx, v, nh.x + nh.w - 6 - textRenderer.getWidth(v), nh.y + 3, accent);
                    }
                }
                sy += SUBROW;
            }
            if (ext == null) {
                Hit kh = hit(sx + 2, sy, sw - 4, SUBROW - 1, true, () -> binding = m, () -> m.key = -1);
                if (sy + SUBROW > viewTop - 4 && sy < viewBottom) {
                    if (kh.in(mx, my)) box(ctx, kh.x, kh.y, kh.w, kh.h, Theme.ROW);
                    text(ctx, "Keybind", kh.x + 6, kh.y + 4, Theme.MUTED);
                    String kt = binding == m ? "press a key" : (m.key == -1 ? "none" : keyName(m.key));
                    kt = "[" + kt.toUpperCase() + "]";
                    text(ctx, kt, kh.x + kh.w - 6 - textRenderer.getWidth(kt), kh.y + 4, binding == m ? accent : Theme.MUTED);
                }
                sy += SUBROW;
            }
            y = sy + 4;
        }
        return y;
    }

    // ------------------------------------------------------------------ input

    private void slide(Hit h, double mx) {
        double f = Math.max(0, Math.min(1, (mx - h.x) / h.w));
        h.slider.set(h.slider.min + f * (h.slider.max - h.slider.min));
    }

    @Override public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x(), my = click.y();
        int button = click.button();
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (!h.in(mx, my)) continue;
            if (h.content && (my < viewTop - 3 || my >= viewBottom)) continue;
            if (!h.search) searchFocus = false;
            if (h.slider != null) { dragging = h; slide(h, mx); }
            else if (button == 0 && h.left != null) h.left.run();
            else if (button == 1 && h.right != null) h.right.run();
            return true;
        }
        searchFocus = false;
        return super.mouseClicked(click, doubled);
    }

    @Override public boolean mouseDragged(Click click, double dx, double dy) {
        if (dragging != null) { slide(dragging, click.x()); return true; }
        return super.mouseDragged(click, dx, dy);
    }

    @Override public boolean mouseReleased(Click click) {
        dragging = null;
        return super.mouseReleased(click);
    }

    @Override public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        if (my >= viewTop - 3 && my < viewBottom) scrollTarget -= (int) (vertical * 26);
        return true;
    }

    @Override public boolean keyPressed(KeyInput input) {
        int key = input.key();
        if (binding != null) {
            binding.key = key == GLFW.GLFW_KEY_ESCAPE ? -1 : key;
            binding = null;
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE && !query.isEmpty()) { query = ""; searchFocus = false; scrollTarget = 0; return true; }
        if (key == GLFW.GLFW_KEY_BACKSPACE && !query.isEmpty()) {
            query = query.substring(0, query.length() - 1);
            scrollTarget = 0;
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) { searchFocus = false; return true; }
        if (LClient.guiKey != null && LClient.guiKey.matchesKey(input)) { close(); return true; }
        return super.keyPressed(input);
    }

    @Override public boolean charTyped(CharInput input) {
        if (binding != null) return true;
        String s = input.asString();
        if (s.isEmpty() || s.charAt(0) < 32 || query.length() >= 24) return true;
        query += s;          // typing anywhere searches, like a launcher
        searchFocus = true;
        scrollTarget = 0;
        return true;
    }

    @Override public void close() {
        Config.save();
        super.close();
    }
}
