package xyz.tcheeric.nsecbunker.core.model;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a connection to an nsecBunker instance.
 *
 * <p>A bunker connection string follows the format:
 * {@code bunker://<remote-pubkey>?relay=<relay-url>&relay=<relay-url>&secret=<secret>}
 *
 * <p>Example: {@code bunker://npub1...?relay=wss://relay.nsecbunker.com&secret=abc123}
 */
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString(exclude = "secret")
public final class BunkerConnection {

    /**
     * The URI scheme for bunker connection strings.
     */
    public static final String BUNKER_SCHEME = "bunker";

    /**
     * The public key (npub) of the remote bunker key to connect to.
     */
    private final String remotePubkey;

    /**
     * The list of relay URLs to connect through.
     */
    @Builder.Default
    private final List<String> relays = Collections.emptyList();

    /**
     * Optional secret for authentication.
     */
    private final String secret;

    /**
     * Optional token for bunker connection (alternative to secret).
     */
    private final String token;

    /**
     * Creates a new BunkerConnection with the specified remote pubkey.
     *
     * @param remotePubkey the remote public key (npub or hex)
     * @return a new BunkerConnection builder
     */
    public static BunkerConnectionBuilder forPubkey(String remotePubkey) {
        Objects.requireNonNull(remotePubkey, "remotePubkey must not be null");
        return builder().remotePubkey(remotePubkey);
    }

    /**
     * Returns an unmodifiable view of the relay list.
     *
     * @return the relay URLs
     */
    public List<String> getRelays() {
        return relays == null ? Collections.emptyList() : Collections.unmodifiableList(relays);
    }

    /**
     * Checks if this connection has a secret.
     *
     * @return true if a secret is present
     */
    public boolean hasSecret() {
        return secret != null && !secret.isEmpty();
    }

    /**
     * Checks if this connection has a token.
     *
     * @return true if a token is present
     */
    public boolean hasToken() {
        return token != null && !token.isEmpty();
    }

    /**
     * Checks if this connection has any authentication credentials.
     *
     * @return true if either secret or token is present
     */
    public boolean hasCredentials() {
        return hasSecret() || hasToken();
    }

    /**
     * Returns the connection string representation.
     *
     * @return the bunker:// connection string
     */
    public String toConnectionString() {
        StringBuilder sb = new StringBuilder();
        sb.append(BUNKER_SCHEME).append("://").append(remotePubkey);

        boolean hasParams = false;
        if (relays != null && !relays.isEmpty()) {
            for (String relay : relays) {
                sb.append(hasParams ? "&" : "?");
                sb.append("relay=").append(relay);
                hasParams = true;
            }
        }

        if (hasSecret()) {
            sb.append(hasParams ? "&" : "?");
            sb.append("secret=").append(secret);
        }

        return sb.toString();
    }
}
