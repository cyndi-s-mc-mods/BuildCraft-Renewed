package buildcraft.core.item;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;

import buildcraft.api.tools.IToolWrench;
import buildcraft.lib.block.IWrenchable;
import buildcraft.lib.misc.RotationUtil;

public class ItemWrench extends Item implements IToolWrench {
    public ItemWrench(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canWrench(Player player, InteractionHand hand, ItemStack wrench, HitResult rayTrace) {
        return true;
    }

    @Override
    public void wrenchUsed(Player player, InteractionHand hand, ItemStack wrench, HitResult rayTrace) {
        // Using an item on a block already swings the arm.
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        if (state.getBlock() instanceof IWrenchable wrenchable) {
            InteractionResult result = wrenchable.onWrench(state, level, pos, player, context.getClickedFace());
            if (result.consumesAction()) {
                playSound(level, pos);
            }
            return result;
        }
        if (player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (RotationUtil.rotateVanilla(level, pos, state)) {
            playSound(level, pos);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    private static void playSound(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 0.8f, 1.2f);
    }
}
