package com.jamming_dino.jd_resource_nodes.client;

import com.jamming_dino.jd_resource_nodes.ResourceNodes;
import com.jamming_dino.jd_resource_nodes.block.ResourceNodeBlock;
import com.jamming_dino.jd_resource_nodes.block.entity.ResourceNodeBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;

public class ResourceNodeRenderer implements BlockEntityRenderer<ResourceNodeBlockEntity> {
    private final BlockRenderDispatcher blockRenderer;

    public ResourceNodeRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(ResourceNodeBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = blockEntity.getBlockState();
        if (!(state.getBlock() instanceof ResourceNodeBlock nodeBlock)) return;

        ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(nodeBlock);
        boolean isCustom = id != null && id.getPath().startsWith("node_custom_");
        
        // If not custom, it has a JSON model, so we don't need to BER it.
        // Unless it's regenerating? Actually JSON model handles regenerating too for normal nodes.
        if (!isCustom) {
            return; 
        }

        boolean regenerating = state.getValue(ResourceNodeBlock.REGENERATING);
        Block renderBlock = regenerating ? nodeBlock.getBaseBlock() : nodeBlock.getReadyBlock();
        if (renderBlock == null || renderBlock == Blocks.AIR) return;
        
        BlockState renderState = renderBlock.defaultBlockState();
        if (blockEntity.getLevel() != null) {
            CustomNodeRenderHelper.renderNodeInWorld(
                    blockRenderer,
                    nodeBlock,
                    renderState,
                    blockEntity.getLevel(),
                    blockEntity.getBlockPos(),
                    poseStack,
                    bufferSource,
                    packedOverlay
            );
        }
    }
}

