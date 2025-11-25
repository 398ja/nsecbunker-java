package xyz.tcheeric.nsecbunker.examples.multitenant.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.tcheeric.nsecbunker.client.signer.NsecBunkerSigner;
import xyz.tcheeric.nsecbunker.examples.multitenant.model.Tenant;
import xyz.tcheeric.nsecbunker.examples.multitenant.repository.TenantRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Service for managing tenants and their signing operations.
 *
 * <p>This service provides:
 * <ul>
 *   <li>Tenant CRUD operations</li>
 *   <li>Per-tenant signing operations</li>
 *   <li>Tenant isolation and security</li>
 *   <li>Lifecycle management</li>
 * </ul>
 *
 * <h2>Security Considerations</h2>
 * <ul>
 *   <li>Each tenant's keys are isolated</li>
 *   <li>Tenant credentials should be encrypted at rest</li>
 *   <li>Access should be authenticated and authorized</li>
 *   <li>Operations should be audited</li>
 * </ul>
 */
public class TenantService {

    private static final Logger log = LoggerFactory.getLogger(TenantService.class);

    private final TenantRepository repository;
    private final TenantSignerRegistry signerRegistry;

    public TenantService(TenantRepository repository, TenantSignerRegistry signerRegistry) {
        this.repository = repository;
        this.signerRegistry = signerRegistry;
    }

    // ========== Tenant Management ==========

    /**
     * Creates a new tenant.
     *
     * @param id     the tenant ID
     * @param name   the tenant name
     * @param config the bunker configuration
     * @return the created tenant
     */
    public Tenant createTenant(String id, String name, Tenant.TenantBunkerConfig config) {
        if (repository.existsById(id)) {
            throw new IllegalArgumentException("Tenant already exists: " + id);
        }

        Tenant tenant = Tenant.create(id, name, config);
        repository.save(tenant);
        log.info("Created tenant: {} ({})", name, id);
        return tenant;
    }

    /**
     * Gets a tenant by ID.
     *
     * @param id the tenant ID
     * @return the tenant if found
     */
    public Optional<Tenant> getTenant(String id) {
        return repository.findById(id);
    }

    /**
     * Gets all tenants.
     *
     * @return all tenants
     */
    public Collection<Tenant> getAllTenants() {
        return repository.findAll();
    }

