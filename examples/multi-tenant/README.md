# Multi-Tenant Application Example

A comprehensive example demonstrating how to build a multi-tenant application
where each tenant has their own nsecBunker signer with isolated credentials.

## Features

- **Per-Tenant Signers**: Each tenant has their own NsecBunkerSigner instance
- **Lazy Initialization**: Signers are created on-demand when first used
- **Tenant Isolation**: Complete separation of credentials and operations
- **Connection Pooling**: Efficient reuse of signer connections
- **Automatic Cleanup**: Idle signers are evicted after configurable timeout
- **Lifecycle Management**: Suspend, activate, and delete tenants

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    TenantController                         │
│  (REST API endpoints for tenant and signing operations)    │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│                     TenantService                           │
│  (Business logic, tenant CRUD, signing orchestration)      │
└─────────────┬───────────────────────────────┬──────────────┘
              │                               │
┌─────────────▼─────────────┐   ┌─────────────▼─────────────┐
│    TenantRepository       │   │  TenantSignerRegistry     │
│  (Tenant data storage)    │   │  (Signer management)      │
└───────────────────────────┘   └─────────────┬─────────────┘
                                              │
                              ┌───────────────▼───────────────┐
                              │      NsecBunkerSigner         │
                              │   (Per-tenant instances)      │
                              └───────────────────────────────┘
```

## Components

### Tenant (Model)

```java
public record Tenant(
    String id,
    String name,
    TenantBunkerConfig bunkerConfig,
    TenantStatus status,
    Instant createdAt,
    Instant lastActiveAt
) { }
```

### TenantBunkerConfig

```java
public record TenantBunkerConfig(
    String bunkerPubkey,
    List<String> relays,
    String secret,
    String clientPrivateKey,
    String adminPrivateKey,  // optional
    boolean useEphemeralKey
) { }
```

## Quick Start

### 1. Create the Infrastructure

```java
// Create components
TenantRepository repository = new TenantRepository();
TenantSignerRegistry signerRegistry = new TenantSignerRegistry(
    Duration.ofSeconds(30),  // connect timeout
    Duration.ofSeconds(60)   // request timeout
);
TenantService service = new TenantService(repository, signerRegistry);
TenantController controller = new TenantController(service);

// Or use the application wrapper
MultiTenantDemoApplication app = new MultiTenantDemoApplication();
app.start();
TenantController controller = app.getController();
```

### 2. Register Tenants

```java
Tenant tenant = controller.createTenant(new CreateTenantRequest(
    "tenant-123",                    // unique ID
    "Acme Corporation",              // display name
    "bunker-pubkey-hex",             // bunker's public key
    List.of("wss://relay.example.com"),
    "optional-secret",               // bunker secret (optional)
    "client-privkey-hex",            // client private key
    null,                            // admin key (optional)
    true                             // use ephemeral keys
));
```

### 3. Perform Signing Operations

```java
// Sign an event
CompletableFuture<SignResponse> result = controller.signEvent(
    "tenant-123",
    eventJson
);

// Get public key
CompletableFuture<String> pubkey = controller.getPublicKey("tenant-123");

// Encrypt (NIP-44)
CompletableFuture<EncryptResponse> encrypted = controller.encrypt(
    "tenant-123",
    new EncryptRequest(recipientPubkey, "secret message")
);

// Decrypt (NIP-44)
CompletableFuture<DecryptResponse> decrypted = controller.decrypt(
    "tenant-123",
    new DecryptRequest(senderPubkey, ciphertext)
);
```

### 4. Manage Tenant Lifecycle

```java
// Suspend a tenant (closes signer, blocks operations)
controller.suspendTenant("tenant-123");

// Reactivate a tenant
controller.activateTenant("tenant-123");

