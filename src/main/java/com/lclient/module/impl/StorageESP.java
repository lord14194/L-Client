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

public class StorageESP extends Module {
    private final NumSetting range = num("Range (chunks)", 8, 2, 32, 1);
    private final BoolSetting chests = bool("Chests", true);
    private final BoolSetting shulkers = bool("Shulkers", true);
    private final BoolSetting barrels = bool("Barrels", true);
    private final BoolSetting enderChests = bool("Ender chests", false);
    private final BoolSetting furnaces = bool("Furnaces", false);
    private final BoolSetting misc = bool("Hoppers / dispensers", false);
    private final BoolSetting fill = bool("Fill", true);

    private final List<BlockEntity> cache = new ArrayList<>();
    private int ticks;

    public StorageESP() { super("StorageESP", "Highlights containers through walls", Category.BASEHUNT); }

    @Override public void onTick() {
        if (ticks++ % 10 != 0) return;
        cache.clear();
        for (WorldChunk c : ChunkScanner.around(range.i()))
            for (BlockEntity be : c.getBlockEntities().values())
                if (color(be) != 0) cache.add(be);
    }

    private int color(BlockEntity be) {
        if (be instanceof TrappedChestBlockEntity) return chests.value ? 0xE5484D : 0;
        if (be instanceof ChestBlockEntity) return chests.value ? 0xFFA500 : 0;
        if (be instanceof ShulkerBoxBlockEntity) return shulkers.value ? 0xE255E2 : 0;
        if (be instanceof BarrelBlockEntity) return barrels.value ? 0xC8963C : 0;
        if (be instanceof EnderChestBlockEntity) return enderChests.value ? 0x8B5CF6 : 0;
        if (be instanceof AbstractFurnaceBlockEntity) return furnaces.value ? 0xB0B0B8 : 0;
        if (be instanceof HopperBlockEntity || be instanceof DispenserBlockEntity || be instanceof BrewingStandBlockEntity)
            return misc.value ? 0x3DD6D0 : 0;
        return 0;
    }

    @Override public void onRender3D() {
        for (BlockEntity be : cache) {
            if (be.isRemoved()) continue;
            Render.box(new Box(be.getPos()).contract(0.02), color(be), fill.value ? 45 : 0);
        }
    }
}
