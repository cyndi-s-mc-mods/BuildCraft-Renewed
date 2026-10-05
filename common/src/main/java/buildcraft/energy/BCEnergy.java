package buildcraft.energy;

public final class BCEnergy {
    private BCEnergy() {}

    public static void init() {
        BCEnergyBlocks.init();
        BCEnergyMenus.init();
    }
}
