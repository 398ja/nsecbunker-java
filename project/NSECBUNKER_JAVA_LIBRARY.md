# nsecbunker-java: Java Client Library for nsecBunker

## Executive Summary

**nsecbunker-java** is a comprehensive Java client library for interacting with nsecBunker instances. It provides high-level APIs for key management, remote signing, permission control, and monitoring - enabling Java applications to leverage nsecBunker's secure key storage and signing capabilities without dealing with low-level NIP-46 protocol details.

**Target Use Cases:**
- cashu-client identity plugin integration
- Nostr client applications requiring secure key management
- Enterprise applications needing centralized key delegation
- Multi-user Nostr applications with granular access control
- Automated bot/service accounts with restricted permissions

## Complete Feature Set

### 1. Admin Operations
- ✅ **Key Management**
  - Create new keys (with or without importing nsec)
  - List all keys in bunker
  - Unlock encrypted keys
  - Delete keys
  - Rotate keys (create new + migrate permissions)
  - Export key metadata (not private keys)

- ✅ **Policy Management**
  - Create policies with rules
  - List all policies
  - Update policy metadata
  - Delete policies
  - Attach policies to keys

- ✅ **Permission Management**
  - Grant permissions to pubkeys (create KeyUser)
  - Revoke permissions
  - List key users
  - List permissions for a specific user
  - Rename key users (update descriptions)

- ✅ **Token Management**
  - Create access tokens
  - List tokens for a key
  - Check token validity
  - Revoke tokens
  - Get token usage statistics

### 2. Remote Signing (NIP-46 Client)
- ✅ **Connection Management**
  - Parse bunker connection strings
  - Connect to bunker via relays
  - Handle connection lifecycle
  - Auto-reconnection with backoff
  - Multi-relay support with fallback

- ✅ **Signing Operations**
  - Sign Nostr events (all kinds)
  - Encrypt messages (NIP-04)
  - Decrypt messages (NIP-04)
  - Encrypt messages (NIP-44)
  - Decrypt messages (NIP-44)
  - Get public key
  - Ping/health check

- ✅ **Request Management**
  - Async request/response handling
  - Request timeouts
  - Request cancellation
  - Batch operations
  - Error handling and retry

### 3. Monitoring & Audit
- ✅ **Logging**
  - Get signing logs for a key
  - Filter logs by date/method/user
  - Export logs to CSV/JSON

- ✅ **Statistics**
  - Key usage statistics
  - Token usage tracking
  - User activity metrics
  - Policy enforcement metrics

- ✅ **Health Monitoring**
  - Bunker availability
  - Relay connection status
  - Response time monitoring
  - Error rate tracking

### 4. Account Management
- ✅ **OAuth-like Flow**
  - Create new user accounts
  - Setup NIP-05 identifiers
  - Configure wallet integration (LNBits)

### 5. Security Features
- ✅ **Credential Management**
  - Secure passphrase storage
  - Token validation
  - Connection string validation
  - Encrypted configuration storage

- ✅ **Permission Validation**
  - Pre-flight permission checks
  - Capability discovery
  - Access control validation

### 6. Utilities
- ✅ **Connection Helpers**
  - Parse bunker:// URIs
  - Generate connection strings
  - Relay URL validation

- ✅ **Data Converters**
  - Bech32 encoding/decoding (nsec, npub)
  - Hex ↔ bytes conversion
  - JSON serialization

- ✅ **Testing Utilities**
  - Mock bunker server
  - Test fixtures
  - Integration test helpers

## Library Architecture

### Module Structure

```
nsecbunker-java/
├── nsecbunker-core/              # Core functionality
│   ├── api/                      # Public API interfaces
│   ├── model/                    # Domain models
│   ├── connection/               # Connection management
│   └── security/                 # Security utilities
│
├── nsecbunker-admin/             # Admin operations
│   ├── key/                      # Key management
│   ├── policy/                   # Policy management
│   ├── permission/               # Permission management
│   └── token/                    # Token management
│
├── nsecbunker-client/            # NIP-46 client
│   ├── signer/                   # Remote signer
│   ├── protocol/                 # NIP-46 protocol
│   └── relay/                    # Relay communication
│
├── nsecbunker-monitoring/        # Monitoring & audit
│   ├── logging/                  # Log access
│   ├── metrics/                  # Statistics
│   └── health/                   # Health checks
│
├── nsecbunker-account/           # Account management
│   ├── registration/             # User registration
│   └── nip05/                    # NIP-05 management
│
└── nsecbunker-spring-boot-starter/ # Spring Boot integration
    ├── autoconfigure/
    └── properties/
```

### Core Components

```
┌─────────────────────────────────────────┐
│         Application Layer               │
│  (cashu-client, other Java apps)        │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│     nsecbunker-java Library             │
│                                          │
│  ┌────────────────────────────────┐     │
│  │   NsecBunkerClient (Facade)    │     │
│  └──┬─────────┬─────────┬─────────┘     │
│     │         │         │                │
│  ┌──▼──┐  ┌──▼──┐  ┌───▼────┐          │
│  │Admin│  │Signer│ │Monitor │          │
│  └──┬──┘  └──┬──┘  └───┬────┘          │
│     │        │         │                │
│  ┌──▼────────▼─────────▼──────┐        │
│  │   Protocol Layer (NIP-46)  │        │
│  └──────────┬──────────────────┘        │
│             │                           │
│  ┌──────────▼──────────────────┐       │
│  │   Transport Layer (Relays)  │       │
│  └─────────────────────────────┘       │
└─────────────────────────────────────────┘
                  │
                  │ WebSocket
                  │
┌─────────────────▼───────────────────────┐
│         Nostr Relays                    │
└─────────────────┬───────────────────────┘
                  │
┌─────────────────▼───────────────────────┐
│         nsecBunker Instance             │
└─────────────────────────────────────────┘
```

### Design Principles

1. **Fluent API**: Chainable, readable method calls
2. **Async-First**: CompletableFuture-based for non-blocking operations
3. **Type-Safe**: Strong typing, compile-time safety
4. **Immutable**: Immutable models where appropriate
5. **Testable**: Dependency injection, interface-based design
6. **Extensible**: Plugin architecture for custom functionality

## Implementation Phases

### Phase 1: Foundation (2-3 weeks)

**Goal**: Core infrastructure and basic connectivity

**Deliverables:**
- Working connection to nsecBunker
- Ability to send/receive encrypted NIP-46 messages
- Comprehensive unit tests (>80% coverage)

