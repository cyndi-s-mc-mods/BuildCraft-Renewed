/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.net;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import buildcraft.BuildCraft;
import buildcraft.lib.platform.Platform;

/** BuildCraft's own packets. They are declared here (during mod construction) and each loader registers them with its
 * own networking API. Handlers run on the main thread of the receiving side. */
public final class BCNetwork {
    /** A packet type, with the handler for when the server receives it (null for packets sent to clients). */
    public record PacketType<T extends CustomPacketPayload>(CustomPacketPayload.Type<T> type,
        StreamCodec<RegistryFriendlyByteBuf, T> codec, BiConsumer<T, ServerPlayer> serverHandler) {}

    private static final List<PacketType<?>> TO_SERVER = new ArrayList<>();
    private static final List<PacketType<?>> TO_CLIENT = new ArrayList<>();
    /** Set by the client when it starts, so that no client code is loaded on dedicated servers. */
    private static final Map<CustomPacketPayload.Type<?>, Consumer<?>> CLIENT_HANDLERS = new HashMap<>();

    private BCNetwork() {}

    public static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> type(String name) {
        return new CustomPacketPayload.Type<>(BuildCraft.id(name));
    }

    public static <T extends CustomPacketPayload> void toServer(CustomPacketPayload.Type<T> type,
        StreamCodec<RegistryFriendlyByteBuf, T> codec, BiConsumer<T, ServerPlayer> handler) {
        TO_SERVER.add(new PacketType<>(type, codec, handler));
    }

    public static <T extends CustomPacketPayload> void toClient(CustomPacketPayload.Type<T> type,
        StreamCodec<RegistryFriendlyByteBuf, T> codec) {
        TO_CLIENT.add(new PacketType<>(type, codec, (payload, player) -> {}));
    }

    /** Called by the client's setup to say what to do with a packet sent to it. */
    public static <T extends CustomPacketPayload> void setClientHandler(CustomPacketPayload.Type<T> type, Consumer<T> handler) {
        CLIENT_HANDLERS.put(type, handler);
    }

    public static List<PacketType<?>> toServerTypes() {
        return Collections.unmodifiableList(TO_SERVER);
    }

    public static List<PacketType<?>> toClientTypes() {
        return Collections.unmodifiableList(TO_CLIENT);
    }

    /** Called by the loaders when a packet reaches the client. */
    @SuppressWarnings("unchecked")
    public static <T extends CustomPacketPayload> void handleOnClient(T payload) {
        Consumer<T> handler = (Consumer<T>) CLIENT_HANDLERS.get(payload.type());
        if (handler != null) handler.accept(payload);
    }

    /** Called by the loaders when a packet reaches the server. */
    @SuppressWarnings("unchecked")
    public static <T extends CustomPacketPayload> void handleOnServer(T payload, ServerPlayer player) {
        for (PacketType<?> type : TO_SERVER) {
            if (type.type().equals(payload.type())) {
                ((PacketType<T>) type).serverHandler().accept(payload, player);
                return;
            }
        }
    }

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        Platform.INSTANCE.sendToPlayer(player, payload);
    }

    public static void sendToServer(CustomPacketPayload payload) {
        Platform.INSTANCE.sendToServer(payload);
    }
}
