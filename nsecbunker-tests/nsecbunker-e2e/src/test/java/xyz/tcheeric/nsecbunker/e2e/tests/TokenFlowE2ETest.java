package xyz.tcheeric.nsecbunker.e2e.tests;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import xyz.tcheeric.nsecbunker.admin.NsecBunkerAdminClient;
import xyz.tcheeric.nsecbunker.admin.key.KeyManager;
import xyz.tcheeric.nsecbunker.admin.policy.PolicyManager;
import xyz.tcheeric.nsecbunker.admin.token.TokenManager;
import xyz.tcheeric.nsecbunker.core.model.AccessToken;
import xyz.tcheeric.nsecbunker.core.model.BunkerKey;
import xyz.tcheeric.nsecbunker.core.model.BunkerPolicy;
import xyz.tcheeric.nsecbunker.core.model.PolicyRule;
import xyz.tcheeric.nsecbunker.e2e.E2ETestBase;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * E2E tests for token management operations.
 *
 * <p>Tests: create token, list tokens, get token, revoke token, validate token.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Token Flow E2E Tests")
class TokenFlowE2ETest extends E2ETestBase {

    private NsecBunkerAdminClient adminClient;
    private KeyManager keyManager;
    private PolicyManager policyManager;
    private TokenManager tokenManager;

    // Test fixtures
    private String testKeyName;
    private BunkerKey testKey;
    private BunkerPolicy testPolicy;

    @BeforeEach
    void setUp() throws Exception {
        adminClient = NsecBunkerAdminClient.builder()
                .bunkerPubkey(getBunkerNpub())
                .adminPrivateKey(getAdminNsec())
                .relay(getRelayUrl())
                .connectTimeout(DEFAULT_CONNECT_TIMEOUT)
                .requestTimeout(DEFAULT_REQUEST_TIMEOUT)
                .useEphemeralKey(false) // Use admin key directly for jaonoctus/nsecbunkerd
                .build();

        adminClient.connect();

        await().atMost(30, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(adminClient.isConnected()).isTrue());

        keyManager = adminClient.keyManager();
        policyManager = adminClient.policyManager();
        tokenManager = adminClient.tokenManager();

        // Create test fixtures
        testKeyName = "token-test-key-" + UUID.randomUUID().toString().substring(0, 8);
        testKey = keyManager.createKey(testKeyName, "test-passphrase")
                .get(30, TimeUnit.SECONDS);

        testPolicy = policyManager.createPolicy(
                BunkerPolicy.builder()
                        .name("token-test-policy-" + UUID.randomUUID().toString().substring(0, 8))
                        .rule(PolicyRule.allowMethod("sign_event"))
                        .rule(PolicyRule.allowMethod("get_public_key"))
                        .build()
        ).get(30, TimeUnit.SECONDS);

        log.info("Test fixtures created: key={}, policy={}",
                testKeyName, testPolicy.getId());
    }

