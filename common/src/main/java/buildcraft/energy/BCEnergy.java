package buildcraft.energy;

public final class BCEnergy {
    private BCEnergy() {}

    public static void init() {
        BCEnergyFluids.init();
        BCEnergyBlocks.init();
        BCEnergyMenus.init();
    }
}
