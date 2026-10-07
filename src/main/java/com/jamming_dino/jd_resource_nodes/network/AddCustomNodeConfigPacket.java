package com.jamming_dino.jd_resource_nodes.network;

import com.jamming_dino.jd_resource_nodes.ResourceNodes;
import com.jamming_dino.jd_resource_nodes.ResourceNodesConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record AddCustomNodeConfigPacket(String id, String purityMode, String originalBlockId, String regeneratingBlockId, String outputItemId) {
    public static AddCustomNodeConfigPacket decode(FriendlyByteBuf buffer) {
        return new AddCustomNodeConfigPacket(
                buffer.readUtf(),
                buffer.readUtf(),
                buffer.readUtf(),
                buffer.readUtf(),
                buffer.readUtf()
        );
    }

    public static void encode(AddCustomNodeConfigPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.id());
        buffer.writeUtf(packet.purityMode());
        buffer.writeUtf(packet.originalBlockId());
        buffer.writeUtf(packet.regeneratingBlockId());
        buffer.writeUtf(packet.outputItemId());
    }

    public static void handle(AddCustomNodeConfigPacket payload, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            boolean canEdit = player.getServer() != null && (!player.getServer().isDedicatedServer() || player.hasPermissions(2));
            if (!canEdit) {
                player.displayClientMessage(Component.literal("You need operator permissions to add custom nodes."), true);
                return;
            }

            ResourceNodesConfig.CustomNodeConfig config = new ResourceNodesConfig.CustomNodeConfig();
            config.id = payload.id();
            config.category = payload.id();
            config.purity_mode = payload.purityMode();
            config.ready_block = payload.originalBlockId();
            config.regenerating_block = payload.regeneratingBlockId();
            config.output_item = payload.outputItemId();
            config.overlay_source = "iron";
            ResourceNodesConfig.addCustomNode(config);

            ResourceNodes.LOGGER.info(
                    "Saved custom node config id='{}' purity='{}' original='{}' regen='{}' drop='{}'",
                    config.id,
                    config.purity_mode,
                    config.ready_block,
                    config.regenerating_block,
                    config.output_item
            );

            player.displayClientMessage(Component.literal("Custom node saved. It should appear in node list now; restart required for Creative tab registration."), false);
            NodeSettingsNetworkSync.syncTo(player);
        });
        context.setPacketHandled(true);
    }
}







