package buildcraft.lib.fluid;

import java.util.IdentityHashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import buildcraft.lib.platform.Platform;

/** Moving fluids between buckets and BuildCraft's tanks. */
public final class FluidUtilBC {
    private static Map<Item, Fluid> bucketToFluid;

    private FluidUtilBC() {}

    /** @return The fluid in a filled bucket (from any mod), or {@link Fluids#EMPTY}. */
    public static Fluid getBucketFluid(ItemStack stack) {
        if (bucketToFluid == null) {
            Map<Item, Fluid> map = new IdentityHashMap<>();
            for (Fluid fluid : BuiltInRegistries.FLUID) {
                if (fluid.isSource(fluid.defaultFluidState())) {
                    Item bucket = fluid.getBucket();
                    if (bucket != Items.AIR && bucket != Items.BUCKET) {
                        map.putIfAbsent(bucket, fluid);
                    }
                }
            }
            bucketToFluid = map;
        }
        return bucketToFluid.getOrDefault(stack.getItem(), Fluids.EMPTY);
    }

    /** Pushes as much fluid out of the tank as the neighbouring blocks accept. */
    public static void pushFluidAround(Level level, BlockPos pos, Tank tank) {
        for (Direction side : Direction.values()) {
            if (tank.isEmpty()) return;
            IFluidHandlerBC handler = Platform.INSTANCE.getFluidHandler(level, pos.relative(side), side.getOpposite());
            if (handler == null) continue;
            int filled = handler.fill(tank.getFluid(), false);
            if (filled > 0) {
                tank.drainInternal(filled, false);
            }
        }
    }

    /** Fills or empties the held bucket into or out of the handler.
     * @return True if anything happened. */
    public static boolean interactWithHandler(Player player, InteractionHand hand, IFluidHandlerBC handler) {
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) return false;
        Fluid inBucket = getBucketFluid(held);
        if (inBucket != Fluids.EMPTY) {
            BCFluidStack stack = BCFluidStack.of(inBucket, BCFluidStack.BUCKET);
            if (handler.fill(stack, true) == BCFluidStack.BUCKET) {
                if (!player.level().isClientSide()) {
                    handler.fill(stack, false);
                    player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, new ItemStack(Items.BUCKET)));
                    player.level().playSound(null, player.blockPosition(), SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1, 1);
                }
                return true;
            }
            return false;
        }
        if (held.is(Items.BUCKET)) {
            BCFluidStack drained = handler.drain(f -> f.getFluid().getBucket() != Items.AIR, BCFluidStack.BUCKET, true);
            if (drained.getAmount() == BCFluidStack.BUCKET) {
                if (!player.level().isClientSide()) {
                    handler.drain(drained, false);
                    ItemStack filled = new ItemStack(drained.getFluid().getBucket());
                    player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, filled));
                    player.level().playSound(null, player.blockPosition(), SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1, 1);
                }
                return true;
            }
        }
        return false;
    }
}
