package xyz.tcheeric.nsecbunker.starter.info;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.info.Info;
import xyz.tcheeric.nsecbunker.starter.NsecBunkerProperties;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BunkerInfoContributorTest {

    private NsecBunkerProperties properties;
    private BunkerInfoContributor contributor;

    @BeforeEach
    void setUp() {
        properties = new NsecBunkerProperties();
        contributor = new BunkerInfoContributor(properties);
    }

    @Test
    void shouldContributeBunkerPubkey() {
        properties.getSigner().setBunkerPubkey("npub1test");
        Info.Builder builder = new Info.Builder();

        contributor.contribute(builder);
        Info info = builder.build();

        @SuppressWarnings("unchecked")
        Map<String, Object> details = (Map<String, Object>) info.getDetails().get("nsecbunker");
        assertThat(details).containsEntry("bunkerPubkey", "npub1test");
    }

    @Test
    void shouldContributeRelays() {
        properties.getSigner().setRelays(List.of("wss://relay1.example.com", "wss://relay2.example.com"));
        Info.Builder builder = new Info.Builder();

        contributor.contribute(builder);
        Info info = builder.build();

        @SuppressWarnings("unchecked")
        Map<String, Object> details = (Map<String, Object>) info.getDetails().get("nsecbunker");
        @SuppressWarnings("unchecked")
        List<String> relays = (List<String>) details.get("relays");
        assertThat(relays).containsExactly("wss://relay1.example.com", "wss://relay2.example.com");
    }

    @Test
    void shouldContributeEmptyRelaysWhenNoneConfigured() {
        Info.Builder builder = new Info.Builder();

        contributor.contribute(builder);
        Info info = builder.build();

        @SuppressWarnings("unchecked")
        Map<String, Object> details = (Map<String, Object>) info.getDetails().get("nsecbunker");
        @SuppressWarnings("unchecked")
        List<String> relays = (List<String>) details.get("relays");
        assertThat(relays).isEmpty();
    }

    @Test
    void shouldContributeNullBunkerPubkeyWhenNotConfigured() {
        Info.Builder builder = new Info.Builder();

        contributor.contribute(builder);
        Info info = builder.build();

        @SuppressWarnings("unchecked")
        Map<String, Object> details = (Map<String, Object>) info.getDetails().get("nsecbunker");
        assertThat(details.get("bunkerPubkey")).isNull();
    }

    @Test
    void shouldContributeUnderNsecbunkerKey() {
        Info.Builder builder = new Info.Builder();

        contributor.contribute(builder);
        Info info = builder.build();

        assertThat(info.getDetails()).containsKey("nsecbunker");
    }
}
