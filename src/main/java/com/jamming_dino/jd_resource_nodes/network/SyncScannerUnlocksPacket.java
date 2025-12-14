package com.jamming_dino.jd_resource_nodes.network;

import com.jamming_dino.jd_resource_nodes.ResourceNodes;
import com.jamming_dino.jd_resource_nodes.capability.ScannerUnlockData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs; // Import
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashSet;
import java.util.Set;

public record SyncScannerUnlocksPacket(Set<String> unlocks) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncScannerUnlocksPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ResourceNodes.MODID, "sync_scanner_unlocks"));

    // Use ByteBufCodecs to create a clean codec for Set<String>
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncScannerUnlocksPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.collection(HashSet::new, ByteBufCodecs.STRING_UTF8), // Codec for Set<String>
            SyncScannerUnlocksPacket::unlocks,                                 // Getter: Extracts Set from Packet
            SyncScannerUnlocksPacket::new                                      // Factory: Creates Packet from Set
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncScannerUnlocksPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Client side handling
            if (context.flow().isClientbound()) {
                net.minecraft.world.entity.player.Player player = Minecraft.getInstance().player;
                if (player != null) {
                    ScannerUnlockData data = player.getData(ResourceNodes.SCANNER_DATA);
                    data.setUnlockedCategories(payload.unlocks);
                }
            }
        });
    }
}