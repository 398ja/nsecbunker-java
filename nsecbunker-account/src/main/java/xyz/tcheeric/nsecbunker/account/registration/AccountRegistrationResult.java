package xyz.tcheeric.nsecbunker.account.registration;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

/**
 * Result of an account registration operation.
 */
@Value
@Builder(toBuilder = true)
public class AccountRegistrationResult {
    String username;
    String domain;
    String nip05;
    String npub;
    String keyName;
    Instant createdAt;
}
