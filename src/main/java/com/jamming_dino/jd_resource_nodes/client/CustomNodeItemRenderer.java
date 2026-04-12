package com.jamming_dino.jd_resource_nodes.client;

import com.jamming_dino.jd_resource_nodes.block.ResourceNodeBlock;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class CustomNodeItemRenderer extends BlockEntityWithoutLevelRenderer {

    private static CustomNodeItemRenderer instance;

    private CustomNodeItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static CustomNodeItemRenderer getInstance() {
        if (instance == null) {
            instance = new CustomNodeItemRenderer();
        }
        return instance;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof ResourceNodeBlock nodeBlock) {
            poseStack.pushPose();

            // BEWLR items render in block-space units; center+halve to match vanilla held block scale.
            poseStack.translate(0.25D, 0.25D, 0.25D);
            poseStack.scale(0.5F, 0.5F, 0.5F);

            // Display transform is already applied by the item renderer context.
            int effectiveLight = packedLight == 0 ? LightTexture.FULL_BRIGHT : packedLight;

            CustomNodeRenderHelper.renderNodeInItem(
                    Minecraft.getInstance().getBlockRenderer(),
                    nodeBlock,
                    nodeBlock.getReadyBlock() != null ? nodeBlock.getReadyBlock().defaultBlockState() : nodeBlock.defaultBlockState(),
                    poseStack,
                    buffer,
                    effectiveLight,
                    packedOverlay
            );
            poseStack.popPose();
        }
    }
}
