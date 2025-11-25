package xyz.tcheeric.nsecbunker.examples.multitenant.repository;

import xyz.tcheeric.nsecbunker.examples.multitenant.model.Tenant;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory repository for tenant storage.
 *
 * <p>In production, replace with a persistent store (database, etc.)
 * with proper encryption for sensitive credentials.
 */
public class TenantRepository {

    private final Map<String, Tenant> tenants = new ConcurrentHashMap<>();

    /**
     * Saves a tenant.
     *
     * @param tenant the tenant to save
     * @return the saved tenant
     */
    public Tenant save(Tenant tenant) {
        tenants.put(tenant.id(), tenant);
        return tenant;
    }

    /**
     * Finds a tenant by ID.
     *
     * @param id the tenant ID
     * @return the tenant if found
     */
    public Optional<Tenant> findById(String id) {
        return Optional.ofNullable(tenants.get(id));
    }

    /**
     * Checks if a tenant exists.
     *
     * @param id the tenant ID
     * @return true if exists
     */
    public boolean existsById(String id) {
        return tenants.containsKey(id);
    }

    /**
     * Deletes a tenant.
     *
     * @param id the tenant ID
     */
    public void deleteById(String id) {
        tenants.remove(id);
    }

    /**
     * Gets all tenants.
     *
     * @return collection of all tenants
     */
    public Collection<Tenant> findAll() {
        return tenants.values();
    }

    /**
     * Gets the count of tenants.
     *
     * @return tenant count
     */
    public int count() {
        return tenants.size();
    }

    /**
     * Clears all tenants.
     */
    public void clear() {
        tenants.clear();
    }
}
