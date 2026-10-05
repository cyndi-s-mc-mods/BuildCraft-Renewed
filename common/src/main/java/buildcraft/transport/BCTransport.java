package buildcraft.transport;

public final class BCTransport {
    private BCTransport() {}

    public static void init() {
        BCTransportPipes.init();
        BCTransportBlocks.init();
        BCTransportMenus.init();
        BCTransportItems.init();
        BCTransportPlugs.init();
    }

    public static void setup() {}
}
