/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.core.marker;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import buildcraft.core.BCCoreEntities;

/** A box marked out in the world without markers: placed with the volume box item, and resized (by dragging a corner)
 * or removed with the marker connector. Fillers and architect tables placed next to one use its area. Each corner can
 * hold an {@link VolumeBoxAddon}. */
public class VolumeBoxEntity extends Entity {
    /** The largest a box can be along each axis. */
    public static final int MAX_SIZE = 64;
    public static final int LOCK_NONE = 0, LOCK_READ = 1, LOCK_WRITE = 2;
    /** How far from a corner (in blocks) clicks on its addon reach. */
    public static final double ADDON_RADIUS = 0.2;

    private static final EntityDataAccessor<BlockPos> DATA_MIN = SynchedEntityData.defineId(VolumeBoxEntity.class,
        EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<BlockPos> DATA_MAX = SynchedEntityData.defineId(VolumeBoxEntity.class,
        EntityDataSerializers.BLOCK_POS);
    /** The network id of the player resizing the box, or -1. */
    private static final EntityDataAccessor<Integer> DATA_EDITOR = SynchedEntityData.defineId(VolumeBoxEntity.class,
        EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_LOCK = SynchedEntityData.defineId(VolumeBoxEntity.class,
        EntityDataSerializers.INT);
    /** The addons, as SNBT (as there is no data serializer for compound tags). */
    private static final EntityDataAccessor<String> DATA_ADDONS = SynchedEntityData.defineId(VolumeBoxEntity.class,
        EntityDataSerializers.STRING);

    /** Indexed by corner: bit 0 set for the corner on the high x side, bit 1 for high y and bit 2 for high z. */
    private final @Nullable VolumeBoxAddon[] addons = new VolumeBoxAddon[8];

    // Resizing (server only)
    @Nullable
    private UUID editor;
    private BlockPos held = BlockPos.ZERO;
    private double heldDist;
    private BlockPos oldMin = BlockPos.ZERO, oldMax = BlockPos.ZERO;

    @Nullable
    private BlockPos lockedBy;

    public VolumeBoxEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public static VolumeBoxEntity create(Level level, BlockPos pos) {
        VolumeBoxEntity box = new VolumeBoxEntity(BCCoreEntities.VOLUME_BOX.get(), level);
        box.setBox(pos, pos);
        return box;
    }

    // Finding boxes

    /** @return Every box that may touch the given area. */
    public static List<VolumeBoxEntity> near(Level level, AABB area) {
        AABB search = area.inflate(MAX_SIZE);
        return level.getEntitiesOfClass(VolumeBoxEntity.class, search, box -> box.getBoundingBox().intersects(area));
    }

    /** @return The box that contains the given position (the smallest, if several do), or null. */
    @Nullable
    public static VolumeBoxEntity at(Level level, BlockPos pos) {
        return near(level, new AABB(pos)).stream()
            .filter(box -> box.getBox().isInside(pos))
            .min(Comparator.comparingInt(box -> box.getBox().getXSpan() * box.getBox().getYSpan() * box.getBox().getZSpan()))
            .orElse(null);
    }

    // The box

    public BlockPos getMin() {
        return entityData.get(DATA_MIN);
    }

    public BlockPos getMax() {
        return entityData.get(DATA_MAX);
    }

    public BoundingBox getBox() {
        return BoundingBox.fromCorners(getMin(), getMax());
    }

    /** @return The index of a position in the box: x first, then z, then y (as {@link buildcraft.builders.BuildEngine}
     *         uses). */
    public int indexOf(BlockPos pos) {
        BlockPos min = getMin();
        BoundingBox box = getBox();
        return (pos.getX() - min.getX()) + box.getXSpan() * ((pos.getZ() - min.getZ()) + box.getZSpan() * (pos.getY() - min.getY()));
    }

    public void setBox(BlockPos a, BlockPos b) {
        BlockPos min = BlockPos.min(a, b), max = BlockPos.max(a, b);
        if (min.equals(getMin()) && max.equals(getMax())) return;
        entityData.set(DATA_MIN, min);
        entityData.set(DATA_MAX, max);
        AABB aabb = makeBoundingBox(Vec3.ZERO);
        setPos(aabb.getCenter().x, aabb.minY, aabb.getCenter().z);
    }

    @Override
    protected AABB makeBoundingBox(Vec3 position) {
        BlockPos min = entityData.get(DATA_MIN), max = entityData.get(DATA_MAX);
        return new AABB(min.getX(), min.getY(), min.getZ(), max.getX() + 1, max.getY() + 1, max.getZ() + 1);
    }

    /** @return The point at a corner of the box. */
    public Vec3 getCorner(int corner) {
        AABB aabb = getBoundingBox();
        return new Vec3((corner & 1) != 0 ? aabb.maxX : aabb.minX, (corner & 2) != 0 ? aabb.maxY : aabb.minY,
            (corner & 4) != 0 ? aabb.maxZ : aabb.minZ);
    }

    /** @return The block at a corner of the box. */
    public BlockPos getCornerBlock(int corner) {
        BlockPos min = getMin(), max = getMax();
        return new BlockPos((corner & 1) != 0 ? max.getX() : min.getX(), (corner & 2) != 0 ? max.getY() : min.getY(),
            (corner & 4) != 0 ? max.getZ() : min.getZ());
    }

    // Locks

    public int getLock() {
        return entityData.get(DATA_LOCK);
    }

    public boolean isLocked() {
        return getLock() != LOCK_NONE;
    }

    /** Locks this box while the block entity at the given position uses it (see {@link IVolumeBoxUser}). */
    public void lock(BlockPos user, int type) {
        lockedBy = user.immutable();
        entityData.set(DATA_LOCK, type);
        if (isEditing()) cancelEditing();
    }

    // Addons

    @Nullable
    public VolumeBoxAddon getAddon(int corner) {
        return addons[corner];
    }

    public <T extends VolumeBoxAddon> @Nullable T getAddon(Class<T> type) {
        for (VolumeBoxAddon addon : addons) {
            if (type.isInstance(addon)) return type.cast(addon);
        }
        return null;
    }

    /** @return True if it was added (each kind of addon can only be added once to each box). */
    public boolean addAddon(int corner, VolumeBoxAddon addon) {
        if (addons[corner] != null || isLocked()) return false;
        for (VolumeBoxAddon other : addons) {
            if (other != null && other.getType().equals(addon.getType())) return false;
        }
        addons[corner] = addon;
        addon.onBoxChanged(this);
        addonsChanged();
        return true;
    }

    @Nullable
    public VolumeBoxAddon removeAddon(int corner) {
        VolumeBoxAddon addon = addons[corner];
        if (addon == null || isLocked()) return null;
        addons[corner] = null;
        addonsChanged();
        return addon;
    }

    /** Sends the addons to clients again. Call after changing an addon. */
    public void addonsChanged() {
        if (level().isClientSide()) return;
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registryAccess());
        saveAddons(output);
        entityData.set(DATA_ADDONS, output.buildResult().toString());
    }

    private void saveAddons(ValueOutput output) {
        for (int i = 0; i < addons.length; i++) {
            VolumeBoxAddon addon = addons[i];
            if (addon != null) {
                ValueOutput child = output.child("addon" + i);
                child.putString("type", addon.getType().toString());
                addon.save(child);
            }
        }
    }

    private void loadAddons(ValueInput input) {
        for (int i = 0; i < addons.length; i++) {
            addons[i] = input.child("addon" + i).map(child -> {
                Identifier type = Identifier.tryParse(child.getStringOr("type", ""));
                VolumeBoxAddon addon = type == null ? null : VolumeBoxAddon.create(type);
                if (addon != null) addon.load(child);
                return addon;
            }).orElse(null);
        }
        for (VolumeBoxAddon addon : addons) {
            if (addon != null) addon.onBoxChanged(this);
        }
    }

    // Resizing

    public boolean isEditing() {
        return entityData.get(DATA_EDITOR) >= 0;
    }

    public boolean isEditedBy(Player player) {
        return entityData.get(DATA_EDITOR) == player.getId();
    }

    /** Starts resizing the box: the given corner follows where the player looks, at the distance it is now. */
    public void startEditing(Player player, int corner, double distance) {
        editor = player.getUUID();
        entityData.set(DATA_EDITOR, player.getId());
        held = getCornerBlock(corner ^ 7);
        heldDist = Math.max(1.5, distance);
        oldMin = getMin();
        oldMax = getMax();
    }

    public void confirmEditing() {
        stopEditing();
        for (VolumeBoxAddon addon : addons) {
            if (addon != null) addon.onBoxChanged(this);
        }
        addonsChanged();
    }

    public void cancelEditing() {
        setBox(oldMin, oldMax);
        stopEditing();
    }

    private void stopEditing() {
        editor = null;
        entityData.set(DATA_EDITOR, -1);
    }

    @Nullable
    public static VolumeBoxEntity getEditing(Player player) {
        return near(player.level(), player.getBoundingBox()).stream().filter(box -> box.isEditedBy(player)).findFirst()
            .orElse(null);
    }

    @Override
    public void tick() {
        if (!(level() instanceof ServerLevel level)) return;
        if (editor != null) {
            Player player = level.getPlayerByUUID(editor);
            if (player == null || player.isRemoved()) {
                cancelEditing();
            } else {
                BlockPos looking = BlockPos.containing(player.getEyePosition().add(player.getLookAngle().scale(heldDist)));
                int x = clamp(looking.getX(), held.getX()), y = clamp(looking.getY(), held.getY()), z = clamp(looking.getZ(), held.getZ());
                setBox(held, new BlockPos(x, y, z));
            }
        }
        if (lockedBy != null && tickCount % 20 == 0 && level.isLoaded(lockedBy)) {
            if (!(level.getBlockEntity(lockedBy) instanceof IVolumeBoxUser user && user.isUsing(this))) {
                lockedBy = null;
                entityData.set(DATA_LOCK, LOCK_NONE);
            }
        }
    }

    private static int clamp(int value, int held) {
        return Math.clamp(value, held - MAX_SIZE + 1, held + MAX_SIZE - 1);
    }

    // Entity

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_MIN, BlockPos.ZERO);
        builder.define(DATA_MAX, BlockPos.ZERO);
        builder.define(DATA_EDITOR, -1);
        builder.define(DATA_LOCK, LOCK_NONE);
        builder.define(DATA_ADDONS, "");
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (DATA_MIN.equals(accessor) || DATA_MAX.equals(accessor)) {
            setBoundingBox(makeBoundingBox());
        } else if (DATA_ADDONS.equals(accessor) && level().isClientSide()) {
            CompoundTag tag;
            try {
                String snbt = entityData.get(DATA_ADDONS);
                tag = snbt.isEmpty() ? new CompoundTag() : TagParser.parseCompoundFully(snbt);
            } catch (CommandSyntaxException e) {
                tag = new CompoundTag();
            }
            loadAddons(TagValueInput.create(ProblemReporter.DISCARDING, registryAccess(), tag));
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.store("min", BlockPos.CODEC, getMin());
        output.store("max", BlockPos.CODEC, getMax());
        if (lockedBy != null) {
            output.store("lockedBy", BlockPos.CODEC, lockedBy);
            output.putInt("lock", getLock());
        }
        saveAddons(output.child("addons"));
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        BlockPos min = input.read("min", BlockPos.CODEC).orElse(blockPosition());
        BlockPos max = input.read("max", BlockPos.CODEC).orElse(min);
        entityData.set(DATA_MIN, min);
        entityData.set(DATA_MAX, max);
        setBoundingBox(makeBoundingBox());
        lockedBy = input.read("lockedBy", BlockPos.CODEC).orElse(null);
        entityData.set(DATA_LOCK, lockedBy == null ? LOCK_NONE : input.getIntOr("lock", LOCK_READ));
        input.child("addons").ifPresent(this::loadAddons);
        addonsChanged();
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity other) {
        return false;
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE_ENTITY;
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    @Override
    protected boolean couldAcceptPassenger() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 256 * 256;
    }
}
