# Integration and E2E Test Suites Reference

This document provides a comprehensive reference for all integration and end-to-end tests in the nsecbunker-java library.

## Test Architecture Overview

The test infrastructure is organized into distinct layers:

```
nsecbunker-tests/
├── nsecbunker-e2e/       # Full end-to-end tests with real containers
├── nsecbunker-it/        # Integration tests (mocked/container-based)
├── nsecbunker-chaos/     # Network resilience and chaos tests
├── nsecbunker-security/  # Cryptographic and security validation
└── nsecbunker-perf/      # Performance benchmarks
```

---

## E2E Test Suite

**Location:** `nsecbunker-tests/nsecbunker-e2e/`

E2E tests validate complete workflows against real Docker containers running the Nostr relay and nsecbunkerd daemon.

### Shared Infrastructure

#### E2ETestBase

Base class providing common utilities for all E2E tests.

| Method | Description |
|--------|-------------|
| `getRelayUrl()` | Returns the WebSocket URL for the shared relay |
| `getAdminNsec()` | Returns the admin private key (nsec format) |
| `getAdminNpub()` | Returns the admin public key (npub format) |
| `getBunkerNpub()` | Returns the bunker's public key |
| `generateTestIdentity()` | Creates a random Nostr identity for testing |
| `waitFor(Duration)` | Blocking wait utility |

**Default Timeout:** 120 seconds

#### SharedContainers (Singleton)

Manages the lifecycle of shared Docker containers using the singleton pattern.

**Containers:**
- **NostrRelayContainer**: Uses `dockurr/strfry:latest` image on port 7777
- **NsecBunkerdContainer**: Uses `nsecbunkerd-local:latest` with SQLite database

**Configuration:**
- Network alias: `relay` on port 8080 (internal)
- Database: SQLite at `/app/config/nsecbunker.db`
- JVM shutdown hook for cleanup

---

### E2E Test Classes

#### 1. AdminConnectionLifecycleE2ETest

**Purpose:** Validates admin client connection lifecycle against real infrastructure.

| Test Method | Description |
|-------------|-------------|
| `shouldConnectToBunkerViaRelay()` | Establishes connection and verifies ping response |
| `shouldReconnectAfterDisconnect()` | Tests connection resilience after manual disconnect |
| `shouldHandleMultipleRelayConnections()` | Validates multi-relay support |
| `shouldMaintainConnectionForExtendedPeriod()` | Stability test with 5 sequential pings |
| `shouldHandleConcurrentConnections()` | Tests multiple simultaneous client connections |

**Timeout:** 120 seconds

---

#### 2. KeyCrudFlowE2ETest

**Purpose:** Tests complete key management CRUD operations.

| Test Method | Description |
|-------------|-------------|
| `shouldCreateNewKeyWithPassphrase()` | Creates a new key protected by passphrase |
| `shouldImportExistingKey()` | Imports an external nsec/npub pair |
| `shouldListAllKeys()` | Lists all keys stored in the bunker |
| `shouldUnlockKeyWithPassphrase()` | Unlocks a key for signing operations |
| `shouldGetKeyDetails()` | Retrieves metadata for a specific key |
| `shouldDeleteKey()` | Removes a key from the bunker |
| `shouldCompleteFullKeyLifecycle()` | Full 6-step lifecycle: create → list → get → unlock → get → delete |

**Timeout:** 90 seconds

---

#### 3. TokenFlowE2ETest

**Purpose:** Validates token creation, validation, and revocation workflows.

| Test Method | Description |
|-------------|-------------|
| `shouldCreateTokenWithoutExpiry()` | Creates a token with unlimited validity |
| `shouldCreateTokenWithExpiry()` | Creates a time-limited token (1 hour) |
| `shouldListTokensForKey()` | Lists all tokens associated with a key |
| `shouldGetTokenById()` | Retrieves token details by ID |
| `shouldRevokeToken()` | Revokes an active token |
| `shouldValidateToken()` | Validates token authenticity |
| `shouldCompleteFullTokenLifecycle()` | 6-step flow: create key → create token → list → get → revoke → validate (expect invalid) |

**Note:** nsecbunkerd requires a policyId for token creation.

