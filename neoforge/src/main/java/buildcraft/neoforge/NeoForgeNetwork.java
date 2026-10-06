package buildcraft.neoforge;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import buildcraft.lib.net.BCNetwork;

/** Registers BuildCraft's packets with NeoForge. */
final class NeoForgeNetwork {
    private NeoForgeNetwork() {}

    static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        for (BCNetwork.PacketType<?> type : BCNetwork.toServerTypes()) {
            registerToServer(registrar, type);
        }
        for (BCNetwork.PacketType<?> type : BCNetwork.toClientTypes()) {
            registerToClient(registrar, type);
        }
    }

    private static <T extends CustomPacketPayload> void registerToServer(PayloadRegistrar registrar, BCNetwork.PacketType<T> type) {
        registrar.playToServer(type.type(), type.codec(), (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) BCNetwork.handleOnServer(payload, player);
        });
    }

    private static <T extends CustomPacketPayload> void registerToClient(PayloadRegistrar registrar, BCNetwork.PacketType<T> type) {
        registrar.playToClient(type.type(), type.codec(), (payload, context) -> BCNetwork.handleOnClient(payload));
    }
}
