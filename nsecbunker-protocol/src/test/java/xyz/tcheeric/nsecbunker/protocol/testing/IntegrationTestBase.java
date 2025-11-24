package xyz.tcheeric.nsecbunker.protocol.testing;

import okhttp3.OkHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import xyz.tcheeric.nsecbunker.connection.testing.MockRelayServer;

import java.util.concurrent.TimeUnit;

/**
 * Base class for integration tests that need mock relay and bunker servers.
 *
 * <p>This class provides common setup and teardown for integration tests,
 * including:
 * <ul>
 *   <li>Mock relay server</li>
 *   <li>Mock bunker server</li>
 *   <li>OkHttp client for direct WebSocket testing</li>
 *   <li>Common test utilities</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>{@code
 * class MyIntegrationTest extends IntegrationTestBase {
 *
 *     @Test
 *     void testSomething() {
 *         // Use inherited fields
 *         String relayUrl = mockRelay.getUrl();
 *         String bunkerUrl = mockBunker.getRelayUrl();
 *
 *         // Configure bunker
 *         mockBunker.onSignEvent(event -> "signed");
 *
 *         // Run test
 *     }
 * }
 * }</pre>
 */
public abstract class IntegrationTestBase {

    /**
     * Mock relay server for testing relay connections.
     */
    protected MockRelayServer mockRelay;

    /**
     * Mock bunker server for testing NIP-46 operations.
     */
    protected MockBunkerServer mockBunker;

    /**
     * OkHttp client for direct WebSocket testing.
     */
    protected OkHttpClient httpClient;

    /**
     * Default timeout for async operations in milliseconds.
     */
    protected static final long DEFAULT_TIMEOUT_MS = 5000;

    /**
     * Default timeout for async operations.
     */
    protected static final TimeUnit DEFAULT_TIMEOUT_UNIT = TimeUnit.MILLISECONDS;

    /**
     * Sets up the test environment.
     * Override this method and call super.setUp() to add custom setup.
     *
     * @throws Exception if setup fails
     */
    @BeforeEach
    protected void setUp() throws Exception {
        httpClient = createHttpClient();
        mockRelay = createMockRelay();
        mockBunker = createMockBunker();

        if (mockRelay != null) {
            mockRelay.start();
            mockRelay.enqueueWebSocketUpgrade();
        }

        if (mockBunker != null) {
            mockBunker.start();
        }
    }

    /**
     * Tears down the test environment.
     * Override this method and call super.tearDown() to add custom teardown.
     *
     * @throws Exception if teardown fails
     */
    @AfterEach
    protected void tearDown() throws Exception {
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
        }

        if (mockRelay != null) {
            mockRelay.close();
        }

        if (mockBunker != null) {
            mockBunker.close();
        }
    }

    /**
     * Creates the OkHttp client for testing.
     * Override to customize the client configuration.
     *
     * @return the HTTP client
     */
    protected OkHttpClient createHttpClient() {
        return new OkHttpClient.Builder()
                .readTimeout(DEFAULT_TIMEOUT_MS, DEFAULT_TIMEOUT_UNIT)
                .writeTimeout(DEFAULT_TIMEOUT_MS, DEFAULT_TIMEOUT_UNIT)
                .connectTimeout(DEFAULT_TIMEOUT_MS, DEFAULT_TIMEOUT_UNIT)
                .build();
    }

    /**
     * Creates the mock relay server.
     * Override to customize or return null to skip relay setup.
     *
     * @return the mock relay server, or null
     */
    protected MockRelayServer createMockRelay() {
        return new MockRelayServer();
    }

    /**
     * Creates the mock bunker server.
     * Override to customize or return null to skip bunker setup.
     *
     * @return the mock bunker server, or null
     */
    protected MockBunkerServer createMockBunker() {
        return new MockBunkerServer();
    }

    /**
     * Gets the relay URL for testing.
     *
     * @return the relay URL
     */
    protected String getRelayUrl() {
        return mockRelay != null ? mockRelay.getUrl() : null;
    }

    /**
     * Gets the bunker relay URL for testing.
     *
     * @return the bunker's relay URL
     */
    protected String getBunkerRelayUrl() {
        return mockBunker != null ? mockBunker.getRelayUrl() : null;
    }

    /**
     * Gets the bunker connection string.
     *
     * @return the bunker connection string
     */
    protected String getBunkerConnectionString() {
        return mockBunker != null ? mockBunker.getConnectionString() : null;
    }

    /**
     * Waits for a condition with default timeout.
     *
     * @param condition the condition to wait for
     * @throws InterruptedException if interrupted
     */
    protected void waitFor(BooleanSupplier condition) throws InterruptedException {
        waitFor(condition, DEFAULT_TIMEOUT_MS);
    }

    /**
     * Waits for a condition with custom timeout.
     *
     * @param condition the condition to wait for
     * @param timeoutMs the timeout in milliseconds
     * @throws InterruptedException if interrupted
     */
    protected void waitFor(BooleanSupplier condition, long timeoutMs) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() >= deadline) {
                throw new AssertionError("Timeout waiting for condition");
            }
            Thread.sleep(10);
        }
    }

    /**
     * Functional interface for boolean conditions.
     */
    @FunctionalInterface
    protected interface BooleanSupplier {
        boolean getAsBoolean();
    }
}
