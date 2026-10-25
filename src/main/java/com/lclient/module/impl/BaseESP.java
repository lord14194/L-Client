package com.lclient.module.impl;

import com.lclient.module.*;
import com.lclient.module.Module;
import com.lclient.setting.*;
import com.lclient.util.*;
import net.minecraft.block.entity.*;
import net.minecraft.util.math.Box;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.List;

/** Player-base giveaways: beds, signs, banners, beacons, spawners. */
public class BaseESP extends Module {
    private final NumSetting range = num("Range (chunks)", 8, 2, 32, 1);
    private final BoolSetting beds = bool("Beds", true);
    private final BoolSetting signs = bool("Signs", true);
    private final BoolSetting banners = bool("Banners", true);
    private final BoolSetting beacons = bool("Beacons", true);
    private final BoolSetting spawners = bool("Spawners", true);
    private final BoolSetting fill = bool("Fill", true);

    private final List<BlockEntity> cache = new ArrayList<>();
    private int ticks;

    public BaseESP() { super("BaseESP", "Beds, signs, banners, beacons, spawners", Category.BASEHUNT); }

    private int color(BlockEntity be) {
        if (be instanceof BedBlockEntity) return beds.value ? 0xFF4D6D : 0;
        if (be instanceof SignBlockEntity) return signs.value ? 0xF2E394 : 0;
        if (be instanceof BannerBlockEntity) return banners.value ? 0x4DA3FF : 0;
        if (be instanceof BeaconBlockEntity) return beacons.value ? 0x5CFFB0 : 0;
        if (be instanceof MobSpawnerBlockEntity) return spawners.value ? 0xB14DFF : 0;
        return 0;
    }

    @Override public void onTick() {
        if (ticks++ % 20 != 0) return;
        cache.clear();
        for (WorldChunk c : ChunkScanner.around(range.i()))
            for (BlockEntity be : c.getBlockEntities().values())
                if (color(be) != 0) cache.add(be);
    }

    @Override public void onRender3D() {
        for (BlockEntity be : cache) {
            if (be.isRemoved()) continue;
            Render.box(new Box(be.getPos()).contract(0.02), color(be), fill.value ? 45 : 0);
        }
    }
}
