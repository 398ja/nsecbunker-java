package xyz.tcheeric.nsecbunker.monitoring.logging;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Reads signing logs from nsecBunker.
 */
public interface LogReader {

    /**
     * Retrieves logs for a key using the given filter.
     */
    CompletableFuture<List<LogEntry>> getLogs(String keyName, LogFilter filter);

    /**
     * Exports logs in the requested format.
     */
    CompletableFuture<String> exportLogs(String keyName, LogFilter filter, LogReader.Format format);

    /**
     * Streams logs (single shot in this implementation) to the consumer.
     */
    CompletableFuture<Void> streamLogs(String keyName, LogFilter filter, Consumer<List<LogEntry>> consumer);

    enum Format {
        JSON,
        CSV
    }
}
