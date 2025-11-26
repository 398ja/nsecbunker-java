package xyz.tcheeric.nsecbunker.e2e.tests;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import java.time.Duration;
import xyz.tcheeric.nsecbunker.admin.NsecBunkerAdminClient;
import xyz.tcheeric.nsecbunker.admin.key.KeyManager;
import xyz.tcheeric.nsecbunker.admin.permission.PermissionManager;
import xyz.tcheeric.nsecbunker.admin.policy.PolicyManager;
import xyz.tcheeric.nsecbunker.core.model.BunkerKey;
import xyz.tcheeric.nsecbunker.core.model.BunkerPolicy;
import xyz.tcheeric.nsecbunker.core.model.KeyUser;
import xyz.tcheeric.nsecbunker.core.model.PolicyRule;
import xyz.tcheeric.nsecbunker.e2e.E2ETestBase;
import nostr.id.Identity;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * E2E tests for permission management operations.
 *
 * <p>Tests: grant permission, revoke permission, list key users, get permissions.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Permission Flow E2E Tests")
class PermissionFlowE2ETest extends E2ETestBase {

    private NsecBunkerAdminClient adminClient;
    private KeyManager keyManager;
    private PolicyManager policyManager;
    private PermissionManager permissionManager;
    private static final Duration E2E_TIMEOUT = Duration.ofSeconds(90);

    // Test fixtures
    private String testKeyName;
    private BunkerKey testKey;
    private BunkerPolicy testPolicy;
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
                .untilAsserted(() -> assertThat(adminClient.ping().get(10, TimeUnit.SECONDS)).isEqualTo("pong"));

        keyManager = adminClient.keyManager();
        policyManager = adminClient.policyManager();
        permissionManager = adminClient.permissionManager();

