package xyz.tcheeric.nsecbunker.examples.multitenant.model;

import java.time.Instant;
import java.util.List;

/**
 * Represents a tenant (user) in the multi-tenant system.
 *
 * <p>Each tenant has their own:
 * <ul>
 *   <li>Bunker configuration (pubkey, relays, secret)</li>
 *   <li>Client credentials for signing operations</li>
 *   <li>Optional admin credentials for key management</li>
 * </ul>
 */
public record Tenant(
        String id,
        String name,
        TenantBunkerConfig bunkerConfig,
        TenantStatus status,
        Instant createdAt,
        Instant lastActiveAt
) {
    /**
     * Bunker configuration for a tenant.
     */
    public record TenantBunkerConfig(
            String bunkerPubkey,
            List<String> relays,
            String secret,
            String clientPrivateKey,
            String adminPrivateKey,
            boolean useEphemeralKey
    ) {
        /**
         * Creates a signer-only config (no admin access).
         */
        public static TenantBunkerConfig signerOnly(
                String bunkerPubkey,
                List<String> relays,
                String secret,
                String clientPrivateKey) {
            return new TenantBunkerConfig(
                    bunkerPubkey, relays, secret, clientPrivateKey, null, true);
        }

        /**
         * Creates a full config with admin access.
         */
        public static TenantBunkerConfig withAdmin(
                String bunkerPubkey,
                List<String> relays,
                String secret,
                String clientPrivateKey,
                String adminPrivateKey) {
            return new TenantBunkerConfig(
                    bunkerPubkey, relays, secret, clientPrivateKey, adminPrivateKey, true);
        }

        /**
         * Checks if this tenant has admin capabilities.
         */
        public boolean hasAdminAccess() {
            return adminPrivateKey != null && !adminPrivateKey.isBlank();
        }
    }

    /**
     * Tenant status.
     */
    public enum TenantStatus {
        ACTIVE,
        SUSPENDED,
        PENDING_SETUP
    }

    /**
     * Creates a new active tenant.
     */
    public static Tenant create(String id, String name, TenantBunkerConfig config) {
        return new Tenant(id, name, config, TenantStatus.ACTIVE, Instant.now(), Instant.now());
    }

    /**
     * Updates the last active timestamp.
     */
    public Tenant touch() {
        return new Tenant(id, name, bunkerConfig, status, createdAt, Instant.now());
    }

    /**
     * Suspends the tenant.
     */
    public Tenant suspend() {
        return new Tenant(id, name, bunkerConfig, TenantStatus.SUSPENDED, createdAt, lastActiveAt);
    }

    /**
     * Activates the tenant.
     */
    public Tenant activate() {
        return new Tenant(id, name, bunkerConfig, TenantStatus.ACTIVE, createdAt, Instant.now());
    }
}
