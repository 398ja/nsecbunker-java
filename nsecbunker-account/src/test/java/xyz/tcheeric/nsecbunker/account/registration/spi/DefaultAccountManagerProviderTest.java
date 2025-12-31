package xyz.tcheeric.nsecbunker.account.registration.spi;

import org.junit.jupiter.api.Test;
import xyz.tcheeric.nsecbunker.account.registration.AccountManager;
import xyz.tcheeric.nsecbunker.account.registration.DefaultAccountManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultAccountManagerProvider}.
 */
class DefaultAccountManagerProviderTest {

    /**
     * Tests that the provider creates a DefaultAccountManager instance.
     */
    @Test
    void shouldCreateDefaultAccountManager() {
        // Arrange
        DefaultAccountManagerProvider provider = new DefaultAccountManagerProvider();

        // Act
        AccountManager manager = provider.create();

        // Assert
        assertThat(manager).isInstanceOf(DefaultAccountManager.class);
    }

    /**
     * Tests that the provider has priority 0 (default/lowest).
     */
    @Test
    void shouldHavePriorityZero() {
        // Arrange
        DefaultAccountManagerProvider provider = new DefaultAccountManagerProvider();

        // Act & Assert
        assertThat(provider.priority()).isEqualTo(0);
    }

    /**
     * Tests that the provider name is "in-memory".
     */
    @Test
    void shouldHaveInMemoryName() {
        // Arrange
        DefaultAccountManagerProvider provider = new DefaultAccountManagerProvider();

        // Act & Assert
        assertThat(provider.name()).isEqualTo("in-memory");
    }

    /**
     * Tests that the provider is always available.
     */
    @Test
    void shouldBeAvailable() {
        // Arrange
        DefaultAccountManagerProvider provider = new DefaultAccountManagerProvider();

        // Act & Assert
        assertThat(provider.isAvailable()).isTrue();
    }

    /**
     * Tests that the created manager can create accounts.
     */
    @Test
    void shouldCreateFunctionalManager() {
        // Arrange
        DefaultAccountManagerProvider provider = new DefaultAccountManagerProvider();
        AccountManager manager = provider.create();

        // Act
        var result = manager.createAccount("alice", "example.com").join();

        // Assert
        assertThat(result.getNip05()).isEqualTo("alice@example.com");
        assertThat(result.getUsername()).isEqualTo("alice");
        assertThat(result.getDomain()).isEqualTo("example.com");
    }

    /**
     * Tests that the created manager supports the registration flow.
     */
    @Test
    void shouldSupportRegistrationFlow() {
        // Arrange
        DefaultAccountManagerProvider provider = new DefaultAccountManagerProvider();
        AccountManager manager = provider.create();

        // Act
        var result = manager.registerAccount("bob", "test.com").join();

        // Assert
        assertThat(result.getNip05()).isEqualTo("bob@test.com");
        assertThat(result.getKeyName()).isEqualTo("key-bob");
    }
}
