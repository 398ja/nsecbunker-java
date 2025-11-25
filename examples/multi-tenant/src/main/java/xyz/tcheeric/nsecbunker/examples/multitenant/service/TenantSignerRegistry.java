package xyz.tcheeric.nsecbunker.examples.multitenant.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.tcheeric.nsecbunker.client.signer.NsecBunkerSigner;
import xyz.tcheeric.nsecbunker.client.signer.SignerConfig;
import xyz.tcheeric.nsecbunker.examples.multitenant.model.Tenant;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing NsecBunkerSigner instances per tenant.
 *
 * <p>This registry provides:
 * <ul>
 *   <li>Lazy initialization of signers</li>
 *   <li>Connection pooling per tenant</li>
 *   <li>Automatic cleanup of idle signers</li>
 *   <li>Thread-safe access to signers</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Get signer for a tenant
 * NsecBunkerSigner signer = registry.getOrCreate(tenant);
 *
 * // Sign an event
 * String signed = signer.signEvent(eventJson).join();
 *
 * // Cleanup when tenant is removed
 * registry.remove(tenantId);
 * }</pre>
 */
public class TenantSignerRegistry {

    private static final Logger log = LoggerFactory.getLogger(TenantSignerRegistry.class);

    private final Map<String, SignerEntry> signers = new ConcurrentHashMap<>();
    private final Duration connectTimeout;
    private final Duration requestTimeout;

    /**
     * Entry holding signer and metadata.
     */
    private record SignerEntry(
            NsecBunkerSigner signer,
            long createdAt,
            long lastUsedAt
    ) {
        SignerEntry touch() {
            return new SignerEntry(signer, createdAt, System.currentTimeMillis());
        }
    }

    /**
     * Creates a registry with default timeouts.
     */
    public TenantSignerRegistry() {
        this(Duration.ofSeconds(30), Duration.ofSeconds(60));
    }

    /**
     * Creates a registry with custom timeouts.
     *
     * @param connectTimeout connection timeout
     * @param requestTimeout request timeout
     */
    public TenantSignerRegistry(Duration connectTimeout, Duration requestTimeout) {
        this.connectTimeout = connectTimeout;
        this.requestTimeout = requestTimeout;
    }

    /**
     * Gets or creates a signer for the tenant.
     *
     * @param tenant the tenant
     * @return the signer
     */
    public NsecBunkerSigner getOrCreate(Tenant tenant) {
        SignerEntry entry = signers.compute(tenant.id(), (id, existing) -> {
            if (existing != null) {
                return existing.touch();
            }
            return createSignerEntry(tenant);
        });
        return entry.signer();
    }

    /**
     * Gets a signer if it exists.
     *
     * @param tenantId the tenant ID
     * @return the signer if exists
     */
    public Optional<NsecBunkerSigner> get(String tenantId) {
        SignerEntry entry = signers.get(tenantId);
        if (entry != null) {
            signers.put(tenantId, entry.touch());
            return Optional.of(entry.signer());
        }
        return Optional.empty();
    }

    /**
     * Removes and closes a signer.
     *
     * @param tenantId the tenant ID
     */
    public void remove(String tenantId) {
        SignerEntry entry = signers.remove(tenantId);
        if (entry != null) {
            try {
                entry.signer().close();
                log.info("Closed signer for tenant: {}", tenantId);
            } catch (Exception e) {
                log.warn("Error closing signer for tenant {}: {}", tenantId, e.getMessage());
            }
        }
    }

    /**
     * Checks if a signer exists for the tenant.
     *
     * @param tenantId the tenant ID
     * @return true if exists
     */
    public boolean exists(String tenantId) {
        return signers.containsKey(tenantId);
    }

    /**
     * Gets the count of active signers.
     *
     * @return signer count
     */
    public int size() {
        return signers.size();
    }

    /**
     * Removes signers that haven't been used since the given duration.
     *
     * @param idleThreshold the idle threshold
     * @return count of removed signers
     */
    public int evictIdle(Duration idleThreshold) {
        long cutoff = System.currentTimeMillis() - idleThreshold.toMillis();
        int evicted = 0;
        for (Map.Entry<String, SignerEntry> entry : signers.entrySet()) {
            if (entry.getValue().lastUsedAt() < cutoff) {
                remove(entry.getKey());
                evicted++;
            }
        }
        if (evicted > 0) {
            log.info("Evicted {} idle signers", evicted);
        }
        return evicted;
    }

    /**
     * Closes all signers and clears the registry.
     */
    public void shutdown() {
        log.info("Shutting down signer registry with {} signers", signers.size());
        for (String tenantId : signers.keySet()) {
            remove(tenantId);
        }
    }

    private SignerEntry createSignerEntry(Tenant tenant) {
        log.info("Creating signer for tenant: {}", tenant.id());

        Tenant.TenantBunkerConfig config = tenant.bunkerConfig();

        SignerConfig signerConfig = SignerConfig.builder()
                .bunkerPubkey(config.bunkerPubkey())
                .clientPrivateKey(config.clientPrivateKey())
                .relays(config.relays())
                .secret(config.secret())
                .useEphemeralKey(config.useEphemeralKey())
                .connectTimeout(connectTimeout)
                .requestTimeout(requestTimeout)
                .build();

        // In production, provide a real transport handler
        NsecBunkerSigner signer = new NsecBunkerSigner(
                signerConfig,
                request -> {
                    throw new UnsupportedOperationException(
                            "Connect to real relay transport for production use");
                },
                null,
                null
        );

        long now = System.currentTimeMillis();
        return new SignerEntry(signer, now, now);
    }
}
