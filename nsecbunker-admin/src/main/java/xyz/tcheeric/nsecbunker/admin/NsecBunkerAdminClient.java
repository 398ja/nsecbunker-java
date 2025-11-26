package xyz.tcheeric.nsecbunker.admin;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nostr.id.Identity;
import xyz.tcheeric.nsecbunker.connection.ConnectionListener;
import xyz.tcheeric.nsecbunker.connection.ConnectionState;
import xyz.tcheeric.nsecbunker.connection.ExponentialBackoffStrategy;
import xyz.tcheeric.nsecbunker.connection.RelayConnection;
import xyz.tcheeric.nsecbunker.connection.RelayListener;
import xyz.tcheeric.nsecbunker.connection.RelayPool;
import xyz.tcheeric.nsecbunker.admin.key.DefaultKeyManager;
import xyz.tcheeric.nsecbunker.admin.key.KeyManager;
import xyz.tcheeric.nsecbunker.admin.policy.DefaultPolicyManager;
import xyz.tcheeric.nsecbunker.admin.policy.PolicyManager;
import xyz.tcheeric.nsecbunker.admin.permission.DefaultPermissionManager;
import xyz.tcheeric.nsecbunker.admin.permission.PermissionManager;
import xyz.tcheeric.nsecbunker.admin.token.DefaultTokenManager;
import xyz.tcheeric.nsecbunker.admin.token.TokenManager;
import xyz.tcheeric.nsecbunker.core.exception.BunkerConnectionException;
import xyz.tcheeric.nsecbunker.protocol.crypto.Nip04Crypto;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Decoder;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Encoder;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Request;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Response;
import xyz.tcheeric.nsecbunker.protocol.nip46.PendingRequestManager;

import java.io.Closeable;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Admin client for interacting with nsecBunker instances.
 *
 * <p>This client provides administrative operations for managing keys, users,
 * policies, and tokens in an nsecBunker instance.
 *
 * <p>Usage:
 * <pre>{@code
 * NsecBunkerAdminClient client = NsecBunkerAdminClient.builder()
 *     .bunkerPubkey("npub1...")
 *     .adminPrivateKey("nsec1...")
 *     .relay("wss://relay.example.com")
 *     .build();
 *
 * client.connect();
 *
 * // Perform admin operations
 * List<BunkerKey> keys = client.listKeys();
 *
 * client.close();
 * }</pre>
 */
@Slf4j
public class NsecBunkerAdminClient implements Closeable {

    /**
     * Event kind for NIP-46 admin requests.
     */
    public static final int KIND_ADMIN_REQUEST = 24133;

    /**
     * Event kind for NIP-46 responses.
     */
    public static final int KIND_RESPONSE = 24133;

    /**
     * The admin client configuration.
     */
    @Getter
    private final AdminConfig config;

    /**
     * The admin identity (for signing events).
     */
    private final Identity adminIdentity;

    /**
     * The ephemeral identity used for communication (if configured).
     */
    private final Identity ephemeralIdentity;

    /**
     * The identity actually used for communication.
     */
    @Getter
    private final Identity communicationIdentity;

    /**
     * The bunker's public key in hex format.
     */
    @Getter
    private final String bunkerPubkeyHex;

    /**
     * NIP-04 crypto for encrypting/decrypting messages.
     */
    private final Nip04Crypto crypto;

    /**
     * NIP-46 encoder for requests.
     */
    private final Nip46Encoder encoder;

    /**
     * NIP-46 decoder for responses.
     */
    private final Nip46Decoder decoder;

    /**
     * The relay pool for connecting to multiple relays.
     */
    private RelayPool relayPool;

    /**
     * Manager for pending requests.
     */
    private final PendingRequestManager pendingRequests;

    /**
     * Admin event listeners.
     */
    private final List<AdminEventListener> eventListeners;

    /**
     * Current connection state.
     */
    private final AtomicReference<ConnectionState> connectionState;

    /**
     * Whether the client has been closed.
     */
    private final AtomicBoolean closed;

    /**
     * The current subscription ID for receiving responses.
     */
    private volatile String subscriptionId;