**Timeout:** 120 seconds

---

#### 4. PolicyFlowE2ETest

**Purpose:** Tests policy CRUD operations and rule evaluation.

| Test Method | Description |
|-------------|-------------|
| `shouldCreateSimplePolicy()` | Creates a policy with a single `allow_method` rule |
| `shouldCreatePolicyWithMultipleRules()` | Creates a complex policy with 4+ rules |
| `shouldCreatePolicyWithEventKindRestrictions()` | Creates policies with event kind filtering (allow/deny specific kinds) |
| `shouldListAllPolicies()` | Lists all policies in the bunker |
| `shouldGetPolicyById()` | Retrieves policy details by ID |
| `shouldDeletePolicy()` | Removes a policy from the bunker |
| `shouldCompleteFullPolicyLifecycle()` | 5-step flow with rule validation |

**Rule Types:**
- Method-based: `allow_method`, `deny_method`
- Event-kind-based: `allow_kind`, `deny_kind`

**Timeout:** 120 seconds

---

#### 5. PermissionFlowE2ETest

**Purpose:** Validates permission grant and revocation workflows.

| Test Method | Description |
|-------------|-------------|
| `shouldGrantPermission()` | Grants a user access to a key with a specific policy |
| `shouldRevokePermission()` | Revokes a user's access to a key |
| `shouldListKeyUsers()` | Lists all users with access to a specific key |
| `shouldGetPermissions()` | Retrieves detailed permissions for a user |
| `shouldUpdateKeyUserDescription()` | Updates user metadata/description |
| `shouldCompleteFullPermissionLifecycle()` | 6-step flow: grant → list → get → update → revoke → verify |

**Key Entities:** Key → User → Policy mapping

**Timeout:** 120 seconds

---

#### 6. SigningFlowE2ETest

**Purpose:** Tests complete signing setup workflow from key creation to token generation.

| Test Method | Description |
|-------------|-------------|
| `shouldSetUpKeyForSigning()` | Creates and unlocks a key for signing |
| `shouldCreateSigningPolicy()` | Creates a policy with signing rules (`sign_event`, `get_public_key`; `allow_kind: 1,7`; `deny_kind: 4`) |
| `shouldGrantSigningPermission()` | Grants signing permission to a user |
| `shouldGenerateSigningToken()` | Generates a token and connection string |
| `shouldCompleteFullSigningSetupFlow()` | Complete 6-step signing setup |
| `shouldImportExistingKeyForSigning()` | Imports an external key for signing operations |

**Connection String Format:** `bunker://npub1...`

**Timeout:** 120 seconds

---

#### 7. BatchSigningE2ETest

**Purpose:** Tests batch operations and concurrent handling for high-throughput scenarios.

| Test Method | Description |
|-------------|-------------|
| `shouldCreateMultipleKeys()` | Creates 5+ keys sequentially |
| `shouldCreateKeysConcurrently()` | Creates 3 keys concurrently using CompletableFuture |
| `shouldAssignSamePolicyToMultipleKeys()` | Assigns a shared policy across multiple keys |
| `shouldGenerateMultipleTokensForBatchClient()` | Generates 5 tokens for the same key |
| `shouldHandleConcurrentTokenGeneration()` | Tests concurrent token generation requests |
| `shouldCompleteFullBatchSetupFlow()` | 5-step batch setup: 3 keys with 2 clients each |
| `shouldListKeysEfficiently()` | Performance test for list operations |

**Concurrency:** Uses `CompletableFuture` for async operations

**Timeout:** 120 seconds

---

## Integration Test Suite

**Location:** `nsecbunker-tests/nsecbunker-it/`

Integration tests validate individual components with mocked dependencies or lightweight containers.

### AdminConnectionLifecycleTest

**Purpose:** Tests admin client connection state machine with a mock relay server.

**Infrastructure:** MockRelayServer (okhttp3 WebSocket server)

