package xyz.tcheeric.nsecbunker.starter;

import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import xyz.tcheeric.nsecbunker.admin.NsecBunkerAdminClient;
import xyz.tcheeric.nsecbunker.client.signer.NsecBunkerSigner;
import xyz.tcheeric.nsecbunker.client.signer.SignerConfig;
import xyz.tcheeric.nsecbunker.starter.health.BunkerHealthIndicator;
import xyz.tcheeric.nsecbunker.starter.info.BunkerInfoContributor;

import java.util.List;

/**
 * Auto-configuration for nsecBunker integration.
 *
 * <p>This configuration automatically creates:
 * <ul>
 *   <li>{@link NsecBunkerAdminClient} - if admin properties are configured</li>
 *   <li>{@link NsecBunkerSigner} - if signer properties are configured</li>
 *   <li>{@link BunkerHealthIndicator} - for Spring Boot Actuator health checks</li>
 *   <li>{@link BunkerInfoContributor} - for Spring Boot Actuator info endpoint</li>
 * </ul>
 *
 * @see NsecBunkerProperties
 */
@AutoConfiguration
@EnableConfigurationProperties(NsecBunkerProperties.class)
public class NsecBunkerAutoConfiguration {

    /**
     * Creates the admin client bean if admin properties are configured.
     *
     * @param properties the configuration properties
     * @return the admin client
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(NsecBunkerAdminClient.class)
    public NsecBunkerAdminClient nsecBunkerAdminClient(NsecBunkerProperties properties) {
        NsecBunkerProperties.Admin admin = properties.getAdmin();
        validateAdmin(admin);
        return NsecBunkerAdminClient.builder()
                .bunkerPubkey(admin.getBunkerPubkey())
                .adminPrivateKey(admin.getAdminPrivateKey())
                .relays(admin.getRelays())
                .secret(admin.getSecret())
                .connectTimeout(admin.getConnectTimeout())
                .requestTimeout(admin.getRequestTimeout())
                .useEphemeralKey(admin.isUseEphemeralKey())
                .build();
    }

    /**
     * Creates the signer bean if signer properties are configured.
     *
     * @param properties the configuration properties
     * @return the signer
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(NsecBunkerSigner.class)
    public NsecBunkerSigner nsecBunkerSigner(NsecBunkerProperties properties) {
        NsecBunkerProperties.Signer signer = properties.getSigner();
        validateSigner(signer);
        return new NsecBunkerSigner(SignerConfig.builder()
                .bunkerPubkey(signer.getBunkerPubkey())
                .clientPrivateKey(signer.getClientPrivateKey())
                .relays(signer.getRelays())
                .secret(signer.getSecret())
                .useEphemeralKey(signer.isUseEphemeralKey())
                .connectTimeout(signer.getConnectTimeout())
                .requestTimeout(signer.getRequestTimeout())
                .build());
    }

    /**
     * Creates the health indicator for Actuator.
     *
     * @param properties the configuration properties
     * @param signers    available signers (may be empty)
     * @return the health indicator
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(HealthIndicator.class)
    public HealthIndicator bunkerHealthIndicator(NsecBunkerProperties properties, List<NsecBunkerSigner> signers) {
        NsecBunkerSigner signer = signers.isEmpty() ? null : signers.get(0);
        return new BunkerHealthIndicator(signer, properties);
    }

    /**
     * Creates the info contributor for Actuator.
     *
     * @param properties the configuration properties
     * @return the info contributor
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(InfoContributor.class)
    public InfoContributor bunkerInfoContributor(NsecBunkerProperties properties) {
        return new BunkerInfoContributor(properties);
    }

    private void validateAdmin(NsecBunkerProperties.Admin admin) {
        if (admin.getBunkerPubkey() == null || admin.getBunkerPubkey().isBlank()) {
            throw new IllegalStateException("nsecbunker.admin.bunker-pubkey is required");
        }
        if (admin.getAdminPrivateKey() == null || admin.getAdminPrivateKey().isBlank()) {
            throw new IllegalStateException("nsecbunker.admin.admin-private-key is required");
        }
        if (admin.getRelays() == null || admin.getRelays().isEmpty()) {
            throw new IllegalStateException("nsecbunker.admin.relays is required");
        }
    }

    private void validateSigner(NsecBunkerProperties.Signer signer) {
        if (signer.getBunkerPubkey() == null || signer.getBunkerPubkey().isBlank()) {
            throw new IllegalStateException("nsecbunker.signer.bunker-pubkey is required");
        }
        if (signer.getClientPrivateKey() == null || signer.getClientPrivateKey().isBlank()) {
            throw new IllegalStateException("nsecbunker.signer.client-private-key is required");
        }
        if (signer.getRelays() == null || signer.getRelays().isEmpty()) {
            throw new IllegalStateException("nsecbunker.signer.relays is required");
        }
    }
}
