package buildcraft.transport;

public final class BCTransport {
    private BCTransport() {}

    public static void init() {
        BCTransportPipes.init();
        BCTransportBlocks.init();
        BCTransportMenus.init();
        BCTransportItems.init();
        BCTransportPlugs.init();
        buildcraft.transport.statements.BCTransportStatements.init();
    }

    public static void setup() {}
}
