package xyz.tcheeric.nsecbunker.admin;

import nostr.base.PublicKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.nsecbunker.connection.ConnectionState;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link NsecBunkerAdminClient}.
 */
class NsecBunkerAdminClientTest {

    // Well-known test keys (secp256k1 scalar = 1, 2)
    private static final String TEST_BUNKER_PUBKEY = "79be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798";
    private static final String TEST_ADMIN_PRIVKEY = "0000000000000000000000000000000000000000000000000000000000000002";
    private static final String TEST_RELAY = "wss://relay.example.com";

    @Test
    @DisplayName("Builder creates client with required fields")
    void builderCreatesClient() {
        NsecBunkerAdminClient client = NsecBunkerAdminClient.builder()
                .bunkerPubkey(TEST_BUNKER_PUBKEY)
                .adminPrivateKey(TEST_ADMIN_PRIVKEY)
                .relay(TEST_RELAY)
                .build();

        assertThat(client).isNotNull();
        assertThat(client.getConfig().getBunkerPubkey()).isEqualTo(TEST_BUNKER_PUBKEY);
        assertThat(client.getBunkerPubkeyHex()).isEqualTo(TEST_BUNKER_PUBKEY);

        client.close();
    }

    @Test
    @DisplayName("Builder accepts multiple relays")
    void builderAcceptsMultipleRelays() {
        NsecBunkerAdminClient client = NsecBunkerAdminClient.builder()
                .bunkerPubkey(TEST_BUNKER_PUBKEY)
                .adminPrivateKey(TEST_ADMIN_PRIVKEY)
                .relays(List.of("wss://relay1.com", "wss://relay2.com"))
                .build();

        assertThat(client.getConfig().getRelays())
                .containsExactly("wss://relay1.com", "wss://relay2.com");

        client.close();
    }

    @Test
    @DisplayName("Builder accepts npub format bunker key")
    void builderAcceptsNpubBunkerKey() {
        // Generate npub from hex pubkey using nostr-java
        PublicKey pk = new PublicKey(TEST_BUNKER_PUBKEY);
        String npub = pk.toBech32String();

        NsecBunkerAdminClient client = NsecBunkerAdminClient.builder()
                .bunkerPubkey(npub)
                .adminPrivateKey(TEST_ADMIN_PRIVKEY)
                .relay(TEST_RELAY)
                .build();

        // Should convert to hex internally
        assertThat(client.getBunkerPubkeyHex()).isEqualTo(TEST_BUNKER_PUBKEY);

        client.close();
    }

    @Test
    @DisplayName("Client starts in disconnected state")
    void clientStartsDisconnected() {
        NsecBunkerAdminClient client = NsecBunkerAdminClient.builder()
                .bunkerPubkey(TEST_BUNKER_PUBKEY)
                .adminPrivateKey(TEST_ADMIN_PRIVKEY)
                .relay(TEST_RELAY)
                .build();

        assertThat(client.getConnectionState()).isEqualTo(ConnectionState.DISCONNECTED);
        assertThat(client.isConnected()).isFalse();

        client.close();
    }

    @Test
    @DisplayName("Client generates ephemeral key when configured")
    void clientGeneratesEphemeralKey() {
        NsecBunkerAdminClient client = NsecBunkerAdminClient.builder()
                .bunkerPubkey(TEST_BUNKER_PUBKEY)
                .adminPrivateKey(TEST_ADMIN_PRIVKEY)
                .relay(TEST_RELAY)
                .useEphemeralKey(true)
                .build();

        // Communication identity should be different from admin identity
        // Since we can't access admin identity directly, we verify config setting
        assertThat(client.getConfig().isUseEphemeralKey()).isTrue();
        assertThat(client.getCommunicationIdentity()).isNotNull();

        client.close();
    }

    @Test
    @DisplayName("Client uses admin key directly when ephemeral disabled")
    void clientUsesAdminKeyWhenEphemeralDisabled() {
        NsecBunkerAdminClient client = NsecBunkerAdminClient.builder()
                .bunkerPubkey(TEST_BUNKER_PUBKEY)
                .adminPrivateKey(TEST_ADMIN_PRIVKEY)
                .relay(TEST_RELAY)
                .useEphemeralKey(false)
                .build();

        assertThat(client.getConfig().isUseEphemeralKey()).isFalse();
        assertThat(client.getCommunicationIdentity()).isNotNull();

        client.close();
    }

