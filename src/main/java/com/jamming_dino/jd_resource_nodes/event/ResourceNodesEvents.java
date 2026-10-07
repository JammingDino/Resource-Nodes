package com.jamming_dino.jd_resource_nodes.event;

import com.jamming_dino.jd_resource_nodes.ResourceNodes;
import com.jamming_dino.jd_resource_nodes.block.ResourceNodeBlock;
import com.jamming_dino.jd_resource_nodes.capability.ScannerUnlockData;
import com.jamming_dino.jd_resource_nodes.command.ScannerCommands;
import com.jamming_dino.jd_resource_nodes.network.NodeSettingsNetworkSync;
import com.jamming_dino.jd_resource_nodes.network.ResourceNodesPacketHandler;
import com.jamming_dino.jd_resource_nodes.network.SyncScannerUnlocksPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;

@Mod.EventBusSubscriber(modid = ResourceNodes.MODID)
public class ResourceNodesEvents {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        ScannerCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ScannerUnlockData data = ScannerUnlockData.get(player);
            ResourceNodesPacketHandler.sendToPlayer(player, new SyncScannerUnlocksPacket(data.getUnlockedCategories()));
            NodeSettingsNetworkSync.syncTo(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.getEntity() instanceof ServerPlayer newPlayer && event.getOriginal() instanceof ServerPlayer oldPlayer) {
            ScannerUnlockData oldData = ScannerUnlockData.get(oldPlayer);
            ScannerUnlockData newData = ScannerUnlockData.get(newPlayer);
            newData.setUnlockedCategories(oldData.getUnlockedCategories());
            ScannerUnlockData.save(newPlayer, newData);
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
