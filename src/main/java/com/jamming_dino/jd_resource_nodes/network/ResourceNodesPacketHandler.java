package com.jamming_dino.jd_resource_nodes.network;

import com.jamming_dino.jd_resource_nodes.ResourceNodes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ResourceNodesPacketHandler {
    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(ResourceNodes.MODID, "main"))
            .networkProtocolVersion(() -> PROTOCOL)
            .clientAcceptedVersions(PROTOCOL::equals)
            .serverAcceptedVersions(PROTOCOL::equals)
            .simpleChannel();

    private static int packetId;

    private ResourceNodesPacketHandler() {
    }

    public static void register() {
        packetId = 0;

        CHANNEL.messageBuilder(RequestNodeSettingsPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(RequestNodeSettingsPacket::encode)
                .decoder(RequestNodeSettingsPacket::decode)
                .consumerMainThread(RequestNodeSettingsPacket::handle)
                .add();

        CHANNEL.messageBuilder(UpdateNodeTogglePacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(UpdateNodeTogglePacket::encode)
                .decoder(UpdateNodeTogglePacket::decode)
                .consumerMainThread(UpdateNodeTogglePacket::handle)
                .add();

        CHANNEL.messageBuilder(AddCustomNodeConfigPacket.class, nextId(), NetworkDirection.PLAY_TO_SERVER)
                .encoder(AddCustomNodeConfigPacket::encode)
                .decoder(AddCustomNodeConfigPacket::decode)
                .consumerMainThread(AddCustomNodeConfigPacket::handle)
                .add();

        CHANNEL.messageBuilder(SyncScannerUnlocksPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncScannerUnlocksPacket::encode)
                .decoder(SyncScannerUnlocksPacket::decode)
                .consumerMainThread(SyncScannerUnlocksPacket::handle)
                .add();

        CHANNEL.messageBuilder(SyncNodeSettingsPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncNodeSettingsPacket::encode)
                .decoder(SyncNodeSettingsPacket::decode)
                .consumerMainThread(SyncNodeSettingsPacket::handle)
                .add();

        CHANNEL.messageBuilder(SyncCustomNodeIdsPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncCustomNodeIdsPacket::encode)
                .decoder(SyncCustomNodeIdsPacket::decode)
                .consumerMainThread(SyncCustomNodeIdsPacket::handle)
                .add();

        CHANNEL.messageBuilder(OpenNodeManagerScreenPacket.class, nextId(), NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenNodeManagerScreenPacket::encode)
                .decoder(OpenNodeManagerScreenPacket::decode)
                .consumerMainThread(OpenNodeManagerScreenPacket::handle)
                .add();
    }

    public static void sendToServer(Object message) {
        CHANNEL.sendToServer(message);
    }

    public static void sendToPlayer(ServerPlayer player, Object message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    private static int nextId() {
        return packetId++;
    }
}