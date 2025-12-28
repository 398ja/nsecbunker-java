package xyz.tcheeric.nsecbunker.it;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Testcontainers
@Tag("integration")
class TestcontainersIntegrationSmokeTest {

    @Container
    private final GenericContainer<?> alpine = new GenericContainer<>("alpine:3.18")
            .withCommand("sleep", "30");

    /**
     * Simple smoke test to verify Testcontainers can start a container.
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "ENABLE_IT", matches = "true")
    void shouldStartContainer() {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker not available");
        alpine.start();
        assertThat(alpine.isRunning()).isTrue();
    }
}
