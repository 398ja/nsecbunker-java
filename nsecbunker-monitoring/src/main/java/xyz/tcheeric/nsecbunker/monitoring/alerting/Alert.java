package xyz.tcheeric.nsecbunker.monitoring.alerting;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class Alert {
    String type;
    String message;
}
