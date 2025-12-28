package xyz.tcheeric.nsecbunker.protocol.testing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import xyz.tcheeric.nsecbunker.connection.testing.MockRelayServer;
import xyz.tcheeric.nsecbunker.protocol.crypto.Nip04Crypto;
import xyz.tcheeric.nsecbunker.protocol.nip46.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * Mock NIP-46 bunker server for testing.
 *
 * <p>This class simulates a nsecBunker instance, allowing tests to verify
 * client behavior without connecting to a real bunker.
 *
 * <p>Features:
 * <ul>
 *   <li>Handles all NIP-46 methods (ping, connect, get_public_key, sign_event, etc.)</li>
 *   <li>Programmable responses for each method</li>
 *   <li>NIP-04 encrypted communication</li>
 *   <li>Request recording for verification</li>
 *   <li>Simulated delays and errors</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>{@code
 * try (MockBunkerServer bunker = new MockBunkerServer()) {
 *     bunker.start();
 *     String relayUrl = bunker.getRelayUrl();
 *
 *     // Configure responses
 *     bunker.setPublicKey("abc123...");
 *     bunker.onSignEvent(event -> "signed-event-json");
 *
 *     // Connect and test client
 *     // ...
 *
 *     // Verify requests
 *     List<Nip46Request> requests = bunker.getReceivedRequests();
 * }
 * }</pre>
 */
@Slf4j
public class MockBunkerServer implements AutoCloseable {

    private final MockRelayServer relay;
    private final ObjectMapper objectMapper;
    private final Nip46Encoder encoder;
    private final Nip46Decoder decoder;
    private final List<Nip46Request> receivedRequests;
    private final BlockingQueue<Nip46Request> requestQueue;
    private final Map<String, Function<Nip46Request, Nip46Response>> methodHandlers;

    @Getter
    @Setter
    private String bunkerPrivateKey;

    @Getter
    @Setter
    private String bunkerPublicKey;

    @Getter
    @Setter
    private String clientPublicKey;

    @Getter
    @Setter
    private long responseDelayMs;

    @Getter
    @Setter
    private boolean autoRespond = true;

    private volatile Nip04Crypto crypto;

    /**
     * Creates a new mock bunker server with generated keys.
     */
    public MockBunkerServer() {
        this.relay = new MockRelayServer();
        this.objectMapper = new ObjectMapper();
        this.encoder = new Nip46Encoder();
        this.decoder = new Nip46Decoder();
        this.receivedRequests = Collections.synchronizedList(new ArrayList<>());
        this.requestQueue = new LinkedBlockingQueue<>();
        this.methodHandlers = new ConcurrentHashMap<>();

        // Generate test keys (using well-known test values)
        this.bunkerPrivateKey = "0000000000000000000000000000000000000000000000000000000000000001";
        this.bunkerPublicKey = "79be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798";

        setupDefaultHandlers();
    }

    /**
     * Creates a mock bunker server with specific keys.
     *
     * @param bunkerPrivateKey the bunker's private key (hex)
     * @param bunkerPublicKey  the bunker's public key (hex)
     */
    public MockBunkerServer(String bunkerPrivateKey, String bunkerPublicKey) {
        this();
        this.bunkerPrivateKey = bunkerPrivateKey;
        this.bunkerPublicKey = bunkerPublicKey;
    }

    /**
     * Starts the mock bunker server.
     *
     * @throws IOException if the server cannot be started
     */
    public void start() throws IOException {
        relay.start();
        relay.enqueueWebSocketUpgrade();
        setupRelayHandlers();
        log.info("MockBunkerServer started, relay at {}", getRelayUrl());
    }

    /**
     * Gets the relay URL for connecting to this bunker.
     *
     * @return the WebSocket URL
     */
    public String getRelayUrl() {
        return relay.getUrl();
    }

    /**
     * Gets the bunker connection string.
     *
     * @return bunker://pubkey?relay=url format
     */
    public String getConnectionString() {
        return String.format("bunker://%s?relay=%s", bunkerPublicKey, getRelayUrl());
    }

    /**
     * Sets the client's public key for encrypted communication.
     *
     * @param clientPublicKey the client's public key (hex)
     */
    public void setClientPublicKey(String clientPublicKey) {
        this.clientPublicKey = clientPublicKey;
        if (clientPublicKey != null && bunkerPrivateKey != null) {
            this.crypto = Nip04Crypto.create(bunkerPrivateKey, clientPublicKey);
        }
    }

