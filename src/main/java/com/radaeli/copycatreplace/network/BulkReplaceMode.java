package com.radaeli.copycatreplace.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-side view of each player's momentary bulk-replacement key state. */
@EventBusSubscriber(modid = "copycat_replace")
public final class BulkReplaceMode {
    private static final Set<UUID> ACTIVE_PLAYERS = ConcurrentHashMap.newKeySet();

    private BulkReplaceMode() {
    }

    public static void setDown(Player player, boolean down) {
        if (player instanceof ServerPlayer serverPlayer) {
            if (down) {
                ACTIVE_PLAYERS.add(serverPlayer.getUUID());
            } else {
                ACTIVE_PLAYERS.remove(serverPlayer.getUUID());
            }
        }
    }

    public static boolean isDown(Player player) {
        return player instanceof ServerPlayer && ACTIVE_PLAYERS.contains(player.getUUID());
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ACTIVE_PLAYERS.remove(event.getEntity().getUUID());
    }
}
