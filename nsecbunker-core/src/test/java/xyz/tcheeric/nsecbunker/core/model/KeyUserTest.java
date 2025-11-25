package xyz.tcheeric.nsecbunker.core.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class KeyUserTest {

    private static final String TEST_PUBKEY = "79be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798";
    private static final String TEST_NPUB = "npub1test";

    @Test
    void shouldBuildWithDefaultValues() {
        KeyUser user = KeyUser.builder()
                .pubkeyHex(TEST_PUBKEY)
                .build();

        assertThat(user.getPubkeyHex()).isEqualTo(TEST_PUBKEY);
        assertThat(user.isActive()).isTrue();
        assertThat(user.getSigningCount()).isZero();
        assertThat(user.getSigningConditions()).isEmpty();
    }

    @Test
    void shouldBuildWithAllFields() {
        Instant now = Instant.now();
        BunkerPolicy policy = BunkerPolicy.builder().name("test-policy").build();
        SigningCondition condition = SigningCondition.allowEventKind(1);

        KeyUser user = KeyUser.builder()
                .pubkeyHex(TEST_PUBKEY)
                .npub(TEST_NPUB)
                .keyName("my-key")
                .description("Test user")
                .policy(policy)
                .policyId("policy-123")
                .signingCondition(condition)
                .createdAt(now)
                .updatedAt(now)
                .lastUsedAt(now)
                .signingCount(50)
                .active(true)
                .build();

        assertThat(user.getPubkeyHex()).isEqualTo(TEST_PUBKEY);
        assertThat(user.getNpub()).isEqualTo(TEST_NPUB);
        assertThat(user.getKeyName()).isEqualTo("my-key");
        assertThat(user.getDescription()).isEqualTo("Test user");
        assertThat(user.getPolicy()).isEqualTo(policy);
        assertThat(user.getPolicyId()).isEqualTo("policy-123");
        assertThat(user.getSigningConditions()).hasSize(1);
        assertThat(user.getSigningCount()).isEqualTo(50);
    }

    @Test
    void forPubkeyShouldCreateBuilderWithPubkey() {
        KeyUser user = KeyUser.forPubkey(TEST_PUBKEY)
                .keyName("key")
                .build();

        assertThat(user.getPubkeyHex()).isEqualTo(TEST_PUBKEY);
        assertThat(user.getKeyName()).isEqualTo("key");
    }

    @Test
    void getPublicKeyShouldPreferHexFormat() {
        KeyUser user = KeyUser.builder()
                .pubkeyHex(TEST_PUBKEY)
                .npub(TEST_NPUB)
                .build();

        assertThat(user.getPublicKey()).isEqualTo(TEST_PUBKEY);
    }

    @Test
    void getPublicKeyShouldFallbackToNpub() {
        KeyUser user = KeyUser.builder()
                .npub(TEST_NPUB)
                .build();

        assertThat(user.getPublicKey()).isEqualTo(TEST_NPUB);
    }

    @Test
    void hasPolicyShouldReturnTrueWhenPolicySet() {
        KeyUser user = KeyUser.builder()
                .pubkeyHex(TEST_PUBKEY)
                .policy(BunkerPolicy.builder().name("test").build())
                .build();

        assertThat(user.hasPolicy()).isTrue();
    }

    @Test
    void hasPolicyShouldReturnTrueWhenPolicyIdSet() {
        KeyUser user = KeyUser.builder()
                .pubkeyHex(TEST_PUBKEY)
                .policyId("policy-123")
                .build();

        assertThat(user.hasPolicy()).isTrue();
    }

    @Test
    void hasPolicyShouldReturnFalseWhenNoPolicyOrId() {
        KeyUser user = KeyUser.builder()
                .pubkeyHex(TEST_PUBKEY)
                .build();

        assertThat(user.hasPolicy()).isFalse();
    }

    @Test
    void hasPolicyShouldReturnFalseWhenPolicyIdEmpty() {
        KeyUser user = KeyUser.builder()
                .pubkeyHex(TEST_PUBKEY)
                .policyId("")
                .build();

        assertThat(user.hasPolicy()).isFalse();
    }

    @Test
    void hasSigningConditionsShouldReturnTrueWhenConditionsExist() {
        KeyUser user = KeyUser.builder()
                .pubkeyHex(TEST_PUBKEY)
                .signingCondition(SigningCondition.allowEventKind(1))
                .build();

        assertThat(user.hasSigningConditions()).isTrue();
    }

    @Test
    void hasSigningConditionsShouldReturnFalseWhenNoConditions() {
        KeyUser user = KeyUser.builder()
                .pubkeyHex(TEST_PUBKEY)
                .build();

        assertThat(user.hasSigningConditions()).isFalse();
    }

    @Test
    void hasBeenUsedShouldReturnTrueWhenSigningCountPositive() {
        KeyUser user = KeyUser.builder()
                .pubkeyHex(TEST_PUBKEY)
                .signingCount(1)
                .build();

        assertThat(user.hasBeenUsed()).isTrue();
    }

    @Test
    void hasBeenUsedShouldReturnTrueWhenLastUsedAtSet() {
        KeyUser user = KeyUser.builder()
                .pubkeyHex(TEST_PUBKEY)
                .lastUsedAt(Instant.now())
                .build();

        assertThat(user.hasBeenUsed()).isTrue();
    }

    @Test
    void hasBeenUsedShouldReturnFalseWhenNeverUsed() {
        KeyUser user = KeyUser.builder()
                .pubkeyHex(TEST_PUBKEY)
                .build();

        assertThat(user.hasBeenUsed()).isFalse();
    }

    @Test
    void isAccessValidShouldReturnTrueWhenActiveAndNoPolicy() {
        KeyUser user = KeyUser.builder()
                .pubkeyHex(TEST_PUBKEY)
                .active(true)
                .build();

        assertThat(user.isAccessValid()).isTrue();
    }

    @Test
    void isAccessValidShouldReturnTrueWhenActiveAndValidPolicy() {
        BunkerPolicy validPolicy = BunkerPolicy.builder()
                .name("test")
                .active(true)
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();

        KeyUser user = KeyUser.builder()
                .pubkeyHex(TEST_PUBKEY)
                .active(true)
                .policy(validPolicy)
                .build();

        assertThat(user.isAccessValid()).isTrue();
    }

    @Test
    void isAccessValidShouldReturnFalseWhenNotActive() {
        KeyUser user = KeyUser.builder()
                .pubkeyHex(TEST_PUBKEY)
                .active(false)
                .build();

        assertThat(user.isAccessValid()).isFalse();
    }

    @Test
    void isAccessValidShouldReturnFalseWhenPolicyInvalid() {
        BunkerPolicy expiredPolicy = BunkerPolicy.builder()
                .name("test")
                .active(true)
                .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();

        KeyUser user = KeyUser.builder()
                .pubkeyHex(TEST_PUBKEY)
                .active(true)
                .policy(expiredPolicy)
                .build();

        assertThat(user.isAccessValid()).isFalse();
    }

    @Test
    void toBuilderShouldCreateCopy() {
        KeyUser original = KeyUser.builder()
                .pubkeyHex(TEST_PUBKEY)
                .keyName("key")
                .signingCount(10)
                .build();

        KeyUser copy = original.toBuilder()
                .signingCount(20)
                .build();

        assertThat(copy.getPubkeyHex()).isEqualTo(TEST_PUBKEY);
        assertThat(copy.getSigningCount()).isEqualTo(20);
        assertThat(original.getSigningCount()).isEqualTo(10);
    }

    @Test
    void equalsShouldWorkCorrectly() {
        KeyUser user1 = KeyUser.builder()
                .pubkeyHex(TEST_PUBKEY)
                .keyName("key")
                .build();

        KeyUser user2 = KeyUser.builder()
                .pubkeyHex(TEST_PUBKEY)
                .keyName("key")
                .build();

        assertThat(user1).isEqualTo(user2);
        assertThat(user1.hashCode()).isEqualTo(user2.hashCode());
    }

    @Test
    void shouldDeserializeFromJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        String json = """
            {
                "pubkey": "79be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798",
                "npub": "npub1test",
                "key_name": "my-key",
                "signing_count": 100,
                "active": true
            }
            """;

        KeyUser user = mapper.readValue(json, KeyUser.class);

        assertThat(user.getPubkeyHex()).isEqualTo(TEST_PUBKEY);
        assertThat(user.getNpub()).isEqualTo("npub1test");
        assertThat(user.getKeyName()).isEqualTo("my-key");
        assertThat(user.getSigningCount()).isEqualTo(100);
    }
}
