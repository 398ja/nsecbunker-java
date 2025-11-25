package xyz.tcheeric.nsecbunker.account.registration;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultAccountManagerTest {

    /**
     * Ensures createAccount stores and returns basic metadata.
     */
    @Test
    void shouldCreateAccount() {
        // Arrange
        DefaultAccountManager manager = new DefaultAccountManager();

        // Act
        AccountRegistrationResult result = manager.createAccount("alice", "example.com").join();

        // Assert
        assertThat(result.getNip05()).isEqualTo("alice@example.com");
        assertThat(result.getNpub()).contains("npub_alice");
        assertThat(manager.find("alice@example.com")).isEqualTo(result);
    }

    /**
     * Ensures registerAccount generates a key name.
     */
    @Test
    void shouldRegisterAccount() {
        // Arrange
        DefaultAccountManager manager = new DefaultAccountManager();

        // Act
        AccountRegistrationResult result = manager.registerAccount("bob", "example.com").join();

        // Assert
        assertThat(result.getKeyName()).isEqualTo("key-bob");
    }

    /**
     * Ensures validation prevents blanks.
     */
    @Test
    void shouldValidateInputs() {
        // Arrange
        DefaultAccountManager manager = new DefaultAccountManager();

        // Act + Assert
        assertThatThrownBy(() -> manager.createAccount("", "example.com"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
