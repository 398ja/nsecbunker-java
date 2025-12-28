package xyz.tcheeric.nsecbunker.it;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import xyz.tcheeric.nsecbunker.admin.NsecBunkerAdminClient;
import xyz.tcheeric.nsecbunker.client.signer.NsecBunkerSigner;
import xyz.tcheeric.nsecbunker.starter.NsecBunkerAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class SpringBootStarterIntegrationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(NsecBunkerAutoConfiguration.class))
            .withPropertyValues(
                    "nsecbunker.admin.bunker-pubkey=test",
                    "nsecbunker.admin.admin-private-key=test",
                    "nsecbunker.admin.relays[0]=wss://relay.example.com",
                    "nsecbunker.signer.bunker-pubkey=test",
                    "nsecbunker.signer.client-private-key=test",
                    "nsecbunker.signer.relays[0]=wss://relay.example.com"
            );

    /**
     * Ensures beans are created from properties in an app context.
     */
    @Test
    void shouldCreateBeans() {
        contextRunner.run(ctx -> {
            assertThat(ctx).hasSingleBean(NsecBunkerAdminClient.class);
            assertThat(ctx).hasSingleBean(NsecBunkerSigner.class);
            assertThat(ctx).hasSingleBean(HealthIndicator.class);
        });
    }
}
