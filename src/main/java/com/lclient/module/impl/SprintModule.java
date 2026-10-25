package com.lclient.module.impl;

import com.lclient.module.*;
import com.lclient.module.Module;

public class SprintModule extends Module {
    public SprintModule() { super("Sprint", "Always sprint while moving forward", Category.MOVEMENT); }

    @Override public void onTick() {
        if (mc.options.forwardKey.isPressed() && !mc.player.isSneaking() && !mc.player.horizontalCollision
                && mc.player.getHungerManager().getFoodLevel() > 6)
            mc.player.setSprinting(true);
    }
}