// Delete a tenant (removes signer and data)
controller.deleteTenant("tenant-123");
```

## REST API Endpoints

If integrating with a REST framework (Spring, JAX-RS, etc.):

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/tenants` | Create tenant |
| GET | `/api/tenants` | List all tenants |
| GET | `/api/tenants/{id}` | Get tenant |
| DELETE | `/api/tenants/{id}` | Delete tenant |
| POST | `/api/tenants/{id}/suspend` | Suspend tenant |
| POST | `/api/tenants/{id}/activate` | Activate tenant |
| POST | `/api/tenants/{id}/sign` | Sign event |
| GET | `/api/tenants/{id}/pubkey` | Get public key |
| POST | `/api/tenants/{id}/encrypt` | Encrypt message |
| POST | `/api/tenants/{id}/decrypt` | Decrypt message |
| GET | `/api/tenants/{id}/status` | Get tenant status |

## Signer Registry Features

### Lazy Initialization

Signers are only created when a tenant performs their first operation:

```java
// No signer created yet
Tenant tenant = controller.createTenant(...);

// Signer created on first use
String signed = controller.signEvent(tenant.id(), event).join();
```

### Idle Eviction

Configure automatic cleanup of unused signers:

```java
// Evict signers idle for more than 30 minutes
signerRegistry.evictIdle(Duration.ofMinutes(30));
```

### Statistics

Monitor registry health:

```java
int activeSigners = signerRegistry.size();
boolean hasSignerForTenant = signerRegistry.exists("tenant-123");
```

## Project Structure

```
multi-tenant/
├── src/main/java/.../multitenant/
│   ├── model/
│   │   └── Tenant.java                 # Tenant entity
│   ├── repository/
│   │   └── TenantRepository.java       # Data storage
│   ├── service/
│   │   ├── TenantService.java          # Business logic
│   │   └── TenantSignerRegistry.java   # Signer management
│   ├── controller/
│   │   └── TenantController.java       # API layer
│   └── MultiTenantDemoApplication.java # Demo app
└── README.md
```

## Production Considerations

### 1. Persistent Storage

Replace `TenantRepository` with a database-backed implementation:

```java
public class JpaTenantRepository implements TenantRepository {
    // Use JPA/Hibernate for persistence
    // Encrypt credentials at rest using @Convert
}
```

### 2. Credential Security

- Encrypt tenant credentials at rest
- Use a secrets manager (HashiCorp Vault, AWS Secrets Manager)
- Implement key rotation
- Audit credential access

### 3. Rate Limiting

Implement per-tenant rate limits:

```java
public class RateLimitedTenantService extends TenantService {
    private final Map<String, RateLimiter> limiters;

    @Override
    public CompletableFuture<String> signEvent(String tenantId, String event) {
        if (!limiters.get(tenantId).tryAcquire()) {
            throw new RateLimitExceededException(tenantId);
        }
        return super.signEvent(tenantId, event);
    }
}
```

### 4. Authentication & Authorization

- Authenticate API requests (OAuth2, API keys)
- Verify tenant ownership on each request
- Implement RBAC for admin operations

### 5. Monitoring

- Export metrics per tenant (requests, latency, errors)
- Alert on high error rates
- Monitor connection health
- Track signer pool size

### 6. Connection Limits

Configure maximum signers and connection pooling:

```java
TenantSignerRegistry registry = new TenantSignerRegistry(
    Duration.ofSeconds(30),
    Duration.ofSeconds(60),
    maxSigners: 100,
    maxIdleTime: Duration.ofMinutes(30)
);
```

## Dependencies

```xml
<dependency>
    <groupId>xyz.tcheeric</groupId>
    <artifactId>nsecbunker-client</artifactId>
    <version>${nsecbunker.version}</version>
</dependency>
```

## Running the Demo

```bash
# Run the demo application
cd examples/multi-tenant
mvn compile exec:java -Dexec.mainClass="xyz.tcheeric.nsecbunker.examples.multitenant.MultiTenantDemoApplication"
```

The demo creates sample tenants, demonstrates lifecycle operations, and shows
the statistics API. Note that actual signing operations require a running
nsecBunker instance.
