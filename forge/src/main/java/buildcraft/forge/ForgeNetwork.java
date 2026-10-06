package buildcraft.forge;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.payload.PayloadFlow;

import buildcraft.BuildCraft;
import buildcraft.lib.net.BCNetwork;

/** BuildCraft's network channel on Forge. */
final class ForgeNetwork {
    static Channel<CustomPacketPayload> channel;

    private ForgeNetwork() {}

    static void register() {
        PayloadFlow<RegistryFriendlyByteBuf, CustomPacketPayload> flow = ChannelBuilder.named(BuildCraft.id("main"))
            .networkProtocolVersion(1).payloadChannel().play().serverbound();
        for (BCNetwork.PacketType<?> type : BCNetwork.toServerTypes()) {
            flow = addToServer(flow, type);
        }
        flow = flow.clientbound();
        for (BCNetwork.PacketType<?> type : BCNetwork.toClientTypes()) {
            flow = addToClient(flow, type);
        }
        channel = flow.build();
    }

    private static <T extends CustomPacketPayload> PayloadFlow<RegistryFriendlyByteBuf, CustomPacketPayload> addToServer(
        PayloadFlow<RegistryFriendlyByteBuf, CustomPacketPayload> flow, BCNetwork.PacketType<T> type) {
        return flow.addMain(type.type(), type.codec(), (payload, context) -> {
            if (context.getSender() != null) BCNetwork.handleOnServer(payload, context.getSender());
        });
    }

    private static <T extends CustomPacketPayload> PayloadFlow<RegistryFriendlyByteBuf, CustomPacketPayload> addToClient(
        PayloadFlow<RegistryFriendlyByteBuf, CustomPacketPayload> flow, BCNetwork.PacketType<T> type) {
        return flow.addMain(type.type(), type.codec(), (payload, context) -> BCNetwork.handleOnClient(payload));
    }
}