    /**
     * Updates a tenant's configuration.
     *
     * @param id     the tenant ID
     * @param config the new configuration
     * @return the updated tenant
     */
    public Tenant updateTenantConfig(String id, Tenant.TenantBunkerConfig config) {
        Tenant existing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + id));

        // Close existing signer to force recreation with new config
        signerRegistry.remove(id);

        Tenant updated = new Tenant(
                existing.id(),
                existing.name(),
                config,
                existing.status(),
                existing.createdAt(),
                existing.lastActiveAt()
        );

        repository.save(updated);
        log.info("Updated configuration for tenant: {}", id);
        return updated;
    }

    /**
     * Suspends a tenant.
     *
     * @param id the tenant ID
     * @return the suspended tenant
     */
    public Tenant suspendTenant(String id) {
        Tenant tenant = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + id));

        // Close signer when suspended
        signerRegistry.remove(id);

        Tenant suspended = tenant.suspend();
        repository.save(suspended);
        log.info("Suspended tenant: {}", id);
        return suspended;
    }

    /**
     * Activates a tenant.
     *
     * @param id the tenant ID
     * @return the activated tenant
     */
    public Tenant activateTenant(String id) {
        Tenant tenant = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + id));

        Tenant activated = tenant.activate();
        repository.save(activated);
        log.info("Activated tenant: {}", id);
        return activated;
    }

    /**
     * Deletes a tenant.
     *
     * @param id the tenant ID
     */
    public void deleteTenant(String id) {
        signerRegistry.remove(id);
        repository.deleteById(id);
        log.info("Deleted tenant: {}", id);
    }

    // ========== Signing Operations ==========

    /**
     * Signs an event for a tenant.
     *
     * @param tenantId  the tenant ID
     * @param eventJson the event to sign
     * @return future with the signed event
     */
    public CompletableFuture<String> signEvent(String tenantId, String eventJson) {
        Tenant tenant = getActiveTenant(tenantId);
        NsecBunkerSigner signer = signerRegistry.getOrCreate(tenant);

        log.debug("Signing event for tenant: {}", tenantId);
        return signer.signEvent(eventJson)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.warn("Signing failed for tenant {}: {}", tenantId, error.getMessage());
                    } else {
                        repository.save(tenant.touch());
                    }
                });
    }

    /**
     * Gets the public key for a tenant.
     *
     * @param tenantId the tenant ID
     * @return future with the public key
     */
    public CompletableFuture<String> getPublicKey(String tenantId) {
        Tenant tenant = getActiveTenant(tenantId);
        NsecBunkerSigner signer = signerRegistry.getOrCreate(tenant);

        return signer.getPublicKey();
    }

    /**
     * Encrypts a message for a tenant using NIP-44.
     *
     * @param tenantId        the tenant ID
     * @param recipientPubkey the recipient's public key
     * @param plaintext       the message to encrypt
     * @return future with the ciphertext
     */
    public CompletableFuture<String> encrypt(String tenantId, String recipientPubkey, String plaintext) {
        Tenant tenant = getActiveTenant(tenantId);
        NsecBunkerSigner signer = signerRegistry.getOrCreate(tenant);

        return signer.nip44Encrypt(recipientPubkey, plaintext);
    }

    /**
     * Decrypts a message for a tenant using NIP-44.
     *
     * @param tenantId     the tenant ID
     * @param senderPubkey the sender's public key
     * @param ciphertext   the message to decrypt
     * @return future with the plaintext
     */
    public CompletableFuture<String> decrypt(String tenantId, String senderPubkey, String ciphertext) {
        Tenant tenant = getActiveTenant(tenantId);
        NsecBunkerSigner signer = signerRegistry.getOrCreate(tenant);

        return signer.nip44Decrypt(senderPubkey, ciphertext);
    }

    /**
     * Pings the bunker for a tenant.
     *
     * @param tenantId the tenant ID
     * @return future with the pong response
     */
    public CompletableFuture<String> ping(String tenantId) {
        Tenant tenant = getActiveTenant(tenantId);
        NsecBunkerSigner signer = signerRegistry.getOrCreate(tenant);

        return signer.ping();
    }

    // ========== Status ==========

    /**
     * Checks if a tenant's signer is connected.
     *
     * @param tenantId the tenant ID
     * @return true if connected
     */
    public boolean isConnected(String tenantId) {
        return signerRegistry.get(tenantId)
                .map(NsecBunkerSigner::isConnected)
                .orElse(false);
    }

    /**
     * Gets statistics about the service.
     *
     * @return service statistics
     */
    public ServiceStats getStats() {
        return new ServiceStats(
                repository.count(),
                signerRegistry.size(),
                (int) repository.findAll().stream()
                        .filter(t -> t.status() == Tenant.TenantStatus.ACTIVE)
                        .count()
        );
    }

    /**
     * Service statistics.
     */
    public record ServiceStats(
            int totalTenants,
            int activeSigners,
            int activeTenants
    ) {}

    // ========== Lifecycle ==========

    /**
     * Shuts down the service, closing all signers.
     */
    public void shutdown() {
        log.info("Shutting down tenant service");
        signerRegistry.shutdown();
    }

    // ========== Internal ==========

    private Tenant getActiveTenant(String tenantId) {
        Tenant tenant = repository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));

        if (tenant.status() != Tenant.TenantStatus.ACTIVE) {
            throw new IllegalStateException("Tenant is not active: " + tenantId);
        }

        return tenant;
    }
}
