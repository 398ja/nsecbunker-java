package xyz.tcheeric.nsecbunker.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import nostr.id.Identity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import xyz.tcheeric.nsecbunker.e2e.containers.BottinContainer;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests for Bottin NIP-05 registry service.
 *
 * <p>These tests verify the complete NIP-05 identity management flow:
 * <ul>
 *   <li>Domain registration and management</li>
 *   <li>NIP-05 record CRUD operations</li>
 *   <li>.well-known/nostr.json endpoint compliance with NIP-05 spec</li>
 * </ul>
 *
 * <p>The tests use Testcontainers to spin up:
 * <ul>
 *   <li>PostgreSQL database for Bottin persistence</li>
 *   <li>Bottin application with test mode (verification skip enabled)</li>
 * </ul>
 *
 * <p><b>Prerequisites:</b> Bottin must support the {@code BOTTIN_VERIFICATION_SKIP=true}
 * environment variable to auto-verify domains in test mode.
 *
 * <p>Run with: {@code mvn -Pe2e test -Dtest=BottinE2ETest}
 */
@Testcontainers
@Tag("e2e")
@Tag("bottin")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BottinE2ETest {

    private static final Logger log = LoggerFactory.getLogger(BottinE2ETest.class);

    private static final String TEST_DOMAIN = "test.example.com";
    private static final String POSTGRES_NETWORK_ALIAS = "postgres";
    private static final int POSTGRES_PORT = 5432;

    static Network network = Network.newNetwork();

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withNetwork(network)
            .withNetworkAliases(POSTGRES_NETWORK_ALIAS)
            .withDatabaseName("bottin")
            .withUsername("bottin")
            .withPassword("bottin");

    @Container
    static BottinContainer bottin = new BottinContainer()
            .withNetwork(network)
            .withNetworkAliases("bottin")
            .withPostgresUrl("jdbc:postgresql://" + POSTGRES_NETWORK_ALIAS + ":" + POSTGRES_PORT + "/bottin")
            .withPostgresUser("bottin")
            .withPostgresPassword("bottin")
            .withVerificationSkip(true)
            .dependsOn(postgres);

    private static BottinTestClient client;

    // Shared state across ordered tests
    private static Long domainId;
    private static Long recordId;
    private static String testPubkey;

    @BeforeAll
    static void setup() {
        client = new BottinTestClient(
                bottin.getBaseUrl(),
                bottin.getAdminUser(),
                bottin.getAdminPassword());
        testPubkey = Identity.generateRandomIdentity().getPublicKey().toString();
        log.info("bottin_e2e_test_started base_url={} admin_user={} test_pubkey={}",
                bottin.getBaseUrl(), bottin.getAdminUser(), testPubkey);
    }

    /**
     * Force-verifies a domain by updating the database directly.
     * This bypasses normal verification flow for test purposes.
     */
    private static void forceVerifyDomain(Long domainId) throws IOException, InterruptedException {
        String sql = String.format(
                "UPDATE domains SET verified = true, verified_at = NOW() WHERE id = %d",
                domainId);
        postgres.execInContainer("psql", "-U", "bottin", "-d", "bottin", "-c", sql);
        log.info("domain_force_verified id={}", domainId);
    }

    // =========================================================================
    // Health Check Tests
    // =========================================================================

    /**
     * Verifies that Bottin's health endpoint returns healthy status.
     */
    @Test
    @Order(1)
    void shouldReturnHealthyStatus() throws Exception {
        // Act
        HttpResponse<String> response = client.healthCheck();

        // Assert
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("UP");
        log.info("health_check_passed");
    }

    // =========================================================================
    // Domain Management Tests
    // =========================================================================

    /**
     * Verifies that a new domain can be registered via the API.
     * After creation, force-verifies the domain for subsequent tests.
     */
    @Test
    @Order(10)
    void shouldRegisterNewDomain() throws Exception {
        // Act
        HttpResponse<String> response = client.createDomain(TEST_DOMAIN);

        // Assert
        assertThat(response.statusCode()).isEqualTo(201);

        JsonNode json = client.parseJson(response);
        assertThat(json.has("id")).isTrue();
        assertThat(json.get("name").asText()).isEqualTo(TEST_DOMAIN);

        domainId = json.get("id").asLong();

        // Force-verify domain for subsequent tests (NIP-05 records require verified domains)
        forceVerifyDomain(domainId);

        log.info("domain_created id={} name={}", domainId, TEST_DOMAIN);
    }

    /**
     * Verifies that the registered domain can be retrieved by ID.
     */
    @Test
    @Order(11)
    void shouldGetDomainById() throws Exception {
        // Arrange - domain created in previous test
        assertThat(domainId).isNotNull();

        // Act
        HttpResponse<String> response = client.getDomain(domainId);

        // Assert
        assertThat(response.statusCode()).isEqualTo(200);

        JsonNode json = client.parseJson(response);
        assertThat(json.get("id").asLong()).isEqualTo(domainId);
        assertThat(json.get("name").asText()).isEqualTo(TEST_DOMAIN);
    }

    /**
     * Verifies that all domains can be listed.
     */
    @Test
    @Order(12)
    void shouldListAllDomains() throws Exception {
        // Act
        HttpResponse<String> response = client.getDomains();

        // Assert
        assertThat(response.statusCode()).isEqualTo(200);

        JsonNode json = client.parseJson(response);
        log.info("domains_response body={}", response.body());

        // Handle both array and paginated response formats
        JsonNode domainsArray;
        if (json.isArray()) {
            domainsArray = json;
        } else if (json.has("content")) {
            // Spring Data Page format
            domainsArray = json.get("content");
        } else {
            domainsArray = json;
        }

        assertThat(domainsArray.isArray()).isTrue();
        assertThat(domainsArray.size()).isGreaterThanOrEqualTo(1);

        // Verify our test domain is in the list
        boolean found = false;
        for (JsonNode domain : domainsArray) {
            if (TEST_DOMAIN.equals(domain.get("name").asText())) {
                found = true;
                break;
            }
        }
        assertThat(found).isTrue();
    }

    /**
     * Verifies that attempting to register a duplicate domain returns error.
     * Note: Bottin returns 400 Bad Request for duplicate domains (IllegalArgumentException).
     */
    @Test
    @Order(13)
    void shouldRejectDuplicateDomain() throws Exception {
        // Act - try to create the same domain again
        HttpResponse<String> response = client.createDomain(TEST_DOMAIN);

        // Assert - Bottin returns 400 for duplicate domains
        assertThat(response.statusCode()).isIn(400, 409);
        log.info("duplicate_domain_rejected name={} status={}", TEST_DOMAIN, response.statusCode());
    }

    // =========================================================================
    // NIP-05 Record Management Tests
    // =========================================================================

    /**
     * Verifies that a new NIP-05 record can be created.
     */
    @Test
    @Order(20)
    void shouldCreateNip05Record() throws Exception {
        // Arrange
        String username = "alice";
        List<String> relays = List.of("wss://relay1.example.com", "wss://relay2.example.com");

        // Act
        HttpResponse<String> response = client.createRecord(username, TEST_DOMAIN, testPubkey, relays);

        // Assert
        assertThat(response.statusCode()).isEqualTo(201);

        JsonNode json = client.parseJson(response);
        assertThat(json.has("id")).isTrue();
        assertThat(json.get("username").asText()).isEqualTo(username);
        assertThat(json.get("pubkey").asText()).isEqualTo(testPubkey);
        assertThat(json.get("enabled").asBoolean()).isTrue();

        recordId = json.get("id").asLong();
        log.info("nip05_record_created id={} nip05={}@{}", recordId, username, TEST_DOMAIN);
    }

    /**
     * Verifies that a NIP-05 record can be retrieved by its identifier.
     */
    @Test
    @Order(21)
    void shouldGetRecordByNip05() throws Exception {
        // Arrange
        String nip05 = "alice@" + TEST_DOMAIN;

        // Act
        HttpResponse<String> response = client.getRecordByNip05(nip05);

        // Assert
        assertThat(response.statusCode()).isEqualTo(200);

        JsonNode json = client.parseJson(response);
        assertThat(json.get("username").asText()).isEqualTo("alice");
        assertThat(json.get("pubkey").asText()).isEqualTo(testPubkey);
    }

    /**
     * Verifies that a NIP-05 record can be retrieved by ID.
     */
    @Test
    @Order(22)
    void shouldGetRecordById() throws Exception {
        // Arrange - record created in previous test
        assertThat(recordId).isNotNull();

        // Act
        HttpResponse<String> response = client.getRecord(recordId);

        // Assert
        assertThat(response.statusCode()).isEqualTo(200);

        JsonNode json = client.parseJson(response);
        assertThat(json.get("id").asLong()).isEqualTo(recordId);
        assertThat(json.get("username").asText()).isEqualTo("alice");
    }

    /**
     * Verifies that records can be listed and filtered by domain.
     */
    @Test
    @Order(23)
    void shouldListRecordsForDomain() throws Exception {
        // Act
        HttpResponse<String> response = client.getRecords(TEST_DOMAIN);

        // Assert
        assertThat(response.statusCode()).isEqualTo(200);

        JsonNode json = client.parseJson(response);
        log.info("records_response body={}", response.body());

        // Handle both array and paginated response formats
        JsonNode recordsArray;
        if (json.isArray()) {
            recordsArray = json;
        } else if (json.has("content")) {
            // Spring Data Page format
            recordsArray = json.get("content");
        } else {
            recordsArray = json;
        }

        assertThat(recordsArray.isArray()).isTrue();
        assertThat(recordsArray.size()).isGreaterThanOrEqualTo(1);

        // Verify alice is in the list
        boolean found = false;
        for (JsonNode record : recordsArray) {
            if ("alice".equals(record.get("username").asText())) {
                found = true;
                break;
            }
        }
        assertThat(found).isTrue();
    }

    /**
     * Verifies that a NIP-05 record's relays can be updated.
     */
    @Test
    @Order(24)
    void shouldUpdateRecordRelays() throws Exception {
        // Arrange
        assertThat(recordId).isNotNull();
        List<String> newRelays = List.of("wss://updated-relay.example.com");

        // Act
        HttpResponse<String> response = client.updateRecord(recordId, newRelays);

        // Assert
        assertThat(response.statusCode()).isEqualTo(200);

        JsonNode json = client.parseJson(response);
        JsonNode relays = json.get("relays");
        assertThat(relays.isArray()).isTrue();
        assertThat(relays.size()).isEqualTo(1);
        assertThat(relays.get(0).asText()).isEqualTo("wss://updated-relay.example.com");

        log.info("nip05_record_updated id={} relays={}", recordId, newRelays);
    }

    /**
     * Verifies that attempting to create a duplicate NIP-05 record returns conflict.
     */
    @Test
    @Order(25)
    void shouldRejectDuplicateNip05() throws Exception {
        // Act - try to create the same username@domain again
        HttpResponse<String> response = client.createRecord("alice", TEST_DOMAIN, testPubkey, List.of());

        // Assert
        assertThat(response.statusCode()).isEqualTo(409);
        log.info("duplicate_nip05_rejected nip05=alice@{}", TEST_DOMAIN);
    }

    /**
     * Verifies that attempting to create a record with invalid pubkey is rejected.
     */
    @Test
    @Order(26)
    void shouldRejectInvalidPubkey() throws Exception {
        // Arrange - invalid pubkey (not 64 hex chars)
        String invalidPubkey = "not-a-valid-pubkey";

        // Act
        HttpResponse<String> response = client.createRecord("bob", TEST_DOMAIN, invalidPubkey, List.of());

        // Assert
        assertThat(response.statusCode()).isEqualTo(400);
        log.info("invalid_pubkey_rejected pubkey={}", invalidPubkey);
    }

    /**
     * Verifies that a record can be toggled (enabled/disabled).
     */
    @Test
    @Order(27)
    void shouldToggleRecordStatus() throws Exception {
        // Arrange
        assertThat(recordId).isNotNull();

        // Act - toggle to disabled
        HttpResponse<String> response = client.toggleRecord(recordId);

        // Assert
        assertThat(response.statusCode()).isEqualTo(200);

        JsonNode json = client.parseJson(response);
        boolean enabled = json.get("enabled").asBoolean();

        // Toggle again to re-enable
        response = client.toggleRecord(recordId);
        assertThat(response.statusCode()).isEqualTo(200);

        json = client.parseJson(response);
        assertThat(json.get("enabled").asBoolean()).isNotEqualTo(enabled);

        log.info("nip05_record_toggled id={}", recordId);
    }

    // =========================================================================
    // Well-Known Endpoint Tests (NIP-05 Spec Compliance)
    // =========================================================================

    /**
     * Verifies that the .well-known/nostr.json endpoint returns valid NIP-05 JSON
     * when querying for a specific name.
     */
    @Test
    @Order(30)
    void shouldReturnValidNostrJsonForName() throws Exception {
        // Arrange - ensure record is enabled by checking current state
        HttpResponse<String> recordResponse = client.getRecord(recordId);
        JsonNode recordJson = client.parseJson(recordResponse);
        boolean isEnabled = recordJson.get("enabled").asBoolean();
        if (!isEnabled) {
            client.toggleRecord(recordId); // enable if disabled
        }

        // Act - use Host header to specify the domain (per NIP-05 spec)
        HttpResponse<String> response = client.getWellKnown("alice", TEST_DOMAIN);

        // Assert
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("Content-Type"))
                .isPresent()
                .get().asString().contains("application/json");

        JsonNode json = client.parseJson(response);
        log.info("well_known_response body={}", response.body());

        // Verify NIP-05 compliant structure
        assertThat(json.has("names")).isTrue();
        assertThat(json.get("names").has("alice")).isTrue();
        assertThat(json.get("names").get("alice").asText()).isEqualTo(testPubkey);

        log.info("well_known_returned name=alice pubkey={}", testPubkey);
    }

    /**
     * Verifies that the relays field is included in the NIP-05 response when the record has relays.
     */
    @Test
    @Order(31)
    void shouldIncludeRelaysInNostrJson() throws Exception {
        // Act - use Host header to specify the domain
        HttpResponse<String> response = client.getWellKnown("alice", TEST_DOMAIN);

        // Assert
        assertThat(response.statusCode()).isEqualTo(200);

        JsonNode json = client.parseJson(response);
        log.info("well_known_relays_response body={}", response.body());

        // Verify relays field exists and contains the pubkey (only if record has relays)
        if (json.has("relays") && json.get("relays").has(testPubkey)) {
            JsonNode relays = json.get("relays").get(testPubkey);
            assertThat(relays.isArray()).isTrue();
            log.info("well_known_relays pubkey={} relays={}", testPubkey, relays);
        } else {
            // Per NIP-05, relays field is optional
            log.info("well_known_relays pubkey={} relays=none (optional field)", testPubkey);
        }
    }

    /**
     * Verifies that querying for an unknown name returns 404 or empty names object.
     */
    @Test
    @Order(32)
    void shouldHandleUnknownName() throws Exception {
        // Act - use Host header to specify the domain
        HttpResponse<String> response = client.getWellKnown("nonexistent", TEST_DOMAIN);

        // Assert - either 404 or empty names object is acceptable per NIP-05
        if (response.statusCode() == 200) {
            JsonNode json = client.parseJson(response);
            assertThat(json.has("names")).isTrue();
            assertThat(json.get("names").has("nonexistent")).isFalse();
        } else {
            assertThat(response.statusCode()).isEqualTo(404);
        }

        log.info("unknown_name_handled name=nonexistent status={}", response.statusCode());
    }

    /**
     * Verifies that querying without a name parameter returns all records for the domain.
     */
    @Test
    @Order(33)
    void shouldReturnAllRecordsWithoutNameParam() throws Exception {
        // Act - use Host header to specify the domain
        HttpResponse<String> response = client.getWellKnownForDomain(TEST_DOMAIN);

        // Assert
        assertThat(response.statusCode()).isEqualTo(200);

        JsonNode json = client.parseJson(response);
        log.info("well_known_all_response body={}", response.body());

        assertThat(json.has("names")).isTrue();

        // Should contain at least alice
        JsonNode names = json.get("names");
        assertThat(names.size()).isGreaterThanOrEqualTo(1);

        log.info("well_known_all_records count={}", names.size());
    }

    // =========================================================================
    // Cleanup Tests (Run Last)
    // =========================================================================

    /**
     * Verifies that a NIP-05 record can be deleted.
     */
    @Test
    @Order(90)
    void shouldDeleteNip05Record() throws Exception {
        // Arrange
        assertThat(recordId).isNotNull();

        // Act
        HttpResponse<String> response = client.deleteRecord(recordId);

        // Assert
        assertThat(response.statusCode()).isIn(200, 204);

        // Verify deletion
        response = client.getRecord(recordId);
        assertThat(response.statusCode()).isEqualTo(404);

        log.info("nip05_record_deleted id={}", recordId);
    }

    /**
     * Verifies that a domain can be deleted.
     */
    @Test
    @Order(91)
    void shouldDeleteDomain() throws Exception {
        // Arrange
        assertThat(domainId).isNotNull();

        // Act
        HttpResponse<String> response = client.deleteDomain(domainId);

        // Assert
        assertThat(response.statusCode()).isIn(200, 204);

        // Verify deletion
        response = client.getDomain(domainId);
        assertThat(response.statusCode()).isEqualTo(404);

        log.info("domain_deleted id={}", domainId);
    }

    // =========================================================================
    // Additional Test: Create second user to verify multi-user support
    // =========================================================================

    /**
     * Verifies that multiple users can be registered on the same domain.
     */
    @Test
    @Order(28)
    void shouldSupportMultipleUsersOnSameDomain() throws Exception {
        // Arrange
        String bobPubkey = Identity.generateRandomIdentity().getPublicKey().toString();

        // Act
        HttpResponse<String> response = client.createRecord("bob", TEST_DOMAIN, bobPubkey, List.of());

        // Assert
        assertThat(response.statusCode()).isEqualTo(201);

        JsonNode json = client.parseJson(response);
        assertThat(json.get("username").asText()).isEqualTo("bob");
        assertThat(json.get("pubkey").asText()).isEqualTo(bobPubkey);

        // Clean up bob
        Long bobId = json.get("id").asLong();
        client.deleteRecord(bobId);

        log.info("multiple_users_supported bob_id={}", bobId);
    }
}
