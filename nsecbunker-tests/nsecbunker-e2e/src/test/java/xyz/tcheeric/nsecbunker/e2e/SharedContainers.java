package xyz.tcheeric.nsecbunker.e2e;

import nostr.id.Identity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;
import xyz.tcheeric.nsecbunker.e2e.containers.NostrRelayContainer;
import xyz.tcheeric.nsecbunker.e2e.containers.NsecBunkerdContainer;

/**
 * Singleton container manager for E2E tests.
 *
 * <p>This class ensures that the Nostr relay and nsecBunker containers are started
 * only once across all test classes and stopped when the JVM shuts down.
 *
 * <p>Using the singleton pattern avoids the overhead of starting/stopping containers
 * for each test class, significantly reducing E2E test execution time.
 */
public final class SharedContainers {

    private static final Logger log = LoggerFactory.getLogger(SharedContainers.class);

    // Network aliases
    private static final String RELAY_NETWORK_ALIAS = "relay";
    private static final int RELAY_INTERNAL_PORT = 7777;

    // Singleton instance
    private static SharedContainers instance;
    private static final Object LOCK = new Object();

    // Container network
    private final Network network;

    // Containers
    private final NostrRelayContainer relayContainer;
    private final NsecBunkerdContainer bunkerContainer;

    // Admin identity (for authenticating admin operations)
    private final Identity adminIdentity;
    private final String adminNsec;
    private final String adminNpub;

    // Bunker identity (the bunker's own key for signing/encryption)
    private final Identity bunkerIdentity;
    private final String bunkerNsec;
    private final String bunkerNpub;

    // Relay URL accessible from the host
    private final String relayUrl;

    private SharedContainers() {
        log.info("Initializing shared E2E test containers (singleton)...");

        // Generate admin identity (for authenticating admin operations)
        adminIdentity = Identity.generateRandomIdentity();
        adminNsec = adminIdentity.getPrivateKey().toBech32String();
        adminNpub = adminIdentity.getPublicKey().toBech32String();
        log.info("Generated admin identity: npub={}", adminNpub);

        // Generate bunker identity (the bunker's own signing key)
        bunkerIdentity = Identity.generateRandomIdentity();
        bunkerNsec = bunkerIdentity.getPrivateKey().toBech32String();
        bunkerNpub = bunkerIdentity.getPublicKey().toBech32String();
        log.info("Generated bunker identity: npub={}", bunkerNpub);

        // Create shared network
        network = Network.newNetwork();

        // Start relay container
        relayContainer = new NostrRelayContainer()
                .withNetwork(network)
                .withNetworkAliases(RELAY_NETWORK_ALIAS);
        relayContainer.start();
        relayUrl = relayContainer.getWsUrl();
        log.info("Relay started at: {}", relayUrl);

        // Start bunker container connected to relay with known identity
        // jaonoctus/nsecbunkerd uses JSON config with adminNpub and bunker private key in hex
        String internalRelayUrl = String.format("ws://%s:%d", RELAY_NETWORK_ALIAS, RELAY_INTERNAL_PORT);
        String bunkerPrivateKeyHex = bunkerIdentity.getPrivateKey().toString();
        bunkerContainer = new NsecBunkerdContainer()
                .withNetwork(network)
                .withRelay(internalRelayUrl)
                .withAdminNpub(adminNpub)
                .withAdminNsec(adminNsec)
                .withBunkerPrivateKeyHex(bunkerPrivateKeyHex)
                .withBunkerNsec(bunkerNsec);
        bunkerContainer.start();
        log.info("Bunker started, connected to relay: {}", internalRelayUrl);

        // Wait a bit for the bunker to fully initialize
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        log.info("Shared E2E test containers started successfully");

        // Register shutdown hook to stop containers when JVM exits
        Runtime.getRuntime().addShutdownHook(new Thread(this::stopContainers, "e2e-container-shutdown"));
    }

    /**
     * Gets the singleton instance, starting containers if necessary.
     *
     * @return the shared containers instance
     */
    public static SharedContainers getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new SharedContainers();
                }
            }
        }
        return instance;
    }

    /**
     * Stops all containers and releases resources.
     */
    private void stopContainers() {
        log.info("Stopping shared E2E test containers...");

        if (bunkerContainer != null && bunkerContainer.isRunning()) {
            bunkerContainer.stop();
        }
        if (relayContainer != null && relayContainer.isRunning()) {
            relayContainer.stop();
        }
        if (network != null) {
            network.close();
        }

        log.info("Shared E2E test containers stopped");
    }

    // Getters

    public Network getNetwork() {
        return network;
    }

    public NostrRelayContainer getRelayContainer() {
        return relayContainer;
    }

    public NsecBunkerdContainer getBunkerContainer() {
        return bunkerContainer;
    }

    public Identity getAdminIdentity() {
        return adminIdentity;
    }

    public String getAdminNsec() {
        return adminNsec;
    }

    public String getAdminNpub() {
        return adminNpub;
    }

    public Identity getBunkerIdentity() {
        return bunkerIdentity;
    }

    public String getBunkerNsec() {
        return bunkerNsec;
    }

    public String getBunkerNpub() {
        return bunkerNpub;
    }

    public String getRelayUrl() {
        return relayUrl;
    }
}
