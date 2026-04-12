package com.jamming_dino.jd_resource_nodes.network;

import com.jamming_dino.jd_resource_nodes.ResourceNodes;
import com.jamming_dino.jd_resource_nodes.client.NodeSettingsClientCache;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashSet;
import java.util.Set;

public record SyncNodeSettingsPacket(Set<String> disabledGlobal, Set<String> disabledWorld) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncNodeSettingsPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ResourceNodes.MODID, "sync_node_settings"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncNodeSettingsPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.collection(HashSet::new, ByteBufCodecs.STRING_UTF8),
            SyncNodeSettingsPacket::disabledGlobal,
            ByteBufCodecs.collection(HashSet::new, ByteBufCodecs.STRING_UTF8),
            SyncNodeSettingsPacket::disabledWorld,
            SyncNodeSettingsPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncNodeSettingsPacket payload, IPayloadContext context) {
        context.enqueueWork(() -> NodeSettingsClientCache.update(payload.disabledGlobal(), payload.disabledWorld()));
    }
}

