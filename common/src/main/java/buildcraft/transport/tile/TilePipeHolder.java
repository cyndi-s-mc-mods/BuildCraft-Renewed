package buildcraft.transport.tile;

import java.util.ArrayList;
import java.util.List;
import java.util.EnumMap;
import java.util.Map;


import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import buildcraft.api.core.EnumPipePart;
import buildcraft.api.mj.IMjConnector;
import buildcraft.api.tools.IToolWrench;
import buildcraft.api.transport.pluggable.IItemPluggable;
import buildcraft.api.transport.pluggable.PipePluggable;
import buildcraft.api.transport.pluggable.PluggableDefinition;
import buildcraft.api.mj.IMjConnectorProvider;
import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.IPipeHolder;
import buildcraft.api.transport.pipe.PipeEvent;
import buildcraft.lib.fluid.IFluidHandlerBC;
import buildcraft.lib.fluid.IFluidHandlerProvider;
import buildcraft.lib.inventory.IItemHandlerProvider;
import buildcraft.lib.inventory.IItemTransactor;
import buildcraft.lib.tile.TileBC;
import buildcraft.transport.BCTransportBlocks;
import buildcraft.transport.block.BlockPipe;
import buildcraft.transport.pipe.Pipe;
import buildcraft.transport.pipe.PipeEventBus;

public class TilePipeHolder extends TileBC implements IPipeHolder, IMjConnectorProvider, IFluidHandlerProvider, IItemHandlerProvider {
    private final Pipe pipe;
    private final PipeEventBus eventBus = new PipeEventBus();
    private boolean blockStateDirty = true;
    private final Map<Direction, PipePluggable> pluggables = new EnumMap<>(Direction.class);

    public TilePipeHolder(BlockPos pos, BlockState state) {
        super(BCTransportBlocks.PIPE_HOLDER.get(), pos, state);
        pipe = new Pipe(this, ((BlockPipe) state.getBlock()).definition);
        eventBus.registerHandler(pipe.behaviour);
        eventBus.registerHandler(pipe.flow);
    }

