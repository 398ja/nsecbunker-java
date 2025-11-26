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
import xyz.tcheeric.nsecbunker.core.model.BunkerKey;
import xyz.tcheeric.nsecbunker.e2e.E2ETestBase;
import nostr.id.Identity;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * E2E tests for key CRUD operations.
 *
 * <p>Tests: create/import key, list, unlock with passphrase, get details, delete;
 * verify bunker responds with npub.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Key CRUD Flow E2E Tests")
class KeyCrudFlowE2ETest extends E2ETestBase {

    private NsecBunkerAdminClient adminClient;
    private KeyManager keyManager;

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
    }

    @AfterEach
    void tearDown() {
        if (adminClient != null) {
            adminClient.disconnect();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Should create a new key with passphrase")
    void shouldCreateNewKeyWithPassphrase() throws Exception {
        String keyName = "test-key-" + UUID.randomUUID().toString().substring(0, 8);
        String passphrase = "test-passphrase-123";

        // Create a new key (generates new keypair)
        BunkerKey createdKey = keyManager.createKey(keyName, passphrase)
                .get(30, TimeUnit.SECONDS);

        // Verify key was created
        assertThat(createdKey).isNotNull();
        assertThat(createdKey.getName()).isEqualTo(keyName);
        assertThat(createdKey.getNpub()).isNotNull().startsWith("npub1");

        log.info("Created new key: name={}, npub={}", createdKey.getName(), createdKey.getNpub());

        // Clean up - delete the key
        keyManager.deleteKey(keyName).get(30, TimeUnit.SECONDS);
    }

    @Test
    @Order(2)
    @DisplayName("Should import an existing key")
    void shouldImportExistingKey() throws Exception {
        String keyName = "imported-key-" + UUID.randomUUID().toString().substring(0, 8);
        String passphrase = "import-passphrase-456";

        // Generate a test identity to import
        Identity testIdentity = generateTestIdentity();
        String nsecToImport = testIdentity.getPrivateKey().toBech32String();
        String expectedNpub = testIdentity.getPublicKey().toBech32String();

        // Import the key
        BunkerKey importedKey = keyManager.createKey(keyName, nsecToImport, passphrase)
                .get(30, TimeUnit.SECONDS);

        // Verify key was imported with correct pubkey
        assertThat(importedKey).isNotNull();
        assertThat(importedKey.getName()).isEqualTo(keyName);
        assertThat(importedKey.getNpub()).isEqualTo(expectedNpub);

        log.info("Imported key: name={}, npub={}", importedKey.getName(), importedKey.getNpub());

        // Clean up
        keyManager.deleteKey(keyName).get(30, TimeUnit.SECONDS);
    }

    @Test
    @Order(3)
    @DisplayName("Should list all keys")
    void shouldListAllKeys() throws Exception {
        // Create a few test keys
        String keyName1 = "list-test-key-1-" + UUID.randomUUID().toString().substring(0, 8);
        String keyName2 = "list-test-key-2-" + UUID.randomUUID().toString().substring(0, 8);
        String passphrase = "list-test-passphrase";

        keyManager.createKey(keyName1, passphrase).get(30, TimeUnit.SECONDS);
        keyManager.createKey(keyName2, passphrase).get(30, TimeUnit.SECONDS);

        // List all keys
        List<BunkerKey> keys = keyManager.listKeys().get(30, TimeUnit.SECONDS);

        // Verify the created keys are in the list
        assertThat(keys).isNotNull();
        assertThat(keys.stream().map(BunkerKey::getName))
                .contains(keyName1, keyName2);

        log.info("Listed {} keys", keys.size());

        // Clean up
        keyManager.deleteKey(keyName1).get(30, TimeUnit.SECONDS);
        keyManager.deleteKey(keyName2).get(30, TimeUnit.SECONDS);
    }

    @Test
    @Order(4)
    @DisplayName("Should unlock key with passphrase")
    void shouldUnlockKeyWithPassphrase() throws Exception {
        String keyName = "unlock-test-key-" + UUID.randomUUID().toString().substring(0, 8);
        String passphrase = "unlock-test-passphrase";

        // Create a key
        keyManager.createKey(keyName, passphrase).get(30, TimeUnit.SECONDS);

        // Unlock the key
        Boolean unlocked = keyManager.unlockKey(keyName, passphrase)
                .get(30, TimeUnit.SECONDS);

        assertThat(unlocked).isTrue();

        log.info("Successfully unlocked key: {}", keyName);

        // Get key details to verify it's unlocked
        BunkerKey keyDetails = keyManager.getKeyDetails(keyName)
                .get(30, TimeUnit.SECONDS);

        assertThat(keyDetails).isNotNull();
        assertThat(keyDetails.isLocked()).isFalse();

        // Clean up
        keyManager.deleteKey(keyName).get(30, TimeUnit.SECONDS);
    }

    @Test
    @Order(5)
    @DisplayName("Should get key details")
    void shouldGetKeyDetails() throws Exception {
        String keyName = "details-test-key-" + UUID.randomUUID().toString().substring(0, 8);
        String passphrase = "details-test-passphrase";

        // Create a key
        BunkerKey created = keyManager.createKey(keyName, passphrase)
                .get(30, TimeUnit.SECONDS);

        // Get key details
        BunkerKey details = keyManager.getKeyDetails(keyName)
                .get(30, TimeUnit.SECONDS);

        // Verify details
        assertThat(details).isNotNull();
        assertThat(details.getName()).isEqualTo(keyName);
        assertThat(details.getNpub()).isEqualTo(created.getNpub());
        assertThat(details.getNpub()).startsWith("npub1");

        log.info("Retrieved key details: name={}, npub={}, locked={}",
                details.getName(), details.getNpub(), details.isLocked());

        // Clean up
        keyManager.deleteKey(keyName).get(30, TimeUnit.SECONDS);
    }

    @Test
    @Order(6)
    @DisplayName("Should delete key")
    void shouldDeleteKey() throws Exception {
        String keyName = "delete-test-key-" + UUID.randomUUID().toString().substring(0, 8);
        String passphrase = "delete-test-passphrase";

        // Create a key
        keyManager.createKey(keyName, passphrase).get(30, TimeUnit.SECONDS);

        // Verify key exists
        List<BunkerKey> keysBefore = keyManager.listKeys().get(30, TimeUnit.SECONDS);
        assertThat(keysBefore.stream().map(BunkerKey::getName)).contains(keyName);

        // Delete the key
        Boolean deleted = keyManager.deleteKey(keyName).get(30, TimeUnit.SECONDS);

        assertThat(deleted).isTrue();
        log.info("Deleted key: {}", keyName);

        // Verify key no longer exists
        List<BunkerKey> keysAfter = keyManager.listKeys().get(30, TimeUnit.SECONDS);
        assertThat(keysAfter.stream().map(BunkerKey::getName)).doesNotContain(keyName);
    }

    @Test
    @Order(7)
    @DisplayName("Should complete full key lifecycle")
    void shouldCompleteFullKeyLifecycle() throws Exception {
        String keyName = "lifecycle-key-" + UUID.randomUUID().toString().substring(0, 8);
        String passphrase = "lifecycle-passphrase";

        // 1. Create key
        log.info("Step 1: Creating key");
        BunkerKey created = keyManager.createKey(keyName, passphrase)
                .get(30, TimeUnit.SECONDS);
        assertThat(created).isNotNull();
        assertThat(created.getNpub()).startsWith("npub1");
        log.info("Created key with npub: {}", created.getNpub());

        // 2. List keys and verify
        log.info("Step 2: Listing keys");
        List<BunkerKey> keys = keyManager.listKeys().get(30, TimeUnit.SECONDS);
        assertThat(keys.stream().map(BunkerKey::getName)).contains(keyName);
        log.info("Key found in list");

        // 3. Get details
        log.info("Step 3: Getting key details");
        BunkerKey details = keyManager.getKeyDetails(keyName).get(30, TimeUnit.SECONDS);
        assertThat(details.getNpub()).isEqualTo(created.getNpub());
        log.info("Key details retrieved successfully");

        // 4. Unlock key
        log.info("Step 4: Unlocking key");
        Boolean unlocked = keyManager.unlockKey(keyName, passphrase)
                .get(30, TimeUnit.SECONDS);
        assertThat(unlocked).isTrue();
        log.info("Key unlocked successfully");

        // 5. Delete key
        log.info("Step 5: Deleting key");
        Boolean deleted = keyManager.deleteKey(keyName).get(30, TimeUnit.SECONDS);
        assertThat(deleted).isTrue();
        log.info("Key deleted successfully");

        // 6. Verify deletion
        log.info("Step 6: Verifying deletion");
        List<BunkerKey> keysAfter = keyManager.listKeys().get(30, TimeUnit.SECONDS);
        assertThat(keysAfter.stream().map(BunkerKey::getName)).doesNotContain(keyName);
        log.info("Key lifecycle completed successfully");
    }
}
