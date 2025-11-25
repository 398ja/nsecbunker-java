package xyz.tcheeric.nsecbunker.account.nip05;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Nip05RecordTest {

    @Test
    void shouldBuildWithAllFields() {
        Nip05Record record = Nip05Record.builder()
                .nip05("alice@example.com")
                .pubkey("79be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798")
                .relaysJson("[\"wss://relay1.example.com\",\"wss://relay2.example.com\"]")
                .build();

        assertThat(record.getNip05()).isEqualTo("alice@example.com");
        assertThat(record.getPubkey()).isEqualTo("79be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798");
        assertThat(record.getRelaysJson()).contains("relay1.example.com");
    }

    @Test
    void shouldBuildWithNullFields() {
        Nip05Record record = Nip05Record.builder()
                .nip05("bob@test.com")
                .build();

        assertThat(record.getNip05()).isEqualTo("bob@test.com");
        assertThat(record.getPubkey()).isNull();
        assertThat(record.getRelaysJson()).isNull();
    }

    @Test
    void toBuilderShouldCreateCopy() {
        Nip05Record original = Nip05Record.builder()
                .nip05("alice@example.com")
                .pubkey("pubkey1")
                .build();

        Nip05Record copy = original.toBuilder()
                .pubkey("pubkey2")
                .build();

        assertThat(copy.getNip05()).isEqualTo("alice@example.com");
        assertThat(copy.getPubkey()).isEqualTo("pubkey2");
        assertThat(original.getPubkey()).isEqualTo("pubkey1");
    }

    @Test
    void equalsShouldWorkCorrectly() {
        Nip05Record record1 = Nip05Record.builder()
                .nip05("alice@example.com")
                .pubkey("pubkey")
                .build();

        Nip05Record record2 = Nip05Record.builder()
                .nip05("alice@example.com")
                .pubkey("pubkey")
                .build();

        assertThat(record1).isEqualTo(record2);
        assertThat(record1.hashCode()).isEqualTo(record2.hashCode());
    }

    @Test
    void toStringShouldContainFields() {
        Nip05Record record = Nip05Record.builder()
                .nip05("alice@example.com")
                .pubkey("abc123")
                .build();

        String str = record.toString();
        assertThat(str).contains("alice@example.com");
        assertThat(str).contains("abc123");
    }
}
