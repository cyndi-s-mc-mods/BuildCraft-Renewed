package buildcraft.transport.container;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

import buildcraft.lib.gui.ContainerBC;
import buildcraft.lib.gui.MenuData;
import buildcraft.lib.gui.SlotPhantom;
import buildcraft.transport.BCTransportMenus;
import buildcraft.transport.pipe.behaviour.PipeBehaviourWoodDiamond;
import buildcraft.transport.pipe.behaviour.PipeBehaviourWoodDiamond.FilterMode;

public class ContainerDiamondWoodPipe extends ContainerBC<BlockEntity> {
    private final @Nullable PipeBehaviourWoodDiamond behaviour;
    public final MenuData.Field mode;
    public final MenuData.Field currentFilter;

    public ContainerDiamondWoodPipe(int id, Inventory inventory) {
        this(id, inventory, null);
    }

    public ContainerDiamondWoodPipe(int id, Inventory inventory, @Nullable PipeBehaviourWoodDiamond behaviour) {
        super(BCTransportMenus.PIPE_DIAMOND_WOOD.get(), id, inventory,
            behaviour == null ? null : (BlockEntity) behaviour.pipe.getHolder());
        this.behaviour = behaviour;
        Container filters = behaviour == null ? new SimpleContainer(9) : behaviour.filters;
        for (int i = 0; i < 9; i++) {
            addSlot(new SlotPhantom(filters, i, 8 + i * 18, 18));
        }
        mode = data.addInt(behaviour == null ? null : () -> behaviour.filterMode.ordinal());
        currentFilter = data.addInt(behaviour == null ? null : () -> behaviour.currentFilter);
        addPlayerInventory(79);
    }

    public FilterMode getMode() {
        return FilterMode.get(mode.getInt());
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (behaviour != null && id >= 0 && id < FilterMode.values().length) {
            behaviour.setFilterMode(FilterMode.values()[id]);
            return true;
        }
        return false;
    }
}
