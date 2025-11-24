package xyz.tcheeric.nsecbunker.core.connection;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import xyz.tcheeric.nsecbunker.core.model.BunkerConnection;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BunkerConnectionStringTest {

    // Sample valid hex pubkey (64 chars)
    private static final String VALID_HEX_PUBKEY =
            "a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2";

    // Sample valid npub (must be npub1 + exactly 58 lowercase alphanumeric chars)
    private static final String VALID_NPUB =
            "npub1qqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqqq9nvr4t";

    @Test
    void parse_shouldParseSimpleConnectionString() {
        String input = "bunker://" + VALID_HEX_PUBKEY;

        BunkerConnection result = BunkerConnectionString.parse(input);

        assertThat(result).isNotNull();
        assertThat(result.getRemotePubkey()).isEqualTo(VALID_HEX_PUBKEY);
        assertThat(result.getRelays()).isEmpty();
        assertThat(result.getSecret()).isNull();
    }

    @Test
    void parse_shouldParseConnectionStringWithSingleRelay() {
        String input = "bunker://" + VALID_HEX_PUBKEY + "?relay=wss://relay.example.com";

        BunkerConnection result = BunkerConnectionString.parse(input);

        assertThat(result).isNotNull();
        assertThat(result.getRemotePubkey()).isEqualTo(VALID_HEX_PUBKEY);
        assertThat(result.getRelays()).containsExactly("wss://relay.example.com");
        assertThat(result.getSecret()).isNull();
    }

    @Test
    void parse_shouldParseConnectionStringWithMultipleRelays() {
        String input = "bunker://" + VALID_HEX_PUBKEY
                + "?relay=wss://relay1.com&relay=wss://relay2.com";

        BunkerConnection result = BunkerConnectionString.parse(input);

        assertThat(result).isNotNull();
        assertThat(result.getRelays()).containsExactly(
                "wss://relay1.com",
                "wss://relay2.com"
        );
    }

    @Test
    void parse_shouldParseConnectionStringWithSecret() {
        String input = "bunker://" + VALID_HEX_PUBKEY
                + "?relay=wss://relay.example.com&secret=mysecret123";

        BunkerConnection result = BunkerConnectionString.parse(input);

        assertThat(result).isNotNull();
        assertThat(result.getSecret()).isEqualTo("mysecret123");
    }

    @Test
    void parse_shouldParseConnectionStringWithNpubPubkey() {
        String input = "bunker://" + VALID_NPUB + "?relay=wss://relay.example.com";

        BunkerConnection result = BunkerConnectionString.parse(input);

        assertThat(result).isNotNull();
        assertThat(result.getRemotePubkey()).isEqualTo(VALID_NPUB);
    }

    @Test
    void parse_shouldDecodeUrlEncodedRelays() {
        String encodedRelay = "wss%3A%2F%2Frelay.example.com%2Fpath";
        String input = "bunker://" + VALID_HEX_PUBKEY + "?relay=" + encodedRelay;

        BunkerConnection result = BunkerConnectionString.parse(input);

        assertThat(result.getRelays()).containsExactly("wss://relay.example.com/path");
    }

    @Test
    void parse_shouldDecodeUrlEncodedSecret() {
        String input = "bunker://" + VALID_HEX_PUBKEY + "?secret=my%20secret%26special";

        BunkerConnection result = BunkerConnectionString.parse(input);

        assertThat(result.getSecret()).isEqualTo("my secret&special");
    }

    @Test
    void parse_shouldThrowOnNullInput() {
        assertThatThrownBy(() -> BunkerConnectionString.parse(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("must not be null");
    }

    @Test
    void parse_shouldThrowOnEmptyInput() {
        assertThatThrownBy(() -> BunkerConnectionString.parse(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be empty");
    }

    @Test
    void parse_shouldThrowOnInvalidScheme() {
        assertThatThrownBy(() -> BunkerConnectionString.parse("http://example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid scheme");
    }

    @Test
    void parse_shouldThrowOnMissingPubkey() {
        // bunker:// with no pubkey throws either "Missing public key" or URI parse error
        assertThatThrownBy(() -> BunkerConnectionString.parse("bunker://"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "bunker://invalid",
            "bunker://123abc",
            "bunker://npub1invalid"
    })
    void parse_shouldThrowOnInvalidPubkey(String input) {
        assertThatThrownBy(() -> BunkerConnectionString.parse(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid public key");
    }

    @Test
    void parse_shouldThrowOnInvalidRelayUrl() {
        String input = "bunker://" + VALID_HEX_PUBKEY + "?relay=not-a-url";

        assertThatThrownBy(() -> BunkerConnectionString.parse(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid relay URL");
    }

    @Test
    void tryParse_shouldReturnNullOnInvalidInput() {
        assertThat(BunkerConnectionString.tryParse("invalid")).isNull();
        assertThat(BunkerConnectionString.tryParse(null)).isNull();
        assertThat(BunkerConnectionString.tryParse("")).isNull();
    }

    @Test
    void tryParse_shouldReturnConnectionOnValidInput() {
        String input = "bunker://" + VALID_HEX_PUBKEY;

        BunkerConnection result = BunkerConnectionString.tryParse(input);

        assertThat(result).isNotNull();
        assertThat(result.getRemotePubkey()).isEqualTo(VALID_HEX_PUBKEY);
    }

    @Test
    void isValid_shouldReturnTrueForValidStrings() {
        assertThat(BunkerConnectionString.isValid("bunker://" + VALID_HEX_PUBKEY)).isTrue();
        assertThat(BunkerConnectionString.isValid("bunker://" + VALID_NPUB)).isTrue();
        assertThat(BunkerConnectionString.isValid(
                "bunker://" + VALID_HEX_PUBKEY + "?relay=wss://relay.com")).isTrue();
    }

    @Test
    void isValid_shouldReturnFalseForInvalidStrings() {
        assertThat(BunkerConnectionString.isValid(null)).isFalse();
        assertThat(BunkerConnectionString.isValid("")).isFalse();
        assertThat(BunkerConnectionString.isValid("invalid")).isFalse();
        assertThat(BunkerConnectionString.isValid("http://example.com")).isFalse();
    }

    @Test
    void build_shouldCreateSimpleConnectionString() {
        String result = BunkerConnectionString.build(VALID_HEX_PUBKEY, null, null);

        assertThat(result).isEqualTo("bunker://" + VALID_HEX_PUBKEY);
    }

    @Test
    void build_shouldCreateConnectionStringWithRelays() {
        List<String> relays = Arrays.asList("wss://relay1.com", "wss://relay2.com");

        String result = BunkerConnectionString.build(VALID_HEX_PUBKEY, relays, null);

        assertThat(result).contains("relay=wss%3A%2F%2Frelay1.com");
        assertThat(result).contains("relay=wss%3A%2F%2Frelay2.com");
    }

    @Test
    void build_shouldCreateConnectionStringWithSecret() {
        String result = BunkerConnectionString.build(
                VALID_HEX_PUBKEY,
                Collections.singletonList("wss://relay.com"),
                "mysecret"
        );

        assertThat(result).contains("secret=mysecret");
    }

    @Test
    void build_shouldUrlEncodeSpecialCharacters() {
        String result = BunkerConnectionString.build(
                VALID_HEX_PUBKEY,
                null,
                "secret with spaces&special"
        );

        assertThat(result).contains("secret=secret+with+spaces%26special");
    }

    @Test
    void build_shouldThrowOnNullPubkey() {
        assertThatThrownBy(() -> BunkerConnectionString.build(null, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void build_shouldThrowOnInvalidPubkey() {
        assertThatThrownBy(() -> BunkerConnectionString.build("invalid", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid public key");
    }

    @Test
    void build_shouldThrowOnInvalidRelayUrl() {
        List<String> invalidRelays = Collections.singletonList("not-a-url");

        assertThatThrownBy(() -> BunkerConnectionString.build(VALID_HEX_PUBKEY, invalidRelays, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid relay URL");
    }

    @Test
    void buildFromConnection_shouldCreateConnectionString() {
        BunkerConnection connection = BunkerConnection.builder()
                .remotePubkey(VALID_HEX_PUBKEY)
                .relays(Collections.singletonList("wss://relay.com"))
                .secret("mysecret")
                .build();

        String result = BunkerConnectionString.build(connection);

        assertThat(result).startsWith("bunker://" + VALID_HEX_PUBKEY);
        assertThat(result).contains("relay=");
        assertThat(result).contains("secret=");
    }

    @Test
    void roundTrip_shouldPreserveAllFields() {
        BunkerConnection original = BunkerConnection.builder()
                .remotePubkey(VALID_HEX_PUBKEY)
                .relays(Arrays.asList("wss://relay1.com", "wss://relay2.com"))
                .secret("mysecret123")
                .build();

        String connectionString = BunkerConnectionString.build(original);
        BunkerConnection parsed = BunkerConnectionString.parse(connectionString);

        assertThat(parsed.getRemotePubkey()).isEqualTo(original.getRemotePubkey());
        assertThat(parsed.getRelays()).containsExactlyElementsOf(original.getRelays());
        assertThat(parsed.getSecret()).isEqualTo(original.getSecret());
    }

    @Test
    void isValidPubkey_shouldValidateHexPubkeys() {
        assertThat(BunkerConnectionString.isValidPubkey(VALID_HEX_PUBKEY)).isTrue();
        assertThat(BunkerConnectionString.isValidPubkey("abc123")).isFalse();
        assertThat(BunkerConnectionString.isValidPubkey(null)).isFalse();
        assertThat(BunkerConnectionString.isValidPubkey("")).isFalse();
    }

    @Test
    void isValidPubkey_shouldValidateNpubPubkeys() {
        assertThat(BunkerConnectionString.isValidPubkey(VALID_NPUB)).isTrue();
        assertThat(BunkerConnectionString.isValidPubkey("npub1invalid")).isFalse();
    }

    @Test
    void isValidRelayUrl_shouldValidateRelayUrls() {
        assertThat(BunkerConnectionString.isValidRelayUrl("wss://relay.example.com")).isTrue();
        assertThat(BunkerConnectionString.isValidRelayUrl("ws://relay.example.com")).isTrue();
        assertThat(BunkerConnectionString.isValidRelayUrl("wss://relay.com/path")).isTrue();
        assertThat(BunkerConnectionString.isValidRelayUrl("http://example.com")).isFalse();
        assertThat(BunkerConnectionString.isValidRelayUrl("not-a-url")).isFalse();
        assertThat(BunkerConnectionString.isValidRelayUrl(null)).isFalse();
        assertThat(BunkerConnectionString.isValidRelayUrl("")).isFalse();
    }
}
