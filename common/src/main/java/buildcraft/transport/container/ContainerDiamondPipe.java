package buildcraft.transport.container;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

import buildcraft.lib.gui.ContainerBC;
import buildcraft.lib.gui.SlotPhantom;
import buildcraft.transport.BCTransportMenus;
import buildcraft.transport.pipe.behaviour.PipeBehaviourDiamond;

public class ContainerDiamondPipe extends ContainerBC<BlockEntity> {
    private final @Nullable PipeBehaviourDiamond behaviour;

    public ContainerDiamondPipe(int id, Inventory inventory) {
        this(id, inventory, null);
    }

    public ContainerDiamondPipe(int id, Inventory inventory, @Nullable PipeBehaviourDiamond behaviour) {
        super(BCTransportMenus.PIPE_DIAMOND.get(), id, inventory,
            behaviour == null ? null : (BlockEntity) behaviour.pipe.getHolder());
        this.behaviour = behaviour;
        Container filters = behaviour == null ? new SimpleContainer(PipeBehaviourDiamond.FILTERS_PER_SIDE * 6) : behaviour.filters;
        for (int y = 0; y < 6; y++) {
            for (int x = 0; x < 9; x++) {
                addSlot(new SlotPhantom(filters, x + y * 9, 8 + x * 18, 18 + y * 18));
            }
        }
        addPlayerInventory(140);
    }

    @Override
    public boolean stillValid(Player player) {
        return super.stillValid(player) && (tile == null || !tile.isRemoved());
    }
}
