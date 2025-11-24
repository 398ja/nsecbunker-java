package xyz.tcheeric.nsecbunker.admin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link AdminConfig}.
 */
class AdminConfigTest {

    private static final String TEST_BUNKER_PUBKEY = "79be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798";
    private static final String TEST_ADMIN_PRIVKEY = "0000000000000000000000000000000000000000000000000000000000000001";
    private static final String TEST_RELAY = "wss://relay.example.com";

    @Test
    @DisplayName("Builder creates valid config with required fields")
    void builderCreatesValidConfig() {
        AdminConfig config = AdminConfig.builder()
                .bunkerPubkey(TEST_BUNKER_PUBKEY)
                .adminPrivateKey(TEST_ADMIN_PRIVKEY)
                .relays(List.of(TEST_RELAY))
                .build();

        assertThat(config.getBunkerPubkey()).isEqualTo(TEST_BUNKER_PUBKEY);
        assertThat(config.getAdminPrivateKey()).isEqualTo(TEST_ADMIN_PRIVKEY);
        assertThat(config.getRelays()).containsExactly(TEST_RELAY);
    }

    @Test
    @DisplayName("Builder sets default values")
    void builderSetsDefaults() {
        AdminConfig config = AdminConfig.builder()
                .bunkerPubkey(TEST_BUNKER_PUBKEY)
                .adminPrivateKey(TEST_ADMIN_PRIVKEY)
                .relays(List.of(TEST_RELAY))
                .build();

        assertThat(config.getConnectTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(config.getRequestTimeout()).isEqualTo(Duration.ofSeconds(60));
        assertThat(config.isAutoReconnect()).isTrue();
        assertThat(config.getMaxReconnectAttempts()).isEqualTo(5);
        assertThat(config.getReconnectDelay()).isEqualTo(Duration.ofSeconds(1));
        assertThat(config.getMaxReconnectDelay()).isEqualTo(Duration.ofSeconds(30));
        assertThat(config.isUseEphemeralKey()).isTrue();
    }

    @Test
    @DisplayName("Validate throws on missing bunker pubkey")
    void validateThrowsOnMissingBunkerPubkey() {
        AdminConfig config = AdminConfig.builder()
                .adminPrivateKey(TEST_ADMIN_PRIVKEY)
                .relays(List.of(TEST_RELAY))
                .build();

        assertThatThrownBy(config::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Bunker public key is required");
    }

    @Test
    @DisplayName("Validate throws on missing admin private key")
    void validateThrowsOnMissingAdminPrivkey() {
        AdminConfig config = AdminConfig.builder()
                .bunkerPubkey(TEST_BUNKER_PUBKEY)
                .relays(List.of(TEST_RELAY))
                .build();

        assertThatThrownBy(config::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Admin private key is required");
    }

    @Test
    @DisplayName("Validate throws on empty relays")
    void validateThrowsOnEmptyRelays() {
        AdminConfig config = AdminConfig.builder()
                .bunkerPubkey(TEST_BUNKER_PUBKEY)
                .adminPrivateKey(TEST_ADMIN_PRIVKEY)
                .relays(List.of())
                .build();

        assertThatThrownBy(config::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("At least one relay URL is required");
    }

    @Test
    @DisplayName("Validate throws on invalid relay URL")
    void validateThrowsOnInvalidRelayUrl() {
        AdminConfig config = AdminConfig.builder()
                .bunkerPubkey(TEST_BUNKER_PUBKEY)
                .adminPrivateKey(TEST_ADMIN_PRIVKEY)
                .relays(List.of("http://invalid.com"))
                .build();

        assertThatThrownBy(config::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid relay URL");
    }

    @Test
    @DisplayName("FromConnectionString parses bunker URI correctly")
    void fromConnectionStringParsesBunkerUri() {
        String connectionString = "bunker://" + TEST_BUNKER_PUBKEY +
                "?relay=" + TEST_RELAY + "&secret=test-secret";

        AdminConfig config = AdminConfig.fromConnectionString(connectionString, TEST_ADMIN_PRIVKEY);

        assertThat(config.getBunkerPubkey()).isEqualTo(TEST_BUNKER_PUBKEY);
        assertThat(config.getAdminPrivateKey()).isEqualTo(TEST_ADMIN_PRIVKEY);
        assertThat(config.getRelays()).containsExactly(TEST_RELAY);
        assertThat(config.getSecret()).isEqualTo("test-secret");
    }

    @Test
    @DisplayName("FromConnectionString handles URI without query params")
    void fromConnectionStringHandlesNoQueryParams() {
        String connectionString = "bunker://" + TEST_BUNKER_PUBKEY;

        AdminConfig config = AdminConfig.fromConnectionString(connectionString, TEST_ADMIN_PRIVKEY);

        assertThat(config.getBunkerPubkey()).isEqualTo(TEST_BUNKER_PUBKEY);
        assertThat(config.getRelays()).isEmpty();
        assertThat(config.getSecret()).isNull();
    }

    @Test
    @DisplayName("FromConnectionString throws on invalid scheme")
    void fromConnectionStringThrowsOnInvalidScheme() {
        assertThatThrownBy(() -> AdminConfig.fromConnectionString("nostr://" + TEST_BUNKER_PUBKEY, TEST_ADMIN_PRIVKEY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must start with bunker://");
    }

    @Test
    @DisplayName("FromConnectionString handles multiple relays")
    void fromConnectionStringHandlesMultipleRelays() {
        String connectionString = "bunker://" + TEST_BUNKER_PUBKEY +
                "?relay=wss://relay1.com&relay=wss://relay2.com";

        AdminConfig config = AdminConfig.fromConnectionString(connectionString, TEST_ADMIN_PRIVKEY);

        assertThat(config.getRelays()).containsExactly("wss://relay1.com", "wss://relay2.com");
    }

    @Test
    @DisplayName("ToBuilder creates correct copy")
    void toBuilderCreatesCorrectCopy() {
        AdminConfig original = AdminConfig.builder()
                .bunkerPubkey(TEST_BUNKER_PUBKEY)
                .adminPrivateKey(TEST_ADMIN_PRIVKEY)
                .relays(List.of(TEST_RELAY))
                .connectTimeout(Duration.ofSeconds(60))
                .build();

        AdminConfig copy = original.toBuilder()
                .secret("new-secret")
                .build();

        assertThat(copy.getBunkerPubkey()).isEqualTo(original.getBunkerPubkey());
        assertThat(copy.getConnectTimeout()).isEqualTo(Duration.ofSeconds(60));
        assertThat(copy.getSecret()).isEqualTo("new-secret");
    }

    @Test
    @DisplayName("ToString excludes private key")
    void toStringExcludesPrivateKey() {
        AdminConfig config = AdminConfig.builder()
                .bunkerPubkey(TEST_BUNKER_PUBKEY)
                .adminPrivateKey(TEST_ADMIN_PRIVKEY)
                .relays(List.of(TEST_RELAY))
                .build();

        String str = config.toString();

        assertThat(str).doesNotContain(TEST_ADMIN_PRIVKEY);
        assertThat(str).contains(TEST_BUNKER_PUBKEY);
    }
}
