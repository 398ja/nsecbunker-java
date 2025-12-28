package xyz.tcheeric.nsecbunker.protocol.nip46;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a NIP-46 request message.
 *
 * <p>NIP-46 requests are JSON-RPC-like messages with:
 * <ul>
 *   <li>{@code id} - Unique request identifier for correlation</li>
 *   <li>{@code method} - The operation to perform</li>
 *   <li>{@code params} - Parameters for the operation</li>
 * </ul>
 *
 * <p>Example JSON:
 * <pre>{@code
 * {
 *   "id": "random-id",
 *   "method": "sign_event",
 *   "params": ["{\"kind\":1,\"content\":\"Hello\",\"tags\":[]}"]
 * }
 * }</pre>
 *
 * <p>Usage:
 * <pre>{@code
 * // Create a sign_event request
 * Nip46Request request = Nip46Request.builder()
 *     .method(Nip46Method.SIGN_EVENT)
 *     .params(List.of(eventJson))
 *     .build();
 *
 * // Create using factory methods
 * Nip46Request pingRequest = Nip46Request.ping();
 * Nip46Request signRequest = Nip46Request.signEvent(eventJson);
 * }</pre>
 *
 * @see Nip46Method
 * @see Nip46Response
 * @see <a href="https://github.com/nostr-protocol/nips/blob/master/46.md">NIP-46</a>
 */
