package xyz.tcheeric.nsecbunker.account.registration;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class AccountRegistrationResultTest {

    @Test
    void shouldBuildWithAllFields() {
        Instant now = Instant.now();

        AccountRegistrationResult result = AccountRegistrationResult.builder()
                .username("alice")
                .domain("example.com")
                .nip05("alice@example.com")
                .npub("npub1test")
                .keyName("alice-key")
                .createdAt(now)
                .build();

        assertThat(result.getUsername()).isEqualTo("alice");
        assertThat(result.getDomain()).isEqualTo("example.com");
        assertThat(result.getNip05()).isEqualTo("alice@example.com");
        assertThat(result.getNpub()).isEqualTo("npub1test");
        assertThat(result.getKeyName()).isEqualTo("alice-key");
        assertThat(result.getCreatedAt()).isEqualTo(now);
    }

    @Test
    void shouldBuildWithNullFields() {
        AccountRegistrationResult result = AccountRegistrationResult.builder()
                .username("bob")
                .build();

        assertThat(result.getUsername()).isEqualTo("bob");
        assertThat(result.getDomain()).isNull();
        assertThat(result.getNip05()).isNull();
    }

    @Test
    void toBuilderShouldCreateCopy() {
        AccountRegistrationResult original = AccountRegistrationResult.builder()
                .username("alice")
                .domain("test.com")
                .build();

        AccountRegistrationResult copy = original.toBuilder()
                .domain("example.com")
                .build();

        assertThat(copy.getUsername()).isEqualTo("alice");
        assertThat(copy.getDomain()).isEqualTo("example.com");
        assertThat(original.getDomain()).isEqualTo("test.com");
    }

    @Test
    void equalsShouldWorkCorrectly() {
        Instant now = Instant.now();

        AccountRegistrationResult result1 = AccountRegistrationResult.builder()
                .username("alice")
                .domain("example.com")
                .createdAt(now)
                .build();

        AccountRegistrationResult result2 = AccountRegistrationResult.builder()
                .username("alice")
                .domain("example.com")
                .createdAt(now)
                .build();

        assertThat(result1).isEqualTo(result2);
        assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
    }

    @Test
    void toStringShouldContainFields() {
        AccountRegistrationResult result = AccountRegistrationResult.builder()
                .username("alice")
                .nip05("alice@example.com")
                .build();

        String str = result.toString();
        assertThat(str).contains("alice");
        assertThat(str).contains("alice@example.com");
    }
}
