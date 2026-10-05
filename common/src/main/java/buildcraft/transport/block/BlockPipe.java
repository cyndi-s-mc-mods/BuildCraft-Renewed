package buildcraft.transport.block;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.transport.pipe.PipeDefinition;
import buildcraft.lib.block.BlockBCTile;
import buildcraft.transport.tile.TilePipeHolder;

/** A pipe. Each pipe type has its own block, with a property for each connected side and some for visual details. */
public class BlockPipe extends BlockBCTile<TilePipeHolder> {
    /** The side that a directional pipe (wooden, iron, daizuli...) points at. */
    public static final EnumProperty<EnumPipePart> DIR = EnumProperty.create("dir", EnumPipePart.class);
    public static final EnumProperty<DyeColor> COLOUR = EnumProperty.create("colour", DyeColor.class);
    /** The power limit setting of iron and diamond kinesis pipes: 0 is no limit, 6 is off. */
    public static final IntegerProperty LIMIT = IntegerProperty.create("limit", 0, 6);

    private static final VoxelShape CENTER = Block.box(4, 4, 4, 12, 12, 12);
    private static final VoxelShape[] ARMS = {
        Block.box(4, 0, 4, 12, 4, 12), Block.box(4, 12, 4, 12, 16, 12),
        Block.box(4, 4, 0, 12, 12, 4), Block.box(4, 4, 12, 12, 12, 16),
        Block.box(0, 4, 4, 4, 12, 12), Block.box(12, 4, 4, 16, 12, 12) };
    private static final VoxelShape[] SHAPES = new VoxelShape[64];

    static {
        for (int mask = 0; mask < 64; mask++) {
            VoxelShape shape = CENTER;
            for (int i = 0; i < 6; i++) {
                if ((mask & (1 << i)) != 0) {
                    shape = Shapes.or(shape, ARMS[i]);
                }
            }
            SHAPES[mask] = shape.optimize();
        }
    }

    public enum Visual {
        PLAIN,
        DIRECTIONAL,
        COLOURED,
        DIRECTIONAL_COLOURED,
        LIMITER
    }

    public final PipeDefinition definition;

    public static BlockPipe create(Properties properties, Supplier<BlockEntityType<TilePipeHolder>> type, PipeDefinition def,
        Visual visual) {
        return switch (visual) {
            case PLAIN -> new BlockPipe(properties, type, def);
            case DIRECTIONAL -> new Directional(properties, type, def);
            case COLOURED -> new Coloured(properties, type, def);
            case DIRECTIONAL_COLOURED -> new DirectionalColoured(properties, type, def);
            case LIMITER -> new Limiter(properties, type, def);
        };
    }

    protected BlockPipe(Properties properties, Supplier<BlockEntityType<TilePipeHolder>> type, PipeDefinition definition) {
        super(properties.noOcclusion(), type);
        this.definition = definition;
        BlockState state = stateDefinition.any();
        for (BooleanProperty prop : PipeBlock.PROPERTY_BY_DIRECTION.values()) {
            state = state.setValue(prop, false);
        }
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        for (BooleanProperty prop : PipeBlock.PROPERTY_BY_DIRECTION.values()) {
            builder.add(prop);
        }
    }

    public static boolean isConnected(BlockState state, Direction side) {
        BooleanProperty prop = PipeBlock.PROPERTY_BY_DIRECTION.get(side);
        return state.hasProperty(prop) && state.getValue(prop);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int mask = 0;
        for (Direction dir : Direction.values()) {
            if (isConnected(state, dir)) {
                mask |= 1 << dir.get3DDataValue();
            }
        }
        if (level.getBlockEntity(pos) instanceof TilePipeHolder tile && !tile.getPluggables().isEmpty()) {
            return Shapes.or(SHAPES[mask], tile.getPluggableShape());
        }
        return SHAPES[mask];
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        // direction points from the block asking towards this pipe
        return level.getBlockEntity(pos) instanceof TilePipeHolder tile ? tile.getRedstoneOutput(direction.getOpposite()) : 0;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier,
        boolean isPrecise) {
        TilePipeHolder tile = getTile(level, pos);
        if (tile != null) {
            tile.getPipe().getBehaviour().onEntityCollide(entity);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
        net.minecraft.world.level.redstone.@Nullable Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
        TilePipeHolder tile = getTile(level, pos);
        if (tile != null) {
            tile.getPipe().markForUpdate();
        }
    }

    public static class Directional extends BlockPipe {
        protected Directional(Properties properties, Supplier<BlockEntityType<TilePipeHolder>> type, PipeDefinition def) {
            super(properties, type, def);
            registerDefaultState(defaultBlockState().setValue(DIR, EnumPipePart.CENTER));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(DIR);
        }
    }

    public static class Coloured extends BlockPipe {
        protected Coloured(Properties properties, Supplier<BlockEntityType<TilePipeHolder>> type, PipeDefinition def) {
            super(properties, type, def);
            registerDefaultState(defaultBlockState().setValue(COLOUR, DyeColor.WHITE));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(COLOUR);
        }
    }

    public static class DirectionalColoured extends BlockPipe {
        protected DirectionalColoured(Properties properties, Supplier<BlockEntityType<TilePipeHolder>> type, PipeDefinition def) {
            super(properties, type, def);
            registerDefaultState(defaultBlockState().setValue(DIR, EnumPipePart.CENTER).setValue(COLOUR, DyeColor.WHITE));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(DIR, COLOUR);
        }
    }

    public static class Limiter extends BlockPipe {
        protected Limiter(Properties properties, Supplier<BlockEntityType<TilePipeHolder>> type, PipeDefinition def) {
            super(properties, type, def);
            registerDefaultState(defaultBlockState().setValue(LIMIT, 0));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
            super.createBlockStateDefinition(builder);
            builder.add(LIMIT);
        }
    }
}
