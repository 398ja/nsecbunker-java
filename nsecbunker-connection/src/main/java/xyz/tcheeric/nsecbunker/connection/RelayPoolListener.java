package xyz.tcheeric.nsecbunker.connection;

import nostr.event.impl.GenericEvent;

/**
 * Listener interface for relay pool events.
 *
 * <p>This interface extends the concept of {@link RelayListener} to work with
 * a pool of relays. Events include the source relay so listeners can track
 * which relay sent the event.
 */
public interface RelayPoolListener {

    /**
     * Called when a relay's connection state changes.
     *
     * @param relay    the relay connection
     * @param oldState the previous state
     * @param newState the new state
     */
    default void onRelayStateChange(RelayConnection relay, ConnectionState oldState, ConnectionState newState) {
        // Default no-op
    }

    /**
     * Called when a relay connects.
     *
     * @param relay the relay that connected
     */
    default void onRelayConnect(RelayConnection relay) {
        // Default no-op
    }

    /**
     * Called when a relay disconnects.
     *
     * @param relay  the relay that disconnected
     * @param code   the close code
     * @param reason the close reason
     */
    default void onRelayDisconnect(RelayConnection relay, int code, String reason) {
        // Default no-op
    }

    /**
     * Called when a relay encounters an error.
     *
     * @param relay     the relay with the error
     * @param throwable the error
     */
    default void onRelayError(RelayConnection relay, Throwable throwable) {
        // Default no-op
    }

    /**
     * Called when an EVENT message is received from any relay.
     *
     * <p>If deduplication is enabled on the pool, this will only be called
     * once per unique event ID across all relays.
     *
     * @param relay          the source relay
     * @param subscriptionId the subscription ID
     * @param event          the received event
     */
    default void onEvent(RelayConnection relay, String subscriptionId, GenericEvent event) {
        // Default no-op
    }

    /**
     * Called when an OK message is received from a relay.
     *
     * @param relay   the source relay
     * @param eventId the event ID
     * @param success whether the event was accepted
     * @param message optional message from relay
     */
    default void onOk(RelayConnection relay, String eventId, boolean success, String message) {
        // Default no-op
    }

    /**
     * Called when an EOSE message is received from a relay.
     *
     * @param relay          the source relay
     * @param subscriptionId the subscription ID
     */
    default void onEndOfStoredEvents(RelayConnection relay, String subscriptionId) {
        // Default no-op
    }

    /**
     * Called when a NOTICE message is received from a relay.
     *
     * @param relay   the source relay
     * @param message the notice message
     */
    default void onNotice(RelayConnection relay, String message) {
        // Default no-op
    }

    /**
     * Called when a CLOSED message is received from a relay.
     *
     * @param relay          the source relay
     * @param subscriptionId the subscription ID
     * @param message        the close message
     */
    default void onClosed(RelayConnection relay, String subscriptionId, String message) {
        // Default no-op
    }

    /**
     * Called when an AUTH challenge is received from a relay.
     *
     * @param relay     the source relay
     * @param challenge the authentication challenge
     */
    default void onAuth(RelayConnection relay, String challenge) {
        // Default no-op
    }
}
