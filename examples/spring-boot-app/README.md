# Spring Boot Application Example

A complete Spring Boot application demonstrating nsecBunker integration with:
- REST API for signing operations
- Spring Boot Actuator health checks
- Micrometer metrics
- Auto-configuration

## Features

- **REST API**: Sign events, get public key, encrypt/decrypt messages
- **Health Checks**: Monitor bunker connection status
- **Metrics**: Track signing operations with Micrometer
- **Auto-Configuration**: Zero-config setup with properties

## Quick Start

### 1. Configure Environment

```bash
export BUNKER_PUBKEY="your-bunker-pubkey-hex-or-npub"
export CLIENT_PRIVKEY="your-client-private-key-hex-or-nsec"
export RELAY_URL="wss://your-relay.example.com"
```

### 2. Run the Application

```bash
# With Maven
mvn spring-boot:run

# Or with Gradle
./gradlew bootRun
```

### 3. Test the Endpoints

```bash
# Get public key
curl http://localhost:8080/api/pubkey

# Sign an event
curl -X POST http://localhost:8080/api/sign \
  -H "Content-Type: application/json" \
  -d '{"kind":1,"content":"Hello Nostr!","created_at":1234567890,"tags":[]}'

# Encrypt a message
curl -X POST http://localhost:8080/api/encrypt \
  -H "Content-Type: application/json" \
  -d '{"pubkey":"recipient-pubkey","plaintext":"secret message"}'

# Check health
curl http://localhost:8080/actuator/health

# View metrics
curl http://localhost:8080/actuator/metrics/nsecbunker.signing.total
```

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/sign` | Sign a Nostr event |
| GET | `/api/pubkey` | Get signing public key |
| POST | `/api/encrypt` | Encrypt message (NIP-44) |
| POST | `/api/decrypt` | Decrypt message (NIP-44) |
| GET | `/api/ping` | Ping the bunker |
| GET | `/api/status` | Get service status |

## Actuator Endpoints

| Path | Description |
|------|-------------|
| `/actuator/health` | Health check with bunker status |
| `/actuator/info` | Application info |
| `/actuator/metrics` | All available metrics |
| `/actuator/prometheus` | Prometheus format metrics |

## Configuration

All configuration is in `application.yml`:

```yaml
nsecbunker:
  signer:
    bunker-pubkey: ${BUNKER_PUBKEY}
    client-private-key: ${CLIENT_PRIVKEY}
    relays:
      - ${RELAY_URL}
    use-ephemeral-key: true
    connect-timeout: 30s
    request-timeout: 60s

  metrics:
    enabled: true
    percentiles: true
```

## Available Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `nsecbunker.signing.total` | Counter | Total signing operations |
| `nsecbunker.signing.success` | Counter | Successful operations |
| `nsecbunker.signing.errors` | Counter | Failed operations |
| `nsecbunker.signing.latency` | Timer | Signing latency |
| `nsecbunker.signing.active` | Gauge | Active operations |
| `nsecbunker.connection.status` | Gauge | Connection status |

## Project Structure

```
spring-boot-app/
├── src/main/java/.../springboot/
│   ├── NsecBunkerDemoApplication.java  # Main application
│   ├── SigningController.java          # REST controller
│   └── SigningService.java             # Service layer
├── src/main/resources/
│   └── application.yml                 # Configuration
└── README.md
```

## Dependencies

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>xyz.tcheeric</groupId>
    <artifactId>nsecbunker-spring-boot-starter</artifactId>
    <version>${nsecbunker.version}</version>
</dependency>
```

## Production Considerations

1. **Secure Configuration**: Use environment variables or vault for secrets
2. **Rate Limiting**: Add rate limiting to the REST endpoints
3. **Authentication**: Secure the API with Spring Security
4. **Monitoring**: Export metrics to your monitoring system
5. **Logging**: Configure appropriate log levels for production