#### 1.1 Project Setup

| Task ID | Task Description | Note | Size | Commit ID | Dependency | Status |
|---------|-----------------|------|------|-----------|------------|--------|
| 1.1.1 | Create multi-module Maven project structure | Root pom.xml with modules | M | d96ba2c | - | ✅ DONE |
| 1.1.2 | Setup parent POM with dependency management | Added OkHttp, Micrometer, Testcontainers, Logback; fixed nostr-java groupId to xyz.tcheeric | S | d53a0a3 | 1.1.1 | ✅ DONE |
| 1.1.3 | Configure build plugins (compiler, javadoc, sources) | Enhanced compiler (release flag, -parameters, lint), JAR manifest, flatten plugin, failsafe for IT | S | 59f9675 | 1.1.2 | ✅ DONE |
| 1.1.4 | Setup code quality tools (Checkstyle, SpotBugs, PMD) | Added checkstyle.xml, spotbugs-exclude.xml, pmd-ruleset.xml; quality profile ready | M | 565c570 | 1.1.2 | ✅ DONE |
| 1.1.5 | Configure CI/CD (GitHub Actions) | CI, CodeQL, release, dependency review workflows; issue/PR templates | M | d1734b6 | 1.1.3, 1.1.4 | ✅ DONE |
| 1.1.6 | Create README and basic documentation structure | README with badges, install, examples; CONTRIBUTING.md; SECURITY.md | S | cd153e7 | 1.1.1 | ✅ DONE |

#### 1.2 Core Models (`nsecbunker-core/model`)

| Task ID | Task Description | Note | Size | Commit ID | Dependency | Status |
|---------|-----------------|------|------|-----------|------------|--------|
| 1.2.1 | Implement `BunkerConnection` model | Connection string representation with builder, toConnectionString() | S | c2e7cfa | 1.1.1 | ✅ DONE |
| 1.2.2 | Implement `BunkerKey` model | Key metadata with name, npub, pubkeyHex, counts, timestamps | S | c2e7cfa | 1.1.1 | ✅ DONE |
| 1.2.3 | Implement `BunkerPolicy` model | Policy with rules list, expiration, validation methods | S | c2e7cfa | 1.1.1 | ✅ DONE |
| 1.2.4 | Implement `PolicyRule` model | ALLOW/DENY rules for methods, event kinds, usage limits | S | c2e7cfa | 1.2.3 | ✅ DONE |
| 1.2.5 | Implement `KeyUser` model | User pubkey, policy, signing conditions, activity tracking | S | c2e7cfa | 1.1.1 | ✅ DONE |
| 1.2.6 | Implement `SigningCondition` model | ACL conditions: event kind, method, time window, rate limit | S | c2e7cfa | 1.2.5 | ✅ DONE |
| 1.2.7 | Implement `AccessToken` model | Token with policy, expiration, usage tracking, connection string | S | c2e7cfa | 1.2.3 | ✅ DONE |
| 1.2.8 | Create `BunkerException` hierarchy | 6 exception types: Connection, Auth, AuthZ, Protocol, Timeout, Key | S | c2e7cfa | 1.1.1 | ✅ DONE |

#### 1.3 Connection Management (`nsecbunker-core/connection`)

| Task ID | Task Description | Note | Size | Commit ID | Dependency | Status |
|---------|-----------------|------|------|-----------|------------|--------|
| 1.3.1 | Implement `BunkerConnectionString` parser | parse/tryParse/build, hex/npub validation, URL encoding, 31 tests | M | 6cd6fc3 | 1.2.1 | ✅ DONE |
| 1.3.2 | Implement `RelayConnection` | OkHttp WebSocket, ConnectionState enum, RelayListener, 31 tests | L | 6da84ed | 1.1.2 | ✅ DONE |
| 1.3.3 | Implement `RelayPool` | Multi-relay management, broadcasting, deduplication, RelayPoolListener, 35 tests | L | a1e9de9 | 1.3.2 | ✅ DONE |
| 1.3.4 | Implement `ConnectionListener` interface | ConnectionListener with default methods, LoggingConnectionListener, CompositeConnectionListener, RelayConnection integration, 23 tests | S | e8454d1 | 1.3.2 | ✅ DONE |
| 1.3.5 | Implement `ReconnectionStrategy` | ReconnectionStrategy interface, ExponentialBackoffStrategy with jitter, FixedDelayStrategy, NoReconnectionStrategy, RelayConnection integration, 44 tests | M | 104c98c | 1.3.2 | ✅ DONE |
| 1.3.6 | Add connection health monitoring | ConnectionHealth model, HealthMonitor interface, RelayHealthMonitor with latency/ping tracking, RelayConnection/RelayPool integration, 46 tests | M | b8322f3 | 1.3.3 | ✅ DONE |

#### 1.4 Protocol Layer (`nsecbunker-client/protocol`)

| Task ID | Task Description | Note | Size | Commit ID | Dependency | Status |
|---------|-----------------|------|------|-----------|------------|--------|
| 1.4.1 | Implement `Nip46Request` model | Request format with id, method, params | S | 7562f75 | 1.1.1 | ✅ DONE |
| 1.4.2 | Implement `Nip46Response` model | Response format with id, result/error | S | 7562f75 | 1.1.1 | ✅ DONE |
| 1.4.3 | Implement `Nip46Encoder` | JSON encoding for requests | S | 7562f75 | 1.4.1 | ✅ DONE |
| 1.4.4 | Implement `Nip46Decoder` | JSON decoding for responses | S | 7562f75 | 1.4.2 | ✅ DONE |
| 1.4.5 | Implement `Nip04Crypto` | NIP-04 encryption/decryption | M | 00aa562 | 1.1.2 | ✅ DONE |
| 1.4.6 | Add request/response correlation | Match responses to requests by ID | M | 00aa562 | 1.4.1, 1.4.2 | ✅ DONE |

#### 1.5 Testing Infrastructure

| Task ID | Task Description | Note | Size | Commit ID | Dependency | Status |
|---------|-----------------|------|------|-----------|------------|--------|
| 1.5.1 | Create mock relay server | For testing without real relays | L | 12fd202 | 1.3.2 | ✅ DONE |
| 1.5.2 | Create mock bunker server | Simulate bunker responses | L | 585e5b1 | 1.4.1, 1.4.2 | ✅ DONE |
| 1.5.3 | Create test fixtures and builders | Reusable test data | M | 5eb044c | 1.2.* | ✅ DONE |
| 1.5.4 | Create integration test base classes | Common setup for integration tests | M | 5eb044c | 1.5.1, 1.5.2 | ✅ DONE |

