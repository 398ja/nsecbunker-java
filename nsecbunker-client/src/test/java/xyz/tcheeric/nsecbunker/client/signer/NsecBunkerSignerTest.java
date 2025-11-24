package xyz.tcheeric.nsecbunker.client.signer;

import org.junit.jupiter.api.Test;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Request;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Response;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NsecBunkerSignerTest {

    private static final String TEST_BUNKER_PUBKEY = "79be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798";
    private static final String TEST_CLIENT_PRIVKEY = "0000000000000000000000000000000000000000000000000000000000000001";

    /**
     * Ensures connect sends a connect request and marks the signer connected on ack.
     */
    @Test
    void shouldConnectAndSetConnectedFlag() {
        // Arrange
        AtomicReference<Nip46Request> captured = new AtomicReference<>();
        NsecBunkerSigner signer = new NsecBunkerSigner(
                SignerConfig.builder()
                        .bunkerPubkey(TEST_BUNKER_PUBKEY)
                        .clientPrivateKey(TEST_CLIENT_PRIVKEY)
                        .relays(List.of("wss://relay.example.com"))
                        .requestTimeout(Duration.ofSeconds(5))
                        .build(),
                request -> {
                    captured.set(request);
                    return Nip46Response.success(request.getId(), "ack");
                },
                null,
                null
        );

        // Act
        signer.connect().join();

        // Assert
        assertThat(signer.isConnected()).isTrue();
        assertThat(captured.get().getMethod()).isEqualTo("connect");
        assertThat(captured.get().getParams()).contains(signer.getCommunicationIdentity().getPublicKey().toString());
    }

    /**
     * Ensures requestPermissions sends the right method and returns the bunker result.
     */
    @Test
    void shouldRequestPermissions() {
        // Arrange
        AtomicReference<Nip46Request> captured = new AtomicReference<>();
        NsecBunkerSigner signer = new NsecBunkerSigner(
                SignerConfig.builder()
                        .bunkerPubkey(TEST_BUNKER_PUBKEY)
                        .clientPrivateKey(TEST_CLIENT_PRIVKEY)
                        .relays(List.of("wss://relay.example.com"))
                        .build(),
                request -> {
                    captured.set(request);
                    return Nip46Response.success(request.getId(), "granted");
                },
                null,
                null
        );

        // Act
        String result = signer.requestPermissions(List.of("sign_event", "nip04_encrypt")).join();

        // Assert
        assertThat(result).isEqualTo("granted");
        assertThat(captured.get().getMethod()).isEqualTo("request_permissions");
        assertThat(captured.get().getParams()).containsExactly("sign_event", "nip04_encrypt");
    }

    /**
     * Ensures unsupported transport without handler fails fast.
     */
    @Test
    void shouldFailWhenNoRequestHandlerConfigured() {
        // Arrange
        NsecBunkerSigner signer = new NsecBunkerSigner(
                SignerConfig.builder()
                        .bunkerPubkey(TEST_BUNKER_PUBKEY)
                        .clientPrivateKey(TEST_CLIENT_PRIVKEY)
                        .relays(List.of("wss://relay.example.com"))
                        .build()
        );

        // Act + Assert
        assertThatThrownBy(() -> signer.connect().join())
                .hasCauseInstanceOf(UnsupportedOperationException.class);
    }

    /**
     * Ensures ephemeral identity is generated when configured.
     */
    @Test
    void shouldGenerateEphemeralIdentity() {
        // Arrange
        NsecBunkerSigner signer = new NsecBunkerSigner(
                SignerConfig.builder()
                        .bunkerPubkey(TEST_BUNKER_PUBKEY)
                        .clientPrivateKey(TEST_CLIENT_PRIVKEY)
                        .relays(List.of("wss://relay.example.com"))
                        .useEphemeralKey(true)
                        .build(),
                request -> Nip46Response.success(request.getId(), "ack"),
                null,
                null
        );

        // Act
        signer.connect().join();

        // Assert
        assertThat(signer.getEphemeralIdentity()).isNotNull();
        assertThat(signer.getCommunicationIdentity()).isEqualTo(signer.getEphemeralIdentity());
    }

    /**
     * Ensures signing and crypto methods issue correct requests and parse results.
     */
    @Test
    void shouldPerformSigningAndCryptoOperations() {
        // Arrange
        AtomicReference<Nip46Request> captured = new AtomicReference<>();
        NsecBunkerSigner signer = new NsecBunkerSigner(
                SignerConfig.builder()
                        .bunkerPubkey(TEST_BUNKER_PUBKEY)
                        .clientPrivateKey(TEST_CLIENT_PRIVKEY)
                        .relays(List.of("wss://relay.example.com"))
                        .build(),
                request -> {
                    captured.set(request);
                    return Nip46Response.success(request.getId(), "result-" + request.getMethod());
                },
                null,
                null
        );

        // Act / Assert: sign_event
        String sig = signer.signEvent("{\"id\":1}").join();
        assertThat(sig).isEqualTo("result-sign_event");
        assertThat(captured.get().getMethod()).isEqualTo("sign_event");

        // nip04_encrypt
        String enc04 = signer.nip04Encrypt(TEST_BUNKER_PUBKEY, "hi").join();
        assertThat(enc04).isEqualTo("result-nip04_encrypt");
        assertThat(captured.get().getParams()).contains(TEST_BUNKER_PUBKEY, "hi");

        // nip04_decrypt
        String dec04 = signer.nip04Decrypt(TEST_BUNKER_PUBKEY, "cipher").join();
        assertThat(dec04).isEqualTo("result-nip04_decrypt");

        // nip44_encrypt
        String enc44 = signer.nip44Encrypt(TEST_BUNKER_PUBKEY, "hi").join();
        assertThat(enc44).isEqualTo("result-nip44_encrypt");

        // nip44_decrypt
        String dec44 = signer.nip44Decrypt(TEST_BUNKER_PUBKEY, "cipher").join();
        assertThat(dec44).isEqualTo("result-nip44_decrypt");

        // ping
        String pong = signer.ping().join();
        assertThat(pong).isEqualTo("result-ping");

        // get_public_key
        String pub = signer.getPublicKey().join();
        assertThat(pub).isEqualTo("result-get_public_key");
    }

    /**
     * Ensures auth_url responses set state appropriately.
     */
    @Test
    void shouldHandleAuthUrlResponse() {
        // Arrange
        NsecBunkerSigner signer = new NsecBunkerSigner(
                SignerConfig.builder()
                        .bunkerPubkey(TEST_BUNKER_PUBKEY)
                        .clientPrivateKey(TEST_CLIENT_PRIVKEY)
                        .relays(List.of("wss://relay.example.com"))
                        .build(),
                request -> Nip46Response.success(request.getId(), "auth_url:https://auth"),
                null,
                null
        );

        // Act + Assert
        assertThatThrownBy(() -> signer.connect().join())
                .hasCauseInstanceOf(SignerException.class)
                .hasMessageContaining("Authorization required");
        assertThat(signer.getState()).isEqualTo(SignerState.AUTH_URL_REQUIRED);
    }
}
