package xyz.tcheeric.nsecbunker.starter.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import xyz.tcheeric.nsecbunker.client.signer.NsecBunkerSigner;
import xyz.tcheeric.nsecbunker.starter.NsecBunkerProperties;

/**
 * Auto-configuration for nsecBunker signing metrics with Micrometer.
 *
 * <p>This configuration is enabled when:
 * <ul>
 *   <li>Micrometer is on the classpath</li>
 *   <li>A MeterRegistry bean exists</li>
 *   <li>The property {@code nsecbunker.metrics.enabled} is true (default)</li>
 * </ul>
 *
 * <p>Metrics provided:
 * <ul>
 *   <li>{@code nsecbunker.signing.total} - Total signing operations</li>
 *   <li>{@code nsecbunker.signing.success} - Successful signing operations</li>
 *   <li>{@code nsecbunker.signing.errors} - Failed signing operations (with error type tag)</li>
 *   <li>{@code nsecbunker.signing.latency} - Signing latency distribution</li>
 *   <li>{@code nsecbunker.signing.active} - Currently active signing operations</li>
 *   <li>{@code nsecbunker.encryption.total} - Encryption operations (with NIP type tag)</li>
 *   <li>{@code nsecbunker.decryption.total} - Decryption operations (with NIP type tag)</li>
 *   <li>{@code nsecbunker.connection.status} - Connection status (1=connected, 0=disconnected)</li>
 *   <li>{@code nsecbunker.ping.latency} - Ping latency distribution</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>{@code
 * @Autowired
 * private SigningMetrics signingMetrics;
 *
 * public void sign(String eventJson) {
 *     signingMetrics.recordSignEvent(eventJson)
 *         .thenAccept(signed -> processSignedEvent(signed));
 * }
 * }</pre>
 */
@AutoConfiguration(after = {MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class})
@ConditionalOnClass({MeterRegistry.class, NsecBunkerSigner.class})
@ConditionalOnProperty(prefix = "nsecbunker.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SigningMetricsAutoConfiguration {

    /**
     * Creates SigningMetrics bean and binds it to the MeterRegistry.
     *
     * @param registry   the Micrometer meter registry
     * @param signer     the nsecBunker signer (optional)
     * @param properties the nsecBunker properties
     * @return the SigningMetrics instance
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(MeterRegistry.class)
    public SigningMetrics signingMetrics(
            MeterRegistry registry,
            NsecBunkerSigner signer,
            NsecBunkerProperties properties) {
        SigningMetrics metrics = new SigningMetrics(signer);
        metrics.bindTo(registry);
        return metrics;
    }
}
