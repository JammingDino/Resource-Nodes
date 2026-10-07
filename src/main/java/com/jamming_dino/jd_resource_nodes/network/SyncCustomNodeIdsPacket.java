package com.jamming_dino.jd_resource_nodes.network;

import com.jamming_dino.jd_resource_nodes.client.NodeSettingsClientCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public record SyncCustomNodeIdsPacket(Set<String> blockIds) {
    public static SyncCustomNodeIdsPacket decode(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        Set<String> ids = new HashSet<>();
        for (int i = 0; i < size; i++) {
            ids.add(buffer.readUtf());
        }
        return new SyncCustomNodeIdsPacket(ids);
    }

    public static void encode(SyncCustomNodeIdsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.blockIds().size());
        for (String id : packet.blockIds()) {
            buffer.writeUtf(id);
        }
    }

    public static void handle(SyncCustomNodeIdsPacket payload, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> NodeSettingsClientCache.updateCustomNodeIds(payload.blockIds()));
        context.setPacketHandled(true);
    }
}
