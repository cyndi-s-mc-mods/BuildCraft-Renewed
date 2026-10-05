/*
 * Copyright (c) 2017 SpaceToad and the BuildCraft team
 * This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy of the MPL was not
 * distributed with this file, You can obtain one at https://mozilla.org/MPL/2.0/
 */

package buildcraft.transport;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import buildcraft.api.mj.MjAPI;
import buildcraft.api.transport.pipe.IPipe;
import buildcraft.api.transport.pipe.PipeApi;
import buildcraft.api.transport.pipe.PipeBehaviour;
import buildcraft.api.transport.pipe.PipeDefinition;
import buildcraft.api.transport.pipe.PipeFlowType;
import buildcraft.lib.registry.RegistrationHelper;
import buildcraft.lib.registry.RegistryEntry;
import buildcraft.transport.block.BlockPipe;
import buildcraft.transport.block.BlockPipe.Visual;
import buildcraft.transport.pipe.behaviour.*;
import buildcraft.transport.pipe.flow.PipeFlowFluids;
import buildcraft.transport.pipe.flow.PipeFlowItems;
import buildcraft.transport.pipe.flow.PipeFlowPower;
import buildcraft.transport.pipe.flow.PipeFlowStructure;

public final class BCTransportPipes {
    public static final PipeFlowType FLOW_STRUCTURE = new PipeFlowType("structure", PipeFlowStructure::new);
    public static final PipeFlowType FLOW_ITEMS = new PipeFlowType("items", PipeFlowItems::new);
    public static final PipeFlowType FLOW_FLUIDS = new PipeFlowType("fluids", PipeFlowFluids::new);
    public static final PipeFlowType FLOW_POWER = new PipeFlowType("power", PipeFlowPower::new);

    /** Every pipe definition and its block, in the order they appear in the creative tab. */
    public static final Map<PipeDefinition, RegistryEntry<Block, BlockPipe>> BLOCKS = new LinkedHashMap<>();
    public static final Map<String, PipeDefinition> DEFINITIONS = new LinkedHashMap<>();

    private BCTransportPipes() {}

    static void init() {
        define("structure", FLOW_STRUCTURE, PipeBehaviourStructure::new, Visual.PLAIN);

        define("wood_item", FLOW_ITEMS, PipeBehaviourWood::new, Visual.DIRECTIONAL);
        define("wood_fluid", FLOW_FLUIDS, PipeBehaviourWood::new, Visual.DIRECTIONAL);
        define("wood_power", FLOW_POWER, PipeBehaviourWoodPower::new, Visual.PLAIN);

        define("stone_item", FLOW_ITEMS, PipeBehaviourStone::new, Visual.PLAIN);
        define("stone_fluid", FLOW_FLUIDS, PipeBehaviourStone::new, Visual.PLAIN);
        define("stone_power", FLOW_POWER, PipeBehaviourStone::new, Visual.PLAIN);

        define("cobblestone_item", FLOW_ITEMS, PipeBehaviourCobble::new, Visual.PLAIN);
        define("cobblestone_fluid", FLOW_FLUIDS, PipeBehaviourCobble::new, Visual.PLAIN);
        define("cobblestone_power", FLOW_POWER, PipeBehaviourCobble::new, Visual.PLAIN);

        define("quartz_item", FLOW_ITEMS, PipeBehaviourQuartz::new, Visual.PLAIN);
        define("quartz_fluid", FLOW_FLUIDS, PipeBehaviourQuartz::new, Visual.PLAIN);
        define("quartz_power", FLOW_POWER, PipeBehaviourQuartz::new, Visual.PLAIN);

        define("gold_item", FLOW_ITEMS, PipeBehaviourGold::new, Visual.PLAIN);
        define("gold_fluid", FLOW_FLUIDS, PipeBehaviourGold::new, Visual.PLAIN);
        define("gold_power", FLOW_POWER, PipeBehaviourGold::new, Visual.PLAIN);

        define("sandstone_item", FLOW_ITEMS, PipeBehaviourSandstone::new, Visual.PLAIN);
        define("sandstone_fluid", FLOW_FLUIDS, PipeBehaviourSandstone::new, Visual.PLAIN);
        define("sandstone_power", FLOW_POWER, PipeBehaviourSandstone::new, Visual.PLAIN);

        define("iron_item", FLOW_ITEMS, PipeBehaviourIron::new, Visual.DIRECTIONAL);
        define("iron_fluid", FLOW_FLUIDS, PipeBehaviourIron::new, Visual.DIRECTIONAL);
        define("iron_power", FLOW_POWER, PipeBehaviourLimiter::new, Visual.LIMITER);

        define("diamond_item", FLOW_ITEMS, PipeBehaviourDiamondItem::new, Visual.PLAIN);
        define("diamond_fluid", FLOW_FLUIDS, PipeBehaviourDiamondFluid::new, Visual.PLAIN);
        define("diamond_power", FLOW_POWER, PipeBehaviourLimiter::new, Visual.LIMITER);

        define("diamond_wood_item", FLOW_ITEMS, PipeBehaviourWoodDiamond::new, Visual.DIRECTIONAL);
        define("diamond_wood_fluid", FLOW_FLUIDS, PipeBehaviourWoodDiamond::new, Visual.DIRECTIONAL);
        define("diamond_wood_power", FLOW_POWER, PipeBehaviourWoodPower::new, Visual.PLAIN);

        define("clay_item", FLOW_ITEMS, PipeBehaviourClay::new, Visual.PLAIN);
        define("clay_fluid", FLOW_FLUIDS, PipeBehaviourClay::new, Visual.PLAIN);

        define("void_item", FLOW_ITEMS, PipeBehaviourVoid::new, Visual.PLAIN);
        define("void_fluid", FLOW_FLUIDS, PipeBehaviourVoid::new, Visual.PLAIN);

        define("obsidian_item", FLOW_ITEMS, PipeBehaviourObsidian::new, Visual.PLAIN);
        define("lapis_item", FLOW_ITEMS, PipeBehaviourLapis::new, Visual.COLOURED);
        define("daizuli_item", FLOW_ITEMS, PipeBehaviourDaizuli::new, Visual.DIRECTIONAL_COLOURED);
        define("emzuli_item", FLOW_ITEMS, PipeBehaviourEmzuli::new, Visual.DIRECTIONAL);
        define("stripes_item", FLOW_ITEMS, PipeBehaviourStripes::new, Visual.PLAIN);

        setTransferRates();
    }

