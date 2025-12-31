package xyz.tcheeric.nsecbunker.core.testing;

import xyz.tcheeric.nsecbunker.core.model.AccessToken;
import xyz.tcheeric.nsecbunker.core.model.BunkerConnection;
import xyz.tcheeric.nsecbunker.core.model.BunkerKey;
import xyz.tcheeric.nsecbunker.core.model.BunkerPolicy;
import xyz.tcheeric.nsecbunker.core.model.KeyUser;
import xyz.tcheeric.nsecbunker.core.model.PolicyRule;
import xyz.tcheeric.nsecbunker.core.model.SigningCondition;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Test fixtures providing pre-built test data and factory methods.
 *
 * <p>This class contains well-known test values, pre-built model instances,
 * and factory methods for creating test data with sensible defaults.
 *
 * <p>Usage:
 * <pre>{@code
 * // Use well-known test keys
 * String pubkey = TestFixtures.ALICE_PUBKEY;
 *
 * // Use pre-built fixtures
 * BunkerKey key = TestFixtures.bunkerKey();
 * BunkerConnection conn = TestFixtures.bunkerConnection();
 *
 * // Use builders with custom values
 * BunkerKey customKey = TestFixtures.bunkerKeyBuilder()
 *     .name("custom")
 *     .build();
 * }</pre>
 */
public final class TestFixtures {

    private TestFixtures() {
        // Utility class
    }

    // ========== Well-known Test Keys ==========

    /**
     * Alice's private key (secp256k1 scalar = 1).
     */
    public static final String ALICE_PRIVATE_KEY =
            "0000000000000000000000000000000000000000000000000000000000000001";

    /**
     * Alice's public key (corresponding to private key = 1).
     */
    public static final String ALICE_PUBKEY =
            "79be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798";

    /**
     * Alice's npub (bech32 encoded public key).
     */
    public static final String ALICE_NPUB =
            "npub108nmlcg3xmelt2rthf5weqf0wpvuf00y6spdrwu0l9qp9t8lu0vsn96l6z";

    /**
     * Bob's private key (secp256k1 scalar = 2).
     */
    public static final String BOB_PRIVATE_KEY =
            "0000000000000000000000000000000000000000000000000000000000000002";

    /**
     * Bob's public key (corresponding to private key = 2).
     */
    public static final String BOB_PUBKEY =
            "c6047f9441ed7d6d3045406e95c07cd85c778e4b8cef3ca7abac09b95c709ee5";

    /**
     * Bob's npub (bech32 encoded public key).
     */
    public static final String BOB_NPUB =
            "npub1csp805evg04kdsz29qm54q87mx9cau5f3n8udr40vgxhkthq800sn3t59v";

    /**
     * Charlie's private key (secp256k1 scalar = 3).
     */
    public static final String CHARLIE_PRIVATE_KEY =
            "0000000000000000000000000000000000000000000000000000000000000003";

    /**
     * Charlie's public key.
     */
    public static final String CHARLIE_PUBKEY =
            "f9308a019258c31049344f85f89d5229b531c845836f99b08601f113bce036f9";

    // ========== Well-known Test Relays ==========

    /**
     * Test relay URL 1.
     */
    public static final String RELAY_URL_1 = "wss://relay1.example.com";

    /**
     * Test relay URL 2.
     */
    public static final String RELAY_URL_2 = "wss://relay2.example.com";

    /**
     * Test relay URL 3.
     */
    public static final String RELAY_URL_3 = "wss://relay3.example.com";

    /**
     * List of common test relay URLs.
     */
    public static final List<String> RELAY_URLS = List.of(RELAY_URL_1, RELAY_URL_2);

    // ========== Well-known Test Secrets/Tokens ==========

    /**
     * Test secret for authentication.
     */
    public static final String TEST_SECRET = "test-secret-12345";

    /**
     * Test token for authentication.
     */
    public static final String TEST_TOKEN = "test-token-abcdef";

    // ========== BunkerConnection Fixtures ==========

    /**
     * Creates a basic BunkerConnection with default values.
     *
     * @return a pre-configured BunkerConnection
     */
    public static BunkerConnection bunkerConnection() {
        return bunkerConnectionBuilder().build();
    }

    /**
     * Creates a BunkerConnection builder with default values.
     *
     * @return a pre-configured builder
     */
    public static BunkerConnection.BunkerConnectionBuilder bunkerConnectionBuilder() {
        return BunkerConnection.builder()
                .remotePubkey(ALICE_PUBKEY)
                .relays(RELAY_URLS);
    }

    /**
     * Creates a BunkerConnection with secret authentication.
     *
     * @return a BunkerConnection with secret
     */
    public static BunkerConnection bunkerConnectionWithSecret() {
        return bunkerConnectionBuilder()
                .secret(TEST_SECRET)
                .build();
    }

