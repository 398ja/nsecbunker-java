package xyz.tcheeric.nsecbunker.examples.multitenant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.tcheeric.nsecbunker.examples.multitenant.controller.TenantController;
import xyz.tcheeric.nsecbunker.examples.multitenant.model.Tenant;
import xyz.tcheeric.nsecbunker.examples.multitenant.repository.TenantRepository;
import xyz.tcheeric.nsecbunker.examples.multitenant.service.TenantService;
import xyz.tcheeric.nsecbunker.examples.multitenant.service.TenantSignerRegistry;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Multi-tenant demo application showing how to manage multiple
 * nsecBunker signers for different tenants.
 *
 * <p>This example demonstrates:
 * <ul>
 *   <li>Per-tenant signer management</li>
 *   <li>Tenant isolation and lifecycle</li>
 *   <li>Connection pooling and lazy initialization</li>
 *   <li>Automatic idle signer eviction</li>
 *   <li>Service statistics and monitoring</li>
 * </ul>
 *
 * <h2>Architecture Overview</h2>
 * <pre>
 * ┌─────────────────────────────────────────────────────────────┐
 * │                    TenantController                         │
 * │  (REST API endpoints for tenant and signing operations)    │
 * └──────────────────────────┬──────────────────────────────────┘
 *                            │
 * ┌──────────────────────────▼──────────────────────────────────┐
 * │                     TenantService                           │
 * │  (Business logic, tenant CRUD, signing orchestration)      │
 * └─────────────┬───────────────────────────────┬──────────────┘
 *               │                               │
 * ┌─────────────▼─────────────┐   ┌─────────────▼─────────────┐
 * │    TenantRepository       │   │  TenantSignerRegistry     │
 * │  (Tenant storage)         │   │  (Signer management)      │
 * └───────────────────────────┘   └─────────────┬─────────────┘
 *                                               │
 *                               ┌───────────────▼───────────────┐
 *                               │      NsecBunkerSigner         │
 *                               │   (Per-tenant instances)      │
 *                               └───────────────────────────────┘
 * </pre>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Create the multi-tenant infrastructure
 * TenantRepository repository = new TenantRepository();
 * TenantSignerRegistry signerRegistry = new TenantSignerRegistry();
 * TenantService service = new TenantService(repository, signerRegistry);
 * TenantController controller = new TenantController(service);
 *
 * // Register a tenant
 * Tenant tenant = controller.createTenant(new TenantController.CreateTenantRequest(
 *     "tenant-1",
 *     "Acme Corp",
 *     "bunker-pubkey-hex",
 *     List.of("wss://relay.example.com"),
 *     "optional-secret",
 *     "client-privkey-hex",
 *     null, // admin key (optional)
 *     true  // use ephemeral keys
 * ));
 *
 * // Sign events for the tenant
 * String signedEvent = controller.signEvent("tenant-1", eventJson).join().signedEvent();
 * }</pre>
 *
 * <h2>Production Considerations</h2>
 * <ul>
 *   <li>Replace TenantRepository with persistent storage (database)</li>
 *   <li>Encrypt tenant credentials at rest</li>
 *   <li>Add authentication/authorization to controller</li>
 *   <li>Configure connection pooling limits</li>
 *   <li>Set up monitoring and alerting</li>
 *   <li>Implement rate limiting per tenant</li>
 * </ul>
 */
public class MultiTenantDemoApplication {

    private static final Logger log = LoggerFactory.getLogger(MultiTenantDemoApplication.class);

    private final TenantRepository repository;
    private final TenantSignerRegistry signerRegistry;
    private final TenantService tenantService;
    private final TenantController controller;
    private final ScheduledExecutorService scheduler;

    public MultiTenantDemoApplication() {
        this(Duration.ofSeconds(30), Duration.ofSeconds(60));
    }

    public MultiTenantDemoApplication(Duration connectTimeout, Duration requestTimeout) {
        this.repository = new TenantRepository();
        this.signerRegistry = new TenantSignerRegistry(connectTimeout, requestTimeout);
        this.tenantService = new TenantService(repository, signerRegistry);
        this.controller = new TenantController(tenantService);
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "tenant-maintenance");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Starts the application and schedules maintenance tasks.
     */
    public void start() {
        log.info("Starting multi-tenant demo application");

        // Schedule idle signer eviction every 5 minutes
        scheduler.scheduleAtFixedRate(
                () -> evictIdleSigners(Duration.ofMinutes(30)),
                5, 5, TimeUnit.MINUTES
        );

        // Schedule stats logging every minute
        scheduler.scheduleAtFixedRate(
                this::logStats,
                1, 1, TimeUnit.MINUTES
        );
    }

    /**
     * Stops the application and cleans up resources.
     */
    public void stop() {
        log.info("Stopping multi-tenant demo application");

        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        tenantService.shutdown();
    }

    /**
     * Gets the controller for handling tenant operations.
     */
    public TenantController getController() {
        return controller;
    }

