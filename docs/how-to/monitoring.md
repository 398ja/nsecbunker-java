# Monitoring How-To

This guide covers basic health and info exposure for nsecbunker-java when running in Spring Boot.

## Health checks
- The starter registers `BunkerHealthIndicator`, which pings the signer.
- Enable Actuator and hit `/actuator/health` to see `nsecbunker` status.

## Info endpoint
- `BunkerInfoContributor` adds bunker pubkey and configured relays to `/actuator/info`.

## Configuration
```properties
nsecbunker.signer.bunker-pubkey=npub1...
nsecbunker.signer.client-private-key=nsec1...
nsecbunker.signer.relays[0]=wss://relay.example.com
management.endpoints.web.exposure.include=health,info
```

## Alerts and metrics
- Health is up when ping succeeds; down on exceptions.
- For metrics, add Micrometer timers around signer calls and expose them via your monitoring stack.

## Tips
- Keep relay lists short and reliable to reduce health noise.
- Mask secrets in logs; avoid printing tokens or nsec values.
