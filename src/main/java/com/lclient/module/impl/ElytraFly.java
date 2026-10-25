package com.lclient.module.impl;

import com.lclient.module.*;
import com.lclient.module.Module;
import com.lclient.setting.*;
import net.minecraft.util.math.Vec3d;

public class ElytraFly extends Module {
    private final NumSetting speed = num("Speed", 1.5, 0.5, 4.0, 0.1);
    private final NumSetting vertical = num("Vertical speed", 0.6, 0.1, 2.0, 0.1);
    private final BoolSetting hover = bool("Hover when idle", true);

    public ElytraFly() { super("ElytraFly", "Controlled elytra flight", Category.MOVEMENT); }

    @Override public void onTick() {
        if (!mc.player.isGliding()) return;
        boolean fwd = mc.options.forwardKey.isPressed();
        double vy = 0;
        if (mc.options.jumpKey.isPressed()) vy += vertical.d();
        if (mc.options.sneakKey.isPressed()) vy -= vertical.d();
        Vec3d look = mc.player.getRotationVec(1f);
        if (fwd) {
            Vec3d flat = new Vec3d(look.x, 0, look.z).normalize().multiply(speed.d());
            mc.player.setVelocity(flat.x, vy != 0 ? vy : look.y * speed.d(), flat.z);
        } else if (hover.value) {
            mc.player.setVelocity(0, vy, 0);
        }
    }
}
