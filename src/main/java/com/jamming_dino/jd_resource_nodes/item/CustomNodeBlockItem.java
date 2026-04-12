package com.jamming_dino.jd_resource_nodes.item;

import com.jamming_dino.jd_resource_nodes.ResourceNodeTier;
import com.jamming_dino.jd_resource_nodes.block.ResourceNodeBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class CustomNodeBlockItem extends BlockItem {
    public CustomNodeBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        Block block = this.getBlock();
        if (block instanceof ResourceNodeBlock nodeBlock) {
            if (BuiltInRegistries.BLOCK.getKey(block).getPath().startsWith("node_custom_")) {
                Component outputName = new ItemStack(nodeBlock.getOutputItem()).getHoverName();
                ResourceNodeTier tier = nodeBlock.getTier();
                return outputName.copy().append(" ").append(Component.translatable("item.jd_resource_nodes.purity." + tier.getSerializedName()));
            }
        }
        return super.getName(stack);
    }
}


