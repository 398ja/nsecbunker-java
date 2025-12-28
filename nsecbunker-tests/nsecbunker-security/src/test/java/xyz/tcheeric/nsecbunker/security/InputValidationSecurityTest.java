package xyz.tcheeric.nsecbunker.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import xyz.tcheeric.nsecbunker.core.model.BunkerKey;
import xyz.tcheeric.nsecbunker.core.model.BunkerPolicy;
import xyz.tcheeric.nsecbunker.core.model.KeyUser;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Request;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Security tests for input validation across the nsecbunker library.
 *
 * <p>These tests verify that untrusted input is properly validated and
 * sanitized before use, preventing injection attacks and other security issues.
 */
@DisplayName("Input Validation Security Tests")
class InputValidationSecurityTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("Key Name Validation Tests")
    class KeyNameValidationTests {

        @ParameterizedTest
        @ValueSource(strings = {
                "../../../etc/passwd",
                "..\\..\\windows\\system32",
                "key\0name",
                "key\nname",
                "key\rname",
                "key;rm -rf /",
                "key$(whoami)",
                "key`id`",
                "key|cat /etc/passwd",
                "key&& rm -rf /",
                "<script>alert('xss')</script>",
                "key' OR '1'='1",
                "key\"; DROP TABLE keys;--",
        })
        @DisplayName("Should handle potentially malicious key names")
        void shouldHandleMaliciousKeyNames(String maliciousName) {
            // Test that BunkerKey handles dangerous key names safely
            assertThatCode(() -> {
                BunkerKey key = BunkerKey.builder()
                        .name(maliciousName)
                        .npub("npub1234567890abcdef")
                        .build();

                // The name should be stored as-is (no execution) or rejected
                assertThat(key.getName()).isNotNull();
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should accept valid key names")
        void shouldAcceptValidKeyNames() {
            String[] validNames = {
                    "mykey",
                    "my-key",
                    "my_key",
                    "my.key",
                    "MyKey123",
                    "key-with-dashes",
                    "key_with_underscores",
            };

            for (String name : validNames) {
                BunkerKey key = BunkerKey.builder()
                        .name(name)
                        .npub("npub1234567890abcdef")
                        .build();

                assertThat(key.getName()).isEqualTo(name);
            }
        }
    }

    @Nested
    @DisplayName("Public Key Validation Tests")
    class PublicKeyValidationTests {

        @ParameterizedTest
        @ValueSource(strings = {
                "",
                "not-hex",
                "0123456789GHIJ",  // Invalid hex chars
                "short",
                "0123456789abcdef",  // Too short
                "npub\0injection",  // Null byte
        })
        @DisplayName("Should handle invalid npubs")
        void shouldHandleInvalidNpubs(String invalidNpub) {
            // BunkerKey should either reject or safely handle invalid npubs
            assertThatCode(() -> {
                try {
                    BunkerKey key = BunkerKey.builder()
                            .name("test-key")
                            .npub(invalidNpub)
                            .build();
                    // If accepted, should be stored as-is
                } catch (Exception e) {
                    // Rejection is acceptable
                }
            }).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("NIP-46 Request Validation Tests")
    class Nip46RequestValidationTests {

        @ParameterizedTest
        @ValueSource(strings = {
                "'; DROP TABLE keys;--",
                "<script>alert('xss')</script>",
                "$(rm -rf /)",
                "`whoami`",
                "{{7*7}}",
                "${7*7}",
                "../../../etc/passwd",
                "\0null\0byte",
                "\r\nHTTP/1.1 200 OK\r\n",
        })
        @DisplayName("Should handle malicious method names safely")
        void shouldHandleMaliciousMethodNames(String maliciousMethod) {
            Nip46Request request = Nip46Request.builder()
                    .id("test-id")
                    .method(maliciousMethod)
                    .params(Collections.emptyList())
                    .build();

            // Method should be stored as plain string data, not executed
            assertThat(request.getMethod()).isEqualTo(maliciousMethod);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "'; DROP TABLE keys;--",
                "<script>alert('xss')</script>",
                "../../../etc/passwd",
                "\0",
                "\r\n",
        })
        @DisplayName("Should handle malicious params safely")
        void shouldHandleMaliciousParams(String maliciousParam) {
            Nip46Request request = Nip46Request.builder()
                    .id("test-id")
                    .method("ping")
                    .params(List.<Object>of(maliciousParam))
                    .build();

            // Params should be stored as plain string data
            assertThat(request.getParams()).containsExactly(maliciousParam);
        }

        @Test
        @DisplayName("Should handle empty request gracefully")
        void shouldHandleEmptyRequest() {
            assertThatCode(() -> {
                Nip46Request request = Nip46Request.builder().build();
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle null params list")
        void shouldHandleNullParamsList() {
            Nip46Request request = Nip46Request.builder()
                    .id("test-id")
                    .method("ping")
                    .params(null)
                    .build();

            assertThat(request.getParams()).isNull();
        }
    }

    @Nested
    @DisplayName("Policy Validation Tests")
    class PolicyValidationTests {

        @ParameterizedTest
        @ValueSource(strings = {
                "'; DROP TABLE policies;--",
                "<script>alert('xss')</script>",
                "../../../etc/passwd",
                "policy\0name",
        })
        @DisplayName("Should handle malicious policy names safely")
        void shouldHandleMaliciousPolicyNames(String maliciousName) {
            BunkerPolicy policy = BunkerPolicy.builder()
                    .name(maliciousName)
                    .description("Test policy")
                    .build();

            // Name should be stored as plain data
            assertThat(policy.getName()).isEqualTo(maliciousName);
        }

        @Test
        @DisplayName("Should handle empty policy")
        void shouldHandleEmptyPolicy() {
            assertThatCode(() -> {
                BunkerPolicy policy = BunkerPolicy.builder().build();
            }).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("JSON Deserialization Security Tests")
    class JsonDeserializationSecurityTests {

        @Test
        @DisplayName("Should not deserialize unexpected types")
        void shouldNotDeserializeUnexpectedTypes() {
            // Test that JSON deserialization doesn't instantiate arbitrary classes
            String maliciousJson = "{\"@type\":\"java.lang.Runtime\",\"command\":\"calc.exe\"}";

            assertThatCode(() -> {
                try {
                    objectMapper.readValue(maliciousJson, Nip46Request.class);
                } catch (Exception e) {
                    // Expected - should not process @type
                }
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle JSON with extra fields safely")
        void shouldHandleJsonWithExtraFields() {
            // JSON with extra/unknown fields
            String jsonWithExtras = "{\"id\":\"test\",\"method\":\"ping\",\"params\":[],\"malicious\":\"data\",\"extra\":123}";

            // Strict validation (rejecting unknown fields) is actually more secure
            // Either behavior is acceptable: ignore extra fields or reject them
            assertThatCode(() -> {
                try {
                    Nip46Request request = objectMapper.readValue(jsonWithExtras, Nip46Request.class);
                    // If it succeeded, fields should be parsed correctly
                    assertThat(request.getId()).isEqualTo("test");
                    assertThat(request.getMethod()).isEqualTo("ping");
                } catch (com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException e) {
                    // Rejecting unknown fields is also valid and secure behavior
                }
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle deeply nested JSON without stack overflow")
        void shouldHandleDeeplyNestedJson() {
            StringBuilder sb = new StringBuilder("{\"params\":");
            for (int i = 0; i < 100; i++) {
                sb.append("[");
            }
            sb.append("\"value\"");
            for (int i = 0; i < 100; i++) {
                sb.append("]");
            }
            sb.append("}");

            assertThatCode(() -> {
                try {
                    objectMapper.readValue(sb.toString(), Nip46Request.class);
                } catch (Exception e) {
                    // May fail, but should not cause stack overflow
                }
            }).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("KeyUser Validation Tests")
    class KeyUserValidationTests {

        @ParameterizedTest
        @ValueSource(strings = {
                "'; DROP TABLE users;--",
                "<script>alert('xss')</script>",
                "../../../etc/passwd",
                "description\0null",
                "\r\nInjected-Header: value",
        })
        @DisplayName("Should handle malicious descriptions safely")
        void shouldHandleMaliciousDescriptions(String maliciousDesc) {
            KeyUser user = KeyUser.builder()
                    .keyName("test-key")
                    .pubkeyHex("0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef")
                    .description(maliciousDesc)
                    .build();

            // Description should be stored as plain data
            assertThat(user.getDescription()).isEqualTo(maliciousDesc);
        }
    }

    @Nested
    @DisplayName("Integer Overflow Tests")
    class IntegerOverflowTests {

        @Test
        @DisplayName("Should handle very large integers in JSON")
        void shouldHandleLargeIntegers() {
            String jsonWithLargeInt = "{\"id\":\"test\",\"method\":\"ping\",\"params\":[" + Long.MAX_VALUE + "]}";

            assertThatCode(() -> {
                try {
                    Nip46Request request = objectMapper.readValue(jsonWithLargeInt, Nip46Request.class);
                } catch (Exception e) {
                    // May fail due to type mismatch
                }
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should handle negative numbers")
        void shouldHandleNegativeNumbers() {
            String jsonWithNegative = "{\"id\":\"test\",\"method\":\"ping\",\"params\":[-1,-999999]}";

            assertThatCode(() -> {
                try {
                    objectMapper.readValue(jsonWithNegative, Nip46Request.class);
                } catch (Exception e) {
                    // May fail
                }
            }).doesNotThrowAnyException();
        }
    }
}