    /**
     * Creates a new admin client with the given configuration.
     *
     * @param config the admin configuration
     */
    private NsecBunkerAdminClient(AdminConfig config) {
        this.config = Objects.requireNonNull(config, "Config must not be null");
        config.validate();

        // Create admin identity
        this.adminIdentity = createIdentity(config.getAdminPrivateKey());

        // Create ephemeral identity if configured
        if (config.isUseEphemeralKey()) {
            this.ephemeralIdentity = Identity.generateRandomIdentity();
            this.communicationIdentity = ephemeralIdentity;
            log.debug("Using ephemeral key for communication: {}",
                    ephemeralIdentity.getPublicKey().toString());
        } else {
            this.ephemeralIdentity = null;
            this.communicationIdentity = adminIdentity;
            log.debug("Using admin key directly for communication");
        }

        // Resolve bunker pubkey to hex
        this.bunkerPubkeyHex = resolvePubkeyToHex(config.getBunkerPubkey());

        // Create NIP-04 crypto
        String privateKeyHex = communicationIdentity.getPrivateKey().toHexString();
        this.crypto = Nip04Crypto.create(privateKeyHex, bunkerPubkeyHex);

        // Create encoder/decoder
        this.encoder = new Nip46Encoder();
        this.decoder = new Nip46Decoder();

        // Create pending request manager
        this.pendingRequests = new PendingRequestManager();

        // Initialize state
        this.eventListeners = new CopyOnWriteArrayList<>();
        this.connectionState = new AtomicReference<>(ConnectionState.DISCONNECTED);
        this.closed = new AtomicBoolean(false);
    }

    /**
     * Creates a new builder for the admin client.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Connects to the bunker relays.
     *
     * @throws BunkerConnectionException if connection fails
     */
    public void connect() throws BunkerConnectionException {
        if (closed.get()) {
            throw new BunkerConnectionException("Client has been closed");
        }

        if (connectionState.get() == ConnectionState.CONNECTED) {
            log.debug("Already connected");
            return;
        }

        connectionState.set(ConnectionState.CONNECTING);
        log.info("Connecting to bunker at {} via {} relays",
                bunkerPubkeyHex, config.getRelays().size());

        try {
            // Create relay pool
            RelayPool.Builder poolBuilder = RelayPool.builder()
                    .connectTimeout(config.getConnectTimeout())
                    .minConnectedRelays(1)
                    .deduplicateEvents(true);

            for (String relayUrl : config.getRelays()) {
                poolBuilder.relay(relayUrl);
            }

            this.relayPool = poolBuilder.build();

            // Add listener to each relay in the pool
            AdminRelayListener relayListener = new AdminRelayListener();
            AdminConnectionListener connectionListener = new AdminConnectionListener();
            for (RelayConnection relay : relayPool.getRelays()) {
                relay.addListener(relayListener);
                relay.addConnectionListener(connectionListener);

                // Configure reconnection if enabled
                if (config.isAutoReconnect()) {
                    relay.setReconnectionStrategy(ExponentialBackoffStrategy.builder()
                            .maxAttempts(config.getMaxReconnectAttempts())
                            .initialDelay(config.getReconnectDelay())
                            .maxDelay(config.getMaxReconnectDelay())
                            .build());
                }
            }

            // Connect to all relays
            relayPool.connectAll(config.getConnectTimeout());

            // Subscribe to responses
            subscribeToResponses();

            connectionState.set(ConnectionState.CONNECTED);
            log.info("Connected to bunker successfully");

        } catch (Exception e) {
            connectionState.set(ConnectionState.FAILED);
            throw new BunkerConnectionException("Failed to connect to bunker: " + e.getMessage(),
                    config.getRelays().isEmpty() ? null : config.getRelays().get(0), e);
        }
    }

