package buildcraft.lib.fluid;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

/** A BuildCraft oil or fuel fluid. Forge and NeoForge subclass {@link Source} and {@link Flowing} to give them a
 * FluidType. */
public abstract class BCFluid extends FlowingFluid {
    public final BCFluidDefinition def;

    protected BCFluid(BCFluidDefinition def) {
        this.def = def;
    }

    @Override
    public Fluid getFlowing() {
        return def.flowing.get();
    }

    @Override
    public Fluid getSource() {
        return def.source.get();
    }

    @Override
    public Item getBucket() {
        return def.bucket.get();
    }

    @Override
    protected boolean canConvertToSource(ServerLevel level) {
        return false;
    }

    @Override
    protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {
        BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
        Block.dropResources(state, level, pos, blockEntity);
    }

    @Override
    protected int getSlopeFindDistance(LevelReader level) {
        return def.spread >= 8 ? 4 : 2;
    }

    @Override
    protected int getDropOff(LevelReader level) {
        // Spread is how far the fluid travels (vanilla water travels 8 blocks with a drop off of 1).
        return def.spread >= 7 ? 1 : 2;
    }

    @Override
    public int getTickDelay(LevelReader level) {
        return Math.max(5, Math.min(30, def.viscosity / 200));
    }

    @Override
    public boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid other, Direction direction) {
        return direction == Direction.DOWN && !isSame(other);
    }

    @Override
    protected float getExplosionResistance() {
        return 100;
    }

    @Override
    public BlockState createLegacyBlock(FluidState state) {
        return def.block.get().defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
    }

    @Override
    public boolean isSame(Fluid other) {
        return other == def.source.get() || other == def.flowing.get();
    }

    @Override
    public Optional<SoundEvent> getPickupSound() {
        return Optional.of(SoundEvents.BUCKET_FILL);
    }

    public static class Source extends BCFluid {
        public Source(BCFluidDefinition def) {
            super(def);
        }

        @Override
        public int getAmount(FluidState state) {
            return 8;
        }

        @Override
        public boolean isSource(FluidState state) {
            return true;
        }
    }

    public static class Flowing extends BCFluid {
        public Flowing(BCFluidDefinition def) {
            super(def);
        }

        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getAmount(FluidState state) {
            return state.getValue(LEVEL);
        }

        @Override
        public boolean isSource(FluidState state) {
            return false;
        }
    }
}
