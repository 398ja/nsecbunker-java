package xyz.tcheeric.nsecbunker.client.signer;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultBatchSignerTest {

    /**
     * Ensures signEvents fans out and aggregates results.
     */
    @Test
    void shouldSignEventsInBatch() {
        // Arrange
        AtomicInteger calls = new AtomicInteger();
        RemoteSigner signer = new RemoteSigner() {
            @Override
            public CompletableFuture<Void> connect() { return CompletableFuture.completedFuture(null); }
            @Override
            public CompletableFuture<Void> disconnect() { return CompletableFuture.completedFuture(null); }
            @Override
            public boolean isConnected() { return true; }
            @Override
            public CompletableFuture<String> signEvent(String eventJson) {
                calls.incrementAndGet();
                return CompletableFuture.completedFuture("sig-" + eventJson);
            }
            @Override
            public CompletableFuture<String> getPublicKey() { return CompletableFuture.completedFuture("pub"); }
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
        };
        DefaultBatchSigner batchSigner = new DefaultBatchSigner(signer);

        // Act
        List<String> results = batchSigner.signEvents(List.of("e1", "e2", "e3")).join();

        // Assert
        assertThat(results).containsExactly("sig-e1", "sig-e2", "sig-e3");
        assertThat(calls.get()).isEqualTo(3);
    }

    /**
     * Ensures executeBatch aggregates arbitrary operations.
     */
    @Test
    void shouldAggregateBatchResults() {
        // Arrange
        DefaultBatchSigner batchSigner = new DefaultBatchSigner(new NoopRemoteSigner());
        List<CompletableFuture<String>> ops = List.of(
                CompletableFuture.completedFuture("a"),
                CompletableFuture.completedFuture("b")
        );

        // Act
        List<String> results = batchSigner.executeBatch(ops).join();

        // Assert
        assertThat(results).containsExactly("a", "b");
    }

    private static final class NoopRemoteSigner implements RemoteSigner {
        @Override
        public CompletableFuture<Void> connect() { return CompletableFuture.completedFuture(null); }
        @Override
        public CompletableFuture<Void> disconnect() { return CompletableFuture.completedFuture(null); }
        @Override
        public boolean isConnected() { return true; }
        @Override
        public CompletableFuture<String> signEvent(String eventJson) { return CompletableFuture.completedFuture(eventJson); }
        @Override
        public CompletableFuture<String> getPublicKey() { return CompletableFuture.completedFuture(""); }
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
        public CompletableFuture<String> requestPermissions(List<String> methods) { return CompletableFuture.completedFuture(""); }
        @Override
        public void close() { }
    }
}
