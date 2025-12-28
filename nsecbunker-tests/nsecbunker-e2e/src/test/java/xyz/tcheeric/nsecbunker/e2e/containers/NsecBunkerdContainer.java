package xyz.tcheeric.nsecbunker.e2e.containers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Testcontainer for nsecBunker daemon (nsecbunkerd).
 *
 * <p>Uses the jaonoctus/nsecbunkerd image from Docker Hub.
 *
 * <p>This image requires a JSON config file and uses a different CLI than
 * the original pablof7z/nsecbunkerd image. It communicates via Nostr relays
 * using the NIP-46 protocol.
 *
 * <p>Note: pablof7z/nsecbunkerd is ARM64-only and cannot be used on AMD64 systems.
 */
public class NsecBunkerdContainer extends GenericContainer<NsecBunkerdContainer> {

    private static final Logger log = LoggerFactory.getLogger(NsecBunkerdContainer.class);

    // Use local patched image that works with nostr-tools v2
    private static final DockerImageName DEFAULT_IMAGE = DockerImageName.parse("nsecbunkerd-local:latest");

    private static final String CONFIG_PATH = "/app/config/nsecbunker.json";

    // Pattern to extract bunker pubkey from logs (npub format)
    private static final Pattern NPUB_PATTERN = Pattern.compile("npub1[a-z0-9]{58}");

    private final List<String> relayUrls = new ArrayList<>();
    private String adminNpub;      // Admin public key (npub format) for config
    private String adminNsec;      // Admin private key (nsec format) for test use
    private String bunkerPrivateKeyHex;  // Bunker's private key in hex format for config
    private String bunkerNsec;     // Bunker's private key (nsec format) for reference

    // Captured bunker pubkey from logs
    private final AtomicReference<String> capturedBunkerPubkey = new AtomicReference<>();

    public NsecBunkerdContainer() {
        this(DEFAULT_IMAGE);
    }

    public NsecBunkerdContainer(DockerImageName imageName) {
        super(imageName);

        // Log consumer that captures bunker pubkey and logs to slf4j
        Consumer<OutputFrame> logConsumer = frame -> {
            String message = frame.getUtf8String();
            log.info("[bunker] {}", message.trim());

            // Try to capture bunker pubkey from logs
            if (capturedBunkerPubkey.get() == null) {
                Matcher npubMatcher = NPUB_PATTERN.matcher(message);
                if (npubMatcher.find()) {
                    capturedBunkerPubkey.set(npubMatcher.group());
                    log.info("Captured bunker npub: {}", capturedBunkerPubkey.get());
                }
            }
        };
        withLogConsumer(logConsumer);

        // Wait for the daemon to start and connect to relays
        // The jaonoctus image logs "Connected to" when relay connects and "ready to serve" when ready
        waitingFor(Wait.forLogMessage(".*ready to serve.*", 1)
                .withStartupTimeout(Duration.ofSeconds(120)));

        // Override entrypoint since the image has a default one that interferes
        // Also add DNS servers for internet access (needed for Prisma binary download)
        withCreateContainerCmdModifier(cmd -> {
            cmd.withEntrypoint("sh");
            cmd.withDns("8.8.8.8", "8.8.4.4");
        });

        // Set DATABASE_URL environment variable for Prisma
        withEnv("DATABASE_URL", "file:/app/config/nsecbunker.db");
    }

    /**
     * Adds a relay URL for the bunker to connect to.
     *
     * @param relayUrl the relay WebSocket URL
     * @return this container for chaining
     */
    public NsecBunkerdContainer withRelay(String relayUrl) {
        this.relayUrls.add(relayUrl);
        return this;
    }

    /**
     * Sets the admin npub for the bunker config.
     *
     * @param adminNpub the admin public key in npub format
     * @return this container for chaining
     */
    public NsecBunkerdContainer withAdminNpub(String adminNpub) {
        this.adminNpub = adminNpub;
        return this;
    }

    /**
     * Sets the admin nsec (stored for test use, not sent to bunker).
     *
     * @param adminNsec the admin private key in nsec format
     * @return this container for chaining
     */
    public NsecBunkerdContainer withAdminNsec(String adminNsec) {
        this.adminNsec = adminNsec;
        return this;
    }

    /**
     * Sets the bunker's own private key in hex format (for the config file).
     *
     * @param bunkerPrivateKeyHex the bunker's private key in hex format
     * @return this container for chaining
     */
    public NsecBunkerdContainer withBunkerPrivateKeyHex(String bunkerPrivateKeyHex) {
        this.bunkerPrivateKeyHex = bunkerPrivateKeyHex;
        return this;
    }

    /**
     * Sets the bunker's nsec (stored for reference).
     *
     * @param bunkerNsec the bunker's private key in nsec format
     * @return this container for chaining
     */
    public NsecBunkerdContainer withBunkerNsec(String bunkerNsec) {
        this.bunkerNsec = bunkerNsec;
        return this;
    }

