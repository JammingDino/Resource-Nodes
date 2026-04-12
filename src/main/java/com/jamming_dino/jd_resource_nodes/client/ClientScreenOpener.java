package com.jamming_dino.jd_resource_nodes.client;

import net.minecraft.client.Minecraft;

public class ClientScreenOpener {
    private ClientScreenOpener() {
    }

    public static void openNodeManagerScreen() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.setScreen(new NodeManagerScreen());
        }
    }
}

