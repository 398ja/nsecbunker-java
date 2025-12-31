package xyz.tcheeric.nsecbunker.account.nip05.spi;

import org.junit.jupiter.api.Test;
import xyz.tcheeric.nsecbunker.account.nip05.DefaultNip05Manager;
import xyz.tcheeric.nsecbunker.account.nip05.Nip05Manager;
import xyz.tcheeric.nsecbunker.account.registration.DefaultAccountManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultNip05ManagerProvider}.
 */
class DefaultNip05ManagerProviderTest {

    /**
     * Tests that the provider creates a DefaultNip05Manager instance.
     */
    @Test
    void shouldCreateDefaultNip05Manager() {
        // Arrange
        DefaultNip05ManagerProvider provider = new DefaultNip05ManagerProvider();

        // Act
        Nip05Manager manager = provider.create();

        // Assert
        assertThat(manager).isInstanceOf(DefaultNip05Manager.class);
    }

    /**
     * Tests that the provider has priority 0 (default/lowest).
     */
    @Test
    void shouldHavePriorityZero() {
        // Arrange
        DefaultNip05ManagerProvider provider = new DefaultNip05ManagerProvider();

        // Act & Assert
        assertThat(provider.priority()).isEqualTo(0);
    }

    /**
     * Tests that the provider name is "in-memory".
     */
    @Test
    void shouldHaveInMemoryName() {
        // Arrange
        DefaultNip05ManagerProvider provider = new DefaultNip05ManagerProvider();

        // Act & Assert
        assertThat(provider.name()).isEqualTo("in-memory");
    }

    /**
     * Tests that the provider is always available.
     */
    @Test
    void shouldBeAvailable() {
        // Arrange
        DefaultNip05ManagerProvider provider = new DefaultNip05ManagerProvider();

        // Act & Assert
        assertThat(provider.isAvailable()).isTrue();
    }

    /**
     * Tests that the provider can be created with a custom AccountManager.
     */
    @Test
    void shouldAcceptCustomAccountManager() {
        // Arrange
        DefaultAccountManager accountManager = new DefaultAccountManager();
        DefaultNip05ManagerProvider provider = new DefaultNip05ManagerProvider(accountManager);

        // Act
        Nip05Manager manager = provider.create();

        // Assert
        assertThat(manager).isNotNull();
    }

    /**
     * Tests that the created manager can setup NIP-05 identifiers.
     */
    @Test
    void shouldCreateFunctionalManager() {
        // Arrange
        DefaultNip05ManagerProvider provider = new DefaultNip05ManagerProvider();
        Nip05Manager manager = provider.create();

        // Act
        var record = manager.setupNip05("alice", "example.com").join();

        // Assert
        assertThat(record.getNip05()).isEqualTo("alice@example.com");
        assertThat(manager.verifyNip05("alice@example.com").join()).isTrue();
    }
}
