package com.radaeli.copycatreplace.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** Only carries a momentary modifier; the server chooses and validates action targets. */
public record BulkReplaceKeyStatePayload(boolean down) {
    public static void encode(BulkReplaceKeyStatePayload message, FriendlyByteBuf buffer) { buffer.writeBoolean(message.down); }
    public static BulkReplaceKeyStatePayload decode(FriendlyByteBuf buffer) { return new BulkReplaceKeyStatePayload(buffer.readBoolean()); }
    public static void handle(BulkReplaceKeyStatePayload message, Supplier<NetworkEvent.Context> context) {
        var sender = context.get().getSender();
        if (sender != null) BulkReplaceMode.setDown(sender, message.down);
        context.get().setPacketHandled(true);
    }
}
