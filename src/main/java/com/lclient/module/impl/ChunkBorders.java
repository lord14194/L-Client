package com.lclient.module.impl;

import com.lclient.module.*;
import com.lclient.module.Module;
import com.lclient.setting.*;
import com.lclient.util.*;
import net.minecraft.util.math.Box;

public class ChunkBorders extends Module {
    private final NumSetting radius = num("Radius (chunks)", 1, 0, 4, 1);
    private final NumSetting height = num("Height", 24, 8, 96, 4);

    public ChunkBorders() { super("ChunkBorders", "Chunk grid around you", Category.BASEHUNT); }

    @Override public void onRender3D() {
        int pcx = mc.player.getBlockX() >> 4, pcz = mc.player.getBlockZ() >> 4;
        double y = mc.player.getY(), h = height.d();
        int r = radius.i();
        for (int x = pcx - r; x <= pcx + r; x++)
            for (int z = pcz - r; z <= pcz + r; z++) {
                boolean home = x == pcx && z == pcz;
                Render.box(new Box(x * 16, y - h, z * 16, x * 16 + 16, y + h, z * 16 + 16),
                        home ? (Theme.accent & 0xFFFFFF) : 0x5A5A6A, 0);
            }
    }
}
