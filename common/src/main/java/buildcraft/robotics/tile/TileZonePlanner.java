/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.robotics.tile;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;

import buildcraft.core.item.ItemMapLocation;
import buildcraft.core.item.ItemPaintbrush;
import buildcraft.core.item.MapLocation;
import buildcraft.lib.inventory.ItemHandlerSimple;
import buildcraft.lib.misc.ZonePlan;
import buildcraft.lib.net.BCNetwork;
import buildcraft.lib.tile.TileBC;
import buildcraft.robotics.BCRoboticsBlocks;
import buildcraft.robotics.container.ContainerZonePlanner;
import buildcraft.robotics.zone.ZoneMapColours;
import buildcraft.robotics.zone.ZonePackets;

/** Shows a map of the area around it, on which zones can be painted in each of the 16 colours (holding a paintbrush of
 * that colour). Zones are saved to map locations (a paintbrush of the colour in the right hand slot, and a map location
 * below it) and loaded from them (the slots at the bottom left). */
public class TileZonePlanner extends TileBC implements MenuProvider {
    /** How far (in chunks) from the planner the map reaches. */
    public static final int MAP_RADIUS = 16;
    /** The largest area that can be painted at once. */
    private static final int MAX_PAINT = 256;
    private static final int COPY_TIME = 100;
    private static final int MAX_QUEUED = 1024;
    private static final int MAP_CHUNKS_PER_TICK = 4;

    public final ItemHandlerSimple invPaintbrushes = new ItemHandlerSimple(16, (slot, stack) -> stack.getItem() instanceof ItemPaintbrush,
        this::setChanged);
    public final ItemHandlerSimple invInputPaintbrush = new ItemHandlerSimple(1, (slot, stack) -> stack.getItem() instanceof ItemPaintbrush,
        this::setChanged);
    public final ItemHandlerSimple invInputMap = new ItemHandlerSimple(1, (slot, stack) -> isZoneMap(stack), this::setChanged);
    public final ItemHandlerSimple invInputResult = new ItemHandlerSimple(1, (slot, stack) -> false, this::setChanged);
    public final ItemHandlerSimple invOutputPaintbrush = new ItemHandlerSimple(1, (slot, stack) -> stack.getItem() instanceof ItemPaintbrush,
        this::setChanged);
    public final ItemHandlerSimple invOutputMap = new ItemHandlerSimple(1, (slot, stack) -> stack.getItem() instanceof ItemMapLocation,
        this::setChanged);
    public final ItemHandlerSimple invOutputResult = new ItemHandlerSimple(1, (slot, stack) -> false, this::setChanged);

    /** The zone of each colour, indexed by {@link DyeColor#getId()}. */
    private final ZonePlan[] layers = new ZonePlan[16];
    private int progressInput = 0, progressOutput = 0;

    private record MapRequest(UUID player, long chunk) {}

    private final Deque<MapRequest> mapRequests = new ArrayDeque<>();

    public TileZonePlanner(BlockPos pos, BlockState state) {
        super(BCRoboticsBlocks.ZONE_PLANNER_TILE.get(), pos, state);
        java.util.Arrays.fill(layers, ZonePlan.EMPTY);
    }

    private static boolean isZoneMap(ItemStack stack) {
        MapLocation location = ItemMapLocation.get(stack);
        return stack.getItem() instanceof ItemMapLocation && location != null && location.type() == MapLocation.Type.ZONE;
    }

    /** @return True if the given (carried) item is a paintbrush of the given colour. */
    public static boolean canPaint(ItemStack held, int colour) {
        if (!(held.getItem() instanceof ItemPaintbrush)) return false;
        DyeColor brush = ItemPaintbrush.getColour(held);
        return brush != null && brush.getId() == colour;
    }

    public ZonePlan getLayer(int colour) {
        return layers[colour];
    }

    public int getProgressInput() {
        return progressInput;
    }

    public int getProgressOutput() {
        return progressOutput;
    }

    public void paint(int colour, int x0, int z0, int x1, int z1, boolean add) {
        if (colour < 0 || colour >= 16) return;
        if (Math.abs(x1 - x0) >= MAX_PAINT || Math.abs(z1 - z0) >= MAX_PAINT) return;
        int limit = MAP_RADIUS * 16 + 16;
        for (int v : new int[] { x0 - worldPosition.getX(), x1 - worldPosition.getX(), z0 - worldPosition.getZ(), z1 - worldPosition.getZ() }) {
            if (Math.abs(v) > limit) return;
        }
        layers[colour] = layers[colour].withArea(x0, z0, x1, z1, add);
        setChanged();
        sendNetworkUpdate();
    }

