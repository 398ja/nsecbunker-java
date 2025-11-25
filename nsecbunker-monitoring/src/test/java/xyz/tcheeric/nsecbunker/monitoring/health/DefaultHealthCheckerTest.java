package xyz.tcheeric.nsecbunker.monitoring.health;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.tcheeric.nsecbunker.admin.NsecBunkerAdminClient;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultHealthCheckerTest {

    @Mock
    private NsecBunkerAdminClient adminClient;

    /**
     * Tests that successful ping sets status UP.
     */
    @Test
    void shouldReportUpOnPong() {
        when(adminClient.ping()).thenReturn(CompletableFuture.completedFuture("pong"));

        DefaultHealthChecker checker = new DefaultHealthChecker(adminClient);
        HealthCheckResult result = checker.checkBunker().join();

        assertThat(result.getStatus()).isEqualTo(HealthStatus.UP);
        assertThat(result.getMessage()).contains("OK");
    }

    /**
     * Tests that errors set status DOWN.
     */
    @Test
    void shouldReportDownOnError() {
        when(adminClient.ping()).thenReturn(CompletableFuture.failedFuture(new RuntimeException("fail")));

        DefaultHealthChecker checker = new DefaultHealthChecker(adminClient);
        HealthCheckResult result = checker.checkBunker().join();

        assertThat(result.getStatus()).isEqualTo(HealthStatus.DOWN);
        assertThat(result.getMessage()).contains("fail");
    }
}
