package xyz.tcheeric.nsecbunker.admin.permission;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.tcheeric.nsecbunker.admin.AdminException;
import xyz.tcheeric.nsecbunker.admin.NsecBunkerAdminClient;
import xyz.tcheeric.nsecbunker.core.model.BunkerPolicy;
import xyz.tcheeric.nsecbunker.core.model.KeyUser;
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
class DefaultPermissionManagerTest {

    @Mock
    private NsecBunkerAdminClient adminClient;

    private ObjectMapper mapper;
    private DefaultPermissionManager permissionManager;

    @BeforeEach
    void setUp() {
        mapper = DefaultPermissionManager.createDefaultObjectMapper();
        permissionManager = new DefaultPermissionManager(adminClient, mapper);
    }

    /**
     * Ensures granting permission sends policy JSON and returns created user metadata.
     */
    @Test
    void shouldGrantPermission() throws Exception {
        // Arrange
        BunkerPolicy policy = BunkerPolicy.builder()
                .id("p1")
                .name("allow-sign")
                .build();
        KeyUser user = KeyUser.builder()
                .keyName("cashu-key")
                .npub("npub1alice")
                .description("alice")
                .lastUsedAt(Instant.parse("2024-01-01T00:00:00Z"))
                .build();
        String userJson = mapper.writeValueAsString(user);
        ArgumentCaptor<Nip46Request> requestCaptor = ArgumentCaptor.forClass(Nip46Request.class);
        when(adminClient.sendRequest(requestCaptor.capture()))
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.success("1", userJson)));

        // Act
        KeyUser result = permissionManager.grantPermission("cashu-key", "npub1alice", policy).join();

        // Assert
        assertThat(result).isEqualTo(user);
        Nip46Request request = requestCaptor.getValue();
        assertThat(request.getMethod()).isEqualTo(DefaultPermissionManager.METHOD_GRANT_PERMISSION);
        assertThat(request.getParams()).hasSize(3);
        BunkerPolicy sentPolicy = mapper.readValue(request.getParams().get(2), BunkerPolicy.class);
        assertThat(sentPolicy).isEqualTo(policy);
    }

    /**
     * Ensures revokePermission issues the command and returns true.
     */
    @Test
    void shouldRevokePermission() {
        // Arrange
        ArgumentCaptor<Nip46Request> requestCaptor = ArgumentCaptor.forClass(Nip46Request.class);
        when(adminClient.sendRequest(requestCaptor.capture()))
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.success("1", "ok")));

        // Act
        boolean result = permissionManager.revokePermission("cashu-key", "npub1alice").join();

        // Assert
        assertThat(result).isTrue();
        Nip46Request request = requestCaptor.getValue();
        assertThat(request.getMethod()).isEqualTo(DefaultPermissionManager.METHOD_REVOKE_PERMISSION);
        assertThat(request.getParams()).containsExactly("cashu-key", "npub1alice");
    }

    /**
     * Ensures listing key users decodes JSON array payload.
     */
    @Test
    void shouldListKeyUsers() throws Exception {
        // Arrange
        List<KeyUser> expected = List.of(
                KeyUser.builder().keyName("k").pubkeyHex("u1").description("one").build(),
                KeyUser.builder().keyName("k").pubkeyHex("u2").description("two").build()
        );
        String json = mapper.writeValueAsString(expected);
        ArgumentCaptor<Nip46Request> requestCaptor = ArgumentCaptor.forClass(Nip46Request.class);
        when(adminClient.sendRequest(requestCaptor.capture()))
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.success("1", json)));

        // Act
        List<KeyUser> result = permissionManager.listKeyUsers("k").join();

        // Assert
        assertThat(result).containsExactlyElementsOf(expected);
        Nip46Request request = requestCaptor.getValue();
        assertThat(request.getMethod()).isEqualTo(DefaultPermissionManager.METHOD_LIST_KEY_USERS);
        assertThat(request.getParams()).containsExactly("k");
    }

    /**
     * Ensures retrieving permissions returns the expected user.
     */
    @Test
    void shouldGetPermissions() throws Exception {
        // Arrange
        KeyUser user = KeyUser.builder()
                .keyName("k")
                .pubkeyHex("u1")
                .description("desc")
                .build();
        String json = mapper.writeValueAsString(user);
        ArgumentCaptor<Nip46Request> requestCaptor = ArgumentCaptor.forClass(Nip46Request.class);
        when(adminClient.sendRequest(requestCaptor.capture()))
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.success("1", json)));

        // Act
        KeyUser result = permissionManager.getPermissions("k", "u1").join();

        // Assert
        assertThat(result).isEqualTo(user);
        Nip46Request request = requestCaptor.getValue();
        assertThat(request.getMethod()).isEqualTo(DefaultPermissionManager.METHOD_GET_PERMISSIONS);
        assertThat(request.getParams()).containsExactly("k", "u1");
    }

    /**
     * Ensures updating description sends the new text and parses the response.
     */
    @Test
    void shouldUpdateDescription() throws Exception {
        // Arrange
        KeyUser updated = KeyUser.builder()
                .keyName("k")
                .pubkeyHex("u1")
                .description("updated")
                .build();
        String json = mapper.writeValueAsString(updated);
        ArgumentCaptor<Nip46Request> requestCaptor = ArgumentCaptor.forClass(Nip46Request.class);
        when(adminClient.sendRequest(requestCaptor.capture()))
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.success("1", json)));

        // Act
        KeyUser result = permissionManager.updateKeyUserDescription("k", "u1", "updated").join();

        // Assert
        assertThat(result).isEqualTo(updated);
        Nip46Request request = requestCaptor.getValue();
        assertThat(request.getMethod()).isEqualTo(DefaultPermissionManager.METHOD_UPDATE_DESCRIPTION);
        assertThat(request.getParams()).containsExactly("k", "u1", "updated");
    }

    /**
     * Ensures NIP-46 errors are surfaced as AdminException.
     */
    @Test
    void shouldSurfaceAdminError() {
        // Arrange
        Nip46Error error = Nip46Error.of("FORBIDDEN", "denied");
        ArgumentCaptor<Nip46Request> requestCaptor = ArgumentCaptor.forClass(Nip46Request.class);
        when(adminClient.sendRequest(requestCaptor.capture()))
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.error("1", error)));

        // Act + Assert
        assertThatThrownBy(() -> permissionManager.getPermissions("k", "u").join())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(AdminException.class)
                .hasRootCauseMessage("get_permissions: [FORBIDDEN] denied");

        Nip46Request request = requestCaptor.getValue();
        assertThat(request.getMethod()).isEqualTo(DefaultPermissionManager.METHOD_GET_PERMISSIONS);
    }
}
