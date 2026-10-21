package com.radaeli.copycatreplace.network;

import com.radaeli.copycatreplace.CopycatReplace;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Sends the authoritative server action limit to a joining client for its preview. */
public record ConnectedBlockLimitPayload(int maximum) implements CustomPacketPayload {
    public static final Type<ConnectedBlockLimitPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CopycatReplace.MOD_ID, "connected_block_limit")
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, ConnectedBlockLimitPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT,
                    ConnectedBlockLimitPayload::maximum,
                    ConnectedBlockLimitPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ConnectedBlockLimitPayload payload, IPayloadContext context) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            context.enqueueWork(() -> com.radaeli.copycatreplace.client.ConnectedBulkIndicator
                    .setServerMaximum(payload.maximum()));
        }
    }
}