@Getter
@Builder(toBuilder = true)
@Jacksonized
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Nip46Request {

    /**
     * Unique request identifier for request/response correlation.
     * If not provided, a random UUID will be generated.
     */
    @JsonProperty("id")
    @Builder.Default
    private final String id = UUID.randomUUID().toString();

    /**
     * The NIP-46 method to invoke.
     */
    @JsonProperty("method")
    private final String method;

    /**
     * Parameters for the method.
     * May be empty but never null.
     * Can contain String, Integer, Long, or other JSON-serializable types.
     */
    @JsonProperty("params")
    @Builder.Default
    private final List<Object> params = Collections.emptyList();

    /**
     * Returns the method as a Nip46Method enum.
     *
     * @return the method enum, or null if not a recognized method
     */
    @JsonIgnore
    public Nip46Method getMethodEnum() {
        return Nip46Method.tryFromValue(method);
    }

    /**
     * Returns whether this request has a recognized method.
     *
     * @return true if the method is a known NIP-46 method
     */
    @JsonIgnore
    public boolean hasKnownMethod() {
        return getMethodEnum() != null;
    }

    /**
     * Returns the first parameter, or null if no parameters.
     *
     * @return the first parameter
     */
    @JsonIgnore
    public String getFirstParam() {
        return params != null && !params.isEmpty() ? String.valueOf(params.get(0)) : null;
    }

    /**
     * Returns the second parameter, or null if fewer than 2 parameters.
     *
     * @return the second parameter
     */
    @JsonIgnore
    public String getSecondParam() {
        return params != null && params.size() > 1 ? String.valueOf(params.get(1)) : null;
    }

    // ==================== Factory Methods ====================

    /**
     * Creates a connect request.
     *
     * @param pubkey the client's public key (hex)
     * @return a connect request
     */
    public static Nip46Request connect(String pubkey) {
        return connect(pubkey, null);
    }

    /**
     * Creates a connect request with optional secret.
     *
     * @param pubkey the client's public key (hex)
     * @param secret optional secret from connection string
     * @return a connect request
     */
    public static Nip46Request connect(String pubkey, String secret) {
        Objects.requireNonNull(pubkey, "pubkey must not be null");
        List<Object> params = secret != null
                ? Arrays.asList(pubkey, secret)
                : Collections.singletonList(pubkey);
        return Nip46Request.builder()
                .method(Nip46Method.CONNECT.getValue())
                .params(params)
                .build();
    }

    /**
     * Creates a get_public_key request.
     *
     * @return a get_public_key request
     */
    public static Nip46Request getPublicKey() {
        return Nip46Request.builder()
                .method(Nip46Method.GET_PUBLIC_KEY.getValue())
                .build();
    }

    /**
     * Creates a sign_event request.
     *
     * @param eventJson the unsigned event JSON
     * @return a sign_event request
     */
    public static Nip46Request signEvent(String eventJson) {
        Objects.requireNonNull(eventJson, "eventJson must not be null");
        return Nip46Request.builder()
                .method(Nip46Method.SIGN_EVENT.getValue())
                .params(Collections.singletonList(eventJson))
                .build();
    }

    /**
     * Creates a nip04_encrypt request.
     *
     * @param thirdPartyPubkey the recipient's public key (hex)
     * @param plaintext        the text to encrypt
     * @return a nip04_encrypt request
     */
    public static Nip46Request nip04Encrypt(String thirdPartyPubkey, String plaintext) {
        Objects.requireNonNull(thirdPartyPubkey, "thirdPartyPubkey must not be null");
        Objects.requireNonNull(plaintext, "plaintext must not be null");
        return Nip46Request.builder()
                .method(Nip46Method.NIP04_ENCRYPT.getValue())
                .params(Arrays.asList(thirdPartyPubkey, plaintext))
                .build();
    }

    /**
     * Creates a nip04_decrypt request.
     *
     * @param thirdPartyPubkey the sender's public key (hex)
     * @param ciphertext       the ciphertext to decrypt
     * @return a nip04_decrypt request
     */
    public static Nip46Request nip04Decrypt(String thirdPartyPubkey, String ciphertext) {
        Objects.requireNonNull(thirdPartyPubkey, "thirdPartyPubkey must not be null");
        Objects.requireNonNull(ciphertext, "ciphertext must not be null");
        return Nip46Request.builder()
                .method(Nip46Method.NIP04_DECRYPT.getValue())
                .params(Arrays.asList(thirdPartyPubkey, ciphertext))
                .build();
    }

    /**
     * Creates a nip44_encrypt request.
     *
     * @param thirdPartyPubkey the recipient's public key (hex)
     * @param plaintext        the text to encrypt
     * @return a nip44_encrypt request
     */
    public static Nip46Request nip44Encrypt(String thirdPartyPubkey, String plaintext) {
        Objects.requireNonNull(thirdPartyPubkey, "thirdPartyPubkey must not be null");
        Objects.requireNonNull(plaintext, "plaintext must not be null");
        return Nip46Request.builder()
                .method(Nip46Method.NIP44_ENCRYPT.getValue())
                .params(Arrays.asList(thirdPartyPubkey, plaintext))
                .build();
    }

    /**
     * Creates a nip44_decrypt request.
     *
     * @param thirdPartyPubkey the sender's public key (hex)
     * @param ciphertext       the ciphertext to decrypt
     * @return a nip44_decrypt request
     */
    public static Nip46Request nip44Decrypt(String thirdPartyPubkey, String ciphertext) {
        Objects.requireNonNull(thirdPartyPubkey, "thirdPartyPubkey must not be null");
        Objects.requireNonNull(ciphertext, "ciphertext must not be null");
        return Nip46Request.builder()
                .method(Nip46Method.NIP44_DECRYPT.getValue())
                .params(Arrays.asList(thirdPartyPubkey, ciphertext))
                .build();
    }

    /**
     * Creates a ping request.
     *
     * @return a ping request
     */
    public static Nip46Request ping() {
        return Nip46Request.builder()
                .method(Nip46Method.PING.getValue())
                .build();
    }

    /**
     * Creates a get_relays request.
     *
     * @return a get_relays request
     */
    public static Nip46Request getRelays() {
        return Nip46Request.builder()
                .method(Nip46Method.GET_RELAYS.getValue())
                .build();
    }

    @Override
    public String toString() {
        return "Nip46Request{id='" + id + "', method='" + method + "', params=" + params + "}";
    }
}
