package xyz.tcheeric.nsecbunker.client.profile;

import nostr.event.impl.GenericEvent;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.nsecbunker.client.signer.RemoteSigner;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultProfileManagerTest {

    /**
     * Ensures updateProfile builds a kind-0 event and applies the returned signature.
     */
    @Test
    void shouldBuildAndSignProfileEvent() {
        // Arrange
        AtomicReference<String> signedPayload = new AtomicReference<>();
        RemoteSigner signer = new StubSigner("deadbeef", "79be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798", signedPayload);
        DefaultProfileManager manager = new DefaultProfileManager(signer);
        ProfileMetadata metadata = ProfileMetadata.builder()
                .name("alice")
                .about("hi")
                .build();

        // Act
        GenericEvent event = manager.updateProfile(metadata).join();

        // Assert
        assertThat(event.getKind()).isEqualTo(0);
        assertThat(event.getContent()).contains("alice");
        assertThat(event.getSignature().toString()).isEqualTo("deadbeef");
        assertThat(signedPayload.get()).contains("\"kind\":0");
    }

    private static final class StubSigner implements RemoteSigner {
        private final String signature;
        private final String pubkey;
        private final AtomicReference<String> payloadSink;

        StubSigner(String signature, String pubkey, AtomicReference<String> payloadSink) {
            this.signature = signature;
            this.pubkey = pubkey;
            this.payloadSink = payloadSink;
        }

        @Override
        public CompletableFuture<Void> connect() { return CompletableFuture.completedFuture(null); }
        @Override
        public CompletableFuture<Void> disconnect() { return CompletableFuture.completedFuture(null); }
        @Override
        public boolean isConnected() { return true; }
        @Override
        public CompletableFuture<String> signEvent(String eventJson) {
            payloadSink.set(eventJson);
            return CompletableFuture.completedFuture(signature);
        }
        @Override
        public CompletableFuture<String> getPublicKey() { return CompletableFuture.completedFuture(pubkey); }
        @Override
        public CompletableFuture<String> nip04Encrypt(String pubkeyHex, String plaintext) { return CompletableFuture.completedFuture(""); }
        @Override
        public CompletableFuture<String> nip04Decrypt(String pubkeyHex, String ciphertext) { return CompletableFuture.completedFuture(""); }
        @Override
        public CompletableFuture<String> nip44Encrypt(String pubkeyHex, String plaintext) { return CompletableFuture.completedFuture(""); }
        @Override
        public CompletableFuture<String> nip44Decrypt(String pubkeyHex, String ciphertext) { return CompletableFuture.completedFuture(""); }
        @Override
        public CompletableFuture<String> ping() { return CompletableFuture.completedFuture("pong"); }
        @Override
        public CompletableFuture<String> requestPermissions(List<String> methods) { return CompletableFuture.completedFuture("ok"); }
        @Override
        public void close() { }
    }
}
