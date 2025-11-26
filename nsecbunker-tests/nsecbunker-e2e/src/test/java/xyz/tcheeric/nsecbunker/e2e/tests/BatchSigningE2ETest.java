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
import xyz.tcheeric.nsecbunker.admin.permission.PermissionManager;
import xyz.tcheeric.nsecbunker.admin.policy.PolicyManager;
import xyz.tcheeric.nsecbunker.admin.token.TokenManager;
import xyz.tcheeric.nsecbunker.core.model.AccessToken;
import xyz.tcheeric.nsecbunker.core.model.BunkerKey;
import xyz.tcheeric.nsecbunker.core.model.BunkerPolicy;
import xyz.tcheeric.nsecbunker.core.model.PolicyRule;
import xyz.tcheeric.nsecbunker.e2e.E2ETestBase;
import nostr.id.Identity;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * E2E tests for batch signing operations setup.
 *
 * <p>Tests the setup required for batch signing: multiple keys, shared policies,
 * and concurrent token generation. Verifies that the bunker can handle
 * multiple simultaneous operations.
 *
 * <p>Note: Full batch signing requires a client-side transport implementation
 * that connects to relays. These tests verify the admin-side setup is correct
 * for batch operations.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Batch Signing E2E Tests")
class BatchSigningE2ETest extends E2ETestBase {

    private NsecBunkerAdminClient adminClient;
    private KeyManager keyManager;
    private PolicyManager policyManager;
    private PermissionManager permissionManager;
    private TokenManager tokenManager;
    private static final Duration E2E_TIMEOUT = Duration.ofSeconds(120);

