package buildcraft.energy;

public final class BCEnergyConfig {
    public static boolean enableOilGeneration = true;
    /** Multiplies the chance of every kind of oil well. */
    public static double oilWellGenerationRate = 1.0;
    /** Chances (out of 1) of each kind of oil deposit in a chunk. Small lakes only appear in deserts and oceans. */
    public static double smallOilGenProb = 0.02;
    public static double mediumOilGenProb = 0.001;
    public static double largeOilGenProb = 0.0004;
    public static boolean enableOilSpouts = true;
    public static int smallSpoutMinHeight = 6;
    public static int smallSpoutMaxHeight = 12;
    public static int largeSpoutMinHeight = 10;
    public static int largeSpoutMaxHeight = 20;

    private BCEnergyConfig() {}
}
