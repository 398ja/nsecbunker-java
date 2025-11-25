package xyz.tcheeric.nsecbunker.starter;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
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
    void shouldHaveWorkingEqualsAndHashCode() {
        NsecBunkerProperties props1 = new NsecBunkerProperties();
        NsecBunkerProperties props2 = new NsecBunkerProperties();

        assertThat(props1).isEqualTo(props2);
        assertThat(props1.hashCode()).isEqualTo(props2.hashCode());
        assertThat(props1).isEqualTo(props1);
        assertThat(props1).isNotEqualTo(null);
        assertThat(props1).isNotEqualTo("string");
    }

    @Test
    void shouldHaveWorkingToString() {
        NsecBunkerProperties properties = new NsecBunkerProperties();
        String toString = properties.toString();

        assertThat(toString).contains("NsecBunkerProperties");
        assertThat(toString).contains("admin");
        assertThat(toString).contains("signer");
        assertThat(toString).contains("metrics");
    }

    @Test
    void canEqualShouldWorkCorrectly() {
        NsecBunkerProperties props1 = new NsecBunkerProperties();
        NsecBunkerProperties props2 = new NsecBunkerProperties();

        assertThat(props1.canEqual(props2)).isTrue();
        assertThat(props1.canEqual("string")).isFalse();
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

    // Admin class tests for Lombok-generated methods
    @Test
    void adminShouldHaveWorkingEqualsAndHashCode() {
        NsecBunkerProperties.Admin admin1 = new NsecBunkerProperties.Admin();
        NsecBunkerProperties.Admin admin2 = new NsecBunkerProperties.Admin();

        assertThat(admin1).isEqualTo(admin2);
        assertThat(admin1.hashCode()).isEqualTo(admin2.hashCode());
        assertThat(admin1).isEqualTo(admin1);
        assertThat(admin1).isNotEqualTo(null);
        assertThat(admin1).isNotEqualTo("string");

        // Test inequality
        admin2.setBunkerPubkey("different");
        assertThat(admin1).isNotEqualTo(admin2);
    }

    @Test
    void adminShouldHaveWorkingToString() {
        NsecBunkerProperties.Admin admin = new NsecBunkerProperties.Admin();
        admin.setBunkerPubkey("npub1test");

        String toString = admin.toString();
        assertThat(toString).contains("Admin");
        assertThat(toString).contains("npub1test");
    }

    @Test
    void adminCanEqualShouldWorkCorrectly() {
        NsecBunkerProperties.Admin admin1 = new NsecBunkerProperties.Admin();
        NsecBunkerProperties.Admin admin2 = new NsecBunkerProperties.Admin();

        assertThat(admin1.canEqual(admin2)).isTrue();
        assertThat(admin1.canEqual("string")).isFalse();
    }

    @Test
    void adminShouldHandleRelaysList() {
        NsecBunkerProperties.Admin admin = new NsecBunkerProperties.Admin();
        List<String> relays = new ArrayList<>();
        relays.add("wss://relay1.example.com");
        relays.add("wss://relay2.example.com");
        admin.setRelays(relays);

        assertThat(admin.getRelays()).hasSize(2);
        assertThat(admin.getRelays()).containsExactly("wss://relay1.example.com", "wss://relay2.example.com");
    }

    // Signer class tests for Lombok-generated methods
    @Test
    void signerShouldHaveWorkingEqualsAndHashCode() {
        NsecBunkerProperties.Signer signer1 = new NsecBunkerProperties.Signer();
        NsecBunkerProperties.Signer signer2 = new NsecBunkerProperties.Signer();

        assertThat(signer1).isEqualTo(signer2);
        assertThat(signer1.hashCode()).isEqualTo(signer2.hashCode());
        assertThat(signer1).isEqualTo(signer1);
        assertThat(signer1).isNotEqualTo(null);
        assertThat(signer1).isNotEqualTo("string");

        // Test inequality
        signer2.setBunkerPubkey("different");
        assertThat(signer1).isNotEqualTo(signer2);
    }

    @Test
    void signerShouldHaveWorkingToString() {
        NsecBunkerProperties.Signer signer = new NsecBunkerProperties.Signer();
        signer.setBunkerPubkey("npub1signer");

        String toString = signer.toString();
        assertThat(toString).contains("Signer");
        assertThat(toString).contains("npub1signer");
    }

    @Test
    void signerCanEqualShouldWorkCorrectly() {
        NsecBunkerProperties.Signer signer1 = new NsecBunkerProperties.Signer();
        NsecBunkerProperties.Signer signer2 = new NsecBunkerProperties.Signer();

        assertThat(signer1.canEqual(signer2)).isTrue();
        assertThat(signer1.canEqual("string")).isFalse();
    }

    @Test
    void signerShouldHandleRelaysList() {
        NsecBunkerProperties.Signer signer = new NsecBunkerProperties.Signer();
        List<String> relays = new ArrayList<>();
        relays.add("wss://relay.example.com");
        signer.setRelays(relays);

        assertThat(signer.getRelays()).hasSize(1);
    }

    // Metrics class tests for Lombok-generated methods
    @Test
    void metricsShouldHaveWorkingEqualsAndHashCode() {
        NsecBunkerProperties.Metrics metrics1 = new NsecBunkerProperties.Metrics();
        NsecBunkerProperties.Metrics metrics2 = new NsecBunkerProperties.Metrics();

        assertThat(metrics1).isEqualTo(metrics2);
        assertThat(metrics1.hashCode()).isEqualTo(metrics2.hashCode());
        assertThat(metrics1).isEqualTo(metrics1);
        assertThat(metrics1).isNotEqualTo(null);
        assertThat(metrics1).isNotEqualTo("string");

        // Test inequality
        metrics2.setEnabled(false);
        assertThat(metrics1).isNotEqualTo(metrics2);
    }

    @Test
    void metricsShouldHaveWorkingToString() {
        NsecBunkerProperties.Metrics metrics = new NsecBunkerProperties.Metrics();
        metrics.setPrefix("custom.prefix");

        String toString = metrics.toString();
        assertThat(toString).contains("Metrics");
        assertThat(toString).contains("custom.prefix");
    }

    @Test
    void metricsCanEqualShouldWorkCorrectly() {
        NsecBunkerProperties.Metrics metrics1 = new NsecBunkerProperties.Metrics();
        NsecBunkerProperties.Metrics metrics2 = new NsecBunkerProperties.Metrics();

        assertThat(metrics1.canEqual(metrics2)).isTrue();
        assertThat(metrics1.canEqual("string")).isFalse();
    }

    // Additional edge case tests
    @Test
    void propertiesShouldNotBeEqualWhenAdminDiffers() {
        NsecBunkerProperties props1 = new NsecBunkerProperties();
        NsecBunkerProperties props2 = new NsecBunkerProperties();
        props2.getAdmin().setBunkerPubkey("different");

        assertThat(props1).isNotEqualTo(props2);
    }

    @Test
    void propertiesShouldNotBeEqualWhenSignerDiffers() {
        NsecBunkerProperties props1 = new NsecBunkerProperties();
        NsecBunkerProperties props2 = new NsecBunkerProperties();
        props2.getSigner().setBunkerPubkey("different");

        assertThat(props1).isNotEqualTo(props2);
    }

    @Test
    void propertiesShouldNotBeEqualWhenMetricsDiffers() {
        NsecBunkerProperties props1 = new NsecBunkerProperties();
        NsecBunkerProperties props2 = new NsecBunkerProperties();
        props2.getMetrics().setEnabled(false);

        assertThat(props1).isNotEqualTo(props2);
    }
}
