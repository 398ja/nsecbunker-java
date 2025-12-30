package xyz.tcheeric.nsecbunker.account.registration.spi;

import xyz.tcheeric.nsecbunker.account.registration.AccountManager;
import xyz.tcheeric.nsecbunker.account.registration.DefaultAccountManager;

/**
 * Default provider for in-memory {@link AccountManager} implementation.
 *
 * <p>This provider creates instances of {@link DefaultAccountManager} which stores
 * account data in memory. Data is not persisted and will be lost when the
 * application restarts.</p>
 *
 * <p>Priority: 0 (lowest) - will be overridden by persistent implementations.</p>
 */
public class DefaultAccountManagerProvider implements AccountManagerProvider {

    private static final String NAME = "in-memory";
    private static final int PRIORITY = 0;

    @Override
    public AccountManager create() {
        return new DefaultAccountManager();
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