---

### Phase 2: Admin Operations (2-3 weeks)

**Goal**: Complete admin interface for key and permission management

**Deliverables:**
- Complete admin API
- Fluent builders for complex operations
- Comprehensive integration tests
- Admin API documentation

#### 2.1 Admin Client Foundation (`nsecbunker-admin`)

| Task ID | Task Description | Note | Size | Commit ID | Dependency | Status |
|---------|-----------------|------|------|-----------|------------|--------|
| 2.1.1 | Implement `NsecBunkerAdminClient` class | Main admin client with builder | M | 5928e8c | Phase 1 | ✅ DONE |
| 2.1.2 | Add admin connection setup | Generate ephemeral keypair for auth | S | 5928e8c | 2.1.1 | ✅ DONE |
| 2.1.3 | Implement admin request/response handling | Send kind 24134 messages | M | 5928e8c | 2.1.1, 1.4.* | ✅ DONE |
| 2.1.4 | Add admin authentication validation | Verify admin npub matches | S | 5928e8c | 2.1.3 | ✅ DONE |

#### 2.2 Key Management (`nsecbunker-admin/key`)

| Task ID | Task Description | Note | Size | Commit ID | Dependency | Status |
|---------|-----------------|------|------|-----------|------------|--------|
| 2.2.1 | Create `KeyManager` interface | Abstract key operations | S | acdaf63 | 2.1.1 | ✅ DONE |
| 2.2.2 | Implement `DefaultKeyManager` | Concrete implementation | M | acdaf63 | 2.2.1 | ✅ DONE |
| 2.2.3 | Implement `createKey(name, nsec, passphrase)` | Import existing key | S | acdaf63 | 2.2.2 | ✅ DONE |
| 2.2.4 | Implement `createKey(name, passphrase)` | Generate new key in bunker | S | acdaf63 | 2.2.2 | ✅ DONE |
| 2.2.5 | Implement `listKeys()` | Get all keys with metadata | S | acdaf63 | 2.2.2 | ✅ DONE |
| 2.2.6 | Implement `unlockKey(name, passphrase)` | Decrypt key in bunker | S | acdaf63 | 2.2.2 | ✅ DONE |
| 2.2.7 | Implement `deleteKey(name)` | Remove key from bunker | S | acdaf63 | 2.2.2 | ✅ DONE |
| 2.2.8 | Implement `getKeyDetails(name)` | Get single key metadata | S | acdaf63 | 2.2.2 | ✅ DONE |
| 2.2.9 | Implement `rotateKey(oldName, newName, passphrase)` | Create new + migrate permissions | L | acdaf63 | 2.2.3, 2.4.* | ✅ DONE |

#### 2.3 Policy Management (`nsecbunker-admin/policy`)

| Task ID | Task Description | Note | Size | Commit ID | Dependency | Status |
|---------|-----------------|------|------|-----------|------------|--------|
| 2.3.1 | Create `PolicyManager` interface | Abstract policy operations | S | cce2ca7 | 2.1.1 | ✅ DONE |
| 2.3.2 | Implement `DefaultPolicyManager` | Concrete implementation | M | cce2ca7 | 2.3.1 | ✅ DONE |
| 2.3.3 | Implement `PolicyBuilder` | Fluent API for policies | M | cce2ca7 | 1.2.3, 1.2.4 | ✅ DONE |
| 2.3.4 | Implement `createPolicy(policy)` | Create policy with rules | S | cce2ca7 | 2.3.2 | ✅ DONE |
| 2.3.5 | Implement `listPolicies()` | Get all policies | S | cce2ca7 | 2.3.2 | ✅ DONE |
| 2.3.6 | Implement `getPolicy(id)` | Get single policy | S | cce2ca7 | 2.3.2 | ✅ DONE |
| 2.3.7 | Implement `deletePolicy(id)` | Remove policy | S | cce2ca7 | 2.3.2 | ✅ DONE |
| 2.3.8 | Create pre-defined policy templates | Read-only, full-access, etc. | M | cce2ca7 | 2.3.3 | ✅ DONE |

#### 2.4 Permission Management (`nsecbunker-admin/permission`)

| Task ID | Task Description | Note | Size | Commit ID | Dependency | Status |
|---------|-----------------|------|------|-----------|------------|--------|
| 2.4.1 | Create `PermissionManager` interface | Abstract permission operations | S | 24d9e9f | 2.1.1 | ✅ DONE |
| 2.4.2 | Implement `DefaultPermissionManager` | Concrete implementation | M | 24d9e9f | 2.4.1 | ✅ DONE |
| 2.4.3 | Implement `grantPermission(keyName, userPubkey, policy)` | Create KeyUser with policy | M | 24d9e9f | 2.4.2 | ✅ DONE |
| 2.4.4 | Implement `revokePermission(keyName, userPubkey)` | Remove KeyUser access | S | 24d9e9f | 2.4.2 | ✅ DONE |
| 2.4.5 | Implement `listKeyUsers(keyName)` | Get all users for key | S | 24d9e9f | 2.4.2 | ✅ DONE |
| 2.4.6 | Implement `getPermissions(keyName, userPubkey)` | Get specific user permissions | S | 24d9e9f | 2.4.2 | ✅ DONE |
| 2.4.7 | Implement `updateKeyUserDescription(keyName, userPubkey, desc)` | Update user metadata | S | 24d9e9f | 2.4.2 | ✅ DONE |

#### 2.5 Token Management (`nsecbunker-admin/token`)

| Task ID | Task Description | Note | Size | Commit ID | Dependency | Status |
|---------|-----------------|------|------|-----------|------------|--------|
| 2.5.1 | Create `TokenManager` interface | Abstract token operations | S | a247a9e | 2.1.1 | ✅ DONE |
| 2.5.2 | Implement `DefaultTokenManager` | Concrete implementation | M | a247a9e | 2.5.1 | ✅ DONE |
| 2.5.3 | Implement `createToken(keyName, clientName, policyId, duration)` | Create access token | M | a247a9e | 2.5.2 | ✅ DONE |
| 2.5.4 | Implement `listTokens(keyName)` | Get all tokens for key | S | a247a9e | 2.5.2 | ✅ DONE |
| 2.5.5 | Implement `getToken(tokenId)` | Get single token details | S | a247a9e | 2.5.2 | ✅ DONE |
| 2.5.6 | Implement `revokeToken(tokenId)` | Invalidate token | S | a247a9e | 2.5.2 | ✅ DONE |
| 2.5.7 | Implement `validateToken(token)` | Check token validity | S | a247a9e | 2.5.2 | ✅ DONE |

