package com.lclient.integration;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;

/**
 * Bridges the three bundled mods into the L Client GUI using reflection.
 * That keeps L Client compiling on its own and means a changed internal API in one of the
 * bundled mods can only hide that mod's rows - it can never crash L Client.
 */
public final class Integrations {
    public static final String METEOR = "Meteor", DUPERS = "DupersUnited", UIUTILS = "UI Utils";

    private static final Map<String, ExternalModule> CACHE = new LinkedHashMap<>();
    private static final Set<String> LOGGED = new HashSet<>();

    private Integrations() {}

    public static boolean present(String group) {
        return switch (group) {
            case METEOR -> FabricLoader.getInstance().isModLoaded("meteor-client");
            case DUPERS -> FabricLoader.getInstance().isModLoaded("dupersunited");
            case UIUTILS -> FabricLoader.getInstance().isModLoaded("ui_utils");
            default -> false;
        };
    }

    public static List<String> groups() {
        List<String> out = new ArrayList<>();
        for (String g : new String[]{METEOR, DUPERS, UIUTILS}) if (present(g)) out.add(g);
        return out;
    }

    /** Re-read every bundled mod's module list. Called whenever the GUI opens. */
    public static void refresh() {
        Set<String> alive = new HashSet<>();
        if (present(METEOR)) guarded(METEOR, () -> meteor(alive));
        if (present(DUPERS)) guarded(DUPERS, () -> dupers(alive));
        if (present(UIUTILS)) guarded(UIUTILS, () -> uiUtils(alive));
        CACHE.keySet().retainAll(alive);
    }

    public static List<ExternalModule> group(String group) {
        List<ExternalModule> out = new ArrayList<>();
        for (ExternalModule m : CACHE.values()) if (m.group.equals(group)) out.add(m);
        out.sort((a, b) -> {
            // launchers first, then by page, then by name
            if (a.isAction() != b.isAction()) return a.isAction() ? -1 : 1;
            int c = a.page.compareToIgnoreCase(b.page);
            return c != 0 ? c : a.name.compareToIgnoreCase(b.name);
        });
        return out;
    }

    @FunctionalInterface
    private interface ThrowingRunnable { void run() throws Exception; }

    private static void guarded(String group, ThrowingRunnable r) {
        try { r.run(); }
        catch (Throwable t) {
            if (LOGGED.add(group)) {
                System.err.println("[L Client] Could not read modules from " + group + ": " + t);
                t.printStackTrace();
            }
        }
    }

    private static Class<?> cls(String name) throws ClassNotFoundException {
        return Class.forName(name, true, Integrations.class.getClassLoader());
    }

    private static ExternalModule keep(Set<String> alive, String key, java.util.function.Supplier<ExternalModule> make) {
        alive.add(key);
        return CACHE.computeIfAbsent(key, k -> make.get());
    }

    private static void open(Screen parent, String what, java.util.concurrent.Callable<Screen> make) {
        MinecraftClient mc = MinecraftClient.getInstance();
        try { Screen s = make.call(); if (s != null) mc.setScreen(s); }
        catch (Throwable t) {
            System.err.println("[L Client] Could not open " + what + ": " + t);
            t.printStackTrace();
            if (mc.player != null) mc.player.sendMessage(net.minecraft.text.Text.literal("\u00a7c[L] Could not open " + what + " (see log)"), false);
            mc.setScreen(parent);
        }
    }

    private static String str(Object v) { return v == null ? "" : String.valueOf(v); }

    // ---------------------------------------------------------------- Meteor Client

