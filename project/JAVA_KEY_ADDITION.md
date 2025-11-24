# Adding nsec Keys to nsecBunker from Java (cashu-client)

## Overview

Yes! The cashu-client identity plugin can add nsec keys to nsecBunker programmatically via the **Admin RPC Interface** using NIP-46.

## How It Works

nsecBunker exposes an admin RPC method called `create_new_key` that:
- Accepts an nsec (or generates a new one)
- Encrypts it with a passphrase
- Stores it in the bunker
- Returns the npub

**Protocol**: NIP-46 RPC over Nostr relays (kind 24134)

## Java Implementation

### 1. NsecBunker Admin Client

```java
// NsecBunkerAdminClient.java
package xyz.tcheeric.identity.infrastructure.bunker;

import nostr.base.PrivateKey;
import nostr.base.PublicKey;
import nostr.crypto.bech32.Bech32;
import nostr.event.impl.GenericEvent;
import nostr.event.Kind;
import nostr.crypto.nip04.Nip04;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Admin client for nsecBunker.
 *
 * Connects to nsecBunker's admin interface and can:
 * - Add new keys
 * - Create policies
 * - Grant permissions
 * - List keys
 */
public class NsecBunkerAdminClient {

    private final PrivateKey adminPrivkey;
    private final PublicKey bunkerPubkey;
    private final NostrRelayPool relayPool;
    private final ConcurrentHashMap<String, CompletableFuture<?>> pendingRequests;

    /**
     * Creates an admin client.
     *
     * @param adminNsec Your admin nsec (must be in bunker's ADMIN_NPUBS)
     * @param bunkerConnectionString Bunker connection string (bunker://npub@relay)
     * @param relays Admin relays to use
     */
    public NsecBunkerAdminClient(
        String adminNsec,
        String bunkerConnectionString,
        String[] relays
    ) {
        // Decode admin nsec
        Bech32.Bech32Data decoded = Bech32.decode(adminNsec);
        this.adminPrivkey = new PrivateKey(decoded.data);

        // Parse bunker connection string
        BunkerConnection conn = BunkerConnection.parse(bunkerConnectionString);
        this.bunkerPubkey = conn.getBunkerPubkey();

        // Initialize relay pool
        this.relayPool = new NostrRelayPool(relays);
        this.pendingRequests = new ConcurrentHashMap<>();
    }

    /**
     * Connects to the bunker's admin interface.
     */
    public void connect() throws Exception {
        relayPool.connect();

        // Subscribe to admin responses (kind 24134)
        relayPool.subscribe(
            new SubscriptionFilter()
                .kinds(24134)
                .authors(bunkerPubkey.toString())
                .since(System.currentTimeMillis() / 1000),
            this::handleAdminResponse
        );

        // Wait for connection
        Thread.sleep(1000);
    }

    /**
     * Adds a new key to nsecBunker.
     *
     * @param keyName Name for the key (e.g., "cashu-alice-wallet")
     * @param nsec The nsec to add (bech32 encoded)
     * @param passphrase Passphrase to encrypt the key
     * @return CompletableFuture with the npub of the added key
     */
    public CompletableFuture<String> createNewKey(
        String keyName,
        String nsec,
        String passphrase
    ) {
        CompletableFuture<String> future = new CompletableFuture<>();

        String requestId = UUID.randomUUID().toString();

        // Create RPC request payload
        // Format: ["req", <id>, "create_new_key", <keyName>, <passphrase>, <nsec>]
        String payload = String.format(
            "[\"req\",\"%s\",\"create_new_key\",\"%s\",\"%s\",\"%s\"]",
            requestId,
            keyName,
            passphrase,
            nsec
        );

        // Encrypt with NIP-04
        String encrypted = Nip04.encrypt(
            adminPrivkey,
            bunkerPubkey,
            payload
        );

        // Create kind 24134 event
        GenericEvent event = new GenericEvent();
        event.setKind(24134);
        event.setPubKey(adminPrivkey.getPublicKey());
        event.setContent(encrypted);
        event.addTag("p", bunkerPubkey.toString());

        // Sign and publish
        event.sign(adminPrivkey);
        relayPool.publish(event);

        // Store pending request
        pendingRequests.put(requestId, future);

        // Timeout after 30 seconds
        CompletableFuture.delayedExecutor(30, TimeUnit.SECONDS).execute(() -> {
            if (!future.isDone()) {
                future.completeExceptionally(
                    new TimeoutException("Admin request timed out")
                );
            }
        });

        return future;
    }

    /**
     * Creates a new key by generating it in the bunker.
     *
     * @param keyName Name for the key
     * @param passphrase Passphrase to encrypt the key
     * @return CompletableFuture with the npub of the generated key
     */
    public CompletableFuture<String> createNewKey(
        String keyName,
        String passphrase
    ) {
        CompletableFuture<String> future = new CompletableFuture<>();

        String requestId = UUID.randomUUID().toString();

        // Request without nsec - bunker will generate one
        String payload = String.format(
            "[\"req\",\"%s\",\"create_new_key\",\"%s\",\"%s\"]",
            requestId,
            keyName,
            passphrase
        );

        String encrypted = Nip04.encrypt(adminPrivkey, bunkerPubkey, payload);

        GenericEvent event = new GenericEvent();
        event.setKind(24134);
        event.setPubKey(adminPrivkey.getPublicKey());
        event.setContent(encrypted);
        event.addTag("p", bunkerPubkey.toString());

        event.sign(adminPrivkey);
        relayPool.publish(event);

        pendingRequests.put(requestId, future);

        return future;
    }

    /**
     * Lists all keys in the bunker.
     */
    public CompletableFuture<List<BunkerKey>> getKeys() {
        CompletableFuture<List<BunkerKey>> future = new CompletableFuture<>();

        String requestId = UUID.randomUUID().toString();

        String payload = String.format(
            "[\"req\",\"%s\",\"get_keys\"]",
            requestId
        );

        String encrypted = Nip04.encrypt(adminPrivkey, bunkerPubkey, payload);

        GenericEvent event = new GenericEvent();
        event.setKind(24134);
        event.setPubKey(adminPrivkey.getPublicKey());
        event.setContent(encrypted);
        event.addTag("p", bunkerPubkey.toString());

        event.sign(adminPrivkey);
        relayPool.publish(event);

        pendingRequests.put(requestId, future);

        return future;
    }

    /**
     * Unlocks a key in the bunker.
     */
    public CompletableFuture<Boolean> unlockKey(
        String keyName,
        String passphrase
    ) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        String requestId = UUID.randomUUID().toString();

        String payload = String.format(
            "[\"req\",\"%s\",\"unlock_key\",\"%s\",\"%s\"]",
            requestId,
            keyName,
            passphrase
        );

        String encrypted = Nip04.encrypt(adminPrivkey, bunkerPubkey, payload);

        GenericEvent event = new GenericEvent();
        event.setKind(24134);
        event.setPubKey(adminPrivkey.getPublicKey());
        event.setContent(encrypted);
        event.addTag("p", bunkerPubkey.toString());

        event.sign(adminPrivkey);
        relayPool.publish(event);

        pendingRequests.put(requestId, future);

        return future;
    }

    private void handleAdminResponse(GenericEvent event) {
        try {
            // Decrypt NIP-04 content
            String decrypted = Nip04.decrypt(
                adminPrivkey,
                bunkerPubkey,
                event.getContent()
            );

            // Parse response: ["res", <id>, <result>]
            AdminResponse response = parseAdminResponse(decrypted);

            // Resolve pending request
            CompletableFuture future = pendingRequests.remove(response.getId());
            if (future != null) {
                if (response.isError()) {
                    future.completeExceptionally(
                        new BunkerException(response.getError())
                    );
                } else {
                    future.complete(response.getResult());
                }
            }

        } catch (Exception e) {
            System.err.println("Error handling admin response: " + e.getMessage());
        }
    }

    private AdminResponse parseAdminResponse(String json) {
        // Parse JSON: ["res", <id>, <result>]
        // or: ["res", <id>, "error", <message>]
        // Implementation depends on your JSON library
        // Example using org.json:
        JSONArray arr = new JSONArray(json);
        String type = arr.getString(0); // "res"
        String id = arr.getString(1);

        if (arr.length() > 3 && "error".equals(arr.getString(2))) {
            String error = arr.getString(3);
            return AdminResponse.error(id, error);
        } else {
            Object result = arr.get(2);
            return AdminResponse.success(id, result);
        }
    }

    public void disconnect() {
        relayPool.disconnect();
    }
}
```

