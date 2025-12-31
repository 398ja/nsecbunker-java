package xyz.tcheeric.nsecbunker.starter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import xyz.tcheeric.nsecbunker.account.nip05.DefaultNip05Manager;
import xyz.tcheeric.nsecbunker.account.nip05.Nip05Manager;
import xyz.tcheeric.nsecbunker.account.nip05.spi.DefaultNip05ManagerProvider;
import xyz.tcheeric.nsecbunker.account.nip05.spi.Nip05ManagerProvider;
import xyz.tcheeric.nsecbunker.account.registration.AccountManager;
import xyz.tcheeric.nsecbunker.account.registration.DefaultAccountManager;
import xyz.tcheeric.nsecbunker.account.registration.spi.AccountManagerProvider;
import xyz.tcheeric.nsecbunker.account.registration.spi.DefaultAccountManagerProvider;
import xyz.tcheeric.nsecbunker.admin.NsecBunkerAdminClient;
import xyz.tcheeric.nsecbunker.client.signer.NsecBunkerSigner;
import xyz.tcheeric.nsecbunker.client.signer.SignerConfig;
import xyz.tcheeric.nsecbunker.starter.health.BunkerHealthIndicator;
import xyz.tcheeric.nsecbunker.starter.info.BunkerInfoContributor;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Auto-configuration for nsecBunker integration.
 *
 * <p>This configuration automatically creates:
 * <ul>
 *   <li>{@link NsecBunkerAdminClient} - if admin properties are configured</li>
 *   <li>{@link NsecBunkerSigner} - if signer properties are configured</li>
 *   <li>{@link BunkerHealthIndicator} - for Spring Boot Actuator health checks</li>
 *   <li>{@link BunkerInfoContributor} - for Spring Boot Actuator info endpoint</li>
 *   <li>{@link Nip05Manager} - for NIP-05 identity management</li>
 *   <li>{@link AccountManager} - for account registration</li>
 * </ul>
 *
 * @see NsecBunkerProperties
 */
@Slf4j
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

    // ========== NIP-05 and Account Management ==========

    /**
     * Creates the AccountManager bean using the highest priority available provider.
     *
     * <p>If multiple {@link AccountManagerProvider} beans are registered, the one
     * with the highest priority will be used. If none are registered, a default
     * in-memory implementation is created.</p>
     *
     * @param providers list of available providers (may be empty)
     * @param properties the configuration properties
     * @return the account manager
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "nsecbunker.nip05.enabled", havingValue = "true", matchIfMissing = true)
    public AccountManager accountManager(List<AccountManagerProvider> providers,
                                          NsecBunkerProperties properties) {
        String requestedProvider = properties.getNip05().getProvider();

        // If a specific provider is requested (not "auto"), try to find it
        if (!"auto".equalsIgnoreCase(requestedProvider)) {
            Optional<AccountManagerProvider> specific = providers.stream()
                    .filter(p -> requestedProvider.equalsIgnoreCase(p.name()))
                    .filter(AccountManagerProvider::isAvailable)
                    .findFirst();

            if (specific.isPresent()) {
                log.info("account_manager_created provider={}", specific.get().name());
                return specific.get().create();
            }

            log.warn("account_manager_provider_not_found requested={} using=default",
                    requestedProvider);
        }

        // Auto-select highest priority available provider
        Optional<AccountManagerProvider> best = providers.stream()
                .filter(AccountManagerProvider::isAvailable)
                .max(Comparator.comparingInt(AccountManagerProvider::priority));

        if (best.isPresent()) {
            log.info("account_manager_created provider={} priority={}",
                    best.get().name(), best.get().priority());
            return best.get().create();
        }

        // Fallback to default
        log.info("account_manager_created provider=in-memory (fallback)");
        return new DefaultAccountManager();
    }

    /**
     * Creates the Nip05Manager bean using the highest priority available provider.
     *
     * <p>If multiple {@link Nip05ManagerProvider} beans are registered, the one
     * with the highest priority will be used. If none are registered, a default
     * in-memory implementation is created.</p>
     *
     * @param providers list of available providers (may be empty)
     * @param accountManager the account manager for NIP-05 operations
     * @param properties the configuration properties
     * @return the NIP-05 manager
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "nsecbunker.nip05.enabled", havingValue = "true", matchIfMissing = true)
    public Nip05Manager nip05Manager(List<Nip05ManagerProvider> providers,
                                      AccountManager accountManager,
                                      NsecBunkerProperties properties) {
        String requestedProvider = properties.getNip05().getProvider();

        // If a specific provider is requested (not "auto"), try to find it
        if (!"auto".equalsIgnoreCase(requestedProvider)) {
            Optional<Nip05ManagerProvider> specific = providers.stream()
                    .filter(p -> requestedProvider.equalsIgnoreCase(p.name()))
                    .filter(Nip05ManagerProvider::isAvailable)
                    .findFirst();

            if (specific.isPresent()) {
                log.info("nip05_manager_created provider={}", specific.get().name());
                return specific.get().create();
            }

            log.warn("nip05_manager_provider_not_found requested={} using=default",
                    requestedProvider);
        }

        // Auto-select highest priority available provider
        Optional<Nip05ManagerProvider> best = providers.stream()
                .filter(Nip05ManagerProvider::isAvailable)
                .max(Comparator.comparingInt(Nip05ManagerProvider::priority));

        if (best.isPresent()) {
            log.info("nip05_manager_created provider={} priority={}",
                    best.get().name(), best.get().priority());
            return best.get().create();
        }

        // Fallback to default using the provided AccountManager
        log.info("nip05_manager_created provider=in-memory (fallback)");
        return new DefaultNip05Manager(accountManager);
    }

    /**
     * Registers the default in-memory AccountManager provider.
     *
     * @return the default provider with priority 0
     */
    @Bean
    @ConditionalOnMissingBean(name = "defaultAccountManagerProvider")
    @ConditionalOnProperty(name = "nsecbunker.nip05.enabled", havingValue = "true", matchIfMissing = true)
    public AccountManagerProvider defaultAccountManagerProvider() {
        return new DefaultAccountManagerProvider();
    }

    /**
     * Registers the default in-memory Nip05Manager provider.
     *
     * @param accountManager the account manager dependency
     * @return the default provider with priority 0
     */
    @Bean
    @ConditionalOnMissingBean(name = "defaultNip05ManagerProvider")
    @ConditionalOnProperty(name = "nsecbunker.nip05.enabled", havingValue = "true", matchIfMissing = true)
    public Nip05ManagerProvider defaultNip05ManagerProvider(AccountManager accountManager) {
        return new DefaultNip05ManagerProvider(accountManager);
    }
}
