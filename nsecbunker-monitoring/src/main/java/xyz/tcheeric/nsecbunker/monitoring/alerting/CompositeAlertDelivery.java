package xyz.tcheeric.nsecbunker.monitoring.alerting;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Delivers alerts to multiple delivery channels.
 *
 * <p>Useful for sending alerts to multiple destinations simultaneously
 * (e.g., webhook + callback).
 *
 * <p>Usage example:
 * <pre>{@code
 * AlertDelivery composite = new CompositeAlertDelivery(
 *     new CallbackAlertDelivery(alert -> log.info("Alert: {}", alert)),
 *     WebhookAlertDelivery.builder()
 *         .webhookUrl("https://hooks.example.com/alerts")
 *         .build()
 * );
 *
 * composite.deliver(alert);
 * }</pre>
 */
@Slf4j
public class CompositeAlertDelivery implements AlertDelivery {

    private static final String CHANNEL_NAME = "composite";

    private final List<AlertDelivery> deliveries;
    private final String name;

    /**
     * Creates a composite delivery with the given channels.
     *
     * @param deliveries the delivery channels
     */
    public CompositeAlertDelivery(AlertDelivery... deliveries) {
        this(CHANNEL_NAME, Arrays.asList(deliveries));
    }

    /**
     * Creates a composite delivery with custom name and channels.
     *
     * @param name the channel name
     * @param deliveries the delivery channels
     */
    public CompositeAlertDelivery(String name, List<AlertDelivery> deliveries) {
        this.name = name != null ? name : CHANNEL_NAME;
        this.deliveries = new CopyOnWriteArrayList<>(deliveries);
    }

    /**
     * Adds a delivery channel.
     *
     * @param delivery the delivery to add
     */
    public void addDelivery(AlertDelivery delivery) {
        if (delivery != null) {
            deliveries.add(delivery);
        }
    }

    /**
     * Removes a delivery channel.
     *
     * @param delivery the delivery to remove
     * @return true if removed
     */
    public boolean removeDelivery(AlertDelivery delivery) {
        return deliveries.remove(delivery);
    }

    /**
     * Gets all delivery channels.
     *
     * @return list of delivery channels
     */
    public List<AlertDelivery> getDeliveries() {
        return new ArrayList<>(deliveries);
    }

    @Override
    public CompletableFuture<DeliveryResult> deliver(Alert alert) {
        if (deliveries.isEmpty()) {
            return CompletableFuture.completedFuture(
                    DeliveryResult.failure(alert, name, "No delivery channels configured"));
        }

        List<CompletableFuture<DeliveryResult>> futures = deliveries.stream()
                .map(d -> d.deliver(alert))
                .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    // Return success if at least one channel succeeded
                    boolean anySuccess = futures.stream()
                            .map(CompletableFuture::join)
                            .anyMatch(DeliveryResult::isSuccess);

                    if (anySuccess) {
                        return DeliveryResult.success(alert, name, 0);
                    } else {
                        String errors = futures.stream()
                                .map(CompletableFuture::join)
                                .filter(r -> !r.isSuccess())
                                .map(r -> r.getChannel() + ": " + r.getErrorMessage())
                                .collect(Collectors.joining("; "));
                        return DeliveryResult.failure(alert, name, errors);
                    }
                });
    }

    @Override
    public CompletableFuture<List<DeliveryResult>> deliverAll(List<Alert> alerts) {
        if (deliveries.isEmpty()) {
            return CompletableFuture.completedFuture(
                    alerts.stream()
                            .map(a -> DeliveryResult.failure(a, name, "No delivery channels configured"))
                            .collect(Collectors.toList()));
        }

        // Collect all results from all deliveries for all alerts
        List<CompletableFuture<List<DeliveryResult>>> allFutures = deliveries.stream()
                .map(d -> d.deliverAll(alerts))
                .collect(Collectors.toList());

        return CompletableFuture.allOf(allFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    // Aggregate results per alert - return success if any channel succeeded
                    List<DeliveryResult> results = new ArrayList<>();
                    for (int i = 0; i < alerts.size(); i++) {
                        Alert alert = alerts.get(i);
                        final int idx = i;

                        boolean anySuccess = allFutures.stream()
                                .map(CompletableFuture::join)
                                .map(list -> list.get(idx))
                                .anyMatch(DeliveryResult::isSuccess);

                        if (anySuccess) {
                            results.add(DeliveryResult.success(alert, name, 0));
                        } else {
                            String errors = allFutures.stream()
                                    .map(CompletableFuture::join)
                                    .map(list -> list.get(idx))
                                    .filter(r -> !r.isSuccess())
                                    .map(r -> r.getChannel() + ": " + r.getErrorMessage())
                                    .collect(Collectors.joining("; "));
                            results.add(DeliveryResult.failure(alert, name, errors));
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
