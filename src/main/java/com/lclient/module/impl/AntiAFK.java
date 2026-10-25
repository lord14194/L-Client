package com.lclient.module.impl;

import com.lclient.module.*;
import com.lclient.module.Module;
import com.lclient.setting.*;
import net.minecraft.util.Hand;

import java.util.Random;

public class AntiAFK extends Module {
    private final NumSetting interval = num("Interval (sec)", 20, 5, 120, 5);
    private final BoolSetting jump = bool("Jump", true);
    private final BoolSetting turn = bool("Turn", true);
    private final BoolSetting swing = bool("Swing", false);
    private final Random rng = new Random();
    private int ticks;

    public AntiAFK() { super("AntiAFK", "Small actions so you don't get kicked", Category.PLAYER); }

    @Override public void onTick() {
        if (++ticks < interval.i() * 20) return;
        ticks = 0;
        if (jump.value && mc.player.isOnGround()) mc.player.jump();
        if (turn.value) mc.player.setYaw(mc.player.getYaw() + (rng.nextFloat() * 40f - 20f));
        if (swing.value) mc.player.swingHand(Hand.MAIN_HAND);
    }
}
