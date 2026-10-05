/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.item;

import org.jspecify.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import buildcraft.builders.BCBuildersComponents;

/** Remembers one block: use it on a block to copy it, or use it in the air while sneaking to clear it. Used by the
 * replacer to say which block to replace with which. */
public class ItemSchematicSingle extends Item {
    public ItemSchematicSingle(Properties properties) {
        super(properties);
    }

    @Nullable
    public static BlockState getState(ItemStack stack) {
        return stack.get(BCBuildersComponents.SCHEMATIC.get());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && player.isShiftKeyDown()) return InteractionResult.PASS;
        BlockState state = context.getLevel().getBlockState(context.getClickedPos());
        if (state.isAir()) return InteractionResult.PASS;
        if (!context.getLevel().isClientSide()) {
            context.getItemInHand().set(BCBuildersComponents.SCHEMATIC.get(), state);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown() || getState(stack) == null) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            stack.remove(BCBuildersComponents.SCHEMATIC.get());
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public Component getName(ItemStack stack) {
        BlockState state = getState(stack);
        if (state == null) return super.getName(stack);
        return Component.translatable("item.buildcraft.schematic_single.used", super.getName(stack), state.getBlock().getName());
    }
}
