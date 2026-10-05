/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.silicon.plug;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;

/** Works out which block states can be made into facades: plain full cubes without a block entity. Blocks that can face
 * different ways (such as logs) give a facade for each way. */
public final class FacadeStates {
    private static Map<Item, List<BlockState>> byItem;

    private FacadeStates() {}

    public static boolean isValid(BlockState state) {
        Block block = state.getBlock();
        if (state.isAir() || state.hasBlockEntity() || state.getRenderShape() != RenderShape.MODEL) return false;
        if (!state.getFluidState().isEmpty()) return false;
        if (!state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) return false;
        if (!Block.isShapeFullBlock(state.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO))) return false;
        return block.asItem() != Items.AIR;
    }

    /** @return The facade states that can be made from the given item (none if it isn't a block that can be a facade). */
    public static List<BlockState> getStates(Item item) {
        return getAll().getOrDefault(item, List.of());
    }

    public static synchronized Map<Item, List<BlockState>> getAll() {
        if (byItem == null) {
            Map<Item, List<BlockState>> map = new LinkedHashMap<>();
            for (Block block : BuiltInRegistries.BLOCK) {
                BlockState base = block.defaultBlockState();
                if (!isValid(base)) continue;
                Set<BlockState> states = new LinkedHashSet<>();
                states.add(base);
                addVariants(states, base, BlockStateProperties.AXIS);
                addVariants(states, base, BlockStateProperties.HORIZONTAL_AXIS);
                addVariants(states, base, BlockStateProperties.FACING);
                addVariants(states, base, BlockStateProperties.HORIZONTAL_FACING);
                List<BlockState> valid = new ArrayList<>();
                for (BlockState state : states) {
                    if (isValid(state)) valid.add(state);
                }
                map.computeIfAbsent(block.asItem(), k -> new ArrayList<>()).addAll(valid);
            }
            byItem = map;
        }
        return byItem;
    }

    private static <T extends Comparable<T>> void addVariants(Set<BlockState> states, BlockState base, Property<T> property) {
        if (!base.hasProperty(property)) return;
        for (T value : property.getPossibleValues()) {
            states.add(base.setValue(property, value));
        }
    }
}
