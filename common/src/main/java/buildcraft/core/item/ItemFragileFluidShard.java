/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core.item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;

import buildcraft.core.BCCoreComponents;
import buildcraft.core.BCCoreItems;
import buildcraft.lib.fluid.BCFluid;
import buildcraft.lib.fluid.BCFluidStack;
import buildcraft.lib.fluid.IFluidHandlerBC;
import buildcraft.lib.platform.Platform;

/** Holds up to half a bucket of fluid that was in a tank, machine or pipe when it was broken. Right click a tank with it
 * to put the fluid back. */
public class ItemFragileFluidShard extends Item {
    public static final int MAX_FLUID = 500;

    public ItemFragileFluidShard(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static BCFluidStack getFluid(ItemStack stack) {
        return stack.getOrDefault(BCCoreComponents.FLUID.get(), BCFluidStack.EMPTY);
    }

    public static ItemStack create(BCFluidStack fluid) {
        ItemStack stack = new ItemStack(BCCoreItems.FRAGILE_FLUID_SHARD.get());
        stack.set(BCCoreComponents.FLUID.get(), fluid);
        // The item model tints the fluid part with this
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(), List.of(colourOf(fluid))));
        return stack;
    }

    private static int colourOf(BCFluidStack fluid) {
        if (fluid.getFluid() instanceof BCFluid bcFluid) return bcFluid.def.lightColour;
        if (fluid.getFluid().isSame(Fluids.WATER)) return 0x3F76E4;
        if (fluid.getFluid().isSame(Fluids.LAVA)) return 0xD96415;
        return 0xFFFFFF;
    }

    /** Adds shards holding the given fluid (as many as are needed) to the list. */
    public static void addDrops(List<ItemStack> drops, BCFluidStack fluid) {
        int amount = fluid.getAmount();
        while (amount > 0 && !fluid.isEmpty()) {
            int inShard = Math.min(amount, MAX_FLUID);
            drops.add(create(fluid.withAmount(inShard)));
            amount -= inShard;
        }
    }

    /** Drops the fluid in the given handlers as shards. Used when blocks with tanks are broken. */
    public static void dropFluids(Level level, BlockPos pos, IFluidHandlerBC... handlers) {
        if (level.isClientSide()) return;
        List<ItemStack> drops = new ArrayList<>();
        for (IFluidHandlerBC handler : handlers) {
            for (int i = 0; i < handler.getTanks(); i++) {
                addDrops(drops, handler.getFluidInTank(i));
            }
        }
        for (ItemStack drop : drops) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop);
        }
    }

    /** Empties as much of a shard as possible into a handler.
     * @return The shard left over (empty if it was all used), or null if nothing was moved. */
    public static @Nullable ItemStack emptyInto(ItemStack shard, IFluidHandlerBC handler, boolean simulate) {
        BCFluidStack fluid = getFluid(shard);
        int filled = fluid.isEmpty() ? 0 : handler.fill(fluid, simulate);
        if (filled <= 0) return null;
        if (filled >= fluid.getAmount()) return ItemStack.EMPTY;
        ItemStack rest = shard.copy();
        rest.set(BCCoreComponents.FLUID.get(), fluid.withAmount(fluid.getAmount() - filled));
        return rest;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        IFluidHandlerBC handler = Platform.INSTANCE.getFluidHandler(level, context.getClickedPos(), context.getClickedFace());
        if (handler == null) return InteractionResult.PASS;
        ItemStack held = context.getItemInHand();
        if (emptyInto(held, handler, true) == null) return InteractionResult.FAIL;
        if (!level.isClientSide()) {
            ItemStack rest = emptyInto(held, handler, false);
            if (rest != null && context.getPlayer() != null) {
                context.getPlayer().setItemInHand(context.getHand(), rest);
            }
            level.playSound(null, context.getClickedPos(), SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1, 1);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public Component getName(ItemStack stack) {
        BCFluidStack fluid = getFluid(stack);
        return Component.translatable("item.buildcraft.fragile_fluid_shard.of",
            fluid.isEmpty() ? Component.translatable("gui.buildcraft.tank.empty_fluid") : fluid.getName());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder,
        TooltipFlag flag) {
        BCFluidStack fluid = getFluid(stack);
        if (!fluid.isEmpty()) {
            builder.accept(Component.translatable("gui.buildcraft.tank.amount", fluid.getAmount(), MAX_FLUID));
        }
    }
}