    /**
     * Connects to the bunker asynchronously.
     *
     * @return a future that completes when connected
     */
    public CompletableFuture<Void> connectAsync() {
        return CompletableFuture.runAsync(() -> {
            try {
                connect();
            } catch (BunkerConnectionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Sends a request to the bunker and waits for a response.
     *
     * @param request the request to send
     * @return a future that completes with the response
     */
    public CompletableFuture<Nip46Response> sendRequest(Nip46Request request) {
        if (closed.get()) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Client has been closed"));
        }

        if (connectionState.get() != ConnectionState.CONNECTED) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("Not connected to bunker"));
        }

        // Register the request
        Duration timeout = config.getRequestTimeout();
        CompletableFuture<Nip46Response> future = pendingRequests.register(request, timeout);

        try {
            // Encode and encrypt the request
            String requestJson = encoder.encodeRequest(request);
            String encryptedContent = crypto.encrypt(requestJson);

            // Create the event JSON
            String eventJson = createAdminEventJson(encryptedContent);

            // Send via relay pool
            int sent = relayPool.broadcastEvent(eventJson);
            if (sent == 0) {
                pendingRequests.cancel(request.getId());
                return CompletableFuture.failedFuture(
                        new RuntimeException("Failed to send request to any relay"));
            }

            log.debug("Sent admin request: method={}, id={}", request.getMethod(), request.getId());

        } catch (Exception e) {
            pendingRequests.cancel(request.getId());
            return CompletableFuture.failedFuture(e);
        }

        return future;
    }

    /**
     * Pings the bunker to check connectivity.
     *
     * @return a future that completes with "pong" on success
     */
    public CompletableFuture<String> ping() {
        Nip46Request request = Nip46Request.ping();
        return sendRequest(request).thenApply(response -> {
            if (response.isError()) {
                throw new RuntimeException("Ping failed: " + response.getError());
            }
            return response.getResult();
        });
    }

    /**
     * Gets the current connection state.
     *
     * @return the connection state
     */
    public ConnectionState getConnectionState() {
        return connectionState.get();
    }

    /**
     * Checks if the client is connected.
     *
     * @return true if connected
     */
    public boolean isConnected() {
        return connectionState.get() == ConnectionState.CONNECTED;
    }

    /**
     * Provides access to key management operations backed by this client.
     *
     * @return a {@link KeyManager} instance using this admin client
     */
    public KeyManager keyManager() {
        return new DefaultKeyManager(this);
    }

    /**
     * Provides access to policy management operations backed by this client.
     *
     * @return a {@link PolicyManager} instance using this admin client
     */
    public PolicyManager policyManager() {
        return new DefaultPolicyManager(this);
    }

    /**
     * Provides access to permission management operations backed by this client.
     *
     * @return a {@link PermissionManager} instance using this admin client
     */
    public PermissionManager permissionManager() {
        return new DefaultPermissionManager(this);
    }

    /**
     * Provides access to token management operations backed by this client.
     *
     * @return a {@link TokenManager} instance using this admin client
     */
    public TokenManager tokenManager() {
        return new DefaultTokenManager(this);
    }

    /**
     * Adds an event listener.
     *
     * @param listener the listener to add
     */
    public void addEventListener(AdminEventListener listener) {
        if (listener != null) {
            eventListeners.add(listener);
        }
    }

    /**
     * Removes an event listener.
     *
     * @param listener the listener to remove
     */
    public void removeEventListener(AdminEventListener listener) {
        eventListeners.remove(listener);
    }

    /**
     * Disconnects from the bunker.
     */
    public void disconnect() {
        if (connectionState.compareAndSet(ConnectionState.CONNECTED, ConnectionState.DISCONNECTING)) {
            log.info("Disconnecting from bunker");

            // Cancel all pending requests
            pendingRequests.cancelAll();

            // Close relay pool
            relayPool.close();

            connectionState.set(ConnectionState.DISCONNECTED);
            log.info("Disconnected from bunker");
        }
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            disconnect();
            pendingRequests.shutdown();
        }
    }

    // ========== Private Methods ==========

    /**
     * Creates an Identity from a private key (hex or nsec format).
     */
    private Identity createIdentity(String privateKey) {
        String hexKey = privateKey;
        if (privateKey.startsWith("nsec1")) {
            // Decode bech32 nsec to hex
            try {
                hexKey = nostr.crypto.bech32.Bech32.fromBech32(privateKey);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid nsec: " + privateKey, e);
            }
        }
        return Identity.create(hexKey);
    }

    /**
     * Resolves a public key to hex format.
     */
    private String resolvePubkeyToHex(String pubkey) {
        if (pubkey.startsWith("npub1")) {
            // Decode bech32 to hex
            try {
                return nostr.crypto.bech32.Bech32.fromBech32(pubkey);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid npub: " + pubkey, e);
            }
        }
        // Assume hex format
        return pubkey;
    }