#### 2.6 Integration Tests

| Task ID | Task Description | Note | Size | Commit ID | Dependency | Status |
|---------|-----------------|------|------|-----------|------------|--------|
| 2.6.1 | Create admin connection lifecycle tests | Connect, disconnect, reconnect | M | 7c82fe0 | 2.1.* | ✅ DONE |
| 2.6.2 | Create key CRUD operation tests | Create, list, unlock, delete | M | 8c396f5 | 2.2.* | ✅ DONE |
| 2.6.3 | Create policy CRUD operation tests | Create, list, get, delete | M | 8c396f5 | 2.3.* | ✅ DONE |
| 2.6.4 | Create permission grant/revoke tests | Grant, revoke, list | M | 8c396f5 | 2.4.* | ✅ DONE |
| 2.6.5 | Create token creation/validation tests | Create, validate, revoke | M | 8c396f5 | 2.5.* | ✅ DONE |

---

### Phase 3: Remote Signing (3-4 weeks)

**Goal**: Full NIP-46 client implementation with remote signing

**Deliverables:**
- Complete remote signer implementation
- nostr-java compatibility
- Performance benchmarks
- Signing API documentation

#### 3.1 Signer Foundation (`nsecbunker-client/signer`)

| Task ID | Task Description | Note | Size | Commit ID | Dependency | Status |
|---------|-----------------|------|------|-----------|------------|--------|
| 3.1.1 | Create `RemoteSigner` interface | Extends nostr-java signer interface | S | 5e5e44b | Phase 1 | ✅ DONE |
| 3.1.2 | Implement `NsecBunkerSigner` class | Concrete remote signer | L | 5e5e44b | 3.1.1 | ✅ DONE |
| 3.1.3 | Add ephemeral keypair generation | For client-side NIP-46 auth | S | 5e5e44b | 3.1.2 | ✅ DONE |
| 3.1.4 | Implement connection establishment flow | Initial connect handshake | M | 5e5e44b | 3.1.2 | ✅ DONE |
| 3.1.5 | Implement permission request flow | Request signing permission | M | 5e5e44b | 3.1.4 | ✅ DONE |

#### 3.2 Signing Operations (`nsecbunker-client/signer`)

| Task ID | Task Description | Note | Size | Commit ID | Dependency | Status |
|---------|-----------------|------|------|-----------|------------|--------|
| 3.2.1 | Implement `signEvent(event)` | Sign Nostr event remotely | M | 5abfc61 | 3.1.2 | ✅ DONE |
| 3.2.2 | Implement `getPublicKey()` | Get remote key's pubkey | S | 5abfc61 | 3.1.2 | ✅ DONE |
| 3.2.3 | Implement `encrypt(pubkey, message)` | NIP-04 encryption | S | 5abfc61 | 3.1.2 | ✅ DONE |
| 3.2.4 | Implement `decrypt(pubkey, ciphertext)` | NIP-04 decryption | S | 5abfc61 | 3.1.2 | ✅ DONE |
| 3.2.5 | Implement `encryptNip44(pubkey, message)` | NIP-44 encryption | S | 5abfc61 | 3.1.2 | ✅ DONE |
| 3.2.6 | Implement `decryptNip44(pubkey, ciphertext)` | NIP-44 decryption | S | 5abfc61 | 3.1.2 | ✅ DONE |
| 3.2.7 | Implement `ping()` | Health check / keepalive | S | 5abfc61 | 3.1.2 | ✅ DONE |

#### 3.3 Request Management (`nsecbunker-client`)

| Task ID | Task Description | Note | Size | Commit ID | Dependency | Status |
|---------|-----------------|------|------|-----------|------------|--------|
| 3.3.1 | Implement `RequestQueue` | Queue pending requests | M | aa2e180 | 3.1.2 | ✅ DONE |
| 3.3.2 | Implement `RequestExecutor` | Execute requests async | M | aa2e180 | 3.3.1 | ✅ DONE |
| 3.3.3 | Add request timeout handling | CompletableFuture timeouts | S | aa2e180 | 3.3.2 | ✅ DONE |
| 3.3.4 | Add request cancellation | Cancel pending requests | S | aa2e180 | 3.3.2 | ✅ DONE |
| 3.3.5 | Implement request retry logic | Retry on transient failures | M | aa2e180 | 3.3.2 | ✅ DONE |
| 3.3.6 | Implement error recovery strategies | Fallback, circuit breaker | L | aa2e180 | 3.3.5 | ✅ DONE |

#### 3.4 Batch Operations

| Task ID | Task Description | Note | Size | Commit ID | Dependency | Status |
|---------|-----------------|------|------|-----------|------------|--------|
| 3.4.1 | Implement `BatchSigner` class | Batch multiple operations | M | f886d37 | 3.2.* | ✅ DONE |
| 3.4.2 | Implement `signEvents(events)` | Sign multiple events | S | f886d37 | 3.4.1 | ✅ DONE |
| 3.4.3 | Add batch result aggregation | Collect all results | S | f886d37 | 3.4.2 | ✅ DONE |
| 3.4.4 | Add partial failure handling | Some succeed, some fail | M | f886d37 | 3.4.3 | ✅ DONE |

#### 3.5 Connection Authorization

| Task ID | Task Description | Note | Size | Commit ID | Dependency | Status |
|---------|-----------------|------|------|-----------|------------|--------|
| 3.5.1 | Implement `connect()` method | Initial connection request | M | f0a791b | 3.1.4 | ✅ DONE |
| 3.5.2 | Add authorization URL handling | Handle auth_url responses | M | f0a791b | 3.5.1 | ✅ DONE |
| 3.5.3 | Add token-based connection | Connect with bunker token | S | f0a791b | 3.5.1 | ✅ DONE |
| 3.5.4 | Implement connection state management | Track connection state | M | f0a791b | 3.5.1 | ✅ DONE |

#### 3.6 Integration with nostr-java

