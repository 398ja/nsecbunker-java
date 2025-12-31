package xyz.tcheeric.nsecbunker.it;

import org.junit.jupiter.api.Test;
import xyz.tcheeric.nsecbunker.client.signer.NsecBunkerSigner;
import xyz.tcheeric.nsecbunker.client.signer.SignerConfig;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Response;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class SignerRequestExecutorIntegrationTest {

    private static final String TEST_BUNKER_PUBKEY = "79be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798";
    private static final String TEST_CLIENT_PRIVKEY = "0000000000000000000000000000000000000000000000000000000000000001";

    /**
     * Ensures retries happen when the transport fails initially.
     */
    @Test
    void shouldRetryAndSucceed() {
        AtomicInteger attempts = new AtomicInteger();

        NsecBunkerSigner signer = new NsecBunkerSigner(
                SignerConfig.builder()
                        .bunkerPubkey(TEST_BUNKER_PUBKEY)
                        .clientPrivateKey(TEST_CLIENT_PRIVKEY)
                        .relays(List.of("wss://relay.example.com"))
                        .requestTimeout(Duration.ofSeconds(1))
                        .build(),
                request -> {
                    if (attempts.incrementAndGet() == 1) {
                        throw new RuntimeException("fail first");
                    }
                    return Nip46Response.success(request.getId(), "ok" + attempts.get());
                },
                null,
                null
        );

        String result = signer.signEvent("{\"id\":1}").join();

        assertThat(result).isEqualTo("ok2");
        assertThat(attempts.get()).isEqualTo(2);
    }
}