    @Override
    public Pipe getPipe() {
        return pipe;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        pipe.save(output.child("pipe"));
        if (!pluggables.isEmpty()) {
            ValueOutput plugs = output.child("plugs");
            for (Map.Entry<Direction, PipePluggable> entry : pluggables.entrySet()) {
                ValueOutput plug = plugs.child(entry.getKey().getSerializedName());
                plug.putString("id", entry.getValue().definition.id.toString());
                entry.getValue().save(plug);
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("pipe").ifPresent(pipe::load);
        Map<Direction, PipePluggable> old = new EnumMap<>(pluggables);
        pluggables.clear();
        input.child("plugs").ifPresent(plugs -> {
            for (Direction side : Direction.values()) {
                plugs.child(side.getSerializedName()).ifPresent(plug -> {
                    Identifier id = Identifier.tryParse(plug.getStringOr("id", ""));
                    PluggableDefinition def = id == null ? null : PluggableDefinition.get(id);
                    if (def != null) {
                        pluggables.put(side, def.loader.load(def, this, side, plug));
                    }
                });
            }
        });
        if (!old.keySet().equals(pluggables.keySet())) {
            pipe.markForUpdate();
        }
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        pipe.markForUpdate();
    }

    @Override
    public void tick() {
        for (PipePluggable plug : List.copyOf(pluggables.values())) {
            plug.onTick();
        }
        pipe.onTick();
        if (blockStateDirty && level != null && !level.isClientSide()) {
            blockStateDirty = false;
            updateBlockState();
        }
        super.tick();
    }

    private void updateBlockState() {
        BlockState state = getBlockState();
        BlockState newState = state;
        for (Direction dir : Direction.values()) {
            newState = newState.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(dir), pipe.isConnected(dir));
        }
        newState = pipe.behaviour.updateBlockState(newState);
        if (newState != state && level != null) {
            level.setBlock(worldPosition, newState, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null && !level.isClientSide()) {
            List<ItemStack> drops = new ArrayList<>();
            pipe.addDrops(drops);
            for (PipePluggable plug : pluggables.values()) {
                plug.onRemove();
                plug.addDrops(drops);
            }
            for (ItemStack stack : drops) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
            }
        }
    }

    /** @return The part of the pipe that the player clicked on. */
    public static EnumPipePart getPartHit(BlockPos pos, Vec3 hit) {
        double x = hit.x - pos.getX(), y = hit.y - pos.getY(), z = hit.z - pos.getZ();
        final double lo = 0.25 - 1e-4, hi = 0.75 + 1e-4;
        boolean inX = x >= lo && x <= hi, inY = y >= lo && y <= hi, inZ = z >= lo && z <= hi;
        if (inX && inY && inZ) return EnumPipePart.CENTER;
        if (inX && inZ) return y < lo ? EnumPipePart.DOWN : EnumPipePart.UP;
        if (inX && inY) return z < lo ? EnumPipePart.NORTH : EnumPipePart.SOUTH;
        if (inY && inZ) return x < lo ? EnumPipePart.WEST : EnumPipePart.EAST;
        return EnumPipePart.CENTER;
    }

    @Override
    public InteractionResult onActivated(Player player, InteractionHand hand, ItemStack held, BlockHitResult hit) {
        EnumPipePart part = getPartHit(worldPosition, hit.getLocation());
        Direction plugSide = getPluggableHit(hit.getLocation());
        if (plugSide != null) {
            PipePluggable plug = pluggables.get(plugSide);
            if (player.isShiftKeyDown() && (held.isEmpty() || held.getItem() instanceof IToolWrench)) {
                // Take the pluggable off
                if (!isClient() && plug != null) {
                    replacePluggable(plugSide, null);
                    if (!player.getAbilities().instabuild) {
                        List<ItemStack> drops = new ArrayList<>();
                        plug.addDrops(drops);
                        for (ItemStack stack : drops) {
                            if (!player.getInventory().add(stack) && level != null) {
                                Containers.dropItemStack(level, player.getX(), player.getY(), player.getZ(), stack);
                            }
                        }
                    }
                }
                return InteractionResult.SUCCESS;
            }
            if (plug != null) {
                InteractionResult result = plug.onPluggableActivate(player, hand, hit);
                if (result != InteractionResult.PASS) return result;
            }
        }
        if (held.getItem() instanceof IItemPluggable itemPlug) {
            Direction side = part.face != null ? part.face : hit.getDirection();
            if (pluggables.get(side) == null) {
                if (!isClient()) {
                    PipePluggable plug = itemPlug.onPlace(held, this, side, player, hand);
                    if (plug == null) return InteractionResult.FAIL;
                    replacePluggable(side, plug);
                    plug.onPlacedBy(player);
                    if (!player.getAbilities().instabuild) {
                        held.shrink(1);
                    }
                    if (level != null) {
                        level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.METAL_PLACE,
                            net.minecraft.sounds.SoundSource.BLOCKS, 1, 1);
                    }
                }
                return InteractionResult.SUCCESS;
            }
        }
        InteractionResult result = pipe.behaviour.onPipeActivate(player, hand, held, hit, part);
        if (result != InteractionResult.PASS) {
            return result;
        }
        MenuProvider menu = pipe.behaviour.getMenuProvider();
        if (menu != null && !(held.getItem() instanceof net.minecraft.world.item.BlockItem)) {
            if (!isClient()) {
                player.openMenu(menu);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    // IPipeHolder

    @Override
    public Level getPipeWorld() {
        return level;
    }

    @Override
    public BlockPos getPipePos() {
        return worldPosition;
    }

    @Override
    public @Nullable BlockEntity getNeighbourTile(Direction side) {
        if (level == null) return null;
        BlockPos offset = worldPosition.relative(side);
        if (!level.isLoaded(offset)) return null;
        return level.getBlockEntity(offset);
    }

    @Override
    public @Nullable IPipe getNeighbourPipe(Direction side) {
        return getNeighbourTile(side) instanceof TilePipeHolder holder ? holder.pipe : null;
    }

    @Override
    public boolean fireEvent(PipeEvent event) {
        return eventBus.fireEvent(event);
    }

    @Override
    public void scheduleNetworkUpdate() {
        sendNetworkUpdate();
        setChanged();
    }

    @Override
    public void scheduleBlockStateUpdate() {
        blockStateDirty = true;
    }

    @Override
    public @Nullable PipePluggable getPluggable(Direction side) {
        return pluggables.get(side);
    }

    @Override
    public @Nullable PipePluggable replacePluggable(Direction side, @Nullable PipePluggable with) {
        PipePluggable old = with == null ? pluggables.remove(side) : pluggables.put(side, with);
        if (old != null && old != with) {
            old.onRemove();
        }
        pipe.markForUpdate();
        scheduleNetworkUpdate();
        if (level != null) {
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
        return old;
    }

    public Map<Direction, PipePluggable> getPluggables() {
        return pluggables;
    }

    /** @return The side of the pluggable at the given point (in world coordinates), or null. */
    @Nullable
    public Direction getPluggableHit(Vec3 hit) {
        Vec3 local = hit.subtract(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
        for (Map.Entry<Direction, PipePluggable> entry : pluggables.entrySet()) {
            if (entry.getValue().getBoundingBox().inflate(1e-3).contains(local)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /** @return The shape of every pluggable, for collisions. */
    public VoxelShape getPluggableShape() {
        VoxelShape shape = Shapes.empty();
        for (PipePluggable plug : pluggables.values()) {
            shape = Shapes.or(shape, Shapes.create(plug.getBoundingBox()));
        }
        return shape;
    }

    // Exposed to other mods

    @Override
    public @Nullable IMjConnector getMjConnector(Direction side) {
        PipePluggable plug = pluggables.get(side);
        if (plug != null) {
            return plug.getMjConnector();
        }
        if (!pipe.isConnected(side)) return null;
        IMjConnector connector = pipe.behaviour.getMjConnector(side);
        return connector != null ? connector : pipe.flow.getMjConnector(side);
    }

    @Override
    public @Nullable IFluidHandlerBC getFluidHandler(@Nullable Direction side) {
        return pipe.flow.getFluidHandler(side);
    }

    @Override
    public @Nullable IItemTransactor getItemTransactor(@Nullable Direction side) {
        return pipe.flow.getItemTransactor(side);
    }
}
