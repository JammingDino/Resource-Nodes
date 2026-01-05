package com.jamming_dino.jd_resource_nodes.client;

import com.jamming_dino.jd_resource_nodes.ResourceNodeData;
import com.jamming_dino.jd_resource_nodes.ResourceNodes;
import com.jamming_dino.jd_resource_nodes.ResourceNodesConfig;
import com.jamming_dino.jd_resource_nodes.block.ResourceNodeBlock;
import com.jamming_dino.jd_resource_nodes.block.entity.ResourceNodeBlockEntity;
import com.jamming_dino.jd_resource_nodes.capability.ScannerUnlockData;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = ResourceNodes.MODID, value = Dist.CLIENT)
public class ScannerHandler {

    // REMOVED: private static final int SCAN_RADIUS = 128;
    private static final float SCAN_SPEED = 2f;
    private static final int PING_LIFETIME = 200;
    private static final int FADE_TICKS = 40;

    // List to store active scan results
    private static final List<ScanResult> activePings = new ArrayList<>();
    private static int scanTickCounter = 0;

    // --- STATE VARIABLES ---
    private static boolean isRadialMenuOpen = false;
    private static RadialSelectionScreen radialScreen = null;
    private static boolean isScanning = false;

    private static class ScanResult {
        final BlockPos pos;
        final Component name;
        final int color;
        final double scanDistance;
        boolean playedSound = false;

        public ScanResult(BlockPos pos, Component name, int color, double scanDistance) {
            this.pos = pos;
            this.name = name;
            this.color = color;
            this.scanDistance = scanDistance;
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        if (event.getKey() == ResourceNodesKeys.PING_KEY.getKey().getValue()) {
            if (event.getAction() == 1) { // Pressed
                if (!isRadialMenuOpen) {
                    if (mc.screen != null) return;
                    if (openRadialMenu(mc)) {
                        isRadialMenuOpen = true;
                    }
                }
            } else if (event.getAction() == 0) { // Released
                if (isRadialMenuOpen) {
                    closeRadialMenuAndScan(mc);
                    isRadialMenuOpen = false;
                }
            }
        }
    }

