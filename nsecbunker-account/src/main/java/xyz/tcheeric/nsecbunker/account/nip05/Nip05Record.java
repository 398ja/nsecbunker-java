package xyz.tcheeric.nsecbunker.account.nip05;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Value;
import xyz.tcheeric.nsecbunker.core.model.BunkerKey;

import java.util.List;
import java.util.Objects;

/**
 * Represents a NIP-05 record mapping a username@domain identifier to a Nostr public key.
 *
 * <p>NIP-05 is the Nostr Implementation Possibility for mapping internet identifiers
 * to public keys. This record stores the identifier, public key, and optional relay
 * information.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * Nip05Record record = Nip05Record.builder()
 *     .nip05("alice@example.com")
 *     .pubkey("79be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798")
 *     .relaysJson("[\"wss://relay.example.com\"]")
 *     .build();
 * }</pre>
 *
 * @see <a href="https://github.com/nostr-protocol/nips/blob/master/05.md">NIP-05 Specification</a>
 */
@Value
@Builder(toBuilder = true)
public class Nip05Record {

    private static final ObjectMapper DEFAULT_MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {};

    /**
     * The NIP-05 identifier (e.g., "alice@example.com").
     */
    String nip05;

    /**
     * The Nostr public key in hex format (64 characters).
     */
    String pubkey;

    /**
     * The relay list as a JSON array string (e.g., "[\"wss://relay.example.com\"]").
     */
    String relaysJson;

    /**
     * Returns the relay URLs as a parsed list.
     *
     * @return list of relay URLs, empty list if relaysJson is null or empty
     */
    @JsonIgnore
    public List<String> getRelays() {
        if (relaysJson == null || relaysJson.isBlank() || "[]".equals(relaysJson)) {
            return List.of();
        }
        try {
            return DEFAULT_MAPPER.readValue(relaysJson, STRING_LIST_TYPE);
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    /**
     * Extracts the username portion of the NIP-05 identifier.
     *
     * @return the username (e.g., "alice" from "alice@example.com")
     */
    @JsonIgnore
    public String getUsername() {
        if (nip05 == null || !nip05.contains("@")) {
            return null;
        }
        return nip05.split("@")[0];
    }

    /**
     * Extracts the domain portion of the NIP-05 identifier.
     *
     * @return the domain (e.g., "example.com" from "alice@example.com")
     */
    @JsonIgnore
    public String getDomain() {
        if (nip05 == null || !nip05.contains("@")) {
            return null;
        }
        String[] parts = nip05.split("@");
        return parts.length > 1 ? parts[1] : null;
    }

    /**
     * Checks if this record has any relay information.
     *
     * @return true if relays are configured
     */
    @JsonIgnore
    public boolean hasRelays() {
        return !getRelays().isEmpty();
    }

    /**
     * Creates a NIP-05 record from a BunkerKey.
     *
     * <p>Uses the key's hex public key and creates the NIP-05 identifier
     * from the provided username and domain.</p>
     *
     * @param key      the bunker key containing the public key
     * @param username the username for the NIP-05 identifier
     * @param domain   the domain for the NIP-05 identifier
     * @return a new Nip05Record
     * @throws NullPointerException if any parameter is null
     */
    public static Nip05Record fromBunkerKey(BunkerKey key, String username, String domain) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(username, "username must not be null");
        Objects.requireNonNull(domain, "domain must not be null");

        return Nip05Record.builder()
                .nip05(username + "@" + domain)
                .pubkey(key.getPubkeyHex() != null ? key.getPubkeyHex() : key.getNpub())
                .relaysJson("[]")
                .build();
    }

    /**
     * Creates a NIP-05 record from a BunkerKey with relay information.
     *
     * @param key      the bunker key containing the public key
     * @param username the username for the NIP-05 identifier
     * @param domain   the domain for the NIP-05 identifier
     * @param relays   list of relay URLs
     * @return a new Nip05Record
     * @throws NullPointerException if key, username, or domain is null
     */
    public static Nip05Record fromBunkerKey(BunkerKey key, String username, String domain, List<String> relays) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(username, "username must not be null");
        Objects.requireNonNull(domain, "domain must not be null");

        String relaysJsonStr = "[]";
        if (relays != null && !relays.isEmpty()) {
            try {
                relaysJsonStr = DEFAULT_MAPPER.writeValueAsString(relays);
            } catch (JsonProcessingException e) {
                relaysJsonStr = "[]";
            }
        }

        return Nip05Record.builder()
                .nip05(username + "@" + domain)
                .pubkey(key.getPubkeyHex() != null ? key.getPubkeyHex() : key.getNpub())
                .relaysJson(relaysJsonStr)
                .build();
    }

    /**
     * Creates a NIP-05 record with the specified parameters.
     *
     * @param username the username
     * @param domain   the domain
     * @param pubkey   the public key in hex format
     * @return a new Nip05Record
     */
    public static Nip05Record of(String username, String domain, String pubkey) {
        return Nip05Record.builder()
                .nip05(username + "@" + domain)
                .pubkey(pubkey)
                .relaysJson("[]")
                .build();
    }

    /**
     * Creates a NIP-05 record with relays.
     *
     * @param username the username
     * @param domain   the domain
     * @param pubkey   the public key in hex format
     * @param relays   list of relay URLs
     * @return a new Nip05Record
     */
    public static Nip05Record of(String username, String domain, String pubkey, List<String> relays) {
        String relaysJsonStr = "[]";
        if (relays != null && !relays.isEmpty()) {
            try {
                relaysJsonStr = DEFAULT_MAPPER.writeValueAsString(relays);
            } catch (JsonProcessingException e) {
                relaysJsonStr = "[]";
            }
        }

        return Nip05Record.builder()
                .nip05(username + "@" + domain)
                .pubkey(pubkey)
                .relaysJson(relaysJsonStr)
                .build();
    }
}
