package com.lclient.module.impl;

import com.lclient.module.*;
import com.lclient.module.Module;
import com.lclient.setting.*;
import com.lclient.util.*;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.vehicle.VehicleInventory;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.*;

import java.util.*;

/**
 * Scores every loaded chunk on things that rarely generate naturally (hoppers, redstone, beds, signs,
 * player-style building blocks, storage blocks, portals, item frames...) and flags chunks over a threshold.
 * Chunks near trial chambers and villages are ignored.
 */
public class SusScanner extends Module {
    private static final int REDSTONE = 0, FARM = 1, BUILD = 2, OBSIDIAN = 3, PORTAL = 4, VALUE = 5, UTILITY = 6,
            CHESTS = 7, HOPPERS = 8, DISPENSERS = 9, BEDS = 10, SIGNS = 11, BANNERS = 12, BEACON = 13,
            SPAWNER = 14, FURNACES = 15, ENTITIES = 16, N = 17;
    private static final String[] NAMES = {"redstone parts", "farmland", "player-style blocks", "obsidian", "portal blocks",
            "storage blocks", "utility blocks", "chests/barrels", "hoppers", "dispensers/droppers", "beds", "signs",
            "banners", "beacons", "spawners", "furnaces", "frames/stands/carts"};
    private static final double[] W = {1.0, 0.3, 0.4, 0.15, 3.0, 2.0, 2.0, 1.0, 1.5, 1.0, 2.0, 1.2, 0.8, 8.0, 1.0, 0.5, 1.0};
    private static final int[] CAP = {8, 10, 20, 12, 3, 6, 5, 14, 10, 6, 4, 5, 4, 1, 2, 8, 10};

    private final NumSetting threshold = num("Score to flag", 14, 5, 60, 1);
    private final NumSetting range = num("Range (chunks)", 10, 2, 32, 1);
    private final NumSetting perTick = num("Chunks per tick", 2, 1, 8, 1);
    private final NumSetting rescan = num("Rescan (sec)", 30, 5, 300, 5);
    private final BoolSetting ignoreTrial = bool("Ignore trial chambers", true);
    private final BoolSetting ignoreVillage = bool("Ignore villages", true);
    private final NumSetting exclRadius = num("Exclusion radius (chunks)", 4, 1, 8, 1);
    private final BoolSetting notify = bool("Chat notification", true);

    private final Map<Block, Integer> cache = new IdentityHashMap<>();
    private final Map<Long, Long> lastScan = new HashMap<>();
    private final ArrayDeque<Long> queue = new ArrayDeque<>();
    private final Set<Long> flagged = new HashSet<>(), trial = new HashSet<>(), village = new HashSet<>();
    private final Map<Long, Integer> entCount = new HashMap<>();
    private ClientWorld lastWorld;
    private int ticks;

    public SusScanner() { super("SusScanner", "Scores chunks for unnatural activity and flags them", Category.BASEHUNT); }

    // ---------- classification ----------
    private int cat(Block b) {
        Integer c = cache.get(b);
        if (c != null) return c;
        String p = Registries.BLOCK.getId(b).getPath();
        int r = -1;
        switch (p) {
            case "redstone_wire", "repeater", "comparator", "observer", "piston", "sticky_piston", "note_block",
                 "redstone_lamp", "target" -> r = REDSTONE;
            case "farmland" -> r = FARM;
            case "obsidian" -> r = OBSIDIAN;
            case "nether_portal" -> r = PORTAL;
            case "diamond_block", "emerald_block", "netherite_block", "iron_block", "lapis_block", "redstone_block" -> r = VALUE;
            case "enchanting_table", "anvil", "chipped_anvil", "damaged_anvil", "conduit", "lodestone", "respawn_anchor" -> r = UTILITY;
            case "glass", "tinted_glass", "glass_pane", "bricks", "quartz_block", "smooth_quartz", "smooth_stone" -> r = BUILD;
            default -> {
                if (p.endsWith("_concrete") || p.endsWith("_stained_glass") || p.endsWith("_stained_glass_pane")) r = BUILD;
            }
        }
        cache.put(b, r);
        return r;
    }

    private int beCat(BlockEntity be) {
        if (be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity || be instanceof ShulkerBoxBlockEntity) return CHESTS;
        if (be instanceof EnderChestBlockEntity) return UTILITY;
        if (be instanceof HopperBlockEntity) return HOPPERS;
        if (be instanceof DispenserBlockEntity) return DISPENSERS;
        if (be instanceof BedBlockEntity) return BEDS;
        if (be instanceof SignBlockEntity) return SIGNS;
        if (be instanceof BannerBlockEntity) return BANNERS;
        if (be instanceof BeaconBlockEntity) return BEACON;
        if (be instanceof MobSpawnerBlockEntity) return SPAWNER;
        if (be instanceof AbstractFurnaceBlockEntity) return FURNACES;
        return -1;
    }

