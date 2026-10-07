package com.jamming_dino.jd_resource_nodes.network;

import com.jamming_dino.jd_resource_nodes.capability.ScannerUnlockData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public record SyncScannerUnlocksPacket(Set<String> unlocks) {
    public static SyncScannerUnlocksPacket decode(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        Set<String> values = new HashSet<>();
        for (int i = 0; i < size; i++) {
            values.add(buffer.readUtf());
        }
        return new SyncScannerUnlocksPacket(values);
    }

    public static void encode(SyncScannerUnlocksPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.unlocks().size());
        for (String unlocked : packet.unlocks()) {
            buffer.writeUtf(unlocked);
        }
    }

    public static void handle(SyncScannerUnlocksPacket payload, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getDirection().getReceptionSide().isClient()) {
                net.minecraft.world.entity.player.Player player = Minecraft.getInstance().player;
                if (player != null) {
                    ScannerUnlockData data = ScannerUnlockData.get(player);
                    data.setUnlockedCategories(payload.unlocks);
                    ScannerUnlockData.save(player, data);
                }
            }
        });
        context.setPacketHandled(true);
    }
}