    private static boolean openRadialMenu(Minecraft mc) {
        if (mc.player == null) return false;

        ScannerUnlockData data = mc.player.getData(ResourceNodes.SCANNER_DATA);
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
                performScan(mc.player, mc.level, null, selectedBlocks, categoryName);
            }
        } else {
            mc.setScreen(null);
        }
        radialScreen = null;
    }

    // --- OPTIMIZED ENTITY-BASED SCANNING LOGIC ---

    private static class RawNodeData {
        final BlockPos pos;
        final Block block;

        RawNodeData(BlockPos pos, Block block) {
            this.pos = pos;
            this.block = block;
        }
    }

    private static void performScan(Player player, Level level, Block filterBlock, List<Block> filterBlocks, String categoryName) {
        if (isScanning) return;
        isScanning = true;

        // GET CONFIG RADIUS HERE
        int scanRadius = ResourceNodesConfig.getScannerRadius();

        // Feedback Message
        if (filterBlocks != null && !filterBlocks.isEmpty() && categoryName != null) {
            player.displayClientMessage(Component.literal("Scanning for: " + categoryName + "..."), true);
        } else if (filterBlock != null) {
            player.displayClientMessage(Component.literal("Scanning for: " + filterBlock.getName().getString() + "..."), true);
        } else {
            player.displayClientMessage(Component.literal("Scanning for all Nodes..."), true);
        }

        BlockPos playerPos = player.blockPosition();
        Vec3 playerVec = player.position();

        // 1. Gather Raw Data on Main Thread
        List<RawNodeData> candidates = new ArrayList<>();

        int chunkRadius = (scanRadius >> 4) + 1;
        int minChunkX = (playerPos.getX() >> 4) - chunkRadius;
        int maxChunkX = (playerPos.getX() >> 4) + chunkRadius;
        int minChunkZ = (playerPos.getZ() >> 4) - chunkRadius;
        int maxChunkZ = (playerPos.getZ() >> 4) + chunkRadius;

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                if (level.getChunk(cx, cz, ChunkStatus.FULL, false) instanceof LevelChunk chunk) {
                    Map<BlockPos, BlockEntity> entities = chunk.getBlockEntities();
                    for (BlockEntity be : entities.values()) {
                        if (be instanceof ResourceNodeBlockEntity) {
                            candidates.add(new RawNodeData(be.getBlockPos(), be.getBlockState().getBlock()));
                        }
                    }
                }
            }
        }

        // 2. Process Filters and Distance Asynchronously
        CompletableFuture.runAsync(() -> {
            List<ScanResult> results = new ArrayList<>();
            int foundCount = 0;
            double radiusSq = scanRadius * scanRadius;

            for (RawNodeData data : candidates) {
                // Check Filters
                boolean matches = false;
                if (filterBlocks != null && !filterBlocks.isEmpty()) {
                    matches = filterBlocks.contains(data.block);
                } else if (filterBlock != null) {
                    matches = (filterBlock == data.block);
                } else {
                    matches = true;
                }

                if (!matches) continue;

                // Check Distance
                double distSq = data.pos.distToCenterSqr(playerVec);
                if (distSq > radiusSq) continue;

                double dist = Math.sqrt(distSq);
                results.add(new ScanResult(data.pos, data.block.getName(), 0xFFFFFF, dist));
                foundCount++;
            }

            // 3. Apply Results on Main Thread
            int finalFoundCount = foundCount;
            Minecraft.getInstance().execute(() -> {
                isScanning = false;
                scanTickCounter = 0;
                activePings.clear();
                activePings.addAll(results);

                if (finalFoundCount == 0) {
                    player.displayClientMessage(Component.literal("No nodes found nearby."), true);
                } else {
                    player.playSound(SoundEvents.BEACON_ACTIVATE, 0.5f, 2.0f);
                    player.displayClientMessage(Component.literal("Found " + finalFoundCount + " nodes!"), true);
                }
            });
        });
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        if (!activePings.isEmpty()) {
            scanTickCounter++;
            activePings.removeIf(ping -> {
                double ticksToReach = ping.scanDistance / SCAN_SPEED;
                double age = scanTickCounter - ticksToReach;
                return age > PING_LIFETIME;
            });

            for (ScanResult ping : activePings) {
                double ticksToReach = ping.scanDistance / SCAN_SPEED;
                double age = scanTickCounter - ticksToReach;

                if (age >= 0 && !ping.playedSound) {
                    float pitch = 2.0f;
                    mc.player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 0.5f, pitch);
                    ping.playedSound = true;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (activePings.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos = event.getCamera().getPosition();

        List<ScanResult> renderList = new ArrayList<>(activePings);
        renderList.sort((a, b) -> {
            double distA = a.pos.distToCenterSqr(cameraPos);
            double distB = b.pos.distToCenterSqr(cameraPos);
            return Double.compare(distB, distA);
        });

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        Tesselator tesselator = Tesselator.getInstance();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (ScanResult ping : renderList) {
            float alpha = calculateAlpha(ping);
            if (alpha <= 0.1) continue;
            renderPingBeam(poseStack, buffer, cameraPos, ping, alpha);
        }
        MeshData beamMesh = buffer.build();
        if (beamMesh != null) BufferUploader.drawWithShader(beamMesh);

        RenderSystem.lineWidth(5.0f);
        buffer = tesselator.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        for (ScanResult ping : renderList) {
            float alpha = calculateAlpha(ping);
            if (alpha <= 0.1) continue;
            renderPingBox(poseStack, buffer, cameraPos, ping, alpha);
        }
        MeshData boxMesh = buffer.build();
        if (boxMesh != null) BufferUploader.drawWithShader(boxMesh);

        if (ResourceNodesConfig.isTextEnabled()) {
            RenderSystem.setShader(() -> null);
            RenderSystem.lineWidth(1.0f);
            MultiBufferSource.BufferSource textBuffer = MultiBufferSource.immediate(new ByteBufferBuilder(1536));

            for (ScanResult ping : renderList) {
                float alpha = calculateAlpha(ping);
                if (alpha <= 0.1) continue;
                renderPingText(poseStack, textBuffer, mc, cameraPos, ping, alpha);
            }
            textBuffer.endBatch();
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    private static float calculateAlpha(ScanResult ping) {
        double ticksToReach = ping.scanDistance / SCAN_SPEED;
        double age = scanTickCounter - ticksToReach;
        if (age < 0) return 0.0f;
        float remaining = (float)(PING_LIFETIME - age);
        if (remaining < FADE_TICKS) {
            return Math.max(0.0f, remaining / (float)FADE_TICKS);
        }
        return 1.0f;
    }

    private static void renderPingBeam(PoseStack poseStack, BufferBuilder buffer, Vec3 cameraPos, ScanResult ping, float alpha) {
        BlockPos pos = ping.pos;
        double dx = pos.getX() - cameraPos.x;
        double dy = pos.getY() - cameraPos.y;
        double dz = pos.getZ() - cameraPos.z;

        poseStack.pushPose();
        poseStack.translate(dx, dy, dz);
        Matrix4f matrix = poseStack.last().pose();
        float height = 320.0f;
        float width = 0.2f;
        float min = 0.5f - width;
        float max = 0.5f + width;
        int r = 255; int g = 255; int b = 255;
        int alphaBottom = (int) (alpha * 100);
        int alphaTop = 0;

        vertex(buffer, matrix, min, 0, min, r, g, b, alphaBottom);
        vertex(buffer, matrix, min, height, min, r, g, b, alphaTop);
        vertex(buffer, matrix, max, height, min, r, g, b, alphaTop);
        vertex(buffer, matrix, max, 0, min, r, g, b, alphaBottom);
        vertex(buffer, matrix, min, 0, max, r, g, b, alphaBottom);
        vertex(buffer, matrix, max, 0, max, r, g, b, alphaBottom);
        vertex(buffer, matrix, max, height, max, r, g, b, alphaTop);
        vertex(buffer, matrix, min, height, max, r, g, b, alphaTop);
        vertex(buffer, matrix, max, 0, min, r, g, b, alphaBottom);
        vertex(buffer, matrix, max, height, min, r, g, b, alphaTop);
        vertex(buffer, matrix, max, height, max, r, g, b, alphaTop);
        vertex(buffer, matrix, max, 0, max, r, g, b, alphaBottom);
        vertex(buffer, matrix, min, 0, min, r, g, b, alphaBottom);
        vertex(buffer, matrix, min, 0, max, r, g, b, alphaBottom);
        vertex(buffer, matrix, min, height, max, r, g, b, alphaTop);
        vertex(buffer, matrix, min, height, min, r, g, b, alphaTop);
        poseStack.popPose();
    }

    private static void renderPingBox(PoseStack poseStack, BufferBuilder buffer, Vec3 cameraPos, ScanResult ping, float alpha) {
        BlockPos pos = ping.pos;
        double dx = pos.getX() - cameraPos.x;
        double dy = pos.getY() - cameraPos.y;
        double dz = pos.getZ() - cameraPos.z;

        poseStack.pushPose();
        poseStack.translate(dx, dy, dz);
        Matrix4f matrix = poseStack.last().pose();
        int r = 255; int g = 255; int b = 255; int a = (int)(alpha * 255);
        float min = -0.00f; float max = 1.00f;

        vertex(buffer, matrix, min, min, min, r, g, b, a); vertex(buffer, matrix, max, min, min, r, g, b, a);
        vertex(buffer, matrix, max, min, min, r, g, b, a); vertex(buffer, matrix, max, min, max, r, g, b, a);
        vertex(buffer, matrix, max, min, max, r, g, b, a); vertex(buffer, matrix, min, min, max, r, g, b, a);
        vertex(buffer, matrix, min, min, max, r, g, b, a); vertex(buffer, matrix, min, min, min, r, g, b, a);
        vertex(buffer, matrix, min, max, min, r, g, b, a); vertex(buffer, matrix, max, max, min, r, g, b, a);
        vertex(buffer, matrix, max, max, min, r, g, b, a); vertex(buffer, matrix, max, max, max, r, g, b, a);
        vertex(buffer, matrix, max, max, max, r, g, b, a); vertex(buffer, matrix, min, max, max, r, g, b, a);
        vertex(buffer, matrix, min, max, max, r, g, b, a); vertex(buffer, matrix, min, max, min, r, g, b, a);
        vertex(buffer, matrix, min, min, min, r, g, b, a); vertex(buffer, matrix, min, max, min, r, g, b, a);
        vertex(buffer, matrix, max, min, min, r, g, b, a); vertex(buffer, matrix, max, max, min, r, g, b, a);
        vertex(buffer, matrix, max, min, max, r, g, b, a); vertex(buffer, matrix, max, max, max, r, g, b, a);
        vertex(buffer, matrix, min, min, max, r, g, b, a); vertex(buffer, matrix, min, max, max, r, g, b, a);
        poseStack.popPose();
    }

    private static void vertex(BufferBuilder b, Matrix4f m, float x, float y, float z, int r, int g, int blue, int a) {
        b.addVertex(m, x, y, z).setColor(r, g, blue, a);
    }

    private static void renderPingText(PoseStack poseStack, MultiBufferSource buffer, Minecraft mc, Vec3 cameraPos, ScanResult ping, float alpha) {
        BlockPos pos = ping.pos;
        double dx = pos.getX() - cameraPos.x;
        double dy = pos.getY() - cameraPos.y;
        double dz = pos.getZ() - cameraPos.z;

        double currentDistSq = pos.distToCenterSqr(cameraPos);
        double currentDistance = Math.sqrt(currentDistSq);

        poseStack.pushPose();
        poseStack.translate(dx + 0.5, dy + 1.5, dz + 0.5);
        poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());

        float baseScale = 0.025F;
        float configScale = ResourceNodesConfig.getTextScale();
        float constantScale = baseScale * configScale * (float)Math.max(currentDistance, 4.0) / 4.0f;
        poseStack.scale(constantScale, -constantScale, constantScale);

        Component text = Component.literal(ping.name.getString() + " (" + (int)currentDistance + "m)");
        Matrix4f matrix4f = poseStack.last().pose();

        int textAlpha = (int)(alpha * 255);
        int textColor = (ping.color & 0x00FFFFFF) | (textAlpha << 24);

        Font font = mc.font;
        float xOffset = -font.width(text) / 2.0f;

        font.drawInBatch(
                text,
                xOffset,
                0,
                textColor,
                false,
                matrix4f,
                buffer,
                Font.DisplayMode.SEE_THROUGH,
                0,
                15728880
        );

        poseStack.popPose();
    }
}