# nsecBunker Token Usage Guide

## How Tokens Work

**Important**: Tokens in nsecBunker are **single-use invitation codes**, not reusable API keys.

## Token Lifecycle

```
1. Admin creates token → Token is active
2. User redeems token → Permissions granted to user's pubkey
3. Token marked as redeemed → Token cannot be used again
```

### Token Format

```
npub1abc...xyz#a1b2c3d4...token
```

- First part: The npub of the key in the bunker
- Second part: The secret token string

## Token Redemption Flow

When a client connects with a token:

```typescript
// 1. Client connects with token
bunker://npub1abc@relay#token123

// 2. nsecBunker receives connection request
// 3. applyToken() is called with:
//    - userPubkey: The connecting client's pubkey
//    - token: The token string

// 4. Token validation checks:
validateToken(token) {
    - Token exists in database?
    - Token NOT already redeemed?
    - Token NOT expired?
    - Policy exists?
}

// 5. If valid, create KeyUser:
KeyUser {
    keyName: "cashu-alice-wallet"
    userPubkey: "hex-pubkey-of-client"
    description: "cashu-client" (from token's clientName)
}

// 6. Apply all policy rules as SigningConditions
For each rule in policy:
    SigningCondition {
        keyUserId: <new-key-user-id>
        method: rule.method
        kind: rule.kind
        allowed: true
    }

// 7. Mark token as redeemed
Token {
    redeemedAt: new Date()
    keyUserId: <key-user-id>
}
```

## Key Characteristics

### ✅ Single-Use Only

```java
// First redemption - SUCCESS
client1.connect("bunker://npub@relay#token123");
// Creates KeyUser for client1's pubkey

// Second redemption - FAILS
client2.connect("bunker://npub@relay#token123");
// Error: "Token already redeemed"
```

### ✅ Binds Token to Specific Pubkey

Once redeemed, the token is permanently associated with the pubkey that redeemed it:

```typescript
// Token redeemed by pubkey A
token.keyUserId = <KeyUser with pubkey A>
token.redeemedAt = 2025-11-24T12:00:00Z

// This binding is permanent
```

### ✅ Grants Permissions to Pubkey

After redemption, the client's pubkey has the permissions defined in the policy:

```java
// Policy allows: sign_event (kind 1, 4), encrypt, decrypt

// After redemption, this pubkey can:
client.signEvent(kind: 1)  // ✓ Allowed
client.signEvent(kind: 4)  // ✓ Allowed
client.encrypt(message)    // ✓ Allowed
client.decrypt(cipher)     // ✓ Allowed
client.signEvent(kind: 7)  // ✗ Denied (not in policy)
```

## Use Cases

### Use Case 1: Onboarding New Clients

```java
// Admin creates token for new client
AccessToken token = admin.tokens()
    .create("cashu-alice-wallet", "mobile-client")
    .withPolicy(mobilePolicy)
    .expiresIn(Duration.ofHours(24))
    .execute()
    .get();

// Send token to user via secure channel
String connectionString = token.getFullToken();
sendToUser(connectionString); // bunker://npub#token

// User's mobile app redeems token
RemoteSigner signer = RemoteSigner.builder()
    .connectionString(connectionString)
    .build();

signer.connect().get(); // Token is now redeemed
// Mobile app's pubkey is now authorized
```

### Use Case 2: Time-Limited Access

```java
// Create 1-hour access token
AccessToken token = admin.tokens()
    .create("cashu-temp-key", "temporary-access")
    .withPolicy(readOnlyPolicy)
    .expiresIn(Duration.ofHours(1))
    .execute()
    .get();

// After 1 hour, redemption fails
// Error: "Token expired"
```

### Use Case 3: Multiple Clients Need Access

**Problem**: Token is single-use, but you have 3 clients.

**Solution**: Create 3 tokens!

```java
// Create separate tokens for each client
for (String clientId : List.of("client-1", "client-2", "client-3")) {
    AccessToken token = admin.tokens()
        .create("shared-key", clientId)
        .withPolicy(sharedPolicy)
        .execute()
        .get();

    sendToClient(clientId, token.getFullToken());
}

// Each client redeems their own token
// All 3 clients now have access to the same key
```

## Token vs Direct Permission Grant

### Token Approach (Self-Service)

```java
// 1. Admin pre-creates token
token = admin.tokens().create(keyName, clientName, policy).execute();

// 2. User redeems token themselves
client.connect(token); // No admin intervention needed

// Pros:
// - User can onboard themselves
// - No admin manual approval needed
// - Good for public/open access scenarios
// - Token can be distributed via URL, QR code, etc.
```

### Direct Grant Approach (Admin-Controlled)

```java
// 1. User requests access (out-of-band)
// 2. Admin grants permission directly
admin.permissions()
    .grant(keyName, userPubkey)
    .withPolicy(policy)
    .execute();

// 3. User connects (no token needed)
client.connect("bunker://npub@relay");
// Admin approves the connection request

// Pros:
// - Admin has full control
// - Can verify user identity first
// - No risk of token leaking
// - Good for trusted/private scenarios
```

## Token Security

### Best Practices

**1. Use Short Expiration Times**
```java
// For sensitive operations
.expiresIn(Duration.ofMinutes(15))

// For user onboarding
.expiresIn(Duration.ofHours(24))

// For permanent access (be careful!)
.expiresIn(null) // Never expires
```

**2. Use Descriptive Client Names**
```java
// Good
.create(keyName, "alice-mobile-app-ios")
.create(keyName, "bob-desktop-linux")

// Bad
.create(keyName, "client1")
.create(keyName, "user")
```

**3. Secure Token Distribution**
```java
// ✓ Good: Encrypted channel
sendViaSignal(token);
sendViaEncryptedEmail(token);
showQRCodeInPerson(token);

// ✗ Bad: Public channels
tweetToken(token);
postOnPublicForum(token);
```

