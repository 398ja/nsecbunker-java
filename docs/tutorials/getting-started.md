# Getting Started with nsecbunker-java

This tutorial shows how to pull the library into a project, connect as an admin, and list keys using the simplest setup.

## Prerequisites
- Java 21
- Maven 3.9+
- At least one relay URL to reach your bunker

## 1) Add the dependency
```xml
<dependency>
  <groupId>xyz.tcheeric</groupId>
  <artifactId>nsecbunker-admin</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## 2) Configure credentials
- `bunkerPubkey`: npub or hex of your bunker
- `adminPrivateKey`: nsec or hex of an authorized admin
- `relays`: e.g., `wss://relay.example.com`

## 3) Connect and list keys
```java
var client = NsecBunkerAdminClient.builder()
    .bunkerPubkey("npub1...")
    .adminPrivateKey("nsec1...")
    .relay("wss://relay.example.com")
    .build();

client.connect();
var keys = client.keyManager().listKeys().join();
client.close();
```

## 4) Next steps
- Create keys: `client.keyManager().createKey("my-key", "pass").join();`
- Explore signing: see [Remote Signing](../how-to/remote-signing.md)
- For Spring apps: see [Spring Boot Integration](../how-to/spring-boot.md)
