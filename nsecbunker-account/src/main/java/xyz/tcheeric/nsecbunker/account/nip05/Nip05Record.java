package xyz.tcheeric.nsecbunker.account.nip05;

import lombok.Builder;
import lombok.Value;

/**
 * Represents a NIP-05 record.
 */
@Value
@Builder(toBuilder = true)
public class Nip05Record {
    String nip05;
    String pubkey;
    String relaysJson;
}
