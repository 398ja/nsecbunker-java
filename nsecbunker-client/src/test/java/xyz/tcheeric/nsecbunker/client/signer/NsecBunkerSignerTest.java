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
}
