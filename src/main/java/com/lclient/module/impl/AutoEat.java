package com.lclient.module.impl;

import com.lclient.module.*;
import com.lclient.module.Module;
import com.lclient.setting.*;
import net.minecraft.component.DataComponentTypes;

public class AutoEat extends Module {
    private final NumSetting threshold = num("Eat below (hunger)", 14, 4, 19, 1);
    private boolean eating;
    private int prev = -1;

    public AutoEat() { super("AutoEat", "Eats food from your hotbar", Category.PLAYER); }

    @Override public void onDisable() { stop(); }

    private void stop() {
        if (mc.options != null) mc.options.useKey.setPressed(false);
        if (eating && prev != -1 && mc.player != null) mc.player.getInventory().setSelectedSlot(prev);
        eating = false; prev = -1;
    }

    @Override public void onTick() {
        var inv = mc.player.getInventory();
        int food = mc.player.getHungerManager().getFoodLevel();
        if (eating) {
            if (food >= 20 || !mc.player.getMainHandStack().contains(DataComponentTypes.FOOD)) stop();
            else mc.options.useKey.setPressed(true);
            return;
        }
        if (food > threshold.i() || mc.currentScreen != null) return;
        for (int i = 0; i < 9; i++) {
            if (inv.getStack(i).contains(DataComponentTypes.FOOD)) {
                prev = inv.getSelectedSlot();
                inv.setSelectedSlot(i);
                eating = true;
                return;
            }
        }
    }
}
