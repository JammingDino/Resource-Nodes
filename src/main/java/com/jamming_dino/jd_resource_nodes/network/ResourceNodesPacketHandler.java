package com.jamming_dino.jd_resource_nodes.network;

import com.jamming_dino.jd_resource_nodes.ResourceNodes;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ResourceNodesPacketHandler {
    public static void register(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(ResourceNodes.MODID)
                .versioned("1.0.0");

        registrar.playToServer(
                RequestNodeSettingsPacket.TYPE,
                RequestNodeSettingsPacket.STREAM_CODEC,
                RequestNodeSettingsPacket::handle
        );

        registrar.playToServer(
                UpdateNodeTogglePacket.TYPE,
                UpdateNodeTogglePacket.STREAM_CODEC,
                UpdateNodeTogglePacket::handle
        );

        registrar.playToServer(
                AddCustomNodeConfigPacket.TYPE,
                AddCustomNodeConfigPacket.STREAM_CODEC,
                AddCustomNodeConfigPacket::handle
        );

        registrar.playToClient(
                SyncScannerUnlocksPacket.TYPE,
                SyncScannerUnlocksPacket.STREAM_CODEC,
                SyncScannerUnlocksPacket::handle
        );

        registrar.playToClient(
                SyncNodeSettingsPacket.TYPE,
                SyncNodeSettingsPacket.STREAM_CODEC,
                SyncNodeSettingsPacket::handle
        );

        registrar.playToClient(
                SyncCustomNodeIdsPacket.TYPE,
                SyncCustomNodeIdsPacket.STREAM_CODEC,
                SyncCustomNodeIdsPacket::handle
        );

        registrar.playToClient(
                OpenNodeManagerScreenPacket.TYPE,
                OpenNodeManagerScreenPacket.STREAM_CODEC,
                OpenNodeManagerScreenPacket::handle
        );
    }
}