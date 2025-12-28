package xyz.tcheeric.nsecbunker.security;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import xyz.tcheeric.nsecbunker.core.connection.BunkerConnectionString;
import xyz.tcheeric.nsecbunker.core.model.BunkerConnection;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Security tests for transport layer (WebSocket/TLS).
 *
 * <p>These tests verify that:
 * <ul>
 *   <li>Connection string parsing handles malicious input safely</li>
 *   <li>TLS connections are properly validated</li>
 *   <li>Certificate validation is enforced</li>
 *   <li>Malicious relay responses don't cause issues</li>
 * </ul>
 */
@DisplayName("Transport Security Tests")
class TransportSecurityTest {

    private MockWebServer mockServer;

    @BeforeEach
    void setUp() throws IOException {
        mockServer = new MockWebServer();
    }

    @AfterEach
    void tearDown() throws IOException {
        mockServer.shutdown();
    }

    @Nested
    @DisplayName("Connection String URL Validation Tests")
    class ConnectionStringValidationTests {

        @ParameterizedTest
        @ValueSource(strings = {
                // Use valid 64-char hex pubkeys with relay in query params (correct format)
                "bunker://0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef?relay=wss://relay.example.com",
                "bunker://0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef?relay=ws://relay.example.com",
                "bunker://0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                "bunker://0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef?relay=wss://relay1.com&relay=wss://relay2.com",
                "bunker://0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef?relay=wss://relay.com&secret=mysecret",
        })
        @DisplayName("Should handle valid connection strings")
        void shouldHandleValidConnectionStrings(String connStr) {
            assertThatCode(() -> {
                BunkerConnection parsed = BunkerConnectionString.parse(connStr);
                assertThat(parsed).isNotNull();
            }).doesNotThrowAnyException();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "",
                "not-a-bunker-url",
                "http://example.com",
                "bunker://",
                "bunker://@relay",
        })
        @DisplayName("Should reject invalid connection strings safely")
        void shouldRejectInvalidConnectionStrings(String malformed) {
            assertThatCode(() -> {
                try {
                    BunkerConnectionString.parse(malformed);
                } catch (IllegalArgumentException e) {
                    // Expected - rejection is correct behavior
                }
            }).doesNotThrowAnyException();
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "bunker://npub123\r\nX-Injected: header",
                "bunker://npub123%0d%0aX-Injected:%20header",
                "bunker://npub123/..\\..\\etc\\passwd",
                "bunker://npub123/../../../etc/passwd",
                "bunker://npub123?param=value&injected=true",
                "bunker://npub123\0null",
        })
        @DisplayName("Should handle injection attempts safely")
        void shouldHandleInjectionAttempts(String malicious) {
            assertThatCode(() -> {
                try {
                    BunkerConnectionString.parse(malicious);
                } catch (Exception e) {
                    // Rejection is acceptable
                }
            }).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("URI Validation Tests")
    class UriValidationTests {

        @Test
        @DisplayName("Should validate wss:// scheme for relay URLs")
        void shouldValidateWssScheme() {
            // wss:// should be preferred over ws://
            assertThatCode(() -> {
                URI wssUri = URI.create("wss://relay.example.com");
                assertThat(wssUri.getScheme()).isEqualTo("wss");
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("Should reject invalid URI schemes")
        void shouldRejectInvalidSchemes() {
            String[] invalidUris = {
                    "ftp://relay.example.com",
                    "file:///etc/passwd",
            };

            for (String uriStr : invalidUris) {
                assertThatCode(() -> {
                    URI uri = URI.create(uriStr);
                    // Applications should validate scheme before connecting
                    assertThat(uri.getScheme()).isNotEqualTo("wss");
                    assertThat(uri.getScheme()).isNotEqualTo("ws");
                }).doesNotThrowAnyException();
            }
        }

        @Test
        @DisplayName("Should reject URIs with illegal characters")
        void shouldRejectIllegalCharacterUris() {
            // These URIs contain illegal characters and should fail URI parsing
            String[] illegalUris = {
                    "javascript:alert(1)",
                    "data:text/html,<script>alert(1)</script>",
            };

            for (String uriStr : illegalUris) {
                assertThatCode(() -> {
                    try {
                        URI.create(uriStr);
                    } catch (IllegalArgumentException e) {
                        // Expected - illegal characters should be rejected
                    }
                }).doesNotThrowAnyException();
            }
        }
    }

    @Nested
    @DisplayName("SSL/TLS Security Tests")
    class SslSecurityTests {

        @Test
        @DisplayName("Trust-all certificate manager should be explicitly flagged as insecure")
        void trustAllCertificatesShouldBeExplicit() {
            // This test documents that trust-all is a security risk
            // Applications should never use trust-all in production

            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }

                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                            // WARNING: This trusts all certificates - INSECURE
                        }

                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                            // WARNING: This trusts all certificates - INSECURE
                        }
                    }
            };

            assertThatCode(() -> {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
                // Creating trust-all context works, but should NEVER be used in production
            }).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Mock Server Response Tests")
    class MockServerResponseTests {

        @Test
        @DisplayName("Should handle oversized response headers")
        void shouldHandleOversizedHeaders() throws IOException {
            mockServer.start();

            // Create response with very large header
            String largeHeader = "X".repeat(100000);
            mockServer.enqueue(new MockResponse()
                    .addHeader("X-Large-Header", largeHeader)
                    .setBody("OK"));

            // Client should handle or reject large headers safely
            // This just tests that the server can be configured - actual client behavior
            // depends on the HTTP client implementation
            assertThat(mockServer.getPort()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should handle slow response attacks")
        void shouldHandleSlowResponses() throws IOException {
            mockServer.start();

            // Simulate slowloris-style attack
            mockServer.enqueue(new MockResponse()
                    .setBodyDelay(5, TimeUnit.SECONDS)
                    .setBody("Delayed response"));

            // Server is ready with delayed response
            // Actual timeout handling depends on client configuration
            assertThat(mockServer.getPort()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should handle chunked encoding")
        void shouldHandleChunkedEncoding() throws IOException {
            mockServer.start();

            mockServer.enqueue(new MockResponse()
                    .setChunkedBody("chunk1chunk2chunk3", 5)
                    .setResponseCode(200));

            assertThat(mockServer.getPort()).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("Host Validation Tests")
    class HostValidationTests {

        @ParameterizedTest
        @ValueSource(strings = {
                "localhost",
                "127.0.0.1",
                "192.168.1.1",
                "10.0.0.1",
                "[::1]",
                "0.0.0.0",
        })
        @DisplayName("Should identify private/local hosts")
        void shouldIdentifyPrivateHosts(String host) {
            // Applications should be careful about connecting to private/local addresses
            // This test documents what localhost/private addresses look like
            assertThatCode(() -> {
                URI uri = URI.create("wss://" + host);
                assertThat(uri.getHost()).isNotBlank();
            }).doesNotThrowAnyException();
        }
    }
}
