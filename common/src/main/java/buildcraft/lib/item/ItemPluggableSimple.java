package buildcraft.lib.item;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.api.transport.pluggable.IItemPluggable;
import buildcraft.api.transport.pluggable.PipePluggable;
import buildcraft.api.transport.pluggable.PluggableDefinition;

/** An item that places a pluggable that has no settings of its own. */
public class ItemPluggableSimple extends Item implements IItemPluggable {
    @FunctionalInterface
    public interface PlacementPredicate {
        boolean canPlace(ItemStack stack, IPipeHolder holder, Direction side);
    }

    private final Supplier<PluggableDefinition> definition;
    private final PluggableDefinition.Creator creator;
    private final PlacementPredicate canPlace;

    public ItemPluggableSimple(Properties properties, Supplier<PluggableDefinition> definition, PluggableDefinition.Creator creator,
        @Nullable PlacementPredicate canPlace) {
        super(properties);
        this.definition = definition;
        this.creator = creator;
        this.canPlace = canPlace == null ? (s, h, side) -> true : canPlace;
    }

    @Override
    public @Nullable PipePluggable onPlace(ItemStack stack, IPipeHolder holder, Direction side, Player player, InteractionHand hand) {
        if (!canPlace.canPlace(stack, holder, side)) {
            return null;
        }
        return creator.create(definition.get(), holder, side);
    }
}