### 2. Integration with Identity Plugin

```java
// IdentityToNsecBunkerSync.java
package xyz.tcheeric.identity.application.bunker;

import xyz.tcheeric.identity.domain.Identity;
import xyz.tcheeric.identity.domain.PrivateKey;
import xyz.tcheeric.identity.infrastructure.bunker.NsecBunkerAdminClient;
import nostr.crypto.bech32.Bech32;

/**
 * Synchronizes identities to nsecBunker.
 *
 * When a new identity is created in the identity plugin,
 * this service can push it to nsecBunker for remote storage.
 */
public class IdentityToNsecBunkerSync {

    private final NsecBunkerAdminClient bunkerClient;
    private final String defaultPassphrase;

    public IdentityToNsecBunkerSync(
        NsecBunkerAdminClient bunkerClient,
        String defaultPassphrase
    ) {
        this.bunkerClient = bunkerClient;
        this.defaultPassphrase = defaultPassphrase;
    }

    /**
     * Pushes an identity's private key to nsecBunker.
     *
     * @param identity The identity to sync
     * @return The npub returned by the bunker
     */
    public String syncIdentityToBunker(Identity identity) throws Exception {
        // Convert domain private key to nsec (bech32)
        PrivateKey privkey = identity.getPrivateKey();
        nostr.base.PrivateKey nostrPrivkey = privkey.toNostrJava();
        String nsec = encodeBech32("nsec", nostrPrivkey.getRawData());

        // Create key in bunker
        String keyName = "cashu-" + identity.getId();

        CompletableFuture<String> future = bunkerClient.createNewKey(
            keyName,
            nsec,
            defaultPassphrase
        );

        // Wait for result
        String npub = future.get(30, TimeUnit.SECONDS);

        System.out.println("✓ Identity " + identity.getId() + " synced to bunker");
        System.out.println("  Key name: " + keyName);
        System.out.println("  NPub: " + npub);

        return npub;
    }

    /**
     * Generates a new identity directly in the bunker.
     */
    public String createIdentityInBunker(String identityId) throws Exception {
        String keyName = "cashu-" + identityId;

        CompletableFuture<String> future = bunkerClient.createNewKey(
            keyName,
            defaultPassphrase
        );

        String npub = future.get(30, TimeUnit.SECONDS);

        System.out.println("✓ Identity created in bunker: " + identityId);
        System.out.println("  Key name: " + keyName);
        System.out.println("  NPub: " + npub);

        return npub;
    }

    private String encodeBech32(String hrp, byte[] data) {
        return Bech32.encode(hrp, data);
    }
}
```

