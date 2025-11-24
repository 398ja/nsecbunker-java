package xyz.tcheeric.nsecbunker.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;

/**
 * Represents a key stored in an nsecBunker instance.
 *
 * <p>A BunkerKey contains metadata about a Nostr key managed by the bunker,
 * including its name, public key, and usage statistics.
 */
@Getter
@Builder(toBuilder = true)
@Jacksonized
@EqualsAndHashCode
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public final class BunkerKey {

    /**
     * The human-readable name of the key.
     */
    private final String name;

    /**
     * The public key in npub (bech32) format.
     */
    private final String npub;

    /**
     * The public key in hex format.
     */
    @JsonProperty("pubkey")
    private final String pubkeyHex;

    /**
     * Whether the key is currently locked (encrypted and requires passphrase to unlock).
     */
    @Builder.Default
    private final boolean locked = false;

    /**
     * The number of users who have access to this key.
     */
    @Builder.Default
    @JsonProperty("user_count")
    private final int userCount = 0;

    /**
     * The number of tokens issued for this key.
     */
    @Builder.Default
    @JsonProperty("token_count")
    private final int tokenCount = 0;

    /**
     * The number of signing operations performed with this key.
     */
    @Builder.Default
    @JsonProperty("signing_count")
    private final long signingCount = 0;

    /**
     * When the key was created.
     */
    @JsonProperty("created_at")
    private final Instant createdAt;

    /**
     * When the key was last used for signing.
     */
    @JsonProperty("last_used_at")
    private final Instant lastUsedAt;

    /**
     * Optional description or notes about this key.
     */
    private final String description;

    /**
     * Creates a new BunkerKey builder with the specified name.
     *
     * @param name the key name
     * @return a new builder instance
     */
    public static BunkerKeyBuilder withName(String name) {
        return builder().name(name);
    }

    /**
     * Returns the public key, preferring hex format if available.
     *
     * @return the public key in hex format, or npub if hex is not available
     */
    public String getPublicKey() {
        return pubkeyHex != null ? pubkeyHex : npub;
    }

    /**
     * Checks if this key has been used for signing.
     *
     * @return true if the key has been used
     */
    public boolean hasBeenUsed() {
        return signingCount > 0 || lastUsedAt != null;
    }

    /**
     * Checks if this key has any users with access.
     *
     * @return true if the key has users
     */
    public boolean hasUsers() {
        return userCount > 0;
    }

    /**
     * Checks if this key has any active tokens.
     *
     * @return true if the key has tokens
     */
    public boolean hasTokens() {
        return tokenCount > 0;
    }
}
