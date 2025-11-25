package xyz.tcheeric.nsecbunker.account.wallet;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WalletInfoTest {

    @Test
    void shouldBuildWithAllFields() {
        WalletInfo info = WalletInfo.builder()
                .keyName("alice-key")
                .walletId("wallet-123")
                .adminKey("admin-key-456")
                .invoiceKey("invoice-key-789")
                .build();

        assertThat(info.getKeyName()).isEqualTo("alice-key");
        assertThat(info.getWalletId()).isEqualTo("wallet-123");
        assertThat(info.getAdminKey()).isEqualTo("admin-key-456");
        assertThat(info.getInvoiceKey()).isEqualTo("invoice-key-789");
    }

    @Test
    void shouldBuildWithNullFields() {
        WalletInfo info = WalletInfo.builder()
                .keyName("test-key")
                .build();

        assertThat(info.getKeyName()).isEqualTo("test-key");
        assertThat(info.getWalletId()).isNull();
        assertThat(info.getAdminKey()).isNull();
        assertThat(info.getInvoiceKey()).isNull();
    }

    @Test
    void toBuilderShouldCreateCopy() {
        WalletInfo original = WalletInfo.builder()
                .keyName("alice-key")
                .walletId("wallet-123")
                .adminKey("admin-key")
                .build();

        WalletInfo copy = original.toBuilder()
                .walletId("wallet-456")
                .build();

        assertThat(copy.getKeyName()).isEqualTo("alice-key");
        assertThat(copy.getWalletId()).isEqualTo("wallet-456");
        assertThat(copy.getAdminKey()).isEqualTo("admin-key");
        assertThat(original.getWalletId()).isEqualTo("wallet-123");
    }

    @Test
    void equalsShouldWorkCorrectly() {
        WalletInfo info1 = WalletInfo.builder()
                .keyName("alice-key")
                .walletId("wallet-123")
                .build();

        WalletInfo info2 = WalletInfo.builder()
                .keyName("alice-key")
                .walletId("wallet-123")
                .build();

        assertThat(info1).isEqualTo(info2);
        assertThat(info1.hashCode()).isEqualTo(info2.hashCode());
    }

    @Test
    void toStringShouldContainFields() {
        WalletInfo info = WalletInfo.builder()
                .keyName("alice-key")
                .walletId("wallet-123")
                .build();

        String str = info.toString();
        assertThat(str).contains("alice-key");
        assertThat(str).contains("wallet-123");
    }
}
