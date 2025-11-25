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

    @Data
    public static class Admin {
        private String bunkerPubkey;
        private String adminPrivateKey;
        private List<String> relays = new ArrayList<>();
        private String secret;
        private Duration connectTimeout = Duration.ofSeconds(30);
        private Duration requestTimeout = Duration.ofSeconds(60);
        private boolean useEphemeralKey = true;
    }

    @Data
    public static class Signer {
        private String bunkerPubkey;
        private String clientPrivateKey;
        private List<String> relays = new ArrayList<>();
        private String secret;
        private boolean useEphemeralKey = true;
        private Duration connectTimeout = Duration.ofSeconds(30);
        private Duration requestTimeout = Duration.ofSeconds(60);
    }
}
