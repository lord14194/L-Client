package com.lclient.module.impl;

import com.lclient.module.*;
import com.lclient.module.Module;
import com.lclient.setting.*;
import com.lclient.util.*;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.*;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Flags chunks as freshly generated when a flowing-fluid update arrives for them.
 * Trails of "old" gaps inside new terrain are where players have been.
 * Heuristic: player-placed flowing water in old chunks can cause false positives.
 */
public class NewChunks extends Module {
    private final Set<Long> fresh = ConcurrentHashMap.newKeySet();
    private final NumSetting range = num("Range (chunks)", 24, 4, 64, 1);
    private final BoolSetting fill = bool("Fill", true);
    private final NumSetting yOffset = num("Height offset", 0, -64, 64, 1);

    public NewChunks() { super("NewChunks", "Marks freshly generated chunks", Category.BASEHUNT); }

    @Override public void onDisable() { fresh.clear(); }

    /** Called from the packet mixin. */
    public void handle(BlockPos pos, BlockState state) {
        FluidState fs = state.getFluidState();
        if (!fs.isEmpty() && !fs.isStill()) {
            if (fresh.size() > 30000) fresh.clear();
            fresh.add(new ChunkPos(pos).toLong());
        }
    }

    @Override public void onRender3D() {
        int pcx = mc.player.getBlockX() >> 4, pcz = mc.player.getBlockZ() >> 4;
        double y = mc.player.getY() + yOffset.d();
        for (long l : fresh) {
            ChunkPos cp = new ChunkPos(l);
            if (Math.abs(cp.x - pcx) > range.i() || Math.abs(cp.z - pcz) > range.i()) continue;
            Render.box(new Box(cp.getStartX(), y, cp.getStartZ(), cp.getStartX() + 16, y + 0.05, cp.getStartZ() + 16),
                    0xFF6B35, fill.value ? 40 : 0);
        }
    }
}