    private static void meteor(Set<String> alive) throws Exception {
        String base = "meteordevelopment.meteorclient.";
        Class<?> modulesC = cls(base + "systems.modules.Modules");
        Class<?> moduleC = cls(base + "systems.modules.Module");
        Class<?> categoryC = cls(base + "systems.modules.Category");
        Field fName = moduleC.getField("name"), fTitle = moduleC.getField("title"),
              fDesc = moduleC.getField("description"), fCat = moduleC.getField("category");
        Field fCatName = categoryC.getField("name");
        Method isActive = moduleC.getMethod("isActive"), toggle = moduleC.getMethod("toggle");

        // launchers for Meteor's own screens (Modules, Config, GUI, HUD, Friends, Macros, Profiles)
        try {
            Class<?> tabsC = cls(base + "gui.tabs.Tabs"), tabC = cls(base + "gui.tabs.Tab");
            Class<?> themesC = cls(base + "gui.GuiThemes"), themeC = cls(base + "gui.GuiTheme");
            Method tabsGet = tabsC.getMethod("get"), themeGet = themesC.getMethod("get");
            Method openScreen = tabC.getMethod("openScreen", themeC);
            Field tabName = tabC.getField("name");
            List<?> tabs = (List<?>) tabsGet.invoke(null);
            for (Object tab : new ArrayList<>(tabs)) {
                String n = str(tabName.get(tab));
                final Object t = tab;
                keep(alive, "meteor:tab:" + n, () -> new ExternalModule(METEOR, "Meteor screens", "Meteor " + n,
                        "Open Meteor Client's " + n + " screen",
                        (Consumer<Screen>) parent -> open(parent, "Meteor " + n, () -> {
                            openScreen.invoke(t, themeGet.invoke(null)); // Meteor opens its own screen
                            return null;
                        })));
            }
        } catch (Throwable t) {
            if (LOGGED.add("meteor-tabs")) System.err.println("[L Client] Meteor screen launchers unavailable: " + t);
        }

        Object modules = modulesC.getMethod("get").invoke(null);
        Collection<?> all = (Collection<?>) modulesC.getMethod("getAll").invoke(modules);
        for (Object mod : all) {
            String name = str(fName.get(mod));
            String title = str(fTitle.get(mod));
            String page = str(fCatName.get(fCat.get(mod)));
            String desc = str(fDesc.get(mod));
            final Object m = mod;
            keep(alive, "meteor:" + name, () -> new ExternalModule(METEOR, page, title.isEmpty() ? name : title, desc,
                    () -> { try { return (boolean) isActive.invoke(m); } catch (Exception e) { return false; } },
                    () -> { try { toggle.invoke(m); } catch (Exception e) { throw new RuntimeException(e); } }));
        }
    }

    // ---------------------------------------------------------------- DupersUnited

    private static void dupers(Set<String> alive) throws Exception {
        String base = "wtf.dupers.dupersunited.";
        Class<?> mainC = cls(base + "MainClient");
        Class<?> moduleC = cls(base + "api.module.Module");
        Method getName = moduleC.getMethod("getName"), getCat = moduleC.getMethod("getCategory"),
               getDesc = moduleC.getMethod("getDescription"), isEnabled = moduleC.getMethod("isEnabled"),
               toggle = moduleC.getMethod("toggle");

        Class<?> guiC = cls(base + "features.screens.ClickGui");
        Class<?> menuC = cls(base + "features.screens.DupersUnitedScreen");
        Constructor<?> guiCtor = guiC.getConstructor(Screen.class), menuCtor = menuC.getConstructor(Screen.class);
        keep(alive, "dupers:gui", () -> new ExternalModule(DUPERS, "Dupers screens", "Dupers ClickGui",
                "Open DupersUnited's own module GUI",
                (Consumer<Screen>) parent -> open(parent, "Dupers ClickGui", () -> (Screen) guiCtor.newInstance(parent))));
        keep(alive, "dupers:menu", () -> new ExternalModule(DUPERS, "Dupers screens", "Dupers Menu",
                "Open the DupersUnited main menu",
                (Consumer<Screen>) parent -> open(parent, "Dupers Menu", () -> (Screen) menuCtor.newInstance(parent))));

        Collection<?> mods = (Collection<?>) mainC.getMethod("getModules").invoke(null);
        if (mods == null) return;
        for (Object mod : mods) {
            String name = str(getName.invoke(mod));
            String page = str(getCat.invoke(mod));
            String desc = str(getDesc.invoke(mod));
            final Object m = mod;
            keep(alive, "dupers:" + name, () -> new ExternalModule(DUPERS, page.isEmpty() ? "misc" : page, name, desc,
                    () -> { try { return (boolean) isEnabled.invoke(m); } catch (Exception e) { return false; } },
                    () -> { try { toggle.invoke(m); } catch (Exception e) { throw new RuntimeException(e); } }));
        }
    }

    // ---------------------------------------------------------------- UI Utils

    private static void uiUtils(Set<String> alive) throws Exception {
        Class<?> cfgC = cls("com.mrbreaknfix.ui_utils.gui.screen.UiUtilsConfigScreen");
        Constructor<?> ctor = cfgC.getConstructor(Screen.class);
        keep(alive, "uiutils:config", () -> new ExternalModule(UIUTILS, "UI Utils", "UI Utils Settings",
                "Open UI Utils' settings (theme, overlay, command server)",
                (Consumer<Screen>) parent -> open(parent, "UI Utils settings", () -> (Screen) ctor.newInstance(parent))));
    }
}
