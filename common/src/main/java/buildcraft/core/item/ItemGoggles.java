/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core.item;

import org.jspecify.annotations.Nullable;

import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;

import buildcraft.BuildCraft;

/** Worn on the head, these let you see laser beams even when they're hidden in the config
 * ({@link buildcraft.silicon.BCSiliconConfig#renderLaserBeams}). They give no protection, and never wear out. */
public class ItemGoggles extends Item {
    public static final ResourceKey<EquipmentAsset> ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, BuildCraft.id("goggles"));

    public ItemGoggles(Properties properties) {
        super(properties.stacksTo(1).component(net.minecraft.core.component.DataComponents.EQUIPPABLE,
            Equippable.builder(EquipmentSlot.HEAD).setEquipSound(SoundEvents.ARMOR_EQUIP_GENERIC).setAsset(ASSET).build()));
    }

    public static boolean isWearing(@Nullable LivingEntity entity) {
        return entity != null && entity.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof ItemGoggles;
    }
}
