package xyz.tcheeric.nsecbunker.monitoring.alerting;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Alert delivery via callback function.
 *
 * <p>Delivers alerts by invoking a provided callback consumer.
 * Useful for in-process alert handling.
 *
 * <p>Usage example:
 * <pre>{@code
 * AlertDelivery delivery = new CallbackAlertDelivery(alert -> {
 *     System.out.println("Alert: " + alert.getMessage());
 *     sendToSlack(alert);
 * });
 *
 * delivery.deliver(alert);
 * }</pre>
 */
@Slf4j
public class CallbackAlertDelivery implements AlertDelivery {

    private static final String CHANNEL_NAME = "callback";

    private final Consumer<Alert> callback;
    private final String name;

    /**
     * Creates a callback alert delivery with default name.
     *
     * @param callback the callback to invoke for each alert
     */
    public CallbackAlertDelivery(Consumer<Alert> callback) {
        this(callback, CHANNEL_NAME);
    }

    /**
     * Creates a callback alert delivery with custom name.
     *
     * @param callback the callback to invoke for each alert
     * @param name the channel name
     */
    public CallbackAlertDelivery(Consumer<Alert> callback, String name) {
        this.callback = Objects.requireNonNull(callback, "callback must not be null");
        this.name = name != null ? name : CHANNEL_NAME;
    }

    @Override
    public CompletableFuture<DeliveryResult> deliver(Alert alert) {
        return CompletableFuture.supplyAsync(() -> {
            long start = System.currentTimeMillis();
            try {
                callback.accept(alert);
                long latency = System.currentTimeMillis() - start;
                log.debug("callback_delivered channel={} type={}", name, alert.getType());
                return DeliveryResult.success(alert, name, latency);
            } catch (Exception e) {
                log.warn("callback_delivery_failed channel={} type={} error={}",
                        name, alert.getType(), e.getMessage());
                return DeliveryResult.failure(alert, name, e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<List<DeliveryResult>> deliverAll(List<Alert> alerts) {
        return CompletableFuture.supplyAsync(() -> {
            List<DeliveryResult> results = new ArrayList<>();
            for (Alert alert : alerts) {
                long start = System.currentTimeMillis();
                try {
                    callback.accept(alert);
                    long latency = System.currentTimeMillis() - start;
                    results.add(DeliveryResult.success(alert, name, latency));
                } catch (Exception e) {
                    log.warn("callback_batch_delivery_failed channel={} type={} error={}",
                            name, alert.getType(), e.getMessage());
                    results.add(DeliveryResult.failure(alert, name, e.getMessage()));
                }
            }
            return results;
        });
    }

    @Override
    public String getChannelName() {
        return name;
    }
}
