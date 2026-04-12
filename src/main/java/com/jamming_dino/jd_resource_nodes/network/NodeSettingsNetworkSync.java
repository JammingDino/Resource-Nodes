package com.jamming_dino.jd_resource_nodes.network;

import com.jamming_dino.jd_resource_nodes.CustomNodePurityMode;
import com.jamming_dino.jd_resource_nodes.ResourceNodeTier;
import com.jamming_dino.jd_resource_nodes.ResourceNodesConfig;
import com.jamming_dino.jd_resource_nodes.world.NodeWorldSettingsSavedData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.Set;

public class NodeSettingsNetworkSync {
    private NodeSettingsNetworkSync() {
    }

    public static void syncTo(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new SyncNodeSettingsPacket(
                new HashSet<>(ResourceNodesConfig.getDisabledNodesGlobal()),
                NodeWorldSettingsSavedData.get(player.serverLevel()).getDisabledNodes()
        ));

        PacketDistributor.sendToPlayer(player, new SyncCustomNodeIdsPacket(buildCustomNodeIds()));
    }

    private static Set<String> buildCustomNodeIds() {
        Set<String> ids = new HashSet<>();

        for (ResourceNodesConfig.CustomNodeConfig custom : ResourceNodesConfig.getCustomNodes()) {
            String baseId = sanitizeId(custom.id);
            if (baseId.isBlank()) {
                continue;
            }

            CustomNodePurityMode purityMode = CustomNodePurityMode.fromId(custom.purity_mode);
            for (ResourceNodeTier tier : purityMode.getTiers()) {
                ResourceLocation blockId = ResourceLocation.fromNamespaceAndPath("jd_resource_nodes", "node_custom_" + baseId + "_" + tier.getSerializedName());
                if (BuiltInRegistries.BLOCK.containsKey(blockId)) {
                    ids.add(blockId.toString());
                }
            }
        }

        return ids;
    }

    private static String sanitizeId(String id) {
        if (id == null) {
            return "";
        }
        return id.toLowerCase().replaceAll("[^a-z0-9_]+", "_");
    }
}


