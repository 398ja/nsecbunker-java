package xyz.tcheeric.nsecbunker.admin.permission;

import xyz.tcheeric.nsecbunker.core.model.BunkerPolicy;
import xyz.tcheeric.nsecbunker.core.model.KeyUser;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Manages key user permissions via the admin NIP-46 interface.
 */
public interface PermissionManager {

    /**
     * Grants permission to a user for a key using a policy.
     *
     * @param keyName    the key name
     * @param userPubkey the user's pubkey (hex or npub)
     * @param policy     the policy to apply
     * @return a future with the created KeyUser
     */
    CompletableFuture<KeyUser> grantPermission(String keyName, String userPubkey, BunkerPolicy policy);

    /**
     * Revokes a user's permission for a key.
     *
     * @param keyName    the key name
     * @param userPubkey the user's pubkey
     * @return a future that completes with true on success
     */
    CompletableFuture<Boolean> revokePermission(String keyName, String userPubkey);

    /**
     * Lists all users for a key.
     *
     * @param keyName the key name
     * @return a future with users
     */
    CompletableFuture<List<KeyUser>> listKeyUsers(String keyName);

    /**
     * Gets permissions for a specific user.
     *
     * @param keyName    the key name
     * @param userPubkey the user pubkey
     * @return a future with the user permissions
     */
    CompletableFuture<KeyUser> getPermissions(String keyName, String userPubkey);

    /**
     * Updates the description/metadata for a key user.
     *
     * @param keyName     the key name
     * @param userPubkey  the user pubkey
     * @param description new description
     * @return a future with the updated KeyUser
     */
    CompletableFuture<KeyUser> updateKeyUserDescription(String keyName, String userPubkey, String description);
}
