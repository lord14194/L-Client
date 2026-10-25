package com.lclient.module.impl;

import com.lclient.module.*;
import com.lclient.module.Module;
import net.minecraft.entity.effect.*;

public class Fullbright extends Module {
    public Fullbright() { super("Fullbright", "Client-side night vision", Category.VISUAL); }

    @Override public void onTick() {
        mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 1000, 0, false, false, false));
    }
    @Override public void onDisable() {
        if (mc.player != null) mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
    }
}
