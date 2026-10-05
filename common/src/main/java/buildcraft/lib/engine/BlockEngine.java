package buildcraft.lib.engine;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import buildcraft.api.enums.EnumPowerStage;
import buildcraft.lib.block.BlockBCTile;
import buildcraft.lib.block.IWrenchable;

/** An engine block. The facing is the direction power is sent in, and the stage is shown by the trunk colour. */
public class BlockEngine<T extends TileEngineBase> extends BlockBCTile<T> implements IWrenchable {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final EnumProperty<EnumPowerStage> STAGE = EnumProperty.create("stage", EnumPowerStage.class);

    public BlockEngine(Properties properties, Supplier<BlockEntityType<T>> tileType) {
        super(properties.noOcclusion(), tileType);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP).setValue(STAGE, EnumPowerStage.BLUE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, STAGE);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public InteractionResult onWrench(BlockState state, Level level, BlockPos pos, Player player, Direction side) {
        T tile = getTile(level, pos);
        if (tile == null) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            tile.attemptRotation();
        }
        return InteractionResult.SUCCESS;
    }
}