    // ---------- exclusions ----------
    private boolean near(Set<Long> set, int cx, int cz) {
        int r = exclRadius.i();
        for (long l : set) {
            ChunkPos t = new ChunkPos(l);
            if (Math.abs(t.x - cx) <= r && Math.abs(t.z - cz) <= r) return true;
        }
        return false;
    }

    private boolean excluded(int cx, int cz) {
        return (ignoreTrial.value && near(trial, cx, cz)) || (ignoreVillage.value && near(village, cx, cz));
    }

    // ---------- loop ----------
    @Override public void onTick() {
        if (mc.world != lastWorld) {
            lastScan.clear(); queue.clear(); flagged.clear(); trial.clear(); village.clear(); entCount.clear();
            lastWorld = mc.world;
        }
        if (ticks++ % 40 == 0) refresh();
        for (int i = 0; i < perTick.i(); i++) {
            Long k = queue.poll();
            if (k == null) break;
            scan(k);
        }
    }

    private void refresh() {
        List<WorldChunk> chunks = ChunkScanner.around(range.i());

        for (WorldChunk c : chunks)
            for (BlockEntity be : c.getBlockEntities().values()) {
                if (be instanceof TrialSpawnerBlockEntity || be instanceof VaultBlockEntity) trial.add(c.getPos().toLong());
                if (be instanceof BellBlockEntity) village.add(c.getPos().toLong());
            }

        entCount.clear();
        for (Entity e : mc.world.getEntities()) {
            long key = new ChunkPos(e.getBlockPos()).toLong();
            if (e instanceof VillagerEntity) village.add(key);
            else if (e instanceof ItemFrameEntity || e instanceof ArmorStandEntity || e instanceof VehicleInventory)
                entCount.merge(key, 1, Integer::sum);
        }

        String dim = Finds.dim();
        Finds.LIST.removeIf(f -> f.dim().equals(dim) && f.note().startsWith("Sus") && excluded(f.x() >> 4, f.z() >> 4));

        long now = mc.world.getTime(), gap = rescan.i() * 20L;
        for (WorldChunk c : chunks) {
            long key = c.getPos().toLong();
            if (flagged.contains(key)) continue;
            Long last = lastScan.get(key);
            if ((last == null || now - last >= gap) && !queue.contains(key)) queue.add(key);
        }
    }

    private void scan(long key) {
        ChunkPos cp = new ChunkPos(key);
        Chunk ch = mc.world.getChunk(cp.x, cp.z, ChunkStatus.FULL, false);
        if (!(ch instanceof WorldChunk c)) return;
        lastScan.put(key, mc.world.getTime());
        if (excluded(cp.x, cp.z)) return;

        int[] n = new int[N];
        long sx = 0, sy = 0, sz = 0; int hits = 0;

        for (BlockEntity be : c.getBlockEntities().values()) {
            int k = beCat(be);
            if (k < 0) continue;
            n[k]++;
            BlockPos p = be.getPos();
            sx += p.getX(); sy += p.getY(); sz += p.getZ(); hits++;
        }

        ChunkSection[] secs = c.getSectionArray();
        int bottom = c.getBottomSectionCoord();
        for (int i = 0; i < secs.length; i++) {
            ChunkSection s = secs[i];
            if (s == null || !s.hasAny(st -> cat(st.getBlock()) >= 0)) continue;
            int by = (bottom + i) * 16;
            for (int y = 0; y < 16; y++)
                for (int z = 0; z < 16; z++)
                    for (int x = 0; x < 16; x++) {
                        BlockState st = s.getBlockState(x, y, z);
                        int k = cat(st.getBlock());
                        if (k < 0) continue;
                        n[k]++;
                        sx += cp.getStartX() + x; sy += by + y; sz += cp.getStartZ() + z; hits++;
                    }
        }
        n[ENTITIES] = entCount.getOrDefault(key, 0);

        double score = 0;
        List<double[]> parts = new ArrayList<>();
        for (int k = 0; k < N; k++) {
            if (n[k] == 0) continue;
            double pts = Math.min(n[k], CAP[k]) * W[k];
            score += pts;
            parts.add(new double[]{pts, k});
        }
        if (score < threshold.d() || !flagged.add(key)) return;

        parts.sort((a, b) -> Double.compare(b[0], a[0]));
        StringBuilder why = new StringBuilder();
        for (int i = 0; i < Math.min(3, parts.size()); i++) {
            int k = (int) parts.get(i)[1];
            if (i > 0) why.append(", ");
            why.append(n[k]).append(' ').append(NAMES[k]);
        }
        int x = hits > 0 ? (int) (sx / hits) : cp.getCenterX();
        int y = hits > 0 ? (int) (sy / hits) : mc.player.getBlockY();
        int z = hits > 0 ? (int) (sz / hits) : cp.getCenterZ();
        Finds.add(x, y, z, (int) score, "Sus chunk (score " + (int) score + ": " + why + ")", notify.value);
    }
}