| Category | Test Methods |
|----------|-------------|
| **Connect** | `connectEstablishesConnection()`, `connectIsIdempotentWhenConnected()`, `connectThrowsWhenClientClosed()`, `connectAsyncCompletesSuccessfully()`, `connectFailsWithInvalidRelay()` |
| **Disconnect** | `disconnectClosesConnection()`, `disconnectIsSafeWhenNotConnected()`, `disconnectCanBeCalledMultipleTimes()` |
| **Reconnect** | `canReconnectAfterDisconnect()`, `cannotReconnectAfterClose()` |
| **State** | `initialStateIsDisconnected()`, `stateTransitionsThroughLifecycle()` |
| **Close** | `closeDisconnectsAndCleansUp()`, `closeIsIdempotent()`, `closeCancelsPendingRequests()` |
| **Requests** | `sendRequestFailsWhenNotConnected()`, `sendRequestFailsWhenClientClosed()` |
| **Listeners** | `eventListenerReceivesResponses()`, `multipleEventListenersCanBeAdded()`, `eventListenerCanBeRemoved()` |
| **Config** | `connectWithCustomTimeout()`, `connectWithMultipleRelays()`, `connectWithEphemeralKey()` |
| **Server Events** | `handlesServerInitiatedDisconnect()` |

---

### AdminIntegrationTest

**Purpose:** Tests manager implementations (Key, Policy, Permission, Token) with mocked client.

**Infrastructure:** Mockito-based stubs with ArgumentCaptor

| Test Method | Description |
|-------------|-------------|
| `shouldPerformKeyCrudFlow()` | Create → List → Get → Delete with JSON serialization validation |
| `shouldPerformPolicyCrudFlow()` | Create → List → Get → Delete with policy object handling |
| `shouldGrantAndManagePermissions()` | Grant → List → Get → Update → Revoke flow |
| `shouldManageTokens()` | Create → List → Get → Revoke → Validate with duration handling |

---

### RelayContainerIntegrationTest

**Purpose:** Tests relay connection and Nostr protocol messaging with a real relay container.

**Infrastructure:** Docker container (`scsibug/nostr-rs-relay:latest`)

| Test Method | Description |
|-------------|-------------|
| `shouldPublishAndSubscribeThroughRelay()` | Full Nostr message flow: subscribe → EOSE → publish → OK |

**Protocol:** NIP-01 (Nostr protocol), NIP-04 (encryption)

**Enable:** Set `ENABLE_IT=true` environment variable

---

### SpringBootStarterIntegrationTest

**Purpose:** Tests Spring Boot auto-configuration.

**Infrastructure:** ApplicationContextRunner

| Test Method | Description |
|-------------|-------------|
| `shouldCreateBeans()` | Verifies `NsecBunkerAdminClient`, `NsecBunkerSigner`, and `HealthIndicator` beans are created |

**Configuration Properties:**
- `nsecbunker.admin.bunker-pubkey`, `admin-private-key`, `relays[]`
- `nsecbunker.signer.bunker-pubkey`, `client-private-key`, `relays[]`

---

### MonitoringIntegrationTest

**Purpose:** Tests monitoring and health check integration.

**Infrastructure:** Mockito-based stubs

| Test Method | Description |
|-------------|-------------|
| `shouldReportHealthUp()` | HealthChecker returns UP status on "pong" response |
| `shouldTriggerAlerts()` | AlertManager evaluates metrics against thresholds |

---

### SignerRequestExecutorIntegrationTest

**Purpose:** Tests signer request retry logic.

| Test Method | Description |
|-------------|-------------|
| `shouldRetryAndSucceed()` | Verifies transport retry on first failure (2 total attempts) |

---

### TestcontainersIntegrationSmokeTest

**Purpose:** Verifies Testcontainers setup and Docker availability.

| Test Method | Description |
|-------------|-------------|
| `shouldStartContainer()` | Starts Alpine container and verifies running state |

**Enable:** Set `ENABLE_IT=true` environment variable

---

## Chaos Test Suite

**Location:** `nsecbunker-tests/nsecbunker-chaos/`

Chaos tests validate system resilience under network failures and adverse conditions.

### RelayConnectionChaosTest

**Purpose:** Tests RelayConnection resilience under abrupt network failures.

**Infrastructure:** MockWebServer (okhttp3) with WebSocket support

| Test Method | Description |
|-------------|-------------|
| `shouldAttemptReconnectWhenServerDrops()` | Verifies reconnection logic after server shutdown (50ms delay, 3 max attempts) |
| `shouldStopAfterMaxReconnectAttempts()` | Verifies state transitions to `FAILED` after exhausting retries (max attempts = 1) |

