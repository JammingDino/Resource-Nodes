package com.jamming_dino.jd_resource_nodes;

import com.jamming_dino.jd_resource_nodes.client.ConfigScreen;
import com.jamming_dino.jd_resource_nodes.client.ResourceNodeRenderer;
import com.jamming_dino.jd_resource_nodes.client.ResourceNodesKeys;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod.EventBusSubscriber(modid = ResourceNodes.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ResourceNodesClient {
    private ResourceNodesClient() {
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        // Some client setup code
        ResourceNodes.LOGGER.info("HELLO FROM CLIENT SETUP");
        ResourceNodes.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new ConfigScreen(parent)));
    }

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ResourceNodes.RESOURCE_NODE_BE.get(), ResourceNodeRenderer::new);
    }

    @SubscribeEvent
    static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        ResourceNodesKeys.registerKeys(event);
    }
}