package xyz.tcheeric.nsecbunker.client.profile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Basic Nostr profile metadata for kind 0 events.
 *
 * <p>This class represents the standard profile fields defined in NIP-01
 * and related NIPs. It can be serialized to JSON for use in kind 0
 * (set_metadata) events.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * ProfileMetadata profile = ProfileMetadata.builder()
 *     .name("Alice")
 *     .about("Nostr enthusiast")
 *     .nip05("alice@example.com")
 *     .build();
 *
 * String json = profile.toJson(null);
 * }</pre>
 *
 * @see <a href="https://github.com/nostr-protocol/nips/blob/master/01.md">NIP-01</a>
 */
@Value
@Builder(toBuilder = true)
@Jacksonized
public class ProfileMetadata {

    /**
     * Display name for the profile.
     */
    String name;

    /**
     * User's bio or description.
     */
    String about;

    /**
     * URL to the user's profile picture.
     */
    String picture;

    /**
     * NIP-05 identifier (e.g., "alice@example.com").
     *
     * @see <a href="https://github.com/nostr-protocol/nips/blob/master/05.md">NIP-05</a>
     */
    String nip05;

    /**
     * Lightning address for payments (LUD-16).
     */
    String lud16;

    /**
     * User's website URL.
     */
    String website;

    /**
     * Serializes this profile metadata to JSON.
     *
     * @param mapper the ObjectMapper to use, or null to use a default instance
     * @return the JSON string representation
     * @throws IllegalStateException if serialization fails
     */
    public String toJson(ObjectMapper mapper) {
        ObjectMapper m = mapper != null ? mapper : new ObjectMapper();
        try {
            return m.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize profile metadata: " + e.getMessage(), e);
        }
    }
}
