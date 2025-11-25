package xyz.tcheeric.nsecbunker.account.wallet;

import lombok.Builder;
import lombok.Value;

/**
 * Minimal wallet information for LNBits integration.
 */
@Value
@Builder(toBuilder = true)
public class WalletInfo {
    String keyName;
    String walletId;
    String adminKey;
    String invoiceKey;
}
