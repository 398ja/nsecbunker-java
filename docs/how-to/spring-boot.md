# Spring Boot Integration How-To

Use the starter to auto-configure admin and signer beans in a Spring Boot app.

## Dependency
```xml
<dependency>
  <groupId>xyz.tcheeric</groupId>
  <artifactId>nsecbunker-spring-boot-starter</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## Properties
```properties
nsecbunker.admin.bunker-pubkey=npub1...
nsecbunker.admin.admin-private-key=nsec1...
nsecbunker.admin.relays[0]=wss://relay.example.com

nsecbunker.signer.bunker-pubkey=npub1...
nsecbunker.signer.client-private-key=nsec1...
nsecbunker.signer.relays[0]=wss://relay.example.com
```

## Using the beans
```java
@RestController
class KeyController {
  private final NsecBunkerAdminClient admin;
  private final NsecBunkerSigner signer;

  KeyController(NsecBunkerAdminClient admin, NsecBunkerSigner signer) {
    this.admin = admin;
    this.signer = signer;
  }

  @GetMapping("/keys")
  List<BunkerKey> keys() {
    admin.connect();
    return admin.keyManager().listKeys().join();
  }
}
```

## Actuator
- Health: `/actuator/health` includes bunker status when Actuator is on the classpath.
- Info: `/actuator/info` lists bunker pubkey and relays.

## Tips
- Set `useEphemeralKey=true` for clients in untrusted environments.
- Keep secrets out of logs; use externalized configuration for keys and tokens.
