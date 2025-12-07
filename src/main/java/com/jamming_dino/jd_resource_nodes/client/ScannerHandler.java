package com.jamming_dino.jd_resource_nodes.client;

import com.jamming_dino.jd_resource_nodes.ResourceNodes;
import com.jamming_dino.jd_resource_nodes.block.ResourceNodeBlock;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@EventBusSubscriber(modid = ResourceNodes.MODID, value = Dist.CLIENT)
public class ScannerHandler {

    private static final int SCAN_RADIUS = 128;
    private static final float SCAN_SPEED = 2f;     // Blocks per tick
    private static final int PING_LIFETIME = 200;   // How long a node stays visible after being found (10s)
    private static final int FADE_TICKS = 40;       // How long the fade out lasts (2s)

    private static final List<ScanResult> activePings = new ArrayList<>();
    private static int scanTickCounter = 0; // Master clock for the current scan

    // scanDistance: Distance from origin when scan started (for Wave timing)
    private record ScanResult(BlockPos pos, Component name, int color, double scanDistance) {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // If we have active pings, progress the scan time
        if (!activePings.isEmpty()) {
            scanTickCounter++;

            // Cleanup: Remove nodes that have exceeded their lifetime
            activePings.removeIf(ping -> {
                double ticksToReach = ping.scanDistance / SCAN_SPEED;
                double age = scanTickCounter - ticksToReach;
                return age > PING_LIFETIME;
            });
        }

        while (ResourceNodesKeys.PING_KEY.consumeClick()) {
            performScan(mc.player, mc.level);
        }
    }

    private static void performScan(Player player, net.minecraft.world.level.Level level) {
        activePings.clear();
        scanTickCounter = 0;

        ItemStack offhand = player.getOffhandItem();
        Block filterBlock = null;
        if (offhand.getItem() instanceof BlockItem bi && bi.getBlock() instanceof ResourceNodeBlock) {
            filterBlock = bi.getBlock();
            player.displayClientMessage(Component.literal("Scanning for: " + offhand.getHoverName().getString()), true);
        } else {
            player.displayClientMessage(Component.literal("Scanning for all Nodes..."), true);
        }

        BlockPos playerPos = player.blockPosition();
        int foundCount = 0;
        Vec3 pVec = player.position();

        for (int x = -SCAN_RADIUS; x <= SCAN_RADIUS; x++) {
            for (int y = -32; y <= 32; y++) {
                for (int z = -SCAN_RADIUS; z <= SCAN_RADIUS; z++) {
                    BlockPos targetPos = playerPos.offset(x, y, z);
                    BlockState state = level.getBlockState(targetPos);

                    if (state.getBlock() instanceof ResourceNodeBlock) {
                        if (filterBlock != null && filterBlock != state.getBlock()) continue;

                        int color = 0xFFFFFF;
                        String name = state.getBlock().getName().getString();

                        // Calculate distance from scan origin for the Wave Logic
                        double dist = Math.sqrt(targetPos.distToCenterSqr(pVec));

                        activePings.add(new ScanResult(targetPos, Component.literal(name), color, dist));
                        foundCount++;
                    }
                }
            }
        }

        if (foundCount == 0) {
            player.displayClientMessage(Component.literal("No nodes found nearby."), true);
        } else {
            player.displayClientMessage(Component.literal("Found " + foundCount + " nodes!"), true);
        }
    }

