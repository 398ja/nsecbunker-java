package xyz.tcheeric.nsecbunker.core.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BunkerKeyExceptionTest {

    private static final String TEST_KEY_NAME = "my-key";

    @Test
    void shouldCreateWithMessage() {
        BunkerKeyException exception = new BunkerKeyException("Key error");

        assertThat(exception.getMessage()).isEqualTo("Key error");
        assertThat(exception.getKeyName()).isNull();
        assertThat(exception.getReason()).isEqualTo(BunkerKeyException.Reason.UNKNOWN);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateWithKeyNameAndReason() {
        BunkerKeyException exception = new BunkerKeyException(
                "Key not found",
                TEST_KEY_NAME,
                BunkerKeyException.Reason.NOT_FOUND
        );

        assertThat(exception.getMessage()).isEqualTo("Key not found");
        assertThat(exception.getKeyName()).isEqualTo(TEST_KEY_NAME);
        assertThat(exception.getReason()).isEqualTo(BunkerKeyException.Reason.NOT_FOUND);
    }

    @Test
    void shouldCreateWithMessageAndCause() {
        RuntimeException cause = new RuntimeException("Crypto error");
        BunkerKeyException exception = new BunkerKeyException("Key error", cause);

        assertThat(exception.getMessage()).isEqualTo("Key error");
        assertThat(exception.getCause()).isEqualTo(cause);
        assertThat(exception.getKeyName()).isNull();
        assertThat(exception.getReason()).isEqualTo(BunkerKeyException.Reason.UNKNOWN);
    }

    @Test
    void notFoundShouldCreateCorrectException() {
        BunkerKeyException exception = BunkerKeyException.notFound(TEST_KEY_NAME);

        assertThat(exception.getMessage()).isEqualTo("Key not found: " + TEST_KEY_NAME);
        assertThat(exception.getKeyName()).isEqualTo(TEST_KEY_NAME);
        assertThat(exception.getReason()).isEqualTo(BunkerKeyException.Reason.NOT_FOUND);
    }

    @Test
    void lockedShouldCreateCorrectException() {
        BunkerKeyException exception = BunkerKeyException.locked(TEST_KEY_NAME);

        assertThat(exception.getMessage()).isEqualTo("Key is locked and requires passphrase: " + TEST_KEY_NAME);
        assertThat(exception.getKeyName()).isEqualTo(TEST_KEY_NAME);
        assertThat(exception.getReason()).isEqualTo(BunkerKeyException.Reason.LOCKED);
    }

    @Test
    void alreadyExistsShouldCreateCorrectException() {
        BunkerKeyException exception = BunkerKeyException.alreadyExists(TEST_KEY_NAME);

        assertThat(exception.getMessage()).isEqualTo("Key already exists: " + TEST_KEY_NAME);
        assertThat(exception.getKeyName()).isEqualTo(TEST_KEY_NAME);
        assertThat(exception.getReason()).isEqualTo(BunkerKeyException.Reason.ALREADY_EXISTS);
    }

    @Test
    void invalidPassphraseShouldCreateCorrectException() {
        BunkerKeyException exception = BunkerKeyException.invalidPassphrase(TEST_KEY_NAME);

        assertThat(exception.getMessage()).isEqualTo("Invalid passphrase for key: " + TEST_KEY_NAME);
        assertThat(exception.getKeyName()).isEqualTo(TEST_KEY_NAME);
        assertThat(exception.getReason()).isEqualTo(BunkerKeyException.Reason.INVALID_PASSPHRASE);
    }

    @Test
    void shouldSupportAllReasons() {
        for (BunkerKeyException.Reason reason : BunkerKeyException.Reason.values()) {
            BunkerKeyException exception = new BunkerKeyException("Test", "key", reason);
            assertThat(exception.getReason()).isEqualTo(reason);
        }
    }

    @Test
    void shouldExtendBunkerException() {
        BunkerKeyException exception = new BunkerKeyException("Test");

        assertThat(exception).isInstanceOf(BunkerException.class);
    }
}
