package com.lclient.module.impl;

import com.lclient.module.*;
import com.lclient.module.Module;

public class AutoWalk extends Module {
    public AutoWalk() { super("AutoWalk", "Holds forward for you", Category.MOVEMENT); }

    @Override public void onTick() { mc.options.forwardKey.setPressed(true); }
    @Override public void onDisable() { mc.options.forwardKey.setPressed(false); }
}
