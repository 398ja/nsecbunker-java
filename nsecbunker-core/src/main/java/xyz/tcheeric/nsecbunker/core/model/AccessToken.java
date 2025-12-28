package xyz.tcheeric.nsecbunker.core.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import java.time.Duration;
import java.time.Instant;

/**
 * Represents an access token for connecting to nsecBunker.
 *
 * <p>Access tokens provide a way to grant temporary access to a bunker key
 * without sharing the key itself. Tokens can have policies attached and
 * expiration times.
 */
@Getter
@Builder(toBuilder = true)
@Jacksonized
@EqualsAndHashCode(exclude = "token")
@ToString(exclude = "token")
@JsonIgnoreProperties(ignoreUnknown = true)
public final class AccessToken {

    /**
     * Unique identifier for this token.
     */
    private final String id;

    /**
     * The actual token string (sensitive - excluded from toString).
     */
    private final String token;

    /**
     * The name of the key this token grants access to.
     */
    @JsonProperty("key_name")
    private final String keyName;

    /**
     * The public key (npub) of the key this token grants access to.
     * In nsecbunkerd, the token field contains "npub#secret".
     */
    @JsonProperty("key_npub")
    private final String keyNpub;

    /**
     * Human-readable name/description for this token (e.g., "mobile-client").
     */
    @JsonProperty("client_name")
    private final String clientName;

    /**
     * The policy associated with this token.
     */
    private final BunkerPolicy policy;

    /**
     * The ID of the policy, if not fully loaded.
     */
    @JsonProperty("policy_id")
    private final String policyId;

    /**
     * The name of the policy (from nsecbunkerd).
     */
    @JsonProperty("policy_name")
    private final String policyName;

    /**
     * When this token was created.
     */
    @JsonProperty("created_at")
    private final Instant createdAt;

    /**
     * When this token was last updated (from nsecbunkerd).
     */
    @JsonProperty("updated_at")
    private final Instant updatedAt;

    /**
     * When this token expires.
     */
    @JsonProperty("expires_at")
    private final Instant expiresAt;

    /**
     * When this token was last used.
     */
    @JsonProperty("last_used_at")
    private final Instant lastUsedAt;

    /**
     * When this token was redeemed (from nsecbunkerd).
     */
    @JsonProperty("redeemed_at")
    private final Instant redeemedAt;

    /**
     * Description of who redeemed this token (from nsecbunkerd).
     */
    @JsonProperty("redeemed_by")
    private final String redeemedBy;

    /**
     * Seconds until this token expires (from nsecbunkerd).
     */
    @JsonProperty("time_until_expiration")
    private final Long timeUntilExpirationSeconds;

    /**
     * Number of times this token has been used.
     */
    @Builder.Default
    @JsonProperty("usage_count")
    private final long usageCount = 0;

    /**
     * Maximum number of times this token can be used. Null means unlimited.
     */
    @JsonProperty("max_usage")
    private final Long maxUsage;

    /**
     * Whether this token has been revoked.
     * nsecbunkerd may use "revoked" or "is_revoked" to indicate revocation status.
     */
    @Builder.Default
    @JsonAlias({"is_revoked"})
    private final boolean revoked = false;

    /**
     * The relay URLs associated with this token.
     */
    private final String relay;

    /**
     * Creates a new token builder for the given key.
     *
     * @param keyName the key name
     * @return a new builder instance
     */
    public static AccessTokenBuilder forKey(String keyName) {
        return builder().keyName(keyName);
    }

    /**
     * Checks if this token has expired.
     *
     * @return true if the token has an expiration date and it has passed
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    /**
     * Checks if this token is currently valid.
     *
     * @return true if the token is not revoked, not expired, and within usage limits
     */
    public boolean isValid() {
        if (revoked) {
            return false;
        }
        if (isExpired()) {
            return false;
        }
        if (maxUsage != null && usageCount >= maxUsage) {
            return false;
        }
        return true;
    }

    /**
     * Returns the time until this token expires.
     *
     * @return duration until expiration, or null if token doesn't expire
     */
    public Duration getTimeUntilExpiration() {
        if (expiresAt == null) {
            return null;
        }
        Duration remaining = Duration.between(Instant.now(), expiresAt);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    /**
     * Returns the remaining usage count.
     *
     * @return remaining uses, or -1 if unlimited
     */
    public long getRemainingUsage() {
        if (maxUsage == null) {
            return -1;
        }
        return Math.max(0, maxUsage - usageCount);
    }

    /**
     * Checks if this token has a usage limit.
     *
     * @return true if a usage limit is set
     */
    public boolean hasUsageLimit() {
        return maxUsage != null;
    }

    /**
     * Checks if this token has a policy.
     *
     * @return true if a policy is associated
     */
    public boolean hasPolicy() {
        return policy != null || (policyId != null && !policyId.isEmpty());
    }

    /**
     * Returns the full bunker connection string using this token.
     *
     * @return the connection string
     */
    public String getConnectionString() {
        if (keyNpub == null || token == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(BunkerConnection.BUNKER_SCHEME)
                .append("://")
                .append(keyNpub);

        if (relay != null && !relay.isEmpty()) {
            sb.append("?relay=").append(relay);
            sb.append("&secret=").append(token);
        } else {
            sb.append("?secret=").append(token);
        }

        return sb.toString();
    }

    /**
     * Returns the full token string including any prefix.
     *
     * @return the full token
     */
    public String getFullToken() {
        return token;
    }
}
