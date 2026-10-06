/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.item;

import java.util.function.Consumer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

import buildcraft.silicon.BCSiliconComponents;
import buildcraft.silicon.plug.PluggableGate;

/** Copies the settings of one gate onto others: right click a gate to copy it (when empty), then right click other gates
 * to paste. Sneak right click to clear it. */
public class ItemGateCopier extends Item {
    public ItemGateCopier(Properties properties) {
        super(properties.stacksTo(1));
    }

    /** Called (on the server) when a gate is right clicked with this. */
    public static void useOnGate(ItemStack stack, Player player, PluggableGate gate) {
        CompoundTag stored = stack.get(BCSiliconComponents.GATE_COPY.get());
        if (stored != null) {
            gate.logic.loadConfig(TagValueInput.create(ProblemReporter.DISCARDING, player.registryAccess(), stored));
            gate.scheduleNetworkUpdate();
            player.sendOverlayMessage(Component.translatable("chat.buildcraft.gate_copier.pasted"));
        } else {
            TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, player.registryAccess());
            gate.logic.saveConfig(output);
            stack.set(BCSiliconComponents.GATE_COPY.get(), output.buildResult());
            player.sendOverlayMessage(Component.translatable("chat.buildcraft.gate_copier.copied"));
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isSecondaryUseActive() || !stack.has(BCSiliconComponents.GATE_COPY.get())) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            stack.remove(BCSiliconComponents.GATE_COPY.get());
            player.sendOverlayMessage(Component.translatable("chat.buildcraft.gate_copier.cleared"));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder,
        TooltipFlag flag) {
        if (stack.has(BCSiliconComponents.GATE_COPY.get())) {
            builder.accept(Component.translatable("item.buildcraft.gate_copier.full"));
        } else {
            builder.accept(Component.translatable("item.buildcraft.gate_copier.empty"));
        }
    }
}
