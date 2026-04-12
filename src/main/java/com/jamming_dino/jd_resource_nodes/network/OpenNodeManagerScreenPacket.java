package com.jamming_dino.jd_resource_nodes.network;

import com.jamming_dino.jd_resource_nodes.ResourceNodes;
import com.jamming_dino.jd_resource_nodes.client.ClientScreenOpener;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record OpenNodeManagerScreenPacket() implements CustomPacketPayload {
    public static final OpenNodeManagerScreenPacket INSTANCE = new OpenNodeManagerScreenPacket();

    public static final CustomPacketPayload.Type<OpenNodeManagerScreenPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ResourceNodes.MODID, "open_node_manager_screen"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenNodeManagerScreenPacket> STREAM_CODEC =
            StreamCodec.of((buf, packet) -> { }, buf -> INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenNodeManagerScreenPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.flow().isClientbound()) {
                ClientScreenOpener.openNodeManagerScreen();
            }
        });
    }
}

