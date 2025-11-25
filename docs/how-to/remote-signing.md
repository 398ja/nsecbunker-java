# Remote Signing How-To

This guide shows how to use `NsecBunkerSigner` to connect, request permissions, and sign events.

## Configure the signer
```java
var signer = new NsecBunkerSigner(SignerConfig.builder()
    .bunkerPubkey("npub1...")
    .clientPrivateKey("nsec1...") // client identity
    .relays(List.of("wss://relay.example.com"))
    .secret("optional-secret")    // or token
    .useEphemeralKey(true)
    .build(),
    request -> transport.send(request), // supply your transport
    null,
    null
);
```

## Connect and request permissions
```java
signer.connect().join();
signer.requestPermissions(List.of("sign_event", "nip04_encrypt")).join();
```

## Sign and encrypt
```java
String signature = signer.signEvent(eventJson).join();
String pubkey = signer.getPublicKey().join();
String ciphertext = signer.nip04Encrypt(pubkey, "hello").join();
String plaintext = signer.nip04Decrypt(pubkey, ciphertext).join();
```

## Batch signing
```java
var batch = new DefaultBatchSigner(signer);
var results = batch.signEvents(List.of(eventJson1, eventJson2)).join();
```

## Connection health
```java
String pong = signer.ping().join();
signer.disconnect().join();
```

## Notes
- `NsecBunkerSigner` expects a request handler to send/receive NIP-46 messages over your transport.
- Handle `SignerException` for failures and `AUTH_URL_REQUIRED` state when the bunker demands an authorization URL.
