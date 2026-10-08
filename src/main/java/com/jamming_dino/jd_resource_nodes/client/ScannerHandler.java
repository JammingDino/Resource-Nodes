package com.jamming_dino.jd_resource_nodes.client;

import com.jamming_dino.jd_resource_nodes.ResourceNodeData;
import com.jamming_dino.jd_resource_nodes.ResourceNodes;
import com.jamming_dino.jd_resource_nodes.ResourceNodesConfig;
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
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Mod.EventBusSubscriber(modid = ResourceNodes.MODID, value = Dist.CLIENT)
public class ScannerHandler {
    private static final float SCAN_SPEED = 2f;
    private static final int PING_LIFETIME = 200;
    private static final int FADE_TICKS = 40;

    private static final List<ScanResult> activePings = new ArrayList<>();
    private static int scanTickCounter = 0;

    private static boolean isRadialMenuOpen = false;
    private static RadialSelectionScreen radialScreen = null;
    private static boolean isScanning = false;

    private ScannerHandler() {
    }

    private static class ScanResult {
        final BlockPos pos;
        final Component name;
        final double scanDistance;
        boolean playedSound = false;

        ScanResult(BlockPos pos, Component name, double scanDistance) {
            this.pos = pos;
            this.name = name;
            this.scanDistance = scanDistance;
        }
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
        final BlockPos pos;
        final Block block;
        final double distanceSq;

