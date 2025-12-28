package xyz.tcheeric.nsecbunker.admin.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.tcheeric.nsecbunker.admin.NsecBunkerAdminClient;
import xyz.tcheeric.nsecbunker.admin.key.DefaultKeyManager;
import xyz.tcheeric.nsecbunker.admin.policy.DefaultPolicyManager;
import xyz.tcheeric.nsecbunker.admin.permission.DefaultPermissionManager;
import xyz.tcheeric.nsecbunker.admin.token.DefaultTokenManager;
import xyz.tcheeric.nsecbunker.core.model.AccessToken;
import xyz.tcheeric.nsecbunker.core.model.BunkerKey;
import xyz.tcheeric.nsecbunker.core.model.BunkerPolicy;
import xyz.tcheeric.nsecbunker.core.model.KeyUser;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Request;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Response;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Tag("integration")
@ExtendWith(MockitoExtension.class)
class AdminIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminIntegrationTest.class);

    @Mock
    private NsecBunkerAdminClient adminClient;

    private ObjectMapper mapper;
    private DefaultKeyManager keyManager;
    private DefaultPolicyManager policyManager;
    private DefaultPermissionManager permissionManager;
    private DefaultTokenManager tokenManager;

    @BeforeAll
    static void noteExpectedConnectionErrors() {
        LOGGER.info("Admin integration tests stub the admin client; any relay connection errors seen during IT runs are expected.");
    }

    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        keyManager = new DefaultKeyManager(adminClient, mapper);
        policyManager = new DefaultPolicyManager(adminClient, mapper);
        permissionManager = new DefaultPermissionManager(adminClient, mapper);
        tokenManager = new DefaultTokenManager(adminClient, mapper);
    }

    /**
     * Verifies key CRUD flow over the admin managers using a shared client stub.
     */
    @Test
    void shouldPerformKeyCrudFlow() throws Exception {
        // Arrange: create -> list -> get -> delete
        BunkerKey created = BunkerKey.builder()
                .name("cashu-key")
                .npub("npub1cashu")
                .createdAt(Instant.parse("2024-01-01T00:00:00Z"))
                .build();
        BunkerKey fetched = created.toBuilder()
                .description("fetched")
                .build();
        String createdJson = mapper.writeValueAsString(created);
        String listJson = mapper.writeValueAsString(List.of(created));
        String fetchedJson = mapper.writeValueAsString(fetched);

        when(adminClient.sendRequest(any()))
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.success("1", createdJson))) // create
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.success("2", listJson)))    // list
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.success("3", "ok")))       // unlock
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.success("4", fetchedJson)))// details
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.success("5", "deleted"))); // delete

        // Act
        BunkerKey createdResult = keyManager.createKey("cashu-key", "nsec1test", "pass").join();
        List<BunkerKey> keys = keyManager.listKeys().join();
        boolean unlocked = keyManager.unlockKey("cashu-key", "pass").join();
        BunkerKey details = keyManager.getKeyDetails("cashu-key").join();
        boolean deleted = keyManager.deleteKey("cashu-key").join();

        // Assert
        assertThat(createdResult).isEqualTo(created);
        assertThat(keys).containsExactly(created);
        assertThat(unlocked).isTrue();
        assertThat(details).isEqualTo(fetched);
        assertThat(deleted).isTrue();
    }

    /**
     * Verifies policy CRUD flow using the shared admin client stub.
     */
    @Test
    void shouldPerformPolicyCrudFlow() throws Exception {
        // Arrange: create -> list -> get -> delete
        BunkerPolicy policy = BunkerPolicy.builder()
                .id("p1")
                .name("allow-sign")
                .createdAt(Instant.parse("2024-02-01T00:00:00Z"))
                .build();
        String policyJson = mapper.writeValueAsString(policy);
        String listJson = mapper.writeValueAsString(List.of(policy));

        when(adminClient.sendRequest(any()))
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.success("1", policyJson))) // create
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.success("2", listJson)))   // list
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.success("3", policyJson))) // get
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.success("4", "deleted"))); // delete

        // Act
        BunkerPolicy created = policyManager.createPolicy(policy).join();
        List<BunkerPolicy> policies = policyManager.listPolicies().join();
        BunkerPolicy fetched = policyManager.getPolicy("p1").join();
        boolean deleted = policyManager.deletePolicy("p1").join();

        // Assert
        assertThat(created).isEqualTo(policy);
        assertThat(policies).containsExactly(policy);
        assertThat(fetched).isEqualTo(policy);
        assertThat(deleted).isTrue();
    }

    /**
     * Verifies permission grant, lookup, update, list, and revoke flow.
     */
    @Test
    void shouldGrantAndManagePermissions() throws Exception {
        // Arrange
        BunkerPolicy policy = BunkerPolicy.builder()
                .id("p2")
                .name("policy")
                .build();
        KeyUser granted = KeyUser.builder()
                .keyName("k")
                .npub("npub1user")
                .description("granted")
                .build();
        KeyUser updated = granted.toBuilder()
                .description("updated")
                .build();
        String grantedJson = mapper.writeValueAsString(granted);
        String updatedJson = mapper.writeValueAsString(updated);
        String listJson = mapper.writeValueAsString(List.of(granted));

        when(adminClient.sendRequest(any()))
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.success("1", grantedJson)))  // grant
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.success("2", listJson)))    // list
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.success("3", grantedJson))) // get
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.success("4", updatedJson))) // update
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.success("5", "revoked")));  // revoke

        // Act
        KeyUser grantedResult = permissionManager.grantPermission("k", "npub1user", policy).join();
        List<KeyUser> users = permissionManager.listKeyUsers("k").join();
        KeyUser fetched = permissionManager.getPermissions("k", "npub1user").join();
        KeyUser updatedResult = permissionManager.updateKeyUserDescription("k", "npub1user", "updated").join();
        boolean revoked = permissionManager.revokePermission("k", "npub1user").join();

        // Assert
        assertThat(grantedResult).isEqualTo(granted);
        assertThat(users).containsExactly(granted);
        assertThat(fetched).isEqualTo(granted);
        assertThat(updatedResult).isEqualTo(updated);
        assertThat(revoked).isTrue();
    }

    /**
     * Verifies token create/list/get/revoke/validate flow.
     */
    @Test
    void shouldManageTokens() throws Exception {
        // Arrange
        AccessToken token = AccessToken.builder()
                .id("tok-1")
                .keyName("k")
                .clientName("cli")
                .policyId("p1")
                .createdAt(Instant.parse("2024-03-01T00:00:00Z"))
                .build();
        String tokenJson = mapper.writeValueAsString(token);
        String listJson = mapper.writeValueAsString(List.of(token));

        ArgumentCaptor<Nip46Request> requestCaptor = ArgumentCaptor.forClass(Nip46Request.class);
        when(adminClient.sendRequest(requestCaptor.capture()))
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.success("1", tokenJson))) // create
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.success("2", listJson)))  // list
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.success("3", tokenJson))) // get
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.success("4", "revoked"))) // revoke
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.success("5", "true")));   // validate

        // Act
        AccessToken created = tokenManager.createToken("k", "cli", "p1", Duration.ofMinutes(5)).join();
        List<AccessToken> tokens = tokenManager.listTokens("k").join();
        AccessToken fetched = tokenManager.getToken("tok-1").join();
        boolean revoked = tokenManager.revokeToken("tok-1").join();
        boolean valid = tokenManager.validateToken("token-value").join();

        // Assert
        assertThat(created).isEqualTo(token);
        assertThat(tokens).containsExactly(token);
        assertThat(fetched).isEqualTo(token);
        assertThat(revoked).isTrue();
        assertThat(valid).isTrue();

        // Verify create params include lifetime seconds
        Nip46Request firstRequest = requestCaptor.getAllValues().get(0);
        assertThat(firstRequest.getParams()).contains("300");
    }
}
