/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core.item;

import java.util.Optional;
import java.util.function.Supplier;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import buildcraft.core.marker.VolumeBoxAddon;
import buildcraft.core.marker.VolumeBoxEntity;

/** Adds an addon to the corner of a volume box that the player is looking at. */
public class ItemVolumeBoxAddon extends Item {
    private final Supplier<? extends VolumeBoxAddon> factory;

    public ItemVolumeBoxAddon(Properties properties, Supplier<? extends VolumeBoxAddon> factory) {
        super(properties);
        this.factory = factory;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(player.blockInteractionRange()));
        VolumeBoxEntity bestBox = null;
        int bestCorner = -1;
        double bestDist = Double.MAX_VALUE;
        for (VolumeBoxEntity box : VolumeBoxEntity.near(level, new AABB(eye, end))) {
            for (int i = 0; i < 8; i++) {
                Vec3 corner = box.getCorner(i);
                Optional<Vec3> hit = new AABB(corner, corner).inflate(VolumeBoxEntity.ADDON_RADIUS * 1.5).clip(eye, end);
                if (hit.isPresent() && hit.get().distanceTo(eye) < bestDist) {
                    bestDist = hit.get().distanceTo(eye);
                    bestBox = box;
                    bestCorner = i;
                }
            }
        }
        if (bestBox == null) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            ItemStack stack = player.getItemInHand(hand);
            if (bestBox.addAddon(bestCorner, factory.get())) {
                if (!player.getAbilities().instabuild) stack.shrink(1);
            } else {
                player.sendOverlayMessage(Component.translatable("chat.buildcraft.volume_box.cant_add"));
            }
        }
        return InteractionResult.SUCCESS;
    }
}
