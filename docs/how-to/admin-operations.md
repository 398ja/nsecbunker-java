# Admin Operations How-To

Use this guide to perform common admin tasks: create keys, unlock them, and manage permissions.

## Create and unlock a key
```java
var client = NsecBunkerAdminClient.builder()
    .bunkerPubkey("npub1...")
    .adminPrivateKey("nsec1...")
    .relay("wss://relay.example.com")
    .build();
client.connect();

var key = client.keyManager().createKey("alice-key", "passphrase").join();
client.keyManager().unlockKey(key.getName(), "passphrase").join();
```

## Manage permissions
```java
var policy = PolicyTemplates.fullAccess("allow-all");
var permission = client.permissionManager()
    .grantPermission("alice-key", "npub1user...", policy)
    .join();
client.permissionManager().revokePermission("alice-key", permission.getNpub()).join();
```

## List and inspect
```java
var keys = client.keyManager().listKeys().join();
var details = client.keyManager().getKeyDetails("alice-key").join();
var users = client.permissionManager().listKeyUsers("alice-key").join();
```

## Tokens for clients
```java
var token = client.tokenManager()
    .createToken("alice-key", "mobile-client", policy.getId(), Duration.ofHours(1))
    .join();
client.tokenManager().revokeToken(token.getId()).join();
```

## Tips
- Always close the client when done: `client.close();`
- Use policies to restrict methods/kinds; see [Remote Signing](remote-signing.md) for method names.