| Task ID | Task Description | Note | Size | Commit ID | Dependency | Status |
|---------|-----------------|------|------|-----------|------------|--------|
| 3.6.1 | Create adapter for `IIdentity` interface | nostr-java compatibility | M | ad13001 | 3.1.1 | ✅ DONE |
| 3.6.2 | Implement compatibility layer | Bridge differences | M | ad13001 | 3.6.1 | ✅ DONE |
| 3.6.3 | Add event signing integration | Work with nostr-java events | S | ad13001 | 3.6.2, 3.2.1 | ✅ DONE |
| 3.6.4 | Add profile management | Fetch/update profiles | M | cb42276 | 3.6.2 | ✅ DONE |

#### 3.7 Integration Tests

| Task ID | Task Description | Note | Size | Commit ID | Dependency | Status |
|---------|-----------------|------|------|-----------|------------|--------|
| 3.7.1 | Create end-to-end signing tests | Full flow: connect → sign → verify | M | 57acacd | 3.2.* | ✅ DONE |
| 3.7.2 | Create multi-event signing tests | Batch operations | M | 57acacd | 3.4.* | ✅ DONE |
| 3.7.3 | Create encryption/decryption tests | NIP-04 and NIP-44 | M | 57acacd | 3.2.3-3.2.6 | ✅ DONE |
| 3.7.4 | Create connection lifecycle tests | Connect, disconnect, reconnect | M | 57acacd | 3.5.* | ✅ DONE |
| 3.7.5 | Create error scenario tests | Timeouts, failures, retries | M | 57acacd | 3.3.* | ✅ DONE |
| 3.7.6 | Create performance tests | Latency, throughput benchmarks | L | 57acacd | 3.7.1-3.7.5 | ✅ DONE |

---

### Phase 4: Monitoring & Audit (1-2 weeks)

**Goal**: Observability, logging, and metrics

**Deliverables:**
- Monitoring API
- Metrics collection
- Health check implementation
- Monitoring documentation

#### 4.1 Logging (`nsecbunker-monitoring/logging`)

| Task ID | Task Description | Note | Size | Commit ID | Dependency | Status |
|---------|-----------------|------|------|-----------|------------|--------|
| 4.1.1 | Create `LogReader` interface | Abstract log operations | S | dce2fee | Phase 2 | ✅ DONE |
| 4.1.2 | Implement `DefaultLogReader` | Concrete implementation | M | dce2fee | 4.1.1 | ✅ DONE |
| 4.1.3 | Implement `getLogs(keyName, filter)` | Get signing logs | S | dce2fee | 4.1.2 | ✅ DONE |
| 4.1.4 | Implement `LogFilter` class | Filter by date, method, user | M | dce2fee | 4.1.3 | ✅ DONE |
| 4.1.5 | Implement `exportLogs(format)` | Export to CSV/JSON | M | dce2fee | 4.1.3 | ✅ DONE |
| 4.1.6 | Add log streaming | Real-time log updates | L | dce2fee | 4.1.3 | ✅ DONE |

#### 4.2 Statistics (`nsecbunker-monitoring/metrics`)

| Task ID | Task Description | Note | Size | Commit ID | Dependency | Status |
|---------|-----------------|------|------|-----------|------------|--------|
| 4.2.1 | Create `MetricsCollector` interface | Abstract metrics operations | S | 11564df | Phase 2 | ✅ DONE |
| 4.2.2 | Implement `DefaultMetricsCollector` | Concrete implementation | M | 11564df | 4.2.1 | ✅ DONE |
| 4.2.3 | Implement `getKeyStatistics(keyName)` | Key usage stats | S | 11564df | 4.2.2 | ✅ DONE |
| 4.2.4 | Implement `getTokenStatistics(tokenId)` | Token usage stats | S | 11564df | 4.2.2 | ✅ DONE |
| 4.2.5 | Implement `getUserStatistics(userPubkey)` | User activity metrics | S | 11564df | 4.2.2 | ✅ DONE |
| 4.2.6 | Implement `getPolicyStatistics(policyId)` | Policy enforcement stats | S | 11564df | 4.2.2 | ✅ DONE |
| 4.2.7 | Add time-series data aggregation | Aggregate over time periods | M | ca0c82d | 4.2.3-4.2.6 | ✅ DONE |

#### 4.3 Health Monitoring (`nsecbunker-monitoring/health`)

| Task ID | Task Description | Note | Size | Commit ID | Dependency | Status |
|---------|-----------------|------|------|-----------|------------|--------|
| 4.3.1 | Create `HealthChecker` interface | Abstract health checks | S | ca0c82d | Phase 3 | ✅ DONE |
| 4.3.2 | Implement `BunkerHealthChecker` | Concrete implementation | M | ca0c82d | 4.3.1 | ✅ DONE |
| 4.3.3 | Add bunker availability checks | Is bunker responding? | S | ca0c82d | 4.3.2 | ✅ DONE |
| 4.3.4 | Add relay connection health | Are relays connected? | S | 6c68af2 | 4.3.2 | ✅ DONE |
| 4.3.5 | Add response time tracking | Track signing latency | S | ca0c82d | 4.3.2 | ✅ DONE |
| 4.3.6 | Add error rate monitoring | Track failure rates | S | 6c68af2 | 4.3.2 | ✅ DONE |
| 4.3.7 | Implement circuit breaker pattern | Stop requests if unhealthy | M | 6c68af2 | 4.3.3-4.3.6 | ✅ DONE |

#### 4.4 Alerting

| Task ID | Task Description | Note | Size | Commit ID | Dependency | Status |
|---------|-----------------|------|------|-----------|------------|--------|
| 4.4.1 | Implement `AlertManager` class | Configure alerts | M | ca0c82d | 4.3.* | ✅ DONE |
| 4.4.2 | Add threshold-based alerts | Trigger on error rate, latency | M | ca0c82d | 4.4.1 | ✅ DONE |
| 4.4.3 | Add alert delivery | Callback, webhook | S | 6c68af2 | 4.4.2 | ✅ DONE |

#### 4.5 Integration Tests

| Task ID | Task Description | Note | Size | Commit ID | Dependency | Status |
|---------|-----------------|------|------|-----------|------------|--------|
| 4.5.1 | Create log retrieval tests | Get, filter, export logs | M | 6c68af2 | 4.1.* | ✅ DONE |
| 4.5.2 | Create metrics collection tests | Collect and aggregate metrics | M | 6c68af2 | 4.2.* | ✅ DONE |
| 4.5.3 | Create health check tests | Check health, trigger alerts | M | 6c68af2 | 4.3.*, 4.4.* | ✅ DONE |

