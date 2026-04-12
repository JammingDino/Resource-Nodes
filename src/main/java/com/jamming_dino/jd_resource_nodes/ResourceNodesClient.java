package com.jamming_dino.jd_resource_nodes;

import com.jamming_dino.jd_resource_nodes.client.ConfigScreen;
import com.jamming_dino.jd_resource_nodes.client.CustomNodeItemRenderer;
import com.jamming_dino.jd_resource_nodes.client.ResourceNodeRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import java.util.ArrayList;
import java.util.List;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = ResourceNodes.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = ResourceNodes.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ResourceNodesClient {
    public ResourceNodesClient(ModContainer container) {
        // Register our custom config screen
        container.registerExtensionPoint(IConfigScreenFactory.class, (minecraft, parent) -> new ConfigScreen(parent));
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code
        ResourceNodes.LOGGER.info("HELLO FROM CLIENT SETUP");
        ResourceNodes.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ResourceNodes.RESOURCE_NODE_BE.get(), ResourceNodeRenderer::new);
    }

    @SubscribeEvent
    static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        List<Item> customNodeItems = new ArrayList<>();
        for (var holder : ResourceNodes.REGISTERED_NODES) {
            if (holder.getId().getPath().startsWith("node_custom_")) {
                customNodeItems.add(holder.get().asItem());
            }
        }

        if (!customNodeItems.isEmpty()) {
            event.registerItem(new IClientItemExtensions() {
                @Override
                public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                    return CustomNodeItemRenderer.getInstance();
                }
            }, customNodeItems.toArray(Item[]::new));
        }
    }
}