    @Test
    @DisplayName("Builder validates configuration")
    void builderValidatesConfig() {
        assertThatThrownBy(() -> NsecBunkerAdminClient.builder()
                .adminPrivateKey(TEST_ADMIN_PRIVKEY)
                .relay(TEST_RELAY)
                .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Bunker public key is required");
    }

    @Test
    @DisplayName("Close changes state and cleans up")
    void closeChangesState() {
        NsecBunkerAdminClient client = NsecBunkerAdminClient.builder()
                .bunkerPubkey(TEST_BUNKER_PUBKEY)
                .adminPrivateKey(TEST_ADMIN_PRIVKEY)
                .relay(TEST_RELAY)
                .build();

        client.close();

        // Can call close multiple times without error
        client.close();
    }

    @Test
    @DisplayName("Connect throws when client is closed")
    void connectThrowsWhenClosed() {
        NsecBunkerAdminClient client = NsecBunkerAdminClient.builder()
                .bunkerPubkey(TEST_BUNKER_PUBKEY)
                .adminPrivateKey(TEST_ADMIN_PRIVKEY)
                .relay(TEST_RELAY)
                .build();

        client.close();

        assertThatThrownBy(client::connect)
                .isInstanceOf(Exception.class)
                .hasMessageContaining("closed");
    }

    @Test
    @DisplayName("Builder fromConnectionString creates client")
    void builderFromConnectionString() {
        String connectionString = "bunker://" + TEST_BUNKER_PUBKEY +
                "?relay=" + TEST_RELAY + "&secret=test-secret";

        NsecBunkerAdminClient client = NsecBunkerAdminClient.builder()
                .fromConnectionString(connectionString, TEST_ADMIN_PRIVKEY)
                .build();

        assertThat(client.getBunkerPubkeyHex()).isEqualTo(TEST_BUNKER_PUBKEY);
        assertThat(client.getConfig().getRelays()).containsExactly(TEST_RELAY);
        assertThat(client.getConfig().getSecret()).isEqualTo("test-secret");

        client.close();
    }

    @Test
    @DisplayName("Builder allows custom timeouts")
    void builderAllowsCustomTimeouts() {
        NsecBunkerAdminClient client = NsecBunkerAdminClient.builder()
                .bunkerPubkey(TEST_BUNKER_PUBKEY)
                .adminPrivateKey(TEST_ADMIN_PRIVKEY)
                .relay(TEST_RELAY)
                .connectTimeout(Duration.ofSeconds(10))
                .requestTimeout(Duration.ofSeconds(45))
                .build();

        assertThat(client.getConfig().getConnectTimeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(client.getConfig().getRequestTimeout()).isEqualTo(Duration.ofSeconds(45));

        client.close();
    }

    @Test
    @DisplayName("Builder allows reconnection configuration")
    void builderAllowsReconnectionConfig() {
        NsecBunkerAdminClient client = NsecBunkerAdminClient.builder()
                .bunkerPubkey(TEST_BUNKER_PUBKEY)
                .adminPrivateKey(TEST_ADMIN_PRIVKEY)
                .relay(TEST_RELAY)
                .autoReconnect(true)
                .maxReconnectAttempts(10)
                .reconnectDelay(Duration.ofSeconds(2))
                .maxReconnectDelay(Duration.ofMinutes(1))
                .build();

        assertThat(client.getConfig().isAutoReconnect()).isTrue();
        assertThat(client.getConfig().getMaxReconnectAttempts()).isEqualTo(10);
        assertThat(client.getConfig().getReconnectDelay()).isEqualTo(Duration.ofSeconds(2));
        assertThat(client.getConfig().getMaxReconnectDelay()).isEqualTo(Duration.ofMinutes(1));

        client.close();
    }

    @Test
    @DisplayName("Event listener can be added and removed")
    void eventListenerCanBeAddedAndRemoved() {
        NsecBunkerAdminClient client = NsecBunkerAdminClient.builder()
                .bunkerPubkey(TEST_BUNKER_PUBKEY)
                .adminPrivateKey(TEST_ADMIN_PRIVKEY)
                .relay(TEST_RELAY)
                .build();

        AdminEventListener listener = new AdminEventListener() {};

        client.addEventListener(listener);
        client.removeEventListener(listener);

        // Should not throw
        client.addEventListener(null);

        client.close();
    }
}
