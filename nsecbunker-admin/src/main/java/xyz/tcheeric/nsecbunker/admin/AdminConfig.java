package xyz.tcheeric.nsecbunker.admin;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Configuration for the NsecBunker admin client.
 *
 * <p>This class holds all configuration options needed to connect to and
 * authenticate with an nsecBunker instance as an admin.
 *
 * <p>Usage:
 * <pre>{@code
 * AdminConfig config = AdminConfig.builder()
 *     .bunkerPubkey("npub1...")
 *     .adminPrivateKey("nsec1...")
 *     .relays(List.of("wss://relay.example.com"))
 *     .build();
 * }</pre>
 */
@Getter
@Builder(toBuilder = true)
@ToString(exclude = "adminPrivateKey")
public final class AdminConfig {

    /**
     * The bunker's public key (hex or npub format).
     * This is the key that identifies the bunker instance.
     */
    private final String bunkerPubkey;

    /**
     * The admin's private key (hex or nsec format).
     * Used to authenticate admin operations.
     */
    private final String adminPrivateKey;

    /**
     * List of relay URLs to connect to for communication with the bunker.
     */
    @Builder.Default
    private final List<String> relays = List.of();

    /**
     * Optional secret for initial bunker connection.
     */
    private final String secret;

    /**
     * Connection timeout duration.
     */
    @Builder.Default
    private final Duration connectTimeout = Duration.ofSeconds(30);

    /**
     * Request timeout duration.
     */
    @Builder.Default
    private final Duration requestTimeout = Duration.ofSeconds(60);

    /**
     * Whether to automatically reconnect on connection loss.
     */
    @Builder.Default
    private final boolean autoReconnect = true;

    /**
     * Maximum number of reconnection attempts.
     */
    @Builder.Default
    private final int maxReconnectAttempts = 5;

    /**
     * Initial delay between reconnection attempts.
     */
    @Builder.Default
    private final Duration reconnectDelay = Duration.ofSeconds(1);

    /**
     * Maximum delay between reconnection attempts.
     */
    @Builder.Default
    private final Duration maxReconnectDelay = Duration.ofSeconds(30);

    /**
     * Whether to generate an ephemeral keypair for communication.
     * If true, a new keypair is generated for each session.
     * If false, the admin keypair is used directly.
     */
    @Builder.Default
    private final boolean useEphemeralKey = true;

    /**
     * Validates this configuration.
     *
     * @throws IllegalStateException if configuration is invalid
     */
    public void validate() {
        if (bunkerPubkey == null || bunkerPubkey.isBlank()) {
            throw new IllegalStateException("Bunker public key is required");
        }
        if (adminPrivateKey == null || adminPrivateKey.isBlank()) {
            throw new IllegalStateException("Admin private key is required");
        }
        if (relays == null || relays.isEmpty()) {
            throw new IllegalStateException("At least one relay URL is required");
        }
        for (String relay : relays) {
            if (!relay.startsWith("wss://") && !relay.startsWith("ws://")) {
                throw new IllegalStateException("Invalid relay URL: " + relay);
            }
        }
    }

    /**
     * Creates a new builder.
     *
     * @return a new builder instance
     */
    public static AdminConfigBuilder builder() {
        return new AdminConfigBuilder();
    }

    /**
     * Creates a configuration from a bunker connection string.
     *
     * @param connectionString the bunker:// connection string
     * @param adminPrivateKey  the admin's private key
     * @return a new AdminConfig
     */
    public static AdminConfig fromConnectionString(String connectionString, String adminPrivateKey) {
        Objects.requireNonNull(connectionString, "Connection string must not be null");
        Objects.requireNonNull(adminPrivateKey, "Admin private key must not be null");

        // Parse bunker://pubkey?relay=...&secret=...
        if (!connectionString.startsWith("bunker://")) {
            throw new IllegalArgumentException("Invalid connection string: must start with bunker://");
        }

        String remainder = connectionString.substring(9);
        int queryIndex = remainder.indexOf('?');

        String pubkey;
        String query = null;

        if (queryIndex == -1) {
            pubkey = remainder;
        } else {
            pubkey = remainder.substring(0, queryIndex);
            query = remainder.substring(queryIndex + 1);
        }

        AdminConfigBuilder builder = builder()
                .bunkerPubkey(pubkey)
                .adminPrivateKey(adminPrivateKey);

        if (query != null) {
            String[] params = query.split("&");
            java.util.List<String> relayList = new java.util.ArrayList<>();

            for (String param : params) {
                int eqIndex = param.indexOf('=');
                if (eqIndex == -1) continue;

                String key = param.substring(0, eqIndex);
                String value = java.net.URLDecoder.decode(param.substring(eqIndex + 1), java.nio.charset.StandardCharsets.UTF_8);

                switch (key) {
                    case "relay":
                        relayList.add(value);
                        break;
                    case "secret":
                        builder.secret(value);
                        break;
                }
            }

            if (!relayList.isEmpty()) {
                builder.relays(relayList);
            }
        }

        return builder.build();
    }
}
