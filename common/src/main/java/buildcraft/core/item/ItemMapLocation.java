/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core.item;

import java.util.List;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
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
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import buildcraft.api.core.IPathProvider;
import buildcraft.api.tiles.IAreaProvider;
import buildcraft.core.BCCoreComponents;
import buildcraft.core.marker.VolumeBoxEntity;
import buildcraft.lib.misc.InventoryUtil;
import buildcraft.lib.misc.ZonePlan;

/** Remembers a place: right click a path marker to store its path, a volume marker or volume box to store its area, or
 * any other block to store that spot. Zones are stored by the zone planner. Sneak right click to clear it. While held,
 * the stored place is shown with particles. */
public class ItemMapLocation extends Item {
    public ItemMapLocation(Properties properties) {
        super(properties);
    }

    @Nullable
    public static MapLocation get(ItemStack stack) {
        return stack.get(BCCoreComponents.MAP_LOCATION.get());
    }

    /** Sets (or clears, for null) the location, along with the model that shows its kind. */
    public static void set(ItemStack stack, @Nullable MapLocation location) {
        if (location == null) {
            stack.remove(BCCoreComponents.MAP_LOCATION.get());
            stack.remove(DataComponents.CUSTOM_MODEL_DATA);
        } else {
            stack.set(BCCoreComponents.MAP_LOCATION.get(), location);
            stack.set(DataComponents.CUSTOM_MODEL_DATA,
                new CustomModelData(List.of(), List.of(), List.of(location.type().getSerializedName()), List.of()));
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        if (get(stack) != null) return InteractionResult.PASS;
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        MapLocation location;
        VolumeBoxEntity box = VolumeBoxEntity.at(level, pos.relative(context.getClickedFace()));
        if (level.getBlockEntity(pos) instanceof IPathProvider provider && provider.getPath().size() >= 2) {
            location = MapLocation.path(provider.getPath());
        } else if (level.getBlockEntity(pos) instanceof IAreaProvider provider) {
            location = MapLocation.area(provider.min(), provider.max());
        } else if (box != null) {
            location = MapLocation.area(box.getMin(), box.getMax());
        } else {
            location = MapLocation.spot(pos, context.getClickedFace());
        }
        if (!level.isClientSide()) {
            ItemStack written = stack.copyWithCount(1);
            set(written, location);
            stack.shrink(1);
            Player player = context.getPlayer();
            if (stack.isEmpty() && player != null) {
                player.setItemInHand(context.getHand(), written);
            } else if (player != null) {
                InventoryUtil.giveToPlayer(player, written);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isSecondaryUseActive() || get(stack) == null) return InteractionResult.PASS;
        if (!level.isClientSide()) set(stack, null);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder,
        TooltipFlag flag) {
        MapLocation location = get(stack);
        if (location == null) {
            builder.accept(Component.translatable("item.buildcraft.map_location.clean"));
            return;
        }
        List<BlockPos> positions = location.positions();
        switch (location.type()) {
            case SPOT -> builder.accept(Component.translatable("item.buildcraft.map_location.spot", format(positions.getFirst()),
                Component.translatable("direction.buildcraft." + location.side().getSerializedName())));
            case AREA -> {
                BlockPos min = positions.get(0), max = positions.get(1);
                builder.accept(Component.translatable("item.buildcraft.map_location.area", format(min),
                    (max.getX() - min.getX() + 1) + " x " + (max.getY() - min.getY() + 1) + " x " + (max.getZ() - min.getZ() + 1)));
            }
            case PATH, PATH_REPEATING -> builder.accept(Component.translatable("item.buildcraft.map_location.path", format(positions.getFirst()),
                positions.size() - 1));
            case ZONE -> builder.accept(Component.translatable("item.buildcraft.map_location.zone", location.zone().size()));
        }
        builder.accept(Component.translatable("item.buildcraft.map_location.clear"));
    }

    private static String format(BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        MapLocation location = get(stack);
        if (location == null || slot != EquipmentSlot.MAINHAND || !(owner instanceof ServerPlayer player)) return;
        if (level.getGameTime() % 10 != 0) return;
        DustParticleOptions dust = new DustParticleOptions(0x40C0FF, 1);
        List<BlockPos> positions = location.positions();
        switch (location.type()) {
            case SPOT -> {
                Vec3 face = Vec3.atCenterOf(positions.getFirst()).add(Vec3.atLowerCornerOf(location.side().getUnitVec3i()).scale(0.55));
                particle(level, player, dust, face);
            }
            case AREA -> {
                BlockPos min = positions.get(0), max = positions.get(1);
                Vec3 lo = Vec3.atLowerCornerOf(min), hi = Vec3.atLowerCornerOf(max).add(1, 1, 1);
                for (int corner = 0; corner < 8; corner++) {
                    particle(level, player, dust, new Vec3((corner & 1) != 0 ? hi.x : lo.x, (corner & 2) != 0 ? hi.y : lo.y,
                        (corner & 4) != 0 ? hi.z : lo.z));
                }
            }
            case PATH, PATH_REPEATING -> {
                for (BlockPos pos : positions) {
                    particle(level, player, dust, Vec3.atCenterOf(pos));
                }
            }
            case ZONE -> {
                // Show the edge of the zone near the player
                ZonePlan zone = location.zone();
                BlockPos centre = player.blockPosition();
                for (int x = centre.getX() - 12; x <= centre.getX() + 12; x++) {
                    for (int z = centre.getZ() - 12; z <= centre.getZ() + 12; z++) {
                        if (zone.get(x, z) && (!zone.get(x + 1, z) || !zone.get(x - 1, z) || !zone.get(x, z + 1) || !zone.get(x, z - 1))) {
                            particle(level, player, dust, new Vec3(x + 0.5, centre.getY() + 0.2, z + 0.5));
                        }
                    }
                }
            }
        }
    }

    private static void particle(ServerLevel level, ServerPlayer player, DustParticleOptions dust, Vec3 pos) {
        level.sendParticles(player, dust, true, false, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
    }
}
