package xyz.tcheeric.nsecbunker.protocol.nip46;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

/**
 * Represents a NIP-46 response message.
 *
 * <p>NIP-46 responses are JSON-RPC-like messages with:
 * <ul>
 *   <li>{@code id} - Request identifier for correlation</li>
 *   <li>{@code result} - Success result (mutually exclusive with error)</li>
 *   <li>{@code error} - Error information (mutually exclusive with result)</li>
 * </ul>
 *
 * <p>Example success JSON:
 * <pre>{@code
 * {
 *   "id": "request-id",
 *   "result": "signature-hex"
 * }
 * }</pre>
 *
 * <p>Example error JSON:
 * <pre>{@code
 * {
 *   "id": "request-id",
 *   "error": {
 *     "code": "UNAUTHORIZED",
 *     "message": "Permission denied for sign_event"
 *   }
 * }
 * }</pre>
 *
 * <p>Usage:
 * <pre>{@code
 * // Check response
 * if (response.isSuccess()) {
 *     String result = response.getResult();
 * } else {
 *     Nip46Error error = response.getError();
 *     log.error("Error: {} - {}", error.getCode(), error.getMessage());
 * }
 * }</pre>
 *
 * @see Nip46Request
 * @see Nip46Error
 * @see <a href="https://github.com/nostr-protocol/nips/blob/master/46.md">NIP-46</a>
 */
@Getter
@Builder(toBuilder = true)
@Jacksonized
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Nip46Response {

    /**
     * Request identifier for request/response correlation.
     */
    @JsonProperty("id")
    private final String id;

    /**
     * Success result.
     * Mutually exclusive with error.
     */
    @JsonProperty("result")
    private final String result;

    /**
     * Error information.
     * Mutually exclusive with result.
     */
    @JsonProperty("error")
    private final Nip46Error error;

    /**
     * Returns whether this response indicates success.
     *
     * @return true if the response has a result and no error
     */
    @JsonIgnore
    public boolean isSuccess() {
        return error == null;
    }

    /**
     * Returns whether this response indicates an error.
     *
     * @return true if the response has an error
     */
    @JsonIgnore
    public boolean isError() {
        return error != null;
    }

    /**
     * Returns the result, throwing if this is an error response.
     *
     * @return the result
     * @throws IllegalStateException if this is an error response
     */
    @JsonIgnore
    public String getResultOrThrow() {
        if (isError()) {
            throw new IllegalStateException("Response is an error: " + error);
        }
        return result;
    }

    /**
     * Returns the error code if this is an error response.
     *
     * @return the error code, or null if success
     */
    @JsonIgnore
    public String getErrorCode() {
        return error != null ? error.getCode() : null;
    }

    /**
     * Returns the error message if this is an error response.
     *
     * @return the error message, or null if success
     */
    @JsonIgnore
    public String getErrorMessage() {
        return error != null ? error.getMessage() : null;
    }

    // ==================== Factory Methods ====================

    /**
     * Creates a success response.
     *
     * @param id     the request ID
     * @param result the result value
     * @return a success response
     */
    public static Nip46Response success(String id, String result) {
        return Nip46Response.builder()
                .id(id)
                .result(result)
                .build();
    }

    /**
     * Creates an error response.
     *
     * @param id    the request ID
     * @param error the error information
     * @return an error response
     */
    public static Nip46Response error(String id, Nip46Error error) {
        return Nip46Response.builder()
                .id(id)
                .error(error)
                .build();
    }

    /**
     * Creates an error response with code and message.
     *
     * @param id      the request ID
     * @param code    the error code
     * @param message the error message
     * @return an error response
     */
    public static Nip46Response error(String id, String code, String message) {
        return error(id, Nip46Error.of(code, message));
    }

    /**
     * Creates an "ack" success response (used for connect).
     *
     * @param id the request ID
     * @return an ack response
     */
    public static Nip46Response ack(String id) {
        return success(id, "ack");
    }

    /**
     * Creates a "pong" success response (used for ping).
     *
     * @param id the request ID
     * @return a pong response
     */
    public static Nip46Response pong(String id) {
        return success(id, "pong");
    }

    @Override
    public String toString() {
        if (isSuccess()) {
            return "Nip46Response{id='" + id + "', result='" + result + "'}";
        } else {
            return "Nip46Response{id='" + id + "', error=" + error + "}";
        }
    }
}
