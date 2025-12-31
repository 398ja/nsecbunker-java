package xyz.tcheeric.nsecbunker.client.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import nostr.event.impl.GenericEvent;
import nostr.id.Identity;
import xyz.tcheeric.nsecbunker.connection.RelayConnection;
import xyz.tcheeric.nsecbunker.connection.RelayPool;
import xyz.tcheeric.nsecbunker.protocol.crypto.Nip04Crypto;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Decoder;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Encoder;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Request;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Response;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

/**
 * Relay-based transport for NIP-46 requests.
 *
 * <p>This transport sends NIP-46 requests as kind 24133 events over Nostr relays
 * and waits for corresponding response events. It handles:
 * <ul>
 *   <li>NIP-04 encryption/decryption of request/response content</li>
 *   <li>Event creation and signing for outgoing requests</li>
 *   <li>Subscription management for incoming responses</li>
 *   <li>Request/response correlation via request ID</li>
 *   <li>Timeout handling</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>{@code
 * RelayNip46Transport transport = RelayNip46Transport.builder()
 *     .relays(List.of("wss://relay.example.com"))
 *     .clientPrivateKey("...")
 *     .bunkerPubkey("...")
 *     .timeout(Duration.ofSeconds(30))
 *     .build();
 *
 * transport.connect();
 *
 * Function<Nip46Request, Nip46Response> handler = transport.createRequestHandler();
 * NsecBunkerSigner signer = new NsecBunkerSigner(config, handler, encoder, decoder);
 * }</pre>
 */
@Slf4j
public class RelayNip46Transport implements AutoCloseable {

    private static final int NIP46_KIND = 24133;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RelayPool relayPool;
    private final Identity clientIdentity;
    private final String bunkerPubkeyHex;
    private final Nip04Crypto crypto;
    private final Nip46Encoder encoder;
    private final Nip46Decoder decoder;
    private final Duration timeout;
    private final Map<String, CompletableFuture<Nip46Response>> pendingRequests;

    private volatile boolean connected = false;
    private volatile String subscriptionId;

    @Builder
    private RelayNip46Transport(
            List<String> relays,
            String clientPrivateKey,
            String bunkerPubkey,
            Duration timeout,
            Duration connectTimeout
    ) {
        Objects.requireNonNull(relays, "relays must not be null");
        Objects.requireNonNull(clientPrivateKey, "clientPrivateKey must not be null");
        Objects.requireNonNull(bunkerPubkey, "bunkerPubkey must not be null");

        this.timeout = timeout != null ? timeout : Duration.ofSeconds(30);
        Duration connTimeout = connectTimeout != null ? connectTimeout : Duration.ofSeconds(10);

        // Create relay pool
        this.relayPool = RelayPool.builder()
                .relays(relays)
                .connectTimeout(connTimeout)
                .minConnectedRelays(1)
                .deduplicateEvents(true)
                .build();

        // Create client identity from private key
        this.clientIdentity = createIdentity(clientPrivateKey);

        // Resolve bunker pubkey to hex
        this.bunkerPubkeyHex = resolvePubkeyToHex(bunkerPubkey);

        // Setup NIP-04 encryption
        String clientPrivateKeyHex = resolvePrivateKeyToHex(clientPrivateKey);
        this.crypto = Nip04Crypto.create(clientPrivateKeyHex, bunkerPubkeyHex);

        this.encoder = new Nip46Encoder();
        this.decoder = new Nip46Decoder();
        this.pendingRequests = new ConcurrentHashMap<>();

        // Add listener for incoming events
        this.relayPool.addListener(new ResponseListener());
    }

    /**
     * Connects to relays and starts listening for responses.
     *
     * @throws RuntimeException if connection fails
     */
    public void connect() {
        if (connected) {
            return;
        }

        try {
            relayPool.connectAll();

            // Subscribe to events addressed to us
            String clientPubkeyHex = clientIdentity.getPublicKey().toString();
            subscriptionId = "nip46-" + clientPubkeyHex.substring(0, 8);

            // Filter for kind 24133 events from bunker to us
            String filter = String.format(
                    "{\"kinds\":[%d],\"#p\":[\"%s\"],\"authors\":[\"%s\"]}",
                    NIP46_KIND, clientPubkeyHex, bunkerPubkeyHex
            );

            relayPool.broadcastReq(subscriptionId, filter);
            connected = true;
            log.info("relay_transport_connected relays={} subscription={}",
                    relayPool.getConnectedCount(), subscriptionId);

        } catch (Exception e) {
            log.error("relay_transport_connection_failed error={}", e.getMessage());
            throw new RuntimeException("Failed to connect relay transport", e);
        }
    }

    /**
     * Creates a request handler function for use with NsecBunkerSigner.
     *
     * @return a function that sends requests and returns responses
     */
    public Function<Nip46Request, Nip46Response> createRequestHandler() {
        return request -> {
            try {
                return sendRequestAsync(request).get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                log.error("nip46_request_timeout request_id={} method={}",
                        request.getId(), request.getMethod());
                throw new RuntimeException("NIP-46 request timed out: " + request.getId(), e);
            } catch (Exception e) {
                log.error("nip46_request_failed request_id={} method={} error={}",
                        request.getId(), request.getMethod(), e.getMessage());
                throw new RuntimeException("NIP-46 request failed: " + e.getMessage(), e);
            }
        };
    }

    /**
     * Sends a NIP-46 request asynchronously.
     *
     * @param request the request to send
     * @return a future that completes with the response
     */
    public CompletableFuture<Nip46Response> sendRequestAsync(Nip46Request request) {
        Objects.requireNonNull(request, "request must not be null");

        if (!connected) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Transport not connected"));
        }

        CompletableFuture<Nip46Response> future = new CompletableFuture<>();
        pendingRequests.put(request.getId(), future);

