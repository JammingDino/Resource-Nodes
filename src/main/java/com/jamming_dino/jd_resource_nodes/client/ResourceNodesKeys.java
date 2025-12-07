package com.jamming_dino.jd_resource_nodes.client;

import com.jamming_dino.jd_resource_nodes.ResourceNodes;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

// Annotation removed to avoid deprecation warnings. Registered in main class instead.
public class ResourceNodesKeys {

    public static final KeyMapping PING_KEY = new KeyMapping(
            "key.jd_resource_nodes.ping", // Lang key
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V, // Default Key 'V'
            "key.categories.jd_resource_nodes" // Category
    );

    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(PING_KEY);
    }
}