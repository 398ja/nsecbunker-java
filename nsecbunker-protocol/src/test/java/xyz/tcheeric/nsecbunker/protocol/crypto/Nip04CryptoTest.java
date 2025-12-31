package xyz.tcheeric.nsecbunker.protocol.crypto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link Nip04Crypto}.
 */
class Nip04CryptoTest {

    // Test keys (32 bytes each, hex-encoded)
    private static final String ALICE_PRIVATE_KEY = "0000000000000000000000000000000000000000000000000000000000000001";
    private static final String ALICE_PUBLIC_KEY = "79be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798";

    private static final String BOB_PRIVATE_KEY = "0000000000000000000000000000000000000000000000000000000000000002";
    private static final String BOB_PUBLIC_KEY = "c6047f9441ed7d6d3045406e95c07cd85c778e4b8cef3ca7abac09b95c709ee5";

    @Test
    void createWithHexKeys() {
        Nip04Crypto crypto = Nip04Crypto.create(ALICE_PRIVATE_KEY, BOB_PUBLIC_KEY);

        assertNotNull(crypto);
        assertEquals(ALICE_PRIVATE_KEY, crypto.getSenderPrivateKeyHex());
        assertEquals(BOB_PUBLIC_KEY, crypto.getRecipientPublicKeyHex());
    }

    @Test
    void createWithByteKeys() {
        byte[] privateKey = hexToBytes(ALICE_PRIVATE_KEY);
        byte[] publicKey = hexToBytes(BOB_PUBLIC_KEY);

        Nip04Crypto crypto = Nip04Crypto.create(privateKey, publicKey);

        assertNotNull(crypto);
        assertArrayEquals(privateKey, crypto.getSenderPrivateKey());
        assertArrayEquals(publicKey, crypto.getRecipientPublicKey());
    }

    @Test
    void createWithNullPrivateKeyThrows() {
        assertThrows(NullPointerException.class, () ->
                Nip04Crypto.create((String) null, BOB_PUBLIC_KEY));
    }

    @Test
    void createWithNullPublicKeyThrows() {
        assertThrows(NullPointerException.class, () ->
                Nip04Crypto.create(ALICE_PRIVATE_KEY, (String) null));
    }

    @Test
    void createWithInvalidPrivateKeyLengthThrows() {
        String shortKey = "0001020304";
        assertThrows(IllegalArgumentException.class, () ->
                Nip04Crypto.create(shortKey, BOB_PUBLIC_KEY));
    }

    @Test
    void createWithInvalidPublicKeyLengthThrows() {
        String shortKey = "0001020304";
        assertThrows(IllegalArgumentException.class, () ->
                Nip04Crypto.create(ALICE_PRIVATE_KEY, shortKey));
    }

    @Test
    void createWithOddLengthHexThrows() {
        String oddHex = "001";
        assertThrows(IllegalArgumentException.class, () ->
                Nip04Crypto.create(oddHex, BOB_PUBLIC_KEY));
    }

    @Test
    void encryptProducesNip04Format() {
        Nip04Crypto crypto = Nip04Crypto.create(ALICE_PRIVATE_KEY, BOB_PUBLIC_KEY);

        String ciphertext = crypto.encrypt("Hello, World!");

        assertNotNull(ciphertext);
        assertTrue(ciphertext.contains("?iv="), "NIP-04 format should contain ?iv= separator");
    }

    @Test
    void encryptWithNullPlaintextThrows() {
        Nip04Crypto crypto = Nip04Crypto.create(ALICE_PRIVATE_KEY, BOB_PUBLIC_KEY);

        assertThrows(NullPointerException.class, () ->
                crypto.encrypt(null));
    }

    @Test
    void decryptWithNullCiphertextThrows() {
        Nip04Crypto crypto = Nip04Crypto.create(ALICE_PRIVATE_KEY, BOB_PUBLIC_KEY);

        assertThrows(NullPointerException.class, () ->
                crypto.decrypt(null));
    }

    @Test
    void decryptWithInvalidFormatThrows() {
        Nip04Crypto crypto = Nip04Crypto.create(ALICE_PRIVATE_KEY, BOB_PUBLIC_KEY);

        assertThrows(Nip04Crypto.Nip04CryptoException.class, () ->
                crypto.decrypt("invalid-ciphertext-no-iv"));
    }

    @Test
    void encryptDecryptRoundTrip() {
        // Alice encrypts for Bob
        Nip04Crypto aliceCrypto = Nip04Crypto.create(ALICE_PRIVATE_KEY, BOB_PUBLIC_KEY);
        String plaintext = "Hello from Alice!";

        String ciphertext = aliceCrypto.encrypt(plaintext);

        // Bob decrypts from Alice
        Nip04Crypto bobCrypto = Nip04Crypto.create(BOB_PRIVATE_KEY, ALICE_PUBLIC_KEY);
        String decrypted = bobCrypto.decrypt(ciphertext);

        assertEquals(plaintext, decrypted);
    }