    /**
     * Gets the service for direct access.
     */
    public TenantService getService() {
        return tenantService;
    }

    private void evictIdleSigners(Duration idleThreshold) {
        try {
            int evicted = signerRegistry.evictIdle(idleThreshold);
            if (evicted > 0) {
                log.info("Evicted {} idle signers", evicted);
            }
        } catch (Exception e) {
            log.warn("Error evicting idle signers", e);
        }
    }

    private void logStats() {
        try {
            TenantService.ServiceStats stats = tenantService.getStats();
            log.info("Stats - Tenants: {}, Active: {}, Signers: {}",
                    stats.totalTenants(), stats.activeTenants(), stats.activeSigners());
        } catch (Exception e) {
            log.warn("Error logging stats", e);
        }
    }

    /**
     * Demo main method showing basic usage.
     */
    public static void main(String[] args) {
        MultiTenantDemoApplication app = new MultiTenantDemoApplication();

        // Register shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(app::stop));

        try {
            app.start();
            runDemo(app.getController());
        } catch (Exception e) {
            log.error("Demo failed", e);
        } finally {
            app.stop();
        }
    }

    private static void runDemo(TenantController controller) {
        log.info("=".repeat(60));
        log.info("Multi-Tenant nsecBunker Demo");
        log.info("=".repeat(60));

        // This is a demonstration - in production, use real bunker credentials
        // The actual signing will fail without a real bunker, but this shows
        // the API usage pattern

        // Create sample tenants
        log.info("\n--- Creating Tenants ---");

        try {
            // Note: These are example hex strings - use real keys in production
            Tenant tenant1 = controller.createTenant(new TenantController.CreateTenantRequest(
                    "tenant-acme",
                    "Acme Corporation",
                    "79be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798",
                    List.of("wss://relay.damus.io", "wss://nos.lol"),
                    "optional-secret-123",
                    "0000000000000000000000000000000000000000000000000000000000000001",
                    null,
                    true
            ));
            log.info("Created tenant: {} ({})", tenant1.name(), tenant1.id());

            Tenant tenant2 = controller.createTenant(new TenantController.CreateTenantRequest(
                    "tenant-beta",
                    "Beta Industries",
                    "c6047f9441ed7d6d3045406e95c07cd85c778e4b8cef3ca7abac09b95c709ee5",
                    List.of("wss://relay.nostr.band"),
                    null,
                    "0000000000000000000000000000000000000000000000000000000000000002",
                    null,
                    true
            ));
            log.info("Created tenant: {} ({})", tenant2.name(), tenant2.id());

        } catch (Exception e) {
            log.info("Tenant creation example: {}", e.getMessage());
        }

        // List tenants
        log.info("\n--- Listing Tenants ---");
        controller.listTenants().forEach(tenant ->
                log.info("  - {} ({}) - Status: {}",
                        tenant.name(), tenant.id(), tenant.status())
        );

        // Get stats
        log.info("\n--- Service Stats ---");
        TenantService.ServiceStats stats = controller.getStats();
        log.info("Total tenants: {}", stats.totalTenants());
        log.info("Active tenants: {}", stats.activeTenants());
        log.info("Active signers: {}", stats.activeSigners());

        // Demonstrate tenant operations
        log.info("\n--- Tenant Operations ---");
        try {
            // Suspend a tenant
            Tenant suspended = controller.suspendTenant("tenant-beta");
            log.info("Suspended tenant: {} - Status: {}", suspended.name(), suspended.status());

            // Get tenant status
            TenantController.TenantStatus status = controller.getTenantStatus("tenant-acme");
            log.info("Tenant {} status: {}, connected: {}",
                    status.tenantId(), status.status(), status.connected());

            // Activate the tenant back
            Tenant activated = controller.activateTenant("tenant-beta");
            log.info("Activated tenant: {} - Status: {}", activated.name(), activated.status());

        } catch (Exception e) {
            log.info("Tenant operations example: {}", e.getMessage());
        }

        // Signing example (would fail without real bunker)
        log.info("\n--- Signing Example (requires real bunker) ---");
        String sampleEvent = """
                {
                  "kind": 1,
                  "content": "Hello from multi-tenant app!",
                  "tags": [],
                  "created_at": %d
                }
                """.formatted(System.currentTimeMillis() / 1000);

        log.info("Sample event to sign:");
        log.info(sampleEvent);
        log.info("Note: Actual signing requires a running nsecBunker instance");

        // Clean up demo tenants
        log.info("\n--- Cleanup ---");
        try {
            controller.deleteTenant("tenant-acme");
            controller.deleteTenant("tenant-beta");
            log.info("Deleted demo tenants");
        } catch (Exception e) {
            log.info("Cleanup: {}", e.getMessage());
        }

        log.info("\n" + "=".repeat(60));
        log.info("Demo completed");
        log.info("=".repeat(60));
    }
}
