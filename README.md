# nsecbunker-java

[![CI](https://github.com/tcheeric/nsecbunker-java/actions/workflows/ci.yml/badge.svg)](https://github.com/tcheeric/nsecbunker-java/actions/workflows/ci.yml)
[![Maven Central](https://img.shields.io/maven-central/v/xyz.tcheeric/nsecbunker-java.svg)](https://search.maven.org/search?q=g:xyz.tcheeric%20AND%20a:nsecbunker-java)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java Version](https://img.shields.io/badge/Java-17%2B-blue)](https://openjdk.org/)

A comprehensive Java client library for interacting with [nsecBunker](https://github.com/kind-0/nsecbunkerd) instances. Provides high-level APIs for key management, remote signing (NIP-46), permission control, and monitoring.

## What is nsecBunker?

nsecBunker is a self-hosted daemon that provides decentralized key delegation for the Nostr protocol. It enables:
- Remote event signing without exposing private keys
- Granular permission controls (policies, time limits, event kinds)
- Multi-user key sharing with individual permissions
- Token-based onboarding for new clients

## Features

- **Remote Signing (NIP-46)** - Sign Nostr events remotely without exposing private keys
- **Admin Operations** - Key management, policy creation, permission control
- **Token Management** - Create and manage access tokens with fine-grained permissions
- **Monitoring & Audit** - Logging, metrics, and health checks
- **Spring Boot Integration** - Auto-configuration for Spring Boot applications
- **Async-First Design** - Non-blocking operations with CompletableFuture

## Requirements

- Java 17 or higher
- Maven 3.8+ or Gradle 7+
- Running nsecBunker instance

## Installation

### Maven

```xml
<dependency>
    <groupId>xyz.tcheeric</groupId>
    <artifactId>nsecbunker-client</artifactId>
    <version>0.1.0</version>
</dependency>
```

For admin operations:

```xml
<dependency>
    <groupId>xyz.tcheeric</groupId>
    <artifactId>nsecbunker-admin</artifactId>
    <version>0.1.0</version>
</dependency>
```

### Gradle

```groovy
implementation 'xyz.tcheeric:nsecbunker-client:0.1.0'
```

## Quick Start

### Remote Signing

```java
// Create a remote signer
RemoteSigner signer = RemoteSigner.builder()
    .connectionString("bunker://npub1...@wss://relay.nsecbunker.com")
    .timeout(Duration.ofSeconds(30))
    .build();

// Connect to the bunker
signer.connect().get();

// Sign an event
GenericEvent event = new GenericEvent(signer.getPublicKey(), 1, "Hello Nostr!");
signer.signEvent(event).get();

// Encrypt a message (NIP-04)
String encrypted = signer.encrypt(recipientPubkey, "Secret message").get();
```

### Admin Operations

```java
// Create admin client
NsecBunkerAdmin admin = NsecBunkerAdmin.builder()
    .adminNsec(System.getenv("ADMIN_NSEC"))
    .connectionString("bunker://npub1...@wss://relay.nsecbunker.com")
    .build();

admin.connect();

// Create a key
BunkerKey key = admin.keys()
    .create("my-wallet-key")
    .withPassphrase("secure-passphrase")
    .execute()
    .get();

// Create a policy
BunkerPolicy policy = admin.policies()
    .create("wallet-policy")
    .allowEventKind(1)      // Text notes
    .allowEventKind(4)      // DMs
    .allowMethod("encrypt")
    .allowMethod("decrypt")
    .execute()
    .get();

// Grant permissions
admin.permissions()
    .grant(key.getName(), clientPubkey)
    .withPolicy(policy)
    .execute()
    .get();

// Create access token
AccessToken token = admin.tokens()
    .create(key.getName(), "mobile-client")
    .withPolicy(policy.getId())
    .expiresIn(Duration.ofDays(30))
    .execute()
    .get();

System.out.println("Token: " + token.getFullToken());
```

### Spring Boot Integration

```java
@SpringBootApplication
@EnableNsecBunker
public class MyApplication {

    @Autowired
    private RemoteSigner signer;

    public String signNote(String content) throws Exception {
        GenericEvent event = new GenericEvent(signer.getPublicKey(), 1, content);
        signer.signEvent(event).get();
        return event.getId();
    }
}
```

Application properties:

```yaml
nsecbunker:
  connection-string: bunker://npub1...@wss://relay.nsecbunker.com
  timeout: 30s
  auto-connect: true
```

## Modules

| Module | Description |
|--------|-------------|
| `nsecbunker-core` | Core models, interfaces, and utilities |
| `nsecbunker-connection` | Relay connection management |
| `nsecbunker-protocol` | NIP-46 protocol implementation |
| `nsecbunker-client` | Remote signer client |
| `nsecbunker-admin` | Admin operations (key, policy, permission management) |
| `nsecbunker-monitoring` | Logging, metrics, and health checks |
| `nsecbunker-account` | Account management and NIP-05 setup |
| `nsecbunker-spring-boot-starter` | Spring Boot auto-configuration |

## Building from Source

```bash
# Clone the repository
git clone https://github.com/tcheeric/nsecbunker-java.git
cd nsecbunker-java

# Build with Maven
mvn clean install

# Run tests
mvn test

# Run with quality checks
mvn verify -Pquality

# Skip tests
mvn clean install -DskipTests
```

## Project Status

This library is under active development.

### Phase 1: Foundation (Current)
- [x] Multi-module Maven project structure
- [x] Dependency management setup
- [x] Build plugins configuration
- [x] Code quality tools (Checkstyle, SpotBugs, PMD)
- [x] CI/CD with GitHub Actions
- [x] Documentation structure

## Contributing

Contributions are welcome! Please read our [Contributing Guide](CONTRIBUTING.md) for details on:

- Code of conduct
- Development setup
- Submitting pull requests
- Coding standards

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Related Projects

- [nsecBunker](https://github.com/kind-0/nsecbunkerd) - The nsecBunker server
- [nostr-java](https://github.com/tcheeric/nostr-java) - Java implementation of Nostr protocol
- [NIP-46](https://github.com/nostr-protocol/nips/blob/master/46.md) - Nostr Connect specification

## Support

- **GitHub Issues**: Report bugs or request features
- **Discussions**: Ask questions and share ideas