    @Override
    protected void configure() {
        super.configure();

        // Generate the config JSON (single-line for shell embedding)
        String configJson = generateConfigJsonOneLine();
        log.info("Generated nsecbunker config: {}", configJson);

        // Build the start command with CLI args
        // Pass admin npubs via CLI args (--admin-npubs) since that's the primary source checked
        StringBuilder startCommand = new StringBuilder("node /app/dist/index.js start --verbose --config ");
        startCommand.append(CONFIG_PATH);

        // Add admin npubs via CLI args - this is the most reliable way to pass them
        if (adminNpub != null && !adminNpub.isEmpty()) {
            startCommand.append(" --admin-npubs ").append(adminNpub);
        }

        // Use shell to create directory, write config, initialize database, and start daemon
        // Use printf with %s to avoid shell interpretation issues
        // Entrypoint is already "sh", so just pass -c and the command
        // Run prisma db push to create database schema before starting
        String shellCommand = String.format(
                "mkdir -p /app/config && printf '%%s' '%s' > %s && cat %s && npx prisma db push --skip-generate && %s",
                configJson.replace("'", "'\"'\"'"), // escape single quotes: ' becomes '"'"'
                CONFIG_PATH,
                CONFIG_PATH,
                startCommand.toString()
        );
        withCommand("-c", shellCommand);
    }

    /**
     * Generates the JSON configuration for nsecbunkerd as a single line for shell embedding.
     */
    private String generateConfigJsonOneLine() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");

        // Nostr config with relays
        sb.append("\"nostr\":{");
        sb.append("\"relays\":[");
        for (int i = 0; i < relayUrls.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(relayUrls.get(i)).append("\"");
        }
        sb.append("]");
        sb.append("},");

        // Admin config
        sb.append("\"admin\":{");
        sb.append("\"npubs\":[");
        if (adminNpub != null && !adminNpub.isEmpty()) {
            sb.append("\"").append(adminNpub).append("\"");
        }
        sb.append("],");
        sb.append("\"adminRelays\":[");
        for (int i = 0; i < relayUrls.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(relayUrls.get(i)).append("\"");
        }
        sb.append("],");
        if (bunkerPrivateKeyHex != null && !bunkerPrivateKeyHex.isEmpty()) {
            sb.append("\"key\":\"").append(bunkerPrivateKeyHex).append("\",");
        }
        sb.append("\"notifyAdminsOnBoot\":false");
        sb.append("},");

        // Database (use SQLite in /app/config)
        sb.append("\"database\":\"file:/app/config/nsecbunker.db\",");
        sb.append("\"logs\":\"/app/config/nsecbunker.log\",");
        sb.append("\"keys\":{},");
        sb.append("\"verbose\":true");
        sb.append("}");

        return sb.toString();
    }

    /**
     * Gets the configured relay URLs.
     *
     * @return list of relay URLs
     */
    public List<String> getRelayUrls() {
        return new ArrayList<>(relayUrls);
    }

    /**
     * Gets the admin nsec (for test use).
     *
     * @return the admin nsec
     */
    public String getAdminNsec() {
        return adminNsec;
    }

    /**
     * Gets the bunker nsec.
     *
     * @return the bunker nsec
     */
    public String getBunkerNsec() {
        return bunkerNsec;
    }

    /**
     * Gets the bunker pubkey captured from logs.
     *
     * @return the bunker npub or null if not yet captured
     */
    public String getCapturedBunkerPubkey() {
        return capturedBunkerPubkey.get();
    }

    /**
     * Creates a container configured for use with a relay container in the same network.
     *
     * @param network the Docker network
     * @param relayNetworkAlias the network alias of the relay container
     * @param relayPort the relay port
     * @param adminNpub the admin public key in npub format
     * @param adminNsec the admin private key in nsec format (for test use)
     * @return configured container
     */
    public static NsecBunkerdContainer createWithRelay(
            Network network,
            String relayNetworkAlias,
            int relayPort,
            String adminNpub,
            String adminNsec) {
        String internalRelayUrl = String.format("ws://%s:%d", relayNetworkAlias, relayPort);

        return new NsecBunkerdContainer()
                .withNetwork(network)
                .withRelay(internalRelayUrl)
                .withAdminNpub(adminNpub)
                .withAdminNsec(adminNsec);
    }

    /**
     * Creates a container with a known bunker identity for testing.
     *
     * @param network the Docker network
     * @param relayNetworkAlias the network alias of the relay container
     * @param relayPort the relay port
     * @param adminNpub the admin public key in npub format
     * @param adminNsec the admin private key in nsec format (for test use)
     * @param bunkerPrivateKeyHex the bunker's private key in hex format
     * @param bunkerNsec the bunker's private key in nsec format (for reference)
     * @return configured container
     */
    public static NsecBunkerdContainer createWithKnownIdentity(
            Network network,
            String relayNetworkAlias,
            int relayPort,
            String adminNpub,
            String adminNsec,
            String bunkerPrivateKeyHex,
            String bunkerNsec) {
        String internalRelayUrl = String.format("ws://%s:%d", relayNetworkAlias, relayPort);

        return new NsecBunkerdContainer()
                .withNetwork(network)
                .withRelay(internalRelayUrl)
                .withAdminNpub(adminNpub)
                .withAdminNsec(adminNsec)
                .withBunkerPrivateKeyHex(bunkerPrivateKeyHex)
                .withBunkerNsec(bunkerNsec);
    }
}
