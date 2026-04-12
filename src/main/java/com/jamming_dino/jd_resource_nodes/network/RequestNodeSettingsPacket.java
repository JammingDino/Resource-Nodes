package com.jamming_dino.jd_resource_nodes.network;

import com.jamming_dino.jd_resource_nodes.ResourceNodes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RequestNodeSettingsPacket() implements CustomPacketPayload {
    public static final RequestNodeSettingsPacket INSTANCE = new RequestNodeSettingsPacket();

    public static final CustomPacketPayload.Type<RequestNodeSettingsPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ResourceNodes.MODID, "request_node_settings"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestNodeSettingsPacket> STREAM_CODEC =
            StreamCodec.of((buf, packet) -> { }, buf -> INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestNodeSettingsPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                NodeSettingsNetworkSync.syncTo(player);
            }
        });
    }
}

