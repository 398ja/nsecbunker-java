package xyz.tcheeric.nsecbunker.e2e;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * HTTP client for interacting with the Bottin REST API during e2e tests.
 *
 * <p>Provides convenient methods for domain management, NIP-05 record operations,
 * and well-known endpoint verification.
 *
 * <p>Supports Basic Authentication for secured API endpoints.
 */
public class BottinTestClient {

    private static final Logger log = LoggerFactory.getLogger(BottinTestClient.class);

    private final HttpClient httpClient;
    private final String baseUrl;
    private final String apiUrl;
    private final String wellKnownUrl;
    private final ObjectMapper objectMapper;
    private final String authHeader;

    /**
     * Creates a new BottinTestClient without authentication.
     *
     * @param baseUrl the base URL of the Bottin service
     */
    public BottinTestClient(String baseUrl) {
        this(baseUrl, null, null);
    }

    /**
     * Creates a new BottinTestClient with Basic Authentication.
     *
     * @param baseUrl  the base URL of the Bottin service
     * @param username the admin username
     * @param password the admin password
     */
    public BottinTestClient(String baseUrl, String username, String password) {
        this.baseUrl = baseUrl;
        this.apiUrl = baseUrl + "/api/v1";
        this.wellKnownUrl = baseUrl + "/.well-known/nostr.json";
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();

        if (username != null && password != null) {
            String credentials = username + ":" + password;
            this.authHeader = "Basic " + Base64.getEncoder()
                    .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        } else {
            this.authHeader = null;
        }
    }

    /**
     * Adds authentication header to the request builder if credentials are configured.
     */
    private HttpRequest.Builder addAuth(HttpRequest.Builder builder) {
        if (authHeader != null) {
            builder.header("Authorization", authHeader);
        }
        return builder;
    }

    // --- Domain Operations ---

