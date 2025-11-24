package xyz.tcheeric.nsecbunker.connection;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nostr.event.impl.GenericEvent;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.tcheeric.nsecbunker.core.exception.BunkerConnectionException;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * WebSocket connection to a single Nostr relay.
 *
 * <p>This class manages the WebSocket lifecycle including connecting,
 * sending messages, receiving events, and handling disconnections.
 *
 * <p>Usage example:
 * <pre>{@code
 * RelayConnection relay = new RelayConnection("wss://relay.example.com");
 * relay.addListener(new RelayListener() {
 *     @Override
 *     public void onEvent(RelayConnection relay, String subId, GenericEvent event) {
 *         System.out.println("Received event: " + event.getId());
 *     }
 * });
 * relay.connect();
 * relay.send("[\"REQ\",\"sub1\",{\"kinds\":[1],\"limit\":10}]");
 * }</pre>
 */
@Slf4j
public class RelayConnection {

    private static final int NORMAL_CLOSURE = 1000;
    private static final int GOING_AWAY = 1001;

    /**
     * The relay URL (wss:// or ws://).
     */
    @Getter
    private final String url;

    /**
     * The OkHttp client used for WebSocket connections.
     */
    private final OkHttpClient client;

    /**
     * Object mapper for JSON parsing.
     */
    private final ObjectMapper objectMapper;

    /**
     * Connection timeout.
     */
    @Getter
    private final Duration connectTimeout;

    /**
     * The current connection state.
     */
    private final AtomicReference<ConnectionState> state;

    /**
     * The active WebSocket connection.
     */
    private volatile WebSocket webSocket;

    /**
     * Registered relay listeners.
     */
    private final List<RelayListener> listeners;

    /**
     * Registered connection listeners.
     */
    private final List<ConnectionListener> connectionListeners;

    /**
     * Latch for synchronous connect.
     */
    private volatile CountDownLatch connectLatch;

    /**
     * Error during connection.
     */
    private volatile Throwable connectError;

    /**
     * Creates a new RelayConnection with default settings.
     *
     * @param url the relay WebSocket URL
     */
    public RelayConnection(String url) {
        this(url, null, null);
    }

    /**
     * Creates a new RelayConnection with custom client.
     *
     * @param url    the relay WebSocket URL
     * @param client the OkHttp client (null for default)
     */
    public RelayConnection(String url, OkHttpClient client) {
        this(url, client, null);
    }

    /**
     * Creates a new RelayConnection with full configuration.
     *
     * @param url            the relay WebSocket URL
     * @param client         the OkHttp client (null for default)
     * @param connectTimeout connection timeout (null for default 30s)
     */
    public RelayConnection(String url, OkHttpClient client, Duration connectTimeout) {
        Objects.requireNonNull(url, "URL must not be null");
        if (!url.startsWith("wss://") && !url.startsWith("ws://")) {
            throw new IllegalArgumentException("URL must start with wss:// or ws://");
        }

        this.url = url;
        this.connectTimeout = connectTimeout != null ? connectTimeout : Duration.ofSeconds(30);
        this.client = client != null ? client : createDefaultClient();
        this.objectMapper = new ObjectMapper();
        this.state = new AtomicReference<>(ConnectionState.DISCONNECTED);
        this.listeners = new CopyOnWriteArrayList<>();
        this.connectionListeners = new CopyOnWriteArrayList<>();
    }

    /**
     * Returns the current connection state.
     *
     * @return the connection state
     */
    public ConnectionState getState() {
        return state.get();
    }

    /**
     * Checks if the connection is currently connected.
     *
     * @return true if connected
     */
    public boolean isConnected() {
        return state.get() == ConnectionState.CONNECTED;
    }