        try {
            // Encode and encrypt request
            String requestJson = encoder.encodeRequest(request);
            String encryptedContent = crypto.encrypt(requestJson);

            // Create and sign the event
            String eventJson = createSignedEventJson(encryptedContent);

            // Send to all connected relays
            int sentCount = relayPool.broadcastEvent(eventJson);

            if (sentCount == 0) {
                pendingRequests.remove(request.getId());
                return CompletableFuture.failedFuture(
                        new RuntimeException("No relays available to send request"));
            }

            log.debug("nip46_request_sent request_id={} method={} relays={}",
                    request.getId(), request.getMethod(), sentCount);

            // Setup timeout
            CompletableFuture.delayedExecutor(timeout.toMillis(), TimeUnit.MILLISECONDS)
                    .execute(() -> {
                        CompletableFuture<Nip46Response> pending = pendingRequests.remove(request.getId());
                        if (pending != null && !pending.isDone()) {
                            pending.completeExceptionally(
                                    new TimeoutException("Request timed out: " + request.getId()));
                        }
                    });

            return future;

        } catch (Exception e) {
            pendingRequests.remove(request.getId());
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public void close() {
        if (subscriptionId != null) {
            try {
                relayPool.broadcastClose(subscriptionId);
            } catch (Exception e) {
                log.debug("Error closing subscription: {}", e.getMessage());
            }
        }
        relayPool.close();
        pendingRequests.clear();
        connected = false;
        log.info("relay_transport_closed");
    }

    /**
     * Checks if the transport is connected.
     *
     * @return true if connected to at least one relay
     */
    public boolean isConnected() {
        return connected && relayPool.hasMinimumConnections();
    }

    /**
     * Gets the number of connected relays.
     *
     * @return the connected relay count
     */
    public int getConnectedRelayCount() {
        return relayPool.getConnectedCount();
    }

    /**
     * Creates a signed kind 24133 event JSON.
     */
    private String createSignedEventJson(String content) {
        long createdAt = Instant.now().getEpochSecond();
        String clientPubkeyHex = clientIdentity.getPublicKey().toString();

        // Build tags - p tag for recipient (bunker)
        String tagsJson = String.format("[[\"p\",\"%s\"]]", bunkerPubkeyHex);

        // Build unsigned event for ID calculation
        String serialized = String.format(
                "[0,\"%s\",%d,%d,%s,\"%s\"]",
                clientPubkeyHex, createdAt, NIP46_KIND, tagsJson, escapeJson(content)
        );

        // Calculate event ID (sha256 of serialized)
        String eventId = sha256Hex(serialized);

        // Sign the event ID
        String signature = signMessage(eventId);

        // Build complete event JSON
        return String.format(
                "{\"id\":\"%s\",\"pubkey\":\"%s\",\"created_at\":%d,\"kind\":%d,\"tags\":%s,\"content\":\"%s\",\"sig\":\"%s\"}",
                eventId, clientPubkeyHex, createdAt, NIP46_KIND, tagsJson, escapeJson(content), signature
        );
    }

    private String sha256Hex(String input) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private String signMessage(String messageHex) {
        try {
            byte[] messageBytes = hexToBytes(messageHex);
            byte[] privateKeyBytes = clientIdentity.getPrivateKey().getRawData();
            // Generate auxiliary random data for BIP-340 Schnorr signing
            byte[] auxRand = new byte[32];
            new java.security.SecureRandom().nextBytes(auxRand);
            // Use static Schnorr.sign method
            byte[] signature = nostr.crypto.schnorr.Schnorr.sign(messageBytes, privateKeyBytes, auxRand);
            return bytesToHex(signature);
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign message", e);
        }
    }

    private static byte[] hexToBytes(String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            int index = i * 2;
            int value = Integer.parseInt(hex.substring(index, index + 2), 16);
            bytes[i] = (byte) value;
        }
        return bytes;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private Identity createIdentity(String privateKey) {
        String hexKey = resolvePrivateKeyToHex(privateKey);
        return Identity.create(hexKey);
    }

    private String resolvePrivateKeyToHex(String privateKey) {
        if (privateKey.startsWith("nsec1")) {
            try {
                return nostr.crypto.bech32.Bech32.fromBech32(privateKey);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid nsec: " + privateKey, e);
            }
        }
        return privateKey;
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

    /**
     * Listener for incoming NIP-46 response events.
     */
    private class ResponseListener implements xyz.tcheeric.nsecbunker.connection.RelayPoolListener {

        @Override
        public void onEvent(RelayConnection relay, String subscriptionId, GenericEvent event) {
            if (event.getKind() != NIP46_KIND) {
                return;
            }

            try {
                handleResponseEvent(event);
            } catch (Exception e) {
                log.warn("nip46_response_handling_failed event_id={} error={}",
                        event.getId(), e.getMessage());
            }
        }

        private void handleResponseEvent(GenericEvent event) {
            String content = event.getContent();
            if (content == null || content.isEmpty()) {
                return;
            }

            // Decrypt content
            String decrypted;
            try {
                decrypted = crypto.decrypt(content);
            } catch (Exception e) {
                log.debug("nip46_decrypt_failed, trying as plaintext: {}", e.getMessage());
                decrypted = content;
            }

            // Parse response
            decoder.tryDecodeResponse(decrypted).ifPresent(response -> {
                log.debug("nip46_response_received request_id={} result={}",
                        response.getId(), response.getResult() != null ? "present" : "error");

                // Complete pending request
                CompletableFuture<Nip46Response> future = pendingRequests.remove(response.getId());
                if (future != null) {
                    future.complete(response);
                } else {
                    log.debug("nip46_response_orphaned request_id={}", response.getId());
                }
            });
        }
    }
}
