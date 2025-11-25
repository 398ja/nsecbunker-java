package xyz.tcheeric.nsecbunker.starter.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.tcheeric.nsecbunker.client.signer.NsecBunkerSigner;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link SigningMetrics}.
 */
@ExtendWith(MockitoExtension.class)
class SigningMetricsTest {

    @Mock
    private NsecBunkerSigner mockSigner;

    private MeterRegistry registry;
    private SigningMetrics signingMetrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        signingMetrics = new SigningMetrics(mockSigner);
        signingMetrics.bindTo(registry);
    }

    @Test
    @DisplayName("Registers all expected meters on bind")
    void registersAllMetersOnBind() {
        // Counters
        assertThat(registry.find("nsecbunker.signing.total").counter()).isNotNull();
        assertThat(registry.find("nsecbunker.signing.success").counter()).isNotNull();

        // Timer
        assertThat(registry.find("nsecbunker.signing.latency").timer()).isNotNull();
        assertThat(registry.find("nsecbunker.ping.latency").timer()).isNotNull();

        // Gauges
        assertThat(registry.find("nsecbunker.signing.active").gauge()).isNotNull();
        assertThat(registry.find("nsecbunker.connection.status").gauge()).isNotNull();
    }

    @Test
    @DisplayName("Records successful signing operation")
    void recordsSuccessfulSigning() throws Exception {
        when(mockSigner.signEvent(anyString()))
                .thenReturn(CompletableFuture.completedFuture("{\"sig\":\"abc\"}"));

        CompletableFuture<String> result = signingMetrics.recordSignEvent("{\"kind\":1}");
        result.get(5, TimeUnit.SECONDS);

        Counter total = registry.find("nsecbunker.signing.total").counter();
        Counter success = registry.find("nsecbunker.signing.success").counter();
        Timer latency = registry.find("nsecbunker.signing.latency").timer();

        assertThat(total.count()).isEqualTo(1);
        assertThat(success.count()).isEqualTo(1);
        assertThat(latency.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Records failed signing operation")
    void recordsFailedSigning() {
        when(mockSigner.signEvent(anyString()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Signing failed")));

        CompletableFuture<String> result = signingMetrics.recordSignEvent("{\"kind\":1}");
        result.exceptionally(ex -> null).join();

        Counter total = registry.find("nsecbunker.signing.total").counter();
        Counter success = registry.find("nsecbunker.signing.success").counter();

        assertThat(total.count()).isEqualTo(1);
        assertThat(success.count()).isEqualTo(0);

        // Error counter should be recorded
        Counter errors = registry.find("nsecbunker.signing.errors")
                .tag("method", "sign_event")
                .counter();
        assertThat(errors).isNotNull();
        assertThat(errors.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Records encryption operation")
    void recordsEncryptionOperation() throws Exception {
        when(mockSigner.nip04Encrypt(anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture("encrypted"));

        CompletableFuture<String> result = signingMetrics.recordEncrypt("pubkey", "plaintext", false);
        result.get(5, TimeUnit.SECONDS);

        Counter encryptCounter = registry.find("nsecbunker.encryption.total")
                .tag("type", "nip04")
                .counter();

        assertThat(encryptCounter).isNotNull();
        assertThat(encryptCounter.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Records NIP-44 encryption operation")
    void recordsNip44EncryptionOperation() throws Exception {
        when(mockSigner.nip44Encrypt(anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture("encrypted"));

        CompletableFuture<String> result = signingMetrics.recordEncrypt("pubkey", "plaintext", true);
        result.get(5, TimeUnit.SECONDS);

        Counter encryptCounter = registry.find("nsecbunker.encryption.total")
                .tag("type", "nip44")
                .counter();

        assertThat(encryptCounter).isNotNull();
        assertThat(encryptCounter.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Records decryption operation")
    void recordsDecryptionOperation() throws Exception {
        when(mockSigner.nip04Decrypt(anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture("decrypted"));

        CompletableFuture<String> result = signingMetrics.recordDecrypt("pubkey", "ciphertext", false);
        result.get(5, TimeUnit.SECONDS);

        Counter decryptCounter = registry.find("nsecbunker.decryption.total")
                .tag("type", "nip04")
                .counter();

        assertThat(decryptCounter).isNotNull();
        assertThat(decryptCounter.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Records ping operation with latency")
    void recordsPingOperation() throws Exception {
        when(mockSigner.ping())
                .thenReturn(CompletableFuture.completedFuture("pong"));

        CompletableFuture<String> result = signingMetrics.recordPing();
        result.get(5, TimeUnit.SECONDS);

        Timer pingLatency = registry.find("nsecbunker.ping.latency").timer();

        assertThat(pingLatency).isNotNull();
        assertThat(pingLatency.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Records get_public_key operation")
    void recordsGetPublicKeyOperation() throws Exception {
        when(mockSigner.getPublicKey())
                .thenReturn(CompletableFuture.completedFuture("abc123pubkey"));

        CompletableFuture<String> result = signingMetrics.recordGetPublicKey();
        result.get(5, TimeUnit.SECONDS);

        Counter counter = registry.find("nsecbunker.get_public_key.total").counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Tracks active operations")
    void tracksActiveOperations() {
        // Use a delayed future to check active operations during execution
        CompletableFuture<String> delayedFuture = new CompletableFuture<>();
        when(mockSigner.signEvent(anyString())).thenReturn(delayedFuture);

        signingMetrics.recordSignEvent("{\"kind\":1}");

        assertThat(signingMetrics.getActiveOperations()).isEqualTo(1);

        // Complete the future
        delayedFuture.complete("{\"sig\":\"abc\"}");

        // Wait for completion
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        assertThat(signingMetrics.getActiveOperations()).isEqualTo(0);
    }

    @Test
    @DisplayName("Reports connection status")
    void reportsConnectionStatus() {
        when(mockSigner.isConnected()).thenReturn(false);

        Gauge connectionGauge = registry.find("nsecbunker.connection.status").gauge();
        assertThat(connectionGauge).isNotNull();
        assertThat(connectionGauge.value()).isEqualTo(0.0);

        when(mockSigner.isConnected()).thenReturn(true);
        assertThat(connectionGauge.value()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Manual recording of signing success")
    void manualRecordingOfSigningSuccess() {
        signingMetrics.recordSigningSuccess(1_000_000); // 1ms in nanos

        Counter total = registry.find("nsecbunker.signing.total").counter();
        Counter success = registry.find("nsecbunker.signing.success").counter();
        Timer latency = registry.find("nsecbunker.signing.latency").timer();

        assertThat(total.count()).isEqualTo(1);
        assertThat(success.count()).isEqualTo(1);
        assertThat(latency.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Manual recording of signing error")
    void manualRecordingOfSigningError() {
        signingMetrics.recordSigningError("sign_event", "timeout");

        Counter total = registry.find("nsecbunker.signing.total").counter();
        Counter errors = registry.find("nsecbunker.signing.errors")
                .tag("method", "sign_event")
                .tag("error_type", "timeout")
                .counter();

        assertThat(total.count()).isEqualTo(1);
        assertThat(errors).isNotNull();
        assertThat(errors.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Fails gracefully when signer is null")
    void failsGracefullyWhenSignerIsNull() {
        SigningMetrics metricsWithNullSigner = new SigningMetrics((NsecBunkerSigner) null);
        metricsWithNullSigner.bindTo(registry);

        CompletableFuture<String> result = metricsWithNullSigner.recordSignEvent("{\"kind\":1}");

        assertThat(result.isCompletedExceptionally()).isTrue();
    }

    @Test
    @DisplayName("Categorizes timeout errors correctly")
    void categorizesTimeoutErrors() {
        when(mockSigner.signEvent(anyString()))
                .thenReturn(CompletableFuture.failedFuture(
                        new RuntimeException("Request timeout")));

        signingMetrics.recordSignEvent("{\"kind\":1}").exceptionally(ex -> null).join();

        Counter errors = registry.find("nsecbunker.signing.errors")
                .tag("error_type", "timeout")
                .counter();

        assertThat(errors).isNotNull();
        assertThat(errors.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("Categorizes connection errors correctly")
    void categorizesConnectionErrors() {
        // Use an exception with "connection" in the message
        when(mockSigner.signEvent(anyString()))
                .thenReturn(CompletableFuture.failedFuture(
                        new RuntimeException("connection refused")));

        signingMetrics.recordSignEvent("{\"kind\":1}").exceptionally(ex -> null).join();

        // Find any error counter for sign_event method
        Counter errors = registry.find("nsecbunker.signing.errors")
                .tag("method", "sign_event")
                .counter();

        assertThat(errors).isNotNull();
        assertThat(errors.count()).isEqualTo(1);
        // Verify the error was categorized as connection
        assertThat(errors.getId().getTag("error_type")).isEqualTo("connection");
    }
}
