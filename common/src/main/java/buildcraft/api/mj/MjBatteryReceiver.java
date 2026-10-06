package buildcraft.api.mj;

/** A receiver that puts power into a battery. */
public class MjBatteryReceiver implements IMjReadable, IMjReceiver {
    private final MjBattery battery;

    public MjBatteryReceiver(MjBattery battery) {
        this.battery = battery;
    }

    @Override
    public boolean canConnect(IMjConnector other) {
        return true;
    }

    @Override
    public long getStored() {
        return battery.getStored();
    }

    @Override
    public long getCapacity() {
        return battery.getCapacity();
    }

    @Override
    public long getPowerRequested() {
        return Math.max(0, battery.getCapacity() - battery.getStored());
    }

    @Override
    public long receivePower(long microJoules, boolean simulate) {
        return battery.addPowerChecking(microJoules, simulate);
    }

    /** Also accepts power from redstone engines. */
    public static class Redstone extends MjBatteryReceiver implements IMjRedstoneReceiver {
        public Redstone(MjBattery battery) {
            super(battery);
        }
    }
}
