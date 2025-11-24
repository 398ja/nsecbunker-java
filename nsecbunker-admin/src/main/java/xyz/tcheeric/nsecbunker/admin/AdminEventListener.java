package xyz.tcheeric.nsecbunker.admin;

import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Response;

/**
 * Listener interface for admin client events.
 *
 * <p>Implementations can be registered with {@link NsecBunkerAdminClient}
 * to receive notifications about admin operations and responses.
 */
public interface AdminEventListener {

    /**
     * Called when a response is received from the bunker.
     *
     * @param client   the admin client
     * @param response the received response
     */
    default void onResponse(NsecBunkerAdminClient client, Nip46Response response) {
        // Default no-op
    }

    /**
     * Called when the client connects to the bunker.
     *
     * @param client the admin client
     */
    default void onConnected(NsecBunkerAdminClient client) {
        // Default no-op
    }

    /**
     * Called when the client disconnects from the bunker.
     *
     * @param client the admin client
     * @param reason the reason for disconnection (may be null)
     */
    default void onDisconnected(NsecBunkerAdminClient client, String reason) {
        // Default no-op
    }

    /**
     * Called when an error occurs.
     *
     * @param client the admin client
     * @param error  the error
     */
    default void onError(NsecBunkerAdminClient client, Throwable error) {
        // Default no-op
    }

    /**
     * Called when the client is attempting to reconnect.
     *
     * @param client  the admin client
     * @param attempt the reconnection attempt number
     */
    default void onReconnecting(NsecBunkerAdminClient client, int attempt) {
        // Default no-op
    }
}
