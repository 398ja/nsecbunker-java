package xyz.tcheeric.nsecbunker.protocol.nip46;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * Decodes NIP-46 requests and responses from JSON.
 *
 * <p>This decoder handles deserialization of JSON strings to
 * {@link Nip46Request} and {@link Nip46Response} objects.
 *
 * <p>Usage:
 * <pre>{@code
 * Nip46Decoder decoder = new Nip46Decoder();
 *
 * // Decode a request
 * String requestJson = "{\"id\":\"abc\",\"method\":\"ping\",\"params\":[]}";
 * Nip46Request request = decoder.decodeRequest(requestJson);
 *
 * // Decode a response
 * String responseJson = "{\"id\":\"abc\",\"result\":\"pong\"}";
 * Nip46Response response = decoder.decodeResponse(responseJson);
 *
 * // Try to decode (returns Optional)
 * Optional<Nip46Response> maybeResponse = decoder.tryDecodeResponse(json);
 * }</pre>
 *
 * @see Nip46Encoder
 * @see Nip46Request
 * @see Nip46Response
 */
@Slf4j
public class Nip46Decoder {

    private final ObjectMapper objectMapper;

    /**
     * Creates a new decoder with default ObjectMapper.
     */
    public Nip46Decoder() {
        this(new ObjectMapper());
    }

    /**
     * Creates a new decoder with a custom ObjectMapper.
     *
     * @param objectMapper the ObjectMapper to use
     */
    public Nip46Decoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    /**
     * Decodes a NIP-46 request from JSON.
     *
     * @param json the JSON string
     * @return the decoded request
     * @throws Nip46DecodingException if decoding fails
     */
    public Nip46Request decodeRequest(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("JSON must not be null or blank");
        }

        try {
            log.debug("Decoding request: {}", json);
            return objectMapper.readValue(json, Nip46Request.class);
        } catch (JsonProcessingException e) {
            throw new Nip46DecodingException("Failed to decode request: " + e.getMessage(), e);
        }
    }

    /**
     * Tries to decode a NIP-46 request from JSON.
     *
     * @param json the JSON string
     * @return the decoded request, or empty if decoding fails
     */
    public Optional<Nip46Request> tryDecodeRequest(String json) {
        try {
            return Optional.of(decodeRequest(json));
        } catch (Exception e) {
            log.debug("Failed to decode request: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Decodes a NIP-46 response from JSON.
     *
     * <p>This method handles both object-style errors and string-style errors:
     * <ul>
     *   <li>Object style: {@code {"id":"...", "error": {"code":"...", "message":"..."}}}</li>
     *   <li>String style: {@code {"id":"...", "result":"error", "error": "Error message"}}</li>
     * </ul>
     *
     * @param json the JSON string
     * @return the decoded response
     * @throws Nip46DecodingException if decoding fails
     */
    public Nip46Response decodeResponse(String json) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("JSON must not be null or blank");
        }

        try {
            log.debug("Decoding response: {}", json);

            // First, try to parse as JSON tree to handle both string and object errors
            JsonNode node = objectMapper.readTree(json);
            String id = node.has("id") && !node.get("id").isNull() ? node.get("id").asText() : null;
            String result = node.has("result") && !node.get("result").isNull() ? node.get("result").asText() : null;

            // Check if there's an error field
            if (node.has("error") && !node.get("error").isNull()) {
                JsonNode errorNode = node.get("error");
                Nip46Error error;

                if (errorNode.isTextual()) {
                    // String-style error: {"error": "Error message"} or {"result":"error", "error":"message"}
                    error = Nip46Error.of("ERROR", errorNode.asText());
                } else if (errorNode.isObject()) {
                    // Object-style error: {"error": {"code":"...", "message":"..."}}
                    String code = errorNode.has("code") ? errorNode.get("code").asText() : "ERROR";
                    String message = errorNode.has("message") ? errorNode.get("message").asText() : "Unknown error";
                    error = Nip46Error.of(code, message);
                } else {
                    error = Nip46Error.of("ERROR", errorNode.toString());
                }

                return Nip46Response.error(id, error);
            }

            // No error, return success response
            return Nip46Response.success(id, result);

        } catch (JsonProcessingException e) {
            throw new Nip46DecodingException("Failed to decode response: " + e.getMessage(), e);
        }
    }

    /**
     * Tries to decode a NIP-46 response from JSON.
     *
     * @param json the JSON string
     * @return the decoded response, or empty if decoding fails
     */
    public Optional<Nip46Response> tryDecodeResponse(String json) {
        try {
            return Optional.of(decodeResponse(json));
        } catch (Exception e) {
            log.debug("Failed to decode response: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Determines if the JSON represents a request (has "method" field).
     *
     * @param json the JSON string
     * @return true if the JSON appears to be a request
     */
    public boolean isRequest(String json) {
        if (json == null || json.isBlank()) {
            return false;
        }

        try {
            JsonNode node = objectMapper.readTree(json);
            return node.has("method");
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    /**
     * Determines if the JSON represents a response (has "result" or "error" field).
     *
     * @param json the JSON string
     * @return true if the JSON appears to be a response
     */
    public boolean isResponse(String json) {
        if (json == null || json.isBlank()) {
            return false;
        }

        try {
            JsonNode node = objectMapper.readTree(json);
            return node.has("result") || node.has("error");
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    /**
     * Extracts the request/response ID from JSON without full parsing.
     *
     * @param json the JSON string
     * @return the ID, or empty if not found
     */
    public Optional<String> extractId(String json) {
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }

        try {
            JsonNode node = objectMapper.readTree(json);
            JsonNode idNode = node.get("id");
            if (idNode != null && idNode.isTextual()) {
                return Optional.of(idNode.asText());
            }
            return Optional.empty();
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }

    /**
     * Extracts the method name from a request JSON without full parsing.
     *
     * @param json the JSON string
     * @return the method, or empty if not found
     */
    public Optional<String> extractMethod(String json) {
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }

        try {
            JsonNode node = objectMapper.readTree(json);
            JsonNode methodNode = node.get("method");
            if (methodNode != null && methodNode.isTextual()) {
                return Optional.of(methodNode.asText());
            }
            return Optional.empty();
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }

    /**
     * Decodes any object from JSON.
     *
     * @param json  the JSON string
     * @param clazz the target class
     * @param <T>   the target type
     * @return the decoded object
     * @throws Nip46DecodingException if decoding fails
     */
    public <T> T decode(String json, Class<T> clazz) {
        if (json == null || json.isBlank()) {
            throw new IllegalArgumentException("JSON must not be null or blank");
        }

        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new Nip46DecodingException("Failed to decode: " + e.getMessage(), e);
        }
    }

    /**
     * Exception thrown when JSON decoding fails.
     */
    public static class Nip46DecodingException extends RuntimeException {

        /**
         * Creates a new decoding exception.
         *
         * @param message the error message
         * @param cause   the underlying cause
         */
        public Nip46DecodingException(String message, Throwable cause) {
            super(message, cause);
        }

        /**
         * Creates a new decoding exception.
         *
         * @param message the error message
         */
        public Nip46DecodingException(String message) {
            super(message);
        }
    }
}
