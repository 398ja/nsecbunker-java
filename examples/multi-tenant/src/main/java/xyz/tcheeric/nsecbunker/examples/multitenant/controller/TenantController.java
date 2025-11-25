package xyz.tcheeric.nsecbunker.examples.multitenant.controller;

import xyz.tcheeric.nsecbunker.examples.multitenant.model.Tenant;
import xyz.tcheeric.nsecbunker.examples.multitenant.service.TenantService;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Controller for multi-tenant operations.
 *
 * <p>In a real application, this would be a REST controller with proper
 * authentication and authorization. Each endpoint should verify that the
 * caller has permission to access the specified tenant.
 *
 * <h2>Example REST Endpoints</h2>
 * <pre>
 * POST   /api/tenants                  - Create tenant
 * GET    /api/tenants                  - List all tenants
 * GET    /api/tenants/{id}             - Get tenant
 * DELETE /api/tenants/{id}             - Delete tenant
 * POST   /api/tenants/{id}/suspend     - Suspend tenant
 * POST   /api/tenants/{id}/activate    - Activate tenant
 * POST   /api/tenants/{id}/sign        - Sign event
 * GET    /api/tenants/{id}/pubkey      - Get public key
 * POST   /api/tenants/{id}/encrypt     - Encrypt message
 * POST   /api/tenants/{id}/decrypt     - Decrypt message
 * </pre>
 */
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    // ========== Tenant Management ==========

    /**
     * Creates a new tenant.
     */
    public Tenant createTenant(CreateTenantRequest request) {
        Tenant.TenantBunkerConfig config = new Tenant.TenantBunkerConfig(
                request.bunkerPubkey(),
                request.relays(),
                request.secret(),
                request.clientPrivateKey(),
                request.adminPrivateKey(),
                request.useEphemeralKey()
        );

        return tenantService.createTenant(request.id(), request.name(), config);
    }

    /**
     * Gets all tenants (admin only).
     */
    public Collection<Tenant> listTenants() {
        return tenantService.getAllTenants();
    }

    /**
     * Gets a tenant by ID.
     */
    public Tenant getTenant(String tenantId) {
        return tenantService.getTenant(tenantId)
                .orElseThrow(() -> new TenantNotFoundException(tenantId));
    }

    /**
     * Deletes a tenant.
     */
    public void deleteTenant(String tenantId) {
        tenantService.deleteTenant(tenantId);
    }

    /**
     * Suspends a tenant.
     */
    public Tenant suspendTenant(String tenantId) {
        return tenantService.suspendTenant(tenantId);
    }

    /**
     * Activates a tenant.
     */
    public Tenant activateTenant(String tenantId) {
        return tenantService.activateTenant(tenantId);
    }

    // ========== Signing Operations ==========

    /**
     * Signs an event for a tenant.
     */
    public CompletableFuture<SignResponse> signEvent(String tenantId, String eventJson) {
        return tenantService.signEvent(tenantId, eventJson)
                .thenApply(signed -> new SignResponse(signed, null))
                .exceptionally(ex -> new SignResponse(null, ex.getMessage()));
    }

    /**
     * Gets the public key for a tenant.
     */
    public CompletableFuture<String> getPublicKey(String tenantId) {
        return tenantService.getPublicKey(tenantId);
    }

    /**
     * Encrypts a message for a tenant.
     */
    public CompletableFuture<EncryptResponse> encrypt(String tenantId, EncryptRequest request) {
        return tenantService.encrypt(tenantId, request.recipientPubkey(), request.plaintext())
                .thenApply(ciphertext -> new EncryptResponse(ciphertext, null))
                .exceptionally(ex -> new EncryptResponse(null, ex.getMessage()));
    }

    /**
     * Decrypts a message for a tenant.
     */
    public CompletableFuture<DecryptResponse> decrypt(String tenantId, DecryptRequest request) {
        return tenantService.decrypt(tenantId, request.senderPubkey(), request.ciphertext())
                .thenApply(plaintext -> new DecryptResponse(plaintext, null))
                .exceptionally(ex -> new DecryptResponse(null, ex.getMessage()));
    }

    // ========== Status ==========

    /**
     * Gets tenant connection status.
     */
    public TenantStatus getTenantStatus(String tenantId) {
        Tenant tenant = getTenant(tenantId);
        boolean connected = tenantService.isConnected(tenantId);
        return new TenantStatus(tenant.id(), tenant.status(), connected);
    }

    /**
     * Gets service statistics.
     */
    public TenantService.ServiceStats getStats() {
        return tenantService.getStats();
    }

    // ========== Request/Response DTOs ==========

    public record CreateTenantRequest(
            String id,
            String name,
            String bunkerPubkey,
            List<String> relays,
            String secret,
            String clientPrivateKey,
            String adminPrivateKey,
            boolean useEphemeralKey
    ) {}

    public record SignResponse(String signedEvent, String error) {}

    public record EncryptRequest(String recipientPubkey, String plaintext) {}

    public record EncryptResponse(String ciphertext, String error) {}

    public record DecryptRequest(String senderPubkey, String ciphertext) {}

    public record DecryptResponse(String plaintext, String error) {}

    public record TenantStatus(String tenantId, Tenant.TenantStatus status, boolean connected) {}

    // ========== Exceptions ==========

    public static class TenantNotFoundException extends RuntimeException {
        public TenantNotFoundException(String tenantId) {
            super("Tenant not found: " + tenantId);
        }
    }
}