    /**
     * Accepts additional client connections.
     *
     * @param count the number of connections to accept
     */
    public void acceptConnections(int count) {
        relay.acceptConnections(count);
    }

    /**
     * Sets a custom handler for a NIP-46 method.
     *
     * @param method  the method name
     * @param handler function that receives the request and returns a response
     */
    public void onMethod(String method, Function<Nip46Request, Nip46Response> handler) {
        methodHandlers.put(method, handler);
    }

    /**
     * Sets a handler for ping requests.
     *
     * @param handler function that returns the pong result
     */
    public void onPing(Function<Nip46Request, String> handler) {
        onMethod("ping", req -> Nip46Response.success(req.getId(), handler.apply(req)));
    }

    /**
     * Sets a handler for connect requests.
     *
     * @param handler function that returns success (true) or rejection reason
     */
    public void onConnect(Function<Nip46Request, Boolean> handler) {
        onMethod("connect", req -> {
            if (handler.apply(req)) {
                return Nip46Response.ack(req.getId());
            } else {
                return Nip46Response.error(req.getId(), "UNAUTHORIZED", "Connection rejected");
            }
        });
    }

    /**
     * Sets a handler for get_public_key requests.
     *
     * @param handler function that returns the public key
     */
    public void onGetPublicKey(Function<Nip46Request, String> handler) {
        onMethod("get_public_key", req -> Nip46Response.success(req.getId(), handler.apply(req)));
    }

    /**
     * Sets a handler for sign_event requests.
     *
     * @param handler function that receives the event JSON and returns signed event JSON
     */
    public void onSignEvent(Function<String, String> handler) {
        onMethod("sign_event", req -> {
            String eventJson = req.getFirstParam();
            String signedEvent = handler.apply(eventJson);
            return Nip46Response.success(req.getId(), signedEvent);
        });
    }

    /**
     * Sets a handler for nip04_encrypt requests.
     *
     * @param handler function that receives (pubkey, plaintext) and returns ciphertext
     */
    public void onNip04Encrypt(Function<List<Object>, String> handler) {
        onMethod("nip04_encrypt", req -> {
            String result = handler.apply(req.getParams());
            return Nip46Response.success(req.getId(), result);
        });
    }

    /**
     * Sets a handler for nip04_decrypt requests.
     *
     * @param handler function that receives (pubkey, ciphertext) and returns plaintext
     */
    public void onNip04Decrypt(Function<List<Object>, String> handler) {
        onMethod("nip04_decrypt", req -> {
            String result = handler.apply(req.getParams());
            return Nip46Response.success(req.getId(), result);
        });
    }

    /**
     * Configures the bunker to reject a specific method with an error.
     *
     * @param method       the method to reject
     * @param errorCode    the error code
     * @param errorMessage the error message
     */
    public void rejectMethod(String method, String errorCode, String errorMessage) {
        onMethod(method, req -> Nip46Response.error(req.getId(), errorCode, errorMessage));
    }

    /**
     * Configures the bunker to require authorization for a method.
     *
     * @param method the method requiring auth
     */
    public void requireAuth(String method) {
        rejectMethod(method, "UNAUTHORIZED", "Authorization required");
    }

    /**
     * Sends a response directly (for manual testing).
     *
     * @param response the response to send
     */
    public void sendResponse(Nip46Response response) {
        String json = encoder.encodeResponse(response);
        String encrypted = encryptIfPossible(json);
        String eventJson = createEventJson(encrypted);
        relay.sendEvent("nip46", eventJson);
    }

    /**
     * Gets all received requests.
     *
     * @return list of requests
     */
    public List<Nip46Request> getReceivedRequests() {
        return new ArrayList<>(receivedRequests);
    }

    /**
     * Gets requests filtered by method.
     *
     * @param method the method name
     * @return list of matching requests
     */
    public List<Nip46Request> getRequestsByMethod(String method) {
        return receivedRequests.stream()
                .filter(r -> method.equals(r.getMethod()))
                .toList();
    }

    /**
     * Gets the count of received requests.
     *
     * @return the count
     */
    public int getReceivedRequestCount() {
        return receivedRequests.size();
    }

    /**
     * Clears all received requests.
     */
    public void clearReceivedRequests() {
        receivedRequests.clear();
    }

