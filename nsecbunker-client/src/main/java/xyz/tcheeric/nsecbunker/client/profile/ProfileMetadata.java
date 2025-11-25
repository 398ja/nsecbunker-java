package xyz.tcheeric.nsecbunker.client.profile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

/**
 * Basic Nostr profile metadata for kind 0 events.
 */
@Value
@Builder(toBuilder = true)
@Jacksonized
public class ProfileMetadata {
    String name;
    String about;
    String picture;
    String nip05;
    String lud16;
    String website;

    public String toJson(ObjectMapper mapper) {
        ObjectMapper m = mapper != null ? mapper : new ObjectMapper();
        try {
            return m.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize profile metadata: " + e.getMessage(), e);
        }
    }
}