---

### Phase 5: Account Management (1 week)

**Goal**: OAuth-like account creation and NIP-05 setup

**Deliverables:**
- Account management API
- NIP-05 integration
- Wallet setup functionality

#### 5.1 Account Registration (`nsecbunker-account/registration`)

| Task ID | Task Description | Note | Size | Commit ID | Dependency | Status |
|---------|-----------------|------|------|-----------|------------|--------|
| 5.1.1 | Create `AccountManager` interface | Abstract account operations | S | 8dba980 | Phase 2 | ✅ DONE |
| 5.1.2 | Implement `DefaultAccountManager` | Concrete implementation | M | 8dba980 | 5.1.1 | ✅ DONE |
| 5.1.3 | Implement `createAccount(username, domain)` | Create new account | S | 8dba980 | 5.1.2 | ✅ DONE |
| 5.1.4 | Implement account registration flow | Multi-step registration | M | 8dba980 | 5.1.3 | ✅ DONE |
| 5.1.5 | Add key generation for new accounts | Generate keys in bunker | S | 8dba980 | 5.1.4 | ✅ DONE |

#### 5.2 NIP-05 Management (`nsecbunker-account/nip05`)

| Task ID | Task Description | Note | Size | Commit ID | Dependency | Status |
|---------|-----------------|------|------|-----------|------------|--------|
| 5.2.1 | Create `Nip05Manager` interface | Abstract NIP-05 operations | S | 8dba980 | Phase 2 | ✅ DONE |
| 5.2.2 | Implement `DefaultNip05Manager` | Concrete implementation | M | 8dba980 | 5.2.1 | ✅ DONE |
| 5.2.3 | Implement `setupNip05(username, domain)` | Setup identifier | M | 8dba980 | 5.2.2 | ✅ DONE |
| 5.2.4 | Implement `verifyNip05(nip05)` | Verify identifier | M | 8dba980 | 5.2.2 | ✅ DONE |
| 5.2.5 | Add NIP-05 JSON generation | Generate .well-known JSON | S | 8dba980 | 5.2.3 | ✅ DONE |

#### 5.3 Wallet Integration (`nsecbunker-account/wallet`)

| Task ID | Task Description | Note | Size | Commit ID | Dependency | Status |
|---------|-----------------|------|------|-----------|------------|--------|
| 5.3.1 | Create `WalletManager` interface | Abstract wallet operations | S | 8dba980 | Phase 2 | ✅ DONE |
| 5.3.2 | Implement `LNBitsWalletManager` | LNBits implementation | M | 8dba980 | 5.3.1 | ✅ DONE |
| 5.3.3 | Implement `createWallet(keyName)` | Create LNBits wallet | S | 8dba980 | 5.3.2 | ✅ DONE |
| 5.3.4 | Implement `getWalletInfo(keyName)` | Get wallet details | S | 8dba980 | 5.3.2 | ✅ DONE |

---

### Phase 6: Spring Boot Integration (1 week)

**Goal**: Auto-configuration for Spring Boot applications

**Deliverables:**
- Spring Boot starter
- Auto-configuration
- Actuator integration
- Spring Boot example application

#### 6.1 Auto-Configuration (`nsecbunker-spring-boot-starter`)

| Task ID | Task Description | Note | Size | Commit ID | Dependency | Status |
|---------|-----------------|------|------|-----------|------------|--------|
| 6.1.1 | Create `NsecBunkerAutoConfiguration` class | Auto-config beans | M | cc72f8a | Phase 2, Phase 3 | ✅ DONE |
| 6.1.2 | Create `NsecBunkerProperties` class | Configuration properties | S | cc72f8a | 6.1.1 | ✅ DONE |
| 6.1.3 | Add bean definitions for clients | Admin, signer, monitor beans | M | cc72f8a | 6.1.1 | ✅ DONE |
| 6.1.4 | Add conditional configuration | Enable/disable features | S | cc72f8a | 6.1.1 | ✅ DONE |

#### 6.2 Actuator Integration

| Task ID | Task Description | Note | Size | Commit ID | Dependency | Status |
|---------|-----------------|------|------|-----------|------------|--------|
| 6.2.1 | Create health indicator for bunker connection | Spring Boot health check | S | cc72f8a | 6.1.1, Phase 4 | ✅ DONE |
| 6.2.2 | Add metrics for signing operations | Micrometer integration | M | 1b99c0d | 6.1.1, Phase 4 | ✅ DONE |
| 6.2.3 | Add info endpoint with bunker details | Show connection info | S | cc72f8a | 6.1.1 | ✅ DONE |

#### 6.3 Spring Integration

| Task ID | Task Description | Note | Size | Commit ID | Dependency | Status |
|---------|-----------------|------|------|-----------|------------|--------|
| 6.3.1 | Create `@EnableNsecBunker` annotation | Enable auto-configuration | S | cc72f8a | 6.1.1 | ✅ DONE |
| 6.3.2 | Add dependency injection support | @Autowired support | S | cc72f8a | 6.1.3 | ✅ DONE |
| 6.3.3 | Add profile-based configuration | Dev, prod profiles | S | cc72f8a | 6.1.2 | ✅ DONE |

---

### Phase 7: Documentation & Examples (1 week)

**Goal**: Comprehensive documentation and examples

**Deliverables:**
- Complete documentation site
- Multiple example applications
- Migration guides

#### 7.1 API Documentation

| Task ID | Task Description | Note | Size | Commit ID | Dependency | Status |
|---------|-----------------|------|------|-----------|------------|--------|
| 7.1.1 | Write Javadoc for all public APIs | Complete API docs | XL | | All phases | ⬜ TODO |
| 7.1.2 | Create architecture diagrams | System architecture | M | 79150e0 | All phases | ✅ DONE |
| 7.1.3 | Create sequence diagrams for key flows | Visual flow docs | M | 90422b3 | All phases | ✅ DONE |
| 7.1.4 | Write API reference documentation | Markdown API docs | L | 46d51c5 | 7.1.1 | ✅ DONE |

#### 7.2 User Guides

