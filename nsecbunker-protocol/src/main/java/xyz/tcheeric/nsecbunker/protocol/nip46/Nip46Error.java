package xyz.tcheeric.nsecbunker.protocol.nip46;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

/**
 * Represents an error in a NIP-46 response.
 *
 * <p>Errors contain a code and message describing what went wrong.
 * Common error codes include:
 * <ul>
 *   <li>{@code "UNAUTHORIZED"} - Permission denied</li>
 *   <li>{@code "INVALID_REQUEST"} - Malformed request</li>
 *   <li>{@code "TIMEOUT"} - Request timed out</li>
 *   <li>{@code "INTERNAL_ERROR"} - Server error</li>
 * </ul>
 */
@Getter
@Builder(toBuilder = true)
@Jacksonized
@EqualsAndHashCode
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Nip46Error {

    /**
     * Error code identifying the type of error.
     */
    @JsonProperty("code")
    private final String code;

    /**
     * Human-readable error message.
     */
    @JsonProperty("message")
    private final String message;

    /**
     * Creates an error with a code and message.
     *
     * @param code    the error code
     * @param message the error message
     * @return a new error
     */
    public static Nip46Error of(String code, String message) {
        return Nip46Error.builder()
                .code(code)
                .message(message)
                .build();
    }

    /**
     * Creates an unauthorized error.
     *
     * @param message the error message
     * @return an unauthorized error
     */
    public static Nip46Error unauthorized(String message) {
        return of("UNAUTHORIZED", message);
    }

    /**
     * Creates an invalid request error.
     *
     * @param message the error message
     * @return an invalid request error
     */
    public static Nip46Error invalidRequest(String message) {
        return of("INVALID_REQUEST", message);
    }

    /**
     * Creates a timeout error.
     *
     * @param message the error message
     * @return a timeout error
     */
    public static Nip46Error timeout(String message) {
        return of("TIMEOUT", message);
    }

    /**
     * Creates an internal error.
     *
     * @param message the error message
     * @return an internal error
     */
    public static Nip46Error internalError(String message) {
        return of("INTERNAL_ERROR", message);
    }

    @Override
    public String toString() {
        return "Nip46Error{code='" + code + "', message='" + message + "'}";
    }
}
