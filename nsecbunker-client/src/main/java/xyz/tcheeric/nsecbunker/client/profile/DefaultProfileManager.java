package xyz.tcheeric.nsecbunker.client.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import nostr.base.PublicKey;
import nostr.base.Signature;
import nostr.event.impl.GenericEvent;
import nostr.event.serializer.EventSerializer;
import xyz.tcheeric.nsecbunker.client.signer.RemoteSigner;
import xyz.tcheeric.nsecbunker.client.signer.exception.SignerAdapterException;

import java.time.Instant;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Default profile manager that builds and signs kind-0 profile events via the remote signer.
 */
public class DefaultProfileManager implements ProfileManager {

    private static final int KIND_PROFILE = 0;

    private final RemoteSigner signer;
    private final ObjectMapper objectMapper;

    public DefaultProfileManager(RemoteSigner signer) {
        this(signer, new ObjectMapper());
    }

    public DefaultProfileManager(RemoteSigner signer, ObjectMapper objectMapper) {
        this.signer = Objects.requireNonNull(signer, "signer must not be null");
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    @Override
    public CompletableFuture<GenericEvent> updateProfile(ProfileMetadata metadata) {
        Objects.requireNonNull(metadata, "metadata must not be null");

        return signer.getPublicKey()
                .thenCompose(pub -> buildUnsignedEvent(metadata, pub))
                .thenCompose(event -> signer.signEvent(toUnsignedJson(event))
                        .thenApply(sigHex -> {
                            event.setSignature(Signature.fromString(sigHex));
                            return event;
                        }));
    }

    @Override
    public CompletableFuture<ProfileMetadata> fetchProfile() {
        return CompletableFuture.failedFuture(
                new UnsupportedOperationException("Profile fetch requires relay integration"));
    }

    private CompletableFuture<GenericEvent> buildUnsignedEvent(ProfileMetadata metadata, String pubkeyHex) {
        try {
            PublicKey pubKey = new PublicKey(pubkeyHex);
            long createdAt = Instant.now().getEpochSecond();
            String content = metadata.toJson(objectMapper);

            String id = EventSerializer.serializeAndComputeId(pubKey, createdAt, KIND_PROFILE, Collections.emptyList(), content);

            GenericEvent event = new GenericEvent(pubKey, KIND_PROFILE);
            event.setContent(content);
            event.setCreatedAt(createdAt);
            event.setId(id);
            return CompletableFuture.completedFuture(event);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(new SignerAdapterException("Failed to build profile event: " + e.getMessage(), e));
        }
    }

    private String toUnsignedJson(GenericEvent event) {
        return String.format(
                "{\"id\":\"%s\",\"pubkey\":\"%s\",\"created_at\":%d,\"kind\":%d,\"tags\":[],\"content\":%s,\"sig\":\"\"}",
                event.getId(),
                event.getPubKey(),
                event.getCreatedAt(),
                event.getKind(),
                event.getContent() == null ? "\"\"" : event.getContent()
        );
    }
}