    /**
     * Creates a BunkerConnection with token authentication.
     *
     * @return a BunkerConnection with token
     */
    public static BunkerConnection bunkerConnectionWithToken() {
        return bunkerConnectionBuilder()
                .token(TEST_TOKEN)
                .build();
    }

    // ========== BunkerKey Fixtures ==========

    /**
     * Creates a basic BunkerKey with default values.
     *
     * @return a pre-configured BunkerKey
     */
    public static BunkerKey bunkerKey() {
        return bunkerKeyBuilder().build();
    }

    /**
     * Creates a BunkerKey builder with default values.
     *
     * @return a pre-configured builder
     */
    public static BunkerKey.BunkerKeyBuilder bunkerKeyBuilder() {
        return BunkerKey.builder()
                .name("test-key")
                .pubkeyHex(ALICE_PUBKEY)
                .npub(ALICE_NPUB)
                .createdAt(Instant.now().minus(7, ChronoUnit.DAYS));
    }

    /**
     * Creates a BunkerKey with usage statistics.
     *
     * @return a BunkerKey with usage data
     */
    public static BunkerKey bunkerKeyWithUsage() {
        return bunkerKeyBuilder()
                .userCount(5)
                .tokenCount(10)
                .signingCount(100)
                .lastUsedAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();
    }

    /**
     * Creates a locked BunkerKey.
     *
     * @return a locked BunkerKey
     */
    public static BunkerKey lockedBunkerKey() {
        return bunkerKeyBuilder()
                .locked(true)
                .build();
    }

    // ========== BunkerPolicy Fixtures ==========

    /**
     * Creates a basic BunkerPolicy with default values.
     *
     * @return a pre-configured BunkerPolicy
     */
    public static BunkerPolicy bunkerPolicy() {
        return bunkerPolicyBuilder().build();
    }

    /**
     * Creates a BunkerPolicy builder with default values.
     *
     * @return a pre-configured builder
     */
    public static BunkerPolicy.BunkerPolicyBuilder bunkerPolicyBuilder() {
        return BunkerPolicy.builder()
                .id(randomId())
                .name("test-policy")
                .description("Test policy for unit tests");
    }

    /**
     * Creates a BunkerPolicy that allows all operations.
     *
     * @return an allow-all policy
     */
    public static BunkerPolicy allowAllPolicy() {
        return bunkerPolicyBuilder()
                .name("allow-all")
                .rules(List.of(
                        PolicyRule.builder()
                                .type(PolicyRule.RuleType.ALLOW)
                                .build()
                ))
                .build();
    }

    /**
     * Creates a BunkerPolicy that denies all operations.
     *
     * @return a deny-all policy
     */
    public static BunkerPolicy denyAllPolicy() {
        return bunkerPolicyBuilder()
                .name("deny-all")
                .rules(List.of(
                        PolicyRule.builder()
                                .type(PolicyRule.RuleType.DENY)
                                .build()
                ))
                .build();
    }

    /**
     * Creates a BunkerPolicy that only allows signing kind 1 events.
     *
     * @return a policy allowing only kind 1
     */
    public static BunkerPolicy kindOneOnlyPolicy() {
        return bunkerPolicyBuilder()
                .name("kind-1-only")
                .rules(List.of(
                        PolicyRule.builder()
                                .type(PolicyRule.RuleType.ALLOW)
                                .method("sign_event")
                                .eventKind(1)
                                .build(),
                        PolicyRule.builder()
                                .type(PolicyRule.RuleType.DENY)
                                .build()
                ))
                .build();
    }

    // ========== PolicyRule Fixtures ==========

    /**
     * Creates an allow-all PolicyRule.
     *
     * @return an allow rule
     */
    public static PolicyRule allowRule() {
        return PolicyRule.builder()
                .type(PolicyRule.RuleType.ALLOW)
                .build();
    }

    /**
     * Creates a deny-all PolicyRule.
     *
     * @return a deny rule
     */
    public static PolicyRule denyRule() {
        return PolicyRule.builder()
                .type(PolicyRule.RuleType.DENY)
                .build();
    }

    /**
     * Creates a PolicyRule for a specific method.
     *
     * @param method the allowed method
     * @return a rule for the specified method
     */
    public static PolicyRule allowMethod(String method) {
        return PolicyRule.builder()
                .type(PolicyRule.RuleType.ALLOW)
                .method(method)
                .build();
    }

    /**
     * Creates a PolicyRule for a specific event kind.
     *
     * @param eventKind the allowed event kind
     * @return a rule for the specified kind
     */
    public static PolicyRule allowEventKind(int eventKind) {
        return PolicyRule.builder()
                .type(PolicyRule.RuleType.ALLOW)
                .eventKind(eventKind)
                .build();
    }

    // ========== KeyUser Fixtures ==========

