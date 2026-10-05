package buildcraft.lib.tile;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Base class for BuildCraft block entities. The client gets a copy of the saved data whenever
 * {@link #sendNetworkUpdate()} is called, so anything the renderer needs must be saved in
 * {@link #saveAdditional} and read in {@link #loadAdditional}. */
public abstract class TileBC extends BlockEntity {
    private boolean updateQueued = false;

    protected TileBC(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public boolean isClient() {
        return level != null && level.isClientSide();
    }

    /** Sends this block entity's data to clients at the end of the tick. */
    public void sendNetworkUpdate() {
        updateQueued = true;
    }

    /** Called every tick on both sides by {@link buildcraft.lib.block.BlockBCTile}. */
    public void tick() {
        if (updateQueued && level != null && !level.isClientSide()) {
            updateQueued = false;
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    public void onPlacedBy(@Nullable LivingEntity placer, ItemStack stack) {}

    public void onNeighbourChanged() {}

    /** Called when a player right clicks this block, with or without an item. */
    public InteractionResult onActivated(Player player, InteractionHand hand, ItemStack held, BlockHitResult hit) {
        return InteractionResult.PASS;
    }
}
