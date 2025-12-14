package com.jamming_dino.jd_resource_nodes.event;

import com.jamming_dino.jd_resource_nodes.ResourceNodes;
import com.jamming_dino.jd_resource_nodes.block.ResourceNodeBlock;
import com.jamming_dino.jd_resource_nodes.capability.ScannerUnlockData;
import com.jamming_dino.jd_resource_nodes.command.ScannerCommands;
import com.jamming_dino.jd_resource_nodes.network.SyncScannerUnlocksPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid = ResourceNodes.MODID)
public class ResourceNodesEvents {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        ScannerCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ScannerUnlockData data = player.getData(ResourceNodes.SCANNER_DATA);
            PacketDistributor.sendToPlayer(player, new SyncScannerUnlocksPacket(data.getUnlockedCategories()));
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayer newPlayer && event.getOriginal() instanceof ServerPlayer oldPlayer) {
            // Copy data from old player to new player
            // In 1.21 attachments might handle this automatically if configured, but let's be safe
            oldPlayer.getData(ResourceNodes.SCANNER_DATA); // ensure loaded
            ScannerUnlockData oldData = oldPlayer.getData(ResourceNodes.SCANNER_DATA);
            ScannerUnlockData newData = newPlayer.getData(ResourceNodes.SCANNER_DATA);
            newData.setUnlockedCategories(oldData.getUnlockedCategories());
        }
    }

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