    /**
     * Creates a basic KeyUser with default values.
     *
     * @return a pre-configured KeyUser
     */
    public static KeyUser keyUser() {
        return keyUserBuilder().build();
    }

    /**
     * Creates a KeyUser builder with default values.
     *
     * @return a pre-configured builder
     */
    public static KeyUser.KeyUserBuilder keyUserBuilder() {
        return KeyUser.builder()
                .pubkeyHex(BOB_PUBKEY)
                .description("test-user")
                .createdAt(Instant.now().minus(3, ChronoUnit.DAYS));
    }

    /**
     * Creates a KeyUser with an associated policy.
     *
     * @param policy the policy to associate
     * @return a KeyUser with the policy
     */
    public static KeyUser keyUserWithPolicy(BunkerPolicy policy) {
        return keyUserBuilder()
                .policy(policy)
                .build();
    }

    // ========== SigningCondition Fixtures ==========

    /**
     * Creates a basic SigningCondition.
     *
     * @return a pre-configured SigningCondition
     */
    public static SigningCondition signingCondition() {
        return signingConditionBuilder().build();
    }

    /**
     * Creates a SigningCondition builder with default values.
     *
     * @return a pre-configured builder
     */
    public static SigningCondition.SigningConditionBuilder signingConditionBuilder() {
        return SigningCondition.builder()
                .method("sign_event");
    }

    /**
     * Creates a SigningCondition for a specific event kind.
     *
     * @param eventKind the allowed event kind
     * @return a condition for the specified kind
     */
    public static SigningCondition signingConditionForKind(int eventKind) {
        return signingConditionBuilder()
                .eventKind(eventKind)
                .build();
    }

    /**
     * Creates a rate-limited SigningCondition.
     *
     * @param maxOperations maximum operations per window
     * @param windowSeconds window duration in seconds
     * @return a rate-limited condition
     */
    public static SigningCondition rateLimitedCondition(int maxOperations, int windowSeconds) {
        return signingConditionBuilder()
                .type(SigningCondition.ConditionType.RATE_LIMIT)
                .maxOperations(maxOperations)
                .rateLimitWindowSeconds(windowSeconds)
                .build();
    }

    // ========== AccessToken Fixtures ==========

    /**
     * Creates a basic AccessToken with default values.
     *
     * @return a pre-configured AccessToken
     */
    public static AccessToken accessToken() {
        return accessTokenBuilder().build();
    }

    /**
     * Creates an AccessToken builder with default values.
     *
     * @return a pre-configured builder
     */
    public static AccessToken.AccessTokenBuilder accessTokenBuilder() {
        return AccessToken.builder()
                .token(randomId())
                .keyName("test-key")
                .keyNpub(ALICE_NPUB)
                .createdAt(Instant.now());
    }

    /**
     * Creates an AccessToken with expiration.
     *
     * @param expiresIn duration until expiration
     * @param unit the time unit
     * @return a token that expires
     */
    public static AccessToken expiringToken(long expiresIn, ChronoUnit unit) {
        return accessTokenBuilder()
                .expiresAt(Instant.now().plus(expiresIn, unit))
                .build();
    }

    /**
     * Creates an already-expired AccessToken.
     *
     * @return an expired token
     */
    public static AccessToken expiredToken() {
        return accessTokenBuilder()
                .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();
    }

    /**
     * Creates an AccessToken with an associated policy.
     *
     * @param policy the policy
     * @return a token with the policy
     */
    public static AccessToken tokenWithPolicy(BunkerPolicy policy) {
        return accessTokenBuilder()
                .policy(policy)
                .build();
    }

    // ========== Utility Methods ==========

    /**
     * Generates a random ID suitable for test data.
     *
     * @return a random UUID string
     */
    public static String randomId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generates a random hex string of the specified length.
     *
     * @param length the desired length (must be even)
     * @return a random hex string
     */
    public static String randomHex(int length) {
        if (length % 2 != 0) {
            throw new IllegalArgumentException("Length must be even");
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(Integer.toHexString((int) (Math.random() * 16)));
        }
        return sb.toString();
    }

    /**
     * Generates a random 32-byte hex string (like a pubkey or event ID).
     *
     * @return a 64-character hex string
     */
    public static String randomPubkey() {
        return randomHex(64);
    }

    /**
     * Creates a timestamp for a specific time in the past.
     *
     * @param amount the amount to subtract
     * @param unit   the time unit
     * @return the past timestamp
     */
    public static Instant pastInstant(long amount, ChronoUnit unit) {
        return Instant.now().minus(amount, unit);
    }

    /**
     * Creates a timestamp for a specific time in the future.
     *
     * @param amount the amount to add
     * @param unit   the time unit
     * @return the future timestamp
     */
    public static Instant futureInstant(long amount, ChronoUnit unit) {
        return Instant.now().plus(amount, unit);
    }
}
