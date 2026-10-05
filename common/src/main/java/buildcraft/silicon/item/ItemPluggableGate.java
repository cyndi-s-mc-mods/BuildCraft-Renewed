/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.item;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.api.transport.pluggable.IItemPluggable;
import buildcraft.api.transport.pluggable.PipePluggable;
import buildcraft.lib.item.ICreativeVariants;
import buildcraft.silicon.BCSiliconComponents;
import buildcraft.silicon.BCSiliconItems;
import buildcraft.silicon.BCSiliconPlugs;
import buildcraft.silicon.gate.GateVariant;
import buildcraft.silicon.plug.PluggableGate;

/** A gate, which is placed on pipes. Its variant is stored in a data component. */
public class ItemPluggableGate extends Item implements IItemPluggable, ICreativeVariants {
    public ItemPluggableGate(Properties properties) {
        super(properties.component(BCSiliconComponents.GATE_VARIANT.get(), GateVariant.BASIC));
    }

    public static GateVariant getVariant(ItemStack stack) {
        GateVariant variant = stack.get(BCSiliconComponents.GATE_VARIANT.get());
        return variant == null ? GateVariant.BASIC : variant;
    }

    public static ItemStack getStack(GateVariant variant) {
        ItemStack stack = new ItemStack(BCSiliconItems.GATE.get());
        stack.set(BCSiliconComponents.GATE_VARIANT.get(), variant);
        return stack;
    }

    @Override
    public void addCreativeVariants(java.util.function.Consumer<ItemStack> output) {
        for (GateVariant variant : GateVariant.all()) {
            output.accept(getStack(variant));
        }
    }

    @Override
    public Component getName(ItemStack stack) {
        return getVariant(stack).getDisplayName();
    }

    @Override
    public @Nullable PipePluggable onPlace(ItemStack stack, IPipeHolder holder, Direction side, Player player, InteractionHand hand) {
        return new PluggableGate(BCSiliconPlugs.GATE, holder, side, getVariant(stack));
    }
}
