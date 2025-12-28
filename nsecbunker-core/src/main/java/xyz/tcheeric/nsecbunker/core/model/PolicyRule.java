package xyz.tcheeric.nsecbunker.core.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

/**
 * Represents a rule within a {@link BunkerPolicy}.
 *
 * <p>Policy rules define what operations are allowed or denied for a key user.
 * Rules can restrict by method (e.g., sign_event, encrypt), event kind,
 * or usage limits.
 */
@Getter
@Builder(toBuilder = true)
@Jacksonized
@EqualsAndHashCode
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public final class PolicyRule {

    /**
     * The type of rule: ALLOW or DENY.
     */
    public enum RuleType {
        ALLOW,
        DENY
    }

    /**
     * The type of this rule (allow or deny).
     */
    @Builder.Default
    private final RuleType type = RuleType.ALLOW;

    /**
     * The NIP-46 method this rule applies to (e.g., "sign_event", "encrypt", "decrypt").
     * If null, the rule applies to all methods.
     */
    private final String method;

    /**
     * The Nostr event kind this rule applies to (e.g., 1 for text notes, 4 for DMs).
     * If null, the rule applies to all event kinds.
     * Note: nsecbunkerd uses "kind" for input but "kind" in response too
     */
    @JsonProperty("kind")
    private final Integer eventKind;

    /**
     * Maximum number of times this operation can be performed.
     * If null, there is no usage limit.
     * Note: nsecbunkerd uses "use_count" for input but "max_usage_count" in response
     */
    @JsonProperty("use_count")
    private final Long maxUsage;

    /**
     * Current usage count for this rule.
     * Note: nsecbunkerd uses "current_usage_count" in response
     */
    @Builder.Default
    @JsonProperty("current_usage_count")
    private final long currentUsage = 0;

    /**
     * Optional description of this rule.
     */
    private final String description;

    /**
     * Creates an ALLOW rule for a specific method.
     *
     * @param method the method to allow
     * @return a new PolicyRule
     */
    public static PolicyRule allowMethod(String method) {
        return builder()
                .type(RuleType.ALLOW)
                .method(method)
                .build();
    }

    /**
     * Creates an ALLOW rule for a specific event kind.
     *
     * @param eventKind the event kind to allow
     * @return a new PolicyRule
     */
    public static PolicyRule allowEventKind(int eventKind) {
        return builder()
                .type(RuleType.ALLOW)
                .eventKind(eventKind)
                .build();
    }

    /**
     * Creates a DENY rule for a specific method.
     *
     * @param method the method to deny
     * @return a new PolicyRule
     */
    public static PolicyRule denyMethod(String method) {
        return builder()
                .type(RuleType.DENY)
                .method(method)
                .build();
    }

    /**
     * Creates a DENY rule for a specific event kind.
     *
     * @param eventKind the event kind to deny
     * @return a new PolicyRule
     */
    public static PolicyRule denyEventKind(int eventKind) {
        return builder()
                .type(RuleType.DENY)
                .eventKind(eventKind)
                .build();
    }

    /**
     * Checks if this rule has a usage limit.
     *
     * @return true if a usage limit is set
     */
    public boolean hasUsageLimit() {
        return maxUsage != null && maxUsage > 0;
    }

    /**
     * Checks if the usage limit has been reached.
     *
     * @return true if usage limit is exceeded or reached
     */
    public boolean isUsageLimitReached() {
        return hasUsageLimit() && currentUsage >= maxUsage;
    }

    /**
     * Returns the remaining usage count.
     *
     * @return remaining usage, or -1 if no limit is set
     */
    public long getRemainingUsage() {
        if (!hasUsageLimit()) {
            return -1;
        }
        return Math.max(0, maxUsage - currentUsage);
    }

    /**
     * Checks if this rule applies to the given method.
     *
     * @param targetMethod the method to check
     * @return true if this rule applies
     */
    public boolean appliesTo(String targetMethod) {
        return method == null || method.equals(targetMethod);
    }

    /**
     * Checks if this rule applies to the given event kind.
     *
     * @param targetKind the event kind to check
     * @return true if this rule applies
     */
    public boolean appliesTo(int targetKind) {
        return eventKind == null || eventKind == targetKind;
    }

    /**
     * Checks if this rule is an ALLOW rule.
     *
     * @return true if this is an allow rule
     */
    public boolean isAllow() {
        return type == RuleType.ALLOW;
    }

    /**
     * Checks if this rule is a DENY rule.
     *
     * @return true if this is a deny rule
     */
    public boolean isDeny() {
        return type == RuleType.DENY;
    }
}
