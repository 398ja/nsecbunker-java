package xyz.tcheeric.nsecbunker.protocol.nip46;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Nip46Request}.
 */
class Nip46RequestTest {

    private static final String TEST_PUBKEY = "abc123def456";
    private static final String TEST_EVENT_JSON = "{\"kind\":1,\"content\":\"Hello\"}";

    @Test
    void builderCreatesValidRequest() {
        Nip46Request request = Nip46Request.builder()
                .method("ping")
                .build();

        assertNotNull(request.getId());
        assertEquals("ping", request.getMethod());
        assertNotNull(request.getParams());
        assertTrue(request.getParams().isEmpty());
    }

    @Test
    void builderWithCustomId() {
        Nip46Request request = Nip46Request.builder()
                .id("custom-id")
                .method("ping")
                .build();

        assertEquals("custom-id", request.getId());
    }

    @Test
    void builderWithParams() {
        List<Object> params = Arrays.asList("param1", "param2");
        Nip46Request request = Nip46Request.builder()
                .method("test")
                .params(params)
                .build();

        assertEquals(params, request.getParams());
    }

    @Test
    void getMethodEnumReturnsValidMethod() {
        Nip46Request request = Nip46Request.builder()
                .method("ping")
                .build();

        assertEquals(Nip46Method.PING, request.getMethodEnum());
    }

    @Test
    void getMethodEnumReturnsNullForUnknownMethod() {
        Nip46Request request = Nip46Request.builder()
                .method("unknown")
                .build();

        assertNull(request.getMethodEnum());
    }

    @Test
    void hasKnownMethodReturnsTrueForKnownMethod() {
        Nip46Request request = Nip46Request.builder()
                .method("sign_event")
                .build();

        assertTrue(request.hasKnownMethod());
    }

    @Test
    void hasKnownMethodReturnsFalseForUnknownMethod() {
        Nip46Request request = Nip46Request.builder()
                .method("unknown")
                .build();

        assertFalse(request.hasKnownMethod());
    }

    @Test
    void getFirstParamReturnsFirstParam() {
        Nip46Request request = Nip46Request.builder()
                .method("test")
                .params(Arrays.asList("first", "second"))
                .build();

        assertEquals("first", request.getFirstParam());
    }

    @Test
    void getFirstParamReturnsNullWhenNoParams() {
        Nip46Request request = Nip46Request.builder()
                .method("test")
                .build();

        assertNull(request.getFirstParam());
    }

    @Test
    void getSecondParamReturnsSecondParam() {
        Nip46Request request = Nip46Request.builder()
                .method("test")
                .params(Arrays.asList("first", "second"))
                .build();

        assertEquals("second", request.getSecondParam());
    }

    @Test
    void getSecondParamReturnsNullWhenOnlyOneParam() {
        Nip46Request request = Nip46Request.builder()
                .method("test")
                .params(Collections.singletonList("first"))
                .build();

        assertNull(request.getSecondParam());
    }

    // Factory method tests

    @Test
    void connectCreatesValidRequest() {
        Nip46Request request = Nip46Request.connect(TEST_PUBKEY);

        assertEquals("connect", request.getMethod());
        assertEquals(1, request.getParams().size());
        assertEquals(TEST_PUBKEY, request.getFirstParam());
    }

    @Test
    void connectWithSecretCreatesValidRequest() {
        Nip46Request request = Nip46Request.connect(TEST_PUBKEY, "secret123");

        assertEquals("connect", request.getMethod());
        assertEquals(2, request.getParams().size());
        assertEquals(TEST_PUBKEY, request.getFirstParam());
        assertEquals("secret123", request.getSecondParam());
    }

    @Test
    void connectWithNullPubkeyThrows() {
        assertThrows(NullPointerException.class, () ->
                Nip46Request.connect(null));
    }

    @Test
    void getPublicKeyCreatesValidRequest() {
        Nip46Request request = Nip46Request.getPublicKey();

        assertEquals("get_public_key", request.getMethod());
        assertTrue(request.getParams().isEmpty());
    }

    @Test
    void signEventCreatesValidRequest() {
        Nip46Request request = Nip46Request.signEvent(TEST_EVENT_JSON);

        assertEquals("sign_event", request.getMethod());
        assertEquals(1, request.getParams().size());
        assertEquals(TEST_EVENT_JSON, request.getFirstParam());
    }

    @Test
    void signEventWithNullThrows() {
        assertThrows(NullPointerException.class, () ->
                Nip46Request.signEvent(null));
    }

    @Test
    void nip04EncryptCreatesValidRequest() {
        Nip46Request request = Nip46Request.nip04Encrypt(TEST_PUBKEY, "plaintext");

        assertEquals("nip04_encrypt", request.getMethod());
        assertEquals(2, request.getParams().size());
        assertEquals(TEST_PUBKEY, request.getFirstParam());
        assertEquals("plaintext", request.getSecondParam());
    }

    @Test
    void nip04DecryptCreatesValidRequest() {
        Nip46Request request = Nip46Request.nip04Decrypt(TEST_PUBKEY, "ciphertext");

        assertEquals("nip04_decrypt", request.getMethod());
        assertEquals(2, request.getParams().size());
        assertEquals(TEST_PUBKEY, request.getFirstParam());
        assertEquals("ciphertext", request.getSecondParam());
    }

    @Test
    void nip44EncryptCreatesValidRequest() {
        Nip46Request request = Nip46Request.nip44Encrypt(TEST_PUBKEY, "plaintext");

        assertEquals("nip44_encrypt", request.getMethod());
        assertEquals(2, request.getParams().size());
    }

    @Test
    void nip44DecryptCreatesValidRequest() {
        Nip46Request request = Nip46Request.nip44Decrypt(TEST_PUBKEY, "ciphertext");

        assertEquals("nip44_decrypt", request.getMethod());
        assertEquals(2, request.getParams().size());
    }

    @Test
    void pingCreatesValidRequest() {
        Nip46Request request = Nip46Request.ping();

        assertEquals("ping", request.getMethod());
        assertTrue(request.getParams().isEmpty());
    }

    @Test
    void getRelaysCreatesValidRequest() {
        Nip46Request request = Nip46Request.getRelays();

        assertEquals("get_relays", request.getMethod());
        assertTrue(request.getParams().isEmpty());
    }

    @Test
    void equalsAndHashCode() {
        Nip46Request request1 = Nip46Request.builder()
                .id("same-id")
                .method("ping")
                .build();

        Nip46Request request2 = Nip46Request.builder()
                .id("same-id")
                .method("ping")
                .build();

        assertEquals(request1, request2);
        assertEquals(request1.hashCode(), request2.hashCode());
    }

    @Test
    void toStringContainsRelevantInfo() {
        Nip46Request request = Nip46Request.builder()
                .id("test-id")
                .method("ping")
                .build();

        String str = request.toString();
        assertTrue(str.contains("test-id"));
        assertTrue(str.contains("ping"));
    }
}
