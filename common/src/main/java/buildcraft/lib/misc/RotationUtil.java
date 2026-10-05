package buildcraft.lib.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;

/** Wrench rotation for vanilla (and other) blocks that have a facing property. */
public final class RotationUtil {
    private RotationUtil() {}

    /** @return True if the block was rotated. */
    public static boolean rotateVanilla(Level level, BlockPos pos, BlockState state) {
        BlockState rotated = null;
        if (state.hasProperty(BlockStateProperties.CHEST_TYPE)
            && state.getValue(BlockStateProperties.CHEST_TYPE) != ChestType.SINGLE) {
            // Rotating half of a double chest breaks it.
            return false;
        }
        if (state.hasProperty(BlockStateProperties.FACING)) {
            Direction current = state.getValue(BlockStateProperties.FACING);
            rotated = state.setValue(BlockStateProperties.FACING, Direction.from3DDataValue(current.get3DDataValue() + 1));
        } else if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            rotated = state.setValue(BlockStateProperties.HORIZONTAL_FACING,
                state.getValue(BlockStateProperties.HORIZONTAL_FACING).getClockWise());
        } else if (state.hasProperty(BlockStateProperties.FACING_HOPPER)) {
            Direction current = state.getValue(BlockStateProperties.FACING_HOPPER);
            Direction next = current;
            do {
                next = Direction.from3DDataValue(next.get3DDataValue() + 1);
            } while (!BlockStateProperties.FACING_HOPPER.getPossibleValues().contains(next));
            rotated = state.setValue(BlockStateProperties.FACING_HOPPER, next);
        } else if (state.hasProperty(BlockStateProperties.AXIS)) {
            rotated = state.cycle(BlockStateProperties.AXIS);
        }
        if (rotated == null || rotated == state) {
            return false;
        }
        level.setBlock(pos, rotated, Block.UPDATE_ALL);
        return true;
    }

    /** Cycles to the next of the six directions after current. */
    public static Direction next(Direction current) {
        return Direction.from3DDataValue(current.get3DDataValue() + 1);
    }
}
