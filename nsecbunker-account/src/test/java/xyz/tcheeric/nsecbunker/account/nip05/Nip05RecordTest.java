package xyz.tcheeric.nsecbunker.account.nip05;

import org.junit.jupiter.api.Test;
import xyz.tcheeric.nsecbunker.core.model.BunkerKey;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Nip05RecordTest {

    @Test
    void shouldBuildWithAllFields() {
        Nip05Record record = Nip05Record.builder()
                .nip05("alice@example.com")
                .pubkey("79be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798")
                .relaysJson("[\"wss://relay1.example.com\",\"wss://relay2.example.com\"]")
                .build();

        assertThat(record.getNip05()).isEqualTo("alice@example.com");
        assertThat(record.getPubkey()).isEqualTo("79be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798");
        assertThat(record.getRelaysJson()).contains("relay1.example.com");
    }

    @Test
    void shouldBuildWithNullFields() {
        Nip05Record record = Nip05Record.builder()
                .nip05("bob@test.com")
                .build();

        assertThat(record.getNip05()).isEqualTo("bob@test.com");
        assertThat(record.getPubkey()).isNull();
        assertThat(record.getRelaysJson()).isNull();
    }

    @Test
    void toBuilderShouldCreateCopy() {
        Nip05Record original = Nip05Record.builder()
                .nip05("alice@example.com")
                .pubkey("pubkey1")
                .build();

        Nip05Record copy = original.toBuilder()
                .pubkey("pubkey2")
                .build();

        assertThat(copy.getNip05()).isEqualTo("alice@example.com");
        assertThat(copy.getPubkey()).isEqualTo("pubkey2");
        assertThat(original.getPubkey()).isEqualTo("pubkey1");
    }

    @Test
    void equalsShouldWorkCorrectly() {
        Nip05Record record1 = Nip05Record.builder()
                .nip05("alice@example.com")
                .pubkey("pubkey")
                .build();

        Nip05Record record2 = Nip05Record.builder()
                .nip05("alice@example.com")
                .pubkey("pubkey")
                .build();

        assertThat(record1).isEqualTo(record2);
        assertThat(record1.hashCode()).isEqualTo(record2.hashCode());
    }

    @Test
    void toStringShouldContainFields() {
        Nip05Record record = Nip05Record.builder()
                .nip05("alice@example.com")
                .pubkey("abc123")
                .build();

        String str = record.toString();
        assertThat(str).contains("alice@example.com");
        assertThat(str).contains("abc123");
    }

    // ========== Tests for new helper methods ==========

    /**
     * Tests that getRelays parses a valid JSON array of relay URLs.
     */
    @Test
    void shouldParseRelaysFromJson() {
        // Arrange
        Nip05Record record = Nip05Record.builder()
                .nip05("alice@example.com")
                .relaysJson("[\"wss://relay1.example.com\",\"wss://relay2.example.com\"]")
                .build();

        // Act
        List<String> relays = record.getRelays();

        // Assert
        assertThat(relays).hasSize(2);
        assertThat(relays).containsExactly("wss://relay1.example.com", "wss://relay2.example.com");
    }

    /**
     * Tests that getRelays returns empty list for null relaysJson.
     */
    @Test
    void shouldReturnEmptyListWhenRelaysJsonIsNull() {
        // Arrange
        Nip05Record record = Nip05Record.builder()
                .nip05("alice@example.com")
                .relaysJson(null)
                .build();

        // Act
        List<String> relays = record.getRelays();

        // Assert
        assertThat(relays).isEmpty();
    }

    /**
     * Tests that getRelays returns empty list for empty array JSON.
     */
    @Test
    void shouldReturnEmptyListWhenRelaysJsonIsEmptyArray() {
        // Arrange
        Nip05Record record = Nip05Record.builder()
                .nip05("alice@example.com")
                .relaysJson("[]")
                .build();

        // Act
        List<String> relays = record.getRelays();

        // Assert
        assertThat(relays).isEmpty();
    }

    /**
     * Tests that getUsername extracts the username from NIP-05 identifier.
     */
    @Test
    void shouldExtractUsername() {
        // Arrange
        Nip05Record record = Nip05Record.builder()
                .nip05("alice@example.com")
                .build();

        // Act & Assert
        assertThat(record.getUsername()).isEqualTo("alice");
    }

    /**
     * Tests that getDomain extracts the domain from NIP-05 identifier.
     */
    @Test
    void shouldExtractDomain() {
        // Arrange
        Nip05Record record = Nip05Record.builder()
                .nip05("alice@example.com")
                .build();

        // Act & Assert
        assertThat(record.getDomain()).isEqualTo("example.com");
    }

    /**
     * Tests that getUsername returns null for invalid NIP-05.
     */
    @Test
    void shouldReturnNullUsernameWhenNip05Invalid() {
        // Arrange
        Nip05Record record = Nip05Record.builder()
                .nip05("invalid-nip05")
                .build();

        // Act & Assert
        assertThat(record.getUsername()).isNull();
    }

    /**
     * Tests that hasRelays returns true when relays are configured.
     */
    @Test
    void shouldDetectWhenHasRelays() {
        // Arrange
        Nip05Record withRelays = Nip05Record.builder()
                .nip05("alice@example.com")
                .relaysJson("[\"wss://relay.example.com\"]")
                .build();

        Nip05Record withoutRelays = Nip05Record.builder()
                .nip05("bob@example.com")
                .relaysJson("[]")
                .build();

        // Act & Assert
        assertThat(withRelays.hasRelays()).isTrue();
        assertThat(withoutRelays.hasRelays()).isFalse();
    }

    /**
     * Tests the static factory method of() creates a valid record.
     */
    @Test
    void shouldCreateRecordWithOfFactory() {
        // Arrange & Act
        Nip05Record record = Nip05Record.of("alice", "example.com", "abcdef1234");

        // Assert
        assertThat(record.getNip05()).isEqualTo("alice@example.com");
        assertThat(record.getPubkey()).isEqualTo("abcdef1234");
        assertThat(record.getRelaysJson()).isEqualTo("[]");
    }

    /**
     * Tests the static factory method of() with relays.
     */
    @Test
    void shouldCreateRecordWithOfFactoryAndRelays() {
        // Arrange
        List<String> relays = List.of("wss://relay1.com", "wss://relay2.com");

        // Act
        Nip05Record record = Nip05Record.of("bob", "test.com", "pubkey123", relays);

        // Assert
        assertThat(record.getNip05()).isEqualTo("bob@test.com");
        assertThat(record.getPubkey()).isEqualTo("pubkey123");
        assertThat(record.getRelays()).containsExactly("wss://relay1.com", "wss://relay2.com");
    }

    /**
     * Tests creating a record from BunkerKey.
     */
    @Test
    void shouldCreateRecordFromBunkerKey() {
        // Arrange
        BunkerKey key = BunkerKey.builder()
                .name("test-key")
                .pubkeyHex("79be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798")
                .npub("npub1...")
                .build();

        // Act
        Nip05Record record = Nip05Record.fromBunkerKey(key, "alice", "example.com");

        // Assert
        assertThat(record.getNip05()).isEqualTo("alice@example.com");
        assertThat(record.getPubkey()).isEqualTo("79be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798");
        assertThat(record.getRelaysJson()).isEqualTo("[]");
    }

    /**
     * Tests creating a record from BunkerKey with relays.
     */
    @Test
    void shouldCreateRecordFromBunkerKeyWithRelays() {
        // Arrange
        BunkerKey key = BunkerKey.builder()
                .name("test-key")
                .pubkeyHex("abcdef123456")
                .build();
        List<String> relays = List.of("wss://relay.nostr.info");

        // Act
        Nip05Record record = Nip05Record.fromBunkerKey(key, "bob", "nostr.info", relays);

        // Assert
        assertThat(record.getNip05()).isEqualTo("bob@nostr.info");
        assertThat(record.getPubkey()).isEqualTo("abcdef123456");
        assertThat(record.getRelays()).containsExactly("wss://relay.nostr.info");
    }

    /**
     * Tests that fromBunkerKey throws on null key.
     */
    @Test
    void shouldThrowOnNullKeyInFromBunkerKey() {
        assertThatThrownBy(() -> Nip05Record.fromBunkerKey(null, "alice", "example.com"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("key");
    }

    /**
     * Tests that fromBunkerKey uses npub when pubkeyHex is null.
     */
    @Test
    void shouldUseNpubWhenPubkeyHexIsNull() {
        // Arrange
        BunkerKey key = BunkerKey.builder()
                .name("test-key")
                .npub("npub1abcdef...")
                .build();

        // Act
        Nip05Record record = Nip05Record.fromBunkerKey(key, "alice", "example.com");

        // Assert
        assertThat(record.getPubkey()).isEqualTo("npub1abcdef...");
    }
}
