package xyz.tcheeric.nsecbunker.examples;

import xyz.tcheeric.nsecbunker.client.signer.NsecBunkerSigner;
import xyz.tcheeric.nsecbunker.client.signer.SignerConfig;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Example demonstrating nsecBunker integration with a Cashu client.
 *
 * <p>This example shows how to use nsecBunker for remote signing in a Cashu
 * (ecash) application context. The signer can be used to:
 * <ul>
 *   <li>Sign Nostr events for cashu token operations</li>
 *   <li>Encrypt/decrypt messages using NIP-04 or NIP-44</li>
 *   <li>Authenticate with cashu mints that require Nostr identity</li>
 * </ul>
 *
 * <h2>Setup</h2>
 * <p>Set the following environment variables:
 * <pre>
 * export BUNKER_PUBKEY="your-bunker-pubkey-hex-or-npub"
 * export CLIENT_PRIVKEY="your-client-private-key-hex-or-nsec"
 * export RELAY_URL="wss://your-relay.example.com"
 * </pre>
 *
 * <h2>Usage with Cashu Client</h2>
 * <p>In a real application, you would integrate this signer with your
 * cashu client library. The signer provides:
 * <ul>
 *   <li>{@code signEvent()} - Sign Nostr events (kind 7375, 7376 for cashu)</li>
 *   <li>{@code getPublicKey()} - Get the signing public key</li>
 *   <li>{@code nip04Encrypt/Decrypt()} - Legacy encryption for mints</li>
 *   <li>{@code nip44Encrypt/Decrypt()} - Modern encryption for tokens</li>
 * </ul>
 */
public final class CashuIntegrationExample {

    public static void main(String[] args) {
        System.out.println("=== nsecBunker Cashu Integration Example ===\n");

        // Load configuration from environment
        String bunkerPubkey = getenv("BUNKER_PUBKEY");
        String clientPrivkey = getenv("CLIENT_PRIVKEY");
        String relay = getenv("RELAY_URL");

        // Create the remote signer
        NsecBunkerSigner signer = createSigner(bunkerPubkey, clientPrivkey, relay);

        System.out.println("Signer initialized with communication pubkey: " +
                signer.getCommunicationIdentity().getPublicKey());
        System.out.println();

        // Demonstrate cashu-related operations
        demonstrateCashuOperations(signer);
    }

    /**
     * Creates and configures the NsecBunkerSigner.
     */
    private static NsecBunkerSigner createSigner(String bunkerPubkey,
                                                  String clientPrivkey,
                                                  String relay) {
        SignerConfig config = SignerConfig.builder()
                .bunkerPubkey(bunkerPubkey)
                .clientPrivateKey(clientPrivkey)
                .relays(List.of(relay))
                .useEphemeralKey(true)  // Recommended for privacy
                .connectTimeout(Duration.ofSeconds(30))
                .requestTimeout(Duration.ofSeconds(60))
                .build();

        // In production, provide a real transport handler
        // that connects to nostr relays
        return new NsecBunkerSigner(
                config,
                request -> {
                    throw new UnsupportedOperationException(
                            "Connect to real relay transport for production use");
                },
                null,
                null
        );
    }

    /**
     * Demonstrates cashu-related signing operations.
     */
    private static void demonstrateCashuOperations(NsecBunkerSigner signer) {
        System.out.println("--- Cashu Operation Examples ---\n");

        // Example 1: Create a cashu token event (kind 7375)
        demonstrateCashuTokenEvent(signer);

        // Example 2: Create a cashu history event (kind 7376)
        demonstrateCashuHistoryEvent(signer);

        // Example 3: Encrypt token data with NIP-44
        demonstrateTokenEncryption(signer);

        // Example 4: Sign authentication challenge
        demonstrateMintAuthentication(signer);
    }

    /**
     * Example: Sign a cashu token event (NIP-60 kind 7375).
     */
    private static void demonstrateCashuTokenEvent(NsecBunkerSigner signer) {
        System.out.println("1. Signing Cashu Token Event (kind 7375)");

        // Cashu token event structure
        String tokenEventJson = """
                {
                    "kind": 7375,
                    "created_at": %d,
                    "tags": [
                        ["a", "37375:<mint-pubkey>:<mint-url>"],
                        ["e", "<proof-event-id>"]
                    ],
                    "content": "encrypted-cashu-token-data"
                }
                """.formatted(Instant.now().getEpochSecond());

        System.out.println("   Event to sign: " + tokenEventJson.substring(0, 50) + "...");

        // In production:
        // CompletableFuture<String> signedEvent = signer.signEvent(tokenEventJson);
        // signedEvent.thenAccept(signed -> {
        //     // Publish to relay or send to recipient
        //     System.out.println("Signed event: " + signed);
        // });

        System.out.println("   [Would call signer.signEvent() in production]\n");
    }

