package xyz.tcheeric.nsecbunker.connection;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class ConnectionStateTest {

    @Test
    void isActive_shouldReturnTrueForActiveStates() {
        assertThat(ConnectionState.CONNECTED.isActive()).isTrue();
        assertThat(ConnectionState.CONNECTING.isActive()).isTrue();
        assertThat(ConnectionState.RECONNECTING.isActive()).isTrue();
    }

    @Test
    void isActive_shouldReturnFalseForInactiveStates() {
        assertThat(ConnectionState.DISCONNECTED.isActive()).isFalse();
        assertThat(ConnectionState.DISCONNECTING.isActive()).isFalse();
        assertThat(ConnectionState.CLOSED.isActive()).isFalse();
        assertThat(ConnectionState.FAILED.isActive()).isFalse();
    }

    @Test
    void isTerminal_shouldReturnTrueForTerminalStates() {
        assertThat(ConnectionState.CLOSED.isTerminal()).isTrue();
        assertThat(ConnectionState.FAILED.isTerminal()).isTrue();
    }

    @Test
    void isTerminal_shouldReturnFalseForNonTerminalStates() {
        assertThat(ConnectionState.DISCONNECTED.isTerminal()).isFalse();
        assertThat(ConnectionState.CONNECTING.isTerminal()).isFalse();
        assertThat(ConnectionState.CONNECTED.isTerminal()).isFalse();
        assertThat(ConnectionState.DISCONNECTING.isTerminal()).isFalse();
        assertThat(ConnectionState.RECONNECTING.isTerminal()).isFalse();
    }

    @Test
    void canSend_shouldReturnTrueOnlyWhenConnected() {
        assertThat(ConnectionState.CONNECTED.canSend()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = ConnectionState.class, mode = EnumSource.Mode.EXCLUDE, names = {"CONNECTED"})
    void canSend_shouldReturnFalseForOtherStates(ConnectionState state) {
        assertThat(state.canSend()).isFalse();
    }

    @Test
    void allStatesExist() {
        // Ensure all expected states are defined
        assertThat(ConnectionState.values()).containsExactlyInAnyOrder(
                ConnectionState.DISCONNECTED,
                ConnectionState.CONNECTING,
                ConnectionState.CONNECTED,
                ConnectionState.DISCONNECTING,
                ConnectionState.RECONNECTING,
                ConnectionState.CLOSED,
                ConnectionState.FAILED
        );
    }
}
