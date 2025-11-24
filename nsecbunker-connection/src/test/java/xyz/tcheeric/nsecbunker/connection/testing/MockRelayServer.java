package xyz.tcheeric.nsecbunker.connection.testing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Mock Nostr relay server for testing.
 *
 * <p>This class provides a mock WebSocket server that simulates a Nostr relay,
 * allowing tests to run without connecting to real relays.
 *
 * <p>Features:
 * <ul>
 *   <li>WebSocket connections with automatic upgrade</li>
 *   <li>Message recording for verification</li>
 *   <li>Programmable responses</li>
 *   <li>Subscription handling</li>
 *   <li>Event broadcasting</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>{@code
 * try (MockRelayServer relay = new MockRelayServer()) {
 *     relay.start();
 *     String url = relay.getUrl();
 *
 *     // Configure responses
 *     relay.onEvent(event -> relay.sendOk(event));
 *
 *     // Connect and test
 *     RelayConnection connection = new RelayConnection(url);
 *     connection.connect();
 *
 *     // Verify
 *     List<String> received = relay.getReceivedMessages();
 * }
 * }</pre>
 */
@Slf4j
public class MockRelayServer implements AutoCloseable {

    private final MockWebServer server;
    private final ObjectMapper objectMapper;
    private final List<String> receivedMessages;
    private final List<MockWebSocket> connectedClients;
    private final Map<String, List<JsonNode>> subscriptions;
    private final BlockingQueue<String> messageQueue;

    private volatile Function<JsonNode, String> eventHandler;
    private volatile Function<JsonNode, List<String>> reqHandler;
    private volatile Consumer<String> closeHandler;
    private volatile Consumer<JsonNode> authHandler;

    @Getter
    private volatile boolean started;

    /**
     * Creates a new mock relay server.
     */
    public MockRelayServer() {
        this.server = new MockWebServer();
        this.objectMapper = new ObjectMapper();
        this.receivedMessages = Collections.synchronizedList(new ArrayList<>());
        this.connectedClients = Collections.synchronizedList(new ArrayList<>());
        this.subscriptions = new ConcurrentHashMap<>();
        this.messageQueue = new LinkedBlockingQueue<>();
    }

    /**
     * Starts the mock relay server.
     *
     * @throws IOException if the server cannot be started
     */
    public void start() throws IOException {
        server.start();
        started = true;
        log.info("MockRelayServer started on {}", getUrl());
    }

    /**
     * Starts the mock relay server on a specific port.
     *
     * @param port the port to listen on
     * @throws IOException if the server cannot be started
     */
    public void start(int port) throws IOException {
        server.start(port);
        started = true;
        log.info("MockRelayServer started on {}", getUrl());
    }

    /**
     * Gets the WebSocket URL for this relay.
     *
     * @return the WebSocket URL (ws://...)
     */
    public String getUrl() {
        return "ws://" + server.getHostName() + ":" + server.getPort();
    }

    /**
     * Gets the port this server is listening on.
     *
     * @return the port number
     */
    public int getPort() {
        return server.getPort();
    }

    /**
     * Enqueues a WebSocket upgrade response.
     * Call this before a client connects.
     */
    public void enqueueWebSocketUpgrade() {
        server.enqueue(new MockResponse().withWebSocketUpgrade(new RelayWebSocketListener()));
    }

    /**
     * Sets up the server to accept multiple connections.
     *
     * @param count the number of connections to accept
     */
    public void acceptConnections(int count) {
        for (int i = 0; i < count; i++) {
            enqueueWebSocketUpgrade();
        }
    }

    /**
     * Sets a handler for EVENT messages.
     *
     * @param handler function that receives the event and returns an OK message (or null)
     */
    public void onEvent(Function<JsonNode, String> handler) {
        this.eventHandler = handler;
    }

    /**
     * Sets a handler for REQ messages.
     *
     * @param handler function that receives the REQ and returns events to send
     */
    public void onReq(Function<JsonNode, List<String>> handler) {
        this.reqHandler = handler;
    }

    /**
     * Sets a handler for CLOSE messages.
     *
     * @param handler consumer that receives the subscription ID
     */
    public void onClose(Consumer<String> handler) {
        this.closeHandler = handler;
    }

