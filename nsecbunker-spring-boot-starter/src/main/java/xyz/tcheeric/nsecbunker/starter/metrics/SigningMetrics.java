package xyz.tcheeric.nsecbunker.starter.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.extern.slf4j.Slf4j;
import xyz.tcheeric.nsecbunker.client.signer.NsecBunkerSigner;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Micrometer metrics for nsecBunker signing operations.
 *
 * <p>Provides metrics for:
 * <ul>
 *   <li>Total signing operations (counter)</li>
 *   <li>Successful signing operations (counter)</li>
 *   <li>Failed signing operations (counter with error type tag)</li>
 *   <li>Signing latency (timer)</li>
 *   <li>Encryption/decryption operations (counters)</li>
 *   <li>Connection status (gauge)</li>
 *   <li>Active operations (gauge)</li>
 * </ul>
 *
 * <p>Metric names follow the pattern {@code nsecbunker.<category>.<metric>}.
 */
@Slf4j
public class SigningMetrics implements MeterBinder {

    private static final String METRIC_PREFIX = "nsecbunker";

    // Signing metrics
    private static final String SIGNING_TOTAL = METRIC_PREFIX + ".signing.total";
    private static final String SIGNING_SUCCESS = METRIC_PREFIX + ".signing.success";
    private static final String SIGNING_ERRORS = METRIC_PREFIX + ".signing.errors";
    private static final String SIGNING_LATENCY = METRIC_PREFIX + ".signing.latency";
    private static final String SIGNING_ACTIVE = METRIC_PREFIX + ".signing.active";

    // Encryption metrics
    private static final String ENCRYPTION_TOTAL = METRIC_PREFIX + ".encryption.total";
    private static final String DECRYPTION_TOTAL = METRIC_PREFIX + ".decryption.total";

    // Connection metrics
    private static final String CONNECTION_STATUS = METRIC_PREFIX + ".connection.status";
    private static final String PING_LATENCY = METRIC_PREFIX + ".ping.latency";

    // Tags
    private static final String TAG_METHOD = "method";
    private static final String TAG_ERROR_TYPE = "error_type";
    private static final String TAG_ENCRYPTION_TYPE = "type";

    private final Supplier<NsecBunkerSigner> signerSupplier;
    private final AtomicLong activeOperations = new AtomicLong(0);

    private MeterRegistry registry;
    private Counter signingTotal;
    private Counter signingSuccess;
    private Timer signingLatency;
    private Timer pingLatency;

    /**
     * Creates signing metrics with a signer supplier.
     *
     * @param signerSupplier supplier for the signer instance
     */
    public SigningMetrics(Supplier<NsecBunkerSigner> signerSupplier) {
        this.signerSupplier = signerSupplier;
    }

    /**
     * Creates signing metrics with a direct signer reference.
     *
     * @param signer the signer instance
     */
    public SigningMetrics(NsecBunkerSigner signer) {
        this(() -> signer);
    }

    @Override
    public void bindTo(MeterRegistry registry) {
        this.registry = registry;

        // Signing counters
        this.signingTotal = Counter.builder(SIGNING_TOTAL)
                .description("Total number of signing operations")
                .register(registry);

        this.signingSuccess = Counter.builder(SIGNING_SUCCESS)
                .description("Number of successful signing operations")
                .register(registry);

        // Signing timer
        this.signingLatency = Timer.builder(SIGNING_LATENCY)
                .description("Time taken for signing operations")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);

        // Ping timer
        this.pingLatency = Timer.builder(PING_LATENCY)
                .description("Time taken for ping operations")
                .register(registry);

        // Active operations gauge
        Gauge.builder(SIGNING_ACTIVE, activeOperations, AtomicLong::get)
                .description("Number of currently active signing operations")
                .register(registry);

        // Connection status gauge
        Gauge.builder(CONNECTION_STATUS, this, SigningMetrics::getConnectionStatus)
                .description("Connection status (1 = connected, 0 = disconnected)")
                .register(registry);

