package com.jamming_dino.jd_resource_nodes.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RequestNodeSettingsPacket {
    public static final RequestNodeSettingsPacket INSTANCE = new RequestNodeSettingsPacket();

    public static RequestNodeSettingsPacket decode(FriendlyByteBuf buffer) {
        return INSTANCE;
    }

    public static void encode(RequestNodeSettingsPacket packet, FriendlyByteBuf buffer) {
    }

    public static void handle(RequestNodeSettingsPacket payload, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                NodeSettingsNetworkSync.syncTo(player);
            }
        });
        context.setPacketHandled(true);
    }
}

