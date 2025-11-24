package xyz.tcheeric.nsecbunker.connection;

/**
 * Represents the state of a relay connection.
 */
public enum ConnectionState {

    /**
     * Connection has not been established yet.
     */
    DISCONNECTED,

    /**
     * Currently attempting to connect.
     */
    CONNECTING,

    /**
     * Connection is established and ready for communication.
     */
    CONNECTED,

    /**
     * Connection is being closed gracefully.
     */
    DISCONNECTING,

    /**
     * Connection was lost unexpectedly and may be reconnected.
     */
    RECONNECTING,

    /**
     * Connection has been permanently closed and will not reconnect.
     */
    CLOSED,

    /**
     * Connection failed due to an error.
     */
    FAILED;

    /**
     * Checks if this state represents an active connection.
     *
     * @return true if connected or connecting
     */
    public boolean isActive() {
        return this == CONNECTED || this == CONNECTING || this == RECONNECTING;
    }

    /**
     * Checks if this state represents a terminal state.
     *
     * @return true if the connection cannot be used anymore
     */
    public boolean isTerminal() {
        return this == CLOSED || this == FAILED;
    }

    /**
     * Checks if this state allows sending messages.
     *
     * @return true if messages can be sent
     */
    public boolean canSend() {
        return this == CONNECTED;
    }
}
