package buildcraft.lib.misc;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

import buildcraft.api.mj.MjAPI;
import buildcraft.lib.fluid.BCFluidStack;

public final class BlockUtil {
    /** How far down mining wells, pumps and quarries reach. */
    public static final int MINING_MAX_DEPTH = 512;
    public static final int PUMP_MAX_DISTANCE = 64;

    private BlockUtil() {}

    /** @return The power (in micro MJ) needed to break the block, based on its hardness. */
    public static long computeBlockBreakPower(Level level, BlockPos pos) {
        float hardness = level.getBlockState(pos).getDestroySpeed(level, pos);
        return (long) Math.floor(16 * MjAPI.MJ * ((hardness + 1) * 2));
    }

    public static boolean isUnbreakable(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getDestroySpeed(level, pos) < 0;
    }

    /** Breaks a block as if mined with a diamond pickaxe.
     * @return The drops, or null if the block couldn't be broken. */
    @Nullable
    public static List<ItemStack> breakBlockAndGetDrops(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || isUnbreakable(level, pos)) {
            return null;
        }
        List<ItemStack> drops = Block.getDrops(state, level, pos, level.getBlockEntity(pos), null, new ItemStack(Items.DIAMOND_PICKAXE));
        level.destroyBlock(pos, false);
        return drops;
    }

    /** @return The fluid in the block (still or flowing), or {@link Fluids#EMPTY}. Always the source form. */
    public static Fluid getFluidWithFlowing(Level level, BlockPos pos) {
        Fluid fluid = level.getFluidState(pos).getType();
        return fluid instanceof FlowingFluid flowing ? flowing.getSource() : fluid;
    }

    /** @return The fluid if the block is a source block, or {@link Fluids#EMPTY}. */
    public static Fluid getFluid(Level level, BlockPos pos) {
        FluidState state = level.getFluidState(pos);
        return state.isSource() ? state.getType() : Fluids.EMPTY;
    }

    /** Drains a source block.
     * @return The bucket of fluid drained, or empty. */
    public static BCFluidStack drainBlock(Level level, BlockPos pos, boolean simulate) {
        FluidState fluidState = level.getFluidState(pos);
        if (!fluidState.isSource()) {
            return BCFluidStack.EMPTY;
        }
        BCFluidStack result = BCFluidStack.of(fluidState.getType(), BCFluidStack.BUCKET);
        if (!simulate) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof BucketPickup pickup) {
                if (pickup.pickupBlock(null, level, pos, state).isEmpty() && level.getFluidState(pos).isSource()) {
                    return BCFluidStack.EMPTY;
                }
            } else {
                level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        return result;
    }
}
