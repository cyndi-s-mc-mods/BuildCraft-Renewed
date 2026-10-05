package buildcraft.lib.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Blocks that do something special when hit with a wrench (usually rotating). */
public interface IWrenchable {
    InteractionResult onWrench(BlockState state, Level level, BlockPos pos, Player player, Direction side);
}
