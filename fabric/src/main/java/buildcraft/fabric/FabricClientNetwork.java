package buildcraft.fabric;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import buildcraft.lib.net.BCNetwork;

/** The client side of {@link FabricNetwork}. */
final class FabricClientNetwork {
    private FabricClientNetwork() {}

    static void register() {
        for (BCNetwork.PacketType<?> type : BCNetwork.toClientTypes()) {
            registerReceiver(type);
        }
    }

    private static <T extends CustomPacketPayload> void registerReceiver(BCNetwork.PacketType<T> type) {
        ClientPlayNetworking.registerGlobalReceiver(type.type(), (payload, context) -> BCNetwork.handleOnClient(payload));
    }

    static void send(CustomPacketPayload payload) {
        ClientPlayNetworking.send(payload);
    }
}
