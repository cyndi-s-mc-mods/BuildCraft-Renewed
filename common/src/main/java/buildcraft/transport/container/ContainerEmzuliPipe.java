package buildcraft.transport.container;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BlockEntity;

import buildcraft.lib.gui.ContainerBC;
import buildcraft.lib.gui.MenuData;
import buildcraft.lib.gui.SlotPhantom;
import buildcraft.transport.BCTransportMenus;
import buildcraft.transport.pipe.behaviour.PipeBehaviourEmzuli;
import buildcraft.transport.pipe.behaviour.PipeBehaviourEmzuli.SlotIndex;

public class ContainerEmzuliPipe extends ContainerBC<BlockEntity> {
    private final @Nullable PipeBehaviourEmzuli behaviour;
    /** The paint colour of each slot: -1 for none, otherwise the dye id. */
    public final MenuData.Field[] colours = new MenuData.Field[SlotIndex.VALUES.length];
    public final MenuData.Field currentSlot;

    public ContainerEmzuliPipe(int id, Inventory inventory) {
        this(id, inventory, null);
    }

    public ContainerEmzuliPipe(int id, Inventory inventory, @Nullable PipeBehaviourEmzuli behaviour) {
        super(BCTransportMenus.PIPE_EMZULI.get(), id, inventory,
            behaviour == null ? null : (BlockEntity) behaviour.pipe.getHolder());
        this.behaviour = behaviour;
        Container filters = behaviour == null ? new SimpleContainer(4) : behaviour.invFilters;
        addSlot(new SlotPhantom(filters, 0, 25, 21));
        addSlot(new SlotPhantom(filters, 1, 25, 49));
        addSlot(new SlotPhantom(filters, 2, 134, 21));
        addSlot(new SlotPhantom(filters, 3, 134, 49));
        for (SlotIndex index : SlotIndex.VALUES) {
            colours[index.ordinal()] = data.addInt(behaviour == null ? null : () -> {
                DyeColor c = behaviour.slotColours.get(index);
                return c == null ? -1 : c.getId();
            });
        }
        currentSlot = data.addInt(behaviour == null ? null : () -> {
            SlotIndex s = behaviour.getCurrentSlot();
            return s == null ? -1 : s.ordinal();
        });
        addPlayerInventory(84);
    }

    /** Button ids: slot * 17 + 0 clears the colour, slot * 17 + 1 + dye id sets it. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (behaviour == null || id < 0 || id >= SlotIndex.VALUES.length * 17) return false;
        SlotIndex index = SlotIndex.VALUES[id / 17];
        int c = id % 17;
        behaviour.setSlotColour(index, c == 0 ? null : DyeColor.byId(c - 1));
        return true;
    }
}
