package xyz.tcheeric.nsecbunker.account.nip05;

import org.junit.jupiter.api.Test;
import xyz.tcheeric.nsecbunker.account.registration.DefaultAccountManager;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultNip05ManagerTest {

    /**
     * Ensures setupNip05 returns a record and stores it for verification.
     */
    @Test
    void shouldSetupAndVerifyNip05() {
        // Arrange
        DefaultAccountManager accountManager = new DefaultAccountManager();
        DefaultNip05Manager manager = new DefaultNip05Manager(accountManager);

        // Act
        Nip05Record record = manager.setupNip05("alice", "example.com").join();
        boolean verified = manager.verifyNip05("alice@example.com").join();

        // Assert
        assertThat(record.getNip05()).isEqualTo("alice@example.com");
        assertThat(record.getPubkey()).contains("npub_alice");
        assertThat(verified).isTrue();
    }

    /**
     * Ensures generateWellKnown produces JSON with names mapping.
     */
    @Test
    void shouldGenerateWellKnownJson() {
        // Arrange
        DefaultNip05Manager manager = new DefaultNip05Manager(new DefaultAccountManager());
        Nip05Record record = Nip05Record.builder()
                .nip05("bob@example.com")
                .pubkey("npub_bob_example.com")
                .relaysJson("[]")
                .build();

        // Act
        String json = manager.generateWellKnown(record);

        // Assert
        assertThat(json).contains("bob").contains("npub_bob_example.com");
    }
}
