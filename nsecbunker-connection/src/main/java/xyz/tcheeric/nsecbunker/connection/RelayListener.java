package xyz.tcheeric.nsecbunker.connection;

import nostr.event.impl.GenericEvent;

/**
 * Listener interface for relay connection events.
 *
 * <p>Implementations receive callbacks when the connection state changes,
 * messages are received, or errors occur.
 */
public interface RelayListener {

    /**
     * Called when the connection state changes.
     *
     * @param relay    the relay connection
     * @param oldState the previous state
     * @param newState the new state
     */
    default void onStateChange(RelayConnection relay, ConnectionState oldState, ConnectionState newState) {
        // Default no-op
    }

    /**
     * Called when the connection is established.
     *
     * @param relay the relay connection
     */
    default void onConnect(RelayConnection relay) {
        // Default no-op
    }

    /**
     * Called when the connection is closed.
     *
     * @param relay  the relay connection
     * @param code   the close code
     * @param reason the close reason
     */
    default void onDisconnect(RelayConnection relay, int code, String reason) {
        // Default no-op
    }

    /**
     * Called when an EVENT message is received from the relay.
     *
     * @param relay          the relay connection
     * @param subscriptionId the subscription ID
     * @param event          the received event
     */
    default void onEvent(RelayConnection relay, String subscriptionId, GenericEvent event) {
        // Default no-op
    }

    /**
     * Called when an OK message is received (response to EVENT submission).
     *
     * @param relay   the relay connection
     * @param eventId the event ID
     * @param success whether the event was accepted
     * @param message optional message from relay
     */
    default void onOk(RelayConnection relay, String eventId, boolean success, String message) {
        // Default no-op
    }

    /**
     * Called when an EOSE (End of Stored Events) message is received.
     *
     * @param relay          the relay connection
     * @param subscriptionId the subscription ID
     */
    default void onEndOfStoredEvents(RelayConnection relay, String subscriptionId) {
        // Default no-op
    }

    /**
     * Called when a NOTICE message is received from the relay.
     *
     * @param relay   the relay connection
     * @param message the notice message
     */
    default void onNotice(RelayConnection relay, String message) {
        // Default no-op
    }

    /**
     * Called when a CLOSED message is received (subscription closed by relay).
     *
     * @param relay          the relay connection
     * @param subscriptionId the subscription ID
     * @param message        the close message
     */
    default void onClosed(RelayConnection relay, String subscriptionId, String message) {
        // Default no-op
    }

    /**
     * Called when an AUTH challenge is received (NIP-42).
     *
     * @param relay     the relay connection
     * @param challenge the authentication challenge
     */
    default void onAuth(RelayConnection relay, String challenge) {
        // Default no-op
    }

    /**
     * Called when an error occurs on the connection.
     *
     * @param relay     the relay connection
     * @param throwable the error
     */
    default void onError(RelayConnection relay, Throwable throwable) {
        // Default no-op
    }

    /**
     * Called when a raw message is received (for debugging/logging).
     *
     * @param relay   the relay connection
     * @param message the raw message text
     */
    default void onRawMessage(RelayConnection relay, String message) {
        // Default no-op
    }
}
