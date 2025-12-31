package xyz.tcheeric.nsecbunker.admin;

import lombok.extern.slf4j.Slf4j;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Request;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Handles admin authentication validation with nsecBunker.
 *
 * <p>This class verifies that the admin's identity is authorized to perform
 * administrative operations on the bunker. It sends a connect request with
 * the admin's public key and validates the response.
 *
 * <p>Usage:
 * <pre>{@code
 * AdminAuthenticator authenticator = new AdminAuthenticator(client);
 * boolean authorized = authenticator.validateAdminAccess().join();
 * if (!authorized) {
 *     throw new AdminException("Admin access denied");
 * }
 * }</pre>
 */
@Slf4j
public class AdminAuthenticator {

    private final NsecBunkerAdminClient client;

    /**
     * Creates a new authenticator for the given admin client.
     *
     * @param client the admin client
     */
    public AdminAuthenticator(NsecBunkerAdminClient client) {
        this.client = client;
    }

    /**
     * Validates that the admin has access to the bunker.
     *
     * <p>This sends a connect request with the admin's public key and
     * verifies that the bunker accepts the connection.
     *
     * @return a future that completes with true if access is granted
     */
    public CompletableFuture<Boolean> validateAdminAccess() {
        return validateAdminAccess(null);
    }

    /**
     * Validates that the admin has access to the bunker with optional permissions.
     *
     * @param permissions optional list of permissions to request (currently ignored)
     * @return a future that completes with true if access is granted
     */
    public CompletableFuture<Boolean> validateAdminAccess(List<String> permissions) {
        String adminPubkey = client.getCommunicationIdentity().getPublicKey().toString();

        log.info("Validating admin access for pubkey: {}", adminPubkey);

        // Build connect request
        Nip46Request connectRequest = Nip46Request.connect(
                adminPubkey,
                client.getConfig().getSecret()
        );

        return client.sendRequest(connectRequest)
                .thenApply(response -> {
                    if (response.isError()) {
                        log.warn("Admin access denied: {}", response.getError());
                        return false;
                    }

                    String result = response.getResult();
                    if ("ack".equalsIgnoreCase(result)) {
                        log.info("Admin access granted");
                        return true;
                    }

                    log.warn("Unexpected connect response: {}", result);
                    return false;
                })
                .exceptionally(ex -> {
                    log.error("Failed to validate admin access: {}", ex.getMessage());
                    return false;
                });
    }

    /**
     * Validates admin access synchronously with a timeout.
     *
     * @param timeoutSeconds the timeout in seconds
     * @return true if access is granted
     * @throws AdminException if validation fails or times out
     */
    public boolean validateAdminAccessSync(int timeoutSeconds) {
        try {
            Boolean result = validateAdminAccess()
                    .get(timeoutSeconds, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(result);
        } catch (Exception e) {
            throw new AdminException("Admin authentication failed: " + e.getMessage(), e);
        }
    }

    /**
     * Gets the admin's public key in hex format.
     *
     * @return the admin public key hex
     */
    public String getAdminPublicKeyHex() {
        return client.getCommunicationIdentity().getPublicKey().toString();
    }

    /**
     * Checks if the admin identity is using an ephemeral key.
     *
     * @return true if using ephemeral key for communication
     */
    public boolean isUsingEphemeralKey() {
        return client.getConfig().isUseEphemeralKey();
    }

    /**
     * Verifies the admin's public key matches the expected value.
     *
     * @param expectedPubkey the expected public key (hex or npub)
     * @return true if the keys match
     */
    public boolean verifyAdminPubkey(String expectedPubkey) {
        String actualPubkey = getAdminPublicKeyHex();

        // Handle npub format
        if (expectedPubkey.startsWith("npub1")) {
            try {
                expectedPubkey = nostr.crypto.bech32.Bech32.fromBech32(expectedPubkey);
            } catch (Exception e) {
                log.warn("Failed to decode npub: {}", e.getMessage());
                return false;
            }
        }

        return actualPubkey.equalsIgnoreCase(expectedPubkey);
    }

    /**
     * Pings the bunker to verify connectivity.
     *
     * @return a future that completes with the ping latency in milliseconds
     */
    public CompletableFuture<Long> ping() {
        long startTime = System.currentTimeMillis();

        return client.ping()
                .thenApply(result -> System.currentTimeMillis() - startTime);
    }

    /**
     * Pings the bunker synchronously.
     *
     * @param timeoutSeconds the timeout in seconds
     * @return the ping latency in milliseconds
     * @throws AdminException if ping fails
     */
    public long pingSync(int timeoutSeconds) {
        try {
            return ping().get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new AdminException("Ping failed: " + e.getMessage(), e);
        }
    }
}
