package buildcraft.lib.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Shortcuts for registering blocks, items and block entity types. */
public final class RegistrationHelper {
    /** Every item, in the order it was registered. Used for the creative tab. */
    private static final List<Supplier<? extends Item>> ITEMS = new ArrayList<>();

    private RegistrationHelper() {}

    public static List<Supplier<? extends Item>> items() {
        return Collections.unmodifiableList(ITEMS);
    }

    public static <B extends Block> RegistryEntry<Block, B> block(String name,
        Function<BlockBehaviour.Properties, B> factory, Supplier<BlockBehaviour.Properties> properties) {
        return BCRegistry.register(Registries.BLOCK, name, key -> factory.apply(properties.get().setId(key)));
    }

    /** Registers a block along with a plain {@link BlockItem} for it. */
    public static <B extends Block> RegistryEntry<Block, B> blockWithItem(String name,
        Function<BlockBehaviour.Properties, B> factory, Supplier<BlockBehaviour.Properties> properties) {
        RegistryEntry<Block, B> block = block(name, factory, properties);
        item(name, props -> new BlockItem(block.get(), props.useBlockDescriptionPrefix()));
        return block;
    }

    public static <I extends Item> RegistryEntry<Item, I> item(String name, Function<Item.Properties, I> factory) {
        return item(name, factory, Item.Properties::new);
    }

    public static <I extends Item> RegistryEntry<Item, I> item(String name, Function<Item.Properties, I> factory,
        Supplier<Item.Properties> properties) {
        RegistryEntry<Item, I> entry = BCRegistry.register(Registries.ITEM, name,
            key -> factory.apply(properties.get().setId(key)));
        ITEMS.add(entry);
        return entry;
    }

    /** Registers an item that won't appear in the creative tab. */
    public static <I extends Item> RegistryEntry<Item, I> hiddenItem(String name, Function<Item.Properties, I> factory) {
        return BCRegistry.register(Registries.ITEM, name, key -> factory.apply(new Item.Properties().setId(key)));
    }

    @SafeVarargs
    public static <T extends BlockEntity> RegistryEntry<BlockEntityType<?>, BlockEntityType<T>> tile(String name,
        BlockEntityType.BlockEntitySupplier<T> factory, Supplier<? extends Block>... blocks) {
        return BCRegistry.register(Registries.BLOCK_ENTITY_TYPE, name, key -> {
            Block[] array = new Block[blocks.length];
            for (int i = 0; i < blocks.length; i++) {
                array[i] = blocks[i].get();
            }
            return new BlockEntityType<>(factory, Set.of(array));
        });
    }

    public static <M extends AbstractContainerMenu> RegistryEntry<MenuType<?>, MenuType<M>> menu(String name,
        MenuType.MenuSupplier<M> supplier) {
        return BCRegistry.register(Registries.MENU, name, key -> new MenuType<>(supplier, FeatureFlags.VANILLA_SET));
    }

    public static ResourceKey<Item> itemKey(String name) {
        return ResourceKey.create(Registries.ITEM, buildcraft.BuildCraft.id(name));
    }
}