        RawNodeData(BlockPos pos, Block block, double distanceSq) {
            this.pos = pos;
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
                            candidates.add(new RawNodeData(be.getBlockPos(), be.getBlockState().getBlock(), distSq));
                        }
                    }
                }
            }
        }

        CompletableFuture.runAsync(() -> {
            List<ScanResult> results = new ArrayList<>();
            for (RawNodeData node : candidates) {
                if (filterBlocks == null || filterBlocks.isEmpty() || filterBlocks.contains(node.block)) {
                    results.add(new ScanResult(node.pos, node.block.getName(), Math.sqrt(node.distanceSq)));
                }
            }

            int finalFoundCount = results.size();
            Minecraft.getInstance().execute(() -> {
                isScanning = false;
                scanTickCounter = 0;
                activePings.clear();
                activePings.addAll(results);
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

        if (mc.player == null || activePings.isEmpty()) {
            return;
        }

        scanTickCounter++;
        activePings.removeIf(ping -> pingAge(ping) > PING_LIFETIME);
        for (ScanResult ping : activePings) {
            if (pingAge(ping) >= 0 && !ping.playedSound) {
                mc.player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 0.5f, 2.0f);
                ping.playedSound = true;
            }
        }
    }

    // Ticks since the scan wave reached this ping (negative = not reached yet).
    private static double pingAge(ScanResult ping) {
        return scanTickCounter - ping.scanDistance / SCAN_SPEED;
    }

    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || activePings.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos = event.getCamera().getPosition();

        List<ScanResult> renderList = new ArrayList<>(activePings);
        renderList.sort((a, b) -> Double.compare(b.pos.distToCenterSqr(cameraPos), a.pos.distToCenterSqr(cameraPos)));

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (ScanResult ping : renderList) {
            float alpha = calculateAlpha(ping);
            if (alpha > 0.1f) renderPingBeam(poseStack, buffer, cameraPos, ping, alpha);
        }
        BufferUploader.drawWithShader(buffer.end());

        RenderSystem.lineWidth(5.0f);
        buffer.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        for (ScanResult ping : renderList) {
            float alpha = calculateAlpha(ping);
            if (alpha > 0.1f) renderPingBox(poseStack, buffer, cameraPos, ping, alpha);
        }
        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.lineWidth(1.0f);

        if (ResourceNodesConfig.isTextEnabled()) {
            MultiBufferSource.BufferSource textBuffer = mc.renderBuffers().bufferSource();
            for (ScanResult ping : renderList) {
                float alpha = calculateAlpha(ping);
                if (alpha > 0.1f) renderPingText(poseStack, textBuffer, mc, cameraPos, ping, alpha);
            }
            textBuffer.endBatch();
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    private static float calculateAlpha(ScanResult ping) {
        double age = pingAge(ping);
        if (age < 0) return 0.0f;
        float remaining = (float) (PING_LIFETIME - age);
        return remaining < FADE_TICKS ? Math.max(0.0f, remaining / FADE_TICKS) : 1.0f;
    }

    private static void renderPingBeam(PoseStack poseStack, BufferBuilder buffer, Vec3 cameraPos, ScanResult ping, float alpha) {
        poseStack.pushPose();
        poseStack.translate(ping.pos.getX() - cameraPos.x, ping.pos.getY() - cameraPos.y, ping.pos.getZ() - cameraPos.z);
        Matrix4f m = poseStack.last().pose();
        float h = 320.0f, min = 0.3f, max = 0.7f;
        int bottom = (int) (alpha * 100), top = 0;

        vertex(buffer, m, min, 0, min, bottom); vertex(buffer, m, min, h, min, top); vertex(buffer, m, max, h, min, top); vertex(buffer, m, max, 0, min, bottom);
        vertex(buffer, m, min, 0, max, bottom); vertex(buffer, m, max, 0, max, bottom); vertex(buffer, m, max, h, max, top); vertex(buffer, m, min, h, max, top);
        vertex(buffer, m, max, 0, min, bottom); vertex(buffer, m, max, h, min, top); vertex(buffer, m, max, h, max, top); vertex(buffer, m, max, 0, max, bottom);
        vertex(buffer, m, min, 0, min, bottom); vertex(buffer, m, min, 0, max, bottom); vertex(buffer, m, min, h, max, top); vertex(buffer, m, min, h, min, top);
        poseStack.popPose();
    }

    private static void renderPingBox(PoseStack poseStack, BufferBuilder buffer, Vec3 cameraPos, ScanResult ping, float alpha) {
        poseStack.pushPose();
        poseStack.translate(ping.pos.getX() - cameraPos.x, ping.pos.getY() - cameraPos.y, ping.pos.getZ() - cameraPos.z);
        Matrix4f m = poseStack.last().pose();
        int a = (int) (alpha * 255);
        // The 12 edges of the unit cube, as line pairs
        float[][] edges = {
                {0, 0, 0, 1, 0, 0}, {1, 0, 0, 1, 0, 1}, {1, 0, 1, 0, 0, 1}, {0, 0, 1, 0, 0, 0},
                {0, 1, 0, 1, 1, 0}, {1, 1, 0, 1, 1, 1}, {1, 1, 1, 0, 1, 1}, {0, 1, 1, 0, 1, 0},
                {0, 0, 0, 0, 1, 0}, {1, 0, 0, 1, 1, 0}, {1, 0, 1, 1, 1, 1}, {0, 0, 1, 0, 1, 1}
        };
        for (float[] e : edges) {
            vertex(buffer, m, e[0], e[1], e[2], a);
            vertex(buffer, m, e[3], e[4], e[5], a);
        }
        poseStack.popPose();
    }

    private static void vertex(BufferBuilder b, Matrix4f m, float x, float y, float z, int a) {
        b.vertex(m, x, y, z).color(255, 255, 255, a).endVertex();
    }

    private static void renderPingText(PoseStack poseStack, MultiBufferSource buffer, Minecraft mc, Vec3 cameraPos, ScanResult ping, float alpha) {
        BlockPos pos = ping.pos;
        double distance = Math.sqrt(pos.distToCenterSqr(cameraPos));

        poseStack.pushPose();
        poseStack.translate(pos.getX() - cameraPos.x + 0.5, pos.getY() - cameraPos.y + 1.5, pos.getZ() - cameraPos.z + 0.5);
        poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());

        float scale = 0.025F * ResourceNodesConfig.getTextScale() * (float) Math.max(distance, 4.0) / 4.0f;
        poseStack.scale(scale, -scale, scale);

        Component text = Component.literal(ping.name.getString() + " (" + (int) distance + "m)");
        Font font = mc.font;
        int color = 0xFFFFFF | ((int) (alpha * 255) << 24);
        font.drawInBatch(text, -font.width(text) / 2.0f, 0, color, false, poseStack.last().pose(), buffer,
                Font.DisplayMode.SEE_THROUGH, 0, 15728880);

        poseStack.popPose();
    }
}

