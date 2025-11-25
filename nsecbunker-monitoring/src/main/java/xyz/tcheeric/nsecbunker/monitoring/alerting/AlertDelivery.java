package xyz.tcheeric.nsecbunker.monitoring.alerting;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for delivering alerts to external systems.
 *
 * <p>Implementations can deliver alerts via callbacks, webhooks,
 * email, or other notification channels.
 */
public interface AlertDelivery {

    /**
     * Delivers a single alert.
     *
     * @param alert the alert to deliver
     * @return a future completing with the delivery result
     */
    CompletableFuture<DeliveryResult> deliver(Alert alert);

    /**
     * Delivers multiple alerts.
     *
     * @param alerts the alerts to deliver
     * @return a future completing with the delivery results
     */
    CompletableFuture<List<DeliveryResult>> deliverAll(List<Alert> alerts);

    /**
     * Gets the name of this delivery channel.
     *
     * @return the channel name
     */
    String getChannelName();

    /**
     * Result of an alert delivery attempt.
     */
    @lombok.Value
    @lombok.Builder
    class DeliveryResult {
        /**
         * Whether the delivery was successful.
         */
        boolean success;

        /**
         * The alert that was delivered.
         */
        Alert alert;

        /**
         * The delivery channel used.
         */
        String channel;

        /**
         * Error message if delivery failed.
         */
        String errorMessage;

        /**
         * Delivery latency in milliseconds.
         */
        long latencyMs;

        /**
         * Creates a successful delivery result.
         */
        public static DeliveryResult success(Alert alert, String channel, long latencyMs) {
            return DeliveryResult.builder()
                    .success(true)
                    .alert(alert)
                    .channel(channel)
                    .latencyMs(latencyMs)
                    .build();
        }

        /**
         * Creates a failed delivery result.
         */
        public static DeliveryResult failure(Alert alert, String channel, String error) {
            return DeliveryResult.builder()
                    .success(false)
                    .alert(alert)
                    .channel(channel)
                    .errorMessage(error)
                    .build();
        }
    }
}
