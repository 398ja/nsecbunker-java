package xyz.tcheeric.nsecbunker.account.wallet;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LNBitsWalletManagerTest {

    /**
     * Ensures createWallet stores and returns wallet info.
     */
    @Test
    void shouldCreateWallet() {
        // Arrange
        LNBitsWalletManager manager = new LNBitsWalletManager();

        // Act
        WalletInfo info = manager.createWallet("key1").join();

        // Assert
        assertThat(info.getKeyName()).isEqualTo("key1");
        assertThat(info.getAdminKey()).isEqualTo("adm_key1");
        assertThat(info.getInvoiceKey()).isEqualTo("inv_key1");
    }

    /**
     * Ensures getWalletInfo retrieves stored wallet or fails.
     */
    @Test
    void shouldGetWalletInfoOrFail() {
        // Arrange
        LNBitsWalletManager manager = new LNBitsWalletManager();
        manager.createWallet("key2").join();

        // Act
        WalletInfo info = manager.getWalletInfo("key2").join();

        // Assert
        assertThat(info.getKeyName()).isEqualTo("key2");
        assertThatThrownBy(() -> manager.getWalletInfo("missing").join())
                .hasCauseInstanceOf(IllegalArgumentException.class);
    }
}
