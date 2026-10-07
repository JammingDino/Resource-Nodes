package com.jamming_dino.jd_resource_nodes.network;

import com.jamming_dino.jd_resource_nodes.client.NodeSettingsClientCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public record SyncNodeSettingsPacket(Set<String> disabledGlobal, Set<String> disabledWorld) {
    public static SyncNodeSettingsPacket decode(FriendlyByteBuf buffer) {
        return new SyncNodeSettingsPacket(readSet(buffer), readSet(buffer));
    }

    public static void encode(SyncNodeSettingsPacket packet, FriendlyByteBuf buffer) {
        writeSet(buffer, packet.disabledGlobal());
        writeSet(buffer, packet.disabledWorld());
    }

    private static Set<String> readSet(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        Set<String> values = new HashSet<>();
        for (int i = 0; i < size; i++) {
            values.add(buffer.readUtf());
        }
        return values;
    }

    private static void writeSet(FriendlyByteBuf buffer, Set<String> values) {
        buffer.writeVarInt(values.size());
        for (String value : values) {
            buffer.writeUtf(value);
        }
    }

    public static void handle(SyncNodeSettingsPacket payload, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> NodeSettingsClientCache.update(payload.disabledGlobal(), payload.disabledWorld()));
        context.setPacketHandled(true);
    }
}
