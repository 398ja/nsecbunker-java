package xyz.tcheeric.nsecbunker.monitoring.logging;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Filter criteria for log queries.
 */
@Value
@Builder(toBuilder = true)
public class LogFilter {
    Instant since;
    Instant until;
    @Builder.Default
    List<String> methods = Collections.emptyList();
    @Builder.Default
    List<String> users = Collections.emptyList();
    Integer limit;

    public boolean hasMethods() {
        return methods != null && !methods.isEmpty();
    }

    public boolean hasUsers() {
        return users != null && !users.isEmpty();
    }
}