    @Test
    void encryptDecryptEmptyString() {
        Nip04Crypto aliceCrypto = Nip04Crypto.create(ALICE_PRIVATE_KEY, BOB_PUBLIC_KEY);
        Nip04Crypto bobCrypto = Nip04Crypto.create(BOB_PRIVATE_KEY, ALICE_PUBLIC_KEY);

        String ciphertext = aliceCrypto.encrypt("");
        String decrypted = bobCrypto.decrypt(ciphertext);

        assertEquals("", decrypted);
    }

    @Test
    void encryptDecryptLongMessage() {
        Nip04Crypto aliceCrypto = Nip04Crypto.create(ALICE_PRIVATE_KEY, BOB_PUBLIC_KEY);
        Nip04Crypto bobCrypto = Nip04Crypto.create(BOB_PRIVATE_KEY, ALICE_PUBLIC_KEY);

        String longMessage = "A".repeat(10000);

        String ciphertext = aliceCrypto.encrypt(longMessage);
        String decrypted = bobCrypto.decrypt(ciphertext);

        assertEquals(longMessage, decrypted);
    }

    @Test
    void encryptDecryptUnicodeMessage() {
        Nip04Crypto aliceCrypto = Nip04Crypto.create(ALICE_PRIVATE_KEY, BOB_PUBLIC_KEY);
        Nip04Crypto bobCrypto = Nip04Crypto.create(BOB_PRIVATE_KEY, ALICE_PUBLIC_KEY);

        String unicodeMessage = "Hello 世界! 🌍 مرحبا";

        String ciphertext = aliceCrypto.encrypt(unicodeMessage);
        String decrypted = bobCrypto.decrypt(ciphertext);

        assertEquals(unicodeMessage, decrypted);
    }

    @Test
    void encryptDecryptJsonPayload() {
        Nip04Crypto aliceCrypto = Nip04Crypto.create(ALICE_PRIVATE_KEY, BOB_PUBLIC_KEY);
        Nip04Crypto bobCrypto = Nip04Crypto.create(BOB_PRIVATE_KEY, ALICE_PUBLIC_KEY);

        String jsonPayload = "{\"id\":\"test\",\"method\":\"ping\",\"params\":[]}";

        String ciphertext = aliceCrypto.encrypt(jsonPayload);
        String decrypted = bobCrypto.decrypt(ciphertext);

        assertEquals(jsonPayload, decrypted);
    }

    @Test
    void encryptProducesDifferentCiphertextEachTime() {
        Nip04Crypto crypto = Nip04Crypto.create(ALICE_PRIVATE_KEY, BOB_PUBLIC_KEY);
        String plaintext = "Same message";

        String ciphertext1 = crypto.encrypt(plaintext);
        String ciphertext2 = crypto.encrypt(plaintext);

        // Different IVs should produce different ciphertexts
        assertNotEquals(ciphertext1, ciphertext2);
    }

    @Test
    void getSenderPrivateKeyReturnsDefensiveCopy() {
        byte[] originalKey = hexToBytes(ALICE_PRIVATE_KEY);
        Nip04Crypto crypto = Nip04Crypto.create(originalKey, hexToBytes(BOB_PUBLIC_KEY));

        byte[] returnedKey = crypto.getSenderPrivateKey();
        returnedKey[0] = (byte) 0xFF; // Modify returned array

        // Original should not be affected
        assertArrayEquals(originalKey, crypto.getSenderPrivateKey());
    }

    @Test
    void getRecipientPublicKeyReturnsDefensiveCopy() {
        byte[] originalKey = hexToBytes(BOB_PUBLIC_KEY);
        Nip04Crypto crypto = Nip04Crypto.create(hexToBytes(ALICE_PRIVATE_KEY), originalKey);

        byte[] returnedKey = crypto.getRecipientPublicKey();
        returnedKey[0] = (byte) 0xFF; // Modify returned array

        // Original should not be affected
        assertArrayEquals(originalKey, crypto.getRecipientPublicKey());
    }

    @Test
    void cryptoExceptionContainsMessage() {
        Nip04Crypto.Nip04CryptoException ex = new Nip04Crypto.Nip04CryptoException("test message");
        assertEquals("test message", ex.getMessage());
    }

    @Test
    void cryptoExceptionContainsCause() {
        RuntimeException cause = new RuntimeException("cause");
        Nip04Crypto.Nip04CryptoException ex = new Nip04Crypto.Nip04CryptoException("test", cause);
        assertEquals(cause, ex.getCause());
    }

    private static byte[] hexToBytes(String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            int index = i * 2;
            int value = Integer.parseInt(hex.substring(index, index + 2), 16);
            bytes[i] = (byte) value;
        }
        return bytes;
    }
}
