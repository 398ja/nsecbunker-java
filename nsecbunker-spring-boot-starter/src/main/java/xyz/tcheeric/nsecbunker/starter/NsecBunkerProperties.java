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
}
