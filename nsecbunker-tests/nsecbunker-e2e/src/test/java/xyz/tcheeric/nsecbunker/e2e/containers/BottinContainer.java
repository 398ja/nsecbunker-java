package xyz.tcheeric.nsecbunker.e2e.containers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

/**
 * Testcontainer for the Bottin NIP-05 registry service.
 *
 * <p>Bottin provides persistent NIP-05 identity management with a REST API
 * and .well-known/nostr.json endpoint per the NIP-05 specification.
 *
 * <p>This container requires a PostgreSQL database to be available and
 * configured via the withPostgres* methods.
 *
 * <p>For testing, Bottin should be started with test mode enabled
 * (BOTTIN_VERIFICATION_SKIP=true) to auto-verify domains without
 * requiring actual DNS or HTTP verification.
 */
public class BottinContainer extends GenericContainer<BottinContainer> {

    private static final Logger log = LoggerFactory.getLogger(BottinContainer.class);

    private static final DockerImageName DEFAULT_IMAGE =
            DockerImageName.parse("docker.398ja.xyz/bottin-web:latest");
    private static final int HTTP_PORT = 8080;

    private static final String DEFAULT_ADMIN_USER = "admin";
    private static final String DEFAULT_ADMIN_PASSWORD = "admin";

    private String postgresUrl;
    private String postgresUser = "bottin";
    private String postgresPassword = "bottin";
    private String adminUser = DEFAULT_ADMIN_USER;
    private String adminPassword = DEFAULT_ADMIN_PASSWORD;
    private boolean verificationSkip = Boolean.parseBoolean(
            System.getProperty("bottin.verification.skip", "true"));

    public BottinContainer() {
        this(DEFAULT_IMAGE);
    }

    public BottinContainer(DockerImageName imageName) {
        super(imageName);
        withExposedPorts(HTTP_PORT);
        withLogConsumer(new Slf4jLogConsumer(log).withPrefix("bottin"));
        waitingFor(Wait.forHttp("/actuator/health")
                .forPort(HTTP_PORT)
                .forStatusCode(200)
                .withStartupTimeout(Duration.ofSeconds(120)));
    }

    /**
     * Configures the PostgreSQL JDBC URL.
     *
     * <p>For container-to-container communication, use the internal network alias:
     * {@code jdbc:postgresql://postgres:5432/bottin}
     *
     * @param url the JDBC URL
     * @return this container for method chaining
     */
    public BottinContainer withPostgresUrl(String url) {
        this.postgresUrl = url;
        return this;
    }

    /**
     * Configures the PostgreSQL username.
     *
     * @param user the database username
     * @return this container for method chaining
     */
    public BottinContainer withPostgresUser(String user) {
        this.postgresUser = user;
        return this;
    }

    /**
     * Configures the PostgreSQL password.
     *
     * @param password the database password
     * @return this container for method chaining
     */
    public BottinContainer withPostgresPassword(String password) {
        this.postgresPassword = password;
        return this;
    }

    /**
     * Enables or disables domain verification skip mode.
     *
     * <p>When enabled (default for tests), domains are auto-verified
     * without requiring DNS TXT or HTTP well-known file verification.
     *
     * @param skip true to skip verification, false to require it
     * @return this container for method chaining
     */
    public BottinContainer withVerificationSkip(boolean skip) {
        this.verificationSkip = skip;
        return this;
    }

    /**
     * Configures the admin username for API authentication.
     *
     * @param user the admin username
     * @return this container for method chaining
     */
    public BottinContainer withAdminUser(String user) {
        this.adminUser = user;
        return this;
    }

    /**
     * Configures the admin password for API authentication.
     *
     * @param password the admin password
     * @return this container for method chaining
     */
    public BottinContainer withAdminPassword(String password) {
        this.adminPassword = password;
        return this;
    }

    @Override
    protected void configure() {
        // Spring Boot datasource configuration
        withEnv("SPRING_DATASOURCE_URL", postgresUrl);
        withEnv("SPRING_DATASOURCE_USERNAME", postgresUser);
        withEnv("SPRING_DATASOURCE_PASSWORD", postgresPassword);
        withEnv("SPRING_DATASOURCE_DRIVER_CLASS_NAME", "org.postgresql.Driver");

        // JPA configuration for test environment
        withEnv("SPRING_JPA_HIBERNATE_DDL_AUTO", "create-drop");
        withEnv("SPRING_JPA_SHOW_SQL", "false");
        withEnv("SPRING_JPA_DATABASE_PLATFORM", "org.hibernate.dialect.PostgreSQLDialect");

        // Disable Flyway for tests (use Hibernate DDL auto)
        withEnv("SPRING_FLYWAY_ENABLED", "false");

        // Admin credentials for API authentication
        withEnv("BOTTIN_ADMIN_USERNAME", adminUser);
        withEnv("BOTTIN_ADMIN_PASSWORD", adminPassword);

        // Bottin test mode - skip domain verification
        withEnv("BOTTIN_VERIFICATION_SKIP", String.valueOf(verificationSkip));

        // Logging configuration
        withEnv("LOGGING_LEVEL_XYZ_TCHEERIC", "DEBUG");

        log.info("bottin_container_configured postgres_url={} verification_skip={} admin_user={}",
                postgresUrl, verificationSkip, adminUser);
    }

    /**
     * Gets the base HTTP URL for the Bottin API.
     *
     * @return the base URL (e.g., "http://localhost:32768")
     */
    public String getBaseUrl() {
        return String.format("http://%s:%d", getHost(), getMappedPort(HTTP_PORT));
    }

    /**
     * Gets the URL for the .well-known/nostr.json endpoint.
     *
     * @return the well-known URL
     */
    public String getWellKnownUrl() {
        return getBaseUrl() + "/.well-known/nostr.json";
    }

    /**
     * Gets the base URL for the REST API.
     *
     * @return the API base URL (e.g., "http://localhost:32768/api/v1")
     */
    public String getApiUrl() {
        return getBaseUrl() + "/api/v1";
    }

    /**
     * Gets the health check endpoint URL.
     *
     * @return the actuator health URL
     */
    public String getHealthUrl() {
        return getBaseUrl() + "/actuator/health";
    }

    /**
     * Gets the admin username for API authentication.
     *
     * @return the admin username
     */
    public String getAdminUser() {
        return adminUser;
    }

    /**
     * Gets the admin password for API authentication.
     *
     * @return the admin password
     */
    public String getAdminPassword() {
        return adminPassword;
    }
}
