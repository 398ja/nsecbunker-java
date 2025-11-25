package xyz.tcheeric.nsecbunker.starter.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import xyz.tcheeric.nsecbunker.client.signer.NsecBunkerSigner;
import xyz.tcheeric.nsecbunker.starter.NsecBunkerProperties;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BunkerHealthIndicatorTest {

    @Mock
    private NsecBunkerSigner signer;

    private NsecBunkerProperties properties;

    @BeforeEach
    void setUp() {
        properties = new NsecBunkerProperties();
        properties.getSigner().setBunkerPubkey("npub1test");
    }

    @Test
    void shouldThrowWhenPropertiesNull() {
        assertThatNullPointerException()
                .isThrownBy(() -> new BunkerHealthIndicator(signer, null))
                .withMessage("properties must not be null");
    }

    @Test
    void shouldReturnUnknownWhenSignerNull() {
        BunkerHealthIndicator indicator = new BunkerHealthIndicator(null, properties);

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UNKNOWN);
        assertThat(health.getDetails()).containsEntry("reason", "signer bean missing");
    }

    @Test
    void shouldReturnUpWhenPingSucceeds() {
        when(signer.ping()).thenReturn(CompletableFuture.completedFuture("pong"));
        BunkerHealthIndicator indicator = new BunkerHealthIndicator(signer, properties);

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("bunkerPubkey", "npub1test");
        assertThat(health.getDetails()).containsEntry("pong", "pong");
    }

    @Test
    void shouldReturnDownWhenPingFails() {
        CompletableFuture<String> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("Connection timeout"));
        when(signer.ping()).thenReturn(failedFuture);
        BunkerHealthIndicator indicator = new BunkerHealthIndicator(signer, properties);

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("bunkerPubkey", "npub1test");
        assertThat(health.getDetails()).containsKey("error");
    }

    @Test
    void shouldIncludeBunkerPubkeyInDetails() {
        properties.getSigner().setBunkerPubkey("npub1different");
        when(signer.ping()).thenReturn(CompletableFuture.completedFuture("pong"));
        BunkerHealthIndicator indicator = new BunkerHealthIndicator(signer, properties);

        Health health = indicator.health();

        assertThat(health.getDetails()).containsEntry("bunkerPubkey", "npub1different");
    }
}
