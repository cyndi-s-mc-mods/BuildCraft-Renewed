/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.builders;

import net.minecraft.world.inventory.MenuType;

import buildcraft.builders.container.ContainerArchitectTable;
import buildcraft.builders.container.ContainerBuilder;
import buildcraft.builders.container.ContainerElectronicLibrary;
import buildcraft.builders.container.ContainerFiller;
import buildcraft.lib.registry.RegistryEntry;

import static buildcraft.lib.registry.RegistrationHelper.menu;

public final class BCBuildersMenus {
    public static RegistryEntry<MenuType<?>, MenuType<ContainerFiller>> FILLER;
    public static RegistryEntry<MenuType<?>, MenuType<ContainerArchitectTable>> ARCHITECT;
    public static RegistryEntry<MenuType<?>, MenuType<ContainerBuilder>> BUILDER;
    public static RegistryEntry<MenuType<?>, MenuType<ContainerElectronicLibrary>> LIBRARY;

    private BCBuildersMenus() {}

    static void init() {
        FILLER = menu("filler", ContainerFiller::new);
        ARCHITECT = menu("architect", ContainerArchitectTable::new);
        BUILDER = menu("builder", ContainerBuilder::new);
        LIBRARY = menu("library", ContainerElectronicLibrary::new);
    }
}
