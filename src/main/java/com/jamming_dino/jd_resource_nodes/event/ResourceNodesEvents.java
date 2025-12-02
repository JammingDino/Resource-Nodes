package com.jamming_dino.jd_resource_nodes.event;

import com.jamming_dino.jd_resource_nodes.ResourceNodes;
import com.jamming_dino.jd_resource_nodes.block.ResourceNodeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

@EventBusSubscriber(modid = ResourceNodes.MODID)
public class ResourceNodesEvents {

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) return;

        BlockState state = event.getState();

        // Check if the block being broken is one of our nodes
        if (state.getBlock() instanceof ResourceNodeBlock nodeBlock) {
            // Call our custom logic
            // Returns TRUE if we should let the block break (Creative/Silk Touch)
            // Returns FALSE if we handled it (Regenerated it)
            boolean shouldBreak = nodeBlock.handlePlayerBreak(
                    (net.minecraft.world.level.Level) event.getLevel(),
                    event.getPos(),
                    state,
                    event.getPlayer()
            );

            // If handlePlayerBreak returns FALSE, it means "Don't break this block, I already turned it to stone."
            // So we cancel the vanilla break event.
            if (!shouldBreak) {
                event.setCanceled(true);
            }
        }
    }
}