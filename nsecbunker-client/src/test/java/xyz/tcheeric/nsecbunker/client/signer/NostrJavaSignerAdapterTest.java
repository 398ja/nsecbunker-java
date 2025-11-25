package xyz.tcheeric.nsecbunker.client.signer;

import nostr.base.ISignable;
import nostr.base.PublicKey;
import nostr.base.Signature;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class NostrJavaSignerAdapterTest {

    /**
     * Ensures adapter fetches public key via remote signer.
     */
    @Test
    void shouldGetPublicKeyFromSigner() {
        RemoteSigner signer = new StubSigner("sig", "pubkeyhex");
        NostrJavaSignerAdapter adapter = new NostrJavaSignerAdapter(signer);

        PublicKey pk = adapter.getPublicKey();

        assertThat(pk.toString()).isEqualTo("pubkeyhex");
    }

    /**
     * Ensures adapter signs ISignable objects using the remote signer.
     */
    @Test
    void shouldSignSignable() {
        RemoteSigner signer = new StubSigner("deadbeef", "pubkeyhex");
        NostrJavaSignerAdapter adapter = new NostrJavaSignerAdapter(signer);
        DummySignable signable = new DummySignable("payload");

        Signature signature = adapter.sign(signable);

        assertThat(signature.toString()).isEqualTo("deadbeef");
        assertThat(signable.getSignature().toString()).isEqualTo("deadbeef");
    }

    private static final class StubSigner implements RemoteSigner {
        private final String signature;
        private final String pubkey;

        StubSigner(String signature, String pubkey) {
            this.signature = signature;
            this.pubkey = pubkey;
        }

        @Override
        public CompletableFuture<Void> connect() { return CompletableFuture.completedFuture(null); }
        @Override
        public CompletableFuture<Void> disconnect() { return CompletableFuture.completedFuture(null); }
        @Override
        public boolean isConnected() { return true; }
        @Override
        public CompletableFuture<String> signEvent(String eventJson) { return CompletableFuture.completedFuture(signature); }
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

    private static final class DummySignable implements ISignable {
        private Signature signature;
        private final String payload;

        DummySignable(String payload) {
            this.payload = payload;
        }

        @Override
        public Signature getSignature() { return signature; }

        @Override
        public void setSignature(Signature signature) { this.signature = signature; }

        @Override
        public java.util.function.Consumer<Signature> getSignatureConsumer() { return this::setSignature; }

        @Override
        public java.util.function.Supplier<ByteBuffer> getByteArraySupplier() {
            return () -> ByteBuffer.wrap(payload.getBytes());
        }

        @Override
        public String toString() {
            return payload;
        }
    }
}