    /**
     * Sets a handler for AUTH messages.
     *
     * @param handler consumer that receives the auth event
     */
    public void onAuth(Consumer<JsonNode> handler) {
        this.authHandler = handler;
    }

    /**
     * Sends a message to all connected clients.
     *
     * @param message the message to send
     */
    public void broadcast(String message) {
        synchronized (connectedClients) {
            for (MockWebSocket client : connectedClients) {
                client.send(message);
            }
        }
    }

    /**
     * Sends an OK message for an event.
     *
     * @param eventId the event ID
     * @param accepted whether the event was accepted
     * @param message optional message
     */
    public void sendOk(String eventId, boolean accepted, String message) {
        String ok = String.format("[\"OK\",\"%s\",%s,\"%s\"]",
                eventId, accepted, message != null ? message : "");
        broadcast(ok);
    }

    /**
     * Sends an EOSE (End of Stored Events) message.
     *
     * @param subscriptionId the subscription ID
     */
    public void sendEose(String subscriptionId) {
        broadcast(String.format("[\"EOSE\",\"%s\"]", subscriptionId));
    }

    /**
     * Sends a NOTICE message.
     *
     * @param message the notice message
     */
    public void sendNotice(String message) {
        broadcast(String.format("[\"NOTICE\",\"%s\"]", message));
    }

    /**
     * Sends an AUTH challenge.
     *
     * @param challenge the challenge string
     */
    public void sendAuthChallenge(String challenge) {
        broadcast(String.format("[\"AUTH\",\"%s\"]", challenge));
    }

    /**
     * Sends a CLOSED message.
     *
     * @param subscriptionId the subscription ID
     * @param message the reason message
     */
    public void sendClosed(String subscriptionId, String message) {
        broadcast(String.format("[\"CLOSED\",\"%s\",\"%s\"]", subscriptionId, message));
    }

    /**
     * Sends an EVENT message to subscribers.
     *
     * @param subscriptionId the subscription ID
     * @param event the event JSON
     */
    public void sendEvent(String subscriptionId, String event) {
        broadcast(String.format("[\"EVENT\",\"%s\",%s]", subscriptionId, event));
    }

    /**
     * Gets all received messages.
     *
     * @return list of received message strings
     */
    public List<String> getReceivedMessages() {
        return new ArrayList<>(receivedMessages);
    }

    /**
     * Gets the count of received messages.
     *
     * @return the message count
     */
    public int getReceivedMessageCount() {
        return receivedMessages.size();
    }

    /**
     * Clears all received messages.
     */
    public void clearReceivedMessages() {
        receivedMessages.clear();
    }

    /**
     * Waits for a message to be received.
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit
     * @return the received message, or null if timeout
     * @throws InterruptedException if interrupted while waiting
     */
    public String awaitMessage(long timeout, TimeUnit unit) throws InterruptedException {
        return messageQueue.poll(timeout, unit);
    }

