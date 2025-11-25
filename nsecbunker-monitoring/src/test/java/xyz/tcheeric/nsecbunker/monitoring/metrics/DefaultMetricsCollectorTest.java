package xyz.tcheeric.nsecbunker.monitoring.metrics;

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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultMetricsCollectorTest {

    @Mock
    private NsecBunkerAdminClient adminClient;

    /**
     * Ensures getKeyStatistics parses a metrics record.
     */
    @Test
    void shouldGetKeyStatistics() throws Exception {
        // Arrange
        MetricsRecord record = MetricsRecord.builder()
                .id("key1")
                .keyName("key1")
                .count(10)
                .failures(1)
                .lastUpdated(Instant.parse("2024-01-01T00:00:00Z"))
                .build();
        ObjectMapper mapper = DefaultMetricsCollector.createDefaultObjectMapper();
        String json = mapper.writeValueAsString(record);
        ArgumentCaptor<Nip46Request> captor = ArgumentCaptor.forClass(Nip46Request.class);
        when(adminClient.sendRequest(captor.capture()))
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.success("1", json)));

        DefaultMetricsCollector collector = new DefaultMetricsCollector(adminClient, mapper);

        // Act
        MetricsRecord result = collector.getKeyStatistics("key1").join();

        // Assert
        assertThat(result).isEqualTo(record);
        assertThat(captor.getValue().getMethod()).isEqualTo("get_key_statistics");
    }

    /**
     * Ensures errors surface as AdminException.
     */
    @Test
    void shouldSurfaceErrors() {
        when(adminClient.sendRequest(org.mockito.ArgumentMatchers.any()))
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.error("1", "ERR", "bad")));

        DefaultMetricsCollector collector = new DefaultMetricsCollector(adminClient);

        assertThatThrownBy(() -> collector.getTokenStatistics("t1").join())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(AdminException.class);
    }
}
