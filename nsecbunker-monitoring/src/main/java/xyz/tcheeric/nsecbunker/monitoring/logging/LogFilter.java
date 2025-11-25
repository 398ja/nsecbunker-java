package xyz.tcheeric.nsecbunker.monitoring.logging;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Filter criteria for log queries.
 *
 * <p>Use this to filter log entries by time range, methods, users, or limit results.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * LogFilter filter = LogFilter.builder()
 *     .since(Instant.now().minus(Duration.ofHours(1)))
 *     .methods(List.of("sign_event"))
 *     .limit(100)
 *     .build();
 * }</pre>
 */
@Value
@Builder(toBuilder = true)
public class LogFilter {

    /**
     * Start of time range (inclusive). Null means no lower bound.
     */
    Instant since;

    /**
     * End of time range (inclusive). Null means no upper bound.
     */
    Instant until;

    /**
     * Filter to specific NIP-46 methods. Empty list means all methods.
     */
    @Builder.Default
    List<String> methods = Collections.emptyList();

    /**
     * Filter to specific user public keys. Empty list means all users.
     */
    @Builder.Default
    List<String> users = Collections.emptyList();

    /**
     * Maximum number of entries to return. Null means no limit.
     */
    Integer limit;

    /**
     * Checks if method filtering is active.
     *
     * @return true if filtering by specific methods
     */
    public boolean hasMethods() {
        return methods != null && !methods.isEmpty();
    }

    /**
     * Checks if user filtering is active.
     *
     * @return true if filtering by specific users
     */
    public boolean hasUsers() {
        return users != null && !users.isEmpty();
    }
}
