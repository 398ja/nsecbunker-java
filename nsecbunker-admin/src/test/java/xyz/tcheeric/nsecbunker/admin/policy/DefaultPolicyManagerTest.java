package xyz.tcheeric.nsecbunker.admin.policy;

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
import xyz.tcheeric.nsecbunker.core.model.PolicyRule;
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
class DefaultPolicyManagerTest {

    @Mock
    private NsecBunkerAdminClient adminClient;

    private DefaultPolicyManager policyManager;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = DefaultPolicyManager.createDefaultObjectMapper();
        policyManager = new DefaultPolicyManager(adminClient, objectMapper);
    }

    /**
     * Ensures createPolicy serializes the payload and returns the created policy.
     * nsecbunkerd returns ["ok"] on success, so we query listPolicies to get the created policy.
     */
    @Test
    void shouldCreatePolicy() throws Exception {
        // Arrange
        BunkerPolicy policy = BunkerPolicy.builder()
                .name("policy-1")
                .description("signing policy")
                .rule(PolicyRule.allowMethod("sign_event"))
                .build();
        BunkerPolicy policyWithId = policy.toBuilder().id("generated-id").build();
        List<BunkerPolicy> policyList = List.of(policyWithId);
        String policyListJson = objectMapper.writeValueAsString(policyList);

        ArgumentCaptor<Nip46Request> requestCaptor = ArgumentCaptor.forClass(Nip46Request.class);
        // First call: create_new_policy returns ["ok"]
        // Second call: get_policies returns the policy list
        when(adminClient.sendRequest(requestCaptor.capture()))
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.success("1", "[\"ok\"]")))
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.success("2", policyListJson)));

        // Act
        BunkerPolicy result = policyManager.createPolicy(policy).join();

        // Assert - returns the policy found in the list (with generated id)
        assertThat(result.getName()).isEqualTo(policy.getName());
        assertThat(result.getId()).isEqualTo("generated-id");
        List<Nip46Request> requests = requestCaptor.getAllValues();
        assertThat(requests).hasSize(2);
        // First request: create_new_policy
        assertThat(requests.get(0).getMethod()).isEqualTo(DefaultPolicyManager.METHOD_CREATE_POLICY);
        assertThat(requests.get(0).getParams()).hasSize(1);
        BunkerPolicy sentPolicy = objectMapper.readValue(requests.get(0).getParams().get(0).toString(), BunkerPolicy.class);
        assertThat(sentPolicy.getName()).isEqualTo(policy.getName());
        // Second request: get_policies
        assertThat(requests.get(1).getMethod()).isEqualTo(DefaultPolicyManager.METHOD_LIST_POLICIES);
    }

    /**
     * Ensures listPolicies decodes the JSON payload into policy instances.
     */
    @Test
    void shouldListPolicies() throws Exception {
        // Arrange
        List<BunkerPolicy> expected = List.of(
                BunkerPolicy.builder().id("1").name("p1").build(),
                BunkerPolicy.builder().id("2").name("p2").active(false).expiresAt(Instant.parse("2024-01-01T00:00:00Z")).build()
        );
        String json = objectMapper.writeValueAsString(expected);
        ArgumentCaptor<Nip46Request> requestCaptor = ArgumentCaptor.forClass(Nip46Request.class);
        when(adminClient.sendRequest(requestCaptor.capture()))
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.success("1", json)));

        // Act
        List<BunkerPolicy> result = policyManager.listPolicies().join();

        // Assert
        assertThat(result).containsExactlyElementsOf(expected);
        Nip46Request request = requestCaptor.getValue();
        assertThat(request.getMethod()).isEqualTo(DefaultPolicyManager.METHOD_LIST_POLICIES);
        assertThat(request.getParams()).isEmpty();
    }

    /**
     * Ensures getPolicy returns the requested policy metadata.
     */
    @Test
    void shouldGetPolicy() throws Exception {
        // Arrange
        BunkerPolicy expected = BunkerPolicy.builder()
                .id("42")
                .name("policy-42")
                .rule(PolicyRule.allowEventKind(1))
                .build();
        String json = objectMapper.writeValueAsString(expected);
        ArgumentCaptor<Nip46Request> requestCaptor = ArgumentCaptor.forClass(Nip46Request.class);
        when(adminClient.sendRequest(requestCaptor.capture()))
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.success("1", json)));

        // Act
        BunkerPolicy result = policyManager.getPolicy("42").join();

        // Assert
        assertThat(result).isEqualTo(expected);
        Nip46Request request = requestCaptor.getValue();
        assertThat(request.getMethod()).isEqualTo(DefaultPolicyManager.METHOD_GET_POLICY);
        assertThat(request.getParams()).containsExactly("42");
    }

    /**
     * Ensures deletePolicy issues the delete command and reports success.
     */
    @Test
    void shouldDeletePolicy() {
        // Arrange
        ArgumentCaptor<Nip46Request> requestCaptor = ArgumentCaptor.forClass(Nip46Request.class);
        when(adminClient.sendRequest(requestCaptor.capture()))
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.success("1", "deleted")));

        // Act
        boolean result = policyManager.deletePolicy("99").join();

        // Assert
        assertThat(result).isTrue();
        Nip46Request request = requestCaptor.getValue();
        assertThat(request.getMethod()).isEqualTo(DefaultPolicyManager.METHOD_DELETE_POLICY);
        assertThat(request.getParams()).containsExactly("99");
    }

    /**
     * Ensures NIP-46 errors are surfaced as AdminException.
     */
    @Test
    void shouldSurfaceAdminError() {
        // Arrange
        Nip46Error error = Nip46Error.of("NOT_FOUND", "policy missing");
        ArgumentCaptor<Nip46Request> requestCaptor = ArgumentCaptor.forClass(Nip46Request.class);
        when(adminClient.sendRequest(requestCaptor.capture()))
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.error("1", error)));

        // Act + Assert
        assertThatThrownBy(() -> policyManager.getPolicy("missing").join())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(AdminException.class)
                .hasRootCauseMessage("get_policy: [NOT_FOUND] policy missing");

        Nip46Request request = requestCaptor.getValue();
        assertThat(request.getMethod()).isEqualTo(DefaultPolicyManager.METHOD_GET_POLICY);
    }
}
