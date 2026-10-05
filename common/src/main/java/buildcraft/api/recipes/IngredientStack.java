/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.api.recipes;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

/** A number of items that match either an item or an item tag. Tags are looked up when used, so these can be made
 * before tags are loaded. */
public record IngredientStack(@Nullable Item item, @Nullable TagKey<Item> tag, int count, @Nullable Predicate<ItemStack> extra,
    @Nullable Supplier<ItemStack> display) implements Predicate<ItemStack> {
    public IngredientStack(@Nullable Item item, @Nullable TagKey<Item> tag, int count) {
        this(item, tag, count, null, null);
    }

    public static IngredientStack of(ItemLike item, int count) {
        return new IngredientStack(item.asItem(), null, count);
    }

    /** An exact item stack, including its data components (such as a gate's variant). */
    public static IngredientStack exact(Supplier<ItemStack> stack) {
        return new IngredientStack(null, null, 1, s -> ItemStack.isSameItemSameComponents(s, stack.get()), stack);
    }

    public static IngredientStack of(ItemLike item) {
        return of(item, 1);
    }

    /** @param tag An item tag id, such as "c:ingots/iron". */
    public static IngredientStack tag(String tag, int count) {
        return new IngredientStack(null, TagKey.create(Registries.ITEM, Identifier.parse(tag)), count);
    }

    public static IngredientStack tag(String tag) {
        return tag(tag, 1);
    }

    @Override
    public boolean test(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (extra != null) return extra.test(stack);
        if (item != null) return stack.is(item);
        return tag != null && stack.is(tag);
    }

    /** @return Example stacks for showing in GUIs and recipe viewers. */
    public List<ItemStack> getDisplayStacks() {
        if (display != null) return List.of(display.get());
        if (item != null) return List.of(new ItemStack(item, count));
        if (tag == null) return List.of();
        List<ItemStack> stacks = new ArrayList<>();
        for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
            stacks.add(new ItemStack(holder.value(), count));
        }
        return stacks;
    }
}
