package com.radaeli.copycatreplace.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.radaeli.copycatreplace.CopycatReplace;
import com.radaeli.copycatreplace.network.BulkReplaceKeyStatePayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** Client-only key mapping and state synchronization. */
@EventBusSubscriber(modid = CopycatReplace.MOD_ID, value = Dist.CLIENT)
public final class CopycatReplaceClientKeys {
    static final KeyMapping BULK_REPLACE = new KeyMapping(
            "key.copycat_replace.bulk_replace",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_ALT,
            "key.categories.copycat_replace"
    );
    private static boolean lastSentDown;

    private CopycatReplaceClientKeys() {
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        InputConstants.Key pressedKey = InputConstants.getKey(event.getKey(), event.getScanCode());
        if (!pressedKey.equals(BULK_REPLACE.getKey())) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.getConnection() == null) {
            return;
        }

        boolean down = event.getAction() == GLFW.GLFW_PRESS && minecraft.screen == null;
        if (event.getAction() == GLFW.GLFW_PRESS || event.getAction() == GLFW.GLFW_RELEASE) {
            syncState(down);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean connected = minecraft.player != null && minecraft.getConnection() != null;
        boolean down = connected && minecraft.screen == null && BULK_REPLACE.isDown();

        if (connected) {
            syncState(down);
        } else if (!connected) {
            lastSentDown = false;
        }
    }

    private static void syncState(boolean down) {
        if (down == lastSentDown) {
            return;
        }
        PacketDistributor.sendToServer(new BulkReplaceKeyStatePayload(down));
        lastSentDown = down;
    }
}