    /**
     * Example: Sign a cashu wallet history event (kind 7376).
     */
    private static void demonstrateCashuHistoryEvent(NsecBunkerSigner signer) {
        System.out.println("2. Signing Cashu History Event (kind 7376)");

        String historyEventJson = """
                {
                    "kind": 7376,
                    "created_at": %d,
                    "tags": [
                        ["a", "37375:<mint-pubkey>:<mint-url>"],
                        ["direction", "in"],
                        ["amount", "100"],
                        ["unit", "sat"]
                    ],
                    "content": ""
                }
                """.formatted(Instant.now().getEpochSecond());

        System.out.println("   History event to sign: " + historyEventJson.substring(0, 50) + "...");
        System.out.println("   [Would call signer.signEvent() in production]\n");
    }

    /**
     * Example: Encrypt cashu token data using NIP-44.
     */
    private static void demonstrateTokenEncryption(NsecBunkerSigner signer) {
        System.out.println("3. Encrypting Cashu Token Data (NIP-44)");

        String recipientPubkey = "recipient-pubkey-hex";
        String tokenData = """
                {
                    "token": [
                        {
                            "mint": "https://mint.example.com",
                            "proofs": [
                                {"amount": 1, "secret": "...", "C": "..."}
                            ]
                        }
                    ]
                }
                """;

        System.out.println("   Token data to encrypt: " + tokenData.substring(0, 40) + "...");

        // In production:
        // CompletableFuture<String> encrypted = signer.nip44Encrypt(recipientPubkey, tokenData);
        // encrypted.thenAccept(ciphertext -> {
        //     // Include in event content
        //     System.out.println("Encrypted: " + ciphertext);
        // });

        System.out.println("   [Would call signer.nip44Encrypt() in production]\n");
    }

    /**
     * Example: Authenticate with a mint using Nostr identity.
     */
    private static void demonstrateMintAuthentication(NsecBunkerSigner signer) {
        System.out.println("4. Mint Authentication");

        // Some mints may require proof of Nostr identity
        String authChallengeJson = """
                {
                    "kind": 22242,
                    "created_at": %d,
                    "tags": [
                        ["relay", "wss://mint-relay.example.com"],
                        ["challenge", "mint-provided-challenge-string"]
                    ],
                    "content": ""
                }
                """.formatted(Instant.now().getEpochSecond());

        System.out.println("   Auth challenge to sign: " + authChallengeJson.substring(0, 50) + "...");

        // In production:
        // CompletableFuture<String> signedAuth = signer.signEvent(authChallengeJson);
        // signedAuth.thenAccept(signed -> {
        //     // Send to mint for verification
        // });

        System.out.println("   [Would call signer.signEvent() in production]\n");
    }

    /**
     * Example: Full cashu send flow with nsecBunker.
     */
    public static CompletableFuture<Void> sendCashuTokens(
            NsecBunkerSigner signer,
            String recipientPubkey,
            String tokenJson) {

        // Step 1: Encrypt token data with recipient's key
        return signer.nip44Encrypt(recipientPubkey, tokenJson)
                .thenCompose(encryptedToken -> {
                    // Step 2: Create token event
                    String eventJson = """
                            {
                                "kind": 7375,
                                "created_at": %d,
                                "tags": [["p", "%s"]],
                                "content": "%s"
                            }
                            """.formatted(
                            Instant.now().getEpochSecond(),
                            recipientPubkey,
                            encryptedToken
                    );

                    // Step 3: Sign the event
                    return signer.signEvent(eventJson);
                })
                .thenAccept(signedEvent -> {
                    // Step 4: Publish to relays
                    System.out.println("Token event signed and ready to publish");
                    // relayPool.publish(signedEvent);
                });
    }

    /**
     * Example: Full cashu receive flow with nsecBunker.
     */
    public static CompletableFuture<String> receiveCashuTokens(
            NsecBunkerSigner signer,
            String senderPubkey,
            String encryptedContent) {

        // Decrypt the token data from sender
        return signer.nip44Decrypt(senderPubkey, encryptedContent)
                .thenApply(tokenJson -> {
                    System.out.println("Received cashu token: " + tokenJson);
                    // Parse and redeem with mint
                    return tokenJson;
                });
    }

    private static String getenv(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required environment variable: " + key);
        }
        return value;
    }
}
