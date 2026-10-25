package com.lclient.module.impl;

import com.lclient.module.*;
import com.lclient.module.Module;
import com.lclient.setting.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PlayerAlert extends Module {
    private final BoolSetting coords = bool("Show coordinates", true);
    private final Set<UUID> seen = new HashSet<>();
    private int ticks;

    public PlayerAlert() { super("PlayerAlert", "Chat alert when a player shows up", Category.PLAYER); }

    @Override public void onDisable() { seen.clear(); }

    @Override public void onTick() {
        if (ticks++ % 10 != 0) return;
        Set<UUID> now = new HashSet<>();
        for (PlayerEntity p : mc.world.getPlayers()) {
            if (p == mc.player) continue;
            now.add(p.getUuid());
            if (seen.add(p.getUuid())) {
                String at = coords.value ? " §7at §f" + p.getBlockX() + " " + p.getBlockY() + " " + p.getBlockZ() : "";
                mc.player.sendMessage(Text.literal("§d[L] §f" + p.getName().getString() + " §7entered range" + at), false);
            }
        }
        seen.retainAll(now);
    }
}