    public void queueMapRequests(ServerPlayer player, List<Long> chunks) {
        ChunkPos centre = ChunkPos.containing(worldPosition);
        for (long chunk : chunks) {
            if (mapRequests.size() >= MAX_QUEUED) return;
            if (Math.abs(ChunkPos.getX(chunk) - centre.x()) > MAP_RADIUS || Math.abs(ChunkPos.getZ(chunk) - centre.z()) > MAP_RADIUS) continue;
            mapRequests.add(new MapRequest(player.getUUID(), chunk));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level instanceof ServerLevel serverLevel)) return;
        for (int i = 0; i < MAP_CHUNKS_PER_TICK && !mapRequests.isEmpty(); i++) {
            MapRequest request = mapRequests.removeFirst();
            if (serverLevel.getPlayerByUUID(request.player()) instanceof ServerPlayer player
                && player.containerMenu instanceof ContainerZonePlanner menu && menu.tile == this) {
                int[] colours = ZoneMapColours.compute(serverLevel, ChunkPos.getX(request.chunk()), ChunkPos.getZ(request.chunk()));
                BCNetwork.sendToPlayer(player, new ZonePackets.MapData(request.chunk(), colours));
            }
        }
        tickInput();
        tickOutput();
    }

    /** Loads the zone on the map location into the layer of the paintbrush's colour. */
    private void tickInput() {
        DyeColor colour = ItemPaintbrush.getColour(invInputPaintbrush.getItem(0));
        ItemStack map = invInputMap.getItem(0);
        if (colour == null || !isZoneMap(map) || !invInputResult.getItem(0).isEmpty()) {
            progressInput = 0;
            return;
        }
        if (++progressInput < COPY_TIME) return;
        progressInput = 0;
        MapLocation location = ItemMapLocation.get(map);
        if (location == null) return;
        layers[colour.getId()] = location.zone();
        ItemStack clean = map.copyWithCount(1);
        ItemMapLocation.set(clean, null);
        invInputMap.setItem(0, ItemStack.EMPTY);
        invInputResult.setItem(0, clean);
        setChanged();
        sendNetworkUpdate();
    }

    /** Saves the layer of the paintbrush's colour onto the map location. */
    private void tickOutput() {
        DyeColor colour = ItemPaintbrush.getColour(invOutputPaintbrush.getItem(0));
        ItemStack map = invOutputMap.getItem(0);
        if (colour == null || map.isEmpty() || !invOutputResult.getItem(0).isEmpty()) {
            progressOutput = 0;
            return;
        }
        if (++progressOutput < COPY_TIME) return;
        progressOutput = 0;
        ItemStack written = map.copyWithCount(1);
        ItemMapLocation.set(written, MapLocation.zone(layers[colour.getId()]));
        map.shrink(1);
        invOutputMap.setItem(0, map);
        invOutputResult.setItem(0, written);
        setChanged();
    }

    public static int copyTime() {
        return COPY_TIME;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        invPaintbrushes.save(output, "paintbrushes");
        invInputPaintbrush.save(output, "inputPaintbrush");
        invInputMap.save(output, "inputMap");
        invInputResult.save(output, "inputResult");
        invOutputPaintbrush.save(output, "outputPaintbrush");
        invOutputMap.save(output, "outputMap");
        invOutputResult.save(output, "outputResult");
        List<ZonePlan> list = new ArrayList<>(List.of(layers));
        output.store("layers", ZonePlan.CODEC.listOf(), list);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        invPaintbrushes.load(input, "paintbrushes");
        invInputPaintbrush.load(input, "inputPaintbrush");
        invInputMap.load(input, "inputMap");
        invInputResult.load(input, "inputResult");
        invOutputPaintbrush.load(input, "outputPaintbrush");
        invOutputMap.load(input, "outputMap");
        invOutputResult.load(input, "outputResult");
        List<ZonePlan> list = input.read("layers", ZonePlan.CODEC.listOf()).orElse(List.of());
        for (int i = 0; i < layers.length; i++) {
            layers[i] = i < list.size() ? list.get(i) : ZonePlan.EMPTY;
        }
    }

    @Override
    public InteractionResult onActivated(Player player, InteractionHand hand, ItemStack held, BlockHitResult hit) {
        if (!isClient()) {
            player.openMenu(this);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ContainerZonePlanner(id, inventory, this);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level == null) return;
        for (ItemHandlerSimple inv : List.of(invPaintbrushes, invInputPaintbrush, invInputMap, invInputResult, invOutputPaintbrush,
            invOutputMap, invOutputResult)) {
            Containers.dropContents(level, pos, inv);
        }
    }
}
