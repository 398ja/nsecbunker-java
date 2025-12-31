package xyz.tcheeric.nsecbunker.client.integration;

import org.junit.jupiter.api.Test;
import xyz.tcheeric.nsecbunker.client.signer.DefaultBatchSigner;
import xyz.tcheeric.nsecbunker.client.signer.NsecBunkerSigner;
import xyz.tcheeric.nsecbunker.client.signer.SignerConfig;
import xyz.tcheeric.nsecbunker.client.signer.SignerException;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Response;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SignerIntegrationTest {

    private static final String TEST_BUNKER_PUBKEY = "79be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798";
    private static final String TEST_CLIENT_PRIVKEY = "0000000000000000000000000000000000000000000000000000000000000001";

    /**
     * Verifies connect → sign → verify response flow with stub transport.
     */
    @Test
    void shouldConnectAndSignEvent() {
        // Arrange
        NsecBunkerSigner signer = new NsecBunkerSigner(
                SignerConfig.builder()
                        .bunkerPubkey(TEST_BUNKER_PUBKEY)
                        .clientPrivateKey(TEST_CLIENT_PRIVKEY)
                        .relays(List.of("wss://relay.example.com"))
                        .requestTimeout(Duration.ofSeconds(2))
                        .build(),
                request -> switch (request.getMethod()) {
                    case "connect" -> Nip46Response.success(request.getId(), "ack");
                    case "sign_event" -> Nip46Response.success(request.getId(), "sig-hex");
                    default -> Nip46Response.error(request.getId(), "UNKNOWN", "unknown");
                },
                null,
                null
        );

        // Act
        signer.connect().join();
        String sig = signer.signEvent("{\"id\":1}").join();

        // Assert
        assertThat(signer.isConnected()).isTrue();
        assertThat(sig).isEqualTo("sig-hex");
    }

    /**
     * Verifies batch signing aggregates results.
     */
    @Test
    void shouldSignMultipleEventsWithBatchSigner() {
        // Arrange
        NsecBunkerSigner signer = new NsecBunkerSigner(
                SignerConfig.builder()
                        .bunkerPubkey(TEST_BUNKER_PUBKEY)
                        .clientPrivateKey(TEST_CLIENT_PRIVKEY)
                        .relays(List.of("wss://relay.example.com"))
                        .build(),
                request -> Nip46Response.success(request.getId(), "sig-" + request.getFirstParam()),
                null,
                null
        );
        DefaultBatchSigner batchSigner = new DefaultBatchSigner(signer);

        // Act
        List<String> results = batchSigner.signEvents(List.of("e1", "e2")).join();

        // Assert
        assertThat(results).containsExactly("sig-e1", "sig-e2");
    }

    /**
     * Verifies NIP-04/44 encryption/decryption via signer.
     */
    @Test
    void shouldEncryptAndDecrypt() {
        // Arrange
        NsecBunkerSigner signer = new NsecBunkerSigner(
                SignerConfig.builder()
                        .bunkerPubkey(TEST_BUNKER_PUBKEY)
                        .clientPrivateKey(TEST_CLIENT_PRIVKEY)
                        .relays(List.of("wss://relay.example.com"))
                        .build(),
                request -> Nip46Response.success(request.getId(), request.getMethod() + "-ok"),
                null,
                null
        );

        // Act
        String enc04 = signer.nip04Encrypt(TEST_BUNKER_PUBKEY, "hi").join();
        String dec04 = signer.nip04Decrypt(TEST_BUNKER_PUBKEY, "cipher").join();
        String enc44 = signer.nip44Encrypt(TEST_BUNKER_PUBKEY, "hi").join();
        String dec44 = signer.nip44Decrypt(TEST_BUNKER_PUBKEY, "cipher").join();

        // Assert
        assertThat(enc04).isEqualTo("nip04_encrypt-ok");
        assertThat(dec04).isEqualTo("nip04_decrypt-ok");
        assertThat(enc44).isEqualTo("nip44_encrypt-ok");
        assertThat(dec44).isEqualTo("nip44_decrypt-ok");
    }

    /**
     * Verifies connection lifecycle transitions.
     */
    @Test
    void shouldHandleConnectionLifecycle() {
        // Arrange
        NsecBunkerSigner signer = new NsecBunkerSigner(
                SignerConfig.builder()
                        .bunkerPubkey(TEST_BUNKER_PUBKEY)
                        .clientPrivateKey(TEST_CLIENT_PRIVKEY)
                        .relays(List.of("wss://relay.example.com"))
                        .build(),
                request -> Nip46Response.success(request.getId(), "ack"),
                null,
                null
        );

        // Act
        signer.connect().join();
        signer.disconnect().join();

        // Assert
        assertThat(signer.isConnected()).isFalse();
    }

    /**
     * Verifies errors propagate through the executor.
     */
    @Test
    void shouldPropagateErrors() {
        // Arrange
        NsecBunkerSigner signer = new NsecBunkerSigner(
                SignerConfig.builder()
                        .bunkerPubkey(TEST_BUNKER_PUBKEY)
                        .clientPrivateKey(TEST_CLIENT_PRIVKEY)
                        .relays(List.of("wss://relay.example.com"))
                        .build(),
                request -> {
                    throw new RuntimeException("transport failure");
                },
                null,
                null
        );

        // Act + Assert
        assertThatThrownBy(() -> signer.signEvent("{\"id\":1}").join())
                .hasCauseInstanceOf(SignerException.class);
    }

    /**
     * Light performance smoke: batch of small operations completes quickly.
     */
    @Test
    void shouldCompleteBatchQuickly() {
        // Arrange
        AtomicReference<Integer> counter = new AtomicReference<>(0);
        NsecBunkerSigner signer = new NsecBunkerSigner(
                SignerConfig.builder()
                        .bunkerPubkey(TEST_BUNKER_PUBKEY)
                        .clientPrivateKey(TEST_CLIENT_PRIVKEY)
                        .relays(List.of("wss://relay.example.com"))
                        .build(),
                request -> {
                    counter.set(counter.get() + 1);
                    return Nip46Response.success(request.getId(), "ok");
                },
                null,
                null
        );
        DefaultBatchSigner batchSigner = new DefaultBatchSigner(signer);

        // Act
        long start = System.currentTimeMillis();
        batchSigner.signEvents(List.of("a", "b", "c")).join();
        long elapsed = System.currentTimeMillis() - start;

        // Assert
        assertThat(counter.get()).isEqualTo(3);
        assertThat(elapsed).isLessThan(500);
    }
}
