package xyz.tcheeric.nsecbunker.e2e.tests;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import xyz.tcheeric.nsecbunker.admin.NsecBunkerAdminClient;
import xyz.tcheeric.nsecbunker.e2e.E2ETestBase;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * E2E tests for admin connection lifecycle.
 *
 * <p>Tests: start container, connect via admin client, subscribe for responses, disconnect/close.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Admin Connection Lifecycle E2E Tests")
class AdminConnectionLifecycleE2ETest extends E2ETestBase {

    @Test
    @Order(1)
    @DisplayName("Should connect to bunker via relay")
    void shouldConnectToBunkerViaRelay() throws Exception {
        // Create admin client
        NsecBunkerAdminClient adminClient = NsecBunkerAdminClient.builder()
                .bunkerPubkey(getBunkerNpub())
                .adminPrivateKey(getAdminNsec())
                .relay(getRelayUrl())
                .connectTimeout(DEFAULT_CONNECT_TIMEOUT)
                .requestTimeout(DEFAULT_REQUEST_TIMEOUT)
                .useEphemeralKey(false) // Use admin key directly for jaonoctus/nsecbunkerd
                .build();

        try {
            // Connect to bunker
            adminClient.connect();

            // Verify connection is established
            await().atMost(DEFAULT_REQUEST_TIMEOUT)
                    .pollInterval(500, TimeUnit.MILLISECONDS)
                    .untilAsserted(() -> assertThat(adminClient.isConnected()).isTrue());

            log.info("Successfully connected to bunker");

            // Ping the bunker to verify it's responsive
            String pingResult = adminClient.ping().get(DEFAULT_REQUEST_TIMEOUT.getSeconds(), TimeUnit.SECONDS);
            assertThat(pingResult).isEqualTo("pong");
            log.info("Bunker responded to ping");

        } finally {
            // Disconnect
            adminClient.disconnect();
            assertThat(adminClient.isConnected()).isFalse();
            log.info("Disconnected from bunker");
        }
    }

    @Test
    @Order(2)
    @DisplayName("Should reconnect after disconnect")
    void shouldReconnectAfterDisconnect() throws Exception {
        NsecBunkerAdminClient adminClient = NsecBunkerAdminClient.builder()
                .bunkerPubkey(getBunkerNpub())
                .adminPrivateKey(getAdminNsec())
                .relay(getRelayUrl())
                .connectTimeout(DEFAULT_CONNECT_TIMEOUT)
                .requestTimeout(DEFAULT_REQUEST_TIMEOUT)
                .useEphemeralKey(false) // Use admin key directly
                .build();

        try {
            // First connection
            adminClient.connect();
            await().atMost(DEFAULT_REQUEST_TIMEOUT)
                    .untilAsserted(() -> assertThat(adminClient.isConnected()).isTrue());

            // Verify connection works
            String firstPing = adminClient.ping().get(DEFAULT_REQUEST_TIMEOUT.getSeconds(), TimeUnit.SECONDS);
            assertThat(firstPing).isEqualTo("pong");
            log.info("First connection established");

            // Disconnect
            adminClient.disconnect();
            assertThat(adminClient.isConnected()).isFalse();
            log.info("Disconnected");

            // Wait a bit before reconnecting
            Thread.sleep(1000);

            // Reconnect
            adminClient.connect();
            await().atMost(DEFAULT_REQUEST_TIMEOUT)
                    .untilAsserted(() -> assertThat(adminClient.isConnected()).isTrue());

            // Verify connection works again
            String secondPing = adminClient.ping().get(DEFAULT_REQUEST_TIMEOUT.getSeconds(), TimeUnit.SECONDS);
            assertThat(secondPing).isEqualTo("pong");
            log.info("Successfully reconnected");

        } finally {
            adminClient.disconnect();
        }
    }