    /**
     * Creates a new domain.
     *
     * @param name the domain name (e.g., "example.com")
     * @return the HTTP response
     */
    public HttpResponse<String> createDomain(String name) throws IOException, InterruptedException {
        String json = objectMapper.writeValueAsString(Map.of("name", name));

        HttpRequest request = addAuth(HttpRequest.newBuilder())
                .uri(URI.create(apiUrl + "/domains"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        log.debug("create_domain name={} status={}", name, response.statusCode());
        return response;
    }

    /**
     * Lists all domains.
     *
     * @return the HTTP response with domain list
     */
    public HttpResponse<String> getDomains() throws IOException, InterruptedException {
        HttpRequest request = addAuth(HttpRequest.newBuilder())
                .uri(URI.create(apiUrl + "/domains"))
                .GET()
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Gets a domain by ID.
     *
     * @param id the domain ID
     * @return the HTTP response
     */
    public HttpResponse<String> getDomain(Long id) throws IOException, InterruptedException {
        HttpRequest request = addAuth(HttpRequest.newBuilder())
                .uri(URI.create(apiUrl + "/domains/" + id))
                .GET()
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Deletes a domain by ID.
     *
     * @param id the domain ID
     * @return the HTTP response
     */
    public HttpResponse<String> deleteDomain(Long id) throws IOException, InterruptedException {
        HttpRequest request = addAuth(HttpRequest.newBuilder())
                .uri(URI.create(apiUrl + "/domains/" + id))
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        log.debug("delete_domain id={} status={}", id, response.statusCode());
        return response;
    }

    // --- NIP-05 Record Operations ---

    /**
     * Creates a new NIP-05 record.
     *
     * @param username the username (e.g., "alice")
     * @param domain   the domain (e.g., "example.com")
     * @param pubkey   the public key in hex format (64 chars)
     * @param relays   optional list of relay URLs
     * @return the HTTP response
     */
    public HttpResponse<String> createRecord(String username, String domain, String pubkey, List<String> relays)
            throws IOException, InterruptedException {
        Map<String, Object> body = Map.of(
                "username", username,
                "domain", domain,
                "pubkey", pubkey,
                "relays", relays != null ? relays : List.of()
        );
        String json = objectMapper.writeValueAsString(body);

        HttpRequest request = addAuth(HttpRequest.newBuilder())
                .uri(URI.create(apiUrl + "/records"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        log.debug("create_record nip05={}@{} status={}", username, domain, response.statusCode());
        return response;
    }

    /**
     * Lists all NIP-05 records, optionally filtered by domain.
     *
     * @param domain optional domain filter
     * @return the HTTP response with record list
     */
    public HttpResponse<String> getRecords(String domain) throws IOException, InterruptedException {
        String url = apiUrl + "/records";
        if (domain != null && !domain.isEmpty()) {
            url += "?domain=" + domain;
        }

        HttpRequest request = addAuth(HttpRequest.newBuilder())
                .uri(URI.create(url))
                .GET()
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Gets a NIP-05 record by its identifier.
     *
     * @param nip05 the full NIP-05 identifier (e.g., "alice@example.com")
     * @return the HTTP response
     */
    public HttpResponse<String> getRecordByNip05(String nip05) throws IOException, InterruptedException {
        HttpRequest request = addAuth(HttpRequest.newBuilder())
                .uri(URI.create(apiUrl + "/records/by-nip05/" + nip05))
                .GET()
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Gets a NIP-05 record by ID.
     *
     * @param id the record ID
     * @return the HTTP response
     */
    public HttpResponse<String> getRecord(Long id) throws IOException, InterruptedException {
        HttpRequest request = addAuth(HttpRequest.newBuilder())
                .uri(URI.create(apiUrl + "/records/" + id))
                .GET()
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Updates a NIP-05 record's relays.
     *
     * @param id     the record ID
     * @param relays the new relay list
     * @return the HTTP response
     */
    public HttpResponse<String> updateRecord(Long id, List<String> relays) throws IOException, InterruptedException {
        String json = objectMapper.writeValueAsString(Map.of("relays", relays));

        HttpRequest request = addAuth(HttpRequest.newBuilder())
                .uri(URI.create(apiUrl + "/records/" + id))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        log.debug("update_record id={} status={}", id, response.statusCode());
        return response;
    }

    /**
     * Deletes a NIP-05 record by ID.
     *
     * @param id the record ID
     * @return the HTTP response
     */
    public HttpResponse<String> deleteRecord(Long id) throws IOException, InterruptedException {
        HttpRequest request = addAuth(HttpRequest.newBuilder())
                .uri(URI.create(apiUrl + "/records/" + id))
                .DELETE()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        log.debug("delete_record id={} status={}", id, response.statusCode());
        return response;
    }

    /**
     * Toggles a NIP-05 record's enabled status.
     *
     * @param id the record ID
     * @return the HTTP response
     */
    public HttpResponse<String> toggleRecord(Long id) throws IOException, InterruptedException {
        HttpRequest request = addAuth(HttpRequest.newBuilder())
                .uri(URI.create(apiUrl + "/records/" + id + "/toggle"))
                .method("PATCH", HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        log.debug("toggle_record id={} status={}", id, response.statusCode());
        return response;
    }

    // --- Well-Known Endpoint (Public - No Auth Required) ---

    /**
     * Queries the .well-known/nostr.json endpoint for a specific name.
     *
     * @param name the username to look up
     * @return the HTTP response with NIP-05 JSON
     */
    public HttpResponse<String> getWellKnown(String name) throws IOException, InterruptedException {
        String url = wellKnownUrl;
        if (name != null && !name.isEmpty()) {
            url += "?name=" + name;
        }

        // Well-known endpoint is public, no auth needed
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        log.debug("get_well_known name={} status={}", name, response.statusCode());
        return response;
    }

    /**
     * Queries the .well-known/nostr.json endpoint for all records.
     *
     * @return the HTTP response with NIP-05 JSON
     */
    public HttpResponse<String> getWellKnown() throws IOException, InterruptedException {
        return getWellKnown(null);
    }

    // --- Health Check (Public - No Auth Required) ---

    /**
     * Checks if Bottin is healthy.
     *
     * @return the HTTP response from actuator health endpoint
     */
    public HttpResponse<String> healthCheck() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/actuator/health"))
                .GET()
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    // --- Utility Methods ---

    /**
     * Parses a JSON response body into a JsonNode.
     *
     * @param response the HTTP response
     * @return the parsed JSON
     */
    public JsonNode parseJson(HttpResponse<String> response) throws JsonProcessingException {
        return objectMapper.readTree(response.body());
    }

    /**
     * Extracts the ID from a response body containing an "id" field.
     *
     * @param response the HTTP response
     * @return the ID value
     */
    public Long extractId(HttpResponse<String> response) throws JsonProcessingException {
        return objectMapper.readTree(response.body()).get("id").asLong();
    }

    /**
     * Gets the base URL.
     *
     * @return the base URL
     */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
     * Gets the API URL.
     *
     * @return the API base URL
     */
    public String getApiUrl() {
        return apiUrl;
    }

    /**
     * Gets the well-known URL.
     *
     * @return the well-known endpoint URL
     */
    public String getWellKnownUrl() {
        return wellKnownUrl;
    }
}
