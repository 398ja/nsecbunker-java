package xyz.tcheeric.nsecbunker.client.signer;

/**
 * Connection state for the signer.
 */
public enum SignerState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    AUTH_URL_REQUIRED
}
