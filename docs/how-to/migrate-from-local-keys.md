# Migrate From Local Keys

This guide shows how to move existing locally stored keys into nsecBunker using the admin API.

## Prerequisites
- Admin nsec for the bunker
- Relay URL reachable from your network
- Passphrase to encrypt imported keys

## Steps
1) Configure the admin client:
```java
var client = NsecBunkerAdminClient.builder()
    .bunkerPubkey("npub1...")
    .adminPrivateKey("nsec1...")
    .relays(List.of("wss://relay.example.com"))
    .build();
client.connect();
```
2) Import each local key:
```java
client.keyManager()
    .createKey("local-key-name", "nsec1local...", "passphrase")
    .join();
```
3) Grant access to users or tokens:
```java
var policy = PolicyTemplates.fullAccess("import-policy");
client.permissionManager()
    .grantPermission("local-key-name", "npub1user...", policy)
    .join();
```
4) Verify and retire the local key copy.

## Tips
- Use unique key names per environment (e.g., `app-prod`, `app-dev`).
- Rotate passphrases after import and remove plaintext nsec copies.
