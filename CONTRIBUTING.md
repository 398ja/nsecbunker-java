# Contributing to nsecbunker-java

Thank you for your interest in contributing to nsecbunker-java! This document provides guidelines and instructions for contributing.

## Code of Conduct

By participating in this project, you agree to maintain a respectful and inclusive environment for everyone.

## How to Contribute

### Reporting Bugs

1. **Check existing issues** - Search [existing issues](https://github.com/tcheeric/nsecbunker-java/issues) to avoid duplicates
2. **Use the bug report template** - Fill out all required fields
3. **Provide reproduction steps** - Include minimal code to reproduce the issue
4. **Include version information** - Java version, library version, OS

### Suggesting Features

1. **Check existing discussions** - Your idea may already be proposed
2. **Use the feature request template** - Describe the problem and proposed solution
3. **Include API examples** - Show how you'd like the API to look

### Submitting Pull Requests

1. **Fork the repository** and create your branch from `master`
2. **Follow coding standards** (see below)
3. **Add tests** for new functionality
4. **Update documentation** as needed
5. **Run the full build** before submitting

## Development Setup

### Prerequisites

- JDK 17 or higher
- Maven 3.8+
- Git

### Building the Project

```bash
# Clone your fork
git clone https://github.com/YOUR_USERNAME/nsecbunker-java.git
cd nsecbunker-java

# Build the project
mvn clean install

# Run tests only
mvn test

# Run with quality checks
mvn verify -Pquality

# Generate site documentation
mvn site
```

### Running Quality Checks

```bash
# Run Checkstyle
mvn checkstyle:check

# Run SpotBugs
mvn spotbugs:check

# Run PMD
mvn pmd:check

# Run all quality checks
mvn verify -Pquality
```

## Coding Standards

### Code Style

- Follow [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html) with modifications in `checkstyle.xml`
- Maximum line length: 150 characters
- Use 4 spaces for indentation (no tabs)
- Always use braces for control structures

### Naming Conventions

- **Classes**: PascalCase (e.g., `RemoteSigner`, `BunkerPolicy`)
- **Methods/Variables**: camelCase (e.g., `signEvent`, `publicKey`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `DEFAULT_TIMEOUT`)
- **Packages**: lowercase (e.g., `xyz.tcheeric.nsecbunker.client`)

### Documentation

- Add Javadoc for all public classes and methods
- Include `@param`, `@return`, and `@throws` tags
- Keep comments concise and meaningful

Example:
```java
/**
 * Signs a Nostr event using the remote signer.
 *
 * @param event the event to sign (must not be null)
 * @return a CompletableFuture containing the signed event
 * @throws SigningException if signing fails
 */
public CompletableFuture<GenericEvent> signEvent(GenericEvent event) {
    // implementation
}
```

### Testing

- Write unit tests for all new functionality
- Use descriptive test method names
- Follow the Arrange-Act-Assert pattern
- Aim for high test coverage on critical paths

Example:
```java
@Test
void signEvent_shouldReturnSignedEvent_whenValidEventProvided() {
    // Arrange
    GenericEvent event = createTestEvent();

    // Act
    GenericEvent signed = signer.signEvent(event).join();

    // Assert
    assertNotNull(signed.getSig());
    assertTrue(signed.verify());
}
```

### Commit Messages

Use clear, descriptive commit messages:

- Start with a verb in present tense (add, fix, update, remove)
- Keep the first line under 72 characters
- Reference issues when applicable

Examples:
```
feat: Add NIP-44 encryption support
fix: Handle connection timeout gracefully (#123)
docs: Update installation instructions
refactor: Extract connection logic to separate class
test: Add integration tests for admin API
```

## Project Structure

```
nsecbunker-java/
├── nsecbunker-core/           # Core models and interfaces
├── nsecbunker-connection/     # Relay connection management
├── nsecbunker-protocol/       # NIP-46 protocol implementation
├── nsecbunker-client/         # Remote signer client
├── nsecbunker-admin/          # Admin operations
├── nsecbunker-monitoring/     # Logging, metrics, health
├── nsecbunker-account/        # Account management
├── nsecbunker-spring-boot-starter/  # Spring Boot integration
└── project/                   # Project documentation
```

## Pull Request Process

1. **Create a feature branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```

2. **Make your changes** following the coding standards

3. **Run the full build**
   ```bash
   mvn clean verify -Pquality
   ```

4. **Push to your fork**
   ```bash
   git push origin feature/your-feature-name
   ```

5. **Open a Pull Request**
   - Fill out the PR template completely
   - Link related issues
   - Request review from maintainers

6. **Address review feedback**
   - Make requested changes
   - Push additional commits
   - Re-request review when ready

## Review Criteria

Pull requests are reviewed for:

- **Correctness** - Does it work as intended?
- **Tests** - Is the code adequately tested?
- **Style** - Does it follow coding standards?
- **Documentation** - Are changes documented?
- **Performance** - Any performance implications?
- **Security** - Any security concerns?

## Getting Help

- **GitHub Issues** - For bugs and feature requests
- **GitHub Discussions** - For questions and general discussion

## License

By contributing, you agree that your contributions will be licensed under the MIT License.