    /**
     * Subscribes to response events from the bunker.
     */
    private void subscribeToResponses() {
        String commPubkeyHex = communicationIdentity.getPublicKey().toString();

        // Create filter for responses addressed to our communication pubkey
        String filter = String.format(
                "{\"kinds\":[%d],\"#p\":[\"%s\"],\"authors\":[\"%s\"]}",
                KIND_RESPONSE, commPubkeyHex, bunkerPubkeyHex);

        subscriptionId = "admin-" + System.currentTimeMillis();
        relayPool.broadcastReq(subscriptionId, filter);

        log.debug("Subscribed for responses with filter: {}", filter);
    }

    /**
     * Creates the admin event JSON.
     */
    private String createAdminEventJson(String encryptedContent) {
        String commPubkeyHex = communicationIdentity.getPublicKey().toString();
        long createdAt = System.currentTimeMillis() / 1000;

        // Create event to sign
        nostr.event.impl.GenericEvent event = new nostr.event.impl.GenericEvent(
                communicationIdentity.getPublicKey(),
                KIND_ADMIN_REQUEST
        );
        event.setContent(encryptedContent);
        event.setCreatedAt(createdAt);

        // Add p tag for recipient
        nostr.event.tag.PubKeyTag pTag = new nostr.event.tag.PubKeyTag(
                new nostr.base.PublicKey(bunkerPubkeyHex));
        event.addTag(pTag);

        // Update and sign
        event.update();
        communicationIdentity.sign(event);

        // Serialize to JSON
        return serializeEvent(event);
    }

    /**
     * Serializes an event to JSON string.
     */
    private String serializeEvent(nostr.event.impl.GenericEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"id\":\"").append(event.getId()).append("\",");
        sb.append("\"pubkey\":\"").append(event.getPubKey().toString()).append("\",");
        sb.append("\"created_at\":").append(event.getCreatedAt()).append(",");
        sb.append("\"kind\":").append(event.getKind()).append(",");
        sb.append("\"tags\":[");

        List<nostr.event.BaseTag> tags = event.getTags();
        for (int i = 0; i < tags.size(); i++) {
            if (i > 0) sb.append(",");
            nostr.event.BaseTag tag = tags.get(i);
            sb.append("[\"").append(tag.getCode()).append("\"");
            // Handle p tag
            if (tag instanceof nostr.event.tag.PubKeyTag pt) {
                sb.append(",\"").append(pt.getPublicKey().toString()).append("\"");
            }
            sb.append("]");
        }

        sb.append("],");
        sb.append("\"content\":\"").append(escapeJson(event.getContent())).append("\",");
        sb.append("\"sig\":\"").append(event.getSignature().toString()).append("\"");
        sb.append("}");

