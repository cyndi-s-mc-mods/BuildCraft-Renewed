/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** A tool for finding problems: right click a block to see the data its block entity saves (what BuildCraft's machines
 * know about themselves), on the server. */
public class ItemDebugger extends Item {
    public ItemDebugger(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockEntity tile = level.getBlockEntity(context.getClickedPos());
        Player player = context.getPlayer();
        if (tile == null || player == null) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            CompoundTag data = tile.saveWithoutMetadata(level.registryAccess());
            player.sendSystemMessage(Component.translatable("chat.buildcraft.debugger", tile.getBlockState().getBlock().getName(),
                context.getClickedPos().toShortString()));
            player.sendSystemMessage(NbtUtils.toPrettyComponent(data));
        }
        return InteractionResult.SUCCESS;
    }
}
