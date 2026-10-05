/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport.pipe.behaviour;

import java.util.EnumMap;
import java.util.EnumSet;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.tools.IToolWrench;
import buildcraft.api.transport.pipe.IFlowItems;
import buildcraft.api.transport.pipe.IPipe;
import buildcraft.lib.inventory.ItemHandlerSimple;
import buildcraft.lib.misc.StackUtil;
import buildcraft.transport.container.ContainerEmzuliPipe;

/** Emzuli pipes extract the items of whichever filter slots are active, painting them that slot's colour. Slots are
 * activated by gates, or (until gates are ported) by a redstone signal, which activates every slot. */
public class PipeBehaviourEmzuli extends PipeBehaviourWood {
    public enum SlotIndex {
        SQUARE(DyeColor.RED),
        CIRCLE(DyeColor.GREEN),
        TRIANGLE(DyeColor.BLUE),
        CROSS(DyeColor.YELLOW);

        public static final SlotIndex[] VALUES = values();
        public final DyeColor colour;

        SlotIndex(DyeColor colour) {
            this.colour = colour;
        }

        public SlotIndex next() {
            return VALUES[(ordinal() + 1) % VALUES.length];
        }
    }

    public final EnumMap<SlotIndex, DyeColor> slotColours = new EnumMap<>(SlotIndex.class);
    public final ItemHandlerSimple invFilters = new ItemHandlerSimple(4, () -> {});
    private final EnumSet<SlotIndex> activeSlots = EnumSet.noneOf(SlotIndex.class);
    private final byte[] activatedTtl = new byte[SlotIndex.VALUES.length];
    private @Nullable SlotIndex currentSlot = null;

    public PipeBehaviourEmzuli(IPipe pipe) {
        super(pipe);
    }

    @Override
    public void save(ValueOutput output) {
        super.save(output);
        invFilters.save(output, "filters");
        int active = 0;
        for (SlotIndex index : activeSlots) {
            active |= 1 << index.ordinal();
        }
        output.putInt("activeSlots", active);
        output.putInt("currentSlot", currentSlot == null ? -1 : currentSlot.ordinal());
        for (SlotIndex index : SlotIndex.VALUES) {
            DyeColor c = slotColours.get(index);
            output.putInt("slotColour" + index.ordinal(), c == null ? -1 : c.getId());
        }
    }

    @Override
    public void load(ValueInput input) {
        super.load(input);
        invFilters.load(input, "filters");
        int active = input.getIntOr("activeSlots", 0);
        activeSlots.clear();
        for (SlotIndex index : SlotIndex.VALUES) {
            if ((active & (1 << index.ordinal())) != 0) activeSlots.add(index);
            int c = input.getIntOr("slotColour" + index.ordinal(), -1);
            if (c >= 0 && c < 16) {
                slotColours.put(index, DyeColor.byId(c));
            } else {
                slotColours.remove(index);
            }
        }
        int current = input.getIntOr("currentSlot", -1);
        currentSlot = current < 0 || current >= SlotIndex.VALUES.length ? null : SlotIndex.VALUES[current];
    }

    @Override
    protected int extractItems(IFlowItems flow, Direction dir, int count, boolean simulate) {
        if (currentSlot == null && !activeSlots.isEmpty()) {
            currentSlot = getNextSlot();
        }
        SlotIndex slot = currentSlot;
        if (slot == null) return 0;
        ItemStack filter = invFilters.getItem(slot.ordinal());
        int extracted = flow.tryExtractItems(count, dir, slotColours.get(slot), s -> StackUtil.isMatchingItemOrList(filter, s), simulate);
        if (extracted > 0 && !simulate) {
            currentSlot = getNextSlot();
            pipe.getHolder().scheduleNetworkUpdate();
        }
        return extracted;
    }

    @Override
    public void onTick() {
        super.onTick();
        if (pipe.getHolder().getPipeWorld().isClientSide()) {
            return;
        }
        if (pipe.getHolder().getPipeWorld().hasNeighborSignal(pipe.getHolder().getPipePos())) {
            for (SlotIndex index : SlotIndex.VALUES) {
                activate(index);
            }
        }
        for (SlotIndex index : SlotIndex.VALUES) {
            byte val = activatedTtl[index.ordinal()];
            if (val > 0) {
                val--;
                activatedTtl[index.ordinal()] = val;
            }
            if (val == 0) {
                activeSlots.remove(index);
                if (currentSlot == index) {
                    currentSlot = getNextSlot();
                }
            }
        }
    }

    /** Activates a slot for the next couple of ticks. */
    public void activate(SlotIndex index) {
        activeSlots.add(index);
        activatedTtl[index.ordinal()] = 2;
    }

    private @Nullable SlotIndex getNextSlot() {
        SlotIndex current = currentSlot == null ? SlotIndex.CROSS : currentSlot;
        for (int i = 0; i < SlotIndex.VALUES.length; i++) {
            current = current.next();
            if (activeSlots.contains(current) && !invFilters.getItem(current.ordinal()).isEmpty()) {
                return current;
            }
        }
        return null;
    }

    public @Nullable SlotIndex getCurrentSlot() {
        return currentSlot;
    }

    public void setSlotColour(SlotIndex index, @Nullable DyeColor colour) {
        if (colour == null) {
            slotColours.remove(index);
        } else {
            slotColours.put(index, colour);
        }
        pipe.getHolder().scheduleNetworkUpdate();
    }

    @Override
    public InteractionResult onPipeActivate(Player player, InteractionHand hand, ItemStack held, BlockHitResult hit,
        EnumPipePart part) {
        if (held.getItem() instanceof IToolWrench) {
            return super.onPipeActivate(player, hand, held, hit, part);
        }
        return InteractionResult.PASS;
    }

    @Override
    public @Nullable MenuProvider getMenuProvider() {
        return new SimpleMenuProvider((id, inv, player) -> new ContainerEmzuliPipe(id, inv, this),
            Component.translatable(((BlockEntity) pipe.getHolder()).getBlockState().getBlock().getDescriptionId()));
    }
}
