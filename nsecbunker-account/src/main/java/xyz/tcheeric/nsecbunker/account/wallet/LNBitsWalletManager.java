package xyz.tcheeric.nsecbunker.account.wallet;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory LNBits wallet manager stub.
 */
public class LNBitsWalletManager implements WalletManager {

    private final Map<String, WalletInfo> wallets = new ConcurrentHashMap<>();

    @Override
    public CompletableFuture<WalletInfo> createWallet(String keyName) {
        Objects.requireNonNull(keyName, "keyName must not be null");
        WalletInfo wallet = WalletInfo.builder()
                .keyName(keyName)
                .walletId(UUID.randomUUID().toString())
                .adminKey("adm_" + keyName)
                .invoiceKey("inv_" + keyName)
                .build();
        wallets.put(keyName, wallet);
        return CompletableFuture.completedFuture(wallet);
    }

    @Override
    public CompletableFuture<WalletInfo> getWalletInfo(String keyName) {
        Objects.requireNonNull(keyName, "keyName must not be null");
        WalletInfo wallet = wallets.get(keyName);
        if (wallet == null) {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Wallet not found for key: " + keyName));
        }
        return CompletableFuture.completedFuture(wallet);
    }
}
