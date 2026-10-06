package buildcraft.fabric;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import buildcraft.lib.net.BCNetwork;

/** Registers BuildCraft's packets with Fabric's networking API. */
final class FabricNetwork {
    private FabricNetwork() {}

    static void register() {
        for (BCNetwork.PacketType<?> type : BCNetwork.toServerTypes()) {
            registerToServer(type);
        }
        for (BCNetwork.PacketType<?> type : BCNetwork.toClientTypes()) {
            registerToClient(type);
        }
    }

    private static <T extends CustomPacketPayload> void registerToServer(BCNetwork.PacketType<T> type) {
        PayloadTypeRegistry.serverboundPlay().register(type.type(), type.codec());
        ServerPlayNetworking.registerGlobalReceiver(type.type(), (payload, context) -> BCNetwork.handleOnServer(payload, context.player()));
    }

    private static <T extends CustomPacketPayload> void registerToClient(BCNetwork.PacketType<T> type) {
        StreamCodec<RegistryFriendlyByteBuf, T> codec = type.codec();
        PayloadTypeRegistry.clientboundPlay().register(type.type(), codec);
    }
}
