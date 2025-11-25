package xyz.tcheeric.nsecbunker.examples;

import xyz.tcheeric.nsecbunker.client.signer.NsecBunkerSigner;
import xyz.tcheeric.nsecbunker.client.signer.SignerConfig;

import java.time.Duration;
import java.util.List;

public final class SignerExample {
    public static void main(String[] args) {
        String bunkerPubkey = getenv("BUNKER_PUBKEY");
        String clientPrivkey = getenv("CLIENT_PRIVKEY");
        String relay = getenv("RELAY_URL");

        NsecBunkerSigner signer = new NsecBunkerSigner(
                SignerConfig.builder()
                        .bunkerPubkey(bunkerPubkey)
                        .clientPrivateKey(clientPrivkey)
                        .relays(List.of(relay))
                        .requestTimeout(Duration.ofSeconds(10))
                        .build(),
                request -> {
                    throw new UnsupportedOperationException("Hook up to real transport");
                },
                null,
                null
        );

        System.out.println("Signer ready with pubkey: " + signer.getCommunicationIdentity().getPublicKey());
    }

    private static String getenv(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing env var: " + key);
        }
        return value;
    }
}
