/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.tile;

import java.util.Arrays;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;

import buildcraft.api.mj.IMjConnector;
import buildcraft.api.mj.IMjConnectorProvider;
import buildcraft.api.mj.IMjReceiver;
import buildcraft.api.mj.MjAPI;
import buildcraft.api.mj.MjBattery;
import buildcraft.api.mj.MjBatteryReceiver;
import buildcraft.api.statements.IStatement;
import buildcraft.api.statements.IStatementParameter;
import buildcraft.api.statements.StatementManager;
import buildcraft.api.tiles.IAreaProvider;
import buildcraft.api.tiles.IControllable;
import buildcraft.api.tiles.IHasWork;
import buildcraft.builders.BCBuildersBlocks;
import buildcraft.builders.BuildEngine;
import buildcraft.builders.block.BlockFiller;
import buildcraft.builders.container.ContainerFiller;
import buildcraft.builders.filler.FilledTemplate;
import buildcraft.builders.filler.Pattern;
import buildcraft.builders.filler.Patterns;
import buildcraft.lib.inventory.IContainerDelegate;
import buildcraft.lib.inventory.ItemHandlerSimple;
import buildcraft.builders.BCBuildersConfig;
import buildcraft.lib.misc.ChunkLoader;
import buildcraft.lib.tile.TileBC;

/** Builds (or clears out) a pattern in the area marked out next to it, using the blocks in its inventory. */
public class TileFiller extends TileBC implements MenuProvider, IMjConnectorProvider, IHasWork, IControllable, IHasBuildBox, IContainerDelegate {
    private final ChunkLoader chunkLoader = new ChunkLoader();
    public static final int PARAM_COUNT = 4;
    public static final int INV_SIZE = 27;
    public final ItemHandlerSimple inv = new ItemHandlerSimple(INV_SIZE, (slot, stack) -> stack.getItem() instanceof BlockItem,
        this::onInventoryChanged);
    private final MjBattery battery = new MjBattery(16000 * MjAPI.MJ);
    private final IMjReceiver receiver = new MjBatteryReceiver(battery);

    @Nullable
    private BoundingBox box;
    private Pattern pattern = Patterns.NONE;
    private final IStatementParameter[] params = new IStatementParameter[PARAM_COUNT];
    private boolean canExcavate = true;
    private boolean inverted = false;
    private Mode mode = Mode.UNKNOWN;
    private int lockedTicks = 0;

    // Worked out again after loading
    @Nullable
    private BuildEngine engine;

    public TileFiller(BlockPos pos, BlockState state) {
        super(BCBuildersBlocks.FILLER_TILE.get(), pos, state);
        resetParams();
    }

    // Accessors (used by the GUI)

    public Pattern getPattern() {
        return pattern;
    }

    public IStatementParameter[] getParams() {
        return params;
    }

    public boolean canExcavate() {
        return canExcavate;
    }

    public boolean isInverted() {
        return inverted;
    }

    public boolean isLocked() {
        return lockedTicks > 0;
    }

    public int getLeftToBreak() {
        return engine == null ? 0 : engine.getLeftToBreak();
    }

    public int getLeftToPlace() {
        return engine == null ? 0 : engine.getLeftToPlace();
    }

    public boolean isFinished() {
        return engine != null && engine.isFinished(canExcavate);
    }

    public long getStoredPower() {
        return battery.getStored();
    }

    public long getCapacity() {
        return battery.getCapacity();
    }

    @Override
    @Nullable
    public BoundingBox getBox() {
        return box;
    }

    @Override
    public @Nullable IMjConnector getMjConnector(Direction side) {
        return receiver;
    }

    // Setup

    @Override
    public void onPlacedBy(@Nullable LivingEntity placer, ItemStack stack) {
        super.onPlacedBy(placer, stack);
        if (level == null || level.isClientSide()) return;
        Direction facing = getBlockState().getValue(BlockFiller.FACING);
        for (BlockPos areaPos : List.of(worldPosition.relative(facing.getOpposite()), worldPosition.above(), worldPosition.below(),
            worldPosition.relative(facing.getClockWise()), worldPosition.relative(facing.getCounterClockWise()),
            worldPosition.relative(facing))) {
            if (level.getBlockEntity(areaPos) instanceof IAreaProvider provider) {
                BlockPos min = provider.min(), max = provider.max();
                box = BoundingBox.fromCorners(min, max);
                provider.removeFromWorld();
                break;
            }
        }
        rebuildTemplate();
        sendNetworkUpdate();
    }

    private void resetParams() {
        for (int i = 0; i < PARAM_COUNT; i++) {
            params[i] = pattern.createParameter(i);
        }
    }

    // Changes from the GUI and gates

    public void setPattern(Pattern pattern) {
        if (this.pattern == pattern) return;
        this.pattern = pattern;
        resetParams();
        onSettingsChanged();
    }

