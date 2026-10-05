/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.snapshot;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/** A scanned area: either a template (which positions have blocks) or a blueprint (which block is at each position).
 * <p>
 * The area is stored relative to the block behind the architect table that scanned it, facing the way the table did, so
 * that a builder can build it in the same place relative to itself, turned to face the way the builder does. */
public final class Snapshot {
    public enum Type {
        TEMPLATE,
        BLUEPRINT;
    }

    public final Type type;
    public final int sizeX, sizeY, sizeZ;
    /** The way the architect table faced. */
    public final Direction facing;
    /** The lowest corner of the area, relative to the block behind the architect table. */
    public final BlockPos offset;
    /** Templates: which positions have blocks. */
    public final BitSet filled;
    /** Blueprints: the block states used, with air first. */
    public final List<BlockState> palette;
    /** Blueprints: the index in the palette of the state at each position. */
    public final int[] data;

    private Snapshot(Type type, int sizeX, int sizeY, int sizeZ, Direction facing, BlockPos offset, BitSet filled,
        List<BlockState> palette, int[] data) {
        this.type = type;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.facing = facing;
        this.offset = offset;
        this.filled = filled;
        this.palette = palette;
        this.data = data;
    }

    public static Snapshot template(int sizeX, int sizeY, int sizeZ, Direction facing, BlockPos offset, BitSet filled) {
        return new Snapshot(Type.TEMPLATE, sizeX, sizeY, sizeZ, facing, offset, filled, List.of(), new int[0]);
    }

    public static Snapshot blueprint(int sizeX, int sizeY, int sizeZ, Direction facing, BlockPos offset, List<BlockState> palette,
        int[] data) {
        return new Snapshot(Type.BLUEPRINT, sizeX, sizeY, sizeZ, facing, offset, new BitSet(), List.copyOf(palette), data);
    }

    public int volume() {
        return sizeX * sizeY * sizeZ;
    }

    public int index(int x, int y, int z) {
        return (y * sizeZ + z) * sizeX + x;
    }

    /** @return The state to build at the given position: air for nothing, or (for templates) null for any block. */
    public BlockState getState(int index) {
        if (type == Type.TEMPLATE) {
            return filled.get(index) ? null : Blocks.AIR.defaultBlockState();
        }
        return palette.get(data[index]);
    }

    /** @return How to turn the snapshot so that it faces the given way. */
    public Rotation rotationTo(Direction builderFacing) {
        int turns = (builderFacing.get2DDataValue() - facing.get2DDataValue() + 4) % 4;
        return switch (turns) {
            case 1 -> Rotation.CLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    /** @return Where the snapshot's area is, for a builder at the given position facing the given way. */
    public BoundingBox getBox(BlockPos builderPos, Direction builderFacing) {
        Rotation rotation = rotationTo(builderFacing);
        BlockPos base = builderPos.relative(builderFacing.getOpposite());
        BlockPos a = base.offset(offset.rotate(rotation));
        BlockPos b = base.offset(offset.offset(sizeX - 1, sizeY - 1, sizeZ - 1).rotate(rotation));
        return BoundingBox.fromCorners(a, b);
    }

    /** @return The index in this snapshot of a position in the world, for a builder at the given position facing the given
     *         way, or -1 if it's outside. */
    public int indexOfWorldPos(BlockPos world, BlockPos builderPos, Direction builderFacing) {
        Rotation rotation = rotationTo(builderFacing);
        BlockPos base = builderPos.relative(builderFacing.getOpposite());
        BlockPos relative = world.subtract(base).rotate(inverse(rotation)).subtract(offset);
        int x = relative.getX(), y = relative.getY(), z = relative.getZ();
        if (x < 0 || y < 0 || z < 0 || x >= sizeX || y >= sizeY || z >= sizeZ) return -1;
        return index(x, y, z);
    }

    public static Rotation inverse(Rotation rotation) {
        return switch (rotation) {
            case CLOCKWISE_90 -> Rotation.COUNTERCLOCKWISE_90;
            case COUNTERCLOCKWISE_90 -> Rotation.CLOCKWISE_90;
            default -> rotation;
        };
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", type.name());
        tag.putIntArray("size", new int[] { sizeX, sizeY, sizeZ });
        tag.putString("facing", facing.getSerializedName());
        tag.putIntArray("offset", new int[] { offset.getX(), offset.getY(), offset.getZ() });
        if (type == Type.TEMPLATE) {
            tag.putLongArray("filled", filled.toLongArray());
        } else {
            ListTag list = new ListTag();
            for (BlockState state : palette) {
                list.add(BlockState.CODEC.encodeStart(NbtOps.INSTANCE, state).getOrThrow());
            }
            tag.put("palette", list);
            tag.putIntArray("data", data);
        }
        return tag;
    }

    public static Snapshot load(CompoundTag tag) {
        Type type = Type.valueOf(tag.getStringOr("type", "TEMPLATE"));
        int[] size = tag.getIntArray("size").orElse(new int[] { 1, 1, 1 });
        Direction facing = Direction.byName(tag.getStringOr("facing", "north"));
        if (facing == null || facing.getAxis().isVertical()) facing = Direction.NORTH;
        int[] off = tag.getIntArray("offset").orElse(new int[3]);
        BlockPos offset = new BlockPos(off[0], off[1], off[2]);
        if (type == Type.TEMPLATE) {
            BitSet filled = BitSet.valueOf(tag.getLongArray("filled").orElse(new long[0]));
            return template(size[0], size[1], size[2], facing, offset, filled);
        }
        List<BlockState> palette = new ArrayList<>();
        for (Tag entry : tag.getListOrEmpty("palette")) {
            palette.add(BlockState.CODEC.parse(NbtOps.INSTANCE, entry).result().orElse(Blocks.AIR.defaultBlockState()));
        }
        if (palette.isEmpty()) palette.add(Blocks.AIR.defaultBlockState());
        int[] data = tag.getIntArray("data").orElse(new int[size[0] * size[1] * size[2]]);
        for (int i = 0; i < data.length; i++) {
            if (data[i] < 0 || data[i] >= palette.size()) data[i] = 0;
        }
        return blueprint(size[0], size[1], size[2], facing, offset, palette, data);
    }
}
