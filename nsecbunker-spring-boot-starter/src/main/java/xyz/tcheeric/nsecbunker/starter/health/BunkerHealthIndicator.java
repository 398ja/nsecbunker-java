package xyz.tcheeric.nsecbunker.starter.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import xyz.tcheeric.nsecbunker.client.signer.NsecBunkerSigner;
import xyz.tcheeric.nsecbunker.starter.NsecBunkerProperties;

import java.util.Objects;

/**
 * Health indicator for nsecBunker signer connectivity.
 *
 * <p>Pings the bunker and reports health status for Spring Boot Actuator.
 */
public class BunkerHealthIndicator implements HealthIndicator {

    private final NsecBunkerSigner signer;
    private final NsecBunkerProperties properties;

    /**
     * Creates a new health indicator.
     *
     * @param signer     the signer to check (may be null)
     * @param properties configuration properties
     */
    public BunkerHealthIndicator(NsecBunkerSigner signer, NsecBunkerProperties properties) {
        this.signer = signer;
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    /**
     * Checks bunker health by sending a ping request.
     *
     * @return health status with bunker details
     */
    @Override
    public Health health() {
        if (signer == null) {
            return Health.unknown().withDetail("reason", "signer bean missing").build();
        }
        try {
            String pong = signer.ping().get();
            return Health.up()
                    .withDetail("bunkerPubkey", properties.getSigner().getBunkerPubkey())
                    .withDetail("pong", pong)
                    .build();
        } catch (Exception e) {
            return Health.down(e).withDetail("bunkerPubkey", properties.getSigner().getBunkerPubkey()).build();
        }
    }
}
