package com.lclient.module;

import com.google.gson.*;
import com.lclient.setting.*;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.*;

public final class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private Config() {}

    private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("L Client.json"); }

    public static void save() {
        try {
            JsonObject root = new JsonObject();
            for (Module m : ModuleManager.ALL) {
                JsonObject o = new JsonObject();
                o.addProperty("enabled", m.enabled);
                o.addProperty("key", m.key);
                JsonObject s = new JsonObject();
                for (Setting<?> st : m.settings) {
                    if (st instanceof BoolSetting b) s.addProperty(b.name, b.value);
                    else if (st instanceof NumSetting n) s.addProperty(n.name, n.value);
                }
                o.add("settings", s);
                root.add(m.name, o);
            }
            Files.writeString(file(), GSON.toJson(root));
        } catch (Exception ignored) {}
    }

    public static void load() {
        try {
            Path src = file();
            if (!Files.exists(src)) return;
            JsonObject root = JsonParser.parseString(Files.readString(src)).getAsJsonObject();
            for (Module m : ModuleManager.ALL) {
                if (!root.has(m.name)) continue;
                JsonObject o = root.getAsJsonObject(m.name);
                if (o.has("key")) m.key = o.get("key").getAsInt();
                if (o.has("settings")) {
                    JsonObject s = o.getAsJsonObject("settings");
                    for (Setting<?> st : m.settings) {
                        if (!s.has(st.name)) continue;
                        if (st instanceof BoolSetting b) b.value = s.get(st.name).getAsBoolean();
                        else if (st instanceof NumSetting n) n.set(s.get(st.name).getAsDouble());
                    }
                }
                if (o.has("enabled")) m.setEnabled(o.get("enabled").getAsBoolean());
            }
        } catch (Exception ignored) {}
    }
}
