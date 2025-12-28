package xyz.tcheeric.nsecbunker.admin.key;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.tcheeric.nsecbunker.admin.AdminException;
import xyz.tcheeric.nsecbunker.admin.NsecBunkerAdminClient;
import xyz.tcheeric.nsecbunker.core.model.BunkerKey;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Error;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Request;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Response;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultKeyManagerTest {

    private static final String TEST_NSEC = "nsec1alicesample";
    private static final String TEST_PASSPHRASE = "super-secret";

    @Mock
    private NsecBunkerAdminClient adminClient;

    private ObjectMapper objectMapper;
    private DefaultKeyManager keyManager;

    @BeforeEach
    void setUp() {
        objectMapper = DefaultKeyManager.createDefaultObjectMapper();
        keyManager = new DefaultKeyManager(adminClient, objectMapper);
    }

    /**
     * Ensures importing an existing nsec builds the correct request and returns parsed metadata.
     */
    @Test
    void shouldCreateKeyWithNsecAndParseResponse() throws Exception {
        // Arrange
        BunkerKey expectedKey = BunkerKey.builder()
                .name("cashu-alice")
                .npub("npub1alice")
                .pubkeyHex("aabbcc")
                .locked(false)
                .userCount(2)
                .tokenCount(1)
                .signingCount(5)
                .createdAt(Instant.parse("2024-01-01T00:00:00Z"))
                .lastUsedAt(Instant.parse("2024-01-02T00:00:00Z"))
                .description("Alice key")
                .build();
        String json = objectMapper.writeValueAsString(expectedKey);
        ArgumentCaptor<Nip46Request> requestCaptor = ArgumentCaptor.forClass(Nip46Request.class);
        when(adminClient.sendRequest(requestCaptor.capture()))
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.success("1", json)));

        // Act
        BunkerKey result = keyManager.createKey(expectedKey.getName(), TEST_NSEC, TEST_PASSPHRASE).join();

        // Assert
        assertThat(result).isEqualTo(expectedKey);
        Nip46Request request = requestCaptor.getValue();
        assertThat(request.getMethod()).isEqualTo(DefaultKeyManager.METHOD_CREATE_NEW_KEY);
        assertThat(request.getParams()).containsExactlyElementsOf(List.of(expectedKey.getName(), TEST_PASSPHRASE, TEST_NSEC));
    }

    /**
     * Ensures generating a new key without supplying an nsec still returns usable minimal metadata.
     */
    @Test
    void shouldCreateKeyWithoutNsecAndReturnMinimalKey() {
        // Arrange
        String npub = "npub1generated";
        ArgumentCaptor<Nip46Request> requestCaptor = ArgumentCaptor.forClass(Nip46Request.class);
        when(adminClient.sendRequest(requestCaptor.capture()))
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.success("1", npub)));

        // Act
        BunkerKey result = keyManager.createKey("cashu-generated", TEST_PASSPHRASE).join();

        // Assert
        assertThat(result.getName()).isEqualTo("cashu-generated");
        assertThat(result.getNpub()).isEqualTo(npub);
        assertThat(result.getPubkeyHex()).isNull();

        Nip46Request request = requestCaptor.getValue();
        assertThat(request.getParams()).containsExactlyElementsOf(List.of("cashu-generated", TEST_PASSPHRASE));
    }

    /**
     * Ensures listKeys parses the JSON payload into a collection of bunker keys.
     */
    @Test
    void shouldListKeysFromJsonArray() throws Exception {
        // Arrange
        List<BunkerKey> expectedKeys = List.of(
                BunkerKey.builder().name("key-1").npub("npub1k1").build(),
                BunkerKey.builder().name("key-2").npub("npub1k2").locked(true).build()
        );
        String json = objectMapper.writeValueAsString(expectedKeys);
        ArgumentCaptor<Nip46Request> requestCaptor = ArgumentCaptor.forClass(Nip46Request.class);
        when(adminClient.sendRequest(requestCaptor.capture()))
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.success("1", json)));

        // Act
        List<BunkerKey> result = keyManager.listKeys().join();

        // Assert
        assertThat(result).containsExactlyElementsOf(expectedKeys);
        Nip46Request request = requestCaptor.getValue();
        assertThat(request.getMethod()).isEqualTo(DefaultKeyManager.METHOD_GET_KEYS);
        assertThat(request.getParams()).isEmpty();
    }

    /**
     * Ensures unlockKey sends the right parameters and reports success on a non-error response.
     */
    @Test
    void shouldUnlockKeyAndReturnTrue() {
        // Arrange
        ArgumentCaptor<Nip46Request> requestCaptor = ArgumentCaptor.forClass(Nip46Request.class);
        when(adminClient.sendRequest(requestCaptor.capture()))
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.success("1", "ok")));

        // Act
        boolean result = keyManager.unlockKey("cashu-locker", TEST_PASSPHRASE).join();

        // Assert
        assertThat(result).isTrue();
        Nip46Request request = requestCaptor.getValue();
        assertThat(request.getMethod()).isEqualTo(DefaultKeyManager.METHOD_UNLOCK_KEY);
        assertThat(request.getParams()).containsExactlyElementsOf(List.of("cashu-locker", TEST_PASSPHRASE));
    }

    /**
     * Ensures deleteKey sends the delete command and completes successfully.
     */
    @Test
    void shouldDeleteKey() {
        // Arrange
        ArgumentCaptor<Nip46Request> requestCaptor = ArgumentCaptor.forClass(Nip46Request.class);
        when(adminClient.sendRequest(requestCaptor.capture()))
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.success("1", "deleted")));

        // Act
        boolean result = keyManager.deleteKey("cashu-legacy").join();

        // Assert
        assertThat(result).isTrue();
        Nip46Request request = requestCaptor.getValue();
        assertThat(request.getMethod()).isEqualTo(DefaultKeyManager.METHOD_DELETE_KEY);
        assertThat(request.getParams()).containsExactlyElementsOf(List.of("cashu-legacy"));
    }

    /**
     * Ensures getKeyDetails returns full metadata for a specific key.
     */
    @Test
    void shouldGetKeyDetails() throws Exception {
        // Arrange
        BunkerKey expectedKey = BunkerKey.builder()
                .name("cashu-detail")
                .npub("npub1details")
                .createdAt(Instant.parse("2024-03-01T00:00:00Z"))
                .build();
        String json = objectMapper.writeValueAsString(expectedKey);
        ArgumentCaptor<Nip46Request> requestCaptor = ArgumentCaptor.forClass(Nip46Request.class);
        when(adminClient.sendRequest(requestCaptor.capture()))
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.success("1", json)));

        // Act
        BunkerKey result = keyManager.getKeyDetails("cashu-detail").join();

        // Assert
        assertThat(result).isEqualTo(expectedKey);
        Nip46Request request = requestCaptor.getValue();
        assertThat(request.getMethod()).isEqualTo(DefaultKeyManager.METHOD_GET_KEY);
        assertThat(request.getParams()).containsExactlyElementsOf(List.of("cashu-detail"));
    }

    /**
     * Ensures rotateKey requests the rotation and returns the new key metadata.
     */
    @Test
    void shouldRotateKey() throws Exception {
        // Arrange
        BunkerKey rotatedKey = BunkerKey.builder()
                .name("cashu-rotated")
                .npub("npub1rotated")
                .build();
        String json = objectMapper.writeValueAsString(rotatedKey);
        ArgumentCaptor<Nip46Request> requestCaptor = ArgumentCaptor.forClass(Nip46Request.class);
        when(adminClient.sendRequest(requestCaptor.capture()))
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.success("1", json)));

        // Act
        BunkerKey result = keyManager.rotateKey("cashu-old", "cashu-rotated", TEST_PASSPHRASE).join();

        // Assert
        assertThat(result).isEqualTo(rotatedKey);
        Nip46Request request = requestCaptor.getValue();
        assertThat(request.getMethod()).isEqualTo(DefaultKeyManager.METHOD_ROTATE_KEY);
        assertThat(request.getParams()).containsExactlyElementsOf(List.of("cashu-old", "cashu-rotated", TEST_PASSPHRASE));
    }

    /**
     * Ensures NIP-46 errors are surfaced as AdminException through the future.
     */
    @Test
    void shouldSurfaceAdminErrorWhenResponseContainsError() {
        // Arrange
        ArgumentCaptor<Nip46Request> requestCaptor = ArgumentCaptor.forClass(Nip46Request.class);
        Nip46Error error = Nip46Error.of("NOT_FOUND", "missing key");
        when(adminClient.sendRequest(requestCaptor.capture()))
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.error("1", error)));

        // Act + Assert
        assertThatThrownBy(() -> keyManager.getKeyDetails("missing").join())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(AdminException.class)
                .hasRootCauseMessage("get_key: [NOT_FOUND] missing key");

        Nip46Request request = requestCaptor.getValue();
        assertThat(request.getMethod()).isEqualTo(DefaultKeyManager.METHOD_GET_KEY);
    }
}
