package buildcraft.builders;

public final class BCBuildersConfig {
    /** The minimum height of quarry frames. */
    public static int quarryFrameMinHeight = 4;
    /** The most tasks (breaking a block, placing a frame, moving the drill) a quarry can do each tick. */
    public static int quarryMaxTasksPerTick = 4;
    /** Each extra task in a tick costs (divisor + n) / divisor times as much power. 0 disables this. */
    public static int quarryTaskPowerDivisor = 2;
    /** The most blocks per second a quarry's drill can move. 0 means no limit. */
    public static double quarryMaxFrameMoveSpeed = 0;
    /** The most blocks per second a quarry can mine. 0 means no limit. */
    public static double quarryMaxBlockMineRate = 0;

    private BCBuildersConfig() {}
}
