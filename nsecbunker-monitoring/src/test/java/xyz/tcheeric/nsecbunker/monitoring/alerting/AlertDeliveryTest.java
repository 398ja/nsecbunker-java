package xyz.tcheeric.nsecbunker.monitoring.alerting;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for alert delivery implementations.
 */
class AlertDeliveryTest {

    private MockWebServer mockServer;

    @BeforeEach
    void setUp() throws IOException {
        mockServer = new MockWebServer();
        mockServer.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        mockServer.shutdown();
    }

    // ========== CallbackAlertDelivery Tests ==========

    @Test
    @DisplayName("CallbackAlertDelivery invokes callback on deliver")
    void callbackDeliveryInvokesCallback() {
        AtomicReference<Alert> received = new AtomicReference<>();
        CallbackAlertDelivery delivery = new CallbackAlertDelivery(received::set);

        Alert alert = Alert.builder()
                .type("TEST")
                .message("Test message")
                .build();

        AlertDelivery.DeliveryResult result = delivery.deliver(alert).join();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getChannel()).isEqualTo("callback");
        assertThat(received.get()).isEqualTo(alert);
    }

    @Test
    @DisplayName("CallbackAlertDelivery handles callback exception")
    void callbackDeliveryHandlesCallbackException() {
        CallbackAlertDelivery delivery = new CallbackAlertDelivery(alert -> {
            throw new RuntimeException("Callback failed");
        });

        Alert alert = Alert.builder().type("TEST").message("test").build();
        AlertDelivery.DeliveryResult result = delivery.deliver(alert).join();

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("Callback failed");
    }

    @Test
    @DisplayName("CallbackAlertDelivery delivers all alerts")
    void callbackDeliveryDeliversAllAlerts() {
        List<Alert> received = new ArrayList<>();
        CallbackAlertDelivery delivery = new CallbackAlertDelivery(received::add);

        List<Alert> alerts = List.of(
                Alert.builder().type("A").message("a").build(),
                Alert.builder().type("B").message("b").build(),
                Alert.builder().type("C").message("c").build()
        );

        List<AlertDelivery.DeliveryResult> results = delivery.deliverAll(alerts).join();

        assertThat(results).hasSize(3);
        assertThat(results).allMatch(AlertDelivery.DeliveryResult::isSuccess);
        assertThat(received).hasSize(3);
    }

    @Test
    @DisplayName("CallbackAlertDelivery uses custom name")
    void callbackDeliveryUsesCustomName() {
        CallbackAlertDelivery delivery = new CallbackAlertDelivery(a -> {}, "custom-callback");

        assertThat(delivery.getChannelName()).isEqualTo("custom-callback");
    }

    // ========== WebhookAlertDelivery Tests ==========

    @Test
    @DisplayName("WebhookAlertDelivery sends POST request")
    void webhookDeliverySendsPostRequest() throws Exception {
        mockServer.enqueue(new MockResponse().setResponseCode(200));

        WebhookAlertDelivery delivery = WebhookAlertDelivery.builder()
                .webhookUrl(mockServer.url("/alerts").toString())
                .build();

        Alert alert = Alert.builder()
                .type("WEBHOOK_TEST")
                .message("Test webhook")
                .build();

        AlertDelivery.DeliveryResult result = delivery.deliver(alert).join();

        assertThat(result.isSuccess()).isTrue();

        RecordedRequest request = mockServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getHeader("Content-Type")).contains("application/json");
        assertThat(request.getBody().readUtf8()).contains("WEBHOOK_TEST");
    }

    @Test
    @DisplayName("WebhookAlertDelivery includes custom headers")
    void webhookDeliveryIncludesCustomHeaders() throws Exception {
        mockServer.enqueue(new MockResponse().setResponseCode(200));

        WebhookAlertDelivery delivery = WebhookAlertDelivery.builder()
                .webhookUrl(mockServer.url("/alerts").toString())
                .header("Authorization", "Bearer token123")
                .header("X-Custom", "custom-value")
                .build();

        Alert alert = Alert.builder().type("TEST").message("test").build();
        delivery.deliver(alert).join();

        RecordedRequest request = mockServer.takeRequest();
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer token123");
        assertThat(request.getHeader("X-Custom")).isEqualTo("custom-value");
    }

    @Test
    @DisplayName("WebhookAlertDelivery handles HTTP error")
    void webhookDeliveryHandlesHttpError() {
        mockServer.enqueue(new MockResponse().setResponseCode(500).setBody("Internal Server Error"));

        WebhookAlertDelivery delivery = WebhookAlertDelivery.builder()
                .webhookUrl(mockServer.url("/alerts").toString())
                .build();

        Alert alert = Alert.builder().type("TEST").message("test").build();
        AlertDelivery.DeliveryResult result = delivery.deliver(alert).join();

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("500");
    }

    @Test
    @DisplayName("WebhookAlertDelivery delivers all alerts")
    void webhookDeliveryDeliversAllAlerts() {
        mockServer.enqueue(new MockResponse().setResponseCode(200));
        mockServer.enqueue(new MockResponse().setResponseCode(200));

        WebhookAlertDelivery delivery = WebhookAlertDelivery.builder()
                .webhookUrl(mockServer.url("/alerts").toString())
                .build();

        List<Alert> alerts = List.of(
                Alert.builder().type("A").message("a").build(),
                Alert.builder().type("B").message("b").build()
        );

        List<AlertDelivery.DeliveryResult> results = delivery.deliverAll(alerts).join();

        assertThat(results).hasSize(2);
        assertThat(results).allMatch(AlertDelivery.DeliveryResult::isSuccess);
    }

    // ========== CompositeAlertDelivery Tests ==========

    @Test
    @DisplayName("CompositeAlertDelivery delivers to all channels")
    void compositeDeliveryDeliversToAllChannels() {
        List<Alert> channel1Alerts = new ArrayList<>();
        List<Alert> channel2Alerts = new ArrayList<>();

        CompositeAlertDelivery composite = new CompositeAlertDelivery(
                new CallbackAlertDelivery(channel1Alerts::add, "channel1"),
                new CallbackAlertDelivery(channel2Alerts::add, "channel2")
        );

        Alert alert = Alert.builder().type("TEST").message("test").build();
        AlertDelivery.DeliveryResult result = composite.deliver(alert).join();

        assertThat(result.isSuccess()).isTrue();
        assertThat(channel1Alerts).hasSize(1);
        assertThat(channel2Alerts).hasSize(1);
    }

    @Test
    @DisplayName("CompositeAlertDelivery succeeds if any channel succeeds")
    void compositeDeliverySucceedsIfAnyChannelSucceeds() {
        CompositeAlertDelivery composite = new CompositeAlertDelivery(
                new CallbackAlertDelivery(a -> {
                    throw new RuntimeException("fail");
                }, "failing"),
                new CallbackAlertDelivery(a -> {}, "working")
        );

        Alert alert = Alert.builder().type("TEST").message("test").build();
        AlertDelivery.DeliveryResult result = composite.deliver(alert).join();

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("CompositeAlertDelivery fails if all channels fail")
    void compositeDeliveryFailsIfAllChannelsFail() {
        CompositeAlertDelivery composite = new CompositeAlertDelivery(
                new CallbackAlertDelivery(a -> {
                    throw new RuntimeException("fail1");
                }),
                new CallbackAlertDelivery(a -> {
                    throw new RuntimeException("fail2");
                })
        );

        Alert alert = Alert.builder().type("TEST").message("test").build();
        AlertDelivery.DeliveryResult result = composite.deliver(alert).join();

        assertThat(result.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("CompositeAlertDelivery handles empty channels")
    void compositeDeliveryHandlesEmptyChannels() {
        CompositeAlertDelivery composite = new CompositeAlertDelivery();

        Alert alert = Alert.builder().type("TEST").message("test").build();
        AlertDelivery.DeliveryResult result = composite.deliver(alert).join();

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("No delivery channels");
    }

    @Test
    @DisplayName("CompositeAlertDelivery allows adding/removing channels")
    void compositeDeliveryAllowsAddingRemovingChannels() {
        CompositeAlertDelivery composite = new CompositeAlertDelivery();

        CallbackAlertDelivery callback = new CallbackAlertDelivery(a -> {});
        composite.addDelivery(callback);
        assertThat(composite.getDeliveries()).hasSize(1);

        composite.removeDelivery(callback);
        assertThat(composite.getDeliveries()).isEmpty();
    }

    // ========== DeliveryResult Tests ==========

    @Test
    @DisplayName("DeliveryResult success factory method works")
    void deliveryResultSuccessFactoryWorks() {
        Alert alert = Alert.builder().type("TEST").message("test").build();
        AlertDelivery.DeliveryResult result = AlertDelivery.DeliveryResult.success(alert, "test-channel", 100);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getAlert()).isEqualTo(alert);
        assertThat(result.getChannel()).isEqualTo("test-channel");
        assertThat(result.getLatencyMs()).isEqualTo(100);
        assertThat(result.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("DeliveryResult failure factory method works")
    void deliveryResultFailureFactoryWorks() {
        Alert alert = Alert.builder().type("TEST").message("test").build();
        AlertDelivery.DeliveryResult result = AlertDelivery.DeliveryResult.failure(alert, "test-channel", "Error occurred");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getAlert()).isEqualTo(alert);
        assertThat(result.getChannel()).isEqualTo("test-channel");
        assertThat(result.getErrorMessage()).isEqualTo("Error occurred");
    }
}
