package com.jamming_dino.jd_resource_nodes.client;

import com.jamming_dino.jd_resource_nodes.ResourceNodes;
import com.jamming_dino.jd_resource_nodes.block.ResourceNodeBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

final class CustomNodeRenderHelper {
    private CustomNodeRenderHelper() {}

    static void renderNodeInItem(BlockRenderDispatcher blockRenderer, ResourceNodeBlock nodeBlock, BlockState renderState, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        blockRenderer.renderSingleBlock(renderState, poseStack, bufferSource, packedLight, packedOverlay);
        renderOverlay(nodeBlock, poseStack, bufferSource, packedOverlay);
    }

    static void renderNodeInWorld(BlockRenderDispatcher blockRenderer, ResourceNodeBlock nodeBlock, BlockState renderState, BlockAndTintGetter level, BlockPos pos, PoseStack poseStack, MultiBufferSource bufferSource, int packedOverlay) {
        var model = blockRenderer.getBlockModel(renderState);
        blockRenderer.getModelRenderer().tesselateBlock(
                level,
                model,
                renderState,
                pos,
                poseStack,
                bufferSource.getBuffer(ItemBlockRenderTypes.getRenderType(renderState, false)),
                false,
                RandomSource.create(),
                level.getBlockState(pos).getSeed(pos),
                packedOverlay
        );
        renderOverlay(nodeBlock, poseStack, bufferSource, packedOverlay);
    }

    private static void renderOverlay(ResourceNodeBlock nodeBlock, PoseStack poseStack, MultiBufferSource bufferSource, int packedOverlay) {
        ResourceLocation borderTexture = ResourceLocation.fromNamespaceAndPath(ResourceNodes.MODID, "block/input/border_" + nodeBlock.getTier().getSerializedName());
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(borderTexture);
        if (sprite == null) {
            return;
        }

        var builder = bufferSource.getBuffer(Sheets.cutoutBlockSheet());
        poseStack.pushPose();
        poseStack.translate(-0.0125D, -0.0125D, -0.0125D);
        poseStack.scale(1.025F, 1.025F, 1.025F);

        org.joml.Matrix4f matrix = poseStack.last().pose();
        int color = 0xFFFFFFFF;

        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();

        // Down
        addVertex(builder, matrix, 0, 0, 1, u0, v1, color, LightTexture.FULL_BRIGHT, packedOverlay, 0, -1, 0);
        addVertex(builder, matrix, 0, 0, 0, u0, v0, color, LightTexture.FULL_BRIGHT, packedOverlay, 0, -1, 0);
        addVertex(builder, matrix, 1, 0, 0, u1, v0, color, LightTexture.FULL_BRIGHT, packedOverlay, 0, -1, 0);
        addVertex(builder, matrix, 1, 0, 1, u1, v1, color, LightTexture.FULL_BRIGHT, packedOverlay, 0, -1, 0);

        // Up
        addVertex(builder, matrix, 0, 1, 0, u0, v0, color, LightTexture.FULL_BRIGHT, packedOverlay, 0, 1, 0);
        addVertex(builder, matrix, 0, 1, 1, u0, v1, color, LightTexture.FULL_BRIGHT, packedOverlay, 0, 1, 0);
        addVertex(builder, matrix, 1, 1, 1, u1, v1, color, LightTexture.FULL_BRIGHT, packedOverlay, 0, 1, 0);
        addVertex(builder, matrix, 1, 1, 0, u1, v0, color, LightTexture.FULL_BRIGHT, packedOverlay, 0, 1, 0);

        // North
        addVertex(builder, matrix, 1, 1, 0, u1, v0, color, LightTexture.FULL_BRIGHT, packedOverlay, 0, 0, -1);
        addVertex(builder, matrix, 1, 0, 0, u1, v1, color, LightTexture.FULL_BRIGHT, packedOverlay, 0, 0, -1);
        addVertex(builder, matrix, 0, 0, 0, u0, v1, color, LightTexture.FULL_BRIGHT, packedOverlay, 0, 0, -1);
        addVertex(builder, matrix, 0, 1, 0, u0, v0, color, LightTexture.FULL_BRIGHT, packedOverlay, 0, 0, -1);

        // South
        addVertex(builder, matrix, 0, 1, 1, u0, v0, color, LightTexture.FULL_BRIGHT, packedOverlay, 0, 0, 1);
        addVertex(builder, matrix, 0, 0, 1, u0, v1, color, LightTexture.FULL_BRIGHT, packedOverlay, 0, 0, 1);
        addVertex(builder, matrix, 1, 0, 1, u1, v1, color, LightTexture.FULL_BRIGHT, packedOverlay, 0, 0, 1);
        addVertex(builder, matrix, 1, 1, 1, u1, v0, color, LightTexture.FULL_BRIGHT, packedOverlay, 0, 0, 1);

        // West
        addVertex(builder, matrix, 0, 1, 0, u1, v0, color, LightTexture.FULL_BRIGHT, packedOverlay, -1, 0, 0);
        addVertex(builder, matrix, 0, 0, 0, u1, v1, color, LightTexture.FULL_BRIGHT, packedOverlay, -1, 0, 0);
        addVertex(builder, matrix, 0, 0, 1, u0, v1, color, LightTexture.FULL_BRIGHT, packedOverlay, -1, 0, 0);
        addVertex(builder, matrix, 0, 1, 1, u0, v0, color, LightTexture.FULL_BRIGHT, packedOverlay, -1, 0, 0);

        // East
        addVertex(builder, matrix, 1, 1, 1, u1, v0, color, LightTexture.FULL_BRIGHT, packedOverlay, 1, 0, 0);
        addVertex(builder, matrix, 1, 0, 1, u1, v1, color, LightTexture.FULL_BRIGHT, packedOverlay, 1, 0, 0);
        addVertex(builder, matrix, 1, 0, 0, u0, v1, color, LightTexture.FULL_BRIGHT, packedOverlay, 1, 0, 0);
        addVertex(builder, matrix, 1, 1, 0, u0, v0, color, LightTexture.FULL_BRIGHT, packedOverlay, 1, 0, 0);

        poseStack.popPose();
    }

    private static void addVertex(com.mojang.blaze3d.vertex.VertexConsumer builder, org.joml.Matrix4f matrix, float x, float y, float z, float u, float v, int color, int light, int overlay, float normalX, float normalY, float normalZ) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        builder.addVertex(matrix, x, y, z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(normalX, normalY, normalZ);
    }
}


