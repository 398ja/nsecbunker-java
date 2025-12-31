package xyz.tcheeric.nsecbunker.it;

import nostr.event.BaseTag;
import nostr.event.impl.GenericEvent;
import nostr.event.tag.PubKeyTag;
import nostr.id.Identity;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import xyz.tcheeric.nsecbunker.connection.RelayConnection;
import xyz.tcheeric.nsecbunker.connection.RelayListener;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Testcontainers
@Tag("integration")
class RelayContainerIntegrationTest {

    @Container
    private final GenericContainer<?> relay = new GenericContainer<>("scsibug/nostr-rs-relay:latest")
            .withExposedPorts(7000)
            .waitingFor(Wait.forListeningPort());

    /**
     * Starts a Nostr relay Testcontainer to support integration/E2E tests.
     * Enable with ENABLE_IT=true and ensure Docker is available.
     */
    @Test
    @EnabledIfEnvironmentVariable(named = "ENABLE_IT", matches = "true")
    void shouldPublishAndSubscribeThroughRelay() throws Exception {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker not available");
        relay.start();

        int mappedPort = relay.getMappedPort(7000);
        String relayUrl = "ws://" + relay.getHost() + ":" + mappedPort;
        RelayConnection connection = new RelayConnection(relayUrl, null, Duration.ofSeconds(15));
        CountDownLatch connected = new CountDownLatch(1);
        CountDownLatch eoseReceived = new CountDownLatch(1);
        CountDownLatch okReceived = new CountDownLatch(1);
        AtomicReference<String> okEventId = new AtomicReference<>();

        connection.addListener(new RelayListener() {
            @Override
            public void onConnect(RelayConnection relayConnection) {
                connected.countDown();
            }

            @Override
            public void onEndOfStoredEvents(RelayConnection relayConnection, String subscriptionId) {
                eoseReceived.countDown();
            }

            @Override
            public void onOk(RelayConnection relayConnection, String eventId, boolean success, String message) {
                if (success) {
                    okEventId.set(eventId);
                    okReceived.countDown();
                }
            }
        });

        connection.connect();
        assertThat(connected.await(10, SECONDS)).isTrue();

        // Subscribe with an empty filter and expect an EOSE response
        connection.sendReq("it-sub", "{\"kinds\":[1],\"limit\":0}");
        assertThat(eoseReceived.await(10, SECONDS)).isTrue();

        // Publish a simple text note and expect an OK
        Identity identity = Identity.create("0000000000000000000000000000000000000000000000000000000000000001");
        GenericEvent event = new GenericEvent(identity.getPublicKey(), 1);
        event.setCreatedAt(Instant.now().getEpochSecond());
        event.setContent("hello from nsecbunker-it");
        event.update();
        identity.sign(event);

        connection.sendEvent(serializeEvent(event));

        assertThat(okReceived.await(10, SECONDS)).isTrue();
        assertThat(okEventId.get()).isEqualTo(event.getId());

        connection.close();
        assertThat(connection.isConnected()).isFalse();
    }

    private String serializeEvent(GenericEvent event) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"id\":\"").append(event.getId()).append("\",");
        sb.append("\"pubkey\":\"").append(event.getPubKey()).append("\",");
        sb.append("\"created_at\":").append(event.getCreatedAt()).append(",");
        sb.append("\"kind\":").append(event.getKind()).append(",");
        sb.append("\"tags\":[");
        List<BaseTag> tags = event.getTags();
        for (int i = 0; i < tags.size(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            BaseTag tag = tags.get(i);
            sb.append("[\"").append(tag.getCode()).append("\"");
            if (tag instanceof PubKeyTag pubKeyTag) {
                sb.append(",\"").append(pubKeyTag.getPublicKey().toString()).append("\"");
            }
            sb.append("]");
        }
        sb.append("],");
        sb.append("\"content\":\"").append(escapeJson(event.getContent())).append("\",");
        sb.append("\"sig\":\"").append(event.getSignature()).append("\"");
        sb.append("}");
        return sb.toString();
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
