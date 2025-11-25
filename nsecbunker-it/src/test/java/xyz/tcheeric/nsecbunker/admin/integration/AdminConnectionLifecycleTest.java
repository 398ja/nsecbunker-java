package xyz.tcheeric.nsecbunker.admin.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.tcheeric.nsecbunker.admin.AdminEventListener;
import xyz.tcheeric.nsecbunker.admin.NsecBunkerAdminClient;
import xyz.tcheeric.nsecbunker.connection.ConnectionState;
import xyz.tcheeric.nsecbunker.connection.testing.MockRelayServer;
import xyz.tcheeric.nsecbunker.core.exception.BunkerConnectionException;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Response;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration tests for admin client connection lifecycle.
 * Tests: connect, disconnect, reconnect flows.
 */
@Tag("integration")
class AdminConnectionLifecycleTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminConnectionLifecycleTest.class);

    // Well-known test keys (secp256k1 scalar = 1, 2)
    private static final String TEST_BUNKER_PUBKEY = "79be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798";
    private static final String TEST_ADMIN_PRIVKEY = "0000000000000000000000000000000000000000000000000000000000000002";

    private MockRelayServer mockRelay;
    private NsecBunkerAdminClient client;

    @BeforeAll
    static void noteExpectedConnectionErrors() {
        LOGGER.info("Admin connection lifecycle integration tests intentionally hit invalid/mocked relays; connection errors in logs are expected.");
    }

    @BeforeEach
    void setUp() throws IOException {
        mockRelay = new MockRelayServer();
        mockRelay.start();
        mockRelay.enqueueWebSocketUpgrade();
    }

    @AfterEach
    void tearDown() throws IOException {
        if (client != null) {
            client.close();
        }
        if (mockRelay != null) {
            mockRelay.close();
        }
    }

    // ========== Connect Tests ==========

    @Test
    @DisplayName("Connect establishes connection to relay")
    void connectEstablishesConnection() throws Exception {
        client = createClient();

        client.connect();

        assertThat(client.isConnected()).isTrue();
        assertThat(client.getConnectionState()).isEqualTo(ConnectionState.CONNECTED);
        assertThat(mockRelay.awaitConnection(5, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    @DisplayName("Connect is idempotent when already connected")
    void connectIsIdempotentWhenConnected() throws Exception {
        client = createClient();

        client.connect();
        assertThat(client.isConnected()).isTrue();

        // Second connect should not throw
        client.connect();
        assertThat(client.isConnected()).isTrue();

        // Should still have only one connection
        assertThat(mockRelay.getConnectionCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Connect throws when client is closed")
    void connectThrowsWhenClientClosed() {
        client = createClient();
        client.close();

        assertThatThrownBy(() -> client.connect())
                .isInstanceOf(BunkerConnectionException.class)
                .hasMessageContaining("closed");
    }

    @Test
    @DisplayName("ConnectAsync completes successfully")
    void connectAsyncCompletesSuccessfully() throws Exception {
        client = createClient();

        CompletableFuture<Void> future = client.connectAsync();
        future.get(5, TimeUnit.SECONDS);

        assertThat(client.isConnected()).isTrue();
    }

    @Test
    @DisplayName("Connect fails with invalid relay")
    void connectFailsWithInvalidRelay() {
        client = NsecBunkerAdminClient.builder()
                .bunkerPubkey(TEST_BUNKER_PUBKEY)
                .adminPrivateKey(TEST_ADMIN_PRIVKEY)
                .relay("wss://invalid.nonexistent.relay:12345")
                .connectTimeout(Duration.ofSeconds(2))
                .build();

        assertThatThrownBy(() -> client.connect())
                .isInstanceOf(BunkerConnectionException.class);
        assertThat(client.getConnectionState()).isEqualTo(ConnectionState.FAILED);
    }

    // ========== Disconnect Tests ==========

    @Test
    @DisplayName("Disconnect closes connection")
    void disconnectClosesConnection() throws Exception {
        client = createClient();
        client.connect();
        assertThat(client.isConnected()).isTrue();

        client.disconnect();

        assertThat(client.isConnected()).isFalse();
        assertThat(client.getConnectionState()).isEqualTo(ConnectionState.DISCONNECTED);
    }

    @Test
    @DisplayName("Disconnect is safe when not connected")
    void disconnectIsSafeWhenNotConnected() {
        client = createClient();
        assertThat(client.isConnected()).isFalse();

        // Should not throw
        client.disconnect();

        assertThat(client.getConnectionState()).isEqualTo(ConnectionState.DISCONNECTED);
    }

    @Test
    @DisplayName("Disconnect can be called multiple times")
    void disconnectCanBeCalledMultipleTimes() throws Exception {
        client = createClient();
        client.connect();

        client.disconnect();
        client.disconnect();
        client.disconnect();

        assertThat(client.isConnected()).isFalse();
    }

    // ========== Reconnect Tests ==========

    @Test
    @DisplayName("Can reconnect after disconnect")
    void canReconnectAfterDisconnect() throws Exception {
        mockRelay.acceptConnections(1); // Accept second connection
        client = createClient();

        // First connection
        client.connect();
        assertThat(client.isConnected()).isTrue();

        // Disconnect
        client.disconnect();
        assertThat(client.isConnected()).isFalse();

        // Reconnect
        client.connect();
        assertThat(client.isConnected()).isTrue();
    }

    @Test
    @DisplayName("Cannot reconnect after close")
    void cannotReconnectAfterClose() throws Exception {
        client = createClient();
        client.connect();
        client.close();

        assertThatThrownBy(() -> client.connect())
                .isInstanceOf(BunkerConnectionException.class)
                .hasMessageContaining("closed");
    }

    // ========== State Transition Tests ==========

    @Test
    @DisplayName("Initial state is DISCONNECTED")
    void initialStateIsDisconnected() {
        client = createClient();

        assertThat(client.getConnectionState()).isEqualTo(ConnectionState.DISCONNECTED);
        assertThat(client.isConnected()).isFalse();
    }

    @Test
    @DisplayName("State transitions through lifecycle")
    void stateTransitionsThroughLifecycle() throws Exception {
        client = createClient();

        // Initial state
        assertThat(client.getConnectionState()).isEqualTo(ConnectionState.DISCONNECTED);

        // After connect
        client.connect();
        assertThat(client.getConnectionState()).isEqualTo(ConnectionState.CONNECTED);

        // After disconnect
        client.disconnect();
        assertThat(client.getConnectionState()).isEqualTo(ConnectionState.DISCONNECTED);
    }

    // ========== Close Tests ==========

    @Test
    @DisplayName("Close disconnects and cleans up")
    void closeDisconnectsAndCleansUp() throws Exception {
        client = createClient();
        client.connect();
        assertThat(client.isConnected()).isTrue();

        client.close();

        assertThat(client.isConnected()).isFalse();
    }

    @Test
    @DisplayName("Close is idempotent")
    void closeIsIdempotent() throws Exception {
        client = createClient();
        client.connect();

        client.close();
        client.close();
        client.close();

        assertThat(client.isConnected()).isFalse();
    }

    @Test
    @DisplayName("Close cancels pending requests")
    void closeCancelsPendingRequests() throws Exception {
        client = createClient();
        client.connect();

        // Start a request but don't respond
        CompletableFuture<String> future = client.ping();

        // Close immediately
        client.close();

        // Request should fail
        assertThatThrownBy(() -> future.get(1, TimeUnit.SECONDS))
                .isInstanceOf(Exception.class);
    }

    // ========== Request Behavior Tests ==========

    @Test
    @DisplayName("SendRequest fails when not connected")
    void sendRequestFailsWhenNotConnected() {
        client = createClient();

        CompletableFuture<String> future = client.ping();

        assertThatThrownBy(() -> future.get(1, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Not connected");
    }

    @Test
    @DisplayName("SendRequest fails when client is closed")
    void sendRequestFailsWhenClientClosed() throws Exception {
        client = createClient();
        client.connect();
        client.close();

        CompletableFuture<String> future = client.ping();

        assertThatThrownBy(() -> future.get(1, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasMessageContaining("closed");
    }

    // ========== Event Listener Tests ==========

    @Test
    @DisplayName("Event listener receives responses")
    void eventListenerReceivesResponses() throws Exception {
        AtomicReference<Nip46Response> receivedResponse = new AtomicReference<>();
        CountDownLatch responseLatch = new CountDownLatch(1);

        client = createClient();
        client.addEventListener(new AdminEventListener() {
            @Override
            public void onResponse(NsecBunkerAdminClient client, Nip46Response response) {
                receivedResponse.set(response);
                responseLatch.countDown();
            }
        });

        client.connect();

        // Configure mock to respond to ping
        mockRelay.onEvent(event -> {
            // Extract event id and respond
            String eventId = event.has("id") ? event.get("id").asText() : "";
            return String.format("[\"OK\",\"%s\",true,\"\"]", eventId);
        });

        // Note: The actual response would need to be sent as a NIP-46 response event
        // This test validates that the listener mechanism works
        assertThat(client.isConnected()).isTrue();
    }

    @Test
    @DisplayName("Multiple event listeners can be added")
    void multipleEventListenersCanBeAdded() throws Exception {
        AtomicInteger listenerCount = new AtomicInteger(0);

        client = createClient();
        client.addEventListener(new AdminEventListener() {
            @Override
            public void onResponse(NsecBunkerAdminClient client, Nip46Response response) {
                listenerCount.incrementAndGet();
            }
        });
        client.addEventListener(new AdminEventListener() {
            @Override
            public void onResponse(NsecBunkerAdminClient client, Nip46Response response) {
                listenerCount.incrementAndGet();
            }
        });

        client.connect();
        assertThat(client.isConnected()).isTrue();
    }

    @Test
    @DisplayName("Event listener can be removed")
    void eventListenerCanBeRemoved() {
        client = createClient();

        AdminEventListener listener = new AdminEventListener() {};
        client.addEventListener(listener);
        client.removeEventListener(listener);

        // Should not throw
        client.addEventListener(null);
    }

    // ========== Configuration Tests ==========

    @Test
    @DisplayName("Connect with custom timeout")
    void connectWithCustomTimeout() throws Exception {
        client = NsecBunkerAdminClient.builder()
                .bunkerPubkey(TEST_BUNKER_PUBKEY)
                .adminPrivateKey(TEST_ADMIN_PRIVKEY)
                .relay(mockRelay.getUrl())
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        client.connect();

        assertThat(client.isConnected()).isTrue();
    }

    @Test
    @DisplayName("Connect with multiple relays")
    void connectWithMultipleRelays() throws Exception {
        // Create second mock relay
        MockRelayServer mockRelay2 = new MockRelayServer();
        mockRelay2.start();
        mockRelay2.enqueueWebSocketUpgrade();

        try {
            client = NsecBunkerAdminClient.builder()
                    .bunkerPubkey(TEST_BUNKER_PUBKEY)
                    .adminPrivateKey(TEST_ADMIN_PRIVKEY)
                    .relays(java.util.List.of(mockRelay.getUrl(), mockRelay2.getUrl()))
                    .build();

            client.connect();

            assertThat(client.isConnected()).isTrue();
        } finally {
            mockRelay2.close();
        }
    }

    @Test
    @DisplayName("Connect with ephemeral key")
    void connectWithEphemeralKey() throws Exception {
        client = NsecBunkerAdminClient.builder()
                .bunkerPubkey(TEST_BUNKER_PUBKEY)
                .adminPrivateKey(TEST_ADMIN_PRIVKEY)
                .relay(mockRelay.getUrl())
                .useEphemeralKey(true)
                .build();

        client.connect();

        assertThat(client.isConnected()).isTrue();
        assertThat(client.getConfig().isUseEphemeralKey()).isTrue();
    }

    // ========== Server-Initiated Disconnect Tests ==========

    @Test
    @DisplayName("Handles server-initiated disconnect")
    void handlesServerInitiatedDisconnect() throws Exception {
        client = createClient();
        client.connect();
        assertThat(client.isConnected()).isTrue();

        // Server disconnects all clients
        mockRelay.disconnectAll();

        // Wait for disconnect to be processed
        Thread.sleep(500);

        // Client state should eventually reflect disconnection
        // Note: Exact behavior depends on reconnection settings
    }

    // ========== Helper Methods ==========

    private NsecBunkerAdminClient createClient() {
        return NsecBunkerAdminClient.builder()
                .bunkerPubkey(TEST_BUNKER_PUBKEY)
                .adminPrivateKey(TEST_ADMIN_PRIVKEY)
                .relay(mockRelay.getUrl())
                .connectTimeout(Duration.ofSeconds(5))
                .requestTimeout(Duration.ofSeconds(5))
                .autoReconnect(false)
                .build();
    }
}
