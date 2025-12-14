package com.jamming_dino.jd_resource_nodes.client;

import com.jamming_dino.jd_resource_nodes.ResourceNodeData;
import com.jamming_dino.jd_resource_nodes.ResourceNodes;
import com.jamming_dino.jd_resource_nodes.ResourceNodesConfig;
import com.jamming_dino.jd_resource_nodes.block.ResourceNodeBlock;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
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

@EventBusSubscriber(modid = ResourceNodes.MODID, value = Dist.CLIENT)
public class ScannerHandler {

    private static final int SCAN_RADIUS = 128;
    private static final float SCAN_SPEED = 2f;
    private static final int PING_LIFETIME = 200;
    private static final int FADE_TICKS = 40;

    private static final List<ScanResult> activePings = new ArrayList<>();
    private static int scanTickCounter = 0;

    // --- STATE VARIABLES ---
    private static boolean isRadialMenuOpen = false;
    private static RadialSelectionScreen radialScreen = null;

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

            // KEY PRESSED
            if (event.getAction() == 1) {
                if (!isRadialMenuOpen) {
                    // Try to open menu. If it returns false (no unlocks), we DO NOT set isRadialMenuOpen to true.
                    boolean opened = openRadialMenu(mc);
                    if (opened) {
                        isRadialMenuOpen = true;
                    }
                }
            }
            // KEY RELEASED
            else if (event.getAction() == 0) {
                if (isRadialMenuOpen) {
                    closeRadialMenuAndScan(mc);
                    isRadialMenuOpen = false;
                }
            }
        }
    }

    // Returns TRUE if the menu successfully opened, FALSE if it was blocked (empty unlocks)
    private static boolean openRadialMenu(Minecraft mc) {
        if (mc.player == null) return false;

        ScannerUnlockData data = mc.player.getData(ResourceNodes.SCANNER_DATA);
        List<ResourceNodeData> allCategories = ResourceNodeData.getAllCategories();

        // Filter based on unlocked capability
        List<ResourceNodeData> unlockedCategories = new ArrayList<>();
        for (ResourceNodeData cat : allCategories) {
            if (data.isUnlocked(cat.getCategory())) {
                unlockedCategories.add(cat);
            }
        }

        if (unlockedCategories.isEmpty()) {
            mc.player.displayClientMessage(Component.literal("No scanners unlocked! Use /scanner unlock <resource>"), true);
            return false; // Failed to open
        }

        radialScreen = new RadialSelectionScreen();
        radialScreen.setCategories(unlockedCategories);
        mc.setScreen(radialScreen);
        return true; // Successfully opened
    }

    private static void closeRadialMenuAndScan(Minecraft mc) {
        // Only trigger scan if the current screen is actually OUR screen.
        // This prevents scanning if the player closed the menu with ESC earlier.
        if (mc.screen == radialScreen && radialScreen != null) {
            List<Block> selectedBlocks = radialScreen.getSelectedBlocks();
            String categoryName = radialScreen.getSelectedCategoryName();

            mc.setScreen(null); // Close the GUI

            if (!selectedBlocks.isEmpty()) {
                performScan(mc.player, mc.level, null, selectedBlocks, categoryName);
            }
        } else {
            // Safety cleanup
            mc.setScreen(null);
        }
        radialScreen = null;
    }

    // --- Tick, Scan Logic, and Rendering (Unchanged) ---

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

    private static void performScan(Player player, net.minecraft.world.level.Level level, Block filterBlock, List<Block> filterBlocks, String categoryName) {
        activePings.clear();
        scanTickCounter = 0;

        if (filterBlocks != null && !filterBlocks.isEmpty() && categoryName != null) {
            player.displayClientMessage(Component.literal("Scanning for: " + categoryName), true);
        } else if (filterBlock != null) {
            player.displayClientMessage(Component.literal("Scanning for: " + filterBlock.getName().getString()), true);
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
                        boolean matches = false;
                        if (filterBlocks != null && !filterBlocks.isEmpty()) {
                            matches = filterBlocks.contains(state.getBlock());
                        } else if (filterBlock != null) {
                            matches = (filterBlock == state.getBlock());
                        } else {
                            matches = true;
                        }

                        if (!matches) continue;

                        int color = 0xFFFFFF;
                        String name = state.getBlock().getName().getString();
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
            player.playSound(SoundEvents.BEACON_ACTIVATE, 0.5f, 2.0f);
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

        activePings.sort((a, b) -> {
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

        for (ScanResult ping : activePings) {
            float alpha = calculateAlpha(ping);
            if (alpha <= 0.1) continue;
            renderPingBeam(poseStack, buffer, cameraPos, ping, alpha);
        }

        MeshData beamMesh = buffer.build();
        if (beamMesh != null) BufferUploader.drawWithShader(beamMesh);

        RenderSystem.lineWidth(5.0f);
        buffer = tesselator.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);

        for (ScanResult ping : activePings) {
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

            for (ScanResult ping : activePings) {
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