    /**
     * Waits for a specific number of messages.
     *
     * @param count the number of messages to wait for
     * @param timeout the maximum time to wait
     * @param unit the time unit
     * @return true if the expected count was reached
     * @throws InterruptedException if interrupted while waiting
     */
    public boolean awaitMessageCount(int count, long timeout, TimeUnit unit) throws InterruptedException {
        long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
        while (receivedMessages.size() < count) {
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) {
                return false;
            }
            Thread.sleep(Math.min(10, remaining));
        }
        return true;
    }

    /**
     * Gets the number of connected clients.
     *
     * @return the connection count
     */
    public int getConnectionCount() {
        return connectedClients.size();
    }

    /**
     * Waits for a client to connect.
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit
     * @return true if a client connected
     * @throws InterruptedException if interrupted while waiting
     */
    public boolean awaitConnection(long timeout, TimeUnit unit) throws InterruptedException {
        long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
        while (connectedClients.isEmpty()) {
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) {
                return false;
            }
            Thread.sleep(Math.min(10, remaining));
        }
        return true;
    }

    /**
     * Gets the active subscriptions.
     *
     * @return map of subscription ID to filter list
     */
    public Map<String, List<JsonNode>> getSubscriptions() {
        return new HashMap<>(subscriptions);
    }

    /**
     * Disconnects all clients.
     */
    public void disconnectAll() {
        synchronized (connectedClients) {
            for (MockWebSocket client : new ArrayList<>(connectedClients)) {
                client.close(1000, "Server closing");
            }
        }
    }

    /**
     * Disconnects all clients with an error.
     *
     * @param code the WebSocket close code
     * @param reason the close reason
     */
    public void disconnectAllWithError(int code, String reason) {
        synchronized (connectedClients) {
            for (MockWebSocket client : new ArrayList<>(connectedClients)) {
                client.close(code, reason);
            }
        }
    }

    @Override
    public void close() throws IOException {
        disconnectAll();
        server.shutdown();
        started = false;
        log.info("MockRelayServer stopped");
    }

    private void handleMessage(MockWebSocket client, String message) {
        log.debug("Received: {}", message);
        receivedMessages.add(message);
        messageQueue.offer(message);

        try {
            JsonNode node = objectMapper.readTree(message);
            if (!node.isArray() || node.isEmpty()) {
                return;
            }

            String type = node.get(0).asText();
            switch (type) {
                case "EVENT" -> handleEventMessage(client, node);
                case "REQ" -> handleReqMessage(client, node);
                case "CLOSE" -> handleCloseMessage(client, node);
                case "AUTH" -> handleAuthMessage(client, node);
                default -> log.debug("Unknown message type: {}", type);
            }
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse message: {}", e.getMessage());
        }
    }

    private void handleEventMessage(MockWebSocket client, JsonNode node) {
        if (node.size() < 2) return;
        JsonNode event = node.get(1);

        if (eventHandler != null) {
            String response = eventHandler.apply(event);
            if (response != null) {
                client.send(response);
            }
        } else {
            // Default: accept all events
            String eventId = event.has("id") ? event.get("id").asText() : "";
            String ok = String.format("[\"OK\",\"%s\",true,\"\"]", eventId);
            client.send(ok);
        }
    }

    private void handleReqMessage(MockWebSocket client, JsonNode node) {
        if (node.size() < 2) return;
        String subId = node.get(1).asText();

        // Store filters
        List<JsonNode> filters = new ArrayList<>();
        for (int i = 2; i < node.size(); i++) {
            filters.add(node.get(i));
        }
        subscriptions.put(subId, filters);

        if (reqHandler != null) {
            List<String> events = reqHandler.apply(node);
            if (events != null) {
                for (String event : events) {
                    client.send(event);
                }
            }
        }

        // Send EOSE by default
        client.send(String.format("[\"EOSE\",\"%s\"]", subId));
    }

    private void handleCloseMessage(MockWebSocket client, JsonNode node) {
        if (node.size() < 2) return;
        String subId = node.get(1).asText();
        subscriptions.remove(subId);

        if (closeHandler != null) {
            closeHandler.accept(subId);
        }
    }

    private void handleAuthMessage(MockWebSocket client, JsonNode node) {
        if (node.size() < 2) return;
        JsonNode authEvent = node.get(1);

        if (authHandler != null) {
            authHandler.accept(authEvent);
        }
    }

    /**
     * Wrapper for WebSocket to track connections.
     */
    private static class MockWebSocket {
        private final WebSocket webSocket;

        MockWebSocket(WebSocket webSocket) {
            this.webSocket = webSocket;
        }

        void send(String message) {
            webSocket.send(message);
        }

        void close(int code, String reason) {
            webSocket.close(code, reason);
        }
    }

    /**
     * WebSocket listener for the mock relay.
     */
    private class RelayWebSocketListener extends WebSocketListener {
        private MockWebSocket mockWebSocket;

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            mockWebSocket = new MockWebSocket(webSocket);
            connectedClients.add(mockWebSocket);
            log.debug("Client connected, total: {}", connectedClients.size());
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            handleMessage(mockWebSocket, text);
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            webSocket.close(code, reason);
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            connectedClients.remove(mockWebSocket);
            log.debug("Client disconnected, total: {}", connectedClients.size());
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            connectedClients.remove(mockWebSocket);
            log.debug("Client connection failed: {}", t.getMessage());
        }
    }
}
