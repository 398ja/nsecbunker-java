package xyz.tcheeric.nsecbunker.examples.springboot;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * REST controller for nsecBunker signing operations.
 *
 * <p>Provides HTTP endpoints for:
 * <ul>
 *   <li>Signing Nostr events</li>
 *   <li>Getting the signing public key</li>
 *   <li>Encrypting/decrypting messages</li>
 *   <li>Health and status checks</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>
 * # Sign an event
 * curl -X POST http://localhost:8080/api/sign \
 *   -H "Content-Type: application/json" \
 *   -d '{"kind":1,"content":"Hello Nostr!","created_at":1234567890,"tags":[]}'
 *
 * # Get public key
 * curl http://localhost:8080/api/pubkey
 *
 * # Encrypt a message
 * curl -X POST http://localhost:8080/api/encrypt \
 *   -H "Content-Type: application/json" \
 *   -d '{"pubkey":"recipient-pubkey","plaintext":"secret message"}'
 * </pre>
 */
@RestController
@RequestMapping("/api")
public class SigningController {

    private final SigningService signingService;

    public SigningController(SigningService signingService) {
        this.signingService = signingService;
    }

    /**
     * Signs a Nostr event.
     *
     * @param eventJson the unsigned event JSON
     * @return the signed event JSON
     */
    @PostMapping("/sign")
    public CompletableFuture<ResponseEntity<SignResponse>> signEvent(@RequestBody String eventJson) {
        return signingService.signEvent(eventJson)
                .thenApply(signedEvent -> ResponseEntity.ok(new SignResponse(signedEvent, null)))
                .exceptionally(ex -> ResponseEntity.badRequest()
                        .body(new SignResponse(null, ex.getMessage())));
    }

    /**
     * Gets the signing public key.
     *
     * @return the public key
     */
    @GetMapping("/pubkey")
    public CompletableFuture<ResponseEntity<Map<String, String>>> getPublicKey() {
        return signingService.getPublicKey()
                .thenApply(pubkey -> ResponseEntity.ok(Map.of("pubkey", pubkey)))
                .exceptionally(ex -> ResponseEntity.internalServerError()
                        .body(Map.of("error", ex.getMessage())));
    }

    /**
     * Encrypts a message using NIP-44.
     *
     * @param request the encryption request
     * @return the ciphertext
     */
    @PostMapping("/encrypt")
    public CompletableFuture<ResponseEntity<EncryptResponse>> encrypt(
            @RequestBody EncryptRequest request) {
        return signingService.encrypt(request.pubkey(), request.plaintext())
                .thenApply(ciphertext -> ResponseEntity.ok(new EncryptResponse(ciphertext, null)))
                .exceptionally(ex -> ResponseEntity.badRequest()
                        .body(new EncryptResponse(null, ex.getMessage())));
    }

    /**
     * Decrypts a message using NIP-44.
     *
     * @param request the decryption request
     * @return the plaintext
     */
    @PostMapping("/decrypt")
    public CompletableFuture<ResponseEntity<DecryptResponse>> decrypt(
            @RequestBody DecryptRequest request) {
        return signingService.decrypt(request.pubkey(), request.ciphertext())
                .thenApply(plaintext -> ResponseEntity.ok(new DecryptResponse(plaintext, null)))
                .exceptionally(ex -> ResponseEntity.badRequest()
                        .body(new DecryptResponse(null, ex.getMessage())));
    }

    /**
     * Pings the bunker to check connectivity.
     *
     * @return pong response
     */
    @GetMapping("/ping")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> ping() {
        return signingService.ping()
                .thenApply(pong -> ResponseEntity.ok(Map.of(
                        "status", "ok",
                        "response", pong
                )))
                .exceptionally(ex -> ResponseEntity.internalServerError()
                        .body(Map.of(
                                "status", "error",
                                "error", ex.getMessage()
                        )));
    }

    /**
     * Gets the current status of the signing service.
     *
     * @return status information
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
                "connected", signingService.isConnected(),
                "activeOperations", signingService.getActiveOperations()
        ));
    }

    // Request/Response records

    record SignResponse(String signedEvent, String error) {}

    record EncryptRequest(String pubkey, String plaintext) {}

    record EncryptResponse(String ciphertext, String error) {}

    record DecryptRequest(String pubkey, String ciphertext) {}

    record DecryptResponse(String plaintext, String error) {}
}
