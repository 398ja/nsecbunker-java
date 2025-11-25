package xyz.tcheeric.nsecbunker.starter;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
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
}
