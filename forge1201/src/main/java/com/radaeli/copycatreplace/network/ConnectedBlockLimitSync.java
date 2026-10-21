package com.radaeli.copycatreplace.network;

import com.radaeli.copycatreplace.config.CopycatReplaceConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraftforge.event.entity.player.PlayerEvent;

/** Distributes the server's common action limit to clients for accurate previews. */
public final class ConnectedBlockLimitSync {
    private ConnectedBlockLimitSync() {
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sendToPlayer(player);
        }
    }

    public static void onConfigReloading(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() != CopycatReplaceConfig.SPEC) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            server.execute(() -> server.getPlayerList().getPlayers().forEach(ConnectedBlockLimitSync::sendToPlayer));
        }
    }

    private static void sendToPlayer(ServerPlayer player) {
        ModNetworking.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new ConnectedBlockLimitPayload(CopycatReplaceConfig.MAX_CONNECTED_BLOCKS.get())
        );
    }
}
