package xyz.tcheeric.nsecbunker.protocol.nip46;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Nip46Decoder}.
 */
class Nip46DecoderTest {

    private Nip46Decoder decoder;

    @BeforeEach
    void setUp() {
        decoder = new Nip46Decoder();
    }

    @Test
    void constructorWithNullObjectMapperUsesDefault() {
        Nip46Decoder decoder = new Nip46Decoder(null);
        assertNotNull(decoder);
    }

    @Test
    void decodeRequestParsesValidJson() {
        String json = "{\"id\":\"test-id\",\"method\":\"ping\",\"params\":[]}";

        Nip46Request request = decoder.decodeRequest(json);

        assertEquals("test-id", request.getId());
        assertEquals("ping", request.getMethod());
        assertTrue(request.getParams().isEmpty());
    }

    @Test
    void decodeRequestWithParams() {
        String json = "{\"id\":\"test-id\",\"method\":\"sign_event\",\"params\":[\"event-json\"]}";

        Nip46Request request = decoder.decodeRequest(json);

        assertEquals("sign_event", request.getMethod());
        assertEquals(1, request.getParams().size());
        assertEquals("event-json", request.getFirstParam());
    }

    @Test
    void decodeRequestWithNullThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                decoder.decodeRequest(null));
    }

    @Test
    void decodeRequestWithBlankThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                decoder.decodeRequest("   "));
    }

    @Test
    void decodeRequestWithInvalidJsonThrows() {
        assertThrows(Nip46Decoder.Nip46DecodingException.class, () ->
                decoder.decodeRequest("not valid json"));
    }

    @Test
    void tryDecodeRequestReturnsRequestForValidJson() {
        String json = "{\"id\":\"test-id\",\"method\":\"ping\",\"params\":[]}";

        Optional<Nip46Request> result = decoder.tryDecodeRequest(json);

        assertTrue(result.isPresent());
        assertEquals("ping", result.get().getMethod());
    }

    @Test
    void tryDecodeRequestReturnsEmptyForInvalidJson() {
        Optional<Nip46Request> result = decoder.tryDecodeRequest("invalid");

        assertTrue(result.isEmpty());
    }

    @Test
    void tryDecodeRequestReturnsEmptyForNull() {
        Optional<Nip46Request> result = decoder.tryDecodeRequest(null);

        assertTrue(result.isEmpty());
    }

    @Test
    void decodeResponseSuccessParsesValidJson() {
        String json = "{\"id\":\"test-id\",\"result\":\"pong\"}";

        Nip46Response response = decoder.decodeResponse(json);

        assertEquals("test-id", response.getId());
        assertEquals("pong", response.getResult());
        assertTrue(response.isSuccess());
    }

    @Test
    void decodeResponseErrorParsesValidJson() {
        String json = "{\"id\":\"test-id\",\"error\":{\"code\":\"ERROR\",\"message\":\"Something went wrong\"}}";

        Nip46Response response = decoder.decodeResponse(json);

        assertEquals("test-id", response.getId());
        assertTrue(response.isError());
        assertEquals("ERROR", response.getErrorCode());
        assertEquals("Something went wrong", response.getErrorMessage());
    }

    @Test
    void decodeResponseWithNullThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                decoder.decodeResponse(null));
    }

    @Test
    void decodeResponseWithBlankThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                decoder.decodeResponse(""));
    }

    @Test
    void decodeResponseWithInvalidJsonThrows() {
        assertThrows(Nip46Decoder.Nip46DecodingException.class, () ->
                decoder.decodeResponse("{broken"));
    }

    @Test
    void tryDecodeResponseReturnsResponseForValidJson() {
        String json = "{\"id\":\"test-id\",\"result\":\"success\"}";

        Optional<Nip46Response> result = decoder.tryDecodeResponse(json);

        assertTrue(result.isPresent());
        assertTrue(result.get().isSuccess());
    }

    @Test
    void tryDecodeResponseReturnsEmptyForInvalidJson() {
        Optional<Nip46Response> result = decoder.tryDecodeResponse("invalid");

        assertTrue(result.isEmpty());
    }

    @Test
    void isRequestReturnsTrueForRequest() {
        String json = "{\"id\":\"abc\",\"method\":\"ping\",\"params\":[]}";

        assertTrue(decoder.isRequest(json));
    }

    @Test
    void isRequestReturnsFalseForResponse() {
        String json = "{\"id\":\"abc\",\"result\":\"pong\"}";

        assertFalse(decoder.isRequest(json));
    }

    @Test
    void isRequestReturnsFalseForNull() {
        assertFalse(decoder.isRequest(null));
    }

    @Test
    void isRequestReturnsFalseForBlank() {
        assertFalse(decoder.isRequest("   "));
    }

    @Test
    void isRequestReturnsFalseForInvalidJson() {
        assertFalse(decoder.isRequest("not json"));
    }

    @Test
    void isResponseReturnsTrueForSuccessResponse() {
        String json = "{\"id\":\"abc\",\"result\":\"pong\"}";

        assertTrue(decoder.isResponse(json));
    }

    @Test
    void isResponseReturnsTrueForErrorResponse() {
        String json = "{\"id\":\"abc\",\"error\":{\"code\":\"ERROR\",\"message\":\"msg\"}}";

        assertTrue(decoder.isResponse(json));
    }

    @Test
    void isResponseReturnsFalseForRequest() {
        String json = "{\"id\":\"abc\",\"method\":\"ping\",\"params\":[]}";

        assertFalse(decoder.isResponse(json));
    }

    @Test
    void isResponseReturnsFalseForNull() {
        assertFalse(decoder.isResponse(null));
    }

    @Test
    void isResponseReturnsFalseForBlank() {
        assertFalse(decoder.isResponse(""));
    }

    @Test
    void isResponseReturnsFalseForInvalidJson() {
        assertFalse(decoder.isResponse("invalid"));
    }

    @Test
    void extractIdReturnsIdForValidJson() {
        String json = "{\"id\":\"test-id-123\",\"method\":\"ping\"}";

        Optional<String> id = decoder.extractId(json);

        assertTrue(id.isPresent());
        assertEquals("test-id-123", id.get());
    }

    @Test
    void extractIdReturnsEmptyForMissingId() {
        String json = "{\"method\":\"ping\"}";

        Optional<String> id = decoder.extractId(json);

        assertTrue(id.isEmpty());
    }

    @Test
    void extractIdReturnsEmptyForNonStringId() {
        String json = "{\"id\":123,\"method\":\"ping\"}";

        Optional<String> id = decoder.extractId(json);

        assertTrue(id.isEmpty());
    }

    @Test
    void extractIdReturnsEmptyForNull() {
        Optional<String> id = decoder.extractId(null);

        assertTrue(id.isEmpty());
    }

    @Test
    void extractIdReturnsEmptyForInvalidJson() {
        Optional<String> id = decoder.extractId("not json");

        assertTrue(id.isEmpty());
    }

    @Test
    void extractMethodReturnsMethodForValidJson() {
        String json = "{\"id\":\"abc\",\"method\":\"sign_event\",\"params\":[]}";

        Optional<String> method = decoder.extractMethod(json);

        assertTrue(method.isPresent());
        assertEquals("sign_event", method.get());
    }

    @Test
    void extractMethodReturnsEmptyForMissingMethod() {
        String json = "{\"id\":\"abc\",\"result\":\"pong\"}";

        Optional<String> method = decoder.extractMethod(json);

        assertTrue(method.isEmpty());
    }

    @Test
    void extractMethodReturnsEmptyForNull() {
        Optional<String> method = decoder.extractMethod(null);

        assertTrue(method.isEmpty());
    }

    @Test
    void extractMethodReturnsEmptyForInvalidJson() {
        Optional<String> method = decoder.extractMethod("{broken");

        assertTrue(method.isEmpty());
    }

    @Test
    void decodeGenericObject() {
        String json = "{\"name\":\"test\",\"value\":42}";

        TestObject obj = decoder.decode(json, TestObject.class);

        assertEquals("test", obj.name);
        assertEquals(42, obj.value);
    }

    @Test
    void decodeWithNullJsonThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                decoder.decode(null, TestObject.class));
    }

    @Test
    void decodeWithInvalidJsonThrows() {
        assertThrows(Nip46Decoder.Nip46DecodingException.class, () ->
                decoder.decode("invalid", TestObject.class));
    }

    // Test helper class
    static class TestObject {
        public String name;
        public int value;
    }

    @Test
    void roundTripRequestEncodeAndDecode() {
        Nip46Encoder encoder = new Nip46Encoder();
        Nip46Request original = Nip46Request.signEvent("{\"kind\":1}");

        String json = encoder.encodeRequest(original);
        Nip46Request decoded = decoder.decodeRequest(json);

        assertEquals(original.getId(), decoded.getId());
        assertEquals(original.getMethod(), decoded.getMethod());
        assertEquals(original.getParams(), decoded.getParams());
    }

    @Test
    void roundTripSuccessResponseEncodeAndDecode() {
        Nip46Encoder encoder = new Nip46Encoder();
        Nip46Response original = Nip46Response.success("test-id", "result-value");

        String json = encoder.encodeResponse(original);
        Nip46Response decoded = decoder.decodeResponse(json);

        assertEquals(original.getId(), decoded.getId());
        assertEquals(original.getResult(), decoded.getResult());
        assertTrue(decoded.isSuccess());
    }

    @Test
    void roundTripErrorResponseEncodeAndDecode() {
        Nip46Encoder encoder = new Nip46Encoder();
        Nip46Response original = Nip46Response.error("test-id", "ERROR_CODE", "Error message");

        String json = encoder.encodeResponse(original);
        Nip46Response decoded = decoder.decodeResponse(json);

        assertEquals(original.getId(), decoded.getId());
        assertEquals(original.getErrorCode(), decoded.getErrorCode());
        assertEquals(original.getErrorMessage(), decoded.getErrorMessage());
        assertTrue(decoded.isError());
    }

    @Test
    void decodingExceptionContainsMessage() {
        Nip46Decoder.Nip46DecodingException exception =
                new Nip46Decoder.Nip46DecodingException("test message");

        assertEquals("test message", exception.getMessage());
    }

    @Test
    void decodingExceptionContainsCause() {
        RuntimeException cause = new RuntimeException("cause");
        Nip46Decoder.Nip46DecodingException exception =
                new Nip46Decoder.Nip46DecodingException("test", cause);

        assertEquals(cause, exception.getCause());
    }
}
