package com.lclient.module.impl;

import com.lclient.module.*;
import com.lclient.module.Module;
import com.lclient.setting.*;
import com.lclient.util.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.VehicleInventory;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class EntityESP extends Module {
    private final BoolSetting players = bool("Players", true);
    private final BoolSetting storage = bool("Storage carts / boats", true);
    private final BoolSetting frames = bool("Item frames", true);
    private final BoolSetting stands = bool("Armor stands", false);
    private final NumSetting range = num("Range (blocks)", 160, 16, 512, 8);

    public EntityESP() { super("EntityESP", "Players, storage carts and frames", Category.BASEHUNT); }

    private int color(Entity e) {
        if (e instanceof PlayerEntity p) return (players.value && p != mc.player) ? 0xFFFFFF : 0;
        if (e instanceof VehicleInventory) return storage.value ? 0xFFA500 : 0;
        if (e instanceof ItemFrameEntity) return frames.value ? 0x6BD6FF : 0;
        if (e instanceof ArmorStandEntity) return stands.value ? 0xB0B0B8 : 0;
        return 0;
    }

    @Override public void onRender3D() {
        float td = mc.getRenderTickCounter().getTickProgress(true);
        double r2 = range.d() * range.d();
        for (Entity e : mc.world.getEntities()) {
            int c = color(e);
            if (c == 0 || e.squaredDistanceTo(mc.player) > r2) continue;
            Vec3d off = e.getLerpedPos(td).subtract(e.getEntityPos());
            Box b = e.getBoundingBox().offset(off);
            Render.box(b, c, e instanceof PlayerEntity ? 30 : 0);
        }
    }
}
