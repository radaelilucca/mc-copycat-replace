package com.radaeli.copycatreplace.network;

import com.radaeli.copycatreplace.CopycatReplace;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Registers the small key-state payload used by the client modifier. */
@EventBusSubscriber(modid = CopycatReplace.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class ModNetworking {
    private ModNetworking() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(
                BulkReplaceKeyStatePayload.TYPE,
                BulkReplaceKeyStatePayload.STREAM_CODEC,
                BulkReplaceKeyStatePayload::handle
        );
    }
}