    @AfterEach
    void tearDown() throws Exception {
        // Clean up test fixtures
        try {
            if (testPolicy != null && testPolicy.getId() != null) {
                policyManager.deletePolicy(testPolicy.getId()).get(30, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.warn("Failed to delete test policy: {}", e.getMessage());
        }

        try {
            if (testKeyName != null) {
                keyManager.deleteKey(testKeyName).get(30, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.warn("Failed to delete test key: {}", e.getMessage());
        }

        if (adminClient != null) {
            adminClient.disconnect();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Should create a token without expiry")
    void shouldCreateTokenWithoutExpiry() throws Exception {
        String clientName = "test-client-" + UUID.randomUUID().toString().substring(0, 8);

        // Create token without expiry
        AccessToken token = tokenManager.createToken(testKeyName, clientName, testPolicy.getId(), null)
                .get(30, TimeUnit.SECONDS);

        assertThat(token).isNotNull();
        assertThat(token.getId()).isNotNull().isNotBlank();
        assertThat(token.getKeyName()).isEqualTo(testKeyName);
        assertThat(token.getClientName()).isEqualTo(clientName);
        assertThat(token.getToken()).isNotNull().isNotBlank();

        log.info("Created token: id={}, clientName={}", token.getId(), token.getClientName());
        // Note: revoke_token not implemented in nsecbunkerd, so tokens persist
    }

    @Test
    @Order(2)
    @DisplayName("Should create a token with expiry")
    void shouldCreateTokenWithExpiry() throws Exception {
        String clientName = "expiring-client-" + UUID.randomUUID().toString().substring(0, 8);
        Duration lifetime = Duration.ofHours(1);

        // Create token with expiry
        AccessToken token = tokenManager.createToken(testKeyName, clientName, testPolicy.getId(), lifetime)
                .get(30, TimeUnit.SECONDS);

        assertThat(token).isNotNull();
        assertThat(token.getExpiresAt()).isNotNull();
        assertThat(token.isExpired()).isFalse();

        log.info("Created token with expiry: id={}, expiresAt={}", token.getId(), token.getExpiresAt());
        // Note: revoke_token not implemented in nsecbunkerd, so tokens persist
    }

    @Test
    @Order(3)
    @DisplayName("Should create a token without policy")
    void shouldCreateTokenWithoutPolicy() throws Exception {
        String clientName = "no-policy-client-" + UUID.randomUUID().toString().substring(0, 8);

        // Create token without policy
        AccessToken token = tokenManager.createToken(testKeyName, clientName, null, null)
                .get(30, TimeUnit.SECONDS);

        assertThat(token).isNotNull();
        assertThat(token.getId()).isNotNull();
        assertThat(token.getToken()).isNotNull();

        log.info("Created token without policy: id={}", token.getId());

        // Clean up
        tokenManager.revokeToken(token.getId()).get(30, TimeUnit.SECONDS);
    }

    @Test
    @Order(4)
    @DisplayName("Should list all tokens for a key")
    void shouldListTokensForKey() throws Exception {
        String clientName1 = "list-test-client-1-" + UUID.randomUUID().toString().substring(0, 8);
        String clientName2 = "list-test-client-2-" + UUID.randomUUID().toString().substring(0, 8);

        // Create multiple tokens (policyId is required in nsecbunkerd)
        AccessToken token1 = tokenManager.createToken(testKeyName, clientName1, testPolicy.getId(), null)
                .get(30, TimeUnit.SECONDS);
        AccessToken token2 = tokenManager.createToken(testKeyName, clientName2, testPolicy.getId(), null)
                .get(30, TimeUnit.SECONDS);

        // List tokens
        List<AccessToken> tokens = tokenManager.listTokens(testKeyName)
                .get(30, TimeUnit.SECONDS);

        assertThat(tokens).isNotNull();
        assertThat(tokens.stream().map(AccessToken::getId))
                .contains(token1.getId(), token2.getId());

        log.info("Listed {} tokens for key {}", tokens.size(), testKeyName);
        // Note: revoke_token not implemented in nsecbunkerd, so tokens persist
    }

    @Test
    @Order(5)
    @DisplayName("Should get token by id")
    void shouldGetTokenById() throws Exception {
        String clientName = "get-test-client-" + UUID.randomUUID().toString().substring(0, 8);

        // Create token
        AccessToken created = tokenManager.createToken(testKeyName, clientName, testPolicy.getId(), null)
                .get(30, TimeUnit.SECONDS);

        // Get token by ID
        AccessToken retrieved = tokenManager.getToken(created.getId())
                .get(30, TimeUnit.SECONDS);

        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getId()).isEqualTo(created.getId());
        assertThat(retrieved.getClientName()).isEqualTo(clientName);

        log.info("Retrieved token: id={}, clientName={}", retrieved.getId(), retrieved.getClientName());

        // Clean up
        tokenManager.revokeToken(created.getId()).get(30, TimeUnit.SECONDS);
    }

    @Test
    @Order(6)
    @DisplayName("Should revoke token")
    void shouldRevokeToken() throws Exception {
        String clientName = "revoke-test-client-" + UUID.randomUUID().toString().substring(0, 8);

        // Create token
        AccessToken token = tokenManager.createToken(testKeyName, clientName, null, null)
                .get(30, TimeUnit.SECONDS);

        // Verify token exists
        List<AccessToken> tokensBefore = tokenManager.listTokens(testKeyName)
                .get(30, TimeUnit.SECONDS);
        assertThat(tokensBefore.stream().map(AccessToken::getId)).contains(token.getId());

        // Revoke token
        Boolean revoked = tokenManager.revokeToken(token.getId())
                .get(30, TimeUnit.SECONDS);

        assertThat(revoked).isTrue();
        log.info("Revoked token: id={}", token.getId());

        // Verify token is revoked (might still be in list but marked as revoked)
        AccessToken revokedToken = tokenManager.getToken(token.getId())
                .get(30, TimeUnit.SECONDS);
        assertThat(revokedToken.isRevoked()).isTrue();
    }

    @Test
    @Order(7)
    @DisplayName("Should validate token")
    void shouldValidateToken() throws Exception {
        String clientName = "validate-test-client-" + UUID.randomUUID().toString().substring(0, 8);

        // Create token
        AccessToken token = tokenManager.createToken(testKeyName, clientName, null, null)
                .get(30, TimeUnit.SECONDS);

        // Validate token
        Boolean valid = tokenManager.validateToken(token.getToken())
                .get(30, TimeUnit.SECONDS);

        assertThat(valid).isTrue();
        log.info("Token validated successfully");

        // Clean up
        tokenManager.revokeToken(token.getId()).get(30, TimeUnit.SECONDS);
    }

    @Test
    @Order(8)
    @DisplayName("Should complete full token lifecycle")
    void shouldCompleteFullTokenLifecycle() throws Exception {
        String clientName = "lifecycle-client-" + UUID.randomUUID().toString().substring(0, 8);

        // 1. Create token
        log.info("Step 1: Creating token");
        AccessToken created = tokenManager.createToken(
                testKeyName, clientName, testPolicy.getId(), Duration.ofHours(24))
                .get(30, TimeUnit.SECONDS);
        assertThat(created).isNotNull();
        assertThat(created.getToken()).isNotBlank();
        log.info("Token created: id={}", created.getId());

        // 2. List tokens and verify
        log.info("Step 2: Listing tokens");
        List<AccessToken> tokens = tokenManager.listTokens(testKeyName)
                .get(30, TimeUnit.SECONDS);
        assertThat(tokens.stream().map(AccessToken::getId)).contains(created.getId());
        log.info("Token found in list");

        // 3. Get token details
        log.info("Step 3: Getting token details");
        AccessToken details = tokenManager.getToken(created.getId())
                .get(30, TimeUnit.SECONDS);
        assertThat(details.getClientName()).isEqualTo(clientName);
        assertThat(details.getExpiresAt()).isNotNull();
        log.info("Token details retrieved");

        // 4. Validate token
        log.info("Step 4: Validating token");
        Boolean valid = tokenManager.validateToken(created.getToken())
                .get(30, TimeUnit.SECONDS);
        assertThat(valid).isTrue();
        log.info("Token validated");

        // 5. Revoke token
        log.info("Step 5: Revoking token");
        Boolean revoked = tokenManager.revokeToken(created.getId())
                .get(30, TimeUnit.SECONDS);
        assertThat(revoked).isTrue();
        log.info("Token revoked");

        // 6. Verify revocation
        log.info("Step 6: Verifying revocation");
        AccessToken revokedToken = tokenManager.getToken(created.getId())
                .get(30, TimeUnit.SECONDS);
        assertThat(revokedToken.isRevoked()).isTrue();
        log.info("Token lifecycle completed successfully");
    }
}
