package xyz.tcheeric.nsecbunker.core.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class AccessTokenTest {

    private static final String TEST_TOKEN = "secret-token-123";
    private static final String TEST_KEY_NAME = "my-key";
    private static final String TEST_KEY_NPUB = "npub1test";

    @Test
    void shouldBuildWithDefaultValues() {
        AccessToken token = AccessToken.builder()
                .token(TEST_TOKEN)
                .keyName(TEST_KEY_NAME)
                .build();

        assertThat(token.getToken()).isEqualTo(TEST_TOKEN);
        assertThat(token.getKeyName()).isEqualTo(TEST_KEY_NAME);
        assertThat(token.isRevoked()).isFalse();
        assertThat(token.getUsageCount()).isZero();
    }

    @Test
    void shouldBuildWithAllFields() {
        Instant now = Instant.now();
        Instant expiry = now.plus(1, ChronoUnit.HOURS);
        BunkerPolicy policy = BunkerPolicy.builder().name("test-policy").build();

        AccessToken token = AccessToken.builder()
                .id("token-123")
                .token(TEST_TOKEN)
                .keyName(TEST_KEY_NAME)
                .keyNpub(TEST_KEY_NPUB)
                .clientName("mobile-client")
                .policy(policy)
                .policyId("policy-123")
                .createdAt(now)
                .expiresAt(expiry)
                .lastUsedAt(now)
                .usageCount(50)
                .maxUsage(100L)
                .revoked(false)
                .relay("wss://relay.example.com")
                .build();

        assertThat(token.getId()).isEqualTo("token-123");
        assertThat(token.getToken()).isEqualTo(TEST_TOKEN);
        assertThat(token.getKeyNpub()).isEqualTo(TEST_KEY_NPUB);
        assertThat(token.getClientName()).isEqualTo("mobile-client");
        assertThat(token.getMaxUsage()).isEqualTo(100L);
        assertThat(token.getRelay()).isEqualTo("wss://relay.example.com");
    }

    @Test
    void forKeyShouldCreateBuilderWithKeyName() {
        AccessToken token = AccessToken.forKey(TEST_KEY_NAME)
                .token(TEST_TOKEN)
                .build();

        assertThat(token.getKeyName()).isEqualTo(TEST_KEY_NAME);
    }

    @Test
    void isExpiredShouldReturnTrueWhenExpired() {
        AccessToken token = AccessToken.builder()
                .token(TEST_TOKEN)
                .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();

        assertThat(token.isExpired()).isTrue();
    }

    @Test
    void isExpiredShouldReturnFalseWhenNotExpired() {
        AccessToken token = AccessToken.builder()
                .token(TEST_TOKEN)
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();

        assertThat(token.isExpired()).isFalse();
    }

    @Test
    void isExpiredShouldReturnFalseWhenNoExpiry() {
        AccessToken token = AccessToken.builder()
                .token(TEST_TOKEN)
                .build();

        assertThat(token.isExpired()).isFalse();
    }

    @Test
    void isValidShouldReturnTrueWhenNotRevokedAndNotExpired() {
        AccessToken token = AccessToken.builder()
                .token(TEST_TOKEN)
                .revoked(false)
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();

        assertThat(token.isValid()).isTrue();
    }

    @Test
    void isValidShouldReturnFalseWhenRevoked() {
        AccessToken token = AccessToken.builder()
                .token(TEST_TOKEN)
                .revoked(true)
                .build();

        assertThat(token.isValid()).isFalse();
    }

    @Test
    void isValidShouldReturnFalseWhenExpired() {
        AccessToken token = AccessToken.builder()
                .token(TEST_TOKEN)
                .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();

        assertThat(token.isValid()).isFalse();
    }

    @Test
    void isValidShouldReturnFalseWhenUsageLimitReached() {
        AccessToken token = AccessToken.builder()
                .token(TEST_TOKEN)
                .maxUsage(10L)
                .usageCount(10)
                .build();

        assertThat(token.isValid()).isFalse();
    }

    @Test
    void isValidShouldReturnTrueWhenUnderUsageLimit() {
        AccessToken token = AccessToken.builder()
                .token(TEST_TOKEN)
                .maxUsage(10L)
                .usageCount(5)
                .build();

        assertThat(token.isValid()).isTrue();
    }

    @Test
    void getTimeUntilExpirationShouldReturnDuration() {
        Instant expiry = Instant.now().plus(2, ChronoUnit.HOURS);
        AccessToken token = AccessToken.builder()
                .token(TEST_TOKEN)
                .expiresAt(expiry)
                .build();

        Duration duration = token.getTimeUntilExpiration();
        assertThat(duration).isNotNull();
        assertThat(duration.toMinutes()).isBetween(119L, 121L);
    }

    @Test
    void getTimeUntilExpirationShouldReturnNullWhenNoExpiry() {
        AccessToken token = AccessToken.builder()
                .token(TEST_TOKEN)
                .build();

        assertThat(token.getTimeUntilExpiration()).isNull();
    }

    @Test
    void getTimeUntilExpirationShouldReturnZeroWhenExpired() {
        AccessToken token = AccessToken.builder()
                .token(TEST_TOKEN)
                .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();

        Duration duration = token.getTimeUntilExpiration();
        assertThat(duration).isEqualTo(Duration.ZERO);
    }

    @Test
    void getRemainingUsageShouldReturnCorrectValue() {
        AccessToken token = AccessToken.builder()
                .token(TEST_TOKEN)
                .maxUsage(100L)
                .usageCount(30)
                .build();

        assertThat(token.getRemainingUsage()).isEqualTo(70L);
    }

    @Test
    void getRemainingUsageShouldReturnMinusOneWhenUnlimited() {
        AccessToken token = AccessToken.builder()
                .token(TEST_TOKEN)
                .build();

        assertThat(token.getRemainingUsage()).isEqualTo(-1L);
    }

    @Test
    void getRemainingUsageShouldReturnZeroWhenOverLimit() {
        AccessToken token = AccessToken.builder()
                .token(TEST_TOKEN)
                .maxUsage(10L)
                .usageCount(15)
                .build();

        assertThat(token.getRemainingUsage()).isZero();
    }

    @Test
    void hasUsageLimitShouldReturnTrueWhenMaxUsageSet() {
        AccessToken token = AccessToken.builder()
                .token(TEST_TOKEN)
                .maxUsage(100L)
                .build();

        assertThat(token.hasUsageLimit()).isTrue();
    }

    @Test
    void hasUsageLimitShouldReturnFalseWhenMaxUsageNull() {
        AccessToken token = AccessToken.builder()
                .token(TEST_TOKEN)
                .build();

        assertThat(token.hasUsageLimit()).isFalse();
    }

    @Test
    void hasPolicyShouldReturnTrueWhenPolicySet() {
        AccessToken token = AccessToken.builder()
                .token(TEST_TOKEN)
                .policy(BunkerPolicy.builder().name("test").build())
                .build();

        assertThat(token.hasPolicy()).isTrue();
    }

    @Test
    void hasPolicyShouldReturnTrueWhenPolicyIdSet() {
        AccessToken token = AccessToken.builder()
                .token(TEST_TOKEN)
                .policyId("policy-123")
                .build();

        assertThat(token.hasPolicy()).isTrue();
    }

    @Test
    void hasPolicyShouldReturnFalseWhenNoPolicyOrId() {
        AccessToken token = AccessToken.builder()
                .token(TEST_TOKEN)
                .build();

        assertThat(token.hasPolicy()).isFalse();
    }

    @Test
    void getConnectionStringShouldReturnValidString() {
        AccessToken token = AccessToken.builder()
                .token(TEST_TOKEN)
                .keyNpub(TEST_KEY_NPUB)
                .relay("wss://relay.example.com")
                .build();

        String connString = token.getConnectionString();
        assertThat(connString).isEqualTo("bunker://npub1test?relay=wss://relay.example.com&secret=" + TEST_TOKEN);
    }

    @Test
    void getConnectionStringShouldReturnStringWithoutRelay() {
        AccessToken token = AccessToken.builder()
                .token(TEST_TOKEN)
                .keyNpub(TEST_KEY_NPUB)
                .build();

        String connString = token.getConnectionString();
        assertThat(connString).isEqualTo("bunker://npub1test?secret=" + TEST_TOKEN);
    }

    @Test
    void getConnectionStringShouldReturnNullWhenMissingFields() {
        AccessToken token = AccessToken.builder()
                .token(TEST_TOKEN)
                .build();

        assertThat(token.getConnectionString()).isNull();
    }

    @Test
    void getFullTokenShouldReturnToken() {
        AccessToken token = AccessToken.builder()
                .token(TEST_TOKEN)
                .build();

        assertThat(token.getFullToken()).isEqualTo(TEST_TOKEN);
    }

    @Test
    void toBuilderShouldCreateCopy() {
        AccessToken original = AccessToken.builder()
                .token(TEST_TOKEN)
                .keyName(TEST_KEY_NAME)
                .usageCount(10)
                .build();

        AccessToken copy = original.toBuilder()
                .usageCount(20)
                .build();

        assertThat(copy.getToken()).isEqualTo(TEST_TOKEN);
        assertThat(copy.getUsageCount()).isEqualTo(20);
        assertThat(original.getUsageCount()).isEqualTo(10);
    }

    @Test
    void equalsAndHashCodeShouldExcludeToken() {
        AccessToken token1 = AccessToken.builder()
                .id("id-1")
                .token("secret-1")
                .keyName(TEST_KEY_NAME)
                .build();

        AccessToken token2 = AccessToken.builder()
                .id("id-1")
                .token("secret-2")
                .keyName(TEST_KEY_NAME)
                .build();

        // Equals excludes token, so these should be equal
        assertThat(token1).isEqualTo(token2);
    }

    @Test
    void toStringShouldExcludeToken() {
        AccessToken token = AccessToken.builder()
                .id("id-1")
                .token(TEST_TOKEN)
                .keyName(TEST_KEY_NAME)
                .build();

        String str = token.toString();
        assertThat(str).doesNotContain(TEST_TOKEN);
        assertThat(str).contains("id-1");
        assertThat(str).contains(TEST_KEY_NAME);
    }

    @Test
    void shouldDeserializeFromJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        String json = """
            {
                "id": "token-123",
                "token": "secret-abc",
                "key_name": "my-key",
                "key_npub": "npub1test",
                "client_name": "mobile",
                "usage_count": 50,
                "max_usage": 100,
                "revoked": false
            }
            """;

        AccessToken token = mapper.readValue(json, AccessToken.class);

        assertThat(token.getId()).isEqualTo("token-123");
        assertThat(token.getToken()).isEqualTo("secret-abc");
        assertThat(token.getKeyName()).isEqualTo("my-key");
        assertThat(token.getUsageCount()).isEqualTo(50);
        assertThat(token.getMaxUsage()).isEqualTo(100L);
    }
}
