# API Reference

Reference overview for core nsecBunker Java APIs.

## Admin Client
- `NsecBunkerAdminClient`
  - Connect: `connect()`, `connectAsync()`, `disconnect()`, `close()`
  - Requests: `sendRequest(Nip46Request)`
  - Managers: `keyManager()`, `policyManager()`, `permissionManager()`, `tokenManager()`
- Config: `AdminConfig` (pubkey, admin key, relays, secret, timeouts, reconnection)
- Auth: `AdminAuthenticator` (validate admin access, ping)
- Exceptions: `AdminException`

## Admin Managers
- `KeyManager` / `DefaultKeyManager`
  - `createKey(name, passphrase)` / `createKey(name, nsec, passphrase)`
  - `listKeys()`, `unlockKey()`, `deleteKey()`, `getKeyDetails()`, `rotateKey()`
- `PolicyManager` / `DefaultPolicyManager`, `PolicyBuilder`, `PolicyTemplates`
  - CRUD policies; allow/deny rules; usage limits
- `PermissionManager` / `DefaultPermissionManager`
  - `grantPermission`, `revokePermission`, `listKeyUsers`, `getPermissions`, `updateKeyUserDescription`
- `TokenManager` / `DefaultTokenManager`
  - `createToken`, `listTokens`, `getToken`, `revokeToken`, `validateToken`

## Client Signer
- `NsecBunkerSigner` implements `RemoteSigner`
  - Connection: `connect()`, `disconnect()`, `isConnected()`, `getState()`
  - Permissions: `requestPermissions(methods)`
  - Signing/crypto: `signEvent`, `getPublicKey`, `nip04Encrypt/Decrypt`, `nip44Encrypt/Decrypt`, `ping`
  - Batch: `BatchSigner`, `DefaultBatchSigner`
  - Request handling: `RequestQueue`, `RequestExecutor` (timeouts, retries)
  - Adapter: `NostrJavaSignerAdapter` (nostr-java `ISignable`)
  - Profile: `ProfileManager`, `DefaultProfileManager`

## Account & Identity
- Account: `AccountManager` / `DefaultAccountManager`
  - `createAccount`, `registerAccount`, `generateKeyForAccount`
- NIP-05: `Nip05Manager` / `DefaultNip05Manager`
  - `setupNip05`, `verifyNip05`, `generateWellKnown`
- Wallet: `WalletManager` / `LNBitsWalletManager` (stub)

## Spring Boot Starter
- Config: `NsecBunkerProperties`
- Auto-config: `NsecBunkerAutoConfiguration`
- Actuator: `BunkerHealthIndicator`, `BunkerInfoContributor`

## Models (Core)
- `BunkerConnection`, `BunkerKey`, `BunkerPolicy`, `PolicyRule`, `KeyUser`, `AccessToken`, `SigningCondition`
- Exceptions: `BunkerException` hierarchy

## Protocol (NIP-46)
- `Nip46Request`, `Nip46Response`, `Nip46Method`, `Nip46Encoder`, `Nip46Decoder`, `PendingRequestManager`

## Logging & Monitoring (planned)
- Logging/metrics/health/alerting modules are reserved for Phase 4.
