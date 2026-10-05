package buildcraft.energy;

public final class BCEnergy {
    private BCEnergy() {}

    public static void init() {
        BCEnergyFluids.init();
        buildcraft.api.enums.EnumSpring.OIL.liquidBlock = () -> BCEnergyFluids.crudeOil[0].block.get().defaultBlockState();
        BCEnergyBlocks.init();
        BCEnergyMenus.init();
        BCEnergyWorldGen.init();
    }

    public static void setup() {
        BCEnergyRecipes.init();
    }
}
