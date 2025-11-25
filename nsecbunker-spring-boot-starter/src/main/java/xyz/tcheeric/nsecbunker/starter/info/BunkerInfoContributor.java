package xyz.tcheeric.nsecbunker.starter.info;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import xyz.tcheeric.nsecbunker.starter.NsecBunkerProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Contributes bunker metadata to the /actuator/info endpoint.
 */
public class BunkerInfoContributor implements InfoContributor {

    private final NsecBunkerProperties properties;

    public BunkerInfoContributor(NsecBunkerProperties properties) {
        this.properties = properties;
    }

    @Override
    public void contribute(Info.Builder builder) {
        Map<String, Object> details = new HashMap<>();
        details.put("bunkerPubkey", properties.getSigner().getBunkerPubkey());
        details.put("relays", properties.getSigner().getRelays());
        builder.withDetail("nsecbunker", details);
    }
}
