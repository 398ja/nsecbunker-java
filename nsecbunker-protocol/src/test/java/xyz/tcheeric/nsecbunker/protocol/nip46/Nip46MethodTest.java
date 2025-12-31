package xyz.tcheeric.nsecbunker.protocol.nip46;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link Nip46Method}.
 */
class Nip46MethodTest {

    @Test
    void getValueReturnsWireFormat() {
        assertEquals("connect", Nip46Method.CONNECT.getValue());
        assertEquals("get_public_key", Nip46Method.GET_PUBLIC_KEY.getValue());
        assertEquals("sign_event", Nip46Method.SIGN_EVENT.getValue());
        assertEquals("nip04_encrypt", Nip46Method.NIP04_ENCRYPT.getValue());
        assertEquals("nip04_decrypt", Nip46Method.NIP04_DECRYPT.getValue());
        assertEquals("nip44_encrypt", Nip46Method.NIP44_ENCRYPT.getValue());
        assertEquals("nip44_decrypt", Nip46Method.NIP44_DECRYPT.getValue());
        assertEquals("ping", Nip46Method.PING.getValue());
        assertEquals("get_relays", Nip46Method.GET_RELAYS.getValue());
    }

    @Test
    void fromValueParsesValidMethod() {
        assertEquals(Nip46Method.CONNECT, Nip46Method.fromValue("connect"));
        assertEquals(Nip46Method.GET_PUBLIC_KEY, Nip46Method.fromValue("get_public_key"));
        assertEquals(Nip46Method.SIGN_EVENT, Nip46Method.fromValue("sign_event"));
        assertEquals(Nip46Method.PING, Nip46Method.fromValue("ping"));
    }

    @Test
    void fromValueThrowsForUnknownMethod() {
        assertThrows(IllegalArgumentException.class, () ->
                Nip46Method.fromValue("unknown_method"));
    }

    @Test
    void tryFromValueReturnsMethodForValidValue() {
        assertEquals(Nip46Method.PING, Nip46Method.tryFromValue("ping"));
        assertEquals(Nip46Method.SIGN_EVENT, Nip46Method.tryFromValue("sign_event"));
    }

    @Test
    void tryFromValueReturnsNullForUnknownMethod() {
        assertNull(Nip46Method.tryFromValue("unknown_method"));
        assertNull(Nip46Method.tryFromValue(""));
        assertNull(Nip46Method.tryFromValue(null));
    }

    @Test
    void toStringReturnsWireFormat() {
        assertEquals("ping", Nip46Method.PING.toString());
        assertEquals("sign_event", Nip46Method.SIGN_EVENT.toString());
    }
}
