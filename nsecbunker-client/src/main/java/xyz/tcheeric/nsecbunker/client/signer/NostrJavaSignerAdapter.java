package xyz.tcheeric.nsecbunker.client.signer;

import nostr.base.ISignable;
import nostr.base.PublicKey;
import nostr.base.Signature;
import xyz.tcheeric.nsecbunker.client.signer.exception.SignerAdapterException;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Adapter to use {@link RemoteSigner} with nostr-java types without requiring local private keys.
 */
public final class NostrJavaSignerAdapter {

    private final RemoteSigner signer;

    public NostrJavaSignerAdapter(RemoteSigner signer) {
        this.signer = Objects.requireNonNull(signer, "signer must not be null");
    }

    public PublicKey getPublicKey() {
        String hex = signer.getPublicKey().join();
        return new PublicKey(hex);
    }

    public Signature sign(ISignable signable) {
        Objects.requireNonNull(signable, "signable must not be null");
        try {
            String payload = extractPayload(signable);
            String sig = signer.signEvent(payload).join();
            Signature signature = Signature.fromString(sig);
            signable.setSignature(signature);
            return signature;
        } catch (Exception e) {
            throw new SignerAdapterException("Failed to sign object: " + e.getMessage(), e);
        }
    }

    /**
     * Asynchronously signs an ISignable using the remote signer.
     *
     * @param signable object to sign
     * @return future with updated signable
     */
    public CompletableFuture<ISignable> signAsync(ISignable signable) {
        Objects.requireNonNull(signable, "signable must not be null");
        String payload = extractPayload(signable);
        return signer.signEvent(payload).thenApply(sig -> {
            Signature signature = Signature.fromString(sig);
            signable.setSignature(signature);
            return signable;
        });
    }

    private String extractPayload(ISignable signable) {
        if (signable.getByteArraySupplier() != null) {
            ByteBuffer buffer = signable.getByteArraySupplier().get();
            if (buffer != null) {
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                return new String(bytes);
            }
        }
        return signable.toString();
    }
}
