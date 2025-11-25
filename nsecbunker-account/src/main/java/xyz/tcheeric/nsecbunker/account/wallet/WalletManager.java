package xyz.tcheeric.nsecbunker.account.wallet;

import java.util.concurrent.CompletableFuture;

/**
 * Manages wallet integration (LNBits stub).
 */
public interface WalletManager {

    /**
     * Creates a wallet for the given key.
     *
     * @param keyName key name
     * @return wallet info
     */
    CompletableFuture<WalletInfo> createWallet(String keyName);

    /**
     * Retrieves wallet info for a key.
     *
     * @param keyName key name
     * @return wallet info
     */
    CompletableFuture<WalletInfo> getWalletInfo(String keyName);
}
