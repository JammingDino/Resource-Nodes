package com.jamming_dino.jd_resource_nodes;

import com.jamming_dino.jd_resource_nodes.world.NodeWorldSettingsSavedData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public class NodeActivationState {
    private NodeActivationState() {
    }

    public static boolean isNodeEnabled(Level level, Block block) {
        String blockId = BuiltInRegistries.BLOCK.getKey(block).toString();

        if (!ResourceNodesConfig.isNodeGloballyEnabled(blockId)) {
            return false;
        }

        if (level instanceof ServerLevel serverLevel) {
            return NodeWorldSettingsSavedData.get(serverLevel).isEnabled(blockId);
        }

        return true;
    }
}

