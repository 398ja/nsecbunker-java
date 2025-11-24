# nsecBunker Java Library

Java client library for interacting with [nsecBunker](https://github.com/kind-0/nsecbunkerd) - a self-hosted remote key signing service implementing NIP-46 (Nostr Connect).

## What is nsecBunker?

nsecBunker is a self-hosted daemon that provides decentralized key delegation for the Nostr protocol. It enables:
- Remote event signing without exposing private keys
- Granular permission controls (policies, time limits, event kinds)
- Multi-user key sharing with individual permissions
- Token-based onboarding for new clients

## Features

This library provides:
- **Admin Operations**: Create keys, manage policies, grant/revoke permissions, create tokens
- **Remote Signing**: Sign events remotely via NIP-46 protocol
- **Connection Management**: Relay pool, automatic reconnection, health checks
- **Monitoring**: Logs, metrics, health indicators
- **Spring Boot Integration**: Auto-configuration and actuator support

## Modules

- `nsecbunker-core`: Core models and interfaces
- `nsecbunker-connection`: Relay connection management
- `nsecbunker-protocol`: NIP-46 protocol implementation
- `nsecbunker-admin`: Admin client for key/permission management
- `nsecbunker-client`: Remote signer client
- `nsecbunker-monitoring`: Monitoring and metrics
- `nsecbunker-account`: Account management and NIP-05
- `nsecbunker-spring-boot-starter`: Spring Boot auto-configuration

## Requirements

- Java 17 or higher
- Maven 3.8+
- Running nsecBunker instance

## Quick Start

### Maven Dependency

```xml
<dependency>
    <groupId>com.github.cashutools</groupId>
    <artifactId>nsecbunker-client</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

### Admin Client Example

```java
// Create admin client
NsecBunkerAdmin admin = NsecBunkerAdmin.builder()
    .adminNsec(System.getenv("ADMIN_NSEC"))
    .connectionString("bunker://npub@relay.nsecbunker.com")
    .relays("wss://relay.nsecbunker.com")
    .build();

admin.connect();

// Create a key
BunkerKey key = admin.keys()
    .create("my-app-key")
    .withNsec("nsec1...")
    .withPassphrase("secure-passphrase")
    .execute()
    .get();

// Create a policy
BunkerPolicy policy = admin.policies()
    .create("app-policy")
    .allowEventKind(1)
    .allowMethod("encrypt")
    .allowMethod("decrypt")
    .execute()
    .get();

// Create access token
AccessToken token = admin.tokens()
    .create("my-app-key", "mobile-client")
    .withPolicy(policy.getId())
    .expiresIn(Duration.ofDays(30))
    .execute()
    .get();

System.out.println("Token: " + token.getFullToken());
```

### Remote Signer Example

```java
// Connect with token
NsecBunkerSigner signer = NsecBunkerSigner.builder()
    .connectionString("bunker://npub@relay#token123")
    .relays("wss://relay.nsecbunker.com")
    .build();

signer.connect().get();

// Sign an event
Event event = Event.builder()
    .kind(1)
    .content("Hello from nsecBunker!")
    .build();

Event signed = signer.signEvent(event).get();
System.out.println("Signed: " + signed.getId());
```

## Building

```bash
# Build all modules
mvn clean install

# Run tests
mvn test

# Run with code quality checks
mvn clean install -P quality

# Skip tests
mvn clean install -DskipTests
```

## Documentation

- [API Documentation](docs/api/)
- [User Guides](docs/guides/)
- [Examples](examples/)

## Project Status

🚧 **In Development** - This library is currently under active development.

### Completed Tasks

- [x] Multi-module Maven project structure

### Current Phase

Phase 1: Core Infrastructure

## Contributing

Contributions are welcome! Please read our contributing guidelines first.

## License

MIT License - see [LICENSE](LICENSE) file for details.

## Related Projects

- [nsecbunkerd](https://github.com/kind-0/nsecbunkerd) - The TypeScript nsecBunker daemon
- [cashu-client](https://github.com/cashutools/cashu-client) - Cashu ecash client with identity plugin
- [nostr-java](https://github.com/tcheeric/nostr-java) - Java Nostr protocol implementation

## Support

- GitHub Issues: Report bugs or request features
- Documentation: Check the docs/ directory
- Examples: See examples/ directory for usage examples
