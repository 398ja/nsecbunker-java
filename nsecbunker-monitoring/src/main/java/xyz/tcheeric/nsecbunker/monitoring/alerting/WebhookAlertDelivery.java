package xyz.tcheeric.nsecbunker.monitoring.alerting;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Alert delivery via HTTP webhook.
 *
 * <p>Sends alerts to a configured webhook URL as JSON payloads.
 * Supports custom headers for authentication.
 *
 * <p>Usage example:
 * <pre>{@code
 * AlertDelivery delivery = WebhookAlertDelivery.builder()
 *     .webhookUrl("https://hooks.example.com/alerts")
 *     .header("Authorization", "Bearer token123")
 *     .timeout(Duration.ofSeconds(10))
 *     .build();
 *
 * delivery.deliver(alert);
 * }</pre>
 */
@Slf4j
public class WebhookAlertDelivery implements AlertDelivery {

    private static final String CHANNEL_NAME = "webhook";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final String webhookUrl;
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    private final Map<String, String> headers;
    private final String name;

    @Builder
    private WebhookAlertDelivery(String webhookUrl, OkHttpClient client,
                                 ObjectMapper objectMapper, Map<String, String> headers,
                                 Duration timeout, String name) {
        this.webhookUrl = Objects.requireNonNull(webhookUrl, "webhookUrl must not be null");
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.headers = headers != null ? new HashMap<>(headers) : new HashMap<>();
        this.name = name != null ? name : CHANNEL_NAME;

        Duration effectiveTimeout = timeout != null ? timeout : Duration.ofSeconds(30);
        this.client = client != null ? client : new OkHttpClient.Builder()
                .connectTimeout(effectiveTimeout)
                .readTimeout(effectiveTimeout)
                .writeTimeout(effectiveTimeout)
                .build();
    }

    @Override
    public CompletableFuture<DeliveryResult> deliver(Alert alert) {
        CompletableFuture<DeliveryResult> future = new CompletableFuture<>();
        long start = System.currentTimeMillis();

        try {
            Map<String, Object> payload = buildPayload(alert);
            String json = objectMapper.writeValueAsString(payload);

            Request.Builder requestBuilder = new Request.Builder()
                    .url(webhookUrl)
                    .post(RequestBody.create(json, JSON));

            // Add custom headers
            for (Map.Entry<String, String> header : headers.entrySet()) {
                requestBuilder.addHeader(header.getKey(), header.getValue());
            }

            Request request = requestBuilder.build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    log.warn("webhook_network_error channel={} error={}", name, e.getMessage());
                    future.complete(DeliveryResult.failure(alert, name, e.getMessage()));
                }

                @Override
                public void onResponse(Call call, Response response) {
                    long latency = System.currentTimeMillis() - start;
                    try (response) {
                        if (response.isSuccessful()) {
                            log.debug("webhook_delivered channel={} type={} status={}", name, alert.getType(), response.code());
                            future.complete(DeliveryResult.success(alert, name, latency));
                        } else {
                            String error = "HTTP " + response.code() + ": " + response.message();
                            log.warn("webhook_http_error channel={} status={}", name, error);
                            future.complete(DeliveryResult.failure(alert, name, error));
                        }
                    }
                }
            });
        } catch (Exception e) {
            log.warn("webhook_request_failed channel={} error={}", name, e.getMessage());
            future.complete(DeliveryResult.failure(alert, name, e.getMessage()));
        }

        return future;
    }

    @Override
    public CompletableFuture<List<DeliveryResult>> deliverAll(List<Alert> alerts) {
        List<CompletableFuture<DeliveryResult>> futures = new ArrayList<>();
        for (Alert alert : alerts) {
            futures.add(deliver(alert));
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    List<DeliveryResult> results = new ArrayList<>();
                    for (CompletableFuture<DeliveryResult> f : futures) {
                        results.add(f.join());
                    }
                    return results;
                });
    }

    @Override
    public String getChannelName() {
        return name;
    }

    /**
     * Builds the JSON payload for an alert.
     */
    private Map<String, Object> buildPayload(Alert alert) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", alert.getType());
        payload.put("message", alert.getMessage());
        payload.put("timestamp", Instant.now().toString());
        payload.put("source", "nsecbunker-java");
        return payload;
    }

    /**
     * Builder class for WebhookAlertDelivery.
     */
    public static class WebhookAlertDeliveryBuilder {
        private Map<String, String> headers = new HashMap<>();

        /**
         * Sets all headers at once.
         *
         * @param headers the headers map
         * @return this builder
         */
        public WebhookAlertDeliveryBuilder headers(Map<String, String> headers) {
            this.headers = headers != null ? new HashMap<>(headers) : new HashMap<>();
            return this;
        }

        /**
         * Adds a single header.
         *
         * @param name the header name
         * @param value the header value
         * @return this builder
         */
        public WebhookAlertDeliveryBuilder header(String name, String value) {
            if (this.headers == null) {
                this.headers = new HashMap<>();
            }
            this.headers.put(name, value);
            return this;
        }
    }
}
