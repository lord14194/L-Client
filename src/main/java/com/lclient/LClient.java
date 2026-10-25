package com.lclient;

import com.lclient.gui.ClickGui;
import com.lclient.module.Config;
import com.lclient.module.ModuleManager;
import com.lclient.util.Render;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class LClient implements ClientModInitializer {
    public static final String NAME = "L Client";
    public static final String MC_VERSION = "1.21.11";

    public static KeyBinding guiKey;

    @Override
    public void onInitializeClient() {
        ModuleManager.init();
        Config.load();

        // Right Ctrl by default: the bundled Meteor Client already owns Right Shift.
        guiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.lclient.gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_CONTROL,
                KeyBinding.Category.create(Identifier.of("lclient", "main"))));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (guiKey.wasPressed()) {
                if (mc.currentScreen == null) mc.setScreen(new ClickGui());
            }
            ModuleManager.tick(mc);
        });

        HudElementRegistry.addLast(Identifier.of("lclient", "hud"), (ctx, tickCounter) -> ModuleManager.hud(ctx));

        // Through-wall ESP: drawn at the end of the main world pass.
        WorldRenderEvents.END_MAIN.register(ctx -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.world == null || mc.player == null || !ModuleManager.anyRender3D()) return;
            Render.begin(ctx.matrices(), ctx.consumers(), ctx.worldState().cameraRenderState.pos);
            ModuleManager.render3D();
            Render.end();
        });

        Runtime.getRuntime().addShutdownHook(new Thread(Config::save));
    }
}
