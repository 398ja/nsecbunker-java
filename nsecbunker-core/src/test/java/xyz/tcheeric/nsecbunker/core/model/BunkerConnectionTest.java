package xyz.tcheeric.nsecbunker.core.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class BunkerConnectionTest {

    private static final String TEST_PUBKEY = "npub1test";
    private static final String TEST_RELAY = "wss://relay.example.com";
    private static final String TEST_SECRET = "secret-123";

    @Test
    void shouldBuildWithDefaultValues() {
        BunkerConnection connection = BunkerConnection.builder()
                .remotePubkey(TEST_PUBKEY)
                .build();

        assertThat(connection.getRemotePubkey()).isEqualTo(TEST_PUBKEY);
        assertThat(connection.getRelays()).isEmpty();
        assertThat(connection.getSecret()).isNull();
        assertThat(connection.getToken()).isNull();
    }

    @Test
    void shouldBuildWithAllFields() {
        BunkerConnection connection = BunkerConnection.builder()
                .remotePubkey(TEST_PUBKEY)
                .relays(List.of(TEST_RELAY, "wss://relay2.example.com"))
                .secret(TEST_SECRET)
                .token("token-123")
                .build();

        assertThat(connection.getRemotePubkey()).isEqualTo(TEST_PUBKEY);
        assertThat(connection.getRelays()).hasSize(2);
        assertThat(connection.getSecret()).isEqualTo(TEST_SECRET);
        assertThat(connection.getToken()).isEqualTo("token-123");
    }

    @Test
    void forPubkeyShouldCreateBuilderWithPubkey() {
        BunkerConnection connection = BunkerConnection.forPubkey(TEST_PUBKEY)
                .secret(TEST_SECRET)
                .build();

        assertThat(connection.getRemotePubkey()).isEqualTo(TEST_PUBKEY);
        assertThat(connection.getSecret()).isEqualTo(TEST_SECRET);
    }

    @Test
    void forPubkeyShouldThrowWhenPubkeyNull() {
        assertThatNullPointerException()
                .isThrownBy(() -> BunkerConnection.forPubkey(null))
                .withMessage("remotePubkey must not be null");
    }

    @Test
    void hasSecretShouldReturnTrueWhenSecretPresent() {
        BunkerConnection connection = BunkerConnection.builder()
                .remotePubkey(TEST_PUBKEY)
                .secret(TEST_SECRET)
                .build();

        assertThat(connection.hasSecret()).isTrue();
    }

    @Test
    void hasSecretShouldReturnFalseWhenSecretNull() {
        BunkerConnection connection = BunkerConnection.builder()
                .remotePubkey(TEST_PUBKEY)
                .build();

        assertThat(connection.hasSecret()).isFalse();
    }

    @Test
    void hasSecretShouldReturnFalseWhenSecretEmpty() {
        BunkerConnection connection = BunkerConnection.builder()
                .remotePubkey(TEST_PUBKEY)
                .secret("")
                .build();

        assertThat(connection.hasSecret()).isFalse();
    }

    @Test
    void hasTokenShouldReturnTrueWhenTokenPresent() {
        BunkerConnection connection = BunkerConnection.builder()
                .remotePubkey(TEST_PUBKEY)
                .token("token-123")
                .build();

        assertThat(connection.hasToken()).isTrue();
    }

    @Test
    void hasTokenShouldReturnFalseWhenTokenNull() {
        BunkerConnection connection = BunkerConnection.builder()
                .remotePubkey(TEST_PUBKEY)
                .build();

        assertThat(connection.hasToken()).isFalse();
    }

    @Test
    void hasTokenShouldReturnFalseWhenTokenEmpty() {
        BunkerConnection connection = BunkerConnection.builder()
                .remotePubkey(TEST_PUBKEY)
                .token("")
                .build();

        assertThat(connection.hasToken()).isFalse();
    }

    @Test
    void hasCredentialsShouldReturnTrueWhenSecretPresent() {
        BunkerConnection connection = BunkerConnection.builder()
                .remotePubkey(TEST_PUBKEY)
                .secret(TEST_SECRET)
                .build();

        assertThat(connection.hasCredentials()).isTrue();
    }

    @Test
    void hasCredentialsShouldReturnTrueWhenTokenPresent() {
        BunkerConnection connection = BunkerConnection.builder()
                .remotePubkey(TEST_PUBKEY)
                .token("token-123")
                .build();

        assertThat(connection.hasCredentials()).isTrue();
    }

    @Test
    void hasCredentialsShouldReturnFalseWhenNoCredentials() {
        BunkerConnection connection = BunkerConnection.builder()
                .remotePubkey(TEST_PUBKEY)
                .build();

        assertThat(connection.hasCredentials()).isFalse();
    }

    @Test
    void toConnectionStringShouldReturnBasicString() {
        BunkerConnection connection = BunkerConnection.builder()
                .remotePubkey(TEST_PUBKEY)
                .build();

        assertThat(connection.toConnectionString()).isEqualTo("bunker://npub1test");
    }

    @Test
    void toConnectionStringShouldIncludeRelay() {
        BunkerConnection connection = BunkerConnection.builder()
                .remotePubkey(TEST_PUBKEY)
                .relays(List.of(TEST_RELAY))
                .build();

        assertThat(connection.toConnectionString())
                .isEqualTo("bunker://npub1test?relay=wss://relay.example.com");
    }

    @Test
    void toConnectionStringShouldIncludeMultipleRelays() {
        BunkerConnection connection = BunkerConnection.builder()
                .remotePubkey(TEST_PUBKEY)
                .relays(List.of(TEST_RELAY, "wss://relay2.example.com"))
                .build();

        assertThat(connection.toConnectionString())
                .isEqualTo("bunker://npub1test?relay=wss://relay.example.com&relay=wss://relay2.example.com");
    }

    @Test
    void toConnectionStringShouldIncludeSecret() {
        BunkerConnection connection = BunkerConnection.builder()
                .remotePubkey(TEST_PUBKEY)
                .secret(TEST_SECRET)
                .build();

        assertThat(connection.toConnectionString())
                .isEqualTo("bunker://npub1test?secret=secret-123");
    }

    @Test
    void toConnectionStringShouldIncludeRelayAndSecret() {
        BunkerConnection connection = BunkerConnection.builder()
                .remotePubkey(TEST_PUBKEY)
                .relays(List.of(TEST_RELAY))
                .secret(TEST_SECRET)
                .build();

        assertThat(connection.toConnectionString())
                .isEqualTo("bunker://npub1test?relay=wss://relay.example.com&secret=secret-123");
    }

    @Test
    void getRelaysShouldReturnUnmodifiableList() {
        BunkerConnection connection = BunkerConnection.builder()
                .remotePubkey(TEST_PUBKEY)
                .relays(List.of(TEST_RELAY))
                .build();

        List<String> relays = connection.getRelays();
        assertThat(relays).hasSize(1);

        // Should be unmodifiable
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> relays.add("new"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void getRelaysShouldReturnEmptyListWhenNull() {
        BunkerConnection connection = BunkerConnection.builder()
                .remotePubkey(TEST_PUBKEY)
                .relays(null)
                .build();

        assertThat(connection.getRelays()).isEmpty();
    }

    @Test
    void toBuilderShouldCreateCopy() {
        BunkerConnection original = BunkerConnection.builder()
                .remotePubkey(TEST_PUBKEY)
                .relays(List.of(TEST_RELAY))
                .secret(TEST_SECRET)
                .build();

        BunkerConnection copy = original.toBuilder()
                .secret("new-secret")
                .build();

        assertThat(copy.getRemotePubkey()).isEqualTo(TEST_PUBKEY);
        assertThat(copy.getSecret()).isEqualTo("new-secret");
        assertThat(original.getSecret()).isEqualTo(TEST_SECRET);
    }

    @Test
    void equalsShouldWorkCorrectly() {
        BunkerConnection conn1 = BunkerConnection.builder()
                .remotePubkey(TEST_PUBKEY)
                .relays(List.of(TEST_RELAY))
                .secret(TEST_SECRET)
                .build();

        BunkerConnection conn2 = BunkerConnection.builder()
                .remotePubkey(TEST_PUBKEY)
                .relays(List.of(TEST_RELAY))
                .secret(TEST_SECRET)
                .build();

        assertThat(conn1).isEqualTo(conn2);
        assertThat(conn1.hashCode()).isEqualTo(conn2.hashCode());
    }

    @Test
    void toStringShouldExcludeSecret() {
        BunkerConnection connection = BunkerConnection.builder()
                .remotePubkey(TEST_PUBKEY)
                .secret(TEST_SECRET)
                .build();

        String str = connection.toString();
        assertThat(str).doesNotContain(TEST_SECRET);
        assertThat(str).contains(TEST_PUBKEY);
    }

    @Test
    void bunkerSchemeShouldBeBunker() {
        assertThat(BunkerConnection.BUNKER_SCHEME).isEqualTo("bunker");
    }
}
