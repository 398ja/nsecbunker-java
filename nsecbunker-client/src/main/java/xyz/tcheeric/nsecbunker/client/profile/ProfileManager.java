package xyz.tcheeric.nsecbunker.client.profile;

import nostr.event.impl.GenericEvent;

import java.util.concurrent.CompletableFuture;

/**
 * Manages profile (kind 0) metadata using a remote signer.
 */
public interface ProfileManager {

    /**
     * Creates and signs a profile metadata event.
     *
     * @param metadata profile fields
     * @return a future with the signed event
     */
    CompletableFuture<GenericEvent> updateProfile(ProfileMetadata metadata);

    /**
     * Fetches the current profile metadata (not implemented without relay integration).
     *
     * @return a future that completes exceptionally if not supported
     */
    CompletableFuture<ProfileMetadata> fetchProfile();
}
