package xyz.tcheeric.nsecbunker.connection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RelayPoolTest {

    private static final String RELAY_URL_1 = "wss://relay1.example.com";
    private static final String RELAY_URL_2 = "wss://relay2.example.com";
    private static final String RELAY_URL_3 = "wss://relay3.example.com";

    private RelayPool pool;

    @BeforeEach
    void setUp() {
        pool = RelayPool.builder().build();
    }

    @AfterEach
    void tearDown() {
        if (pool != null && !pool.isClosed()) {
            pool.close();
        }
    }

    @Test
    void builder_shouldCreateEmptyPool() {
        RelayPool emptyPool = RelayPool.builder().build();

        assertThat(emptyPool.isEmpty()).isTrue();
        assertThat(emptyPool.size()).isZero();

        emptyPool.close();
    }

    @Test
    void builder_shouldCreatePoolWithRelays() {
        RelayPool poolWithRelays = RelayPool.builder()
                .relay(RELAY_URL_1)
                .relay(RELAY_URL_2)
                .build();

        assertThat(poolWithRelays.size()).isEqualTo(2);
        assertThat(poolWithRelays.getRelayUrls()).containsExactlyInAnyOrder(RELAY_URL_1, RELAY_URL_2);

        poolWithRelays.close();
    }

    @Test
    void builder_shouldAcceptRelaysList() {
        List<String> urls = Arrays.asList(RELAY_URL_1, RELAY_URL_2, RELAY_URL_3);

        RelayPool poolWithRelays = RelayPool.builder()
                .relays(urls)
                .build();

        assertThat(poolWithRelays.size()).isEqualTo(3);

        poolWithRelays.close();
    }

    @Test
    void builder_shouldSetCustomTimeout() {
        Duration customTimeout = Duration.ofSeconds(60);

        RelayPool customPool = RelayPool.builder()
                .connectTimeout(customTimeout)
                .build();

        assertThat(customPool.getConnectTimeout()).isEqualTo(customTimeout);

        customPool.close();
    }

    @Test
    void builder_shouldSetMinConnectedRelays() {
        RelayPool customPool = RelayPool.builder()
                .minConnectedRelays(3)
                .build();

        assertThat(customPool.getMinConnectedRelays()).isEqualTo(3);

        customPool.close();
    }

    @Test
    void builder_shouldSetDeduplicateEvents() {
        RelayPool customPool = RelayPool.builder()
                .deduplicateEvents(false)
                .build();

        assertThat(customPool.isDeduplicateEvents()).isFalse();

        customPool.close();
    }

    @Test
    void builder_shouldIgnoreNullRelays() {
        RelayPool poolWithNulls = RelayPool.builder()
                .relay(null)
                .relay(RELAY_URL_1)
                .relay("")
                .build();

        assertThat(poolWithNulls.size()).isEqualTo(1);

        poolWithNulls.close();
    }

    @Test
    void addRelay_shouldAddNewRelay() {
        pool.addRelay(RELAY_URL_1);

        assertThat(pool.size()).isEqualTo(1);
        assertThat(pool.getRelay(RELAY_URL_1)).isNotNull();
    }

    @Test
    void addRelay_shouldReturnExistingRelay() {
        RelayConnection first = pool.addRelay(RELAY_URL_1);
        RelayConnection second = pool.addRelay(RELAY_URL_1);

        assertThat(first).isSameAs(second);
        assertThat(pool.size()).isEqualTo(1);
    }

    @Test
    void addRelay_shouldThrowOnNullUrl() {
        assertThatThrownBy(() -> pool.addRelay(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void addRelay_shouldThrowWhenClosed() {
        pool.close();

        assertThatThrownBy(() -> pool.addRelay(RELAY_URL_1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("closed");
    }

    @Test
    void removeRelay_shouldRemoveExistingRelay() {
        pool.addRelay(RELAY_URL_1);

        boolean removed = pool.removeRelay(RELAY_URL_1);

        assertThat(removed).isTrue();
        assertThat(pool.size()).isZero();
        assertThat(pool.getRelay(RELAY_URL_1)).isNull();
    }

    @Test
    void removeRelay_shouldReturnFalseForNonExisting() {
        boolean removed = pool.removeRelay(RELAY_URL_1);

        assertThat(removed).isFalse();
    }

    @Test
    void getRelays_shouldReturnUnmodifiableCollection() {
        pool.addRelay(RELAY_URL_1);

        assertThatThrownBy(() -> pool.getRelays().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void getRelayUrls_shouldReturnUnmodifiableSet() {
        pool.addRelay(RELAY_URL_1);

        assertThatThrownBy(() -> pool.getRelayUrls().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void getConnectedRelays_shouldReturnEmptyWhenNoneConnected() {
        pool.addRelay(RELAY_URL_1);
        pool.addRelay(RELAY_URL_2);

        assertThat(pool.getConnectedRelays()).isEmpty();
        assertThat(pool.getConnectedCount()).isZero();
    }

    @Test
    void hasMinimumConnections_shouldReturnFalseWhenNoneConnected() {
        pool.addRelay(RELAY_URL_1);

        assertThat(pool.hasMinimumConnections()).isFalse();
    }

    @Test
    void addListener_shouldAcceptListener() {
        TestPoolListener listener = new TestPoolListener();

        pool.addListener(listener);
        // Should not throw
    }

    @Test
    void addListener_shouldAcceptNullListener() {
        // Should not throw
        pool.addListener(null);
    }

    @Test
    void removeListener_shouldRemoveListener() {
        TestPoolListener listener = new TestPoolListener();
        pool.addListener(listener);

        pool.removeListener(listener);
        // Should not receive events after removal
    }

    @Test
    void close_shouldMarkPoolAsClosed() {
        pool.close();

        assertThat(pool.isClosed()).isTrue();
    }

    @Test
    void close_shouldBeIdempotent() {
        pool.close();
        pool.close(); // Should not throw

        assertThat(pool.isClosed()).isTrue();
    }

    @Test
    void close_shouldCloseAllRelays() {
        pool.addRelay(RELAY_URL_1);
        pool.addRelay(RELAY_URL_2);

        pool.close();

        assertThat(pool.getRelays()).isEmpty();
    }

    @Test
    void clearSeenEvents_shouldResetSeenEventCount() {
        // Can't easily test deduplication without mocking events,
        // but we can test the clear functionality
        pool.clearSeenEvents();

        assertThat(pool.getSeenEventCount()).isZero();
    }

    @Test
    void toString_shouldContainUsefulInfo() {
        pool.addRelay(RELAY_URL_1);
        pool.addRelay(RELAY_URL_2);

        String result = pool.toString();

        assertThat(result).contains("relays=2");
        assertThat(result).contains("connected=0");
        assertThat(result).contains("closed=false");
    }

    @Test
    void connectAll_shouldThrowWhenNoRelays() {
        assertThatThrownBy(() -> pool.connectAll())
                .isInstanceOf(Exception.class)
                .hasMessageContaining("No relays");
    }

    @Test
    void connectAll_shouldThrowWhenClosed() {
        pool.addRelay(RELAY_URL_1);
        pool.close();

        assertThatThrownBy(() -> pool.connectAll())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("closed");
    }

    @Test
    void broadcast_shouldReturnZeroWhenNoRelaysConnected() {
        pool.addRelay(RELAY_URL_1);

        int count = pool.broadcast("[\"REQ\",\"test\",{}]");

        assertThat(count).isZero();
    }

    @Test
    void broadcastReq_shouldReturnZeroWhenNoRelaysConnected() {
        pool.addRelay(RELAY_URL_1);

        int count = pool.broadcastReq("sub1", "{}");

        assertThat(count).isZero();
    }

    @Test
    void broadcastClose_shouldReturnZeroWhenNoRelaysConnected() {
        pool.addRelay(RELAY_URL_1);

        int count = pool.broadcastClose("sub1");

        assertThat(count).isZero();
    }

    @Test
    void broadcastEvent_shouldReturnZeroWhenNoRelaysConnected() {
        pool.addRelay(RELAY_URL_1);

        int count = pool.broadcastEvent("{}");

        assertThat(count).isZero();
    }

    @Test
    void sendTo_byUrl_shouldReturnFalseWhenRelayNotFound() {
        boolean sent = pool.sendTo(RELAY_URL_1, "message");

        assertThat(sent).isFalse();
    }

    @Test
    void sendTo_byUrl_shouldReturnFalseWhenRelayNotConnected() {
        pool.addRelay(RELAY_URL_1);

        boolean sent = pool.sendTo(RELAY_URL_1, "message");

        assertThat(sent).isFalse();
    }

    @Test
    void disconnectAll_shouldNotThrowWhenEmpty() {
        // Should not throw
        pool.disconnectAll();
    }

    @Test
    void disconnectAll_shouldDisconnectAllRelays() {
        pool.addRelay(RELAY_URL_1);
        pool.addRelay(RELAY_URL_2);

        pool.disconnectAll();

        // Relays should be disconnected (in CLOSED state)
        for (RelayConnection relay : pool.getRelays()) {
            assertThat(relay.getState()).isIn(
                    ConnectionState.DISCONNECTED,
                    ConnectionState.CLOSED
            );
        }
    }

    /**
     * Test listener for capturing events.
     */
    private static class TestPoolListener implements RelayPoolListener {
        private int connectCount;
        private int disconnectCount;
        private int eventCount;

        @Override
        public void onRelayConnect(RelayConnection relay) {
            connectCount++;
        }

        @Override
        public void onRelayDisconnect(RelayConnection relay, int code, String reason) {
            disconnectCount++;
        }
    }
}
