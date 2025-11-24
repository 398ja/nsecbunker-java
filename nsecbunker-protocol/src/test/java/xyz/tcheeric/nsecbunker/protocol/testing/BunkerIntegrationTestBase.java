package xyz.tcheeric.nsecbunker.protocol.testing;

import xyz.tcheeric.nsecbunker.connection.testing.MockRelayServer;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Request;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Response;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Base class for integration tests that need mock bunker functionality.
 *
 * <p>This class provides convenient methods for testing NIP-46 client code
 * against a mock bunker server.
 *
 * <p>Usage:
 * <pre>{@code
 * class MyBunkerTest extends BunkerIntegrationTestBase {
 *
 *     @Test
 *     void testSignEvent() {
 *         // Configure bunker response
 *         configureBunkerSignEvent(event -> "{\"signed\":true}");
 *
 *         // Connect client and test
 *         // ...
 *
 *         // Verify request was received
 *         List<Nip46Request> requests = getBunkerRequests("sign_event");
 *         assertEquals(1, requests.size());
 *     }
 * }
 * }</pre>
 */
public abstract class BunkerIntegrationTestBase extends IntegrationTestBase {

    /**
     * Override to skip standalone relay setup (bunker has its own relay).
     *
     * @return null (bunker provides relay)
     */
    @Override
    protected MockRelayServer createMockRelay() {
        return null;
    }

    /**
     * Gets the bunker's public key.
     *
     * @return the bunker public key
     */
    protected String getBunkerPublicKey() {
        return mockBunker != null ? mockBunker.getBunkerPublicKey() : null;
    }

    /**
     * Sets the client public key for encrypted communication.
     *
     * @param clientPubkey the client's public key
     */
    protected void setClientPublicKey(String clientPubkey) {
        if (mockBunker != null) {
            mockBunker.setClientPublicKey(clientPubkey);
        }
    }

    /**
     * Configures a response delay for the bunker.
     *
     * @param delayMs the delay in milliseconds
     */
    protected void setBunkerResponseDelay(long delayMs) {
        if (mockBunker != null) {
            mockBunker.setResponseDelayMs(delayMs);
        }
    }

    /**
     * Enables or disables auto-respond mode.
     *
     * @param autoRespond true to enable auto-respond
     */
    protected void setBunkerAutoRespond(boolean autoRespond) {
        if (mockBunker != null) {
            mockBunker.setAutoRespond(autoRespond);
        }
    }

    /**
     * Configures a custom ping handler.
     *
     * @param handler function returning the pong result
     */
    protected void configureBunkerPing(Function<Nip46Request, String> handler) {
        if (mockBunker != null) {
            mockBunker.onPing(handler);
        }
    }

    /**
     * Configures a custom connect handler.
     *
     * @param handler function returning true to accept, false to reject
     */
    protected void configureBunkerConnect(Function<Nip46Request, Boolean> handler) {
        if (mockBunker != null) {
            mockBunker.onConnect(handler);
        }
    }

    /**
     * Configures a custom get_public_key handler.
     *
     * @param handler function returning the public key
     */
    protected void configureBunkerGetPublicKey(Function<Nip46Request, String> handler) {
        if (mockBunker != null) {
            mockBunker.onGetPublicKey(handler);
        }
    }

    /**
     * Configures a custom sign_event handler.
     *
     * @param handler function receiving event JSON and returning signed event JSON
     */
    protected void configureBunkerSignEvent(Function<String, String> handler) {
        if (mockBunker != null) {
            mockBunker.onSignEvent(handler);
        }
    }

    /**
     * Configures the bunker to reject a specific method.
     *
     * @param method       the method to reject
     * @param errorCode    the error code
     * @param errorMessage the error message
     */
    protected void rejectBunkerMethod(String method, String errorCode, String errorMessage) {
        if (mockBunker != null) {
            mockBunker.rejectMethod(method, errorCode, errorMessage);
        }
    }

    /**
     * Configures the bunker to require auth for a method.
     *
     * @param method the method requiring auth
     */
    protected void requireBunkerAuth(String method) {
        if (mockBunker != null) {
            mockBunker.requireAuth(method);
        }
    }

    /**
     * Manually sends a response from the bunker.
     *
     * @param response the response to send
     */
    protected void sendBunkerResponse(Nip46Response response) {
        if (mockBunker != null) {
            mockBunker.sendResponse(response);
        }
    }

    /**
     * Gets all requests received by the bunker.
     *
     * @return list of requests
     */
    protected List<Nip46Request> getBunkerRequests() {
        return mockBunker != null ? mockBunker.getReceivedRequests() : List.of();
    }

    /**
     * Gets requests for a specific method.
     *
     * @param method the method name
     * @return list of matching requests
     */
    protected List<Nip46Request> getBunkerRequests(String method) {
        return mockBunker != null ? mockBunker.getRequestsByMethod(method) : List.of();
    }

    /**
     * Gets the count of received requests.
     *
     * @return the request count
     */
    protected int getBunkerRequestCount() {
        return mockBunker != null ? mockBunker.getReceivedRequestCount() : 0;
    }

    /**
     * Clears all received requests.
     */
    protected void clearBunkerRequests() {
        if (mockBunker != null) {
            mockBunker.clearReceivedRequests();
        }
    }

    /**
     * Waits for a request to be received.
     *
     * @return the received request, or null if timeout
     * @throws InterruptedException if interrupted
     */
    protected Nip46Request awaitBunkerRequest() throws InterruptedException {
        return mockBunker != null ? mockBunker.awaitRequest(DEFAULT_TIMEOUT_MS, DEFAULT_TIMEOUT_UNIT) : null;
    }

    /**
     * Waits for a specific number of requests.
     *
     * @param count the expected count
     * @return true if the count was reached
     * @throws InterruptedException if interrupted
     */
    protected boolean awaitBunkerRequestCount(int count) throws InterruptedException {
        return mockBunker != null && mockBunker.awaitRequestCount(count, DEFAULT_TIMEOUT_MS, DEFAULT_TIMEOUT_UNIT);
    }

    /**
     * Configures the bunker to accept additional connections.
     *
     * @param count the number of connections to accept
     */
    protected void acceptBunkerConnections(int count) {
        if (mockBunker != null) {
            mockBunker.acceptConnections(count);
        }
    }
}
