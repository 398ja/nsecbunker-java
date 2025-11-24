package xyz.tcheeric.nsecbunker.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Singular;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Represents a policy that defines permissions for key access in nsecBunker.
 *
 * <p>A policy contains a set of rules that determine what operations
 * a key user is allowed or denied to perform.
 */
@Getter
@Builder(toBuilder = true)
@Jacksonized
@EqualsAndHashCode
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public final class BunkerPolicy {

    /**
     * Unique identifier for this policy.
     */
    private final String id;

    /**
     * Human-readable name of the policy.
     */
    private final String name;

    /**
     * Optional description of the policy.
     */
    private final String description;

    /**
     * The list of rules that define this policy.
     */
    @Singular
    private final List<PolicyRule> rules;

    /**
     * When this policy was created.
     */
    @JsonProperty("created_at")
    private final Instant createdAt;

    /**
     * When this policy expires. If null, the policy does not expire.
     */
    @JsonProperty("expires_at")
    private final Instant expiresAt;

    /**
     * Whether this policy is currently active.
     */
    @Builder.Default
    private final boolean active = true;

    /**
     * Creates a new policy builder with the given name.
     *
     * @param name the policy name
     * @return a new builder instance
     */
    public static BunkerPolicyBuilder withName(String name) {
        return builder().name(name);
    }

    /**
     * Returns an unmodifiable view of the rules list.
     *
     * @return the policy rules
     */
    public List<PolicyRule> getRules() {
        return rules == null ? Collections.emptyList() : Collections.unmodifiableList(rules);
    }

    /**
     * Checks if this policy has any rules.
     *
     * @return true if the policy has at least one rule
     */
    public boolean hasRules() {
        return rules != null && !rules.isEmpty();
    }

    /**
     * Checks if this policy has expired.
     *
     * @return true if the policy has an expiration date and it has passed
     */
    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    /**
     * Checks if this policy is currently valid (active and not expired).
     *
     * @return true if the policy is valid
     */
    public boolean isValid() {
        return active && !isExpired();
    }

    /**
     * Returns the time until this policy expires.
     *
     * @return duration until expiration, or null if policy doesn't expire
     */
    public Duration getTimeUntilExpiration() {
        if (expiresAt == null) {
            return null;
        }
        return Duration.between(Instant.now(), expiresAt);
    }

    /**
     * Checks if a method is allowed by this policy.
     *
     * @param method the method to check
     * @return true if the method is allowed
     */
    public boolean isMethodAllowed(String method) {
        if (!isValid() || rules == null) {
            return false;
        }

        boolean hasExplicitAllow = false;
        for (PolicyRule rule : rules) {
            if (rule.appliesTo(method)) {
                if (rule.isDeny()) {
                    return false;
                }
                if (rule.isAllow() && !rule.isUsageLimitReached()) {
                    hasExplicitAllow = true;
                }
            }
        }
        return hasExplicitAllow;
    }

    /**
     * Checks if an event kind is allowed by this policy.
     *
     * @param eventKind the event kind to check
     * @return true if the event kind is allowed
     */
    public boolean isEventKindAllowed(int eventKind) {
        if (!isValid() || rules == null) {
            return false;
        }

        boolean hasExplicitAllow = false;
        for (PolicyRule rule : rules) {
            if (rule.appliesTo(eventKind)) {
                if (rule.isDeny()) {
                    return false;
                }
                if (rule.isAllow() && !rule.isUsageLimitReached()) {
                    hasExplicitAllow = true;
                }
            }
        }
        return hasExplicitAllow;
    }

    /**
     * Returns the number of rules in this policy.
     *
     * @return the rule count
     */
    public int getRuleCount() {
        return rules == null ? 0 : rules.size();
    }
}
