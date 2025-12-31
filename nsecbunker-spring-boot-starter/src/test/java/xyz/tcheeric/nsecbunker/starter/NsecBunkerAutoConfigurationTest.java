package xyz.tcheeric.nsecbunker.starter;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import xyz.tcheeric.nsecbunker.account.nip05.Nip05Manager;
import xyz.tcheeric.nsecbunker.account.nip05.spi.Nip05ManagerProvider;
import xyz.tcheeric.nsecbunker.account.registration.AccountManager;
import xyz.tcheeric.nsecbunker.account.registration.spi.AccountManagerProvider;
import xyz.tcheeric.nsecbunker.admin.NsecBunkerAdminClient;
import xyz.tcheeric.nsecbunker.client.signer.NsecBunkerSigner;

import static org.assertj.core.api.Assertions.assertThat;

class NsecBunkerAutoConfigurationTest {

    // Valid test keys (secp256k1 scalar = 1, 2)
    private static final String TEST_BUNKER_PUBKEY = "79be667ef9dcbbac55a06295ce870b07029bfcdb2dce28d959f2815b16f81798";
    private static final String TEST_ADMIN_PRIVKEY = "0000000000000000000000000000000000000000000000000000000000000002";
    private static final String TEST_CLIENT_PRIVKEY = "0000000000000000000000000000000000000000000000000000000000000003";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(NsecBunkerAutoConfiguration.class))
            .withPropertyValues(
                    "nsecbunker.admin.bunker-pubkey=" + TEST_BUNKER_PUBKEY,
                    "nsecbunker.admin.admin-private-key=" + TEST_ADMIN_PRIVKEY,
                    "nsecbunker.admin.relays[0]=wss://relay.example.com",
                    "nsecbunker.signer.bunker-pubkey=" + TEST_BUNKER_PUBKEY,
                    "nsecbunker.signer.client-private-key=" + TEST_CLIENT_PRIVKEY,
                    "nsecbunker.signer.relays[0]=wss://relay.example.com"
            );

    /**
     * Ensures admin and signer beans are created from properties.
     */
    @Test
    void shouldCreateAdminAndSignerBeans() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(NsecBunkerAdminClient.class);
            assertThat(context).hasSingleBean(NsecBunkerSigner.class);
        });
    }

    /**
     * Ensures health indicator is present when actuator is on classpath.
     */
    @Test
    void shouldExposeHealthIndicator() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(HealthIndicator.class);
        });
    }

    // =========================================================================
    // NIP-05 Auto-Configuration Tests
    // =========================================================================

    /**
     * Ensures AccountManager bean is created by default when NIP-05 is enabled (default).
     */
    @Test
    void shouldCreateAccountManagerBeanByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(AccountManager.class);
            assertThat(context).hasSingleBean(AccountManagerProvider.class);
        });
    }

    /**
     * Ensures Nip05Manager bean is created by default when NIP-05 is enabled (default).
     */
    @Test
    void shouldCreateNip05ManagerBeanByDefault() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(Nip05Manager.class);
            assertThat(context).hasSingleBean(Nip05ManagerProvider.class);
        });
    }

    /**
     * Ensures NIP-05 beans are not created when explicitly disabled.
     */
    @Test
    void shouldNotCreateNip05BeansWhenDisabled() {
        contextRunner
                .withPropertyValues("nsecbunker.nip05.enabled=false")
                .run(context -> {
                    assertThat(context).doesNotHaveBean(AccountManager.class);
                    assertThat(context).doesNotHaveBean(Nip05Manager.class);
                    assertThat(context).doesNotHaveBean(AccountManagerProvider.class);
                    assertThat(context).doesNotHaveBean(Nip05ManagerProvider.class);
                });
    }

    /**
     * Ensures default in-memory provider is used when provider is set to "auto".
     */
    @Test
    void shouldUseDefaultProviderWhenAutoSelected() {
        contextRunner
                .withPropertyValues("nsecbunker.nip05.provider=auto")
                .run(context -> {
                    assertThat(context).hasSingleBean(AccountManager.class);
                    assertThat(context).hasSingleBean(Nip05Manager.class);

                    // Verify the default provider is registered
                    AccountManagerProvider accountProvider = context.getBean(AccountManagerProvider.class);
                    assertThat(accountProvider.name()).isEqualTo("in-memory");
                    assertThat(accountProvider.priority()).isEqualTo(0);
                });
    }

    /**
     * Ensures fallback to default when requested provider is not available.
     */
    @Test
    void shouldFallbackToDefaultWhenRequestedProviderNotFound() {
        contextRunner
                .withPropertyValues("nsecbunker.nip05.provider=nonexistent-provider")
                .run(context -> {
                    // Should still create beans using fallback
                    assertThat(context).hasSingleBean(AccountManager.class);
                    assertThat(context).hasSingleBean(Nip05Manager.class);
                });
    }

    /**
     * Ensures in-memory provider is selected when explicitly requested.
     */
    @Test
    void shouldSelectInMemoryProviderWhenExplicitlyRequested() {
        contextRunner
                .withPropertyValues("nsecbunker.nip05.provider=in-memory")
                .run(context -> {
                    assertThat(context).hasSingleBean(AccountManager.class);
                    assertThat(context).hasSingleBean(Nip05Manager.class);

                    AccountManagerProvider provider = context.getBean(AccountManagerProvider.class);
                    assertThat(provider.name()).isEqualTo("in-memory");
                });
    }

    /**
     * Ensures NIP-05 beans are created with minimal configuration.
     *
     * <p>Note: Admin/signer beans require their respective properties to be set,
     * so this test provides minimal valid configuration to verify NIP-05 beans
     * are created and functional independently of admin/signer usage.
     */
    @Test
    void shouldCreateNip05BeansWithMinimalConfiguration() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(NsecBunkerAutoConfiguration.class))
                .withPropertyValues(
                        // Minimal admin/signer config required by auto-configuration
                        "nsecbunker.admin.bunker-pubkey=" + TEST_BUNKER_PUBKEY,
                        "nsecbunker.admin.admin-private-key=" + TEST_ADMIN_PRIVKEY,
                        "nsecbunker.admin.relays[0]=wss://relay.example.com",
                        "nsecbunker.signer.bunker-pubkey=" + TEST_BUNKER_PUBKEY,
                        "nsecbunker.signer.client-private-key=" + TEST_CLIENT_PRIVKEY,
                        "nsecbunker.signer.relays[0]=wss://relay.example.com"
                )
                .run(context -> {
                    // NIP-05 beans should be present
                    assertThat(context).hasSingleBean(AccountManager.class);
                    assertThat(context).hasSingleBean(Nip05Manager.class);

                    // Verify they are functional (can be used without external dependencies)
                    AccountManager accountManager = context.getBean(AccountManager.class);
                    assertThat(accountManager).isNotNull();

                    Nip05Manager nip05Manager = context.getBean(Nip05Manager.class);
                    assertThat(nip05Manager).isNotNull();
                });
    }

    /**
     * Ensures provider availability check is respected.
     */
    @Test
    void shouldCheckProviderAvailability() {
        contextRunner.run(context -> {
            AccountManagerProvider provider = context.getBean(AccountManagerProvider.class);
            // Default in-memory provider should always be available
            assertThat(provider.isAvailable()).isTrue();

            Nip05ManagerProvider nip05Provider = context.getBean(Nip05ManagerProvider.class);
            assertThat(nip05Provider.isAvailable()).isTrue();
        });
    }
}
