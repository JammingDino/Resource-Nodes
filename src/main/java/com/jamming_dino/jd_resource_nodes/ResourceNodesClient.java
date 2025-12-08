package com.jamming_dino.jd_resource_nodes;

import com.jamming_dino.jd_resource_nodes.client.ConfigScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = ResourceNodes.MODID, dist = Dist.CLIENT)
// You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
@EventBusSubscriber(modid = ResourceNodes.MODID, value = Dist.CLIENT)
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
}