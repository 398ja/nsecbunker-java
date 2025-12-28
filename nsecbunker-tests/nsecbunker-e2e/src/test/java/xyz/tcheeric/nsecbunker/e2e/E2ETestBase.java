package xyz.tcheeric.nsecbunker.e2e;

import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import nostr.id.Identity;

import java.time.Duration;

/**
 * Base class for end-to-end tests that need both a Nostr relay and nsecBunker.
 *
 * <p>This class provides access to shared Testcontainers for a Nostr relay
 * and nsecbunkerd. The containers are started only once (singleton pattern)
 * and shared across all test classes to minimize startup overhead.
 *
 * <p>Extend this class to write E2E tests that interact with a real nsecBunker
 * instance through actual relay connections.
 */
public abstract class E2ETestBase {

    protected static final Logger log = LoggerFactory.getLogger(E2ETestBase.class);

    // Shared containers instance (singleton)
    protected static SharedContainers containers;

    // Default timeouts
    protected static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(60);
    protected static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(120);

    @BeforeAll
    static void initializeContainers() {
        // Get the singleton instance - containers are started only once
        containers = SharedContainers.getInstance();
        log.info("Using shared containers: relay={}", containers.getRelayUrl());
    }

    // Note: No @AfterAll to stop containers - they are stopped via JVM shutdown hook
    // This allows containers to be reused across all test classes

    /**
     * Gets the relay URL accessible from the host.
     *
     * @return the relay WebSocket URL
     */
    protected String getRelayUrl() {
        return containers.getRelayUrl();
    }

    /**
     * Gets the admin nsec for bunker administration.
     *
     * @return the admin private key in nsec format
     */
    protected String getAdminNsec() {
        return containers.getAdminNsec();
    }

    /**
     * Gets the admin npub.
     *
     * @return the admin public key in npub format
     */
    protected String getAdminNpub() {
        return containers.getAdminNpub();
    }

    /**
     * Gets the admin identity.
     *
     * @return the admin Identity object
     */
    protected Identity getAdminIdentity() {
        return containers.getAdminIdentity();
    }

    /**
     * Gets the bunker npub.
     *
     * @return the bunker public key in npub format
     */
    protected String getBunkerNpub() {
        return containers.getBunkerNpub();
    }

    /**
     * Gets the bunker identity.
     *
     * @return the bunker Identity object
     */
    protected Identity getBunkerIdentity() {
        return containers.getBunkerIdentity();
    }

    /**
     * Generates a new random identity for testing.
     *
     * @return a new Identity
     */
    protected Identity generateTestIdentity() {
        return Identity.generateRandomIdentity();
    }

    /**
     * Waits for a condition with default timeout.
     *
     * @param condition the condition to wait for
     * @param message the error message if timeout
     * @throws InterruptedException if interrupted
     */
    protected void waitFor(java.util.function.BooleanSupplier condition, String message) throws InterruptedException {
        waitFor(condition, message, DEFAULT_REQUEST_TIMEOUT);
    }

    /**
     * Waits for a condition with specified timeout.
     *
     * @param condition the condition to wait for
     * @param message the error message if timeout
     * @param timeout the timeout duration
     * @throws InterruptedException if interrupted
     */
    protected void waitFor(java.util.function.BooleanSupplier condition, String message, Duration timeout) throws InterruptedException {
        long endTime = System.currentTimeMillis() + timeout.toMillis();
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > endTime) {
                throw new AssertionError("Timeout waiting for: " + message);
            }
            Thread.sleep(100);
        }
    }
}
