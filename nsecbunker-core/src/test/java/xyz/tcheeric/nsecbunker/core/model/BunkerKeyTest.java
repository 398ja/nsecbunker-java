package xyz.tcheeric.nsecbunker.core.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class BunkerKeyTest {

    private static final String TEST_NAME = "test-key";
    private static final String TEST_NPUB = "npub1test";
    private static final String TEST_PUBKEY_HEX = "79be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798";

    @Test
    void shouldBuildWithRequiredFields() {
        BunkerKey key = BunkerKey.builder()
                .name(TEST_NAME)
                .pubkeyHex(TEST_PUBKEY_HEX)
                .build();

        assertThat(key.getName()).isEqualTo(TEST_NAME);
        assertThat(key.getPubkeyHex()).isEqualTo(TEST_PUBKEY_HEX);
        assertThat(key.isLocked()).isFalse();
        assertThat(key.getUserCount()).isZero();
        assertThat(key.getTokenCount()).isZero();
        assertThat(key.getSigningCount()).isZero();
    }

    @Test
    void shouldBuildWithAllFields() {
        Instant now = Instant.now();
        BunkerKey key = BunkerKey.builder()
                .name(TEST_NAME)
                .npub(TEST_NPUB)
                .pubkeyHex(TEST_PUBKEY_HEX)
                .locked(true)
                .userCount(5)
                .tokenCount(3)
                .signingCount(100)
                .createdAt(now)
                .lastUsedAt(now)
                .description("Test description")
                .build();

        assertThat(key.getName()).isEqualTo(TEST_NAME);
        assertThat(key.getNpub()).isEqualTo(TEST_NPUB);
        assertThat(key.getPubkeyHex()).isEqualTo(TEST_PUBKEY_HEX);
        assertThat(key.isLocked()).isTrue();
        assertThat(key.getUserCount()).isEqualTo(5);
        assertThat(key.getTokenCount()).isEqualTo(3);
        assertThat(key.getSigningCount()).isEqualTo(100);
        assertThat(key.getCreatedAt()).isEqualTo(now);
        assertThat(key.getLastUsedAt()).isEqualTo(now);
        assertThat(key.getDescription()).isEqualTo("Test description");
    }

    @Test
    void withNameShouldCreateBuilderWithName() {
        BunkerKey key = BunkerKey.withName(TEST_NAME)
                .pubkeyHex(TEST_PUBKEY_HEX)
                .build();

        assertThat(key.getName()).isEqualTo(TEST_NAME);
    }

    @Test
    void getPublicKeyShouldPreferHexFormat() {
        BunkerKey key = BunkerKey.builder()
                .name(TEST_NAME)
                .npub(TEST_NPUB)
                .pubkeyHex(TEST_PUBKEY_HEX)
                .build();

        assertThat(key.getPublicKey()).isEqualTo(TEST_PUBKEY_HEX);
    }

    @Test
    void getPublicKeyShouldFallbackToNpub() {
        BunkerKey key = BunkerKey.builder()
                .name(TEST_NAME)
                .npub(TEST_NPUB)
                .build();

        assertThat(key.getPublicKey()).isEqualTo(TEST_NPUB);
    }

    @Test
    void hasBeenUsedShouldReturnTrueWhenSigningCountPositive() {
        BunkerKey key = BunkerKey.builder()
                .name(TEST_NAME)
                .signingCount(1)
                .build();

        assertThat(key.hasBeenUsed()).isTrue();
    }

    @Test
    void hasBeenUsedShouldReturnTrueWhenLastUsedAtSet() {
        BunkerKey key = BunkerKey.builder()
                .name(TEST_NAME)
                .lastUsedAt(Instant.now())
                .build();

        assertThat(key.hasBeenUsed()).isTrue();
    }

    @Test
    void hasBeenUsedShouldReturnFalseWhenNeverUsed() {
        BunkerKey key = BunkerKey.builder()
                .name(TEST_NAME)
                .build();

        assertThat(key.hasBeenUsed()).isFalse();
    }

    @Test
    void hasUsersShouldReturnTrueWhenUserCountPositive() {
        BunkerKey key = BunkerKey.builder()
                .name(TEST_NAME)
                .userCount(1)
                .build();

        assertThat(key.hasUsers()).isTrue();
    }

    @Test
    void hasUsersShouldReturnFalseWhenNoUsers() {
        BunkerKey key = BunkerKey.builder()
                .name(TEST_NAME)
                .build();

        assertThat(key.hasUsers()).isFalse();
    }

    @Test
    void hasTokensShouldReturnTrueWhenTokenCountPositive() {
        BunkerKey key = BunkerKey.builder()
                .name(TEST_NAME)
                .tokenCount(1)
                .build();

        assertThat(key.hasTokens()).isTrue();
    }

    @Test
    void hasTokensShouldReturnFalseWhenNoTokens() {
        BunkerKey key = BunkerKey.builder()
                .name(TEST_NAME)
                .build();

        assertThat(key.hasTokens()).isFalse();
    }

    @Test
    void toBuilderShouldCreateCopy() {
        BunkerKey original = BunkerKey.builder()
                .name(TEST_NAME)
                .pubkeyHex(TEST_PUBKEY_HEX)
                .userCount(5)
                .build();

        BunkerKey copy = original.toBuilder()
                .userCount(10)
                .build();

        assertThat(copy.getName()).isEqualTo(TEST_NAME);
        assertThat(copy.getUserCount()).isEqualTo(10);
        assertThat(original.getUserCount()).isEqualTo(5);
    }

    @Test
    void equalsShouldWorkCorrectly() {
        BunkerKey key1 = BunkerKey.builder()
                .name(TEST_NAME)
                .pubkeyHex(TEST_PUBKEY_HEX)
                .build();

        BunkerKey key2 = BunkerKey.builder()
                .name(TEST_NAME)
                .pubkeyHex(TEST_PUBKEY_HEX)
                .build();

        assertThat(key1).isEqualTo(key2);
        assertThat(key1.hashCode()).isEqualTo(key2.hashCode());
    }

    @Test
    void toStringShouldContainKeyInfo() {
        BunkerKey key = BunkerKey.builder()
                .name(TEST_NAME)
                .pubkeyHex(TEST_PUBKEY_HEX)
                .build();

        String str = key.toString();
        assertThat(str).contains(TEST_NAME);
        assertThat(str).contains(TEST_PUBKEY_HEX);
    }

    @Test
    void shouldDeserializeFromJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        String json = """
            {
                "name": "test-key",
                "pubkey": "79be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798",
                "locked": true,
                "user_count": 5,
                "token_count": 3,
                "signing_count": 100
            }
            """;

        BunkerKey key = mapper.readValue(json, BunkerKey.class);

        assertThat(key.getName()).isEqualTo("test-key");
        assertThat(key.getPubkeyHex()).isEqualTo(TEST_PUBKEY_HEX);
        assertThat(key.isLocked()).isTrue();
        assertThat(key.getUserCount()).isEqualTo(5);
        assertThat(key.getTokenCount()).isEqualTo(3);
        assertThat(key.getSigningCount()).isEqualTo(100);
    }

    @Test
    void shouldIgnoreUnknownJsonProperties() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        String json = """
            {
                "name": "test-key",
                "pubkey": "79be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798",
                "unknown_field": "should be ignored"
            }
            """;

        BunkerKey key = mapper.readValue(json, BunkerKey.class);

        assertThat(key.getName()).isEqualTo("test-key");
    }
}
