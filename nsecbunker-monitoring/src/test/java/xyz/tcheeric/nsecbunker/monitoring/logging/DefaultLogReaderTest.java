package xyz.tcheeric.nsecbunker.monitoring.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.tcheeric.nsecbunker.admin.AdminException;
import xyz.tcheeric.nsecbunker.admin.NsecBunkerAdminClient;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Request;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Response;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultLogReaderTest {

    @Mock
    private NsecBunkerAdminClient adminClient;

    /**
     * Ensures getLogs parses responses into log entries.
     */
    @Test
    void shouldGetLogs() throws Exception {
        // Arrange
        LogEntry entry = LogEntry.builder()
                .timestamp(Instant.parse("2024-01-01T00:00:00Z"))
                .keyName("k1")
                .userPubkey("u1")
                .method("sign_event")
                .result("ok")
                .build();
        ObjectMapper mapper = DefaultLogReader.createDefaultObjectMapper();
        String json = mapper.writeValueAsString(List.of(entry));

        ArgumentCaptor<Nip46Request> captor = ArgumentCaptor.forClass(Nip46Request.class);
        when(adminClient.sendRequest(captor.capture()))
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.success("1", json)));

        DefaultLogReader reader = new DefaultLogReader(adminClient, mapper);

        // Act
        List<LogEntry> logs = reader.getLogs("k1", LogFilter.builder().build()).join();

        // Assert
        assertThat(logs).containsExactly(entry);
        Nip46Request sent = captor.getValue();
        assertThat(sent.getMethod()).isEqualTo("get_logs");
        assertThat(sent.getParams()).hasSize(2);
    }

    /**
     * Ensures exportLogs returns JSON and CSV.
     */
    @Test
    void shouldExportJsonAndCsv() throws Exception {
        // Arrange
        LogEntry entry = LogEntry.builder()
                .timestamp(Instant.parse("2024-01-01T00:00:00Z"))
                .keyName("k1")
                .userPubkey("u1")
                .method("sign_event")
                .result("ok")
                .build();
        ObjectMapper mapper = DefaultLogReader.createDefaultObjectMapper();
        String json = mapper.writeValueAsString(List.of(entry));

        when(adminClient.sendRequest(org.mockito.ArgumentMatchers.any()))
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.success("1", json)));

        DefaultLogReader reader = new DefaultLogReader(adminClient, mapper);

        // Act
        String exportedJson = reader.exportLogs("k1", LogFilter.builder().build(), LogReader.Format.JSON).join();
        String exportedCsv = reader.exportLogs("k1", LogFilter.builder().build(), LogReader.Format.CSV).join();

        // Assert
        assertThat(exportedJson).contains("sign_event");
        assertThat(exportedCsv).contains("timestamp").contains("sign_event");
    }

    /**
     * Ensures streamLogs invokes the consumer.
     */
    @Test
    void shouldStreamLogs() throws Exception {
        // Arrange
        LogEntry entry = LogEntry.builder()
                .timestamp(Instant.parse("2024-01-01T00:00:00Z"))
                .method("ping")
                .build();
        ObjectMapper mapper = DefaultLogReader.createDefaultObjectMapper();
        String json = mapper.writeValueAsString(List.of(entry));
        when(adminClient.sendRequest(org.mockito.ArgumentMatchers.any()))
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.success("1", json)));

        DefaultLogReader reader = new DefaultLogReader(adminClient, mapper);
        Consumer<List<LogEntry>> consumer = logs -> assertThat(logs).contains(entry);

        // Act / Assert
        reader.streamLogs("k1", LogFilter.builder().build(), consumer).join();
    }

    /**
     * Ensures errors are surfaced as AdminException.
     */
    @Test
    void shouldSurfaceErrors() {
        // Arrange
        when(adminClient.sendRequest(org.mockito.ArgumentMatchers.any()))
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.error("1", "ERR", "bad")));

        DefaultLogReader reader = new DefaultLogReader(adminClient);

        // Act + Assert
        assertThatThrownBy(() -> reader.getLogs("k1", LogFilter.builder().build()).join())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(AdminException.class);
    }
}