    /**
     * Adds a listener to receive connection events.
     *
     * @param listener the listener to add
     */
    public void addListener(RelayListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * Removes a listener.
     *
     * @param listener the listener to remove
     */
    public void removeListener(RelayListener listener) {
        listeners.remove(listener);
    }

    /**
     * Adds a connection listener to receive connection lifecycle events.
     *
     * @param listener the listener to add
     */
    public void addConnectionListener(ConnectionListener listener) {
        if (listener != null) {
            connectionListeners.add(listener);
        }
    }

    /**
     * Removes a connection listener.
     *
     * @param listener the listener to remove
     */
    public void removeConnectionListener(ConnectionListener listener) {
        connectionListeners.remove(listener);
    }

    /**
     * Connects to the relay synchronously.
     *
     * @throws BunkerConnectionException if connection fails
     */
    public void connect() throws BunkerConnectionException {
        connect(connectTimeout);
    }

    /**
     * Connects to the relay synchronously with custom timeout.
     *
     * @param timeout the connection timeout
     * @throws BunkerConnectionException if connection fails
     */
    public void connect(Duration timeout) throws BunkerConnectionException {
        ConnectionState currentState = state.get();
        if (currentState == ConnectionState.CONNECTED) {
            log.debug("Already connected to {}", url);
            return;
        }
        if (currentState.isTerminal()) {
            throw new BunkerConnectionException("Connection is in terminal state: " + currentState, url);
        }

        if (!state.compareAndSet(currentState, ConnectionState.CONNECTING)) {
            throw new BunkerConnectionException("Connection state changed during connect attempt", url);
        }

        connectLatch = new CountDownLatch(1);
        connectError = null;

        Request request = new Request.Builder()
                .url(url)
                .build();

        webSocket = client.newWebSocket(request, new RelayWebSocketListener());

        try {
            boolean connected = connectLatch.await(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!connected) {
                state.set(ConnectionState.FAILED);
                close();
                throw new BunkerConnectionException("Connection timed out after " + timeout.toMillis() + "ms", url);
            }
            if (connectError != null) {
                state.set(ConnectionState.FAILED);
                throw new BunkerConnectionException("Connection failed: " + connectError.getMessage(), url, connectError);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            state.set(ConnectionState.FAILED);
            throw new BunkerConnectionException("Connection interrupted", url, e);
        }
    }

    /**
     * Connects to the relay asynchronously.
     */
    public void connectAsync() {
        ConnectionState currentState = state.get();
        if (currentState == ConnectionState.CONNECTED || currentState.isTerminal()) {
            return;
        }

        if (!state.compareAndSet(currentState, ConnectionState.CONNECTING)) {
            return;
        }

        Request request = new Request.Builder()
                .url(url)
                .build();

        webSocket = client.newWebSocket(request, new RelayWebSocketListener());
    }

    /**
     * Sends a raw message to the relay.
     *
     * @param message the JSON message to send
     * @return true if the message was queued for sending
     * @throws IllegalStateException if not connected
     */
    public boolean send(String message) {
        WebSocket ws = webSocket;
        if (ws == null || state.get() != ConnectionState.CONNECTED) {
            throw new IllegalStateException("Not connected to relay");
        }

        log.debug("Sending to {}: {}", url, message);
        return ws.send(message);
    }

    /**
     * Sends a REQ message to subscribe to events.
     *
     * @param subscriptionId the subscription ID
     * @param filters        the filter JSON objects
     * @return true if the message was queued
     */
    public boolean sendReq(String subscriptionId, String... filters) {
        StringBuilder sb = new StringBuilder("[\"REQ\",\"").append(subscriptionId).append("\"");
        for (String filter : filters) {
            sb.append(",").append(filter);
        }
        sb.append("]");
        return send(sb.toString());
    }

    /**
     * Sends a CLOSE message to unsubscribe.
     *
     * @param subscriptionId the subscription ID to close
     * @return true if the message was queued
     */
    public boolean sendClose(String subscriptionId) {
        return send("[\"CLOSE\",\"" + subscriptionId + "\"]");
    }

    /**
     * Sends an EVENT message to publish an event.
     *
     * @param eventJson the event JSON
     * @return true if the message was queued
     */
    public boolean sendEvent(String eventJson) {
        return send("[\"EVENT\"," + eventJson + "]");
    }

    /**
     * Closes the connection gracefully.
     */
    public void close() {
        close(NORMAL_CLOSURE, "Client closing");
    }

    /**
     * Closes the connection with a specific code and reason.
     *
     * @param code   the close code
     * @param reason the close reason
     */
    public void close(int code, String reason) {
        ConnectionState currentState = state.get();
        if (currentState.isTerminal()) {
            return;
        }

        state.set(ConnectionState.DISCONNECTING);
        WebSocket ws = webSocket;
        if (ws != null) {
            try {
                ws.close(code, reason);
            } catch (Exception e) {
                log.debug("Error closing WebSocket: {}", e.getMessage());
            }
        }
        state.set(ConnectionState.CLOSED);
    }

    /**
     * Creates the default OkHttp client.
     */
    private OkHttpClient createDefaultClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(connectTimeout)
                .readTimeout(Duration.ofMinutes(5))
                .writeTimeout(Duration.ofSeconds(30))
                .pingInterval(Duration.ofSeconds(30))
                .build();
    }

    /**
     * Changes state and notifies listeners.
     */
    private void setState(ConnectionState newState) {
        ConnectionState oldState = state.getAndSet(newState);
        if (oldState != newState) {
            for (RelayListener listener : listeners) {
                try {
                    listener.onStateChange(this, oldState, newState);
                } catch (Exception e) {
                    log.warn("Error in listener onStateChange: {}", e.getMessage());
                }
            }
            // Notify connection listeners
            for (ConnectionListener listener : connectionListeners) {
                try {
                    listener.onStateChanged(url, oldState, newState);
                } catch (Exception e) {
                    log.warn("Error in ConnectionListener.onStateChanged: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * Notifies listeners of an error.
     */
    private void notifyError(Throwable t) {
        for (RelayListener listener : listeners) {
            try {
                listener.onError(this, t);
            } catch (Exception e) {
                log.warn("Error in listener onError: {}", e.getMessage());
            }
        }
        // Notify connection listeners
        for (ConnectionListener listener : connectionListeners) {
            try {
                listener.onError(url, t);
            } catch (Exception e) {
                log.warn("Error in ConnectionListener.onError: {}", e.getMessage());
            }
        }
    }

    /**
     * Parses and dispatches incoming messages.
     */
    private void handleMessage(String text) {
        // Notify raw message listeners
        for (RelayListener listener : listeners) {
            try {
                listener.onRawMessage(this, text);
            } catch (Exception e) {
                log.warn("Error in listener onRawMessage: {}", e.getMessage());
            }
        }

        try {
            JsonNode root = objectMapper.readTree(text);
            if (!root.isArray() || root.isEmpty()) {
                log.warn("Invalid message format from {}: {}", url, text);
                return;
            }

            String messageType = root.get(0).asText();
            switch (messageType) {
                case "EVENT":
                    handleEventMessage(root);
                    break;
                case "OK":
                    handleOkMessage(root);
                    break;
                case "EOSE":
                    handleEoseMessage(root);
                    break;
                case "NOTICE":
                    handleNoticeMessage(root);
                    break;
                case "CLOSED":
                    handleClosedMessage(root);
                    break;
                case "AUTH":
                    handleAuthMessage(root);
                    break;
                default:
                    log.debug("Unknown message type from {}: {}", url, messageType);
            }
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse message from {}: {}", url, e.getMessage());
        }
    }

    private void handleEventMessage(JsonNode root) {
        if (root.size() < 3) {
            return;
        }
        String subscriptionId = root.get(1).asText();
        JsonNode eventNode = root.get(2);

        try {
            GenericEvent event = objectMapper.treeToValue(eventNode, GenericEvent.class);
            for (RelayListener listener : listeners) {
                try {
                    listener.onEvent(this, subscriptionId, event);
                } catch (Exception e) {
                    log.warn("Error in listener onEvent: {}", e.getMessage());
                }
            }
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse event from {}: {}", url, e.getMessage());
        }
    }

    private void handleOkMessage(JsonNode root) {
        if (root.size() < 3) {
            return;
        }
        String eventId = root.get(1).asText();
        boolean success = root.get(2).asBoolean();
        String message = root.size() > 3 ? root.get(3).asText() : null;

        for (RelayListener listener : listeners) {
            try {
                listener.onOk(this, eventId, success, message);
            } catch (Exception e) {
                log.warn("Error in listener onOk: {}", e.getMessage());
            }
        }
    }

    private void handleEoseMessage(JsonNode root) {
        if (root.size() < 2) {
            return;
        }
        String subscriptionId = root.get(1).asText();

        for (RelayListener listener : listeners) {
            try {
                listener.onEndOfStoredEvents(this, subscriptionId);
            } catch (Exception e) {
                log.warn("Error in listener onEndOfStoredEvents: {}", e.getMessage());
            }
        }
    }

    private void handleNoticeMessage(JsonNode root) {
        if (root.size() < 2) {
            return;
        }
        String message = root.get(1).asText();

        for (RelayListener listener : listeners) {
            try {
                listener.onNotice(this, message);
            } catch (Exception e) {
                log.warn("Error in listener onNotice: {}", e.getMessage());
            }
        }
    }

    private void handleClosedMessage(JsonNode root) {
        if (root.size() < 3) {
            return;
        }
        String subscriptionId = root.get(1).asText();
        String message = root.get(2).asText();

        for (RelayListener listener : listeners) {
            try {
                listener.onClosed(this, subscriptionId, message);
            } catch (Exception e) {
                log.warn("Error in listener onClosed: {}", e.getMessage());
            }
        }
    }

    private void handleAuthMessage(JsonNode root) {
        if (root.size() < 2) {
            return;
        }
        String challenge = root.get(1).asText();

        for (RelayListener listener : listeners) {
            try {
                listener.onAuth(this, challenge);
            } catch (Exception e) {
                log.warn("Error in listener onAuth: {}", e.getMessage());
            }
        }
    }

    /**
     * Internal WebSocket listener.
     */
    private class RelayWebSocketListener extends WebSocketListener {

        @Override
        public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
            log.info("Connected to relay: {}", url);
            setState(ConnectionState.CONNECTED);

            for (RelayListener listener : listeners) {
                try {
                    listener.onConnect(RelayConnection.this);
                } catch (Exception e) {
                    log.warn("Error in listener onConnect: {}", e.getMessage());
                }
            }

            // Notify connection listeners
            for (ConnectionListener listener : connectionListeners) {
                try {
                    listener.onConnected(url);
                } catch (Exception e) {
                    log.warn("Error in ConnectionListener.onConnected: {}", e.getMessage());
                }
            }

            CountDownLatch latch = connectLatch;
            if (latch != null) {
                latch.countDown();
            }
        }

        @Override
        public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
            log.debug("Received from {}: {}", url, text);
            handleMessage(text);
        }

        @Override
        public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
            log.debug("Relay closing: {} - {} {}", url, code, reason);
            webSocket.close(code, reason);
        }

        @Override
        public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
            log.info("Disconnected from relay: {} - {} {}", url, code, reason);

            ConnectionState previousState = state.get();
            if (!previousState.isTerminal()) {
                setState(ConnectionState.DISCONNECTED);
            }

            for (RelayListener listener : listeners) {
                try {
                    listener.onDisconnect(RelayConnection.this, code, reason);
                } catch (Exception e) {
                    log.warn("Error in listener onDisconnect: {}", e.getMessage());
                }
            }

            // Notify connection listeners
            for (ConnectionListener listener : connectionListeners) {
                try {
                    listener.onDisconnected(url, code, reason);
                } catch (Exception e) {
                    log.warn("Error in ConnectionListener.onDisconnected: {}", e.getMessage());
                }
            }

            CountDownLatch latch = connectLatch;
            if (latch != null) {
                latch.countDown();
            }
        }

        @Override
        public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
            log.error("Connection failure to {}: {}", url, t.getMessage());

            connectError = t;
            setState(ConnectionState.FAILED);
            notifyError(t);

            CountDownLatch latch = connectLatch;
            if (latch != null) {
                latch.countDown();
            }
        }
    }

    @Override
    public String toString() {
        return "RelayConnection{url='" + url + "', state=" + state.get() + "}";
    }
}
