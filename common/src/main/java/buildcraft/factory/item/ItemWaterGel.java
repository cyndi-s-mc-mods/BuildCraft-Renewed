/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import buildcraft.factory.BCFactoryBlocks;

/** Thrown into water to start turning it into gel. */
public class ItemWaterGel extends Item {
    public ItemWaterGel(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK || !level.getBlockState(hit.getBlockPos()).is(Blocks.WATER)) {
            return InteractionResult.PASS;
        }
        ItemStack stack = player.getItemInHand(hand);
        stack.consume(1, player);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL, 0.5f,
            0.4f / (level.getRandom().nextFloat() * 0.4f + 0.8f));
        if (!level.isClientSide()) {
            BCFactoryBlocks.WATER_GEL.get().startSpreading(level, hit.getBlockPos());
        }
        return InteractionResult.SUCCESS;
    }
}