**4. Monitor Token Redemption**
```java
// Check if token was redeemed
List<AccessToken> tokens = admin.tokens()
    .forKey(keyName)
    .get();

for (AccessToken token : tokens) {
    if (token.getRedeemedAt() != null) {
        System.out.println("Redeemed by: " + token.getRedeemedByPubkey());
        System.out.println("Redeemed at: " + token.getRedeemedAt());
    }
}
```

### Token Revocation

You cannot "un-redeem" a token, but you can revoke the user's access:

```java
// After token redemption, revoke the user
admin.permissions()
    .revoke(keyName, userPubkey)
    .execute();

// The user can no longer use the key
// Even though they redeemed the token
```

## Comparison with Other Systems

### nsecBunker Tokens vs OAuth Access Tokens

| Feature | nsecBunker Token | OAuth Access Token |
|---------|------------------|-------------------|
| **Usage** | Single-use | Multi-use |
| **Purpose** | Grant initial access | Authenticate requests |
| **Lifespan** | Until redeemed or expired | Until expired |
| **Renewal** | Not renewable | Can be refreshed |
| **Scope** | Policy-based (full RBAC) | Scope-based (limited) |

### nsecBunker Tokens vs Invitation Codes

nsecBunker tokens are more like invitation codes:

```
Invitation Code:
- One-time use ✓
- Grants specific privileges ✓
- Can expire ✓
- Binds to user ✓

API Key:
- Multi-use ✗
- Acts as password ✗
- Represents user identity ✗
```

## Token Metadata

Tokens store useful metadata:

```typescript
Token {
    id: 1
    keyName: "cashu-alice-wallet"
    token: "a1b2c3d4..."  // Secret token string
    clientName: "mobile-app"  // Description
    createdBy: "admin-pubkey-hex"  // Who created it
    createdAt: 2025-11-24T10:00:00Z
    expiresAt: 2025-11-25T10:00:00Z  // Expiration
    redeemedAt: 2025-11-24T12:00:00Z  // When redeemed (null if not)
    keyUserId: 5  // Which KeyUser redeemed it
    policyId: 2  // What policy was applied
}
```

## Common Patterns

### Pattern 1: Multi-Device Access

```java
// User has 3 devices, all need access to same key
String[] devices = {"phone", "tablet", "laptop"};

for (String device : devices) {
    AccessToken token = createToken(
        "alice-wallet",
        "alice-" + device,
        sharedPolicy
    );

    sendToDevice(device, token);
}

// Result: 3 KeyUsers, all with same permissions
```

### Pattern 2: Role-Based Token Distribution

```java
// Different roles get different tokens with different policies
Map<String, Policy> roles = Map.of(
    "admin", fullAccessPolicy,
    "editor", editPolicy,
    "viewer", readOnlyPolicy
);

for (var entry : roles.entrySet()) {
    String role = entry.getKey();
    Policy policy = entry.getValue();

    AccessToken token = createToken(
        keyName,
        role + "-client",
        policy
    );

    distributeToRole(role, token);
}
```

### Pattern 3: Temporary Access with Auto-Expiration

```java
// Grant 1-hour temporary access
AccessToken token = admin.tokens()
    .create("production-key", "temp-debug-access")
    .withPolicy(debugPolicy)
    .expiresIn(Duration.ofHours(1))
    .execute()
    .get();

// After 1 hour, token cannot be redeemed
// If already redeemed, user keeps access until explicitly revoked
```

## FAQs

### Q: Can I reuse a token?
**A:** No, tokens are single-use only.

### Q: Can multiple users share one token?
**A:** No, whoever redeems it first gets the access. Others get "already redeemed" error.

### Q: What if my token leaks?
**A:** Revoke it immediately:
```java
admin.tokens().revoke(tokenId).execute();
```

### Q: Can I see who redeemed a token?
**A:** Yes:
```java
AccessToken token = admin.tokens().get(tokenId).get();
if (token.getRedeemedAt() != null) {
    KeyUser user = token.getKeyUser();
    System.out.println("Redeemed by: " + user.getUserPubkey());
}
```

### Q: Can I create a multi-use token?
**A:** No, but you can:
1. Create multiple tokens with the same policy
2. Use direct permission grants instead
3. Create a new token for each use

### Q: Does the token grant permanent access?
**A:** The token redemption grants permanent access (until revoked). The token itself can expire, preventing redemption, but once redeemed the access persists.

### Q: How do I remove access granted via token?
**A:** Revoke the user's permissions:
```java
admin.permissions().revoke(keyName, userPubkey).execute();
```

## Implementation in nsecbunker-java

The Java library will provide:

```java
// Token creation
public interface TokenManager {
    CompletableFuture<AccessToken> createToken(
        String keyName,
        String clientName,
        int policyId,
        Duration expiresIn
    );

    CompletableFuture<List<AccessToken>> listTokens(String keyName);

    CompletableFuture<AccessToken> getToken(int tokenId);

    CompletableFuture<Boolean> revokeToken(int tokenId);

    CompletableFuture<Boolean> validateToken(String token);
}

// Token redemption (automatic during connection)
RemoteSigner signer = RemoteSigner.builder()
    .connectionString("bunker://npub@relay#token")
    .build();

// Connect automatically redeems the token
signer.connect().get();
```

## Conclusion

**nsecBunker tokens are invitation codes, not API keys:**

- ✅ Single-use
- ✅ Grant permanent permissions (until revoked)
- ✅ Bind to specific pubkey
- ✅ Apply policy rules as signing conditions
- ✅ Can expire before redemption
- ✅ Cannot be reused or shared

For reusable access, use **direct permission grants** instead of tokens.
