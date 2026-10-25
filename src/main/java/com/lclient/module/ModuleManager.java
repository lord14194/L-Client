package com.lclient.module;

import com.lclient.module.impl.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.InputUtil;

import java.util.ArrayList;
import java.util.List;

public final class ModuleManager {
    public static final List<Module> ALL = new ArrayList<>();
    private ModuleManager() {}

    public static void init() {
        // Basehunt
        ALL.add(new StorageESP());
        ALL.add(new BaseESP());
        ALL.add(new EntityESP());
        ALL.add(new StashFinder());
        ALL.add(new SusScanner());
        ALL.add(new NewChunks());
        ALL.add(new Tracers());
        ALL.add(new ChunkBorders());
        // Movement
        ALL.add(new ElytraFly());
        ALL.add(new AutoFirework());
        ALL.add(new AutoWalk());
        ALL.add(new SprintModule());
        ALL.add(new NoFall());
        // Player
        ALL.add(new AutoLog());
        ALL.add(new PlayerAlert());
        ALL.add(new AutoEat());
        ALL.add(new AntiAFK());
        // Visual
        ALL.add(new Fullbright());
        ALL.add(new Hud());
    }

    @SuppressWarnings("unchecked")
    public static <T extends Module> T get(Class<T> c) {
        for (Module m : ALL) if (c.isInstance(m)) return (T) m;
        return null;
    }

    public static List<Module> in(Category c) {
        List<Module> out = new ArrayList<>();
        for (Module m : ALL) if (m.category == c) out.add(m);
        return out;
    }

    public static void tick(MinecraftClient mc) {
        for (Module m : ALL) {
            if (m.key != -1 && mc.currentScreen == null) {
                boolean down = InputUtil.isKeyPressed(mc.getWindow(), m.key);
                if (down && !m.wasDown) m.toggle();
                m.wasDown = down;
            }
            if (m.enabled && mc.player != null && mc.world != null) {
                try { m.onTick(); }
                catch (Throwable t) { fail(m, t); }
            }
        }
    }

    public static boolean anyRender3D() {
        for (Module m : ALL) if (m.enabled) return true;
        return false;
    }

    public static void render3D() {
        for (Module m : ALL) {
            if (!m.enabled) continue;
            try { m.onRender3D(); }
            catch (Throwable t) { fail(m, t); }
        }
    }

    /** A crashing module is switched off instead of taking the game down. */
    private static void fail(Module m, Throwable t) {
        System.err.println("[L Client] " + m.name + " crashed and was disabled: " + t);
        t.printStackTrace();
        m.enabled = false;
    }

    public static void hud(DrawContext ctx) {
        Hud h = get(Hud.class);
        if (h != null && h.enabled) h.render(ctx);
    }
}
