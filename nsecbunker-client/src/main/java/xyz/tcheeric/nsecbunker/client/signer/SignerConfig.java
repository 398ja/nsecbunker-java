package xyz.tcheeric.nsecbunker.client.signer;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.Duration;
import java.util.List;

/**
 * Configuration for {@link NsecBunkerSigner}.
 */
@Getter
@Builder(toBuilder = true)
@ToString(exclude = "secret")
public final class SignerConfig {

    private final String bunkerPubkey;
    @Builder.Default
    private final List<String> relays = List.of();
    private final String secret;
    private final String token;
    private final String clientPrivateKey;

    @Builder.Default
    private final Duration connectTimeout = Duration.ofSeconds(30);
    @Builder.Default
    private final Duration requestTimeout = Duration.ofSeconds(60);
    @Builder.Default
    private final boolean useEphemeralKey = true;

    /**
     * Optional token-based connection string.
     */
    private final String bunkerToken;

    public void validate() {
        if (bunkerPubkey == null || bunkerPubkey.isBlank()) {
            throw new IllegalStateException("Bunker public key is required");
        }
        if (clientPrivateKey == null || clientPrivateKey.isBlank()) {
            throw new IllegalStateException("Client private key is required");
        }
        if (relays == null || relays.isEmpty()) {
            throw new IllegalStateException("At least one relay URL is required");
        }
    }
}
