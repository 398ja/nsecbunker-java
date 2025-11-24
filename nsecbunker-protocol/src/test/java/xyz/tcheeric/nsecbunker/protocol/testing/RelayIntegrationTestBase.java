package xyz.tcheeric.nsecbunker.protocol.testing;

import xyz.tcheeric.nsecbunker.connection.testing.MockRelayServer;

import java.util.concurrent.TimeUnit;

/**
 * Base class for integration tests that only need a mock relay server.
 *
 * <p>This is a lighter-weight alternative to {@link IntegrationTestBase}
 * for tests that don't need NIP-46 bunker functionality.
 *
 * <p>Usage:
 * <pre>{@code
 * class MyRelayTest extends RelayIntegrationTestBase {
 *
 *     @Test
 *     void testRelayConnection() {
 *         String url = mockRelay.getUrl();
 *
 *         // Test relay functionality
 *         RelayConnection conn = new RelayConnection(url);
 *         conn.connect();
 *
 *         // Verify
 *         assertTrue(mockRelay.awaitConnection(5, TimeUnit.SECONDS));
 *     }
 * }
 * }</pre>
 */
public abstract class RelayIntegrationTestBase extends IntegrationTestBase {

    /**
     * Override to skip bunker setup.
     *
     * @return null (no bunker needed)
     */
    @Override
    protected MockBunkerServer createMockBunker() {
        return null;
    }

    /**
     * Configures the relay to accept multiple connections.
     *
     * @param count the number of connections to accept
     */
    protected void acceptRelayConnections(int count) {
        if (mockRelay != null) {
            mockRelay.acceptConnections(count);
        }
    }

    /**
     * Waits for a client to connect to the relay.
     *
     * @return true if a client connected
     * @throws InterruptedException if interrupted
     */
    protected boolean awaitRelayConnection() throws InterruptedException {
        return mockRelay != null && mockRelay.awaitConnection(DEFAULT_TIMEOUT_MS, DEFAULT_TIMEOUT_UNIT);
    }

    /**
     * Waits for the relay to receive a message.
     *
     * @return the received message, or null if timeout
     * @throws InterruptedException if interrupted
     */
    protected String awaitRelayMessage() throws InterruptedException {
        return mockRelay != null ? mockRelay.awaitMessage(DEFAULT_TIMEOUT_MS, DEFAULT_TIMEOUT_UNIT) : null;
    }

    /**
     * Waits for the relay to receive a specific number of messages.
     *
     * @param count the expected count
     * @return true if the count was reached
     * @throws InterruptedException if interrupted
     */
    protected boolean awaitRelayMessageCount(int count) throws InterruptedException {
        return mockRelay != null && mockRelay.awaitMessageCount(count, DEFAULT_TIMEOUT_MS, DEFAULT_TIMEOUT_UNIT);
    }

    /**
     * Broadcasts a message to all connected clients.
     *
     * @param message the message to broadcast
     */
    protected void broadcastToRelay(String message) {
        if (mockRelay != null) {
            mockRelay.broadcast(message);
        }
    }

    /**
     * Sends a Nostr EVENT to connected clients.
     *
     * @param subscriptionId the subscription ID
     * @param eventJson      the event JSON
     */
    protected void sendRelayEvent(String subscriptionId, String eventJson) {
        if (mockRelay != null) {
            mockRelay.sendEvent(subscriptionId, eventJson);
        }
    }

    /**
     * Sends an EOSE (End of Stored Events) message.
     *
     * @param subscriptionId the subscription ID
     */
    protected void sendRelayEose(String subscriptionId) {
        if (mockRelay != null) {
            mockRelay.sendEose(subscriptionId);
        }
    }

    /**
     * Sends a NOTICE message to connected clients.
     *
     * @param message the notice message
     */
    protected void sendRelayNotice(String message) {
        if (mockRelay != null) {
            mockRelay.sendNotice(message);
        }
    }
}
