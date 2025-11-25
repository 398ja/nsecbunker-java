package xyz.tcheeric.nsecbunker.monitoring.metrics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class MetricsRecordTest {

    @Test
    void shouldBuildWithAllFields() {
        Instant now = Instant.now();

        MetricsRecord record = MetricsRecord.builder()
                .id("metrics-123")
                .keyName("my-key")
                .tokenId("token-456")
                .userPubkey("npub1test")
                .count(1000)
                .failures(10)
                .lastUpdated(now)
                .build();

        assertThat(record.getId()).isEqualTo("metrics-123");
        assertThat(record.getKeyName()).isEqualTo("my-key");
        assertThat(record.getTokenId()).isEqualTo("token-456");
        assertThat(record.getUserPubkey()).isEqualTo("npub1test");
        assertThat(record.getCount()).isEqualTo(1000);
        assertThat(record.getFailures()).isEqualTo(10);
        assertThat(record.getLastUpdated()).isEqualTo(now);
    }

    @Test
    void shouldBuildWithNullFields() {
        MetricsRecord record = MetricsRecord.builder()
                .keyName("my-key")
                .count(50)
                .failures(0)
                .build();

        assertThat(record.getKeyName()).isEqualTo("my-key");
        assertThat(record.getTokenId()).isNull();
        assertThat(record.getUserPubkey()).isNull();
        assertThat(record.getCount()).isEqualTo(50);
    }

    @Test
    void toBuilderShouldCreateCopy() {
        MetricsRecord original = MetricsRecord.builder()
                .keyName("my-key")
                .count(100)
                .failures(5)
                .build();

        MetricsRecord copy = original.toBuilder()
                .count(150)
                .failures(10)
                .build();

        assertThat(copy.getKeyName()).isEqualTo("my-key");
        assertThat(copy.getCount()).isEqualTo(150);
        assertThat(copy.getFailures()).isEqualTo(10);
        assertThat(original.getCount()).isEqualTo(100);
    }

    @Test
    void equalsShouldWorkCorrectly() {
        Instant now = Instant.now();

        MetricsRecord record1 = MetricsRecord.builder()
                .id("metrics-123")
                .keyName("my-key")
                .count(100)
                .failures(5)
                .lastUpdated(now)
                .build();

        MetricsRecord record2 = MetricsRecord.builder()
                .id("metrics-123")
                .keyName("my-key")
                .count(100)
                .failures(5)
                .lastUpdated(now)
                .build();

        assertThat(record1).isEqualTo(record2);
        assertThat(record1.hashCode()).isEqualTo(record2.hashCode());
    }

    @Test
    void toStringShouldContainFields() {
        MetricsRecord record = MetricsRecord.builder()
                .id("metrics-123")
                .keyName("my-key")
                .count(100)
                .build();

        String str = record.toString();
        assertThat(str).contains("metrics-123");
        assertThat(str).contains("my-key");
    }

    @Test
    void shouldDeserializeFromJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        String json = """
            {
                "id": "metrics-123",
                "keyName": "my-key",
                "tokenId": "token-456",
                "userPubkey": "npub1test",
                "count": 500,
                "failures": 25
            }
            """;

        MetricsRecord record = mapper.readValue(json, MetricsRecord.class);

        assertThat(record.getId()).isEqualTo("metrics-123");
        assertThat(record.getKeyName()).isEqualTo("my-key");
        assertThat(record.getTokenId()).isEqualTo("token-456");
        assertThat(record.getCount()).isEqualTo(500);
        assertThat(record.getFailures()).isEqualTo(25);
    }

    @Test
    void shouldSerializeToJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        MetricsRecord record = MetricsRecord.builder()
                .id("metrics-123")
                .keyName("my-key")
                .count(100)
                .failures(5)
                .build();

        String json = mapper.writeValueAsString(record);

        assertThat(json).contains("metrics-123");
        assertThat(json).contains("my-key");
        assertThat(json).contains("100");
    }
}
