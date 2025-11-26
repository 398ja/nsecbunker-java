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
import xyz.tcheeric.nsecbunker.core.model.KeyUser;
import xyz.tcheeric.nsecbunker.core.model.PolicyRule;
import xyz.tcheeric.nsecbunker.e2e.E2ETestBase;
import nostr.id.Identity;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * E2E tests for signing flow setup and verification.
 *
 * <p>Tests the complete setup required for signing: key creation, policy assignment,
 * permission granting, and token generation. Verifies that the bunker responds with
 * the expected npub and connection details.
 *
 * <p>Note: Full remote signing requires a client-side transport implementation
 * that connects to relays. These tests verify the admin-side setup is correct.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Signing Flow E2E Tests")
class SigningFlowE2ETest extends E2ETestBase {

    private NsecBunkerAdminClient adminClient;
    private KeyManager keyManager;
    private PolicyManager policyManager;
    private PermissionManager permissionManager;
    private TokenManager tokenManager;
    private static final Duration E2E_TIMEOUT = Duration.ofSeconds(120);
    private static final Duration E2E_TIMEOUT = Duration.ofSeconds(90);

    // Test fixtures
    private String testKeyName;
    private BunkerKey testKey;
    private BunkerPolicy signingPolicy;
    private Identity testUser;

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
                .untilAsserted(() -> assertThat(adminClient.ping().get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS))
                        .isEqualTo("pong"));
        await().atMost(E2E_TIMEOUT)
                .untilAsserted(() -> assertThat(adminClient.ping().get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS)).isEqualTo("pong"));

        keyManager = adminClient.keyManager();
        policyManager = adminClient.policyManager();
        permissionManager = adminClient.permissionManager();
        tokenManager = adminClient.tokenManager();

        // Generate test user identity
        testUser = generateTestIdentity();
    }

    @AfterEach
    void tearDown() throws Exception {
        // Clean up in reverse order of creation
        if (signingPolicy != null && signingPolicy.getId() != null) {
            try {
                policyManager.deletePolicy(signingPolicy.getId()).get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("Failed to delete signing policy: {}", e.getMessage());
            }
        }

        if (testKeyName != null) {
            try {
                keyManager.deleteKey(testKeyName).get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("Failed to delete test key: {}", e.getMessage());
            }
        }

        if (adminClient != null) {
            adminClient.disconnect();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Should set up key for signing")
    void shouldSetUpKeyForSigning() throws Exception {
        // Create a key for signing
        testKeyName = "signing-key-" + UUID.randomUUID().toString().substring(0, 8);
        String passphrase = "signing-passphrase";

        testKey = keyManager.createKey(testKeyName, passphrase)
                .get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);

        assertThat(testKey).isNotNull();
        assertThat(testKey.getNpub()).isNotNull().startsWith("npub1");

        log.info("Created signing key: name={}, npub={}", testKeyName, testKey.getNpub());

        // Unlock the key for signing operations
        Boolean unlocked = keyManager.unlockKey(testKeyName, passphrase)
                .get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);
        assertThat(unlocked).isTrue();

        // Verify key is ready for signing
        BunkerKey keyDetails = keyManager.getKeyDetails(testKeyName)
                .get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);
        assertThat(keyDetails.isLocked()).isFalse();

        log.info("Key is unlocked and ready for signing");
    }

    @Test
    @Order(2)
    @DisplayName("Should create signing policy")
    void shouldCreateSigningPolicy() throws Exception {
        testKeyName = "policy-test-key-" + UUID.randomUUID().toString().substring(0, 8);
        testKey = keyManager.createKey(testKeyName, "test-passphrase")
                .get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);

        // Create a policy that allows signing events
        signingPolicy = policyManager.createPolicy(
                BunkerPolicy.builder()
                        .name("signing-policy-" + UUID.randomUUID().toString().substring(0, 8))
                        .description("Policy for signing text notes and reactions")
                        .rule(PolicyRule.allowMethod("sign_event"))
                        .rule(PolicyRule.allowMethod("get_public_key"))
                        .rule(PolicyRule.allowEventKind(1))  // Text notes
                        .rule(PolicyRule.allowEventKind(7))  // Reactions
                        .rule(PolicyRule.denyEventKind(4))   // Deny DMs
                        .build()
        ).get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);

        assertThat(signingPolicy).isNotNull();
        assertThat(signingPolicy.getId()).isNotBlank();
        // Note: Rule evaluation is tested in unit tests. E2E tests verify server interaction.
        // nsecbunkerd doesn't return rules in list response, so we can't verify them here.
        assertThat(signingPolicy.getName()).startsWith("signing-policy-");

        log.info("Created signing policy: id={}, name={}",
                signingPolicy.getId(), signingPolicy.getName());
    }

    @Test
    @Order(3)
    @DisplayName("Should grant signing permission to user")
    void shouldGrantSigningPermission() throws Exception {
        // Set up key and policy
        testKeyName = "grant-test-key-" + UUID.randomUUID().toString().substring(0, 8);
        testKey = keyManager.createKey(testKeyName, "test-passphrase")
                .get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);

        signingPolicy = policyManager.createPolicy(
                BunkerPolicy.builder()
                        .name("grant-policy-" + UUID.randomUUID().toString().substring(0, 8))
                        .rule(PolicyRule.allowMethod("sign_event"))
                        .build()
        ).get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);

        String userPubkey = testUser.getPublicKey().toString();

        // Grant signing permission
        KeyUser keyUser = permissionManager.grantPermission(
                testKeyName, userPubkey, signingPolicy)
                .get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);

        assertThat(keyUser).isNotNull();
        assertThat(keyUser.getPublicKey()).isEqualTo(userPubkey);
        assertThat(keyUser.isActive()).isTrue();

        log.info("Granted signing permission: user={}, key={}", userPubkey, testKeyName);

        // Clean up permission
        permissionManager.revokePermission(testKeyName, userPubkey)
                .get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);
    }

    @Test
    @Order(4)
    @DisplayName("Should generate signing token with connection string")
    void shouldGenerateSigningToken() throws Exception {
        // Set up key and policy
        testKeyName = "token-test-key-" + UUID.randomUUID().toString().substring(0, 8);
        testKey = keyManager.createKey(testKeyName, "test-passphrase")
                .get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);

        signingPolicy = policyManager.createPolicy(
                BunkerPolicy.builder()
                        .name("token-policy-" + UUID.randomUUID().toString().substring(0, 8))
                        .rule(PolicyRule.allowMethod("sign_event"))
                        .rule(PolicyRule.allowMethod("get_public_key"))
                        .build()
        ).get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);

        // Create a signing token
        String clientName = "signing-client";
        AccessToken token = tokenManager.createToken(
                testKeyName, clientName, signingPolicy.getId(), Duration.ofHours(24))
                .get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);

        assertThat(token).isNotNull();
        assertThat(token.getToken()).isNotBlank();
        assertThat(token.getKeyNpub()).isEqualTo(testKey.getNpub());
        assertThat(token.isValid()).isTrue();

        // Verify connection string can be generated
        String connectionString = token.getConnectionString();
        assertThat(connectionString).isNotNull();
        assertThat(connectionString).startsWith("bunker://");
        assertThat(connectionString).contains(testKey.getNpub());

        log.info("Generated signing token: id={}, connectionString={}...",
                token.getId(), connectionString.substring(0, Math.min(50, connectionString.length())));

        // Clean up
        tokenManager.revokeToken(token.getId()).get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);
    }

    @Test
    @Order(5)
    @DisplayName("Should complete full signing setup flow")
    void shouldCompleteFullSigningSetupFlow() throws Exception {
        // 1. Create key
        log.info("Step 1: Creating signing key");
        testKeyName = "full-flow-key-" + UUID.randomUUID().toString().substring(0, 8);
        String passphrase = "full-flow-passphrase";
        testKey = keyManager.createKey(testKeyName, passphrase)
                .get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);
        assertThat(testKey.getNpub()).startsWith("npub1");
        log.info("Key created: npub={}", testKey.getNpub());

        // 2. Unlock key
        log.info("Step 2: Unlocking key");
        keyManager.unlockKey(testKeyName, passphrase).get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);
        log.info("Key unlocked");

        // 3. Create signing policy
        log.info("Step 3: Creating signing policy");
        signingPolicy = policyManager.createPolicy(
                BunkerPolicy.builder()
                        .name("full-flow-policy-" + UUID.randomUUID().toString().substring(0, 8))
                        .description("Full signing flow policy")
                        .rule(PolicyRule.allowMethod("sign_event"))
                        .rule(PolicyRule.allowMethod("get_public_key"))
                        .rule(PolicyRule.allowMethod("ping"))
                        .rule(PolicyRule.allowEventKind(1))
                        .build()
        ).get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);
        log.info("Policy created: id={}", signingPolicy.getId());

        // 4. Grant permission to test user
        log.info("Step 4: Granting permission");
        String userPubkey = testUser.getPublicKey().toString();
        KeyUser keyUser = permissionManager.grantPermission(
                testKeyName, userPubkey, signingPolicy)
                .get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);
        assertThat(keyUser.isActive()).isTrue();
        log.info("Permission granted to user: {}", testUser.getPublicKey().toBech32String());

        // 5. Create token for the user
        log.info("Step 5: Creating token");
        AccessToken token = tokenManager.createToken(
                testKeyName, "full-flow-client", signingPolicy.getId(), Duration.ofDays(7))
                .get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);
        assertThat(token.isValid()).isTrue();
        log.info("Token created: id={}", token.getId());

        // 6. Verify the complete setup
        log.info("Step 6: Verifying setup");

        // Verify key exists and is unlocked
        BunkerKey keyDetails = keyManager.getKeyDetails(testKeyName).get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);
        assertThat(keyDetails.isLocked()).isFalse();

        // Verify policy allows signing
        BunkerPolicy policyDetails = policyManager.getPolicy(signingPolicy.getId())
                .get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);
        assertThat(policyDetails.isMethodAllowed("sign_event")).isTrue();

        // Verify user has permission
        KeyUser userPermissions = permissionManager.getPermissions(testKeyName, userPubkey)
                .get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);
        assertThat(userPermissions.isActive()).isTrue();

        // Verify token is valid
        Boolean tokenValid = tokenManager.validateToken(token.getToken())
                .get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);
        assertThat(tokenValid).isTrue();

        // Verify connection string is well-formed
        String connectionString = token.getConnectionString();
        assertThat(connectionString).startsWith("bunker://npub1");

        log.info("Full signing setup flow completed successfully");
        log.info("Connection string for remote signing: {}", connectionString);

        // Clean up
        tokenManager.revokeToken(token.getId()).get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);
        permissionManager.revokePermission(testKeyName, userPubkey).get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);
    }

    @Test
    @Order(6)
    @DisplayName("Should import existing key for signing")
    void shouldImportExistingKeyForSigning() throws Exception {
        // Generate a specific identity to import
        Identity signingIdentity = generateTestIdentity();
        String nsec = signingIdentity.getPrivateKey().toBech32String();
        String expectedNpub = signingIdentity.getPublicKey().toBech32String();

        // Import the key
        testKeyName = "imported-signing-key-" + UUID.randomUUID().toString().substring(0, 8);
        String passphrase = "import-passphrase";

        testKey = keyManager.createKey(testKeyName, nsec, passphrase)
                .get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);

        // Verify the npub matches
        assertThat(testKey.getNpub()).isEqualTo(expectedNpub);

        // Unlock and verify ready for signing
        keyManager.unlockKey(testKeyName, passphrase).get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);

        BunkerKey keyDetails = keyManager.getKeyDetails(testKeyName)
                .get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);
        assertThat(keyDetails.isLocked()).isFalse();
        assertThat(keyDetails.getNpub()).isEqualTo(expectedNpub);

        log.info("Imported and unlocked key for signing: npub={}", expectedNpub);
    }
}
