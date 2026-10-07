package com.jamming_dino.jd_resource_nodes.network;

import com.jamming_dino.jd_resource_nodes.client.ClientScreenOpener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenNodeManagerScreenPacket {
    public static final OpenNodeManagerScreenPacket INSTANCE = new OpenNodeManagerScreenPacket();

    public static OpenNodeManagerScreenPacket decode(FriendlyByteBuf buffer) {
        return INSTANCE;
    }

    public static void encode(OpenNodeManagerScreenPacket packet, FriendlyByteBuf buffer) {
    }

    public static void handle(OpenNodeManagerScreenPacket payload, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getDirection().getReceptionSide().isClient()) {
                ClientScreenOpener.openNodeManagerScreen();
            }
        });
        context.setPacketHandled(true);
    }
}
