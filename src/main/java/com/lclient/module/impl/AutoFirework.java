package com.lclient.module.impl;

import com.lclient.module.*;
import com.lclient.module.Module;
import com.lclient.setting.*;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class AutoFirework extends Module {
    private final NumSetting minSpeed = num("Min speed (b/s)", 18, 5, 40, 1);
    private final NumSetting cooldown = num("Cooldown (ticks)", 40, 10, 120, 5);
    private int cd, restore = -1;

    public AutoFirework() { super("AutoFirework", "Boosts when elytra gets slow", Category.MOVEMENT); }

    @Override public void onTick() {
        var inv = mc.player.getInventory();
        if (restore != -1) { inv.setSelectedSlot(restore); restore = -1; return; }
        if (cd > 0) { cd--; return; }
        if (!mc.player.isGliding()) return;
        double bps = mc.player.getVelocity().horizontalLength() * 20;
        if (bps >= minSpeed.d()) return;

        if (mc.player.getOffHandStack().isOf(Items.FIREWORK_ROCKET)) {
            mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
            cd = cooldown.i();
            return;
        }
        for (int i = 0; i < 9; i++) {
            if (inv.getStack(i).isOf(Items.FIREWORK_ROCKET)) {
                restore = inv.getSelectedSlot();
                inv.setSelectedSlot(i);
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                cd = cooldown.i();
                return;
            }
        }
    }
}
