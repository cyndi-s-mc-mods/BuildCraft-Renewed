package buildcraft.neoforge;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Sends packets from the client (kept apart so that dedicated servers never load it). */
final class NeoForgeClientNetwork {
    private NeoForgeClientNetwork() {}

    static void send(CustomPacketPayload payload) {
        ClientPacketDistributor.sendToServer(payload);
    }
}
