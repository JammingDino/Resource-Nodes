package com.jamming_dino.jd_resource_nodes.network;

import com.jamming_dino.jd_resource_nodes.ResourceNodes;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ResourceNodesPacketHandler {
    public static void register(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(ResourceNodes.MODID)
                .versioned("1.0.0");

        registrar.playToClient(
                SyncScannerUnlocksPacket.TYPE,
                SyncScannerUnlocksPacket.STREAM_CODEC,
                SyncScannerUnlocksPacket::handle
        );
    }
}