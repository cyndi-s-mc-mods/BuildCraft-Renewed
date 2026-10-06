/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.factory.tile;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;

import buildcraft.api.recipes.RefineryRecipes;
import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.block.BlockHeatExchange;
import buildcraft.factory.block.BlockHeatExchange.Part;
import buildcraft.lib.fluid.BCFluidStack;
import buildcraft.lib.fluid.FluidUtilBC;
import buildcraft.lib.fluid.IFluidHandlerBC;
import buildcraft.lib.fluid.IFluidHandlerProvider;
import buildcraft.lib.fluid.Tank;
import buildcraft.lib.platform.Platform;
import buildcraft.lib.tile.TileBC;

/** One block of a heat exchanger.
 * <ul>
 * <li>The start takes the fluid to heat from below, and gives out the cooled fluid on its outer side.</li>
 * <li>The end takes the hot fluid on its outer side, and gives out the heated fluid from its top.</li>
 * </ul>
 * The more middle sections there are (1 to 3) the faster it works. */
public class TileHeatExchange extends TileBC implements IFluidHandlerProvider {
    /** The most fluid moved per tick, for each number of middle sections. */
    private static final int[] FLUID_MULT = { 5, 10, 20 };
    private static final int MAX_LENGTH = 5;
    /** How long the exchanger takes to warm up before it starts moving fluid. */
    private static final int WARMUP_TICKS = 120;

    public final Tank tankInput;
    public final Tank tankOutput;
    private boolean checkStructure = true;
    @Nullable
    private BlockPos endPos;
    private int middleCount = 0;
    private int progress = 0;
    private boolean running = false;

    public TileHeatExchange(BlockPos pos, BlockState state) {
        super(BCFactoryBlocks.HEAT_EXCHANGE_TILE.get(), pos, state);
        tankInput = new Tank("input", 2 * BCFluidStack.BUCKET, this::isValidInput, this::setChanged);
        tankOutput = new Tank("output", 2 * BCFluidStack.BUCKET, this::setChanged);
        tankOutput.canFill = false;
    }

    private Part getPart() {
        BlockState state = getBlockState();
        return state.hasProperty(BlockHeatExchange.PART) ? state.getValue(BlockHeatExchange.PART) : Part.MIDDLE;
    }

    @Nullable
    private Direction getFacing() {
        BlockState state = getBlockState();
        return state.hasProperty(BlockHeatExchange.FACING) ? state.getValue(BlockHeatExchange.FACING) : null;
    }

    private boolean isValidInput(BCFluidStack fluid) {
        return switch (getPart()) {
            case START -> RefineryRecipes.getHeatable(fluid) != null;
            case END -> RefineryRecipes.getCoolable(fluid) != null;
            case MIDDLE -> false;
        };
    }

