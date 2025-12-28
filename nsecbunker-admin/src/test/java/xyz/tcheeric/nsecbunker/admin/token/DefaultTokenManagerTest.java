package xyz.tcheeric.nsecbunker.admin.token;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.tcheeric.nsecbunker.admin.AdminException;
import xyz.tcheeric.nsecbunker.admin.NsecBunkerAdminClient;
import xyz.tcheeric.nsecbunker.core.model.AccessToken;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Error;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Request;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Response;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultTokenManagerTest {

    @Mock
    private NsecBunkerAdminClient adminClient;

    private ObjectMapper mapper;
    private DefaultTokenManager tokenManager;

    @BeforeEach
    void setUp() {
        mapper = DefaultTokenManager.createDefaultObjectMapper();
        tokenManager = new DefaultTokenManager(adminClient, mapper);
    }

    /**
     * Ensures creating a token sends expected params and parses the returned token metadata.
     * nsecbunkerd returns ["ok"] on create, so we query listTokens to get the created token.
     */
    @Test
    void shouldCreateToken() throws Exception {
        // Arrange
        AccessToken token = AccessToken.builder()
                .id("t1")
                .keyName("cashu-key")
                .clientName("mobile")
                .policyId("policy-1")
                .createdAt(Instant.parse("2024-01-01T00:00:00Z"))
                .expiresAt(Instant.parse("2024-01-02T00:00:00Z"))
                .relay("wss://relay.example.com")
                .build();
        List<AccessToken> tokenList = List.of(token);
        String tokenListJson = mapper.writeValueAsString(tokenList);

        ArgumentCaptor<Nip46Request> requestCaptor = ArgumentCaptor.forClass(Nip46Request.class);
        // First call: create_new_token returns ["ok"]
        // Second call: get_key_tokens returns the token list
        when(adminClient.sendRequest(requestCaptor.capture()))
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.success("1", "[\"ok\"]")))
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.success("2", tokenListJson)));

        // Act
        AccessToken result = tokenManager.createToken("cashu-key", "mobile", "policy-1", Duration.ofHours(1)).join();

        // Assert
        assertThat(result.getId()).isEqualTo(token.getId());
        assertThat(result.getClientName()).isEqualTo(token.getClientName());
        List<Nip46Request> requests = requestCaptor.getAllValues();
        assertThat(requests).hasSize(2);
        // First request: create_new_token with [keyName, clientName, policyId, durationInHours]
        assertThat(requests.get(0).getMethod()).isEqualTo(DefaultTokenManager.METHOD_CREATE_TOKEN);
        assertThat(requests.get(0).getParams()).containsExactlyElementsOf(List.of("cashu-key", "mobile", "policy-1", "1"));
        // Second request: get_key_tokens
        assertThat(requests.get(1).getMethod()).isEqualTo(DefaultTokenManager.METHOD_LIST_TOKENS);
    }

    /**
     * Ensures listTokens parses the JSON array payload.
     */
    @Test
    void shouldListTokens() throws Exception {
        // Arrange
        List<AccessToken> tokens = List.of(
                AccessToken.builder().id("1").keyName("k").clientName("c1").build(),
                AccessToken.builder().id("2").keyName("k").clientName("c2").revoked(true).build()
        );
        String json = mapper.writeValueAsString(tokens);
        ArgumentCaptor<Nip46Request> requestCaptor = ArgumentCaptor.forClass(Nip46Request.class);
        when(adminClient.sendRequest(requestCaptor.capture()))
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.success("1", json)));

        // Act
        List<AccessToken> result = tokenManager.listTokens("k").join();

        // Assert
        assertThat(result).containsExactlyElementsOf(tokens);
        Nip46Request request = requestCaptor.getValue();
        assertThat(request.getMethod()).isEqualTo(DefaultTokenManager.METHOD_LIST_TOKENS);
        assertThat(request.getParams()).containsExactly("k");
    }

    /**
     * Ensures getToken returns the requested token.
     */
    @Test
    void shouldGetToken() throws Exception {
        // Arrange
        AccessToken token = AccessToken.builder()
                .id("token-123")
                .keyName("key")
                .clientName("cli")
                .build();
        String json = mapper.writeValueAsString(token);
        ArgumentCaptor<Nip46Request> requestCaptor = ArgumentCaptor.forClass(Nip46Request.class);
        when(adminClient.sendRequest(requestCaptor.capture()))
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.success("1", json)));

        // Act
        AccessToken result = tokenManager.getToken("token-123").join();

        // Assert
        assertThat(result).isEqualTo(token);
        Nip46Request request = requestCaptor.getValue();
        assertThat(request.getMethod()).isEqualTo(DefaultTokenManager.METHOD_GET_TOKEN);
        assertThat(request.getParams()).containsExactly("token-123");
    }

    /**
     * Ensures revokeToken issues the revoke command and returns true.
     */
    @Test
    void shouldRevokeToken() {
        // Arrange
        ArgumentCaptor<Nip46Request> requestCaptor = ArgumentCaptor.forClass(Nip46Request.class);
        when(adminClient.sendRequest(requestCaptor.capture()))
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.success("1", "revoked")));

        // Act
        boolean result = tokenManager.revokeToken("token-1").join();

        // Assert
        assertThat(result).isTrue();
        Nip46Request request = requestCaptor.getValue();
        assertThat(request.getMethod()).isEqualTo(DefaultTokenManager.METHOD_REVOKE_TOKEN);
        assertThat(request.getParams()).containsExactly("token-1");
    }

    /**
     * Ensures validateToken parses boolean response strings.
     */
    @Test
    void shouldValidateToken() {
        // Arrange
        ArgumentCaptor<Nip46Request> requestCaptor = ArgumentCaptor.forClass(Nip46Request.class);
        when(adminClient.sendRequest(requestCaptor.capture()))
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.success("1", "true")));

        // Act
        boolean valid = tokenManager.validateToken("token-value").join();

        // Assert
        assertThat(valid).isTrue();
        Nip46Request request = requestCaptor.getValue();
        assertThat(request.getMethod()).isEqualTo(DefaultTokenManager.METHOD_VALIDATE_TOKEN);
        assertThat(request.getParams()).containsExactly("token-value");
    }

    /**
     * Ensures NIP-46 errors surface as AdminException.
     */
    @Test
    void shouldSurfaceAdminError() {
        // Arrange
        Nip46Error error = Nip46Error.of("INVALID", "bad token");
        ArgumentCaptor<Nip46Request> requestCaptor = ArgumentCaptor.forClass(Nip46Request.class);
        when(adminClient.sendRequest(requestCaptor.capture()))
                .thenReturn(CompletableFuture.completedFuture(Nip46Response.error("1", error)));

        // Act + Assert
        assertThatThrownBy(() -> tokenManager.getToken("bad").join())
                .isInstanceOf(CompletionException.class)
                .hasCauseInstanceOf(AdminException.class)
                .hasRootCauseMessage("get_token: [INVALID] bad token");

        Nip46Request request = requestCaptor.getValue();
        assertThat(request.getMethod()).isEqualTo(DefaultTokenManager.METHOD_GET_TOKEN);
    }
}