### 3. Enhanced CreateIdentityUseCase

```java
// CreateIdentityUseCase.java (enhanced version)
package xyz.tcheeric.identity.application;

import xyz.tcheeric.identity.api.ports.IdentityRepository;
import xyz.tcheeric.identity.application.bunker.IdentityToNsecBunkerSync;
import xyz.tcheeric.identity.domain.Identity;
import xyz.tcheeric.identity.domain.PublicKey;
import xyz.tcheeric.identity.domain.PrivateKey;

import java.util.UUID;
import java.time.Instant;

/**
 * Enhanced use case that optionally syncs to nsecBunker.
 */
public class CreateIdentityUseCase {

    private final IdentityRepository repository;
    private final IdentityToNsecBunkerSync bunkerSync; // Optional
    private final boolean syncToBunker;

    public CreateIdentityUseCase(
        IdentityRepository repository,
        IdentityToNsecBunkerSync bunkerSync,
        boolean syncToBunker
    ) {
        this.repository = repository;
        this.bunkerSync = bunkerSync;
        this.syncToBunker = syncToBunker;
    }

    /**
     * Creates a new identity.
     * If syncToBunker is enabled, pushes the key to nsecBunker.
     */
    public Identity execute(
        String label,
        PublicKey publicKey,
        PrivateKey privateKey
    ) {
        String identityId = UUID.randomUUID().toString();

        // Create identity domain object
        Identity identity = new Identity(
            identityId,
            label,
            publicKey,
            privateKey,
            Instant.now(),
            null
        );

        // Save to local repository
        repository.save(identity);

        // Optionally sync to bunker
        if (syncToBunker && bunkerSync != null) {
            try {
                String npub = bunkerSync.syncIdentityToBunker(identity);
                System.out.println("Identity synced to bunker: " + npub);
            } catch (Exception e) {
                System.err.println("Failed to sync to bunker: " + e.getMessage());
                // Continue - identity is still saved locally
            }
        }

        return identity;
    }

    /**
     * Creates a new identity directly in the bunker.
     * The private key never exists locally.
     */
    public Identity executeRemote(String label) throws Exception {
        if (!syncToBunker || bunkerSync == null) {
            throw new IllegalStateException("Bunker sync not enabled");
        }

        String identityId = UUID.randomUUID().toString();

        // Create key in bunker
        String npub = bunkerSync.createIdentityInBunker(identityId);

        // Parse npub to get public key
        PublicKey publicKey = PublicKey.fromBech32(npub);

        // Create identity without private key
        Identity identity = new Identity(
            identityId,
            label,
            publicKey,
            null, // No private key locally!
            Instant.now(),
            null
        );

        // Save to local repository
        repository.save(identity);

        return identity;
    }
}
```

