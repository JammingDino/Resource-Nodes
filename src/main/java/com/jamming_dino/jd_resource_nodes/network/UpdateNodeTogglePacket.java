package com.jamming_dino.jd_resource_nodes.network;

import com.jamming_dino.jd_resource_nodes.ResourceNodes;
import com.jamming_dino.jd_resource_nodes.ResourceNodesConfig;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record UpdateNodeTogglePacket(String blockId, boolean enabled) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<UpdateNodeTogglePacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ResourceNodes.MODID, "update_node_toggle"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateNodeTogglePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            UpdateNodeTogglePacket::blockId,
            ByteBufCodecs.BOOL,
            UpdateNodeTogglePacket::enabled,
            UpdateNodeTogglePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UpdateNodeTogglePacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            boolean canEdit = player.getServer() != null && (!player.getServer().isDedicatedServer() || player.hasPermissions(2));
            if (!canEdit) {
                player.displayClientMessage(Component.literal("You need operator permissions to edit node settings."), true);
                NodeSettingsNetworkSync.syncTo(player);
                return;
            }

            ResourceNodesConfig.setNodeGloballyEnabled(payload.blockId(), payload.enabled());

            NodeSettingsNetworkSync.syncTo(player);
        });
    }
}



