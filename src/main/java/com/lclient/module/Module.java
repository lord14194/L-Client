package com.lclient.module;

import com.lclient.setting.*;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    public final String name, description;
    public final Category category;
    public boolean enabled;
    public int key = -1;
    public final List<Setting<?>> settings = new ArrayList<>();
    protected final MinecraftClient mc = MinecraftClient.getInstance();

    // GUI / HUD animation state
    public float anim, hudAnim;
    public boolean expanded, wasDown;

    protected Module(String name, String description, Category category) {
        this.name = name; this.description = description; this.category = category;
    }

    protected BoolSetting bool(String n, boolean def) { BoolSetting s = new BoolSetting(n, def); settings.add(s); return s; }
    protected NumSetting num(String n, double def, double min, double max, double step) {
        NumSetting s = new NumSetting(n, def, min, max, step); settings.add(s); return s;
    }

    public void toggle() { setEnabled(!enabled); }
    public void setEnabled(boolean on) {
        if (on == enabled) return;
        enabled = on;
        if (on) onEnable(); else onDisable();
    }

    public void onEnable() {}
    public void onDisable() {}
    public void onTick() {}
    public void onRender3D() {}
}
