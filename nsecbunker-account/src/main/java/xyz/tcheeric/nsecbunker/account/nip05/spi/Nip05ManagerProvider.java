package xyz.tcheeric.nsecbunker.account.nip05.spi;

import xyz.tcheeric.nsecbunker.account.nip05.Nip05Manager;

/**
 * Service Provider Interface for creating {@link Nip05Manager} instances.
 *
 * <p>Implementations of this interface allow different NIP-05 storage backends
 * to be plugged into nsecbunker-java applications. The provider with the highest
 * priority will be selected when multiple providers are available.</p>
 *
 * <p>Default implementations:</p>
 * <ul>
 *   <li>{@code DefaultNip05ManagerProvider} - In-memory storage (priority 0)</li>
 *   <li>{@code BottinNip05ManagerProvider} - Database-backed via bottin (priority 100)</li>
 * </ul>
 *
 * <p>Example implementation:</p>
 * <pre>{@code
 * @Component
 * public class CustomNip05ManagerProvider implements Nip05ManagerProvider {
 *     @Override
 *     public Nip05Manager create() {
 *         return new CustomNip05Manager();
 *     }
 *
 *     @Override
 *     public int priority() {
 *         return 50;
 *     }
 *
 *     @Override
 *     public String name() {
 *         return "custom";
 *     }
 * }
 * }</pre>
 */
public interface Nip05ManagerProvider {

    /**
     * Creates a new {@link Nip05Manager} instance.
     *
     * @return a configured Nip05Manager instance
     */
    Nip05Manager create();

    /**
     * Returns the priority of this provider.
     *
     * <p>Higher values indicate higher priority. When multiple providers are
     * available, the one with the highest priority will be selected.</p>
     *
     * <p>Standard priority levels:</p>
     * <ul>
     *   <li>0 - Default in-memory implementation</li>
     *   <li>50 - Custom implementations</li>
     *   <li>100 - Persistent implementations (e.g., bottin)</li>
     * </ul>
     *
     * @return the priority value (higher = more preferred)
     */
    int priority();

    /**
     * Returns the unique name of this provider.
     *
     * <p>Used for logging, configuration selection, and debugging.</p>
     *
     * <p>Examples: "in-memory", "bottin-persistent", "redis-cache"</p>
     *
     * @return the provider name
     */
    String name();

    /**
     * Indicates whether this provider is available and can create instances.
     *
     * <p>Providers may return false if required dependencies are not present
     * or configuration is incomplete.</p>
     *
     * @return true if the provider can create Nip05Manager instances
     */
    default boolean isAvailable() {
        return true;
    }
}
