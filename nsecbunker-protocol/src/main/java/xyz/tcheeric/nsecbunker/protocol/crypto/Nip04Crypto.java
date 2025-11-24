package xyz.tcheeric.nsecbunker.protocol.crypto;

import lombok.extern.slf4j.Slf4j;
import nostr.encryption.MessageCipher04;

import java.util.Objects;

/**
 * NIP-04 encryption/decryption utility for NIP-46 protocol communication.
 *
 * <p>This class wraps the nostr-java-encryption library's NIP-04 implementation,
 * providing a clean API for encrypting and decrypting messages in the NIP-46 context.
 *
 * <p>NIP-04 uses:
 * <ul>
 *   <li>ECDH shared secret derivation (secp256k1)</li>
 *   <li>AES-256-CBC encryption</li>
 *   <li>Base64 encoding with IV prepended</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>{@code
 * // Create cipher with sender's private key and recipient's public key
 * Nip04Crypto crypto = Nip04Crypto.create(senderPrivateKeyHex, recipientPubKeyHex);
 *
 * // Encrypt
 * String ciphertext = crypto.encrypt("Hello, World!");
 *
 * // Decrypt (recipient creates cipher with their private key and sender's public key)
 * Nip04Crypto recipientCrypto = Nip04Crypto.create(recipientPrivateKeyHex, senderPubKeyHex);
 * String plaintext = recipientCrypto.decrypt(ciphertext);
 * }</pre>
 *
 * @see <a href="https://github.com/nostr-protocol/nips/blob/master/04.md">NIP-04</a>
 */
@Slf4j
public class Nip04Crypto {

    private final MessageCipher04 cipher;
    private final byte[] senderPrivateKey;
    private final byte[] recipientPublicKey;

    /**
     * Creates a new NIP-04 crypto instance.
     *
     * @param senderPrivateKey  the sender's private key (32 bytes)
     * @param recipientPublicKey the recipient's public key (32 bytes, x-only)
     */
    private Nip04Crypto(byte[] senderPrivateKey, byte[] recipientPublicKey) {
        this.senderPrivateKey = senderPrivateKey.clone();
        this.recipientPublicKey = recipientPublicKey.clone();
        this.cipher = new MessageCipher04(senderPrivateKey, recipientPublicKey);
    }

    /**
     * Creates a NIP-04 crypto instance from hex-encoded keys.
     *
     * @param senderPrivateKeyHex  the sender's private key as hex string
     * @param recipientPublicKeyHex the recipient's public key as hex string
     * @return a new Nip04Crypto instance
     * @throws IllegalArgumentException if keys are invalid
     */
    public static Nip04Crypto create(String senderPrivateKeyHex, String recipientPublicKeyHex) {
        Objects.requireNonNull(senderPrivateKeyHex, "Sender private key must not be null");
        Objects.requireNonNull(recipientPublicKeyHex, "Recipient public key must not be null");

        byte[] privateKey = hexToBytes(senderPrivateKeyHex);
        byte[] publicKey = hexToBytes(recipientPublicKeyHex);

        validateKeyLength(privateKey, "private key");
        validateKeyLength(publicKey, "public key");

        return new Nip04Crypto(privateKey, publicKey);
    }

    /**
     * Creates a NIP-04 crypto instance from byte array keys.
     *
     * @param senderPrivateKey  the sender's private key (32 bytes)
     * @param recipientPublicKey the recipient's public key (32 bytes)
     * @return a new Nip04Crypto instance
     * @throws IllegalArgumentException if keys are invalid
     */
    public static Nip04Crypto create(byte[] senderPrivateKey, byte[] recipientPublicKey) {
        Objects.requireNonNull(senderPrivateKey, "Sender private key must not be null");
        Objects.requireNonNull(recipientPublicKey, "Recipient public key must not be null");

        validateKeyLength(senderPrivateKey, "private key");
        validateKeyLength(recipientPublicKey, "public key");

        return new Nip04Crypto(senderPrivateKey, recipientPublicKey);
    }

    /**
     * Encrypts a plaintext message using NIP-04.
     *
     * @param plaintext the message to encrypt
     * @return the encrypted message in NIP-04 format (base64(ciphertext)?iv=base64(iv))
     * @throws Nip04CryptoException if encryption fails
     */
    public String encrypt(String plaintext) {
        Objects.requireNonNull(plaintext, "Plaintext must not be null");

        try {
            log.debug("Encrypting message of length {}", plaintext.length());
            return cipher.encrypt(plaintext);
        } catch (Exception e) {
            throw new Nip04CryptoException("Failed to encrypt message", e);
        }
    }

    /**
     * Decrypts a NIP-04 encrypted message.
     *
     * @param ciphertext the encrypted message in NIP-04 format
     * @return the decrypted plaintext
     * @throws Nip04CryptoException if decryption fails
     */
    public String decrypt(String ciphertext) {
        Objects.requireNonNull(ciphertext, "Ciphertext must not be null");

        if (!ciphertext.contains("?iv=")) {
            throw new Nip04CryptoException("Invalid NIP-04 format: missing IV separator");
        }

        try {
            log.debug("Decrypting message of length {}", ciphertext.length());
            return cipher.decrypt(ciphertext);
        } catch (Exception e) {
            throw new Nip04CryptoException("Failed to decrypt message", e);
        }
    }

    /**
     * Gets the sender's private key.
     *
     * @return copy of the private key bytes
     */
    public byte[] getSenderPrivateKey() {
        return senderPrivateKey.clone();
    }

    /**
     * Gets the recipient's public key.
     *
     * @return copy of the public key bytes
     */
    public byte[] getRecipientPublicKey() {
        return recipientPublicKey.clone();
    }

    /**
     * Gets the sender's private key as hex string.
     *
     * @return the private key as hex
     */
    public String getSenderPrivateKeyHex() {
        return bytesToHex(senderPrivateKey);
    }

    /**
     * Gets the recipient's public key as hex string.
     *
     * @return the public key as hex
     */
    public String getRecipientPublicKeyHex() {
        return bytesToHex(recipientPublicKey);
    }

    private static void validateKeyLength(byte[] key, String keyType) {
        if (key.length != 32) {
            throw new IllegalArgumentException(
                    String.format("Invalid %s length: expected 32 bytes, got %d", keyType, key.length));
        }
    }

    private static byte[] hexToBytes(String hex) {
        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("Hex string must have even length");
        }

        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < bytes.length; i++) {
            int index = i * 2;
            int value = Integer.parseInt(hex.substring(index, index + 2), 16);
            bytes[i] = (byte) value;
        }
        return bytes;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    /**
     * Exception thrown when NIP-04 encryption or decryption fails.
     */
    public static class Nip04CryptoException extends RuntimeException {

        /**
         * Creates a new crypto exception.
         *
         * @param message the error message
         */
        public Nip04CryptoException(String message) {
            super(message);
        }

        /**
         * Creates a new crypto exception with a cause.
         *
         * @param message the error message
         * @param cause   the underlying cause
         */
        public Nip04CryptoException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
