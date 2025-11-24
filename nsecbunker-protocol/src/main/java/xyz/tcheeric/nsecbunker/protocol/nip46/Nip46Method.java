package xyz.tcheeric.nsecbunker.protocol.nip46;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * NIP-46 remote signer methods.
 *
 * <p>These methods define the operations that can be requested from a
 * remote signer (nsecBunker) via the NIP-46 protocol.
 *
 * @see <a href="https://github.com/nostr-protocol/nips/blob/master/46.md">NIP-46</a>
 */
public enum Nip46Method {

    /**
     * Request to connect to the signer.
     * Params: [pubkey, secret?]
     * Result: "ack"
     */
    CONNECT("connect"),

    /**
     * Request the signer's public key.
     * Params: []
     * Result: pubkey (hex)
     */
    GET_PUBLIC_KEY("get_public_key"),

    /**
     * Request to sign an event.
     * Params: [event_json]
     * Result: signature (hex) or signed_event_json
     */
    SIGN_EVENT("sign_event"),

    /**
     * Request NIP-04 encryption.
     * Params: [third_party_pubkey, plaintext]
     * Result: ciphertext
     */
    NIP04_ENCRYPT("nip04_encrypt"),

    /**
     * Request NIP-04 decryption.
     * Params: [third_party_pubkey, ciphertext]
     * Result: plaintext
     */
    NIP04_DECRYPT("nip04_decrypt"),

    /**
     * Request NIP-44 encryption.
     * Params: [third_party_pubkey, plaintext]
     * Result: ciphertext
     */
    NIP44_ENCRYPT("nip44_encrypt"),

    /**
     * Request NIP-44 decryption.
     * Params: [third_party_pubkey, ciphertext]
     * Result: plaintext
     */
    NIP44_DECRYPT("nip44_decrypt"),

    /**
     * Ping the signer to check connectivity.
     * Params: []
     * Result: "pong"
     */
    PING("ping"),

    /**
     * Get relay recommendations from the signer.
     * Params: []
     * Result: [relay_urls]
     */
    GET_RELAYS("get_relays");

    private final String value;

    Nip46Method(String value) {
        this.value = value;
    }

    /**
     * Returns the wire format value of this method.
     *
     * @return the method name as used in the protocol
     */
    @JsonValue
    public String getValue() {
        return value;
    }

    /**
     * Parses a method from its wire format value.
     *
     * @param value the method name
     * @return the corresponding method
     * @throws IllegalArgumentException if the value is not recognized
     */
    public static Nip46Method fromValue(String value) {
        for (Nip46Method method : values()) {
            if (method.value.equals(value)) {
                return method;
            }
        }
        throw new IllegalArgumentException("Unknown NIP-46 method: " + value);
    }

    /**
     * Tries to parse a method from its wire format value.
     *
     * @param value the method name
     * @return the corresponding method, or null if not recognized
     */
    public static Nip46Method tryFromValue(String value) {
        for (Nip46Method method : values()) {
            if (method.value.equals(value)) {
                return method;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return value;
    }
}
