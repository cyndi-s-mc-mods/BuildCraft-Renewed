package buildcraft.lib.misc;

import java.util.Optional;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.ints.ResolvableInt;
import net.minecraft.world.phys.Vec3;

import buildcraft.lib.platform.Platform;

public final class FuelUtil {
    private FuelUtil() {}

    /** @return How long the item burns in a furnace, in ticks, or 0 if it isn't fuel. */
    public static <T extends BlockEntity & Container> int getBurnTime(ServerLevel level, T tile, ItemStack stack) {
        if (stack.isEmpty()) return 0;
        LootContext context = new LootContext.Builder(new LootParams.Builder(level)
            .withParameter(LootContextParams.BLOCK_STATE, tile.getBlockState())
            .withParameter(LootContextParams.BLOCK_ENTITY, tile)
            .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(tile.getBlockPos()))
            .withParameter(LootContextParams.CONTAINER, tile)
            .create(LootContextParamSets.CONTAINER_PROCESS)).create(Optional.empty());
        int vanilla = ResolvableInt.getFromItem(stack, DataComponents.COOKING_FUEL, CookingFuel::burnTime, context, 0);
        return Platform.INSTANCE.getBurnTime(stack, vanilla);
    }
}
