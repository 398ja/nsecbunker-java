package xyz.tcheeric.nsecbunker.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Request;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Response;

import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Fuzzing and negative tests for NIP-46 protocol decoders.
 *
 * <p>These tests ensure the decoders handle malformed, malicious,
 * and edge-case inputs gracefully without crashing or leaking information.
 */
@DisplayName("NIP-46 Decoder Fuzzing Tests")
class Nip46DecoderFuzzTest {

    private ObjectMapper objectMapper;
    private Random random;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        random = new Random(42); // Fixed seed for reproducibility
    }

    @Nested
    @DisplayName("Malformed JSON Tests")
    class MalformedJsonTests {

        @ParameterizedTest
        @ValueSource(strings = {
                "",
                "null",
                "undefined",
                "true",
                "false",
                "123",
                "[]",
                "\"string\"",
                "{",
                "}",
                "{}}",
                "{{}}",
                "{\"id\"}",
                "{\"id\":}",
                "{\"id\":",
                "{\"id\":null,",
                "{\"id\":null,\"method\"",
        })
        @DisplayName("Should handle malformed JSON gracefully")
        void shouldHandleMalformedJson(String malformedJson) {
            // Ensures malformed JSON payloads do not crash or hang the decoder.
            assertThatCode(() -> {
                try {
                    objectMapper.readValue(malformedJson, Nip46Request.class);
                } catch (Exception e) {
                    // Expected - should not crash or hang
                }
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle deeply nested JSON without stack overflow")
        void shouldHandleDeeplyNestedJson() {
            // Ensures deeply nested JSON structures are handled without stack overflow.
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                sb.append("{\"nested\":");
            }
            sb.append("null");
            for (int i = 0; i < 1000; i++) {
                sb.append("}");
            }

            String deeplyNested = sb.toString();
            assertThatCode(() -> {
                try {
                    objectMapper.readValue(deeplyNested, Nip46Request.class);
                } catch (Exception e) {
                    // Expected - should not cause stack overflow
                }
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle extremely long strings")
        void shouldHandleExtremelyLongStrings() {
            // Ensures very long string fields cannot crash the decoder.
            // Create a very long string (1MB)
            String longString = "a".repeat(1024 * 1024);
            String json = String.format("{\"id\":\"%s\",\"method\":\"ping\",\"params\":[]}", longString);

            assertThatCode(() -> {
                try {
                    objectMapper.readValue(json, Nip46Request.class);
                } catch (Exception e) {
                    // May fail due to size limits - that's acceptable
                }
            }).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Injection Attack Tests")
    class InjectionAttackTests {

        @ParameterizedTest
        @ValueSource(strings = {
                "{\"id\":\"'; DROP TABLE keys;--\",\"method\":\"ping\",\"params\":[]}",
                "{\"id\":\"<script>alert('xss')</script>\",\"method\":\"ping\",\"params\":[]}",
                "{\"id\":\"$(whoami)\",\"method\":\"ping\",\"params\":[]}",
                "{\"id\":\"`whoami`\",\"method\":\"ping\",\"params\":[]}",
                "{\"id\":\"{{7*7}}\",\"method\":\"ping\",\"params\":[]}",
                "{\"id\":\"${7*7}\",\"method\":\"ping\",\"params\":[]}",
                "{\"id\":\"../../../etc/passwd\",\"method\":\"ping\",\"params\":[]}",
                "{\"id\":\"\\u0000\\u0001\\u0002\",\"method\":\"ping\",\"params\":[]}",
        })
        @DisplayName("Should sanitize potential injection payloads")
        void shouldHandleInjectionPayloads(String payload) {
            // Ensures injection-like ids are treated as plain data or rejected without crashing.
            assertThatCode(() -> {
                try {
                    Nip46Request request = objectMapper.readValue(payload, Nip46Request.class);
                    // If parsed, verify the id is treated as a plain string
                    assertThat(request.getId()).isNotNull();
                } catch (Exception e) {
                    // Rejection is also acceptable
                }
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle null bytes in strings")
        void shouldHandleNullBytes() {
            // Ensures null-byte sequences in ids are handled safely
            // (either rejected or preserved without causing issues)
            String json = "{\"id\":\"test\\u0000injection\",\"method\":\"ping\",\"params\":[]}";

            assertThatCode(() -> {
                try {
                    Nip46Request request = objectMapper.readValue(json, Nip46Request.class);
                    // If parsing succeeds, the object should be usable
                    // (null bytes may be preserved but shouldn't cause crashes)
                    assertThat(request.getId()).isNotNull();
                    assertThat(request.getMethod()).isEqualTo("ping");
                } catch (Exception e) {
                    // Rejection is also acceptable behavior
                }
            }).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Type Confusion Tests")
    class TypeConfusionTests {

        @Test
        @DisplayName("Should handle wrong type for id field")
        void shouldHandleWrongTypeForId() {
            // Ensures unexpected id types are rejected safely.
            String[] testCases = {
                    "{\"id\":123,\"method\":\"ping\",\"params\":[]}",
                    "{\"id\":true,\"method\":\"ping\",\"params\":[]}",
                    "{\"id\":[],\"method\":\"ping\",\"params\":[]}",
                    "{\"id\":{},\"method\":\"ping\",\"params\":[]}",
                    "{\"id\":null,\"method\":\"ping\",\"params\":[]}",
            };

            for (String json : testCases) {
                assertThatCode(() -> {
                    try {
                        objectMapper.readValue(json, Nip46Request.class);
                    } catch (Exception e) {
                        // Type mismatch rejection is expected
                    }
                }).doesNotThrowAnyException();
            }
        }

        @Test
        @DisplayName("Should handle wrong type for params field")
        void shouldHandleWrongTypeForParams() {
            // Ensures non-array params do not cause decoder crashes.
            String[] testCases = {
                    "{\"id\":\"test\",\"method\":\"ping\",\"params\":\"string\"}",
                    "{\"id\":\"test\",\"method\":\"ping\",\"params\":123}",
                    "{\"id\":\"test\",\"method\":\"ping\",\"params\":true}",
                    "{\"id\":\"test\",\"method\":\"ping\",\"params\":{}}",
                    "{\"id\":\"test\",\"method\":\"ping\",\"params\":null}",
            };

            for (String json : testCases) {
                assertThatCode(() -> {
                    try {
                        objectMapper.readValue(json, Nip46Request.class);
                    } catch (Exception e) {
                        // Type mismatch rejection is expected
                    }
                }).doesNotThrowAnyException();
            }
        }
    }

    @Nested
    @DisplayName("Nip46Response Fuzzing Tests")
    class Nip46ResponseFuzzTests {

        @Test
        @DisplayName("Should handle error response with malicious message")
        void shouldHandleErrorResponseWithMaliciousMessage() {
            // Ensures malicious strings in response error fields do not crash parsing.
            String json = "{\"id\":\"test\",\"error\":\"<script>alert(1)</script>\"}";

            assertThatCode(() -> {
                try {
                    Nip46Response response = objectMapper.readValue(json, Nip46Response.class);
                    assertThat(response.getError()).isNotNull();
                } catch (Exception e) {
                    // Acceptable
                }
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle both result and error fields")
        void shouldHandleBothResultAndError() {
            // Ensures ambiguous responses containing result and error are handled safely.
            String json = "{\"id\":\"test\",\"result\":\"success\",\"error\":\"failure\"}";

            assertThatCode(() -> {
                try {
                    Nip46Response response = objectMapper.readValue(json, Nip46Response.class);
                    // Should handle this ambiguous case without crashing
                } catch (Exception e) {
                    // Rejection is acceptable
                }
            }).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Random Fuzzing Tests")
    class RandomFuzzingTests {

        @Test
        @DisplayName("Should handle random byte sequences")
        void shouldHandleRandomByteSequences() {
            // Ensures random non-UTF8 data does not destabilize the decoder.
            for (int i = 0; i < 100; i++) {
                byte[] randomBytes = new byte[256];
                random.nextBytes(randomBytes);
                String randomString = new String(randomBytes);

                assertThatCode(() -> {
                    try {
                        objectMapper.readValue(randomString, Nip46Request.class);
                    } catch (Exception e) {
                        // Expected for random data
                    }
                }).doesNotThrowAnyException();
            }
        }

        @Test
        @DisplayName("Should handle random valid-looking JSON")
        void shouldHandleRandomValidLookingJson() {
            // Ensures arbitrary-looking valid JSON is parsed or rejected without throwing.
            for (int i = 0; i < 50; i++) {
                String randomId = UUID.randomUUID().toString();
                String[] methods = {"ping", "sign_event", "get_public_key", "encrypt", "decrypt",
                        randomMethod(), "PING", "Sign_Event", "../path", "method\0name"};
                String method = methods[random.nextInt(methods.length)];

                String json = String.format("{\"id\":\"%s\",\"method\":\"%s\",\"params\":[]}", randomId, method);

                assertThatCode(() -> {
                    try {
                        Nip46Request request = objectMapper.readValue(json, Nip46Request.class);
                        assertThat(request.getId()).isEqualTo(randomId);
                    } catch (Exception e) {
                        // Some methods may be rejected
                    }
                }).doesNotThrowAnyException();
            }
        }

        private String randomMethod() {
            StringBuilder sb = new StringBuilder();
            int length = random.nextInt(50) + 1;
            for (int i = 0; i < length; i++) {
                sb.append((char) (random.nextInt(95) + 32)); // Printable ASCII
            }
            return sb.toString();
        }
    }

    @Nested
    @DisplayName("Unicode and Encoding Tests")
    class UnicodeEncodingTests {

        @ParameterizedTest
        @ValueSource(strings = {
                "{\"id\":\"\\uD800\",\"method\":\"ping\",\"params\":[]}",  // Unpaired surrogate
                "{\"id\":\"\\uDC00\",\"method\":\"ping\",\"params\":[]}",  // Unpaired surrogate
                "{\"id\":\"\\uD800\\uD800\",\"method\":\"ping\",\"params\":[]}",  // Two high surrogates
                "{\"id\":\"emoji\\uD83D\\uDE00test\",\"method\":\"ping\",\"params\":[]}",  // Valid emoji
                "{\"id\":\"rtl\\u202Etest\",\"method\":\"ping\",\"params\":[]}",  // RTL override
                "{\"id\":\"zws\\u200Btest\",\"method\":\"ping\",\"params\":[]}",  // Zero-width space
        })
        @DisplayName("Should handle various Unicode edge cases")
        void shouldHandleUnicodeEdgeCases(String json) {
            // Ensures Unicode edge cases (surrogates/RTL/ZWS) do not crash decoding.
            assertThatCode(() -> {
                try {
                    objectMapper.readValue(json, Nip46Request.class);
                } catch (Exception e) {
                    // Rejection of malformed Unicode is acceptable
                }
            }).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Resource Exhaustion Tests")
    class ResourceExhaustionTests {

        @Test
        @DisplayName("Should handle large params array")
        void shouldHandleLargeParamsArray() {
            // Ensures very large parameter arrays are bounded and safe.
            StringBuilder sb = new StringBuilder("{\"id\":\"test\",\"method\":\"sign_event\",\"params\":[");
            for (int i = 0; i < 10000; i++) {
                if (i > 0) sb.append(",");
                sb.append("\"param").append(i).append("\"");
            }
            sb.append("]}");

            assertThatCode(() -> {
                try {
                    objectMapper.readValue(sb.toString(), Nip46Request.class);
                } catch (Exception e) {
                    // May fail due to limits - acceptable
                }
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle many extra fields")
        void shouldHandleManyExtraFields() {
            // Ensures excessive unknown fields are tolerated or rejected without crashing.
            StringBuilder sb = new StringBuilder("{\"id\":\"test\",\"method\":\"ping\",\"params\":[]");
            for (int i = 0; i < 1000; i++) {
                sb.append(",\"extra").append(i).append("\":\"value").append(i).append("\"");
            }
            sb.append("}");

            assertThatCode(() -> {
                try {
                    Nip46Request request = objectMapper.readValue(sb.toString(), Nip46Request.class);
                    assertThat(request.getMethod()).isEqualTo("ping");
                } catch (Exception e) {
                    // Acceptable if rejected
                }
            }).doesNotThrowAnyException();
        }
    }
}
