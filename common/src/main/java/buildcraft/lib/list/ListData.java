/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.lib.list;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** What a list item matches: two lines of up to nine items. A line matches the items in it (exactly, with their data, if
 * "precise" is set). Instead, a line can match everything of the same type as its first item (items sharing a tag), or
 * of the same material (such as iron ingots, nuggets and blocks, from their c:something/iron tags), or both (the same
 * kind of item). */
public record ListData(List<Line> lines) {
    public static final int WIDTH = 9;
    public static final int HEIGHT = 2;

    public static final ListData EMPTY = new ListData(List.of(Line.EMPTY, Line.EMPTY));

    public record Line(List<ItemStack> stacks, boolean precise, boolean byType, boolean byMaterial) {
        public static final Line EMPTY = new Line(emptyStacks(), false, false, false);

        public static final Codec<Line> CODEC = RecordCodecBuilder.create(i -> i.group(
            ItemStack.OPTIONAL_CODEC.listOf().fieldOf("stacks").forGetter(Line::stacks),
            Codec.BOOL.optionalFieldOf("precise", false).forGetter(Line::precise),
            Codec.BOOL.optionalFieldOf("type", false).forGetter(Line::byType),
            Codec.BOOL.optionalFieldOf("material", false).forGetter(Line::byMaterial)).apply(i, Line::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Line> STREAM_CODEC = StreamCodec.composite(
            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list()), Line::stacks,
            ByteBufCodecs.BOOL, Line::precise,
            ByteBufCodecs.BOOL, Line::byType,
            ByteBufCodecs.BOOL, Line::byMaterial,
            Line::new);

        public Line {
            List<ItemStack> fixed = new ArrayList<>(stacks);
            while (fixed.size() < WIDTH) fixed.add(ItemStack.EMPTY);
            stacks = List.copyOf(fixed.subList(0, WIDTH));
        }

        private static List<ItemStack> emptyStacks() {
            List<ItemStack> list = new ArrayList<>();
            for (int i = 0; i < WIDTH; i++) list.add(ItemStack.EMPTY);
            return list;
        }

        /** @return True if only the first item is used (matching by type or material). */
        public boolean isOneStackMode() {
            return byType || byMaterial;
        }

        public Line withStack(int index, ItemStack stack) {
            List<ItemStack> list = new ArrayList<>(stacks);
            list.set(index, stack.copyWithCount(1));
            return new Line(list, precise, byType, byMaterial);
        }

        /** @param option 0 for precise, 1 for by type, 2 for by material. */
        public Line toggle(int option) {
            List<ItemStack> list = new ArrayList<>(stacks);
            boolean type = byType, material = byMaterial, exact = precise;
            if (option == 0) exact = !exact;
            if (option == 1) type = !type;
            if (option == 2) material = !material;
            if (!isOneStackMode() && (type || material)) {
                // Switching to matching one item: the rest are cleared
                for (int i = 1; i < WIDTH; i++) list.set(i, ItemStack.EMPTY);
            }
            return new Line(list, exact, type, material);
        }

        public boolean matches(ItemStack target) {
            if (target.isEmpty()) return false;
            if (isOneStackMode()) {
                ItemStack compare = stacks.get(0);
                if (compare.isEmpty()) return false;
                if (byType && byMaterial) {
                    return compare.getItem().getClass() == target.getItem().getClass();
                }
                if (byType) {
                    return compare.is(target.getItem()) || shareTag(compare, target);
                }
                return compare.is(target.getItem()) || shareMaterial(compare, target);
            }
            for (ItemStack stack : stacks) {
                if (stack.isEmpty()) continue;
                if (precise ? ItemStack.isSameItemSameComponents(stack, target) : ItemStack.isSameItem(stack, target)) {
                    return true;
                }
            }
            return false;
        }

        private static boolean shareTag(ItemStack a, ItemStack b) {
            Set<TagKey<Item>> tags = a.typeHolder().tags().collect(Collectors.toSet());
            return b.typeHolder().tags().anyMatch(tags::contains);
        }

        /** @return The materials of an item: the last part of its "c:" tags with a slash, such as "iron" for
         *         c:ingots/iron. */
        private static Set<String> materials(ItemStack stack) {
            return stack.typeHolder().tags().map(TagKey::location).filter(id -> id.getNamespace().equals("c") && id.getPath().contains("/"))
                .map(id -> id.getPath().substring(id.getPath().lastIndexOf('/') + 1)).collect(Collectors.toSet());
        }

        private static boolean shareMaterial(ItemStack a, ItemStack b) {
            Set<String> materials = materials(a);
            return !materials.isEmpty() && materials(b).stream().anyMatch(materials::contains);
        }
    }

    public static final Codec<ListData> CODEC = Line.CODEC.listOf().xmap(ListData::new, ListData::lines);
    public static final StreamCodec<RegistryFriendlyByteBuf, ListData> STREAM_CODEC =
        Line.STREAM_CODEC.apply(ByteBufCodecs.list()).map(ListData::new, ListData::lines);

    public ListData {
        List<Line> fixed = new ArrayList<>(lines);
        while (fixed.size() < HEIGHT) fixed.add(Line.EMPTY);
        lines = List.copyOf(fixed.subList(0, HEIGHT));
    }

    public ListData withLine(int index, Line line) {
        List<Line> list = new ArrayList<>(lines);
        list.set(index, line);
        return new ListData(list);
    }

    public boolean matches(ItemStack target) {
        for (Line line : lines) {
            if (line.matches(target)) return true;
        }
        return false;
    }

    public boolean isEmpty() {
        return lines.stream().allMatch(l -> l.stacks().stream().allMatch(ItemStack::isEmpty));
    }
}
