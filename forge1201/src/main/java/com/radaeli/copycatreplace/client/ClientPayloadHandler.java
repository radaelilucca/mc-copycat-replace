package com.radaeli.copycatreplace.client;

/** Loaded only by a physical-client packet handler. */
public final class ClientPayloadHandler {
    private ClientPayloadHandler() {}
    public static void acceptLimit(int maximum) { ConnectedBulkIndicator.setServerMaximum(maximum); }
}