        log.debug("SigningMetrics bound to MeterRegistry");
    }

    /**
     * Records a signing operation with timing.
     *
     * @param eventJson the event JSON to sign
     * @return a future that completes with the signed event
     */
    public CompletableFuture<String> recordSignEvent(String eventJson) {
        NsecBunkerSigner signer = signerSupplier.get();
        if (signer == null) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("No signer available"));
        }

        signingTotal.increment();
        activeOperations.incrementAndGet();
        long startTime = System.nanoTime();

        return signer.signEvent(eventJson)
                .whenComplete((result, error) -> {
                    activeOperations.decrementAndGet();
                    long duration = System.nanoTime() - startTime;
                    signingLatency.record(duration, TimeUnit.NANOSECONDS);

                    if (error != null) {
                        recordError("sign_event", error);
                    } else {
                        signingSuccess.increment();
                    }
                });
    }

    /**
     * Records an encryption operation.
     *
     * @param pubkeyHex the recipient's public key
     * @param plaintext the plaintext to encrypt
     * @param nip44     true for NIP-44, false for NIP-04
     * @return a future that completes with the ciphertext
     */
    public CompletableFuture<String> recordEncrypt(String pubkeyHex, String plaintext, boolean nip44) {
        NsecBunkerSigner signer = signerSupplier.get();
        if (signer == null) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("No signer available"));
        }

        String encryptionType = nip44 ? "nip44" : "nip04";
        Counter encryptCounter = Counter.builder(ENCRYPTION_TOTAL)
                .tag(TAG_ENCRYPTION_TYPE, encryptionType)
                .description("Total number of encryption operations")
                .register(registry);

        encryptCounter.increment();

        CompletableFuture<String> future = nip44
                ? signer.nip44Encrypt(pubkeyHex, plaintext)
                : signer.nip04Encrypt(pubkeyHex, plaintext);

        return future.whenComplete((result, error) -> {
            if (error != null) {
                recordError(nip44 ? "nip44_encrypt" : "nip04_encrypt", error);
            }
        });
    }

    /**
     * Records a decryption operation.
     *
     * @param pubkeyHex  the sender's public key
     * @param ciphertext the ciphertext to decrypt
     * @param nip44      true for NIP-44, false for NIP-04
     * @return a future that completes with the plaintext
     */
    public CompletableFuture<String> recordDecrypt(String pubkeyHex, String ciphertext, boolean nip44) {
        NsecBunkerSigner signer = signerSupplier.get();
        if (signer == null) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("No signer available"));
        }

        String encryptionType = nip44 ? "nip44" : "nip04";
        Counter decryptCounter = Counter.builder(DECRYPTION_TOTAL)
                .tag(TAG_ENCRYPTION_TYPE, encryptionType)
                .description("Total number of decryption operations")
                .register(registry);

        decryptCounter.increment();

        CompletableFuture<String> future = nip44
                ? signer.nip44Decrypt(pubkeyHex, ciphertext)
                : signer.nip04Decrypt(pubkeyHex, ciphertext);

        return future.whenComplete((result, error) -> {
            if (error != null) {
                recordError(nip44 ? "nip44_decrypt" : "nip04_decrypt", error);
            }
        });
    }

    /**
     * Records a ping operation with timing.
     *
     * @return a future that completes with the pong response
     */
    public CompletableFuture<String> recordPing() {
        NsecBunkerSigner signer = signerSupplier.get();
        if (signer == null) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("No signer available"));
        }

        long startTime = System.nanoTime();

        return signer.ping()
                .whenComplete((result, error) -> {
                    long duration = System.nanoTime() - startTime;
                    pingLatency.record(duration, TimeUnit.NANOSECONDS);

                    if (error != null) {
                        recordError("ping", error);
                    }
                });
    }

    /**
     * Records a get_public_key operation.
     *
     * @return a future that completes with the public key
     */
    public CompletableFuture<String> recordGetPublicKey() {
        NsecBunkerSigner signer = signerSupplier.get();
        if (signer == null) {
            return CompletableFuture.failedFuture(
                    new IllegalStateException("No signer available"));
        }

        Counter counter = Counter.builder(METRIC_PREFIX + ".get_public_key.total")
                .description("Total number of get_public_key operations")
                .register(registry);
        counter.increment();

        return signer.getPublicKey()
                .whenComplete((result, error) -> {
                    if (error != null) {
                        recordError("get_public_key", error);
                    }
                });
    }

    /**
     * Manually record a successful signing operation.
     *
     * @param durationNanos the duration in nanoseconds
     */
    public void recordSigningSuccess(long durationNanos) {
        signingTotal.increment();
        signingSuccess.increment();
        signingLatency.record(durationNanos, TimeUnit.NANOSECONDS);
    }

    /**
     * Manually record a failed signing operation.
     *
     * @param method    the method that failed
     * @param errorType the type of error
     */
    public void recordSigningError(String method, String errorType) {
        signingTotal.increment();
        Counter.builder(SIGNING_ERRORS)
                .tag(TAG_METHOD, method)
                .tag(TAG_ERROR_TYPE, errorType)
                .description("Number of failed signing operations")
                .register(registry)
                .increment();
    }

    /**
     * Gets the current number of active operations.
     *
     * @return the active operation count
     */
    public long getActiveOperations() {
        return activeOperations.get();
    }

    private void recordError(String method, Throwable error) {
        String errorType = categorizeError(error);
        Counter.builder(SIGNING_ERRORS)
                .tag(TAG_METHOD, method)
                .tag(TAG_ERROR_TYPE, errorType)
                .description("Number of failed operations")
                .register(registry)
                .increment();
    }

    private String categorizeError(Throwable error) {
        if (error == null) {
            return "unknown";
        }
        String className = error.getClass().getSimpleName();
        String message = error.getMessage();

        if (className.contains("Timeout") || (message != null && message.contains("timeout"))) {
            return "timeout";
        }
        if (className.contains("Connection") || (message != null && message.contains("connection"))) {
            return "connection";
        }
        if (className.contains("Auth") || (message != null && message.contains("auth"))) {
            return "authentication";
        }
        if (className.contains("Permission") || (message != null && message.contains("permission"))) {
            return "permission";
        }
        return "other";
    }

    private double getConnectionStatus() {
        NsecBunkerSigner signer = signerSupplier.get();
        return (signer != null && signer.isConnected()) ? 1.0 : 0.0;
    }
}
