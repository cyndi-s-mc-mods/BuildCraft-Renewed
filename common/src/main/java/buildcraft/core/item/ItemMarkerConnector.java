/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core.item;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import buildcraft.core.BCCoreComponents;
import buildcraft.core.BCCoreItems;
import buildcraft.core.marker.VolumeBoxAddon;
import buildcraft.core.marker.VolumeBoxEntity;
import buildcraft.core.tile.TileMarkerPath;
import buildcraft.core.tile.TileMarkerVolume;
import buildcraft.lib.misc.InventoryUtil;

/** Connects markers: right click one marker and then another to join them. Also edits volume boxes:
 * <ul>
 * <li>Right click a corner of a box to drag it with where you look, and right click again to put it down (or sneak right
 * click to put it back).</li>
 * <li>Right click a corner's addon to use it, or sneak right click it to take it off.</li>
 * <li>Sneak right click a box to remove it.</li>
 * </ul> */
public class ItemMarkerConnector extends Item {
    public ItemMarkerConnector(Properties properties) {
        super(properties.stacksTo(1));
    }

    /** Called by markers when they're right clicked with this. */
    public static InteractionResult useOnMarker(ItemStack stack, Player player, BlockEntity marker) {
        Level level = marker.getLevel();
        if (level == null || level.isClientSide()) return InteractionResult.SUCCESS;
        BlockPos pos = marker.getBlockPos();
        BlockPos selected = stack.get(BCCoreComponents.MARKER_LINK.get());
        BlockEntity other = selected == null || selected.equals(pos) ? null : level.getBlockEntity(selected);
        boolean connected;
        if (marker instanceof TileMarkerPath path && other instanceof TileMarkerPath from) {
            connected = from.connectTo(path);
        } else if (marker instanceof TileMarkerVolume volume && other instanceof TileMarkerVolume from) {
            connected = from.connectTo(volume);
        } else {
            stack.set(BCCoreComponents.MARKER_LINK.get(), pos);
            player.sendOverlayMessage(Component.translatable("chat.buildcraft.marker_connector.selected"));
            return InteractionResult.SUCCESS;
        }
        if (connected) {
            // Paths are usually made one marker after another, so carry on from this one
            if (marker instanceof TileMarkerPath) {
                stack.set(BCCoreComponents.MARKER_LINK.get(), pos);
            } else {
                stack.remove(BCCoreComponents.MARKER_LINK.get());
            }
            player.sendOverlayMessage(Component.translatable("chat.buildcraft.marker_connector.connected"));
        } else {
            stack.set(BCCoreComponents.MARKER_LINK.get(), pos);
            player.sendOverlayMessage(Component.translatable("chat.buildcraft.marker_connector.cant_connect"));
        }
        return InteractionResult.SUCCESS;
    }

    private record Hit(VolumeBoxEntity box, int corner, double distance) {}

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean sneaking = player.isSecondaryUseActive();
        VolumeBoxEntity editing = VolumeBoxEntity.getEditing(player);
        if (editing != null) {
            if (!level.isClientSide()) {
                if (sneaking) {
                    editing.cancelEditing();
                } else {
                    editing.confirmEditing();
                }
            }
            return InteractionResult.SUCCESS;
        }

        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(player.blockInteractionRange()));
        Hit addon = null, corner = null, whole = null;
        for (VolumeBoxEntity box : VolumeBoxEntity.near(level, new AABB(eye, end))) {
            for (int i = 0; i < 8; i++) {
                if (box.getAddon(i) != null) {
                    addon = closer(addon, box, i, new AABB(box.getCorner(i), box.getCorner(i)).inflate(VolumeBoxEntity.ADDON_RADIUS),
                        eye, end);
                }
                corner = closer(corner, box, i, new AABB(box.getCornerBlock(i)), eye, end);
            }
            whole = box.getBoundingBox().contains(eye) ? new Hit(box, -1, 0) : closer(whole, box, -1, box.getBoundingBox(), eye, end);
        }

        if (addon != null) {
            if (level instanceof ServerLevel && player instanceof ServerPlayer serverPlayer) {
                if (addon.box.isLocked()) {
                    player.sendOverlayMessage(Component.translatable("chat.buildcraft.volume_box.locked"));
                } else if (sneaking) {
                    VolumeBoxAddon removed = addon.box.removeAddon(addon.corner);
                    if (removed != null) InventoryUtil.giveToPlayer(player, new ItemStack(removed.getItem()));
                } else {
                    VolumeBoxAddon clicked = addon.box.getAddon(addon.corner);
                    if (clicked != null) clicked.onRightClick(addon.box, serverPlayer);
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (corner != null && !sneaking) {
            if (!level.isClientSide()) {
                if (corner.box.isLocked()) {
                    player.sendOverlayMessage(Component.translatable("chat.buildcraft.volume_box.locked"));
                } else {
                    corner.box.startEditing(player, corner.corner, corner.distance + 0.5);
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (whole != null && sneaking) {
            if (!level.isClientSide()) {
                if (whole.box.isLocked()) {
                    player.sendOverlayMessage(Component.translatable("chat.buildcraft.volume_box.locked"));
                } else {
                    for (int i = 0; i < 8; i++) {
                        VolumeBoxAddon removed = whole.box.removeAddon(i);
                        if (removed != null) InventoryUtil.giveToPlayer(player, new ItemStack(removed.getItem()));
                    }
                    whole.box.discard();
                    InventoryUtil.giveToPlayer(player, new ItemStack(BCCoreItems.VOLUME_BOX.get()));
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (sneaking && stack.has(BCCoreComponents.MARKER_LINK.get())) {
            stack.remove(BCCoreComponents.MARKER_LINK.get());
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    private static @Nullable Hit closer(@Nullable Hit best, VolumeBoxEntity box, int corner, AABB aabb, Vec3 from, Vec3 to) {
        Optional<Vec3> hit = aabb.clip(from, to);
        if (hit.isEmpty()) return best;
        double distance = hit.get().distanceTo(from);
        return best == null || distance < best.distance ? new Hit(box, corner, distance) : best;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        BlockPos selected = stack.get(BCCoreComponents.MARKER_LINK.get());
        if (selected == null || slot != EquipmentSlot.MAINHAND || !(owner instanceof ServerPlayer player)) return;
        if (level.getGameTime() % 5 != 0) return;
        if (!(level.getBlockEntity(selected) instanceof TileMarkerPath || level.getBlockEntity(selected) instanceof TileMarkerVolume)) {
            stack.remove(BCCoreComponents.MARKER_LINK.get());
            return;
        }
        level.sendParticles(player, new DustParticleOptions(0xFFFF40, 1), true, false, selected.getX() + 0.5, selected.getY() + 0.5,
            selected.getZ() + 0.5, 4, 0.15, 0.15, 0.15, 0);
    }
}
