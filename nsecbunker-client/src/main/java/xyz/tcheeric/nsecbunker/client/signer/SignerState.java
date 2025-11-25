package xyz.tcheeric.nsecbunker.client.signer;

/**
 * Connection state for the NsecBunkerSigner.
 *
 * <p>The signer transitions through these states during its lifecycle:
 * <ul>
 *   <li>{@link #DISCONNECTED} - Initial state or after disconnect</li>
 *   <li>{@link #CONNECTING} - Connection attempt in progress</li>
 *   <li>{@link #CONNECTED} - Successfully connected and ready for operations</li>
 *   <li>{@link #AUTH_URL_REQUIRED} - Bunker requires authorization via URL</li>
 * </ul>
 */
public enum SignerState {

    /**
     * Not connected to the bunker.
     * This is the initial state and the state after disconnection.
     */
    DISCONNECTED,

    /**
     * Currently attempting to connect to the bunker.
     * The signer is establishing a relay connection.
     */
    CONNECTING,

    /**
     * Successfully connected to the bunker.
     * Signing operations can be performed in this state.
     */
    CONNECTED,

    /**
     * The bunker requires user authorization.
     * An authorization URL must be opened in a browser for the user to approve.
     */
    AUTH_URL_REQUIRED
}