### 4. Configuration

```java
// BunkerConfiguration.java
package xyz.tcheeric.identity.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import xyz.tcheeric.identity.infrastructure.bunker.NsecBunkerAdminClient;
import xyz.tcheeric.identity.application.bunker.IdentityToNsecBunkerSync;

@Configuration
public class BunkerConfiguration {

    @Value("${nsecbunker.admin.nsec}")
    private String adminNsec;

    @Value("${nsecbunker.connection.string}")
    private String bunkerConnectionString;

    @Value("${nsecbunker.admin.relays}")
    private String[] adminRelays;

    @Value("${nsecbunker.passphrase}")
    private String bunkerPassphrase;

    @Value("${nsecbunker.sync.enabled:false}")
    private boolean syncEnabled;

    @Bean
    public NsecBunkerAdminClient bunkerAdminClient() {
        if (!syncEnabled) {
            return null;
        }

        NsecBunkerAdminClient client = new NsecBunkerAdminClient(
            adminNsec,
            bunkerConnectionString,
            adminRelays
        );

        try {
            client.connect();
        } catch (Exception e) {
            throw new RuntimeException("Failed to connect to nsecBunker", e);
        }

        return client;
    }

    @Bean
    public IdentityToNsecBunkerSync bunkerSync(
        NsecBunkerAdminClient client
    ) {
        if (!syncEnabled || client == null) {
            return null;
        }

        return new IdentityToNsecBunkerSync(client, bunkerPassphrase);
    }
}
```

### 5. application.properties

```properties
# nsecBunker Admin Configuration
nsecbunker.sync.enabled=true

# Your admin nsec (must be in bunker's ADMIN_NPUBS)
nsecbunker.admin.nsec=nsec1...

# Bunker connection string (from: docker compose exec nsecbunkerd cat /app/config/connection.txt)
nsecbunker.connection.string=bunker://npub1...@relay.nsecbunker.com

# Admin relays (where bunker listens for admin commands)
nsecbunker.admin.relays=wss://relay.nsecbunker.com

# Passphrase to encrypt keys in bunker
nsecbunker.passphrase=${BUNKER_PASSPHRASE}
```

## Usage Examples

### Example 1: Push Existing Identity to Bunker

```java
@Service
public class IdentityService {

    @Autowired
    private IdentityRepository repository;

    @Autowired
    private IdentityToNsecBunkerSync bunkerSync;

    public void pushIdentityToBunker(String identityId) {
        // Find identity
        Identity identity = repository.findById(identityId)
            .orElseThrow(() -> new IdentityNotFoundException(identityId));

        // Push to bunker
        try {
            String npub = bunkerSync.syncIdentityToBunker(identity);
            System.out.println("Identity synced: " + npub);
        } catch (Exception e) {
            throw new RuntimeException("Failed to sync identity", e);
        }
    }
}
```

### Example 2: Create Identity Directly in Bunker

```java
@Service
public class IdentityService {

    @Autowired
    private CreateIdentityUseCase createUseCase;

    public Identity createRemoteIdentity(String label) {
        try {
            // Creates key in bunker, returns identity with pubkey only
            Identity identity = createUseCase.executeRemote(label);

            System.out.println("Created remote identity: " + identity.getId());
            System.out.println("Public key: " + identity.getPublicKey().toHex());
            System.out.println("Private key location: nsecBunker");

            return identity;

        } catch (Exception e) {
            throw new RuntimeException("Failed to create remote identity", e);
        }
    }
}
```

### Example 3: Batch Import

```java
public class BulkIdentityImport {

    public static void main(String[] args) throws Exception {
        // Setup bunker client
        NsecBunkerAdminClient bunker = new NsecBunkerAdminClient(
            "nsec1admin...",
            "bunker://npub1...@relay.nsecbunker.com",
            new String[]{ "wss://relay.nsecbunker.com" }
        );

        bunker.connect();

        IdentityToNsecBunkerSync sync = new IdentityToNsecBunkerSync(
            bunker,
            "secure-passphrase"
        );

        // Import existing identities
        List<Identity> identities = loadIdentitiesFromFile();

        for (Identity identity : identities) {
            System.out.println("Importing: " + identity.getId());

            try {
                String npub = sync.syncIdentityToBunker(identity);
                System.out.println("  ✓ Success: " + npub);
            } catch (Exception e) {
                System.err.println("  ✗ Failed: " + e.getMessage());
            }
        }

        bunker.disconnect();
    }
}
```

