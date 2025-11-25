# Security Best Practices

This explanation summarizes defensive measures when using nsecbunker-java.

## Key handling
- Use ephemeral communication keys for clients; keep admin keys offline when possible.
- Never log nsec, tokens, or secrets. Scrub sensitive values before logging.

## Policies and permissions
- Prefer deny-by-default policies; allow only required methods and event kinds.
- Limit token lifetimes and usage counts; revoke unused tokens promptly.

## Transport and relays
- Use trusted relays over TLS (`wss://`). Keep relay lists short and curated.
- Monitor health and revoke access on repeated failures or suspicious activity.

## Configuration hygiene
- Store secrets in environment variables or vaults; avoid embedding them in code.
- Separate dev/prod relays and credentials; rotate credentials regularly.

## Testing and monitoring
- Run `mvn -q verify` before releases.
- Enable Actuator health/info, and add Micrometer metrics to observe signing latency and error rates.
