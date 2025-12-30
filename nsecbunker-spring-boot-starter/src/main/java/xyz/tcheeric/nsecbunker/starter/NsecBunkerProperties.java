package xyz.tcheeric.nsecbunker.starter;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Spring Boot properties for nsecBunker integration.
 */
@Data
@ConfigurationProperties(prefix = "nsecbunker")
public class NsecBunkerProperties {

    private final Admin admin = new Admin();
    private final Signer signer = new Signer();
    private final Metrics metrics = new Metrics();
    private final Nip05 nip05 = new Nip05();

    /**
     * Admin client configuration properties.
     */
    @Data
    public static class Admin {
        /** Bunker's public key (hex or npub). */
        private String bunkerPubkey;
        /** Admin private key for authentication (hex or nsec). */
        private String adminPrivateKey;
        /** Relay URLs to connect to. */
        private List<String> relays = new ArrayList<>();
        /** Optional bunker secret for additional auth. */
        private String secret;
        /** Timeout for establishing connections. */
        private Duration connectTimeout = Duration.ofSeconds(30);
        /** Timeout for individual requests. */
        private Duration requestTimeout = Duration.ofSeconds(60);
        /** Whether to use ephemeral keys for communication. */
        private boolean useEphemeralKey = true;
    }

    /**
     * Signer client configuration properties.
     */
    @Data
    public static class Signer {
        /** Bunker's public key (hex or npub). */
        private String bunkerPubkey;
        /** Client private key for authentication (hex or nsec). */
        private String clientPrivateKey;
        /** Relay URLs to connect to. */
        private List<String> relays = new ArrayList<>();
        /** Optional bunker secret for additional auth. */
        private String secret;
        /** Whether to use ephemeral keys for communication. */
        private boolean useEphemeralKey = true;
        /** Timeout for establishing connections. */
        private Duration connectTimeout = Duration.ofSeconds(30);
        /** Timeout for individual requests. */
        private Duration requestTimeout = Duration.ofSeconds(60);
    }

    /**
     * Metrics configuration properties.
     */
    @Data
    public static class Metrics {
        /**
         * Whether to enable nsecBunker metrics collection.
         */
        private boolean enabled = true;

        /**
         * Prefix for all nsecBunker metrics.
         */
        private String prefix = "nsecbunker";

        /**
         * Whether to include percentile histograms for latency metrics.
         */
        private boolean percentiles = true;

        /**
         * Whether to record per-method metrics.
         */
        private boolean perMethodMetrics = true;
    }

    /**
     * NIP-05 configuration properties.
     *
     * <p>These properties control how NIP-05 identity management is configured,
     * including which provider implementation to use.</p>
     */
    @Data
    public static class Nip05 {
        /**
         * Whether NIP-05 functionality is enabled.
         */
        private boolean enabled = true;

        /**
         * The provider to use for NIP-05 management.
         *
         * <p>Options:</p>
         * <ul>
         *   <li>{@code auto} - Automatically select highest priority provider</li>
         *   <li>{@code in-memory} - Use in-memory storage (default)</li>
         *   <li>{@code bottin} - Use bottin persistent storage</li>
         *   <li>Custom provider name</li>
         * </ul>
         */
        private String provider = "auto";

        /**
         * Default relays to associate with new NIP-05 records.
         */
        private List<String> defaultRelays = new ArrayList<>();

        /**
         * Whether to auto-register NIP-05 when creating keys via admin client.
         */
        private boolean autoRegister = false;

        /**
         * Default domain for auto-registration (required if autoRegister is true).
         */
        private String defaultDomain;
    }
}