        // Create test fixtures
        testKeyName = "perm-test-key-" + UUID.randomUUID().toString().substring(0, 8);
        testKey = keyManager.createKey(testKeyName, "test-passphrase")
                .get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);

        testPolicy = policyManager.createPolicy(
                BunkerPolicy.builder()
                        .name("perm-test-policy-" + UUID.randomUUID().toString().substring(0, 8))
                        .rule(PolicyRule.allowMethod("sign_event"))
                        .rule(PolicyRule.allowMethod("get_public_key"))
                        .build()
        ).get(E2E_TIMEOUT.getSeconds(), TimeUnit.SECONDS);

        testUser = generateTestIdentity();

        log.info("Test fixtures created: key={}, policy={}, user={}",
                testKeyName, testPolicy.getId(), testUser.getPublicKey().toBech32String());
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
    @DisplayName("Should grant permission to a user with a policy")
    void shouldGrantPermission() throws Exception {
        String userPubkey = testUser.getPublicKey().toString();

        // Grant permission
        KeyUser keyUser = permissionManager.grantPermission(testKeyName, userPubkey, testPolicy)
                .get(30, TimeUnit.SECONDS);

        assertThat(keyUser).isNotNull();
        assertThat(keyUser.getPublicKey()).isEqualTo(userPubkey);
        assertThat(keyUser.getKeyName()).isEqualTo(testKeyName);
        assertThat(keyUser.isActive()).isTrue();

        log.info("Granted permission to user: pubkey={}", userPubkey);

        // Clean up
        permissionManager.revokePermission(testKeyName, userPubkey)
                .get(30, TimeUnit.SECONDS);
    }

    @Test
    @Order(2)
    @DisplayName("Should revoke permission from a user")
    void shouldRevokePermission() throws Exception {
        String userPubkey = testUser.getPublicKey().toString();

        // Grant permission first
        permissionManager.grantPermission(testKeyName, userPubkey, testPolicy)
                .get(30, TimeUnit.SECONDS);

        // Verify user is in list
        List<KeyUser> usersBefore = permissionManager.listKeyUsers(testKeyName)
                .get(30, TimeUnit.SECONDS);
        assertThat(usersBefore.stream().map(KeyUser::getPublicKey)).contains(userPubkey);

        // Revoke permission
        Boolean revoked = permissionManager.revokePermission(testKeyName, userPubkey)
                .get(30, TimeUnit.SECONDS);

        assertThat(revoked).isTrue();
        log.info("Revoked permission from user: pubkey={}", userPubkey);

        // Verify user is no longer in list
        List<KeyUser> usersAfter = permissionManager.listKeyUsers(testKeyName)
                .get(30, TimeUnit.SECONDS);
        assertThat(usersAfter.stream().map(KeyUser::getPublicKey)).doesNotContain(userPubkey);
    }

    @Test
    @Order(3)
    @DisplayName("Should list all users for a key")
    void shouldListKeyUsers() throws Exception {
        Identity user1 = generateTestIdentity();
        Identity user2 = generateTestIdentity();
        String pubkey1 = user1.getPublicKey().toString();
        String pubkey2 = user2.getPublicKey().toString();

        // Grant permission to multiple users
        permissionManager.grantPermission(testKeyName, pubkey1, testPolicy)
                .get(30, TimeUnit.SECONDS);
        permissionManager.grantPermission(testKeyName, pubkey2, testPolicy)
                .get(30, TimeUnit.SECONDS);

        // List key users
        List<KeyUser> users = permissionManager.listKeyUsers(testKeyName)
                .get(30, TimeUnit.SECONDS);

        assertThat(users).isNotNull();
        assertThat(users.stream().map(KeyUser::getPublicKey))
                .contains(pubkey1, pubkey2);

        log.info("Listed {} users for key {}", users.size(), testKeyName);

        // Clean up
        permissionManager.revokePermission(testKeyName, pubkey1).get(30, TimeUnit.SECONDS);
        permissionManager.revokePermission(testKeyName, pubkey2).get(30, TimeUnit.SECONDS);
    }

    @Test
    @Order(4)
    @DisplayName("Should get permissions for a specific user")
    void shouldGetPermissions() throws Exception {
        String userPubkey = testUser.getPublicKey().toString();

        // Grant permission
        permissionManager.grantPermission(testKeyName, userPubkey, testPolicy)
                .get(30, TimeUnit.SECONDS);

        // Get permissions
        KeyUser permissions = permissionManager.getPermissions(testKeyName, userPubkey)
                .get(30, TimeUnit.SECONDS);

        assertThat(permissions).isNotNull();
        assertThat(permissions.getPublicKey()).isEqualTo(userPubkey);
        assertThat(permissions.getKeyName()).isEqualTo(testKeyName);

        log.info("Retrieved permissions for user: pubkey={}, hasPolicy={}",
                userPubkey, permissions.hasPolicy());

        // Clean up
        permissionManager.revokePermission(testKeyName, userPubkey)
                .get(30, TimeUnit.SECONDS);
    }

    @Test
    @Order(5)
    @DisplayName("Should update key user description")
    void shouldUpdateKeyUserDescription() throws Exception {
        String userPubkey = testUser.getPublicKey().toString();
        String originalDescription = "Original description";
        String updatedDescription = "Updated description for user";

        // Grant permission
        permissionManager.grantPermission(testKeyName, userPubkey, testPolicy)
                .get(30, TimeUnit.SECONDS);

        // Update description
        KeyUser updated = permissionManager.updateKeyUserDescription(
                testKeyName, userPubkey, updatedDescription)
                .get(30, TimeUnit.SECONDS);

        assertThat(updated).isNotNull();
        assertThat(updated.getDescription()).isEqualTo(updatedDescription);

        log.info("Updated user description: pubkey={}, description={}",
                userPubkey, updatedDescription);

        // Clean up
        permissionManager.revokePermission(testKeyName, userPubkey)
                .get(30, TimeUnit.SECONDS);
    }

    @Test
    @Order(6)
    @DisplayName("Should complete full permission lifecycle")
    void shouldCompleteFullPermissionLifecycle() throws Exception {
        Identity lifecycleUser = generateTestIdentity();
        String userPubkey = lifecycleUser.getPublicKey().toString();

        // 1. Grant permission
        log.info("Step 1: Granting permission");
        KeyUser granted = permissionManager.grantPermission(testKeyName, userPubkey, testPolicy)
                .get(30, TimeUnit.SECONDS);
        assertThat(granted).isNotNull();
        log.info("Permission granted");

        // 2. List users and verify
        log.info("Step 2: Listing key users");
        List<KeyUser> users = permissionManager.listKeyUsers(testKeyName)
                .get(30, TimeUnit.SECONDS);
        assertThat(users.stream().map(KeyUser::getPublicKey)).contains(userPubkey);
        log.info("User found in list");

        // 3. Get permissions
        log.info("Step 3: Getting permissions");
        KeyUser permissions = permissionManager.getPermissions(testKeyName, userPubkey)
                .get(30, TimeUnit.SECONDS);
        assertThat(permissions.getPublicKey()).isEqualTo(userPubkey);
        log.info("Permissions retrieved");

        // 4. Update description
        log.info("Step 4: Updating description");
        String newDescription = "Lifecycle test user";
        KeyUser updated = permissionManager.updateKeyUserDescription(
                testKeyName, userPubkey, newDescription)
                .get(30, TimeUnit.SECONDS);
        assertThat(updated.getDescription()).isEqualTo(newDescription);
        log.info("Description updated");

        // 5. Revoke permission
        log.info("Step 5: Revoking permission");
        Boolean revoked = permissionManager.revokePermission(testKeyName, userPubkey)
                .get(30, TimeUnit.SECONDS);
        assertThat(revoked).isTrue();
        log.info("Permission revoked");

        // 6. Verify revocation
        log.info("Step 6: Verifying revocation");
        List<KeyUser> usersAfter = permissionManager.listKeyUsers(testKeyName)
                .get(30, TimeUnit.SECONDS);
        assertThat(usersAfter.stream().map(KeyUser::getPublicKey)).doesNotContain(userPubkey);
        log.info("Permission lifecycle completed successfully");
    }
}
