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
 * Represents an Access Control List (ACL) condition for signing operations.
 *
 * <p>Signing conditions define fine-grained rules about what a {@link KeyUser}
 * can sign. They can restrict signing by event kind, time window, recipient,
 * or other criteria.
 */
@Getter
@Builder(toBuilder = true)
@Jacksonized
@EqualsAndHashCode
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public final class SigningCondition {

    /**
     * The type of condition.
     */
    public enum ConditionType {
        /**
         * Allows signing events of specific kinds.
         */
        EVENT_KIND,

        /**
         * Allows specific NIP-46 methods.
         */
        METHOD,

        /**
         * Time-based restriction.
         */
        TIME_WINDOW,

        /**
         * Rate limiting.
         */
        RATE_LIMIT,

        /**
         * Recipient-based restriction (for encryption).
         */
        RECIPIENT,

        /**
         * Content pattern restriction.
         */
        CONTENT_PATTERN,

        /**
         * Custom condition type.
         */
        CUSTOM
    }

    /**
     * The type of this condition.
     */
    private final ConditionType type;

    /**
     * The NIP-46 method this condition applies to (e.g., "sign_event").
     */
    private final String method;

    /**
     * The event kind this condition allows or restricts.
     */
    @JsonProperty("event_kind")
    private final Integer eventKind;

    /**
     * The start of a time window during which signing is allowed.
     */
    @JsonProperty("valid_from")
    private final Instant validFrom;

    /**
     * The end of a time window during which signing is allowed.
     */
    @JsonProperty("valid_until")
    private final Instant validUntil;

    /**
     * Maximum number of operations allowed within the rate limit window.
     */
    @JsonProperty("max_operations")
    private final Integer maxOperations;

    /**
     * The time window for rate limiting (in seconds).
     */
    @JsonProperty("rate_limit_window_seconds")
    private final Integer rateLimitWindowSeconds;

    /**
     * Allowed recipient pubkeys for encryption operations.
     */
    @JsonProperty("allowed_recipient")
    private final String allowedRecipient;

    /**
     * A regex pattern that content must match.
     */
    @JsonProperty("content_pattern")
    private final String contentPattern;

    /**
     * Whether this condition allows or denies the operation.
     */
    @Builder.Default
    @JsonProperty("is_allow")
    private final boolean allow = true;

    /**
     * Optional description of this condition.
     */
    private final String description;

    /**
     * Creates a condition that allows a specific event kind.
     *
     * @param eventKind the event kind to allow
     * @return a new SigningCondition
     */
    public static SigningCondition allowEventKind(int eventKind) {
        return builder()
                .type(ConditionType.EVENT_KIND)
                .eventKind(eventKind)
                .allow(true)
                .build();
    }

    /**
     * Creates a condition that allows a specific method.
     *
     * @param method the method to allow
     * @return a new SigningCondition
     */
    public static SigningCondition allowMethod(String method) {
        return builder()
                .type(ConditionType.METHOD)
                .method(method)
                .allow(true)
                .build();
    }

    /**
     * Creates a time-based condition.
     *
     * @param from  start of valid time window
     * @param until end of valid time window
     * @return a new SigningCondition
     */
    public static SigningCondition timeWindow(Instant from, Instant until) {
        return builder()
                .type(ConditionType.TIME_WINDOW)
                .validFrom(from)
                .validUntil(until)
                .allow(true)
                .build();
    }

    /**
     * Creates a rate limit condition.
     *
     * @param maxOperations maximum operations allowed
     * @param windowSeconds time window in seconds
     * @return a new SigningCondition
     */
    public static SigningCondition rateLimit(int maxOperations, int windowSeconds) {
        return builder()
                .type(ConditionType.RATE_LIMIT)
                .maxOperations(maxOperations)
                .rateLimitWindowSeconds(windowSeconds)
                .allow(true)
                .build();
    }

    /**
     * Checks if this condition is currently within its valid time window.
     *
     * @return true if within the time window or no time restriction exists
     */
    public boolean isWithinTimeWindow() {
        Instant now = Instant.now();
        if (validFrom != null && now.isBefore(validFrom)) {
            return false;
        }
        if (validUntil != null && now.isAfter(validUntil)) {
            return false;
        }
        return true;
    }

    /**
     * Checks if this condition applies to the given event kind.
     *
     * @param kind the event kind to check
     * @return true if this condition applies
     */
    public boolean appliesTo(int kind) {
        return eventKind == null || eventKind == kind;
    }

    /**
     * Checks if this condition applies to the given method.
     *
     * @param targetMethod the method to check
     * @return true if this condition applies
     */
    public boolean appliesTo(String targetMethod) {
        return method == null || method.equals(targetMethod);
    }

    /**
     * Checks if content matches the pattern (if any).
     *
     * @param content the content to check
     * @return true if content matches or no pattern is set
     */
    public boolean matchesContentPattern(String content) {
        if (contentPattern == null || contentPattern.isEmpty()) {
            return true;
        }
        return content != null && content.matches(contentPattern);
    }

    /**
     * Checks if this is an allow condition.
     *
     * @return true if this condition allows the operation
     */
    public boolean isAllowCondition() {
        return allow;
    }

    /**
     * Checks if this is a deny condition.
     *
     * @return true if this condition denies the operation
     */
    public boolean isDenyCondition() {
        return !allow;
    }
}
