package com.radaeli.copycatreplace.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Client references stay in a separate handler so the dedicated server can load this type. */
public record ConnectedBlockLimitPayload(int maximum) {
    public static void encode(ConnectedBlockLimitPayload message, FriendlyByteBuf buffer) { buffer.writeVarInt(message.maximum); }
    public static ConnectedBlockLimitPayload decode(FriendlyByteBuf buffer) { return new ConnectedBlockLimitPayload(buffer.readVarInt()); }
    public static void handle(ConnectedBlockLimitPayload message, Supplier<NetworkEvent.Context> context) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                com.radaeli.copycatreplace.client.ClientPayloadHandler.acceptLimit(message.maximum));
        context.get().setPacketHandled(true);
    }
}
