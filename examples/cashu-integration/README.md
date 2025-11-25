# Cashu Integration Example

This example demonstrates how to integrate nsecBunker with a Cashu (ecash) client application.

## Overview

Cashu is a Chaumian ecash system that can use Nostr for:
- Token transmission (NIP-60)
- User authentication
- Encrypted messaging between wallets

This example shows how to use nsecBunker for secure remote signing in these scenarios.

## Use Cases

### 1. Signing Cashu Token Events (kind 7375)
```java
// Sign a cashu token event for sending tokens
String tokenEventJson = """
    {
        "kind": 7375,
        "created_at": 1234567890,
        "tags": [["p", "recipient-pubkey"]],
        "content": "encrypted-token-data"
    }
    """;
String signedEvent = signer.signEvent(tokenEventJson).join();
```

### 2. Encrypting Token Data (NIP-44)
```java
// Encrypt cashu tokens for the recipient
String encryptedTokens = signer.nip44Encrypt(recipientPubkey, tokenJson).join();
```

### 3. Decrypting Received Tokens
```java
// Decrypt tokens received from another user
String tokenJson = signer.nip44Decrypt(senderPubkey, encryptedContent).join();
```

### 4. Mint Authentication
```java
// Sign auth challenge for mint that requires Nostr identity
String signedAuth = signer.signEvent(authChallengeJson).join();
```

## Setup

1. Set environment variables:
```bash
export BUNKER_PUBKEY="your-bunker-pubkey"
export CLIENT_PRIVKEY="your-client-private-key"
export RELAY_URL="wss://your-relay.example.com"
```

2. Run the example:
```bash
cd examples/cashu-integration
# Requires proper transport implementation for production use
```

## Security Considerations

- **Key Isolation**: Your cashu wallet keys never leave the bunker
- **Approval Flow**: Each signing request can require user approval
- **Audit Trail**: All operations are logged by the bunker
- **Rate Limiting**: Configure request limits in the bunker

## Related NIPs

- [NIP-60](https://github.com/nostr-protocol/nips/blob/master/60.md) - Cashu Wallet
- [NIP-44](https://github.com/nostr-protocol/nips/blob/master/44.md) - Versioned Encryption
- [NIP-46](https://github.com/nostr-protocol/nips/blob/master/46.md) - Nostr Connect (nsecBunker)
