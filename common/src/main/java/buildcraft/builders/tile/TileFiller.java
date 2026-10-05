/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders.tile;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

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
import buildcraft.builders.block.BlockFiller;
import buildcraft.builders.container.ContainerFiller;
import buildcraft.builders.filler.FilledTemplate;
import buildcraft.builders.filler.Pattern;
import buildcraft.builders.filler.Patterns;
import buildcraft.lib.inventory.ItemHandlerSimple;
import buildcraft.lib.misc.BlockUtil;
import buildcraft.lib.misc.InventoryUtil;
import buildcraft.lib.tile.TileBC;

/** Builds (or clears out) a pattern in the area marked out next to it, using the blocks in its inventory. */
public class TileFiller extends TileBC implements MenuProvider, IMjConnectorProvider, IHasWork, IControllable {
    public static final int PARAM_COUNT = 4;
    public static final int INV_SIZE = 27;
    private static final long MAX_POWER_PER_TICK = 256 * MjAPI.MJ;
    private static final int MAX_SCAN_PER_TICK = 4096;
    private static final int MAX_TASKS_PER_TICK = 4;

    private static final byte UNKNOWN = 0, CORRECT = 1, TO_BREAK = 2, TO_PLACE = 3;

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

    // Building state, worked out again after loading
    @Nullable
    private FilledTemplate template;
    private byte[] checks = new byte[0];
    private int[] breakOrder = new int[0];
    private int[] placeOrder = new int[0];
    private int scanIndex = 0;
    private boolean fullScanDone = false;
    private int breakCursor = 0;
    private int placeCursor = 0;
    private int leftToBreak = 0;
    private int leftToPlace = 0;
    /** True when the last place attempt found nothing in the inventory to place. */
    private boolean missingBlocks = false;
    private int taskIndex = -1;
    private boolean taskIsBreak;
    private long taskPower;

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
        return leftToBreak;
    }

    public int getLeftToPlace() {
        return leftToPlace;
    }

    public boolean isFinished() {
        return template != null && fullScanDone && leftToPlace == 0 && (leftToBreak == 0 || !canExcavate);
    }

    public long getStoredPower() {
        return battery.getStored();
    }

    public long getCapacity() {
        return battery.getCapacity();
    }

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
        missingBlocks = false;
        setChanged();
    }

    // Building

    /** Works out the shape to build, and starts checking the area again. */
    private void rebuildTemplate() {
        template = null;
        checks = new byte[0];
        breakOrder = placeOrder = new int[0];
        taskIndex = -1;
        scanIndex = 0;
        fullScanDone = false;
        breakCursor = placeCursor = 0;
        leftToBreak = leftToPlace = 0;
        if (box == null || level == null || level.isClientSide()) return;
        FilledTemplate t = new FilledTemplate(box.getXSpan(), box.getYSpan(), box.getZSpan());
        IStatementParameter[] used = Arrays.copyOf(params, pattern.maxParameters());
        for (int i = 0; i < used.length; i++) {
            if (used[i] == null) used[i] = pattern.createParameter(i);
        }
        if (!pattern.fillTemplate(t, used)) return;
        if (inverted) t.invert();
        template = t;
        int count = t.sizeX * t.sizeY * t.sizeZ;
        checks = new byte[count];
        double cx = t.sizeX / 2.0, cz = t.sizeZ / 2.0;
        Integer[] indices = new Integer[count];
        for (int i = 0; i < count; i++) indices[i] = i;
        // Break from the top down, place from the bottom up; each layer from the middle outwards
        Comparator<Integer> horizontal = Comparator.comparingDouble(i -> {
            double dx = i % t.sizeX + 0.5 - cx, dz = (i / t.sizeX) % t.sizeZ + 0.5 - cz;
            return dx * dx + dz * dz;
        });
        Comparator<Integer> byY = Comparator.comparingInt(i -> i / (t.sizeX * t.sizeZ));
        Integer[] sorted = indices.clone();
        Arrays.sort(sorted, byY.reversed().thenComparing(horizontal));
        breakOrder = Arrays.stream(sorted).mapToInt(Integer::intValue).toArray();
        Arrays.sort(sorted, byY.thenComparing(horizontal));
        placeOrder = Arrays.stream(sorted).mapToInt(Integer::intValue).toArray();
    }

    private BlockPos posOf(int index) {
        FilledTemplate t = template;
        int x = index % t.sizeX, z = (index / t.sizeX) % t.sizeZ, y = index / (t.sizeX * t.sizeZ);
        return new BlockPos(box.minX() + x, box.minY() + y, box.minZ() + z);
    }

    private static boolean isEmpty(BlockState state) {
        return state.isAir() || (!state.getFluidState().isEmpty() && state.canBeReplaced());
    }

    private byte check(int index) {
        BlockPos pos = posOf(index);
        if (pos.equals(worldPosition) || !level.isLoaded(pos)) return CORRECT;
        BlockState state = level.getBlockState(pos);
        if (template.get(index)) {
            return state.canBeReplaced() ? TO_PLACE : CORRECT;
        }
        if (isEmpty(state) || state.getDestroySpeed(level, pos) < 0) return CORRECT;
        return TO_BREAK;
    }

    private void setCheck(int index, byte result) {
        byte old = checks[index];
        if (old == result) return;
        if (old == TO_BREAK) leftToBreak--;
        if (old == TO_PLACE) leftToPlace--;
        if (result == TO_BREAK) leftToBreak++;
        if (result == TO_PLACE) leftToPlace++;
        checks[index] = result;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide()) return;
        if (lockedTicks > 0) lockedTicks--;
        if (template == null) {
            if (box != null && pattern != Patterns.NONE && checks.length == 0 && level.getGameTime() % 20 == 0) {
                rebuildTemplate();
            }
            return;
        }
        // Look over part of the area
        int count = checks.length;
        for (int n = 0; n < Math.min(MAX_SCAN_PER_TICK, count); n++) {
            setCheck(scanIndex, check(scanIndex));
            scanIndex++;
            if (scanIndex >= count) {
                scanIndex = 0;
                fullScanDone = true;
                breakCursor = placeCursor = 0;
                missingBlocks = false;
            }
        }
        if (mode == Mode.OFF || !fullScanDone) return;
        long powerLeft = MAX_POWER_PER_TICK;
        for (int t = 0; t < MAX_TASKS_PER_TICK && powerLeft > 0; t++) {
            if (taskIndex < 0 && !pickTask()) break;
            BlockPos pos = posOf(taskIndex);
            long target = taskIsBreak ? BlockUtil.computeBlockBreakPower(level, pos)
                : (long) (Math.sqrt(pos.distSqr(worldPosition)) * 10 * MjAPI.MJ);
            long wanted = Math.min(target - taskPower, powerLeft);
            long got = battery.extractPower(0, wanted);
            taskPower += got;
            powerLeft -= got;
            if (taskPower < target) break;
            doTask(pos);
            taskIndex = -1;
            taskPower = 0;
        }
    }

    private boolean pickTask() {
        if (canExcavate && leftToBreak > 0) {
            for (; breakCursor < breakOrder.length; breakCursor++) {
                int index = breakOrder[breakCursor];
                if (checks[index] == TO_BREAK) {
                    startTask(index, true);
                    return true;
                }
            }
        }
        // Like the original, nothing is placed while there are still blocks to break
        if (leftToPlace > 0 && (!canExcavate || leftToBreak == 0) && !missingBlocks) {
            if (findBlockSlot() < 0) {
                missingBlocks = true;
                return false;
            }
            for (; placeCursor < placeOrder.length; placeCursor++) {
                int index = placeOrder[placeCursor];
                if (checks[index] == TO_PLACE) {
                    startTask(index, false);
                    return true;
                }
            }
        }
        return false;
    }

    private void startTask(int index, boolean isBreak) {
        taskIndex = index;
        taskIsBreak = isBreak;
        taskPower = 0;
    }

    private int findBlockSlot() {
        for (int i = 0; i < INV_SIZE; i++) {
            if (inv.getItem(i).getItem() instanceof BlockItem) return i;
        }
        return -1;
    }

    private void doTask(BlockPos pos) {
        byte now = check(taskIndex);
        setCheck(taskIndex, now);
        if (taskIsBreak) {
            if (now != TO_BREAK) return;
            List<ItemStack> drops = BlockUtil.breakBlockAndGetDrops((ServerLevel) level, pos);
            if (drops != null) {
                for (ItemStack drop : drops) {
                    InventoryUtil.addToBestAcceptor(level, worldPosition, drop);
                }
            }
        } else {
            if (now != TO_PLACE) return;
            int slot = findBlockSlot();
            if (slot < 0) {
                missingBlocks = true;
                return;
            }
            ItemStack stack = inv.getItem(slot);
            BlockItem item = (BlockItem) stack.getItem();
            BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
            InteractionResult result = item.place(new BlockPlaceContext(level, null, InteractionHand.MAIN_HAND, stack, hit));
            if (result.consumesAction()) {
                inv.setChanged();
            } else {
                // Can't be placed here (for example, an entity is in the way): skip it until the next pass
                placeCursor++;
            }
        }
        setCheck(taskIndex, check(taskIndex));
    }

    // IHasWork

    @Override
    public boolean hasWork() {
        return mode != Mode.OFF && template != null && !isFinished();
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
        if (level != null && !level.isClientSide()) {
            rebuildTemplate();
        } else {
            template = null;
        }
    }

    @Override
    public void setLevel(net.minecraft.world.level.Level level) {
        super.setLevel(level);
        if (!level.isClientSide() && template == null) {
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
        if (level != null) {
            Containers.dropContents(level, pos, inv);
        }
    }
}