    public void setParam(int index, @Nullable IStatementParameter param) {
        if (index < 0 || index >= PARAM_COUNT) return;
        params[index] = param;
        onSettingsChanged();
    }

    public void setPatternFromGate(Pattern pattern, IStatementParameter[] gateParams) {
        lockedTicks = 3;
        boolean changed = this.pattern != pattern;
        this.pattern = pattern;
        for (int i = 0; i < PARAM_COUNT; i++) {
            IStatementParameter param = i < gateParams.length ? gateParams[i] : null;
            if (param == null) param = pattern.createParameter(i);
            if (params[i] != param) changed = true;
            params[i] = param;
        }
        if (changed) onSettingsChanged();
    }

    public void toggleExcavate() {
        canExcavate = !canExcavate;
        setChanged();
    }

    public void toggleInverted() {
        inverted = !inverted;
        onSettingsChanged();
    }

    private void onSettingsChanged() {
        rebuildTemplate();
        setChanged();
        sendNetworkUpdate();
    }

    private void onInventoryChanged() {
        if (engine != null) engine.onResourcesChanged();
        setChanged();
    }

    // Building

    /** Works out the shape to build, and starts checking the area again. */
    private void rebuildTemplate() {
        engine = null;
        if (box == null || !(level instanceof ServerLevel serverLevel)) return;
        FilledTemplate t = new FilledTemplate(box.getXSpan(), box.getYSpan(), box.getZSpan());
        IStatementParameter[] used = Arrays.copyOf(params, pattern.maxParameters());
        for (int i = 0; i < used.length; i++) {
            if (used[i] == null) used[i] = pattern.createParameter(i);
        }
        if (!pattern.fillTemplate(t, used)) return;
        if (inverted) t.invert();
        BlockState air = Blocks.AIR.defaultBlockState();
        engine = new BuildEngine(serverLevel, worldPosition, box, index -> t.get(index) ? null : air);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide()) return;
        if (lockedTicks > 0) lockedTicks--;
        if (level instanceof ServerLevel serverLevel) {
            chunkLoader.tick(serverLevel, ChunkLoader.chunksFor(worldPosition, engine == null ? null : box),
                BCBuildersConfig.chunkLoadMachines);
        }
        if (engine != null) {
            engine.tick(battery, inv, canExcavate, mode != Mode.OFF);
        }
    }

    // IHasWork

    @Override
    public boolean hasWork() {
        return mode != Mode.OFF && engine != null && !isFinished();
    }

    // IControllable

    @Override
    public Mode getControlMode() {
        return mode;
    }

    @Override
    public void setControlMode(Mode mode) {
        this.mode = mode;
        setChanged();
    }

    @Override
    public boolean acceptsControlMode(Mode mode) {
        return mode == Mode.ON || mode == Mode.OFF;
    }

    // Saving

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        inv.save(output, "inv");
        battery.save(output, "battery");
        if (box != null) {
            output.putIntArray("box", new int[] { box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ() });
        }
        output.putString("pattern", pattern.getUniqueTag());
        for (int i = 0; i < PARAM_COUNT; i++) {
            if (params[i] != null) {
                ValueOutput param = output.child("param" + i);
                param.putString("kind", params[i].getUniqueTag());
                params[i].save(param);
            }
        }
        output.putBoolean("excavate", canExcavate);
        output.putBoolean("inverted", inverted);
        output.putString("mode", mode.name());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        inv.load(input, "inv");
        battery.load(input, "battery");
        box = input.getIntArray("box").filter(a -> a.length == 6)
            .map(a -> new BoundingBox(a[0], a[1], a[2], a[3], a[4], a[5])).orElse(null);
        IStatement statement = StatementManager.getStatement(input.getStringOr("pattern", ""));
        pattern = statement instanceof Pattern p ? p : Patterns.NONE;
        for (int i = 0; i < PARAM_COUNT; i++) {
            params[i] = input.child("param" + i).map(StatementManager::loadParameter).orElse(null);
        }
        canExcavate = input.getBooleanOr("excavate", true);
        inverted = input.getBooleanOr("inverted", false);
        try {
            mode = Mode.valueOf(input.getStringOr("mode", "UNKNOWN"));
        } catch (IllegalArgumentException e) {
            mode = Mode.UNKNOWN;
        }
        rebuildTemplate();
    }

    @Override
    public void setLevel(net.minecraft.world.level.Level level) {
        super.setLevel(level);
        if (!level.isClientSide() && engine == null) {
            rebuildTemplate();
        }
    }

    // GUI

    @Override
    public InteractionResult onActivated(Player player, InteractionHand hand, ItemStack held, BlockHitResult hit) {
        if (!isClient()) {
            player.openMenu(this);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ContainerFiller(id, inventory, this);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level instanceof ServerLevel serverLevel) chunkLoader.releaseAll(serverLevel);
    }

    @Override
    public Container getDelegate() {
        return inv;
    }
}