| Task ID | Task Description | Note | Size | Commit ID | Dependency | Status |
|---------|-----------------|------|------|-----------|------------|--------|
| 7.2.1 | Write Getting Started guide | Quick start tutorial | M | b3d02c3 | All phases | ✅ DONE |
| 7.2.2 | Write Admin operations guide | Key, policy, permission mgmt | M | b3d02c3 | Phase 2 | ✅ DONE |
| 7.2.3 | Write Remote signing guide | Using remote signer | M | b3d02c3 | Phase 3 | ✅ DONE |
| 7.2.4 | Write Monitoring guide | Logs, metrics, health | M | b3d02c3 | Phase 4 | ✅ DONE |
| 7.2.5 | Write Spring Boot integration guide | Using with Spring Boot | M | b3d02c3 | Phase 6 | ✅ DONE |
| 7.2.6 | Write Security best practices | Security guidelines | L | b3d02c3 | All phases | ✅ DONE |
| 7.2.7 | Write migration from local keys guide | Move to bunker | M | b3d02c3 | Phase 2, Phase 3 | ✅ DONE |
| 7.2.8 | Write migration from other signers guide | Switch to nsecbunker-java | M | b3d02c3 | Phase 3 | ✅ DONE |

#### 7.3 Example Applications

| Task ID | Task Description | Note | Size | Commit ID | Dependency | Status |
|---------|-----------------|------|------|-----------|------------|--------|
| 7.3.1 | Create simple signer example | Basic signing app | M | cc72f8a | Phase 3 | ✅ DONE |
| 7.3.2 | Create admin CLI example | Command-line admin tool | M | cc72f8a | Phase 2 | ✅ DONE |
| 7.3.3 | Create cashu-client integration example | Integration demo | L | a319ed9 | Phase 3 | ✅ DONE |
| 7.3.4 | Create Spring Boot application example | Full Spring Boot app | L | a319ed9 | Phase 6 | ✅ DONE |
| 7.3.5 | Create multi-tenant application example | Multi-user scenario | XL | 1b0c447 | Phase 2, Phase 3 | ✅ DONE |

#### 7.4 Migration Guides

| Task ID | Task Description | Note | Size | Commit ID | Dependency | Status |
|---------|-----------------|------|------|-----------|------------|--------|
| 7.4.1 | Write migration from local keys guide | Move to bunker | M | b6e2194 | Phase 2, Phase 3 | ✅ DONE |
| 7.4.2 | Write migration from other signers guide | Switch to nsecbunker-java | M | b6e2194 | Phase 3 | ✅ DONE |

---

### Phase 8: Testing & Hardening (1-2 weeks)

**Goal**: Production readiness

**Deliverables:**
- Production-ready library
- Performance benchmarks
- Security audit report

#### 8.1 Comprehensive Testing

| Task ID | Task Description | Note | Size | Commit ID | Dependency | Status |
|---------|-----------------|------|------|-----------|------------|--------|
| 8.1.1 | Increase unit test coverage to >90% | Add missing tests | XL | | All phases | ⬜ TODO |
| 8.1.2 | Create integration test suite | End-to-end integration | L | | All phases | ⬜ TODO |
| 8.1.3 | Create end-to-end test suite | Full user scenarios | L | | All phases | ⬜ TODO |
| 8.1.4 | Create performance test suite | Load, stress tests | L | | Phase 3 | ⬜ TODO |
| 8.1.5 | Create security test suite | Security scanning | M | | All phases | ⬜ TODO |
| 8.1.6 | Add chaos testing | Network failures, etc. | M | | All phases | ⬜ TODO |

#### 8.2 Performance Optimization

| Task ID | Task Description | Note | Size | Commit ID | Dependency | Status |
|---------|-----------------|------|------|-----------|------------|--------|
| 8.2.1 | Profile signing operations | Find bottlenecks | M | | Phase 3 | ⬜ TODO |
| 8.2.2 | Optimize request serialization | Reduce overhead | M | | Phase 1 | ⬜ TODO |
| 8.2.3 | Tune connection pool | Optimize connections | M | | Phase 1 | ⬜ TODO |
| 8.2.4 | Optimize memory usage | Reduce memory footprint | L | | All phases | ⬜ TODO |

#### 8.3 Security Hardening

| Task ID | Task Description | Note | Size | Commit ID | Dependency | Status |
|---------|-----------------|------|------|-----------|------------|--------|
| 8.3.1 | Conduct security audit | External audit | L | | All phases | ⬜ TODO |
| 8.3.2 | Run dependency vulnerability scanning | Check dependencies | S | | All phases | ⬜ TODO |
| 8.3.3 | Review input validation | Validate all inputs | M | | All phases | ⬜ TODO |
| 8.3.4 | Ensure secure defaults | Safe default config | S | | All phases | ⬜ TODO |

#### 8.4 Error Handling

| Task ID | Task Description | Note | Size | Commit ID | Dependency | Status |
|---------|-----------------|------|------|-----------|------------|--------|
| 8.4.1 | Write comprehensive error messages | Clear error messages | M | | All phases | ⬜ TODO |
| 8.4.2 | Implement error recovery strategies | Graceful degradation | M | | All phases | ⬜ TODO |
| 8.4.3 | Add logging and diagnostics | Debug information | M | | All phases | ⬜ TODO |

---

## API Design Examples

### Fluent Admin API

```java
// Create admin client
NsecBunkerAdmin admin = NsecBunkerAdmin.builder()
    .adminNsec(System.getenv("ADMIN_NSEC"))
    .connectionString("bunker://npub@relay.nsecbunker.com")
    .relays("wss://relay.nsecbunker.com")
    .build();

admin.connect();

// Create a key
BunkerKey key = admin.keys()
    .create("cashu-alice-wallet")
    .withNsec("nsec1...")
    .withPassphrase("secure-passphrase")
    .execute()
    .get();

// Create a policy
BunkerPolicy policy = admin.policies()
    .create("cashu-wallet-policy")
    .allowEventKind(1)    // Text notes
    .allowEventKind(4)    // Encrypted DMs
    .allowEventKind(30078) // Cashu tokens
    .allowMethod("encrypt")
    .allowMethod("decrypt")
    .expiresIn(Duration.ofDays(30))
    .execute()
    .get();

// Grant permissions
admin.permissions()
    .grant(key.getName(), clientPubkey)
    .withPolicy(policy)
    .withDescription("cashu-client instance")
    .execute()
    .get();

// Create a token
AccessToken token = admin.tokens()
    .create(key.getName(), "cashu-client")
    .withPolicy(policy)
    .expiresIn(Duration.ofDays(7))
    .execute()
    .get();

System.out.println("Token: " + token.getFullToken());
```

### Fluent Signer API

