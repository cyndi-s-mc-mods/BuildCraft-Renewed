/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.energy;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import buildcraft.lib.fluid.BCFluidDefinition;
import buildcraft.lib.platform.Platform;
import buildcraft.lib.registry.BCRegistry;
import buildcraft.lib.registry.RegistrationHelper;

public final class BCEnergyFluids {
    public static BCFluidDefinition[] crudeOil;
    public static BCFluidDefinition[] oilResidue;
    public static BCFluidDefinition[] oilHeavy;
    public static BCFluidDefinition[] oilDense;
    public static BCFluidDefinition[] oilDistilled;
    public static BCFluidDefinition[] fuelDense;
    public static BCFluidDefinition[] fuelMixedHeavy;
    public static BCFluidDefinition[] fuelLight;
    public static BCFluidDefinition[] fuelMixedLight;
    public static BCFluidDefinition[] fuelGaseous;

    private BCEnergyFluids() {}

    static void init() {
        // @formatter:off
        //                                    density, viscosity, boil, spread,  tex_light,   tex_dark, flammable
        crudeOil       = define("oil",               900, 2000, 3,  6, 0x50_50_50, 0x05_05_05, true);
        oilResidue     = define("oil_residue",      1200, 4000, 3,  4, 0x10_0F_10, 0x42_10_42, false);
        oilHeavy       = define("oil_heavy",         850, 1800, 3,  6, 0xA0_8F_1F, 0x42_35_20, true);
        oilDense       = define("oil_dense",         950, 1600, 3,  5, 0x87_6E_77, 0x42_24_24, true);
        oilDistilled   = define("oil_distilled",     750, 1400, 2,  8, 0xE4_AF_78, 0xB4_7F_00, true);
        fuelDense      = define("fuel_dense",        600,  800, 2,  7, 0xFF_AF_3F, 0xE0_7F_00, true);
        fuelMixedHeavy = define("fuel_mixed_heavy",  700, 1000, 2,  7, 0xF2_A7_00, 0xC4_87_00, true);
        fuelLight      = define("fuel_light",        400,  600, 1,  8, 0xFF_FF_30, 0xE4_CF_00, true);
        fuelMixedLight = define("fuel_mixed_light",  650,  900, 1,  9, 0xF6_D7_00, 0xC4_B7_00, true);
        fuelGaseous    = define("fuel_gaseous",      300,  500, 0, 10, 0xFA_F6_30, 0xE0_D9_00, true);
        // @formatter:on
    }

    private static BCFluidDefinition[] define(String name, int density, int viscosity, int boil, int spread, int light,
        int dark, boolean flammable) {
        BCFluidDefinition[] defs = new BCFluidDefinition[3];
        for (int heat = 0; heat < 3; heat++) {
            // Higher heat values travel a little further
            int heatSpread = Math.min(16, spread + (spread > 6 ? heat : heat / 2));
            BCFluidDefinition def = new BCFluidDefinition(name, heat, density, viscosity, boil, heatSpread, light, dark, flammable);
            register(def);
            defs[heat] = def;
        }
        return defs;
    }

    private static void register(BCFluidDefinition def) {
        def.source = BCRegistry.register(Registries.FLUID, def.id, key -> Platform.INSTANCE.createFluid(def, true));
        def.flowing = BCRegistry.register(Registries.FLUID, "flowing_" + def.id, key -> Platform.INSTANCE.createFluid(def, false));
        def.block = RegistrationHelper.block(def.id, props -> new LiquidBlock(def.source.get(), props),
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).replaceable().noCollision().strength(100)
                .pushReaction(PushReaction.POPPED).noLootTable().liquid().sound(SoundType.EMPTY));
        def.bucket = RegistrationHelper.item(def.id + "_bucket", props -> new BucketItem(def.source.get(), props),
            () -> new net.minecraft.world.item.Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1));
        BCFluidDefinition.ALL.add(def);
    }
}
