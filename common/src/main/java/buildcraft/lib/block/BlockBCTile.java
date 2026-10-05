package buildcraft.lib.block;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;

import buildcraft.lib.tile.TileBC;

/** A block with a {@link TileBC} block entity. Forwards ticks, interaction and neighbour updates to it. */
public class BlockBCTile<T extends TileBC> extends Block implements EntityBlock {
    private final Supplier<BlockEntityType<T>> tileType;

    public BlockBCTile(Properties properties, Supplier<BlockEntityType<T>> tileType) {
        super(properties);
        this.tileType = tileType;
    }

    public BlockEntityType<T> getTileType() {
        return tileType.get();
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return tileType.get().create(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <E extends BlockEntity> @Nullable BlockEntityTicker<E> getTicker(Level level, BlockState state, BlockEntityType<E> type) {
        return type == tileType.get() ? (l, p, s, be) -> ((TileBC) be).tick() : null;
    }

    @Nullable
    protected T getTile(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        return be != null && be.getType() == tileType.get() ? (T) be : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack stack) {
        super.setPlacedBy(level, pos, state, by, stack);
        T tile = getTile(level, pos);
        if (tile != null) {
            tile.onPlacedBy(by, stack);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation,
        boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
        T tile = getTile(level, pos);
        if (tile != null) {
            tile.onNeighbourChanged();
        }
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
        InteractionHand hand, BlockHitResult hit) {
        T tile = getTile(level, pos);
        if (tile != null) {
            InteractionResult result = tile.onActivated(player, hand, stack, hit);
            if (result != InteractionResult.PASS) {
                return result;
            }
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        T tile = getTile(level, pos);
        if (tile != null) {
            return tile.onActivated(player, InteractionHand.MAIN_HAND, ItemStack.EMPTY, hit);
        }
        return InteractionResult.PASS;
    }
}
