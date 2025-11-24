package xyz.tcheeric.nsecbunker.admin.token;

import xyz.tcheeric.nsecbunker.core.model.AccessToken;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Manages bunker access tokens through the admin NIP-46 interface.
 */
public interface TokenManager {

    /**
    * Creates a new access token for a key.
    *
    * @param keyName    the key name
    * @param clientName human-readable client name
    * @param policyId   optional policy id to apply
    * @param lifetime   optional token lifetime (null for no expiry)
    * @return a future with the created token metadata
    */
    CompletableFuture<AccessToken> createToken(String keyName, String clientName, String policyId, Duration lifetime);

    /**
     * Lists all tokens for a key.
     *
     * @param keyName the key name
     * @return a future with tokens
     */
    CompletableFuture<List<AccessToken>> listTokens(String keyName);

    /**
     * Gets a single token by id.
     *
     * @param tokenId the token id
     * @return a future with the token
     */
    CompletableFuture<AccessToken> getToken(String tokenId);

    /**
     * Revokes a token by id.
     *
     * @param tokenId the token id
     * @return a future that completes with true on success
     */
    CompletableFuture<Boolean> revokeToken(String tokenId);

    /**
     * Validates a token string.
     *
     * @param token the token value
     * @return a future that completes with true if valid
     */
    CompletableFuture<Boolean> validateToken(String token);
}