## Security Considerations

### 1. Admin nsec Protection

```java
// Never hardcode admin nsec!
// Load from environment variable or secure vault
String adminNsec = System.getenv("NSECBUNKER_ADMIN_NSEC");

if (adminNsec == null) {
    throw new IllegalStateException(
        "NSECBUNKER_ADMIN_NSEC environment variable not set"
    );
}
```

### 2. Passphrase Management

```java
// Use different passphrases per identity
String passphrase = generateSecurePassphrase(identityId);

// Or derive from master password
String passphrase = derivePassphrase(masterPassword, identityId);
```

### 3. Secure Communication

- All admin commands are encrypted with NIP-04
- Only authorized admin pubkeys can execute commands
- Commands are sent over Nostr relays (decentralized)
- No direct HTTP connections needed

### 4. Error Handling

```java
try {
    String npub = bunkerClient.createNewKey(keyName, nsec, passphrase).get();
} catch (TimeoutException e) {
    // Bunker didn't respond - check if key was created
    List<BunkerKey> keys = bunkerClient.getKeys().get();
    boolean exists = keys.stream()
        .anyMatch(k -> k.getName().equals(keyName));

    if (exists) {
        System.out.println("Key was created despite timeout");
    } else {
        throw new BunkerException("Failed to create key", e);
    }
} catch (ExecutionException e) {
    // Command failed - check the error message
    throw new BunkerException("Bunker error: " + e.getCause().getMessage(), e);
}
```

## Complete Flow Example

```java
public class CompleteIntegrationExample {

    public static void main(String[] args) throws Exception {
        // 1. Setup bunker admin client
        NsecBunkerAdminClient bunker = new NsecBunkerAdminClient(
            System.getenv("ADMIN_NSEC"),
            "bunker://npub1abc@relay.nsecbunker.com",
            new String[]{ "wss://relay.nsecbunker.com" }
        );

        bunker.connect();
        System.out.println("✓ Connected to nsecBunker admin interface");

        // 2. Generate a new identity locally
        KeyPair keyPair = KeyGenerator.generateKeyPair();
        PublicKey publicKey = PublicKey.fromNostrJava(keyPair.publicKey());
        PrivateKey privateKey = PrivateKey.fromNostrJava(keyPair.privateKey());

        Identity identity = new Identity(
            "alice-wallet",
            "Alice's Wallet",
            publicKey,
            privateKey,
            Instant.now(),
            null
        );

        System.out.println("✓ Generated identity: " + identity.getId());

        // 3. Push to bunker
        IdentityToNsecBunkerSync sync = new IdentityToNsecBunkerSync(
            bunker,
            "secure-passphrase"
        );

        String npub = sync.syncIdentityToBunker(identity);
        System.out.println("✓ Identity pushed to bunker: " + npub);

        // 4. Verify it was created
        List<BunkerKey> keys = bunker.getKeys().get();
        Optional<BunkerKey> found = keys.stream()
            .filter(k -> k.getName().equals("cashu-alice-wallet"))
            .findFirst();

        if (found.isPresent()) {
            System.out.println("✓ Verified key exists in bunker");
            System.out.println("  Name: " + found.get().getName());
            System.out.println("  NPub: " + found.get().getNpub());
        }

        // 5. Now you can use remote signing
        RemoteIdentityStorage remoteStorage = new RemoteIdentityStorage(
            new String[]{ "wss://relay.nsecbunker.com" }
        );

        remoteStorage.registerBunkerConnection(
            identity.getId(),
            "bunker://npub1abc@relay.nsecbunker.com"
        );

        System.out.println("✓ Ready for remote signing!");

        bunker.disconnect();
    }
}
```

## Conclusion

**Yes, the identity plugin can add nsec keys to nsecBunker programmatically!**

The integration works by:
1. ✅ Using nsecBunker's Admin RPC Interface (NIP-46)
2. ✅ Sending `create_new_key` commands with the nsec
3. ✅ Bunker encrypts and stores the key
4. ✅ Returns the npub for verification
5. ✅ All communication encrypted (NIP-04)

This enables cashu-client to:
- Generate identities locally, then push to bunker
- Or create identities directly in bunker
- Centralize key management
- Enable remote signing for all operations

**No manual CLI interaction required!**
