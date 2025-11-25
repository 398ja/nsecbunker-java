package xyz.tcheeric.nsecbunker.starter;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NsecBunkerPropertiesTest {

    @Test
    void shouldHaveDefaultValues() {
        NsecBunkerProperties properties = new NsecBunkerProperties();

        assertThat(properties.getAdmin()).isNotNull();
        assertThat(properties.getSigner()).isNotNull();
        assertThat(properties.getMetrics()).isNotNull();
    }

    @Test
    void adminShouldHaveDefaultValues() {
        NsecBunkerProperties.Admin admin = new NsecBunkerProperties.Admin();

        assertThat(admin.getConnectTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(admin.getRequestTimeout()).isEqualTo(Duration.ofSeconds(60));
        assertThat(admin.isUseEphemeralKey()).isTrue();
        assertThat(admin.getRelays()).isEmpty();
        assertThat(admin.getBunkerPubkey()).isNull();
        assertThat(admin.getAdminPrivateKey()).isNull();
        assertThat(admin.getSecret()).isNull();
    }

    @Test
    void adminShouldAllowSettingValues() {
        NsecBunkerProperties.Admin admin = new NsecBunkerProperties.Admin();
        admin.setBunkerPubkey("npub1test");
        admin.setAdminPrivateKey("nsec1test");
        admin.setRelays(List.of("wss://relay.example.com"));
        admin.setSecret("secret123");
        admin.setConnectTimeout(Duration.ofSeconds(10));
        admin.setRequestTimeout(Duration.ofSeconds(30));
        admin.setUseEphemeralKey(false);

        assertThat(admin.getBunkerPubkey()).isEqualTo("npub1test");
        assertThat(admin.getAdminPrivateKey()).isEqualTo("nsec1test");
        assertThat(admin.getRelays()).containsExactly("wss://relay.example.com");
        assertThat(admin.getSecret()).isEqualTo("secret123");
        assertThat(admin.getConnectTimeout()).isEqualTo(Duration.ofSeconds(10));
        assertThat(admin.getRequestTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(admin.isUseEphemeralKey()).isFalse();
    }

    @Test
    void signerShouldHaveDefaultValues() {
        NsecBunkerProperties.Signer signer = new NsecBunkerProperties.Signer();

        assertThat(signer.getConnectTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(signer.getRequestTimeout()).isEqualTo(Duration.ofSeconds(60));
        assertThat(signer.isUseEphemeralKey()).isTrue();
        assertThat(signer.getRelays()).isEmpty();
        assertThat(signer.getBunkerPubkey()).isNull();
        assertThat(signer.getClientPrivateKey()).isNull();
        assertThat(signer.getSecret()).isNull();
    }

    @Test
    void signerShouldAllowSettingValues() {
        NsecBunkerProperties.Signer signer = new NsecBunkerProperties.Signer();
        signer.setBunkerPubkey("npub1bunker");
        signer.setClientPrivateKey("nsec1client");
        signer.setRelays(List.of("wss://relay1.example.com", "wss://relay2.example.com"));
        signer.setSecret("clientsecret");
        signer.setConnectTimeout(Duration.ofSeconds(15));
        signer.setRequestTimeout(Duration.ofSeconds(45));
        signer.setUseEphemeralKey(false);

        assertThat(signer.getBunkerPubkey()).isEqualTo("npub1bunker");
        assertThat(signer.getClientPrivateKey()).isEqualTo("nsec1client");
        assertThat(signer.getRelays()).hasSize(2);
        assertThat(signer.getSecret()).isEqualTo("clientsecret");
        assertThat(signer.getConnectTimeout()).isEqualTo(Duration.ofSeconds(15));
        assertThat(signer.getRequestTimeout()).isEqualTo(Duration.ofSeconds(45));
        assertThat(signer.isUseEphemeralKey()).isFalse();
    }

    @Test
    void metricsShouldHaveDefaultValues() {
        NsecBunkerProperties.Metrics metrics = new NsecBunkerProperties.Metrics();

        assertThat(metrics.isEnabled()).isTrue();
        assertThat(metrics.getPrefix()).isEqualTo("nsecbunker");
        assertThat(metrics.isPercentiles()).isTrue();
        assertThat(metrics.isPerMethodMetrics()).isTrue();
    }

    @Test
    void metricsShouldAllowSettingValues() {
        NsecBunkerProperties.Metrics metrics = new NsecBunkerProperties.Metrics();
        metrics.setEnabled(false);
        metrics.setPrefix("custom.bunker");
        metrics.setPercentiles(false);
        metrics.setPerMethodMetrics(false);

        assertThat(metrics.isEnabled()).isFalse();
        assertThat(metrics.getPrefix()).isEqualTo("custom.bunker");
        assertThat(metrics.isPercentiles()).isFalse();
        assertThat(metrics.isPerMethodMetrics()).isFalse();
    }

    @Test
    void propertiesShouldAllowAccessToNestedObjects() {
        NsecBunkerProperties properties = new NsecBunkerProperties();
        properties.getAdmin().setBunkerPubkey("admin-pubkey");
        properties.getSigner().setBunkerPubkey("signer-pubkey");
        properties.getMetrics().setEnabled(false);

        assertThat(properties.getAdmin().getBunkerPubkey()).isEqualTo("admin-pubkey");
        assertThat(properties.getSigner().getBunkerPubkey()).isEqualTo("signer-pubkey");
        assertThat(properties.getMetrics().isEnabled()).isFalse();
    }
}