```java
// Create remote signer
RemoteSigner signer = RemoteSigner.builder()
    .connectionString("bunker://npub@relay")
    .relays("wss://relay.nsecbunker.com")
    .timeout(Duration.ofSeconds(30))
    .build();

// Connect
signer.connect().get();

// Sign an event
BaseEvent event = new GenericEvent(
    signer.getPublicKey(),
    1,
    "Hello from nsecbunker-java!"
);

Signature signature = signer.signEvent(event).get();

System.out.println("Signed: " + event.getId());

// Encrypt a message
String encrypted = signer.encrypt(
    recipientPubkey,
    "Secret message"
).get();

// Batch signing
List<BaseEvent> events = Arrays.asList(event1, event2, event3);
List<Signature> signatures = signer.signEvents(events).get();
```

### Monitoring API

```java
// Create monitoring client
NsecBunkerMonitor monitor = NsecBunkerMonitor.builder()
    .adminClient(admin)
    .build();

// Get key statistics
KeyStatistics stats = monitor.metrics()
    .forKey("cashu-alice-wallet")
    .since(Instant.now().minus(Duration.ofDays(7)))
    .get();

System.out.println("Signatures: " + stats.getSignatureCount());
System.out.println("Users: " + stats.getActiveUsers());

// Get logs
List<SigningLog> logs = monitor.logs()
    .forKey("cashu-alice-wallet")
    .filterByMethod("sign_event")
    .since(Instant.now().minus(Duration.ofHours(24)))
    .limit(100)
    .get();

// Health check
HealthStatus health = monitor.health()
    .checkBunker()
    .checkRelays()
    .execute()
    .get();

System.out.println("Bunker: " + health.getBunkerStatus());
System.out.println("Relays: " + health.getRelayStatus());
```

### Spring Boot Integration

```java
@SpringBootApplication
@EnableNsecBunker
public class CashuApplication {

    @Autowired
    private NsecBunkerAdmin admin;

    @Autowired
    private RemoteSigner signer;

    @PostConstruct
    public void init() {
        // Auto-configured clients ready to use
        System.out.println("nsecBunker connected!");
    }
}

@Service
public class IdentityService {

    @Autowired
    private RemoteSigner signer;

    public String signNote(String content) throws Exception {
        BaseEvent event = new GenericEvent(
            signer.getPublicKey(),
            1,
            content
        );

        signer.signEvent(event).get();
        return event.getId();
    }
}
```

## Testing Strategy

### Unit Tests
- Mock all external dependencies
- Test business logic in isolation
- Target >90% code coverage

### Integration Tests
- Use testcontainers for real nsecBunker instance
- Test full request/response cycles
- Test error scenarios

### End-to-End Tests
- Complete workflows (create key → grant permission → sign event)
- Multi-client scenarios
- Performance tests

### Performance Tests
- Signing throughput (events/second)
- Latency measurements (p50, p95, p99)
- Connection overhead
- Memory usage

## Documentation Structure

```
docs/
├── index.md                     # Landing page
├── getting-started.md           # Quick start guide
├── architecture.md              # Architecture overview
├── user-guide/
│   ├── admin-operations.md
│   ├── remote-signing.md
│   ├── monitoring.md
│   └── spring-boot.md
├── api-reference/
│   ├── admin-api.md
│   ├── signer-api.md
│   └── monitoring-api.md
├── examples/
│   ├── simple-signer.md
│   ├── cashu-integration.md
│   └── spring-boot-app.md
└── advanced/
    ├── security.md
    ├── performance.md
    └── troubleshooting.md
```

## Dependencies

### Core Dependencies
- `nostr-java` - Nostr protocol implementation
- `okhttp` - WebSocket client
- `jackson` - JSON serialization
- `slf4j` - Logging facade

### Optional Dependencies
- `spring-boot` - Spring Boot integration
- `micrometer` - Metrics
- `testcontainers` - Integration testing

## Success Criteria

### Phase Completion Criteria
- ✅ All tasks completed
- ✅ All tests passing
- ✅ Code review completed
- ✅ Documentation updated
- ✅ Examples working

### Release Criteria
- ✅ All phases completed
- ✅ >90% test coverage
- ✅ Security audit passed
- ✅ Performance benchmarks met
- ✅ Documentation complete
- ✅ At least 2 example applications
- ✅ Maven Central deployment ready

## Project Timeline

**Total Estimated Time: 12-16 weeks**

```
Week 1-3:   Phase 1 - Foundation
Week 4-6:   Phase 2 - Admin Operations
Week 7-10:  Phase 3 - Remote Signing
Week 11-12: Phase 4 - Monitoring
Week 13:    Phase 5 - Account Management
Week 14:    Phase 6 - Spring Boot Integration
Week 15:    Phase 7 - Documentation
Week 16:    Phase 8 - Testing & Hardening
```

## Risk Management

### Technical Risks
- **NIP-46 protocol changes**: Monitor nostr-java updates
- **Relay connectivity**: Implement robust retry logic
- **Performance**: Early performance testing

### Mitigation Strategies
- Continuous integration testing
- Performance monitoring from Phase 1
- Regular security reviews
- Community feedback loops

## Future Enhancements

### Post-V1
- **Multi-signature support**: Coordinate multiple bunkers
- **Hardware security module (HSM) integration**
- **Kubernetes operator**: Deploy bunker clusters
- **CLI tool**: Command-line interface
- **GUI application**: Desktop/web management UI
- **Webhook notifications**: Real-time alerts
- **GraphQL API**: Alternative query interface
- **Rate limiting**: Client-side rate limiting
- **Caching layer**: Local signature caching

## Conclusion

**nsecbunker-java** will provide a comprehensive, production-ready Java client library for nsecBunker, enabling secure key management and remote signing for Java applications. The phased approach ensures steady progress with testable milestones, while the modular architecture allows for flexibility and extensibility.

**Next Steps:**
1. Review and approve this specification
2. Create GitHub repository
3. Setup project infrastructure
4. Begin Phase 1 implementation
5. Establish weekly progress reviews

## Task Status Legend

- ⬜ TODO - Not started
- 🟦 IN PROGRESS - Currently being worked on
- ✅ DONE - Completed and verified
- ⏸️ BLOCKED - Waiting on dependency
- ⚠️ ISSUE - Has problems/blockers

---

**Document Version:** 2.0
**Last Updated:** 2025-11-24
**Status:** Draft - With Task Tracking
**Total Tasks:** 182
