package xyz.tcheeric.nsecbunker.core.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Singular;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Represents a user who has been granted access to a key in nsecBunker.
 *
 * <p>A KeyUser is associated with a specific bunker key and has a set of
 * signing conditions that define what operations they can perform.
 */
@Getter
@Builder(toBuilder = true)
@Jacksonized
@EqualsAndHashCode
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public final class KeyUser {

    /**
     * Unique identifier for this key user (from nsecbunkerd database).
     */
    private final String id;

    /**
     * The public key of this user (in hex format).
     * nsecbunkerd uses "user_pubkey" in grant_permission response, "userPubkey" in get_key_users.
     */
    @JsonProperty("pubkey")
    @JsonAlias({"user_pubkey", "userPubkey"})
    private final String pubkeyHex;

    /**
     * The public key of this user in npub (bech32) format.
     */
    private final String npub;

    /**
     * The name of the key this user has access to.
     */
    @JsonProperty("key_name")
    private final String keyName;

    /**
     * Human-readable description or name for this user.
     */
    private final String description;

    /**
     * The policy associated with this user's access.
     */
    private final BunkerPolicy policy;

    /**
     * The ID of the policy associated with this user, if not fully loaded.
     */
    @JsonProperty("policy_id")
    private final String policyId;

    /**
     * The signing conditions (ACL rules) for this user.
     */
    @Singular
    @JsonProperty("signing_conditions")
    private final List<SigningCondition> signingConditions;

    /**
     * When this user was granted access.
     */
    @JsonProperty("created_at")
    private final Instant createdAt;

    /**
     * When this user's access was last modified.
     */
    @JsonProperty("updated_at")
    private final Instant updatedAt;

    /**
     * When this user last performed a signing operation.
     */
    @JsonProperty("last_used_at")
    private final Instant lastUsedAt;

    /**
     * Total number of signing operations performed by this user.
     */
    @Builder.Default
    @JsonProperty("signing_count")
    private final long signingCount = 0;

    /**
     * Whether this user's access is currently active.
     * nsecbunkerd may use "active", "is_active", or "enabled" to indicate user status.
     */
    @Builder.Default
    @JsonAlias({"is_active", "enabled"})
    private final boolean active = true;

    /**
     * Creates a new KeyUser builder for the given pubkey.
     *
     * @param pubkeyHex the user's public key in hex format
     * @return a new builder instance
     */
    public static KeyUserBuilder forPubkey(String pubkeyHex) {
        return builder().pubkeyHex(pubkeyHex);
    }

    /**
     * Returns the user's public key, preferring hex format.
     *
     * @return the public key
     */
    public String getPublicKey() {
        return pubkeyHex != null ? pubkeyHex : npub;
    }

    /**
     * Returns an unmodifiable view of the signing conditions.
     *
     * @return the signing conditions
     */
    public List<SigningCondition> getSigningConditions() {
        return signingConditions == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(signingConditions);
    }

    /**
     * Checks if this user has a policy assigned.
     *
     * @return true if a policy is assigned
     */
    public boolean hasPolicy() {
        return policy != null || (policyId != null && !policyId.isEmpty());
    }

    /**
     * Checks if this user has any signing conditions.
     *
     * @return true if signing conditions exist
     */
    public boolean hasSigningConditions() {
        return signingConditions != null && !signingConditions.isEmpty();
    }

    /**
     * Checks if this user has been used for signing.
     *
     * @return true if the user has performed signing operations
     */
    public boolean hasBeenUsed() {
        return signingCount > 0 || lastUsedAt != null;
    }

    /**
     * Checks if this user's access is currently valid.
     *
     * @return true if the user is active and any associated policy is valid
     */
    public boolean isAccessValid() {
        if (!active) {
            return false;
        }
        if (policy != null && !policy.isValid()) {
            return false;
        }
        return true;
    }
}