    @Override
    public void onNeighbourChanged() {
        checkStructure = true;
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide()) return;
        if (checkStructure) {
            checkStructure = false;
            updateStructure();
        }
        if (getPart() == Part.START) {
            craft();
            output();
        }
    }

    /** @return Every heat exchanger in this line, from the start to the end. */
    private Deque<TileHeatExchange> findLine() {
        Deque<TileHeatExchange> line = new ArrayDeque<>();
        Direction facing = getFacing();
        line.add(this);
        if (level == null || facing == null) return line;
        Direction toStart = facing.getClockWise();
        for (int i = 1; i < MAX_LENGTH + 1; i++) {
            if (level.getBlockEntity(worldPosition.relative(toStart, i)) instanceof TileHeatExchange other && other.getFacing() == facing) {
                line.addFirst(other);
            } else {
                break;
            }
        }
        for (int i = 1; i < MAX_LENGTH + 1; i++) {
            if (level.getBlockEntity(worldPosition.relative(toStart.getOpposite(), i)) instanceof TileHeatExchange other
                && other.getFacing() == facing) {
                line.addLast(other);
            } else {
                break;
            }
        }
        return line;
    }

    private void updateStructure() {
        Deque<TileHeatExchange> line = findLine();
        boolean formed = line.size() >= 3 && line.size() <= MAX_LENGTH;
        TileHeatExchange start = line.getFirst();
        TileHeatExchange end = line.getLast();
        for (TileHeatExchange tile : line) {
            Part part = !formed ? Part.MIDDLE : tile == start ? Part.START : tile == end ? Part.END : Part.MIDDLE;
            tile.checkStructure = false;
            tile.setPart(part);
            tile.endPos = null;
        }
        if (formed) {
            start.endPos = end.worldPosition;
            start.middleCount = line.size() - 2;
        } else {
            start.running = false;
            start.progress = 0;
        }
    }

    private void setPart(Part part) {
        if (level == null) return;
        BlockState state = getBlockState();
        if (state.hasProperty(BlockHeatExchange.PART) && state.getValue(BlockHeatExchange.PART) != part) {
            level.setBlock(worldPosition, state.setValue(BlockHeatExchange.PART, part), Block.UPDATE_CLIENTS);
        }
    }

    @Nullable
    private TileHeatExchange getEnd() {
        if (level == null || endPos == null) return null;
        return level.getBlockEntity(endPos) instanceof TileHeatExchange end && end.getPart() == Part.END ? end : null;
    }

    public boolean isRunning() {
        return running;
    }

    private void craft() {
        TileHeatExchange end = getEnd();
        if (end == null || middleCount < 1) {
            stop();
            return;
        }
        Tank coolIn = end.tankInput;
        Tank coolOut = tankOutput;
        Tank heatIn = tankInput;
        Tank heatOut = end.tankOutput;
        RefineryRecipes.Coolable coolRecipe = RefineryRecipes.getCoolable(coolIn.getFluid());
        RefineryRecipes.Heatable heatRecipe = RefineryRecipes.getHeatable(heatIn.getFluid());
        if (coolRecipe == null || heatRecipe == null || coolRecipe.heatFrom() <= heatRecipe.heatFrom()) {
            stop();
            return;
        }
        int max = FLUID_MULT[Math.min(middleCount, FLUID_MULT.length) - 1];
        int amount = Math.min(max, Math.min(coolIn.getFluidAmount(), heatIn.getFluidAmount()));
        if (coolRecipe.out() != null) {
            amount = Math.min(amount, coolOut.fillInternal(coolRecipe.out().withAmount(max), true));
        }
        if (heatRecipe.out() != null) {
            amount = Math.min(amount, heatOut.fillInternal(heatRecipe.out().withAmount(max), true));
        }
        if (amount <= 0) {
            stop();
            return;
        }
        if (!running) {
            if (++progress >= WARMUP_TICKS) {
                running = true;
            }
            return;
        }
        if (coolRecipe.out() != null) coolOut.fillInternal(coolRecipe.out().withAmount(amount), false);
        if (heatRecipe.out() != null) heatOut.fillInternal(heatRecipe.out().withAmount(amount), false);
        coolIn.drainInternal(amount, false);
        heatIn.drainInternal(amount, false);
    }

    private void stop() {
        running = false;
        if (progress > 0) progress--;
    }

    /** Pushes the outputs into whatever is next to them. */
    private void output() {
        Direction facing = getFacing();
        if (level == null || facing == null) return;
        Direction outSide = facing.getClockWise();
        moveFluid(tankOutput, Platform.INSTANCE.getFluidHandler(level, worldPosition.relative(outSide), outSide.getOpposite()));
        TileHeatExchange end = getEnd();
        if (end != null) {
            moveFluid(end.tankOutput, Platform.INSTANCE.getFluidHandler(level, end.worldPosition.above(), Direction.DOWN));
        }
    }

    private static void moveFluid(Tank from, @Nullable IFluidHandlerBC to) {
        if (to == null || from.isEmpty()) return;
        BCFluidStack available = from.drainInternal(BCFluidStack.BUCKET, true);
        int filled = to.fill(available, false);
        if (filled > 0) {
            from.drainInternal(filled, false);
        }
    }

    /** Rotates a lone heat exchanger by 90 degrees, or turns a whole line around (swapping its start and end). */
    public void rotate() {
        Direction facing = getFacing();
        if (level == null || facing == null) return;
        List<TileHeatExchange> line = new ArrayList<>(findLine());
        Direction newFacing = line.size() == 1 ? facing.getClockWise() : facing.getOpposite();
        for (TileHeatExchange tile : line) {
            level.setBlock(tile.worldPosition, tile.getBlockState().setValue(BlockHeatExchange.FACING, newFacing), Block.UPDATE_ALL);
            tile.checkStructure = true;
        }
    }

    @Override
    public @Nullable IFluidHandlerBC getFluidHandler(@Nullable Direction side) {
        Direction facing = getFacing();
        if (side == null || facing == null) return null;
        return switch (getPart()) {
            case START -> side == Direction.DOWN ? tankInput : side == facing.getClockWise() ? tankOutput : null;
            case END -> side == Direction.UP ? tankOutput : side == facing.getCounterClockWise() ? tankInput : null;
            case MIDDLE -> null;
        };
    }

    @Override
    public InteractionResult onActivated(Player player, InteractionHand hand, ItemStack held, BlockHitResult hit) {
        if (getPart() == Part.MIDDLE || held.isEmpty()) return InteractionResult.PASS;
        if (FluidUtilBC.interactWithHandler(player, hand, tankInput) || FluidUtilBC.interactWithHandler(player, hand, tankOutput)) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tankInput.save(output);
        tankOutput.save(output);
        output.putInt("progress", progress);
        output.putBoolean("running", running);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        tankInput.load(input);
        tankOutput.load(input);
        progress = input.getIntOr("progress", 0);
        running = input.getBooleanOr("running", false);
        checkStructure = true;
    }

    @Override
    public void preRemoveSideEffects(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) buildcraft.core.item.ItemFragileFluidShard.dropFluids(level, pos, tankInput, tankOutput);
    }
}
