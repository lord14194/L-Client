package com.lclient.module.impl;

import com.lclient.module.*;
import com.lclient.module.Module;
import com.lclient.setting.*;
import com.lclient.util.*;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class Tracers extends Module {
    private final BoolSetting marker = bool("Marker box", true);

    public Tracers() { super("Tracers", "Lines to logged finds", Category.BASEHUNT); }

    @Override public void onRender3D() {
        Vec3d eye = mc.player.getEyePos();
        Vec3d start = eye.add(mc.player.getRotationVec(1f).multiply(0.6));
        String dim = Finds.dim();
        int col = Theme.accent | 0xFF000000;
        for (Finds.Find f : Finds.LIST) {
            if (!f.dim().equals(dim)) continue;
            Vec3d t = new Vec3d(f.x() + 0.5, f.y() + 0.5, f.z() + 0.5);
            Render.line(start, t, col);
            if (marker.value) Render.box(new Box(t.x - 0.5, t.y - 0.5, t.z - 0.5, t.x + 0.5, t.y + 0.5, t.z + 0.5), col, 50);
        }
    }
}
