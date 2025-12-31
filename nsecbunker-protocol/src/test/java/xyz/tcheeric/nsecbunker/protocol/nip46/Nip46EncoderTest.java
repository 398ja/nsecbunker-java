package xyz.tcheeric.nsecbunker.protocol.nip46;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Nip46Encoder}.
 */
class Nip46EncoderTest {

    private Nip46Encoder encoder;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        encoder = new Nip46Encoder();
        objectMapper = new ObjectMapper();
    }

    @Test
    void constructorWithNullObjectMapperUsesDefault() {
        Nip46Encoder encoder = new Nip46Encoder(null);
        assertNotNull(encoder);
    }

    @Test
    void constructorWithCustomObjectMapper() {
        ObjectMapper custom = new ObjectMapper();
        Nip46Encoder encoder = new Nip46Encoder(custom);
        assertNotNull(encoder);
    }

    @Test
    void encodeRequestProducesValidJson() throws Exception {
        Nip46Request request = Nip46Request.builder()
                .id("test-id")
                .method("ping")
                .build();

        String json = encoder.encodeRequest(request);

        // Parse and verify
        var node = objectMapper.readTree(json);
        assertEquals("test-id", node.get("id").asText());
        assertEquals("ping", node.get("method").asText());
        assertTrue(node.has("params"));
    }

    @Test
    void encodeRequestWithParams() throws Exception {
        Nip46Request request = Nip46Request.builder()
                .id("test-id")
                .method("test")
                .params(Arrays.asList("param1", "param2"))
                .build();

        String json = encoder.encodeRequest(request);

        var node = objectMapper.readTree(json);
        var params = node.get("params");
        assertTrue(params.isArray());
        assertEquals(2, params.size());
        assertEquals("param1", params.get(0).asText());
        assertEquals("param2", params.get(1).asText());
    }

    @Test
    void encodeRequestWithNullThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                encoder.encodeRequest(null));
    }

    @Test
    void encodeResponseSuccessProducesValidJson() throws Exception {
        Nip46Response response = Nip46Response.success("test-id", "result-value");

        String json = encoder.encodeResponse(response);

        var node = objectMapper.readTree(json);
        assertEquals("test-id", node.get("id").asText());
        assertEquals("result-value", node.get("result").asText());
        assertFalse(node.has("error"));
    }

    @Test
    void encodeResponseErrorProducesValidJson() throws Exception {
        Nip46Response response = Nip46Response.error("test-id", "ERROR_CODE", "Error message");

        String json = encoder.encodeResponse(response);

        var node = objectMapper.readTree(json);
        assertEquals("test-id", node.get("id").asText());
        assertFalse(node.has("result"));
        assertTrue(node.has("error"));
        assertEquals("ERROR_CODE", node.get("error").get("code").asText());
        assertEquals("Error message", node.get("error").get("message").asText());
    }

    @Test
    void encodeResponseWithNullThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                encoder.encodeResponse(null));
    }

    @Test
    void encodeGenericObject() throws Exception {
        record TestObject(String name, int value) {}
        TestObject obj = new TestObject("test", 42);

        String json = encoder.encode(obj);

        var node = objectMapper.readTree(json);
        assertEquals("test", node.get("name").asText());
        assertEquals(42, node.get("value").asInt());
    }

    @Test
    void encodeWithNullThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                encoder.encode(null));
    }

    @Test
    void encodePingRequest() throws Exception {
        Nip46Request request = Nip46Request.ping();

        String json = encoder.encodeRequest(request);

        var node = objectMapper.readTree(json);
        assertEquals("ping", node.get("method").asText());
    }

    @Test
    void encodeSignEventRequest() throws Exception {
        String eventJson = "{\"kind\":1,\"content\":\"Hello\"}";
        Nip46Request request = Nip46Request.signEvent(eventJson);

        String json = encoder.encodeRequest(request);

        var node = objectMapper.readTree(json);
        assertEquals("sign_event", node.get("method").asText());
        assertEquals(eventJson, node.get("params").get(0).asText());
    }
}
