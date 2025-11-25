package xyz.tcheeric.nsecbunker.starter;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import xyz.tcheeric.nsecbunker.admin.NsecBunkerAdminClient;
import xyz.tcheeric.nsecbunker.client.signer.NsecBunkerSigner;

import static org.assertj.core.api.Assertions.assertThat;

class NsecBunkerAutoConfigurationTest {

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
