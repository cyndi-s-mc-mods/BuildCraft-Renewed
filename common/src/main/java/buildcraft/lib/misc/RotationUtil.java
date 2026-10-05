package buildcraft.lib.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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

    /** Turns a point (relative to the centre of the block) from the west side to the given side. Pluggable models and
     * boxes are made for the west side and turned with this. */
    public static Vec3 rotateFromWest(Vec3 p, Direction side) {
        return switch (side) {
            case WEST -> p;
            case EAST -> new Vec3(-p.x, p.y, -p.z);
            case NORTH -> new Vec3(-p.z, p.y, p.x);
            case SOUTH -> new Vec3(p.z, p.y, -p.x);
            case UP -> new Vec3(p.y, -p.x, p.z);
            case DOWN -> new Vec3(-p.y, p.x, p.z);
        };
    }

    /** @return The direction that a direction on the west side model points to once the model is turned to the given side. */
    public static Direction directionFromWest(Direction local, Direction side) {
        Vec3 v = rotateFromWest(new Vec3(local.getStepX(), local.getStepY(), local.getStepZ()), side);
        return Direction.getApproximateNearest(v.x, v.y, v.z);
    }

    /** @return A box (in pixels, as it would be on the west side) turned to the given side, in block coordinates. */
    public static AABB boxFromWest(double x0, double y0, double z0, double x1, double y1, double z1, Direction side) {
        Vec3 a = rotateFromWest(new Vec3(x0 / 16 - 0.5, y0 / 16 - 0.5, z0 / 16 - 0.5), side);
        Vec3 b = rotateFromWest(new Vec3(x1 / 16 - 0.5, y1 / 16 - 0.5, z1 / 16 - 0.5), side);
        return new AABB(a.x + 0.5, a.y + 0.5, a.z + 0.5, b.x + 0.5, b.y + 0.5, b.z + 0.5);
    }
}
