package com.jamming_dino.jd_resource_nodes.network;

import com.jamming_dino.jd_resource_nodes.ResourceNodesConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record UpdateNodeTogglePacket(String blockId, boolean enabled) {
    public static UpdateNodeTogglePacket decode(FriendlyByteBuf buffer) {
        return new UpdateNodeTogglePacket(buffer.readUtf(), buffer.readBoolean());
    }

    public static void encode(UpdateNodeTogglePacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.blockId());
        buffer.writeBoolean(packet.enabled());
    }

    public static void handle(UpdateNodeTogglePacket payload, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
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
        context.setPacketHandled(true);
    }
}



