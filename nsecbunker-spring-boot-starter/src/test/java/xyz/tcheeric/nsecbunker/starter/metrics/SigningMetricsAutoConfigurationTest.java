package xyz.tcheeric.nsecbunker.starter.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.tcheeric.nsecbunker.client.signer.NsecBunkerSigner;
import xyz.tcheeric.nsecbunker.starter.NsecBunkerProperties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SigningMetricsAutoConfiguration}.
 */
@ExtendWith(MockitoExtension.class)
class SigningMetricsAutoConfigurationTest {

    @Mock
    private NsecBunkerSigner mockSigner;

    @Test
    void shouldCreateSigningMetricsBeanAndBindToRegistry() {
        SigningMetricsAutoConfiguration config = new SigningMetricsAutoConfiguration();
        MeterRegistry registry = new SimpleMeterRegistry();
        NsecBunkerProperties properties = new NsecBunkerProperties();

        SigningMetrics metrics = config.signingMetrics(registry, mockSigner, properties);

        assertThat(metrics).isNotNull();
        // Verify that meters are registered
        assertThat(registry.find("nsecbunker.signing.total").counter()).isNotNull();
        assertThat(registry.find("nsecbunker.signing.success").counter()).isNotNull();
        assertThat(registry.find("nsecbunker.signing.latency").timer()).isNotNull();
        assertThat(registry.find("nsecbunker.connection.status").gauge()).isNotNull();
    }

    @Test
    void shouldReturnSigningMetricsInstance() {
        SigningMetricsAutoConfiguration config = new SigningMetricsAutoConfiguration();
        MeterRegistry registry = new SimpleMeterRegistry();
        NsecBunkerProperties properties = new NsecBunkerProperties();

        SigningMetrics metrics = config.signingMetrics(registry, mockSigner, properties);

        assertThat(metrics).isInstanceOf(SigningMetrics.class);
    }
}
