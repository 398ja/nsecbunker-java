package xyz.tcheeric.nsecbunker.protocol.nip46;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

/**
 * Encodes NIP-46 requests and responses to JSON.
 *
 * <p>This encoder handles serialization of {@link Nip46Request} and
 * {@link Nip46Response} objects to JSON strings suitable for transmission
 * over the NIP-46 protocol.
 *
 * <p>Usage:
 * <pre>{@code
 * Nip46Encoder encoder = new Nip46Encoder();
 *
 * // Encode a request
 * Nip46Request request = Nip46Request.ping();
 * String json = encoder.encodeRequest(request);
 * // {"id":"...","method":"ping","params":[]}
 *
 * // Encode a response
 * Nip46Response response = Nip46Response.pong(request.getId());
 * String responseJson = encoder.encodeResponse(response);
 * // {"id":"...","result":"pong"}
 * }</pre>
 *
 * @see Nip46Decoder
 * @see Nip46Request
 * @see Nip46Response
 */
@Slf4j
public class Nip46Encoder {

    private final ObjectMapper objectMapper;

    /**
     * Creates a new encoder with default ObjectMapper.
     */
    public Nip46Encoder() {
        this(new ObjectMapper());
    }

    /**
     * Creates a new encoder with a custom ObjectMapper.
     *
     * @param objectMapper the ObjectMapper to use
     */
    public Nip46Encoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    /**
     * Encodes a NIP-46 request to JSON.
     *
     * @param request the request to encode
     * @return the JSON string
     * @throws Nip46EncodingException if encoding fails
     */
    public String encodeRequest(Nip46Request request) {
        if (request == null) {
            throw new IllegalArgumentException("Request must not be null");
        }

        try {
            String json = objectMapper.writeValueAsString(request);
            log.debug("Encoded request: {}", json);
            return json;
        } catch (JsonProcessingException e) {
            throw new Nip46EncodingException("Failed to encode request", e);
        }
    }

    /**
     * Encodes a NIP-46 response to JSON.
     *
     * @param response the response to encode
     * @return the JSON string
     * @throws Nip46EncodingException if encoding fails
     */
    public String encodeResponse(Nip46Response response) {
        if (response == null) {
            throw new IllegalArgumentException("Response must not be null");
        }

        try {
            String json = objectMapper.writeValueAsString(response);
            log.debug("Encoded response: {}", json);
            return json;
        } catch (JsonProcessingException e) {
            throw new Nip46EncodingException("Failed to encode response", e);
        }
    }

    /**
     * Encodes any object to JSON.
     *
     * <p>This can be used for custom request/response types.
     *
     * @param object the object to encode
     * @return the JSON string
     * @throws Nip46EncodingException if encoding fails
     */
    public String encode(Object object) {
        if (object == null) {
            throw new IllegalArgumentException("Object must not be null");
        }

        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new Nip46EncodingException("Failed to encode object", e);
        }
    }

    /**
     * Exception thrown when JSON encoding fails.
     */
    public static class Nip46EncodingException extends RuntimeException {

        /**
         * Creates a new encoding exception.
         *
         * @param message the error message
         * @param cause   the underlying cause
         */
        public Nip46EncodingException(String message, Throwable cause) {
            super(message, cause);
        }

        /**
         * Creates a new encoding exception.
         *
         * @param message the error message
         */
        public Nip46EncodingException(String message) {
            super(message);
        }
    }
}
