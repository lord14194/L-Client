package com.lclient.module.impl;

import com.lclient.module.*;
import com.lclient.module.Module;
import com.lclient.setting.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

public class AutoLog extends Module {
    private final NumSetting health = num("Health (hearts)", 5, 1, 19, 1);
    private final BoolSetting onPlayer = bool("Log on player nearby", false);
    private final NumSetting dist = num("Player distance", 64, 8, 256, 8);

    public AutoLog() { super("AutoLog", "Disconnects when in danger", Category.PLAYER); }

    private void bail(String why) {
        setEnabled(false);
        mc.player.networkHandler.getConnection().disconnect(Text.literal("[L] AutoLog: " + why));
    }

    @Override public void onTick() {
        if (mc.player.getHealth() <= health.d() * 2) { bail("low health"); return; }
        if (onPlayer.value) {
            for (PlayerEntity p : mc.world.getPlayers())
                if (p != mc.player && p.distanceTo(mc.player) <= dist.d()) { bail(p.getName().getString() + " nearby"); return; }
        }
    }
}