    private static void define(String id, PipeFlowType flow, Function<IPipe, PipeBehaviour> logic, Visual visual) {
        PipeDefinition def = new PipeDefinition(id, flow, logic, false);
        DEFINITIONS.put(id, def);
        RegistryEntry<Block, BlockPipe> block = RegistrationHelper.blockWithItem("pipe_" + id,
            props -> BlockPipe.create(props, () -> BCTransportBlocks.PIPE_HOLDER.get(), def, visual),
            () -> BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(0.25f).sound(SoundType.GLASS));
        BLOCKS.put(def, block);
    }

    private static PipeDefinition def(String id) {
        return DEFINITIONS.get(id);
    }

    private static void setTransferRates() {
        int baseFlowRate = BCTransportConfig.baseFlowRate;
        fluidTransfer("cobblestone_fluid", baseFlowRate, 10);
        fluidTransfer("wood_fluid", baseFlowRate, 10);
        fluidTransfer("stone_fluid", baseFlowRate * 2, 10);
        fluidTransfer("sandstone_fluid", baseFlowRate * 2, 10);
        fluidTransfer("clay_fluid", baseFlowRate * 4, 10);
        fluidTransfer("iron_fluid", baseFlowRate * 4, 10);
        fluidTransfer("quartz_fluid", baseFlowRate * 4, 10);
        fluidTransfer("diamond_fluid", baseFlowRate * 8, 10);
        fluidTransfer("diamond_wood_fluid", baseFlowRate * 8, 10);
        fluidTransfer("gold_fluid", baseFlowRate * 8, 2);
        fluidTransfer("void_fluid", baseFlowRate * 8, 10);

        int basePowerRate = BCTransportConfig.basePowerRate;
        powerTransfer("cobblestone_power", basePowerRate, 16, false);
        powerTransfer("stone_power", basePowerRate * 2, 32, false);
        powerTransfer("wood_power", basePowerRate * 4, 128, true);
        powerTransfer("sandstone_power", basePowerRate * 4, 32, false);
        powerTransfer("quartz_power", basePowerRate * 8, 32, false);
        powerTransfer("iron_power", basePowerRate * 8, 32, false);
        powerTransfer("gold_power", basePowerRate * 32, 32, false);
        powerTransfer("diamond_power", basePowerRate * 64, 32, false);
        powerTransfer("diamond_wood_power", basePowerRate * 64, 32, true);
    }

    private static void fluidTransfer(String id, int rate, int delay) {
        PipeApi.fluidTransferData.put(def(id), new PipeApi.FluidTransferInfo(rate, delay));
    }

    private static void powerTransfer(String id, int transferMultiplier, int resistanceDivisor, boolean recv) {
        long transfer = MjAPI.MJ * transferMultiplier;
        long resistance = MjAPI.MJ / resistanceDivisor;
        PipeApi.powerTransferData.put(def(id), PipeApi.PowerTransferInfo.createFromResistance(transfer, resistance, recv));
    }
}