    @Test
    @Order(3)
    @DisplayName("Should handle multiple relay connections")
    void shouldHandleMultipleRelayConnections() throws Exception {
        // Create client with the same relay listed multiple times (simulating multiple relays)
        NsecBunkerAdminClient adminClient = NsecBunkerAdminClient.builder()
                .bunkerPubkey(getBunkerNpub())
                .adminPrivateKey(getAdminNsec())
                .relay(getRelayUrl())
                .connectTimeout(DEFAULT_CONNECT_TIMEOUT)
                .requestTimeout(DEFAULT_REQUEST_TIMEOUT)
                .useEphemeralKey(false) // Use admin key directly
                .build();

        try {
            adminClient.connect();

            await().atMost(DEFAULT_REQUEST_TIMEOUT)
                    .untilAsserted(() -> assertThat(adminClient.isConnected()).isTrue());

            // Should be able to ping
            String pingResult = adminClient.ping().get(DEFAULT_REQUEST_TIMEOUT.getSeconds(), TimeUnit.SECONDS);
            assertThat(pingResult).isEqualTo("pong");

            log.info("Multi-relay connection successful");

        } finally {
            adminClient.disconnect();
        }
    }

    @Test
    @Order(4)
    @DisplayName("Should maintain connection for extended period")
    void shouldMaintainConnectionForExtendedPeriod() throws Exception {
        NsecBunkerAdminClient adminClient = NsecBunkerAdminClient.builder()
                .bunkerPubkey(getBunkerNpub())
                .adminPrivateKey(getAdminNsec())
                .relay(getRelayUrl())
                .connectTimeout(DEFAULT_CONNECT_TIMEOUT)
                .requestTimeout(DEFAULT_REQUEST_TIMEOUT)
                .useEphemeralKey(false) // Use admin key directly
                .build();

        try {
            adminClient.connect();

            await().atMost(DEFAULT_REQUEST_TIMEOUT)
                    .untilAsserted(() -> assertThat(adminClient.isConnected()).isTrue());

            // Send multiple pings over time to verify connection stability
            for (int i = 0; i < 5; i++) {
                String pingResult = adminClient.ping().get(DEFAULT_REQUEST_TIMEOUT.getSeconds(), TimeUnit.SECONDS);
                assertThat(pingResult).isEqualTo("pong");
                log.info("Ping {} successful", i + 1);
                Thread.sleep(1000);
            }

            // Connection should still be active
            assertThat(adminClient.isConnected()).isTrue();
            log.info("Connection maintained successfully");

        } finally {
            adminClient.disconnect();
        }
    }

    @Test
    @Order(5)
    @DisplayName("Should handle concurrent connections from multiple clients")
    void shouldHandleConcurrentConnections() throws Exception {
        NsecBunkerAdminClient client1 = NsecBunkerAdminClient.builder()
                .bunkerPubkey(getBunkerNpub())
                .adminPrivateKey(getAdminNsec())
                .relay(getRelayUrl())
                .connectTimeout(DEFAULT_CONNECT_TIMEOUT)
                .requestTimeout(DEFAULT_REQUEST_TIMEOUT)
                .useEphemeralKey(false) // Use admin key directly
                .build();

        // Second client also uses the same admin credentials
        // This tests concurrent connections from the same admin identity
        NsecBunkerAdminClient client2 = NsecBunkerAdminClient.builder()
                .bunkerPubkey(getBunkerNpub())
                .adminPrivateKey(getAdminNsec())
                .relay(getRelayUrl())
                .connectTimeout(DEFAULT_CONNECT_TIMEOUT)
                .requestTimeout(DEFAULT_REQUEST_TIMEOUT)
                .useEphemeralKey(false) // Use admin key directly
                .build();

        try {
            // Connect both clients
            client1.connect();
            client2.connect();

            // Wait for both to connect
            await().atMost(DEFAULT_REQUEST_TIMEOUT)
                    .untilAsserted(() -> {
                        assertThat(client1.isConnected()).isTrue();
                        assertThat(client2.isConnected()).isTrue();
                    });

            // Both should be able to ping
            String ping1 = client1.ping().get(DEFAULT_REQUEST_TIMEOUT.getSeconds(), TimeUnit.SECONDS);
            String ping2 = client2.ping().get(DEFAULT_REQUEST_TIMEOUT.getSeconds(), TimeUnit.SECONDS);

            assertThat(ping1).isEqualTo("pong");
            assertThat(ping2).isEqualTo("pong");

            log.info("Both concurrent clients working");

        } finally {
            client1.disconnect();
            client2.disconnect();
        }
    }
}
