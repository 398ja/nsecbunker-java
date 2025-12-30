# Bottin Integration Guide

This guide explains how to integrate nsecbunker-java with [bottin](https://github.com/tcheeric/bottin), a NIP-05 identity registry service.

## Overview

nsecbunker-java provides interfaces for NIP-05 identity management through the `nsecbunker-account` module. By default, these use in-memory storage. Bottin provides persistent implementations backed by a database.

```
┌─────────────────────────────────────────────────────────────────┐
│                     Your Application                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────┐              ┌─────────────────┐          │
│  │ NsecBunkerClient│              │   Nip05Manager  │          │
│  │   (signing)     │              │ (identity mgmt) │          │
│  └────────┬────────┘              └────────┬────────┘          │
│           │                                │                    │
│           ▼                                ▼                    │
│     nsecbunkerd                    bottin / in-memory           │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## Integration Patterns

### Pattern 1: In-Memory (Default)

No additional dependencies required. Uses `DefaultNip05Manager` and `DefaultAccountManager`.

```yaml
# application.yml
nsecbunker:
  nip05:
    enabled: true
    provider: in-memory  # or "auto" (default)
```

### Pattern 2: Standalone Bottin (REST API)

Bottin runs as a separate service. Your application calls its REST API.

```java
@Service
public class Nip05RestClient {
    private final RestTemplate restTemplate;
    private final String bottinUrl;

    public Nip05RecordResponse createNip05(String username, String domain, String pubkey) {
        var request = Map.of(
            "username", username,
            "domain", domain,
            "pubkey", pubkey
        );
        return restTemplate.postForObject(
            bottinUrl + "/api/v1/records",
            request,
            Nip05RecordResponse.class
        );
    }
}
```

### Pattern 3: Embedded Bottin (Library)

Add bottin as a dependency and it automatically provides persistent implementations.

```xml
<dependency>
    <groupId>xyz.tcheeric</groupId>
    <artifactId>bottin-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

```yaml
# application.yml
nsecbunker:
  nip05:
    enabled: true
    provider: auto  # Will auto-select bottin (priority 100)

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/myapp
```

## Service Provider Interface (SPI)

nsecbunker-java uses an SPI pattern for pluggable NIP-05 implementations.

### Nip05ManagerProvider

```java
public interface Nip05ManagerProvider {
    Nip05Manager create();
    int priority();      // Higher = more preferred
    String name();       // e.g., "in-memory", "bottin"
    boolean isAvailable(); // Check if provider can be used
}
```

### AccountManagerProvider

```java
public interface AccountManagerProvider {
    AccountManager create();
    int priority();
    String name();
    boolean isAvailable();
}
```

### Priority Levels

| Priority | Provider Type |
|----------|---------------|
| 0        | In-memory (default) |
| 50       | Custom implementations |
| 100      | Persistent (bottin) |

## Creating a Custom Provider

```java
@Component
public class RedisNip05ManagerProvider implements Nip05ManagerProvider {

    private final RedisTemplate<String, Nip05Record> redis;

    public RedisNip05ManagerProvider(RedisTemplate<String, Nip05Record> redis) {
        this.redis = redis;
    }

    @Override
    public Nip05Manager create() {
        return new RedisNip05Manager(redis);
    }

    @Override
    public int priority() {
        return 75;  // Between in-memory and persistent
    }

    @Override
    public String name() {
        return "redis-cache";
    }

    @Override
    public boolean isAvailable() {
        try {
            redis.getConnectionFactory().getConnection().ping();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
```

## Configuration Reference

```yaml
nsecbunker:
  nip05:
    # Enable/disable NIP-05 functionality
    enabled: true

    # Provider selection: "auto", "in-memory", "bottin", or custom name
    provider: auto

    # Default relays for new NIP-05 records
    default-relays:
      - wss://relay.example.com

    # Auto-register NIP-05 when creating keys
    auto-register: false

    # Default domain for auto-registration
    default-domain: example.com
```

## Use Cases

### User Registration Flow

```java
@Service
@RequiredArgsConstructor
public class UserRegistrationService {

    private final NsecBunkerAdminClient adminClient;
    private final Nip05Manager nip05Manager;

    public CompletableFuture<UserAccount> registerUser(String username, String domain) {
        // 1. Create key in nsecbunkerd
        return adminClient.createKey(username)
            .thenCompose(bunkerKey -> {
                // 2. Create NIP-05 record
                return nip05Manager.setupNip05(username, domain)
                    .thenApply(nip05 -> new UserAccount(bunkerKey, nip05));
            });
    }
}
```

### NIP-05 Lookup

```java
@Service
@RequiredArgsConstructor
public class IdentityService {

    private final Nip05Manager nip05Manager;

    public CompletableFuture<Optional<String>> lookupPubkey(String nip05) {
        return nip05Manager.findByNip05(nip05)
            .thenApply(opt -> opt.map(Nip05Record::getPubkey));
    }

    public CompletableFuture<Boolean> isRegistered(String nip05) {
        return nip05Manager.verifyNip05(nip05);
    }
}
```

### Well-Known Endpoint

If you need to serve `.well-known/nostr.json`:

```java
@RestController
public class WellKnownController {

    private final Nip05Manager nip05Manager;

    @GetMapping("/.well-known/nostr.json")
    public ResponseEntity<String> nostrJson(@RequestParam(required = false) String name) {
        // If using bottin, it provides this endpoint automatically
        // Otherwise, implement using nip05Manager.findByDomain()
    }
}
```

## Troubleshooting

### Provider Not Found

```
WARN  nip05_manager_provider_not_found requested=bottin using=default
```

Ensure bottin-spring-boot-starter is on the classpath and configured correctly.

### Circular Dependency

If you see circular dependency errors, ensure your custom provider doesn't inject `Nip05Manager` directly. Use `@Lazy` or restructure dependencies.

### Database Connection Issues

With bottin, ensure the database is accessible:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/bottin
    username: ${BOTTIN_DB_USER}
    password: ${BOTTIN_DB_PASS}
```

## Related Documentation

- [NIP-05 Specification](https://github.com/nostr-protocol/nips/blob/master/05.md)
- [nsecbunker-account Module](../reference/account-module.md)
- [Bottin Project](https://github.com/tcheeric/bottin)
