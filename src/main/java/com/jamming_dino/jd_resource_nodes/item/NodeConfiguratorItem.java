package com.jamming_dino.jd_resource_nodes.item;

import com.jamming_dino.jd_resource_nodes.network.OpenNodeManagerScreenPacket;
import com.jamming_dino.jd_resource_nodes.network.ResourceNodesPacketHandler;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class NodeConfiguratorItem extends Item {
    public NodeConfiguratorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);

        if (!level.isClientSide && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            ResourceNodesPacketHandler.sendToPlayer(serverPlayer, OpenNodeManagerScreenPacket.INSTANCE);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}


