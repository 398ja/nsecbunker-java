package xyz.tcheeric.nsecbunker.monitoring.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LogEntryTest {

    @Test
    void shouldBuildWithAllFields() {
        Instant now = Instant.now();
        Map<String, Object> metadata = Map.of("kind", 1, "size", 100);

        LogEntry entry = LogEntry.builder()
                .timestamp(now)
                .keyName("my-key")
                .userPubkey("npub1test")
                .method("sign_event")
                .result("success")
                .metadata(metadata)
                .build();

        assertThat(entry.getTimestamp()).isEqualTo(now);
        assertThat(entry.getKeyName()).isEqualTo("my-key");
        assertThat(entry.getUserPubkey()).isEqualTo("npub1test");
        assertThat(entry.getMethod()).isEqualTo("sign_event");
        assertThat(entry.getResult()).isEqualTo("success");
        assertThat(entry.getMetadata()).containsEntry("kind", 1);
    }

    @Test
    void shouldBuildWithNullFields() {
        LogEntry entry = LogEntry.builder()
                .method("get_public_key")
                .result("success")
                .build();

        assertThat(entry.getMethod()).isEqualTo("get_public_key");
        assertThat(entry.getTimestamp()).isNull();
        assertThat(entry.getKeyName()).isNull();
        assertThat(entry.getMetadata()).isNull();
    }

    @Test
    void toBuilderShouldCreateCopy() {
        LogEntry original = LogEntry.builder()
                .method("sign_event")
                .result("success")
                .build();

        LogEntry copy = original.toBuilder()
                .result("error")
                .build();

        assertThat(copy.getMethod()).isEqualTo("sign_event");
        assertThat(copy.getResult()).isEqualTo("error");
        assertThat(original.getResult()).isEqualTo("success");
    }

    @Test
    void equalsShouldWorkCorrectly() {
        Instant now = Instant.now();

        LogEntry entry1 = LogEntry.builder()
                .timestamp(now)
                .method("sign_event")
                .result("success")
                .build();

        LogEntry entry2 = LogEntry.builder()
                .timestamp(now)
                .method("sign_event")
                .result("success")
                .build();

        assertThat(entry1).isEqualTo(entry2);
        assertThat(entry1.hashCode()).isEqualTo(entry2.hashCode());
    }

    @Test
    void shouldDeserializeFromJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        String json = """
            {
                "timestamp": "2025-01-15T10:30:00Z",
                "key_name": "my-key",
                "user_pubkey": "npub1test",
                "method": "sign_event",
                "result": "success",
                "metadata": {"kind": 1}
            }
            """;

        LogEntry entry = mapper.readValue(json, LogEntry.class);

        assertThat(entry.getKeyName()).isEqualTo("my-key");
        assertThat(entry.getUserPubkey()).isEqualTo("npub1test");
        assertThat(entry.getMethod()).isEqualTo("sign_event");
        assertThat(entry.getResult()).isEqualTo("success");
    }

    @Test
    void shouldIgnoreUnknownJsonProperties() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        String json = """
            {
                "method": "sign_event",
                "result": "success",
                "unknown_field": "should be ignored"
            }
            """;

        LogEntry entry = mapper.readValue(json, LogEntry.class);

        assertThat(entry.getMethod()).isEqualTo("sign_event");
    }
}
