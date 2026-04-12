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

public record AddCustomNodeConfigPacket(String id, String purityMode, String originalBlockId, String regeneratingBlockId, String outputItemId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<AddCustomNodeConfigPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ResourceNodes.MODID, "add_custom_node_config"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AddCustomNodeConfigPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8,
            AddCustomNodeConfigPacket::id,
            ByteBufCodecs.STRING_UTF8,
            AddCustomNodeConfigPacket::purityMode,
            ByteBufCodecs.STRING_UTF8,
            AddCustomNodeConfigPacket::originalBlockId,
            ByteBufCodecs.STRING_UTF8,
            AddCustomNodeConfigPacket::regeneratingBlockId,
            ByteBufCodecs.STRING_UTF8,
            AddCustomNodeConfigPacket::outputItemId,
            AddCustomNodeConfigPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AddCustomNodeConfigPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
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
    }
}







