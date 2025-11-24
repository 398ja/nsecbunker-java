package xyz.tcheeric.nsecbunker.admin.policy;

import xyz.tcheeric.nsecbunker.core.model.BunkerPolicy;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Manages bunker policies through the admin NIP-46 interface.
 *
 * <p>Provides asynchronous CRUD operations for policy definitions that
 * control access to keys, users, and tokens.</p>
 */
public interface PolicyManager {

    /**
     * Creates a new policy.
     *
     * @param policy the policy definition to create
     * @return a future that completes with the created policy (with id)
     */
    CompletableFuture<BunkerPolicy> createPolicy(BunkerPolicy policy);

    /**
     * Lists all policies registered in the bunker.
     *
     * @return a future that completes with all policies
     */
    CompletableFuture<List<BunkerPolicy>> listPolicies();

    /**
     * Retrieves a single policy by identifier.
     *
     * @param policyId the policy id
     * @return a future that completes with the requested policy
     */
    CompletableFuture<BunkerPolicy> getPolicy(String policyId);

    /**
     * Deletes a policy.
     *
     * @param policyId the policy id
     * @return a future that completes with true on success
     */
    CompletableFuture<Boolean> deletePolicy(String policyId);
}
