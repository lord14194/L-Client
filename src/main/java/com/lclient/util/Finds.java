package com.lclient.util;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Logged finds. Kept in memory and appended to .minecraft/L Client/finds.txt */
public final class Finds {
    public record Find(int x, int y, int z, int count, String dim, String note) {}
    public static final List<Find> LIST = new ArrayList<>();
    private Finds() {}

    public static String dim() {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc.world == null ? "?" : mc.world.getRegistryKey().getValue().toString();
    }

    public static void add(int x, int y, int z, int count, boolean chat) {
        add(x, y, z, count, "Stash? " + count + " containers", chat);
    }

    public static void add(int x, int y, int z, int count, String note, boolean chat) {
        Find f = new Find(x, y, z, count, dim(), note);
        LIST.add(f);
        try {
            Path dir = FabricLoader.getInstance().getGameDir().resolve("L Client");
            Files.createDirectories(dir);
            String line = LocalDateTime.now() + " | " + f.dim() + " | " + x + " " + y + " " + z + " | " + note + "\n";
            Files.writeString(dir.resolve("finds.txt"), line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception ignored) {}
        MinecraftClient mc = MinecraftClient.getInstance();
        if (chat && mc.player != null)
            mc.player.sendMessage(Text.literal("§d[L] §f" + note + " §7at §f" + x + " " + y + " " + z), false);
    }
}