    @SubscribeEvent
    public static void onRenderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        if (activePings.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos = event.getCamera().getPosition();

        // SORTING FIX:
        // We must sort based on the distance to the *current camera*, not the scan origin.
        // This ensures transparency renders correctly as you move around.
        activePings.sort((a, b) -> {
            double distA = a.pos.distToCenterSqr(cameraPos);
            double distB = b.pos.distToCenterSqr(cameraPos);
            // Sort Descending (Far -> Close) for painter's algorithm
            return Double.compare(distB, distA);
        });

        // --- GLOBAL SETUP ---
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.lineWidth(5.0f);

        Tesselator tesselator = Tesselator.getInstance();

        // --- PHASE 1: DRAW BOXES ---
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        for (ScanResult ping : activePings) {
            float alpha = calculateAlpha(ping);
            if (alpha <= 0.1) continue;

            renderPingBox(poseStack, tesselator, cameraPos, ping, alpha);
        }

        // --- PHASE 2: DRAW TEXT ---
        RenderSystem.setShader(() -> null);
        MultiBufferSource.BufferSource buffer = MultiBufferSource.immediate(new ByteBufferBuilder(1536));

        for (ScanResult ping : activePings) {
            float alpha = calculateAlpha(ping);
            if (alpha <= 0.1) continue;

            renderPingText(poseStack, buffer, mc, cameraPos, ping, alpha);
        }

        buffer.endBatch();

        // --- RESTORE STATE ---
        RenderSystem.lineWidth(1.0f);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    private static float calculateAlpha(ScanResult ping) {
        // Uses the static 'scanDistance' so the wave logic remains consistent
        // regardless of where the player moves after scanning.
        double ticksToReach = ping.scanDistance / SCAN_SPEED;
        double age = scanTickCounter - ticksToReach;

        if (age < 0) return 0.0f; // Wave hasn't hit yet

        float remaining = (float)(PING_LIFETIME - age);
        if (remaining < FADE_TICKS) {
            return Math.max(0.0f, remaining / (float)FADE_TICKS);
        }

        return 1.0f;
    }

    private static void renderPingBox(PoseStack poseStack, Tesselator tesselator, Vec3 cameraPos, ScanResult ping, float alpha) {
        BlockPos pos = ping.pos;
        double dx = pos.getX() - cameraPos.x;
        double dy = pos.getY() - cameraPos.y;
        double dz = pos.getZ() - cameraPos.z;

        poseStack.pushPose();
        poseStack.translate(dx, dy, dz);
        Matrix4f matrix = poseStack.last().pose();

        int r = 255;
        int g = 255;
        int b = 255;
        int a = (int)(alpha * 255);

        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

        float min = -0.00f;
        float max = 1.00f;

        // Box Vertices
        // Bottom Square
        vertex(buffer, matrix, min, min, min, r, g, b, a); vertex(buffer, matrix, max, min, min, r, g, b, a);
        vertex(buffer, matrix, max, min, min, r, g, b, a); vertex(buffer, matrix, max, min, max, r, g, b, a);
        vertex(buffer, matrix, max, min, max, r, g, b, a); vertex(buffer, matrix, min, min, max, r, g, b, a);
        vertex(buffer, matrix, min, min, max, r, g, b, a); vertex(buffer, matrix, min, min, min, r, g, b, a);

        // Top Square
        vertex(buffer, matrix, min, max, min, r, g, b, a); vertex(buffer, matrix, max, max, min, r, g, b, a);
        vertex(buffer, matrix, max, max, min, r, g, b, a); vertex(buffer, matrix, max, max, max, r, g, b, a);
        vertex(buffer, matrix, max, max, max, r, g, b, a); vertex(buffer, matrix, min, max, max, r, g, b, a);
        vertex(buffer, matrix, min, max, max, r, g, b, a); vertex(buffer, matrix, min, max, min, r, g, b, a);

        // Vertical Pillars
        vertex(buffer, matrix, min, min, min, r, g, b, a); vertex(buffer, matrix, min, max, min, r, g, b, a);
        vertex(buffer, matrix, max, min, min, r, g, b, a); vertex(buffer, matrix, max, max, min, r, g, b, a);
        vertex(buffer, matrix, max, min, max, r, g, b, a); vertex(buffer, matrix, max, max, max, r, g, b, a);
        vertex(buffer, matrix, min, min, max, r, g, b, a); vertex(buffer, matrix, min, max, max, r, g, b, a);

        BufferUploader.drawWithShader(buffer.buildOrThrow());
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

        // CALCULATE CURRENT DISTANCE (Every Frame)
        // This ensures the scale and text label update as you move.
        double currentDistSq = pos.distToCenterSqr(cameraPos);
        double currentDistance = Math.sqrt(currentDistSq);

        poseStack.pushPose();
        poseStack.translate(dx + 0.5, dy + 1.5, dz + 0.5);
        poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());

        // DYNAMIC SCALE (Using Current Distance)
        float baseScale = 0.025F;
        float constantScale = baseScale * (float)Math.max(currentDistance, 4.0) / 4.0f;
        poseStack.scale(constantScale, -constantScale, constantScale);

        // TEXT UPDATE (Using Current Distance)
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