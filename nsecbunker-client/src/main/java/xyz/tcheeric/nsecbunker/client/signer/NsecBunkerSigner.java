package xyz.tcheeric.nsecbunker.client.signer;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nostr.id.Identity;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Decoder;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Encoder;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Request;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Response;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * Remote signer implementation backed by nsecBunker using NIP-46.
 *
 * <p>This foundation focuses on connection and permission flows. Transport is
 * provided via an injected request handler for testability; a real transport
 * can later bridge to relays.</p>
 */
@Slf4j
public class NsecBunkerSigner implements RemoteSigner {

    private final SignerConfig config;
    private final Identity clientIdentity;
    @Getter
    private final Identity communicationIdentity;
    private final String bunkerPubkeyHex;
    private final Nip46Encoder encoder;
    private final Nip46Decoder decoder;
    private final Duration requestTimeout;
    private final AtomicBoolean connected;
    private final Function<Nip46Request, Nip46Response> requestHandler;

    @Getter
    private final Identity ephemeralIdentity;

    /**
     * Creates a signer with the default encoder/decoder and no-op transport.
     *
     * @param config signer configuration
     */
    public NsecBunkerSigner(SignerConfig config) {
        this(config, null, new Nip46Encoder(), new Nip46Decoder());
    }

    /**
     * Creates a signer with a custom request handler (useful for tests).
     *
     * @param config         signer configuration
     * @param requestHandler handler that executes NIP-46 requests and returns responses
     * @param encoder        request encoder
     * @param decoder        response decoder
     */
    public NsecBunkerSigner(
            SignerConfig config,
            Function<Nip46Request, Nip46Response> requestHandler,
            Nip46Encoder encoder,
            Nip46Decoder decoder
    ) {
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.requestHandler = requestHandler;
        this.encoder = encoder != null ? encoder : new Nip46Encoder();
        this.decoder = decoder != null ? decoder : new Nip46Decoder();
        this.connected = new AtomicBoolean(false);
        this.requestTimeout = config.getRequestTimeout();

        config.validate();

        this.clientIdentity = createIdentity(config.getClientPrivateKey());

        if (config.isUseEphemeralKey()) {
            this.ephemeralIdentity = Identity.generateRandomIdentity();
            this.communicationIdentity = ephemeralIdentity;
        } else {
            this.ephemeralIdentity = null;
            this.communicationIdentity = clientIdentity;
        }

        this.bunkerPubkeyHex = resolvePubkeyToHex(config.getBunkerPubkey());
        log.debug("Signer initialized with comm pubkey {}", communicationIdentity.getPublicKey());
    }

    @Override
    public CompletableFuture<Void> connect() {
        Nip46Request connectRequest = Nip46Request.connect(
                communicationIdentity.getPublicKey().toString(),
                config.getSecret()
        );

        return sendRequest(connectRequest)
                .thenAccept(response -> {
                    if (!"ack".equalsIgnoreCase(response.getResult())) {
                        throw new SignerException("Connect failed: " + response.getResult());
                    }
                    connected.set(true);
                });
    }

    @Override
    public CompletableFuture<Void> disconnect() {
        connected.set(false);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void close() {
        disconnect();
    }

    @Override
    public boolean isConnected() {
        return connected.get();
    }

    @Override
    public CompletableFuture<String> requestPermissions(List<String> methods) {
        List<String> params = methods == null ? Collections.emptyList() : methods;
        Nip46Request request = Nip46Request.builder()
                .method("request_permissions")
                .params(params)
                .build();
        return sendRequest(request).thenApply(Nip46Response::getResult);
    }

    private CompletableFuture<Nip46Response> sendRequest(Nip46Request request) {
        if (requestHandler == null) {
            return CompletableFuture.failedFuture(
                    new UnsupportedOperationException("No request handler configured for NsecBunkerSigner"));
        }

        try {
            Nip46Response response = requestHandler.apply(request);
            if (response == null) {
                return CompletableFuture.failedFuture(new SignerException("Null response from request handler"));
            }
            if (response.isError()) {
                return CompletableFuture.failedFuture(new SignerException(response.getError().toString()));
            }
            return CompletableFuture.completedFuture(response);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private Identity createIdentity(String privateKey) {
        if (privateKey.startsWith("nsec1")) {
            return Identity.create(privateKey);
        }
        return Identity.create(privateKey);
    }

    private String resolvePubkeyToHex(String pubkey) {
        if (pubkey.startsWith("npub1")) {
            try {
                return nostr.crypto.bech32.Bech32.fromBech32(pubkey);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid npub: " + pubkey, e);
            }
        }
        return pubkey;
    }
}
