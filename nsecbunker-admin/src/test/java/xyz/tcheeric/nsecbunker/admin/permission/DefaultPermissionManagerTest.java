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
     * Ensures granting permission sends policyId and returns the KeyUser directly.
     * nsecbunkerd returns the KeyUser object directly, so no follow-up query needed.
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
                .pubkeyHex("npub1alice")
                .description("alice")
                .lastUsedAt(Instant.parse("2024-01-01T00:00:00Z"))
                .build();
        String userJson = mapper.writeValueAsString(user);

        ArgumentCaptor<Nip46Request> requestCaptor = ArgumentCaptor.forClass(Nip46Request.class);
        // grant_permission returns the KeyUser object directly
        when(adminClient.sendRequest(requestCaptor.capture()))
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.success("1", userJson)));

        // Act
        KeyUser result = permissionManager.grantPermission("cashu-key", "npub1alice", policy).join();

        // Assert
        assertThat(result.getPubkeyHex()).isEqualTo(user.getPubkeyHex());
        List<Nip46Request> requests = requestCaptor.getAllValues();
        assertThat(requests).hasSize(1);
        // Only one request: grant_permission
        assertThat(requests.get(0).getMethod()).isEqualTo(DefaultPermissionManager.METHOD_GRANT_PERMISSION);
        assertThat(requests.get(0).getParams()).hasSize(3);
        assertThat(requests.get(0).getParams().get(2)).isEqualTo(policy.getId());
    }

    /**
     * Ensures revokePermission first queries listKeyUsers to get the id, then revokes by id.
     * nsecbunkerd's revoke_user expects [keyUserId], not [keyName, userPubkey].
     */
    @Test
    void shouldRevokePermission() throws Exception {
        // Arrange
        KeyUser user = KeyUser.builder()
                .id("user-123")
                .keyName("cashu-key")
                .pubkeyHex("npub1alice")
                .build();
        List<KeyUser> userList = List.of(user);
        String userListJson = mapper.writeValueAsString(userList);

        ArgumentCaptor<Nip46Request> requestCaptor = ArgumentCaptor.forClass(Nip46Request.class);
        // First call: get_key_users returns user list with id
        // Second call: revoke_user returns ["ok"]
        when(adminClient.sendRequest(requestCaptor.capture()))
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.success("1", userListJson)))
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.success("2", "[\"ok\"]")));

        // Act
        boolean result = permissionManager.revokePermission("cashu-key", "npub1alice").join();

        // Assert
        assertThat(result).isTrue();
        List<Nip46Request> requests = requestCaptor.getAllValues();
        assertThat(requests).hasSize(2);
        // First request: get_key_users to find the user id
        assertThat(requests.get(0).getMethod()).isEqualTo(DefaultPermissionManager.METHOD_LIST_KEY_USERS);
        assertThat(requests.get(0).getParams()).containsExactlyElementsOf(List.of("cashu-key"));
        // Second request: revoke_user with [keyUserId]
        assertThat(requests.get(1).getMethod()).isEqualTo(DefaultPermissionManager.METHOD_REVOKE_PERMISSION);
        assertThat(requests.get(1).getParams()).containsExactlyElementsOf(List.of("user-123"));
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
        assertThat(request.getParams()).containsExactlyElementsOf(List.of("k"));
    }

    /**
     * Ensures retrieving permissions returns the expected user.
     * getPermissions() now uses listKeyUsers and filters by pubkey.
     */
    @Test
    void shouldGetPermissions() throws Exception {
        // Arrange
        KeyUser user = KeyUser.builder()
                .keyName("k")
                .pubkeyHex("u1")
                .description("desc")
                .build();
        List<KeyUser> userList = List.of(user);
        String json = mapper.writeValueAsString(userList);
        ArgumentCaptor<Nip46Request> requestCaptor = ArgumentCaptor.forClass(Nip46Request.class);
        when(adminClient.sendRequest(requestCaptor.capture()))
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.success("1", json)));

        // Act
        KeyUser result = permissionManager.getPermissions("k", "u1").join();

        // Assert
        assertThat(result.getPubkeyHex()).isEqualTo(user.getPubkeyHex());
        assertThat(result.getDescription()).isEqualTo(user.getDescription());
        Nip46Request request = requestCaptor.getValue();
        // getPermissions now delegates to listKeyUsers (get_key_users)
        assertThat(request.getMethod()).isEqualTo(DefaultPermissionManager.METHOD_LIST_KEY_USERS);
        assertThat(request.getParams()).containsExactly("k");
    }

    /**
     * Ensures updating description sends [userPubkey, description] and queries list for result.
     * nsecbunkerd's rename_key_user expects [userPubkey, name], returns ["ok"], then we query list.
     */
    @Test
    void shouldUpdateDescription() throws Exception {
        // Arrange
        KeyUser updated = KeyUser.builder()
                .keyName("k")
                .pubkeyHex("u1")
                .description("updated")
                .build();
        List<KeyUser> userList = List.of(updated);
        String userListJson = mapper.writeValueAsString(userList);

        ArgumentCaptor<Nip46Request> requestCaptor = ArgumentCaptor.forClass(Nip46Request.class);
        // First call: rename_key_user returns ["ok"]
        // Second call: get_key_users returns the user list
        when(adminClient.sendRequest(requestCaptor.capture()))
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.success("1", "[\"ok\"]")))
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.success("2", userListJson)));

        // Act
        KeyUser result = permissionManager.updateKeyUserDescription("k", "u1", "updated").join();

        // Assert
        assertThat(result.getDescription()).isEqualTo(updated.getDescription());
        List<Nip46Request> requests = requestCaptor.getAllValues();
        assertThat(requests).hasSize(2);
        // First request: rename_key_user with [userPubkey, description]
        assertThat(requests.get(0).getMethod()).isEqualTo(DefaultPermissionManager.METHOD_UPDATE_DESCRIPTION);
        assertThat(requests.get(0).getParams()).containsExactlyElementsOf(List.of("u1", "updated"));
        // Second request: get_key_users
        assertThat(requests.get(1).getMethod()).isEqualTo(DefaultPermissionManager.METHOD_LIST_KEY_USERS);
    }

    /**
     * Ensures NIP-46 errors are surfaced as AdminException.
     * getPermissions now delegates to listKeyUsers (get_key_users).
     */
    @Test
    void shouldSurfaceAdminError() {
        // Arrange
        Nip46Error error = Nip46Error.of("FORBIDDEN", "denied");
        ArgumentCaptor<Nip46Request> requestCaptor = ArgumentCaptor.forClass(Nip46Request.class);
        when(adminClient.sendRequest(requestCaptor.capture()))
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.error("1", error)));

        // Act + Assert
        // getPermissions now uses get_key_users method
        assertThatThrownBy(() -> permissionManager.getPermissions("k", "u").join())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(AdminException.class)
                .hasRootCauseMessage("get_key_users: [FORBIDDEN] denied");

        Nip46Request request = requestCaptor.getValue();
        assertThat(request.getMethod()).isEqualTo(DefaultPermissionManager.METHOD_LIST_KEY_USERS);
    }
}
