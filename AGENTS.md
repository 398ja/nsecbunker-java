# Repo Guidelines


## Project
- Maintain the versions in the configuration section of the parent pom.xml file.

## Coding
 - When writing code, follow the "Clean Code" principles:
   - [Clean Code](https://dev.398ja.xyz/books/Clean_Architecture.pdf)
     - Relevant chapters: 2, 3, 4, 7, 10, 17
   - [Clean Architecture](https://dev.398ja.xyz/books/Clean_Code.pdf)
     - Relevant chapters: All chapters in part III and IV, 7-14.
 - [Design Patterns](https://github.com/iluwatar/java-design-patterns)
   - Follow design patterns as described in the book, whenever possible.
- Always rely on imports rather than fully qualified class names in code to keep implementations readable.
- When commiting code, follow the [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/) specification.
- When adding new features, ensure they are compliant with the Cashu specification (NUTs) provided above.

## Documentation

- When generating documentation:
  - Follow the Diátaxis framework and classify each document as a tutorial, how-to guide, reference, or explanation.
  - Place new Markdown files under `docs/<section>` matching the chosen category.
  - Start each document with a top-level `#` heading and a short introduction that states the purpose.
  - Link the document from `docs/README.md` in the corresponding section.
  - Use relative links to reference other documents and keep code snippets minimal and tested.
  - Consult the following resources on Diátaxis for guidance:
    - https://github.blog/developer-skills/documentation-done-right-a-developers-guide/
    - https://diataxis.fr/
    - https://diataxis.fr/start-here/
    - https://diataxis.fr/how-to-use-diataxis/
    - https://diataxis.fr/tutorials/
    - https://diataxis.fr/how-to-guides/
    - https://diataxis.fr/tutorials-how-to/
    - https://diataxis.fr/quality/
    - https://diataxis.fr/complex-hierarchies/
    - https://diataxis.fr/compass/

## Testing

- Always run `mvn -q verify` from the repository root before committing your changes.
- Include the command's output in the PR description.
- If tests fail due to dependency or network issues, mention this in the PR.
- Update the documentation files if you add or modify features.
- Update the `pom.xml` file for new modules or dependencies, ensuring compatibility with Java 21.
- Verify new Dockerfiles or `docker-compose.yml` files by running `docker-compose build`.
- Document new REST endpoints in the API documentation and ensure they are tested.
- Add unit tests for new functionality, covering edge cases. Follow "Clean Code" principles on unit tests, as described in the "Clean Code" book (Chapter 9).
- Ensure modifications to existing code do not break functionality and pass all tests.
- Add integration tests for new features to verify end-to-end functionality.
- Ensure new dependencies or configurations do not introduce security vulnerabilities.
- Add a comment on top of every test method to describe the test in plain English.

### Test Structure and Clean Code Principles

All tests should follow Clean Code principles (Chapter 9: "Unit Tests"):

**1. AAA Pattern (Arrange-Act-Assert)**:
Every test should be structured in three clear sections:

```java
@Test
void shouldValidateUserInputCorrectly() {
    // Arrange: Set up test data and preconditions
    WalletRequestValidator validator = WalletRequestValidator.usingDefaultProvider();
    TestRequest request = new TestRequest("valid-field", 100L);

    // Act: Execute the operation being tested
    TestRequest result = validator.validate(request);

    // Then: Verify the expected outcome
    assertThat(result).isSameAs(request);
    assertThat(result.field()).isEqualTo("valid-field");
}
```

**2. One Assert Per Test (or Concept)**:
- Prefer testing one concept per test method
- Multiple asserts are acceptable if they verify the same logical concept
- Split unrelated assertions into separate test methods

**3. F.I.R.S.T. Principles**:
- **Fast**: Tests should run quickly (< 1 second for unit tests)
- **Independent**: Tests should not depend on each other
- **Repeatable**: Tests should produce the same result every time
- **Self-Validating**: Tests should have boolean output (pass/fail)
- **Timely**: Write tests before or with the code (TDD/BDD)

**4. Descriptive Test Names**:
Use clear, behavior-describing names following the pattern: `should[ExpectedBehavior]When[StateUnderTest]`

```java
// ✅ Good: Clear what's being tested
@Test
void shouldReturnEmptyListWhenNoTokensExist() { }

@Test
void shouldThrowExceptionWhenAmountIsNegative() { }

@Test
void shouldPreserveTokenOrderWhenCreatingWalletState() { }

// ❌ Bad: Unclear or implementation-focused
@Test
void testMethod1() { }

@Test
void checkValidation() { }
```

**5. Test Comments**:
Add a brief comment above each test explaining **what** is being tested (in plain English):

```java
/**
 * Tests that WalletState creates an immutable copy of the tokens list,
 * preventing external modification after construction.
 */
@Test
void shouldCreateImmutableCopyOfTokensList() {
    // Given: Mutable token list
    List<WalletToken> mutableTokens = new ArrayList<>(List.of(token1, token2));

    // When: Creating wallet state
    WalletState state = new WalletState(schema, timestamp, mutableTokens, List.of(), List.of());

    // Then: tokens list is immutable
    assertThatThrownBy(() -> state.tokens().add(token3))
        .isInstanceOf(UnsupportedOperationException.class);
}
```

**6. Edge Cases and Boundary Conditions**:
Always test:
- Null inputs
- Empty collections
- Boundary values (0, -1, MAX_VALUE)
- Invalid inputs
- Concurrent access (when applicable)

**7. Test Data Builders**:
For complex objects, use test data builders or factory methods:

```java
// Helper method for test data creation
private WalletState createWalletState(List<WalletToken> tokens) {
    return new WalletState(
        new WalletSchemaMetadata(/* ... */),
        Instant.now(),
        tokens,
        List.of(),
        List.of()
    );
}
```

**8. Property-Based Testing** (for appropriate cases):
Use jqwik for testing properties across many inputs:

```java
@Property
void shouldAlwaysReturnPositiveBalance(@ForAll @Positive long amount) {
    WalletBalance balance = new WalletBalance(amount);
    assertThat(balance.getAmount()).isGreaterThan(0);
}
```

**9. Integration Tests**:
Separate integration tests with `@Tag("integration")` or `@Tag("it-mint")`:
- Use Testcontainers for external dependencies
- Clean up resources in `@AfterEach` or `@AfterAll`
- Keep integration tests focused and fast

**10. Test Organization**:
```
src/test/java/
├── unit/                    # Pure unit tests (no external dependencies)
├── integration/             # Integration tests (databases, APIs, containers)
└── e2e/                     # End-to-end tests
```

**Example of Well-Structured Test Class**:

```java
/**
 * Unit tests for Bech32 encoding/decoding utilities.
 * Tests charset validation and checksum verification per BIP-173.
 */
class Bech32Test {

    @Test
    void shouldAcceptValidBech32Charset() {
        // Given: String with only bech32 characters
        String validData = "qpzry9x8gf2tvdw0s3jn54khce6mua7l";

        // When: Checking charset
        boolean result = Bech32.containsOnlyCharset(validData);

        // Then: All characters are valid
        assertThat(result).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "o", "i", "b"})
    void shouldRejectDisallowedBech32Characters(String invalidChar) {
        // Given: Strings containing disallowed characters
        String data = "qpzry" + invalidChar;

        // When: Checking charset
        boolean result = Bech32.containsOnlyCharset(data);

        // Then: Disallowed character rejected
        assertThat(result).isFalse();
    }

    @Test
    void shouldHandleEmptyString() {
        // Given: Empty string
        String emptyData = "";

        // When: Checking charset
        boolean result = Bech32.containsOnlyCharset(emptyData);

        // Then: Empty string is valid
        assertThat(result).isTrue();
    }
}
```

**Reference**: "Clean Code" by Robert C. Martin, Chapter 9: "Unit Tests"

## Pull Requests

- Always follow the repository's PR submission guidelines and use the PR template located at `.github/pull_request_template.md`.
- Summarize the changes made and describe how they were tested.
- Include any limitations or known issues in the description.
- Ensure all new features are compliant with the Cashu specification (NUTs) provided above.

## Versioning and Commits

- Follow the semantic versioning rules described at [semver.org](https://semver.org/) when updating project versions.
- Coordinate release tags, branching, and commit messages with these practices:
  - Use conventional commit types to signal whether a change is a fix, feature, or breaking change.
  - Derive semantic version bumps and changelog entries from the accumulated conventional commits.
- After completing each task, bump the project version according to semantic versioning rules before publishing changes.
- Use conventional commit types to drive automated version calculation and changelog generation.

## Error Handling and Exceptions

### Exception Hierarchy

The project uses a structured exception hierarchy to provide clear, actionable error information to users:

- **`WalletOperationException`** (abstract base): All exceptions exposed to the CLI boundary should extend this class.
  - Provides structured error information: error code, retryable flag, user message, and actionable suggestion.
  - Use for business logic failures that users need to understand and potentially resolve.
  - **Always include an actionable suggestion** to help users resolve the issue.

- **Infrastructure exceptions** (extend `Exception`): Use for recoverable errors in infrastructure layers.
  - Examples: `HttpRequestException`, `EncryptionException`, `KeyDerivationException`
  - These are checked exceptions requiring explicit handling.

- **Business logic failures** (extend `RuntimeException`): Use for programming errors or unrecoverable business logic failures.
  - Examples: `MintApiException`, `WalletStorageException`, `RelayUnavailableException`
  - These are unchecked exceptions that typically indicate bugs or system-level issues.

### Error Message Format

All error messages must follow this template:

```
{WHAT_HAPPENED}. {WHY_IT_HAPPENED}. Suggestion: {ACTIONABLE_STEP}.
```

**Components:**
- **What**: Clear description of the failure (what operation failed)
- **Why**: Context about why it failed (root cause, when applicable)
- **Suggestion**: Concrete action the user can take to resolve or work around the issue

**Example:**
```java
throw new QuoteExpiredException(
    invoice,
    expiredAt,
    "Quote expired before payment (expired at " + expiredAt + ")"
);
// Suggestion is automatically added: "Request a new quote and complete the payment within 5 minutes..."
```

### Creating New Exceptions

When creating a new `WalletOperationException` subclass:

1. **Define error codes** as constants (e.g., `"QUOTE_EXPIRED"`, `"PROOF_IMPORT_FAILED"`)
2. **Provide default suggestions** as static constants for common failure scenarios
3. **Include factory methods** for common cases with context-specific suggestions
4. **Document common causes** in the class Javadoc

**Example:**
```java
public final class MyException extends WalletOperationException {
    private static final String DEFAULT_SUGGESTION =
            "Clear, actionable suggestion for the most common scenario.";

    public MyException(String userMessage) {
        super("MY_ERROR_CODE", false, userMessage, DEFAULT_SUGGESTION);
    }

    // Factory methods for specific scenarios
    public static MyException forNetworkFailure(String url) {
        return new MyException(
            "Failed to connect to " + url,
            "Check network connectivity and verify the URL is correct"
        );
    }
}
```

### Throwing Exceptions

- **Context is king**: Include relevant context in error messages (URLs, amounts, timestamps, etc.)
- **Be specific**: "Failed to connect to wss://relay.example" is better than "Relay unavailable"
- **Preserve the cause**: Always include the original exception as a cause when wrapping
- **Choose retryable wisely**: Set `retryable=true` only for transient failures (network timeouts, temporary unavailability)

**Good Example:**
```java
try {
    client.connect();
} catch (IOException e) {
    throw new RelayUnavailableException(
        "Failed to connect to relay " + url,
        true,  // retryable
        e      // cause
    );
}
```

**Bad Example:**
```java
catch (Exception e) {
    throw new RuntimeException("Error");  // ❌ No context, no suggestion, loses type information
}
```

### Exception Handling Best Practices

1. **Catch specific exceptions**: Avoid catching `Exception` or `Throwable` unless absolutely necessary
2. **Handle at the right level**: Catch exceptions where you can meaningfully handle them
3. **Add context when rethrowing**: Wrap low-level exceptions with domain-specific ones, preserving the cause
4. **Log before throwing**: Log ERROR for unexpected failures, WARN for expected but problematic conditions
5. **Clean up resources**: Use try-with-resources or finally blocks to ensure cleanup
6. **Don't swallow exceptions**: Every caught exception should be logged or rethrown

**Example:**
```java
try {
    return mintApi.requestQuote(amount);
} catch (HttpRequestException e) {
    LOGGER.error("mint_quote_failed amount={} mint_url={} error={}",
        amount, mintUrl, e.getMessage(), e);
    throw new MintApiException(
        "Failed to request mint quote for " + amount + " sats",
        e
    );
}
```

### Standard Error Codes


### Logging Exceptions

When logging exceptions:

- **ERROR**: Use for unexpected failures that require investigation
- **WARN**: Use for expected but problematic conditions (retryable failures, circuit breaker open)
- **Include structured context**: Log the error code, operation details, and impact

**Example:**
```java
LOGGER.error("wallet_operation_failed error_code={} retryable={} operation={} error={}",
    exception.getErrorCode(),
    exception.isRetryable(),
    "send_payment",
    exception.getMessage(),
    exception);
```

## Logging

- Write each log entry to explain what happened, why it happened, and the resulting impact in a single, plain-language sentence that leads with the subject (component or entity) followed by the action and outcome.
- Keep formatting consistent: prefer structured JSON or key-value pairs, keep field names stable, and keep the free-text portion declarative rather than conversational.
- Include only the context needed to interpret the event—identifiers, parameters, decision factors, correlation or trace IDs—while omitting or masking secrets, personal data, and cryptographic material.
- State the exact state transition or decision (for example, `payment_quote marked_pending`) and clearly indicate whether the action succeeded, failed, or was skipped so responders can act without rereading code.
- For warnings and errors, name the failing operation, summarize the triggering condition, and note any user-facing impact or fallback that occurred.
- Focus `DEBUG`/`TRACE` messages on diagnostic value by logging the specific variables, branch choices, and derived values that explain behavior rather than dumping entire payloads or repeating static descriptions.
- Present dynamic data as ordered key-value pairs (e.g., `user_id=123`) and avoid multi-line blobs so messages remain grep-friendly and machine-parseable.
- Use neutral, professional language in all logs: avoid blameful phrasing, jokes, or exclamation points, and ensure terminology matches the rest of the codebase.
- Avoid duplicating the same event across multiple levels or components; ensure each message adds a unique perspective that will not overwhelm the logs with noise.
- Maintain tense consistency (present tense for in-progress actions, past tense for completed outcomes) and define any abbreviations within the repository documentation before using them in log messages.