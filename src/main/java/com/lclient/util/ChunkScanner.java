package com.lclient.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.List;

public final class ChunkScanner {
    private ChunkScanner() {}

    public static List<WorldChunk> around(int radius) {
        MinecraftClient mc = MinecraftClient.getInstance();
        List<WorldChunk> out = new ArrayList<>();
        if (mc.world == null || mc.player == null) return out;
        int pcx = mc.player.getBlockX() >> 4, pcz = mc.player.getBlockZ() >> 4;
        for (int x = pcx - radius; x <= pcx + radius; x++) {
            for (int z = pcz - radius; z <= pcz + radius; z++) {
                Chunk c = mc.world.getChunk(x, z, ChunkStatus.FULL, false);
                if (c instanceof WorldChunk wc) out.add(wc);
            }
        }
        return out;
    }
}