    /**
     * Waits for a request to be received.
     *
     * @param timeout the maximum time to wait
     * @param unit    the time unit
     * @return the received request, or null if timeout
     * @throws InterruptedException if interrupted
     */
    public Nip46Request awaitRequest(long timeout, TimeUnit unit) throws InterruptedException {
        return requestQueue.poll(timeout, unit);
    }

    /**
     * Waits for a specific number of requests.
     *
     * @param count   the expected count
     * @param timeout the maximum time to wait
     * @param unit    the time unit
     * @return true if the count was reached
     * @throws InterruptedException if interrupted
     */
    public boolean awaitRequestCount(int count, long timeout, TimeUnit unit) throws InterruptedException {
        long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
        while (receivedRequests.size() < count) {
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) {
                return false;
            }
            Thread.sleep(Math.min(10, remaining));
        }
        return true;
    }

    @Override
    public void close() throws IOException {
        relay.close();
        log.info("MockBunkerServer stopped");
    }

    private void setupDefaultHandlers() {
        // Default ping handler
        onMethod("ping", req -> Nip46Response.pong(req.getId()));

        // Default connect handler - always accept
        onMethod("connect", req -> Nip46Response.ack(req.getId()));

        // Default get_public_key handler
        onMethod("get_public_key", req -> Nip46Response.success(req.getId(), bunkerPublicKey));

        // Default sign_event handler - return the same event (not actually signing)
        onMethod("sign_event", req -> Nip46Response.success(req.getId(), req.getFirstParam()));

        // Default get_relays handler
        onMethod("get_relays", req -> Nip46Response.success(req.getId(), "{}"));

        // Default encryption handlers - pass through
        onMethod("nip04_encrypt", req -> Nip46Response.success(req.getId(), "encrypted"));
        onMethod("nip04_decrypt", req -> Nip46Response.success(req.getId(), "decrypted"));
        onMethod("nip44_encrypt", req -> Nip46Response.success(req.getId(), "encrypted"));
        onMethod("nip44_decrypt", req -> Nip46Response.success(req.getId(), "decrypted"));
    }

    private void setupRelayHandlers() {
        relay.onEvent(eventNode -> {
            try {
                handleIncomingEvent(eventNode);
            } catch (Exception e) {
                log.warn("Error handling event: {}", e.getMessage());
            }
            // Return OK for the relay event
            String eventId = eventNode.has("id") ? eventNode.get("id").asText() : "";
            return String.format("[\"OK\",\"%s\",true,\"\"]", eventId);
        });
    }

    private void handleIncomingEvent(JsonNode eventNode) {
        // Extract content (potentially encrypted)
        String content = eventNode.has("content") ? eventNode.get("content").asText() : "";

        // Try to decrypt if we have crypto setup
        String decrypted = decryptIfPossible(content);

        // Try to parse as NIP-46 request
        decoder.tryDecodeRequest(decrypted).ifPresent(request -> {
            log.debug("Received NIP-46 request: {} ({})", request.getId(), request.getMethod());
            receivedRequests.add(request);
            requestQueue.offer(request);

            if (autoRespond) {
                handleRequest(request);
            }
        });
    }

    private void handleRequest(Nip46Request request) {
        // Apply delay if configured
        if (responseDelayMs > 0) {
            try {
                Thread.sleep(responseDelayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        // Get handler for this method
        Function<Nip46Request, Nip46Response> handler = methodHandlers.get(request.getMethod());

        Nip46Response response;
        if (handler != null) {
            response = handler.apply(request);
        } else {
            // Unknown method
            response = Nip46Response.error(request.getId(), "UNSUPPORTED_METHOD",
                    "Method not supported: " + request.getMethod());
        }

        // Send response
        sendResponse(response);
    }

    private String encryptIfPossible(String content) {
        if (crypto != null) {
            try {
                return crypto.encrypt(content);
            } catch (Exception e) {
                log.warn("Failed to encrypt: {}", e.getMessage());
            }
        }
        return content;
    }

    private String decryptIfPossible(String content) {
        if (crypto != null && content.contains("?iv=")) {
            try {
                return crypto.decrypt(content);
            } catch (Exception e) {
                log.debug("Failed to decrypt, assuming plaintext: {}", e.getMessage());
            }
        }
        return content;
    }

    private String createEventJson(String content) {
        // Create a minimal NIP-46 response event
        return String.format("{\"kind\":24133,\"content\":\"%s\",\"pubkey\":\"%s\"}",
                escapeJson(content), bunkerPublicKey);
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
