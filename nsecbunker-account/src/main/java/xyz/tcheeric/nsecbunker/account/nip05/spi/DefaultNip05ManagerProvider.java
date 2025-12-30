package xyz.tcheeric.nsecbunker.account.nip05.spi;

import com.fasterxml.jackson.databind.ObjectMapper;
import xyz.tcheeric.nsecbunker.account.nip05.DefaultNip05Manager;
import xyz.tcheeric.nsecbunker.account.nip05.Nip05Manager;
import xyz.tcheeric.nsecbunker.account.registration.AccountManager;
import xyz.tcheeric.nsecbunker.account.registration.DefaultAccountManager;

/**
 * Default provider for in-memory {@link Nip05Manager} implementation.
 *
 * <p>This provider creates instances of {@link DefaultNip05Manager} which stores
 * NIP-05 records in memory. Data is not persisted and will be lost when the
 * application restarts.</p>
 *
 * <p>Priority: 0 (lowest) - will be overridden by persistent implementations.</p>
 *
 * <p>The provider can be configured with custom {@link AccountManager} and
 * {@link ObjectMapper} instances. If not provided, defaults will be used.</p>
 */
public class DefaultNip05ManagerProvider implements Nip05ManagerProvider {

    private static final String NAME = "in-memory";
    private static final int PRIORITY = 0;

    private final AccountManager accountManager;
    private final ObjectMapper objectMapper;

    /**
     * Creates a provider with default dependencies.
     */
    public DefaultNip05ManagerProvider() {
        this(new DefaultAccountManager(), new ObjectMapper());
    }

    /**
     * Creates a provider with a custom AccountManager.
     *
     * @param accountManager the account manager to use
     */
    public DefaultNip05ManagerProvider(AccountManager accountManager) {
        this(accountManager, new ObjectMapper());
    }

    /**
     * Creates a provider with custom dependencies.
     *
     * @param accountManager the account manager to use
     * @param objectMapper   the object mapper for JSON serialization
     */
    public DefaultNip05ManagerProvider(AccountManager accountManager, ObjectMapper objectMapper) {
        this.accountManager = accountManager != null ? accountManager : new DefaultAccountManager();
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    @Override
    public Nip05Manager create() {
        return new DefaultNip05Manager(accountManager, objectMapper);
    }

    @Override
    public int priority() {
        return PRIORITY;
    }

    @Override
    public String name() {
        return NAME;
    }
}
