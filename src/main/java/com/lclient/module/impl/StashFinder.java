package com.lclient.module.impl;

import com.lclient.module.*;
import com.lclient.module.Module;
import com.lclient.setting.*;
import com.lclient.util.*;
import net.minecraft.block.entity.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.WorldChunk;

import java.util.*;

/**
 * Logs chunks with lots of chests / hoppers / barrels / shulkers.
 * Trial chambers are filtered out: any chunk within a few chunks of a trial spawner or vault is ignored,
 * since those structures are big and spread containers over several chunks.
 */
public class StashFinder extends Module {
    private final NumSetting threshold = num("Containers per chunk", 8, 3, 60, 1);
    private final NumSetting range = num("Range (chunks)", 10, 2, 32, 1);
    private final NumSetting confirm = num("Confirm scans", 3, 1, 8, 1);
    private final NumSetting trialRadius = num("Trial chamber radius (chunks)", 4, 1, 8, 1);
    private final BoolSetting hoppers = bool("Count hoppers", true);
    private final BoolSetting extras = bool("Count furnaces / dispensers", false);
    private final BoolSetting notify = bool("Chat notification", true);

    private final Set<Long> logged = new HashSet<>();
    private final Set<Long> trial = new HashSet<>();
    private final Map<Long, Integer> pending = new HashMap<>();
    private ClientWorld lastWorld;
    private int ticks;

    public StashFinder() { super("StashFinder", "Logs chunks packed with chests/hoppers (skips trial chambers)", Category.BASEHUNT); }

    private boolean counts(BlockEntity be) {
        if (be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity || be instanceof ShulkerBoxBlockEntity) return true;
        if (be instanceof HopperBlockEntity) return hoppers.value;
        return extras.value && (be instanceof DispenserBlockEntity || be instanceof AbstractFurnaceBlockEntity);
    }

    private boolean nearTrial(int cx, int cz) {
        int r = trialRadius.i();
        for (long l : trial) {
            ChunkPos t = new ChunkPos(l);
            if (Math.abs(t.x - cx) <= r && Math.abs(t.z - cz) <= r) return true;
        }
        return false;
    }

    @Override public void onTick() {
        if (mc.world != lastWorld) { logged.clear(); trial.clear(); pending.clear(); lastWorld = mc.world; }
        if (ticks++ % 40 != 0) return;

        List<WorldChunk> chunks = ChunkScanner.around(range.i());

        // pass 1: find trial chamber chunks
        for (WorldChunk c : chunks)
            for (BlockEntity be : c.getBlockEntities().values())
                if (be instanceof TrialSpawnerBlockEntity || be instanceof VaultBlockEntity) { trial.add(c.getPos().toLong()); break; }

        // drop anything already logged that turned out to be trial chamber
        String dim = Finds.dim();
        Finds.LIST.removeIf(f -> f.dim().equals(dim) && nearTrial(f.x() >> 4, f.z() >> 4));

        // pass 2: count containers
        for (WorldChunk c : chunks) {
            long key = c.getPos().toLong();
            if (nearTrial(c.getPos().x, c.getPos().z)) { pending.remove(key); continue; }
            int count = 0, sx = 0, sz = 0; long sy = 0;
            for (BlockEntity be : c.getBlockEntities().values()) {
                if (!counts(be)) continue;
                BlockPos p = be.getPos();
                count++; sx += p.getX(); sy += p.getY(); sz += p.getZ();
            }
            if (count < threshold.i()) { pending.remove(key); continue; }
            int seen = pending.merge(key, 1, Integer::sum);
            if (seen >= confirm.i() && logged.add(key))
                Finds.add(sx / count, (int) (sy / count), sz / count, count, notify.value);
        }
    }
}
