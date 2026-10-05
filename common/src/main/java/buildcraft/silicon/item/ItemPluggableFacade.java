/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.item;

import java.util.List;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.api.transport.pluggable.IItemPluggable;
import buildcraft.api.transport.pluggable.PipePluggable;
import buildcraft.lib.item.ICreativeVariants;
import buildcraft.silicon.BCSiliconComponents;
import buildcraft.silicon.BCSiliconItems;
import buildcraft.silicon.BCSiliconPlugs;
import buildcraft.silicon.plug.FacadeData;
import buildcraft.silicon.plug.FacadeStates;
import buildcraft.silicon.plug.PluggableFacade;

/** A facade, which stores the block it looks like in a data component. Using it while sneaking switches it between solid
 * and hollow. */
public class ItemPluggableFacade extends Item implements IItemPluggable, ICreativeVariants {
    public ItemPluggableFacade(Properties properties) {
        super(properties);
    }

    @Nullable
    public static FacadeData getData(ItemStack stack) {
        return stack.get(BCSiliconComponents.FACADE.get());
    }

    public static ItemStack getStack(FacadeData data, int count) {
        ItemStack stack = new ItemStack(BCSiliconItems.FACADE.get(), count);
        stack.set(BCSiliconComponents.FACADE.get(), data);
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        FacadeData data = getData(stack);
        if (data == null) return super.getName(stack);
        Component block = data.state().getBlock().getName();
        return Component.translatable(data.hollow() ? "item.buildcraft.facade.hollow" : "item.buildcraft.facade.solid", block);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        FacadeData data = getData(stack);
        if (data == null || !player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            stack.set(BCSiliconComponents.FACADE.get(), data.withHollow(!data.hollow()));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void addCreativeVariants(Consumer<ItemStack> output) {
        for (List<BlockState> states : FacadeStates.getAll().values()) {
            output.accept(getStack(new FacadeData(states.get(0), false), 1));
        }
    }

    @Override
    public @Nullable PipePluggable onPlace(ItemStack stack, IPipeHolder holder, Direction side, Player player, InteractionHand hand) {
        FacadeData data = getData(stack);
        if (data == null) return null;
        return new PluggableFacade(BCSiliconPlugs.FACADE, holder, side, data);
    }
}