    // Test fixtures to clean up
    private List<String> createdKeyNames = new ArrayList<>();
    private List<String> createdPolicyIds = new ArrayList<>();
    private List<String> createdTokenIds = new ArrayList<>();

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
        await().atMost(E2E_TIMEOUT)
                .untilAsserted(() -> assertThat(adminClient.ping().get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS)).isEqualTo("pong"));

        keyManager = adminClient.keyManager();
        policyManager = adminClient.policyManager();
        permissionManager = adminClient.permissionManager();
        tokenManager = adminClient.tokenManager();

        createdKeyNames = new ArrayList<>();
        createdPolicyIds = new ArrayList<>();
        createdTokenIds = new ArrayList<>();
    }

    @AfterEach
    void tearDown() throws Exception {
        // Clean up created resources
        for (String tokenId : createdTokenIds) {
            try {
                tokenManager.revokeToken(tokenId).get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("Failed to revoke token {}: {}", tokenId, e.getMessage());
            }
        }

        for (String policyId : createdPolicyIds) {
            try {
                policyManager.deletePolicy(policyId).get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("Failed to delete policy {}: {}", policyId, e.getMessage());
            }
        }

        for (String keyName : createdKeyNames) {
            try {
                keyManager.deleteKey(keyName).get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("Failed to delete key {}: {}", keyName, e.getMessage());
            }
        }

        if (adminClient != null) {
            adminClient.disconnect();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Should create multiple keys for batch operations")
    void shouldCreateMultipleKeys() throws Exception {
        int numKeys = 5;
        List<BunkerKey> keys = new ArrayList<>();

        for (int i = 0; i < numKeys; i++) {
            String keyName = "batch-key-" + i + "-" + UUID.randomUUID().toString().substring(0, 8);
            BunkerKey key = keyManager.createKey(keyName, "batch-passphrase-" + i)
                    .get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);

            keys.add(key);
            createdKeyNames.add(keyName);

            assertThat(key.getNpub()).startsWith("npub1");
        }

        assertThat(keys).hasSize(numKeys);

        // Verify all keys are unique
        List<String> npubs = keys.stream().map(BunkerKey::getNpub).toList();
        assertThat(npubs).doesNotHaveDuplicates();

        log.info("Created {} keys for batch operations", numKeys);
    }

    @Test
    @Order(2)
    @DisplayName("Should create keys concurrently")
    void shouldCreateKeysConcurrently() throws Exception {
        int numKeys = 3;
        List<CompletableFuture<BunkerKey>> futures = new ArrayList<>();

        // Submit key creation requests concurrently
        for (int i = 0; i < numKeys; i++) {
            String keyName = "concurrent-key-" + i + "-" + UUID.randomUUID().toString().substring(0, 8);
            createdKeyNames.add(keyName);

            futures.add(keyManager.createKey(keyName, "concurrent-passphrase-" + i));
        }

        // Wait for all to complete
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0]));
        allFutures.get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);

        // Collect results
        List<BunkerKey> keys = new ArrayList<>();
        for (CompletableFuture<BunkerKey> future : futures) {
            keys.add(future.get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS));
        }

        assertThat(keys).hasSize(numKeys);
        keys.forEach(key -> assertThat(key.getNpub()).startsWith("npub1"));

        log.info("Created {} keys concurrently", numKeys);
    }

    @Test
    @Order(3)
    @DisplayName("Should assign same policy to multiple keys")
    void shouldAssignSamePolicyToMultipleKeys() throws Exception {
        // Create a shared batch signing policy
        BunkerPolicy batchPolicy = policyManager.createPolicy(
                BunkerPolicy.builder()
                        .name("batch-policy-" + UUID.randomUUID().toString().substring(0, 8))
                        .description("Shared policy for batch signing")
                        .rule(PolicyRule.allowMethod("sign_event"))
                        .rule(PolicyRule.allowMethod("get_public_key"))
                        .rule(PolicyRule.allowEventKind(1))
                        .rule(PolicyRule.builder()
                                .type(PolicyRule.RuleType.ALLOW)
                                .method("sign_event")
                                .maxUsage(1000L)
                                .build())
                        .build()
        ).get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);
        createdPolicyIds.add(batchPolicy.getId());

        // Create multiple keys
        List<String> keyNames = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            String keyName = "policy-batch-key-" + i + "-" + UUID.randomUUID().toString().substring(0, 8);
            keyManager.createKey(keyName, "passphrase-" + i).get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);
            createdKeyNames.add(keyName);
            keyNames.add(keyName);
        }

        // Create a test user
        Identity testUser = generateTestIdentity();
        String userPubkey = testUser.getPublicKey().toString();

        // Grant permission with the shared policy to all keys
        for (String keyName : keyNames) {
            permissionManager.grantPermission(keyName, userPubkey, batchPolicy)
                    .get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);
        }

        log.info("Assigned policy {} to {} keys for user {}",
                batchPolicy.getId(), keyNames.size(), testUser.getPublicKey().toBech32String());

        // Verify all permissions are granted
        for (String keyName : keyNames) {
            var permissions = permissionManager.getPermissions(keyName, userPubkey)
                    .get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);
            assertThat(permissions.isActive()).isTrue();

            // Clean up
            permissionManager.revokePermission(keyName, userPubkey).get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);
        }
    }

    @Test
    @Order(4)
    @DisplayName("Should generate multiple tokens for batch client")
    void shouldGenerateMultipleTokensForBatchClient() throws Exception {
        // Create key
        String keyName = "multi-token-key-" + UUID.randomUUID().toString().substring(0, 8);
        keyManager.createKey(keyName, "passphrase").get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);
        createdKeyNames.add(keyName);

        // Create policy
        BunkerPolicy policy = policyManager.createPolicy(
                BunkerPolicy.builder()
                        .name("multi-token-policy-" + UUID.randomUUID().toString().substring(0, 8))
                        .rule(PolicyRule.allowMethod("sign_event"))
                        .build()
        ).get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);
        createdPolicyIds.add(policy.getId());

        // Generate multiple tokens for different clients
        List<AccessToken> tokens = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            AccessToken token = tokenManager.createToken(
                    keyName,
                    "batch-client-" + i,
                    policy.getId(),
                    Duration.ofHours(24)
            ).get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);

            tokens.add(token);
            createdTokenIds.add(token.getId());
        }

        assertThat(tokens).hasSize(5);
        tokens.forEach(token -> {
            assertThat(token.isValid()).isTrue();
            assertThat(token.getToken()).isNotBlank();
        });

        // Verify all tokens are unique
        List<String> tokenValues = tokens.stream().map(AccessToken::getToken).toList();
        assertThat(tokenValues).doesNotHaveDuplicates();

        log.info("Generated {} tokens for batch signing", tokens.size());
    }

    @Test
    @Order(5)
    @DisplayName("Should handle concurrent token generation")
    void shouldHandleConcurrentTokenGeneration() throws Exception {
        // Create key
        String keyName = "concurrent-token-key-" + UUID.randomUUID().toString().substring(0, 8);
        keyManager.createKey(keyName, "passphrase").get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);
        createdKeyNames.add(keyName);

        // Create policy
        BunkerPolicy policy = policyManager.createPolicy(
                BunkerPolicy.builder()
                        .name("concurrent-token-policy-" + UUID.randomUUID().toString().substring(0, 8))
                        .rule(PolicyRule.allowMethod("sign_event"))
                        .build()
        ).get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);
        createdPolicyIds.add(policy.getId());

        // Submit concurrent token generation requests
        int numTokens = 3;
        List<CompletableFuture<AccessToken>> futures = new ArrayList<>();

        for (int i = 0; i < numTokens; i++) {
            futures.add(tokenManager.createToken(
                    keyName,
                    "concurrent-client-" + i,
                    policy.getId(),
                    Duration.ofHours(1)
            ));
        }

        // Wait for all to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);

        // Collect results
        for (CompletableFuture<AccessToken> future : futures) {
            AccessToken token = future.get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);
            assertThat(token.isValid()).isTrue();
            createdTokenIds.add(token.getId());
        }

        log.info("Generated {} tokens concurrently", numTokens);
    }

    @Test
    @Order(6)
    @DisplayName("Should complete full batch setup flow")
    void shouldCompleteFullBatchSetupFlow() throws Exception {
        int numKeys = 3;
        int numClientsPerKey = 2;

        // 1. Create a batch signing policy
        log.info("Step 1: Creating batch signing policy");
        BunkerPolicy batchPolicy = policyManager.createPolicy(
                BunkerPolicy.builder()
                        .name("full-batch-policy-" + UUID.randomUUID().toString().substring(0, 8))
                        .description("Policy for batch signing operations")
                        .rule(PolicyRule.allowMethod("sign_event"))
                        .rule(PolicyRule.allowMethod("get_public_key"))
                        .rule(PolicyRule.allowMethod("ping"))
                        .build()
        ).get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);
        createdPolicyIds.add(batchPolicy.getId());
        log.info("Policy created: id={}", batchPolicy.getId());

        // 2. Create multiple keys
        log.info("Step 2: Creating {} keys", numKeys);
        List<BunkerKey> keys = new ArrayList<>();
        for (int i = 0; i < numKeys; i++) {
            String keyName = "full-batch-key-" + i + "-" + UUID.randomUUID().toString().substring(0, 8);
            BunkerKey key = keyManager.createKey(keyName, "batch-passphrase-" + i)
                    .get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);
            keys.add(key);
            createdKeyNames.add(keyName);

            // Unlock the key
            keyManager.unlockKey(keyName, "batch-passphrase-" + i).get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);
        }
        log.info("Created and unlocked {} keys", keys.size());

        // 3. Create test users
        log.info("Step 3: Creating test users and granting permissions");
        List<Identity> testUsers = new ArrayList<>();
        for (int i = 0; i < numClientsPerKey; i++) {
            testUsers.add(generateTestIdentity());
        }

        // Grant each user permission to all keys
        for (Identity user : testUsers) {
            String userPubkey = user.getPublicKey().toString();
            for (int i = 0; i < keys.size(); i++) {
                permissionManager.grantPermission(createdKeyNames.get(i), userPubkey, batchPolicy)
                        .get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);
            }
        }
        log.info("Granted permissions to {} users across {} keys", testUsers.size(), keys.size());

        // 4. Generate tokens for each user-key combination
        log.info("Step 4: Generating tokens");
        List<AccessToken> allTokens = new ArrayList<>();
        for (int userIdx = 0; userIdx < testUsers.size(); userIdx++) {
            for (int keyIdx = 0; keyIdx < keys.size(); keyIdx++) {
                AccessToken token = tokenManager.createToken(
                        createdKeyNames.get(keyIdx),
                        "batch-user-" + userIdx + "-key-" + keyIdx,
                        batchPolicy.getId(),
                        Duration.ofDays(1)
                ).get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);

                allTokens.add(token);
                createdTokenIds.add(token.getId());
            }
        }
        log.info("Generated {} tokens total", allTokens.size());

        // 5. Verify setup
        log.info("Step 5: Verifying batch setup");

        // All keys should be unlocked
        for (String keyName : createdKeyNames) {
            BunkerKey details = keyManager.getKeyDetails(keyName).get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);
            assertThat(details.isLocked()).isFalse();
        }

        // All tokens should be valid
        for (AccessToken token : allTokens) {
            Boolean valid = tokenManager.validateToken(token.getToken())
                    .get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);
            assertThat(valid).isTrue();
        }

        // Connection strings should be well-formed
        for (AccessToken token : allTokens) {
            String connectionString = token.getConnectionString();
            assertThat(connectionString).startsWith("bunker://npub1");
        }

        log.info("Full batch setup completed successfully:");
        log.info("  - Keys: {}", numKeys);
        log.info("  - Users per key: {}", numClientsPerKey);
        log.info("  - Total tokens: {}", allTokens.size());

        // Clean up permissions
        for (Identity user : testUsers) {
            String userPubkey = user.getPublicKey().toString();
            for (String keyName : createdKeyNames) {
                try {
                    permissionManager.revokePermission(keyName, userPubkey).get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);
                } catch (Exception e) {
                    // Ignore cleanup errors
                }
            }
        }
    }

    @Test
    @Order(7)
    @DisplayName("Should list keys efficiently")
    void shouldListKeysEfficiently() throws Exception {
        // Create multiple keys
        int numKeys = 5;
        for (int i = 0; i < numKeys; i++) {
            String keyName = "list-test-key-" + i + "-" + UUID.randomUUID().toString().substring(0, 8);
            keyManager.createKey(keyName, "passphrase-" + i).get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);
            createdKeyNames.add(keyName);
        }

        // List all keys
        long startTime = System.currentTimeMillis();
        List<BunkerKey> keys = keyManager.listKeys().get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - startTime;

        assertThat(keys.size()).isGreaterThanOrEqualTo(numKeys);

        log.info("Listed {} keys in {}ms", keys.size(), duration);

        // Verify our keys are in the list
        List<String> keyNames = keys.stream().map(BunkerKey::getName).toList();
        for (String expectedName : createdKeyNames) {
            assertThat(keyNames).contains(expectedName);
        }
    }
}
