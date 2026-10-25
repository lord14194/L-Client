package com.lclient.mixin;

import com.lclient.module.ModuleManager;
import com.lclient.module.impl.NewChunks;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onBlockUpdate", at = @At("HEAD"))
    private void lclient$onBlockUpdate(BlockUpdateS2CPacket packet, CallbackInfo ci) {
        NewChunks m = ModuleManager.get(NewChunks.class);
        if (m != null && m.enabled) m.handle(packet.getPos(), packet.getState());
    }
}
