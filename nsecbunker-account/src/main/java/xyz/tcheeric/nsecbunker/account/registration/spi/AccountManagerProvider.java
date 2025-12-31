package xyz.tcheeric.nsecbunker.account.registration.spi;

import xyz.tcheeric.nsecbunker.account.registration.AccountManager;

/**
 * Service Provider Interface for creating {@link AccountManager} instances.
 *
 * <p>Implementations of this interface allow different account storage backends
 * to be plugged into nsecbunker-java applications. The provider with the highest
 * priority will be selected when multiple providers are available.</p>
 *
 * <p>Default implementations:</p>
 * <ul>
 *   <li>{@code DefaultAccountManagerProvider} - In-memory storage (priority 0)</li>
 *   <li>{@code BottinAccountManagerProvider} - Database-backed via bottin (priority 100)</li>
 * </ul>
 *
 * <p>Example implementation:</p>
 * <pre>{@code
 * @Component
 * public class CustomAccountManagerProvider implements AccountManagerProvider {
 *     @Override
 *     public AccountManager create() {
 *         return new CustomAccountManager();
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
public interface AccountManagerProvider {

    /**
     * Creates a new {@link AccountManager} instance.
     *
     * @return a configured AccountManager instance
     */
    AccountManager create();

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
     * <p>Examples: "in-memory", "bottin-persistent"</p>
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
     * @return true if the provider can create AccountManager instances
     */
    default boolean isAvailable() {
        return true;
    }
}
