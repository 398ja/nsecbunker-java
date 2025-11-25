# Migrate From Other Signers

This guide helps move from another remote signer to nsecBunker using nsecBunker-admin and -client.

## Prerequisites
- Access to the current signer to export npub/nsec
- Bunker admin credentials and relay URL
- Target users and permissions you want to preserve

## Steps
1) Export keys from the old signer to nsec:
   - Ensure each key has a unique label to reuse in nsecBunker.
2) Import into nsecBunker:
```java
client.keyManager()
    .createKey("old-signer-key", "nsec1old...", "passphrase")
    .join();
```
3) Recreate policies:
```java
var policy = PolicyBuilder.withName("old-policy")
    .allowMethod("sign_event")
    .build();
client.policyManager().createPolicy(policy).join();
```
4) Re-grant permissions and tokens as needed:
```java
client.permissionManager()
    .grantPermission("old-signer-key", "npub1user...", policy)
    .join();
```
5) Update clients to point at the bunker connection string or the new relay set.

## Tips
- Keep both signers running briefly while you validate signatures from nsecBunker.
- Use short-lived tokens during migration; revoke old signer access once validated.
