package com.jamming_dino.jd_resource_nodes.client;

import com.jamming_dino.jd_resource_nodes.ResourceNodeData;
import com.jamming_dino.jd_resource_nodes.ResourceNodes;
import com.jamming_dino.jd_resource_nodes.ResourceNodesConfig;
import com.jamming_dino.jd_resource_nodes.block.entity.ResourceNodeBlockEntity;
import com.jamming_dino.jd_resource_nodes.capability.ScannerUnlockData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Mod.EventBusSubscriber(modid = ResourceNodes.MODID, value = Dist.CLIENT)
public class ScannerHandler {
    private static boolean isRadialMenuOpen = false;
    private static RadialSelectionScreen radialScreen = null;
    private static boolean isScanning = false;

    private ScannerHandler() {
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }

        if (event.getKey() != ResourceNodesKeys.PING_KEY.getKey().getValue()) {
            return;
        }

        if (event.getAction() == 1) {
            if (!isRadialMenuOpen && mc.screen == null && openRadialMenu(mc)) {
                isRadialMenuOpen = true;
            }
            return;
        }

        if (event.getAction() == 0 && isRadialMenuOpen) {
            closeRadialMenuAndScan(mc);
            isRadialMenuOpen = false;
        }
    }

    private static boolean openRadialMenu(Minecraft mc) {
        if (mc.player == null) {
            return false;
        }

        ScannerUnlockData data = ScannerUnlockData.get(mc.player);
        List<ResourceNodeData> allCategories = ResourceNodeData.getAllCategories();
        List<ResourceNodeData> unlockedCategories = new ArrayList<>();

        for (ResourceNodeData cat : allCategories) {
            if (data.isUnlocked(cat.getCategory())) {
                unlockedCategories.add(cat);
            }
        }

        if (unlockedCategories.isEmpty()) {
            mc.player.displayClientMessage(Component.literal("No scanners unlocked! Use /scanner unlock <resource>"), true);
            return false;
        }

        radialScreen = new RadialSelectionScreen();
        radialScreen.setCategories(unlockedCategories);
        mc.setScreen(radialScreen);
        return true;
    }

    private static void closeRadialMenuAndScan(Minecraft mc) {
        if (mc.screen == radialScreen && radialScreen != null) {
            List<Block> selectedBlocks = radialScreen.getSelectedBlocks();
            String categoryName = radialScreen.getSelectedCategoryName();
            mc.setScreen(null);

            if (!selectedBlocks.isEmpty()) {
                performScan(mc.player, mc.level, selectedBlocks, categoryName);
            }
        } else {
            mc.setScreen(null);
        }
        radialScreen = null;
    }

    private static class RawNodeData {
        final Block block;
        final double distanceSq;

        RawNodeData(Block block, double distanceSq) {
            this.block = block;
            this.distanceSq = distanceSq;
        }
    }

    private static void performScan(Player player, Level level, List<Block> filterBlocks, String categoryName) {
        if (player == null || level == null || isScanning) {
            return;
        }

        isScanning = true;
        int scanRadius = ResourceNodesConfig.getScannerRadius();
        double radiusSq = (double) scanRadius * scanRadius;

        if (categoryName != null) {
            player.displayClientMessage(Component.literal("Scanning for: " + categoryName + "..."), true);
        } else {
            player.displayClientMessage(Component.literal("Scanning for nodes..."), true);
        }

        Vec3 playerVec = player.position();
        List<RawNodeData> candidates = new ArrayList<>();

        int chunkRadius = (scanRadius >> 4) + 1;
        int playerChunkX = player.blockPosition().getX() >> 4;
        int playerChunkZ = player.blockPosition().getZ() >> 4;

        for (int cx = playerChunkX - chunkRadius; cx <= playerChunkX + chunkRadius; cx++) {
            for (int cz = playerChunkZ - chunkRadius; cz <= playerChunkZ + chunkRadius; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }

                LevelChunk chunk = level.getChunk(cx, cz);
                Map<net.minecraft.core.BlockPos, BlockEntity> entities = chunk.getBlockEntities();
                for (BlockEntity be : entities.values()) {
                    if (be instanceof ResourceNodeBlockEntity) {
                        double distSq = be.getBlockPos().distToCenterSqr(playerVec);
                        if (distSq <= radiusSq) {
                            candidates.add(new RawNodeData(be.getBlockState().getBlock(), distSq));
                        }
                    }
                }
            }
        }

        CompletableFuture.runAsync(() -> {
            int foundCount = 0;
            for (RawNodeData node : candidates) {
                if (filterBlocks == null || filterBlocks.isEmpty() || filterBlocks.contains(node.block)) {
                    foundCount++;
                }
            }

            int finalFoundCount = foundCount;
            Minecraft.getInstance().execute(() -> {
                isScanning = false;
                if (finalFoundCount == 0) {
                    player.displayClientMessage(Component.literal("No nodes found nearby."), true);
                } else {
                    player.playSound(SoundEvents.BEACON_ACTIVATE, 0.5f, 2.0f);
                    player.displayClientMessage(Component.literal("Found " + finalFoundCount + " nodes."), true);
                }
            });
        });
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (isRadialMenuOpen && radialScreen != null && mc.screen != radialScreen) {
            isRadialMenuOpen = false;
            radialScreen = null;
        }
    }
}

