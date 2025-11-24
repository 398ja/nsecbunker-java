# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 0.1.x   | :white_check_mark: |

## Reporting a Vulnerability

We take security vulnerabilities seriously. If you discover a security issue, please report it responsibly.

### How to Report

**DO NOT** open a public GitHub issue for security vulnerabilities.

Instead, please report security issues by emailing:

**security@tcheeric.xyz**

Or use [GitHub's private vulnerability reporting](https://github.com/tcheeric/nsecbunker-java/security/advisories/new).

### What to Include

Please provide the following information:

1. **Description** - A clear description of the vulnerability
2. **Impact** - Potential impact and severity assessment
3. **Reproduction Steps** - Detailed steps to reproduce the issue
4. **Affected Versions** - Which versions are affected
5. **Suggested Fix** - If you have one (optional)

### Response Timeline

- **Initial Response**: Within 48 hours
- **Assessment**: Within 7 days
- **Fix Timeline**: Depends on severity
  - Critical: 1-7 days
  - High: 7-14 days
  - Medium: 14-30 days
  - Low: Next release cycle

### What to Expect

1. **Acknowledgment** - We'll confirm receipt of your report
2. **Assessment** - We'll evaluate the vulnerability and its impact
3. **Communication** - We'll keep you informed of our progress
4. **Credit** - With your permission, we'll credit you in the security advisory

## Security Best Practices

When using nsecbunker-java, follow these recommendations:

### Key Management

- **Never hardcode secrets** - Use environment variables or secure vaults
- **Protect nsec keys** - Admin nsec keys should be stored securely
- **Use minimal permissions** - Grant only necessary permissions to clients

### Connection Security

- **Use secure relays** - Connect only to trusted relay endpoints
- **Verify TLS certificates** - Don't disable certificate validation
- **Monitor connections** - Watch for suspicious connection patterns

### Token Handling

- **Short-lived tokens** - Use appropriate expiration times
- **Revoke unused tokens** - Clean up tokens that are no longer needed
- **Audit token usage** - Monitor for unusual access patterns

### Example: Secure Configuration

```java
// Good: Use environment variables for secrets
String adminNsec = System.getenv("NSECBUNKER_ADMIN_NSEC");

NsecBunkerAdmin admin = NsecBunkerAdmin.builder()
    .adminNsec(adminNsec)
    .connectionString(System.getenv("NSECBUNKER_CONNECTION"))
    .build();

// Good: Use short-lived tokens with specific permissions
AccessToken token = admin.tokens()
    .create(keyName, "mobile-client")
    .withPolicy(restrictedPolicyId)
    .expiresIn(Duration.ofHours(24))
    .execute()
    .get();
```

### Logging Security

- **Don't log secrets** - Never log nsec keys, tokens, or passphrases
- **Mask sensitive data** - Redact sensitive information in logs
- **Secure log storage** - Protect log files from unauthorized access

## Dependency Security

We regularly scan dependencies for vulnerabilities using:

- GitHub Dependabot
- OWASP Dependency Check
- GitHub CodeQL Analysis

Updates for security-critical dependencies are prioritized.

## Security Updates

Security fixes are released as patch versions. We recommend:

1. **Subscribe to releases** - Watch the repository for new releases
2. **Update promptly** - Apply security updates as soon as possible
3. **Review changelogs** - Check release notes for security-related changes

## Acknowledgments

We thank the following individuals for responsibly disclosing security issues:

*No security issues have been reported yet.*
