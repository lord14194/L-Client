package com.lclient.integration;

import com.lclient.module.Category;
import com.lclient.module.Module;
import net.minecraft.client.gui.screen.Screen;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * A row in the L Client GUI that stands for something inside a bundled mod
 * (Meteor Client, DupersUnited, UI Utils): either a real module you can toggle,
 * or a launcher button that opens that mod's own screen.
 * These are NOT part of ModuleManager.ALL, so they never touch lclient.json.
 */
public final class ExternalModule extends Module {
    public final String group, page;
    private final BooleanSupplier state;
    private final Runnable toggler;
    public final Consumer<Screen> action;

    /** Real module from a bundled mod. */
    public ExternalModule(String group, String page, String name, String description, BooleanSupplier state, Runnable toggler) {
        super(name, description == null ? "" : description, Category.EXTERNAL);
        this.group = group; this.page = page; this.state = state; this.toggler = toggler; this.action = null;
    }

    /** Launcher button that opens another mod's screen. */
    public ExternalModule(String group, String page, String name, String description, Consumer<Screen> action) {
        super(name, description == null ? "" : description, Category.EXTERNAL);
        this.group = group; this.page = page; this.state = () -> false; this.toggler = () -> {}; this.action = action;
    }

    public boolean isAction() { return action != null; }

    /** Pull the real state of the underlying module into {@link #enabled}. */
    public void sync() {
        if (action != null) { enabled = false; return; }
        try { enabled = state.getAsBoolean(); } catch (Throwable ignored) {}
    }

    @Override public void toggle() {
        if (action != null) return;
        try { toggler.run(); } catch (Throwable t) { System.err.println("[L Client] toggle failed for " + name + ": " + t); }
        sync();
    }

    @Override public void setEnabled(boolean on) { sync(); if (on != enabled) toggle(); }
}