        return sb.toString();
    }

    /**
     * Escapes special characters in JSON strings.
     */
    private String escapeJson(String value) {
        if (value == null) return "";
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Handles an incoming event.
     */
    private void handleEvent(nostr.event.impl.GenericEvent event) {
        // Check if this is a response to our request
        if (event.getKind() != KIND_RESPONSE) {
            return;
        }

        // Check if it's from the bunker
        if (!event.getPubKey().toString().equals(bunkerPubkeyHex)) {
            log.debug("Ignoring event from unknown pubkey: {}", event.getPubKey());
            return;
        }

        try {
            // Decrypt the content
            String decrypted = crypto.decrypt(event.getContent());
            log.debug("Decrypted response: {}", decrypted);

            // Decode the response
            Nip46Response response = decoder.decodeResponse(decrypted);

            // Complete the pending request
            if (pendingRequests.complete(response)) {
                log.debug("Completed request: id={}", response.getId());
            } else {
                log.debug("No pending request for response: id={}", response.getId());
            }

            // Notify listeners
            for (AdminEventListener listener : eventListeners) {
                try {
                    listener.onResponse(this, response);
                } catch (Exception e) {
                    log.warn("Error in event listener: {}", e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Failed to handle response event: {}", e.getMessage());
        }
    }

    /**
     * Converts bytes to hex string.
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // ========== Inner Classes ==========

    /**
     * Relay listener for handling incoming events.
     */
    private class AdminRelayListener implements RelayListener {
        @Override
        public void onEvent(RelayConnection relay, String subId, nostr.event.impl.GenericEvent event) {
            handleEvent(event);
        }

        @Override
        public void onOk(RelayConnection relay, String eventId, boolean success, String message) {
            log.debug("OK from {}: eventId={}, success={}, message={}",
                    relay.getUrl(), eventId, success, message);
        }

        @Override
        public void onNotice(RelayConnection relay, String message) {
            log.info("Notice from {}: {}", relay.getUrl(), message);
        }
    }

    /**
     * Connection listener for handling connection state changes.
     */
    private class AdminConnectionListener implements ConnectionListener {
        @Override
        public void onConnected(String relayUrl) {
            log.debug("Connected to relay: {}", relayUrl);
        }

        @Override
        public void onDisconnected(String relayUrl, int code, String reason) {
            log.debug("Disconnected from relay: {} (code={}, reason={})", relayUrl, code, reason);
        }

        @Override
        public void onError(String relayUrl, Throwable error) {
            log.error("Error from relay {}: {}", relayUrl, error.getMessage());
        }

        @Override
        public void onReconnecting(String relayUrl, int attempt) {
            log.info("Reconnecting to relay {} (attempt {})", relayUrl, attempt);
        }
    }

    /**
     * Builder for NsecBunkerAdminClient.
     */
    public static class Builder {
        private final AdminConfig.AdminConfigBuilder configBuilder = AdminConfig.builder();

        public Builder bunkerPubkey(String bunkerPubkey) {
            configBuilder.bunkerPubkey(bunkerPubkey);
            return this;
        }

        public Builder adminPrivateKey(String adminPrivateKey) {
            configBuilder.adminPrivateKey(adminPrivateKey);
            return this;
        }

        public Builder relay(String relay) {
            return relays(List.of(relay));
        }

        public Builder relays(List<String> relays) {
            configBuilder.relays(relays);
            return this;
        }

        public Builder secret(String secret) {
            configBuilder.secret(secret);
            return this;
        }

        public Builder connectTimeout(Duration connectTimeout) {
            configBuilder.connectTimeout(connectTimeout);
            return this;
        }

        public Builder requestTimeout(Duration requestTimeout) {
            configBuilder.requestTimeout(requestTimeout);
            return this;
        }

        public Builder autoReconnect(boolean autoReconnect) {
            configBuilder.autoReconnect(autoReconnect);
            return this;
        }

        public Builder maxReconnectAttempts(int maxReconnectAttempts) {
            configBuilder.maxReconnectAttempts(maxReconnectAttempts);
            return this;
        }

        public Builder reconnectDelay(Duration reconnectDelay) {
            configBuilder.reconnectDelay(reconnectDelay);
            return this;
        }

        public Builder maxReconnectDelay(Duration maxReconnectDelay) {
            configBuilder.maxReconnectDelay(maxReconnectDelay);
            return this;
        }

        public Builder useEphemeralKey(boolean useEphemeralKey) {
            configBuilder.useEphemeralKey(useEphemeralKey);
            return this;
        }

        public Builder config(AdminConfig config) {
            return bunkerPubkey(config.getBunkerPubkey())
                    .adminPrivateKey(config.getAdminPrivateKey())
                    .relays(config.getRelays())
                    .secret(config.getSecret())
                    .connectTimeout(config.getConnectTimeout())
                    .requestTimeout(config.getRequestTimeout())
                    .autoReconnect(config.isAutoReconnect())
                    .maxReconnectAttempts(config.getMaxReconnectAttempts())
                    .reconnectDelay(config.getReconnectDelay())
                    .maxReconnectDelay(config.getMaxReconnectDelay())
                    .useEphemeralKey(config.isUseEphemeralKey());
        }

        /**
         * Creates a builder from a bunker connection string.
         *
         * @param connectionString the bunker:// connection string
         * @param adminPrivateKey  the admin's private key
         * @return this builder
         */
        public Builder fromConnectionString(String connectionString, String adminPrivateKey) {
            AdminConfig config = AdminConfig.fromConnectionString(connectionString, adminPrivateKey);
            return config(config);
        }

        public NsecBunkerAdminClient build() {
            return new NsecBunkerAdminClient(configBuilder.build());
        }
    }
}
