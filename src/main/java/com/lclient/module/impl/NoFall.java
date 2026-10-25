package com.lclient.module.impl;

import com.lclient.module.*;
import com.lclient.module.Module;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

public class NoFall extends Module {
    public NoFall() { super("NoFall", "Cancels fall damage", Category.MOVEMENT); }

    @Override public void onTick() {
        if (mc.player.fallDistance > 2 && !mc.player.isOnGround() && !mc.player.isGliding() && mc.getNetworkHandler() != null)
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(true, mc.player.horizontalCollision));
    }
}