**Utilities:** `forceClientSideCancel()` helper for controlled WebSocket shutdown

---

## Security Test Suite

**Location:** `nsecbunker-tests/nsecbunker-security/`

Security tests validate cryptographic implementations and input handling.

### Nip04CryptoSecurityTest

**Purpose:** Security validation for NIP-04 encryption/decryption.

| Category | Test Methods |
|----------|-------------|
| **Malformed Ciphertext** | `shouldRejectMalformedCiphertext()` (parametrized: empty, malformed base64, missing IV, short IV, invalid chars), `shouldHandleCiphertextWithExtraSeparators()`, `shouldHandleTruncatedCiphertext()` |
| **Invalid Keys** | `shouldRejectNullPrivateKey()`, `shouldRejectNullPublicKey()`, `shouldRejectKeysOfWrongLength()` (0, 1, 16, 31, 33, 64, 128 bytes), `shouldHandleAllZeroKeys()` |
| **IV Manipulation** | `shouldProduceDifferentOutputWithModifiedIv()`, `shouldHandleShortIv()` (8 bytes), `shouldHandleLongIv()` (32 bytes) |
| **Timing Attack Resistance** | `shouldHaveConsistentTimingForInvalidCiphertexts()` |
| **Edge Cases** | `shouldHandleEmptyPlaintextEncryption()`, `shouldHandleVeryLongPlaintext()` (100k chars), `shouldHandleBinaryDataInPlaintext()` (all byte values), `shouldHandleUnicodePlaintext()` (Chinese, Arabic, emoji) |
| **Null Handling** | `shouldRejectNullCiphertext()`, `shouldRejectNullPlaintext()` |

---

## Running Tests

### E2E Tests

```bash
# Run all E2E tests
mvn test -pl nsecbunker-tests/nsecbunker-e2e

# Run specific E2E test class
mvn test -pl nsecbunker-tests/nsecbunker-e2e -Dtest=SigningFlowE2ETest
```

### Integration Tests

```bash
# Run all IT tests (requires ENABLE_IT=true for container tests)
ENABLE_IT=true mvn test -pl nsecbunker-tests/nsecbunker-it

# Run specific IT test
mvn test -pl nsecbunker-tests/nsecbunker-it -Dtest=AdminIntegrationTest
```

### Chaos Tests

```bash
mvn test -pl nsecbunker-tests/nsecbunker-chaos
```

### Security Tests

```bash
mvn test -pl nsecbunker-tests/nsecbunker-security
```

### All Tests

```bash
mvn verify
```

---

## Test Coverage Summary

| Suite | Test Classes | Test Methods | Focus |
|-------|-------------|--------------|-------|
| E2E | 7 | 43 | Complete workflows with real containers |
| Integration | 7 | 23+ | Component integration with mocks/containers |
| Chaos | 1 | 2 | Network resilience |
| Security | 1 | 18+ | Cryptographic validation |
| **Total** | **16** | **90+** | |

---

## Infrastructure Components

| Component | Image/Technology | Purpose |
|-----------|------------------|---------|
| NostrRelayContainer | `dockurr/strfry:latest` | Nostr relay for WebSocket messaging |
| NsecBunkerdContainer | `nsecbunkerd-local:latest` | nsecBunker daemon |
| MockRelayServer | okhttp3 MockWebServer | Lightweight mock for unit/integration tests |
| ApplicationContextRunner | Spring Boot Test | Spring auto-configuration testing |

---

## Test Data Generation

- **Identities:** `Identity.generateRandomIdentity()` for Nostr keypairs
- **Unique Names:** `UUID.randomUUID()` for test isolation
- **Passphrases:** Hardcoded test values (`"test-passphrase"`)
- **Timeouts:** 90-120 seconds for E2E, 5-10 seconds for IT

---

## Assertion Framework

- **AssertJ:** Fluent assertions for readability
- **Awaitility:** Async condition waiting with configurable timeouts
- **JUnit 5:** Test lifecycle and parametrized tests
- **Mockito:** Mocking and argument capture
