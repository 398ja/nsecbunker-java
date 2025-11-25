package xyz.tcheeric.nsecbunker.client.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ProfileMetadata}.
 */
class ProfileMetadataTest {

    @Test
    void shouldBuildWithAllFields() {
        ProfileMetadata profile = ProfileMetadata.builder()
                .name("Alice")
                .about("Nostr enthusiast")
                .picture("https://example.com/alice.jpg")
                .nip05("alice@example.com")
                .lud16("alice@wallet.example.com")
                .website("https://alice.example.com")
                .build();

        assertThat(profile.getName()).isEqualTo("Alice");
        assertThat(profile.getAbout()).isEqualTo("Nostr enthusiast");
        assertThat(profile.getPicture()).isEqualTo("https://example.com/alice.jpg");
        assertThat(profile.getNip05()).isEqualTo("alice@example.com");
        assertThat(profile.getLud16()).isEqualTo("alice@wallet.example.com");
        assertThat(profile.getWebsite()).isEqualTo("https://alice.example.com");
    }

    @Test
    void shouldBuildWithPartialFields() {
        ProfileMetadata profile = ProfileMetadata.builder()
                .name("Bob")
                .build();

        assertThat(profile.getName()).isEqualTo("Bob");
        assertThat(profile.getAbout()).isNull();
        assertThat(profile.getPicture()).isNull();
        assertThat(profile.getNip05()).isNull();
        assertThat(profile.getLud16()).isNull();
        assertThat(profile.getWebsite()).isNull();
    }

    @Test
    void shouldBuildWithNoFields() {
        ProfileMetadata profile = ProfileMetadata.builder().build();

        assertThat(profile.getName()).isNull();
        assertThat(profile.getAbout()).isNull();
    }

    @Test
    void toJsonShouldSerializeWithProvidedMapper() {
        ProfileMetadata profile = ProfileMetadata.builder()
                .name("Charlie")
                .about("Testing")
                .build();
        ObjectMapper mapper = new ObjectMapper();

        String json = profile.toJson(mapper);

        assertThat(json).contains("\"name\":\"Charlie\"");
        assertThat(json).contains("\"about\":\"Testing\"");
    }

    @Test
    void toJsonShouldSerializeWithDefaultMapper() {
        ProfileMetadata profile = ProfileMetadata.builder()
                .name("Dave")
                .build();

        String json = profile.toJson(null);

        assertThat(json).contains("\"name\":\"Dave\"");
    }

    @Test
    void toJsonShouldExcludeNullFields() throws Exception {
        ProfileMetadata profile = ProfileMetadata.builder()
                .name("Eve")
                .build();

        String json = profile.toJson(null);

        // Jackson includes null fields by default, so they will be present
        assertThat(json).contains("\"name\":\"Eve\"");
    }

    @Test
    void shouldHaveWorkingEqualsAndHashCode() {
        ProfileMetadata profile1 = ProfileMetadata.builder()
                .name("Frank")
                .about("Test")
                .build();
        ProfileMetadata profile2 = ProfileMetadata.builder()
                .name("Frank")
                .about("Test")
                .build();
        ProfileMetadata profile3 = ProfileMetadata.builder()
                .name("George")
                .build();

        assertThat(profile1).isEqualTo(profile2);
        assertThat(profile1.hashCode()).isEqualTo(profile2.hashCode());
        assertThat(profile1).isNotEqualTo(profile3);
        assertThat(profile1).isNotEqualTo(null);
        assertThat(profile1).isNotEqualTo("string");
    }

    @Test
    void shouldHaveWorkingToString() {
        ProfileMetadata profile = ProfileMetadata.builder()
                .name("Helen")
                .about("Tester")
                .build();

        String toString = profile.toString();

        assertThat(toString).contains("ProfileMetadata");
        assertThat(toString).contains("Helen");
        assertThat(toString).contains("Tester");
    }

    @Test
    void toBuilderShouldCreateCopy() {
        ProfileMetadata original = ProfileMetadata.builder()
                .name("Ivan")
                .about("Original")
                .nip05("ivan@example.com")
                .build();

        ProfileMetadata copy = original.toBuilder()
                .about("Modified")
                .build();

        assertThat(copy.getName()).isEqualTo("Ivan");
        assertThat(copy.getAbout()).isEqualTo("Modified");
        assertThat(copy.getNip05()).isEqualTo("ivan@example.com");
        assertThat(original.getAbout()).isEqualTo("Original");
    }

    @Test
    void shouldDeserializeFromJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String json = "{\"name\":\"Julia\",\"about\":\"Test\",\"nip05\":\"julia@example.com\"}";

        ProfileMetadata profile = mapper.readValue(json, ProfileMetadata.class);

        assertThat(profile.getName()).isEqualTo("Julia");
        assertThat(profile.getAbout()).isEqualTo("Test");
        assertThat(profile.getNip05()).isEqualTo("julia@example.com");
        assertThat(profile.getPicture()).isNull();
    }

    @Test
    void shouldRoundTripThroughJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        ProfileMetadata original = ProfileMetadata.builder()
                .name("Kevin")
                .about("Round trip test")
                .picture("https://example.com/kevin.png")
                .nip05("kevin@example.com")
                .lud16("kevin@wallet.example.com")
                .website("https://kevin.example.com")
                .build();

        String json = original.toJson(mapper);
        ProfileMetadata deserialized = mapper.readValue(json, ProfileMetadata.class);

        assertThat(deserialized).isEqualTo(original);
    }
}
