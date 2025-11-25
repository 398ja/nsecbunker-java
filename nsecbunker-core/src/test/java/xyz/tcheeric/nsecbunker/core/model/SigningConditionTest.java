package xyz.tcheeric.nsecbunker.core.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class SigningConditionTest {

    @Test
    void shouldBuildWithDefaultValues() {
        SigningCondition condition = SigningCondition.builder()
                .type(SigningCondition.ConditionType.EVENT_KIND)
                .build();

        assertThat(condition.getType()).isEqualTo(SigningCondition.ConditionType.EVENT_KIND);
        assertThat(condition.isAllow()).isTrue();
        assertThat(condition.isAllowCondition()).isTrue();
        assertThat(condition.isDenyCondition()).isFalse();
    }

    @Test
    void shouldBuildWithAllFields() {
        Instant from = Instant.now();
        Instant until = from.plus(1, ChronoUnit.HOURS);

        SigningCondition condition = SigningCondition.builder()
                .type(SigningCondition.ConditionType.TIME_WINDOW)
                .method("sign_event")
                .eventKind(1)
                .validFrom(from)
                .validUntil(until)
                .maxOperations(100)
                .rateLimitWindowSeconds(3600)
                .allowedRecipient("npub1test")
                .contentPattern(".*test.*")
                .allow(true)
                .description("Test condition")
                .build();

        assertThat(condition.getMethod()).isEqualTo("sign_event");
        assertThat(condition.getEventKind()).isEqualTo(1);
        assertThat(condition.getValidFrom()).isEqualTo(from);
        assertThat(condition.getValidUntil()).isEqualTo(until);
        assertThat(condition.getMaxOperations()).isEqualTo(100);
        assertThat(condition.getRateLimitWindowSeconds()).isEqualTo(3600);
        assertThat(condition.getAllowedRecipient()).isEqualTo("npub1test");
        assertThat(condition.getContentPattern()).isEqualTo(".*test.*");
        assertThat(condition.getDescription()).isEqualTo("Test condition");
    }

    @Test
    void allowEventKindShouldCreateCondition() {
        SigningCondition condition = SigningCondition.allowEventKind(1);

        assertThat(condition.getType()).isEqualTo(SigningCondition.ConditionType.EVENT_KIND);
        assertThat(condition.getEventKind()).isEqualTo(1);
        assertThat(condition.isAllowCondition()).isTrue();
    }

    @Test
    void allowMethodShouldCreateCondition() {
        SigningCondition condition = SigningCondition.allowMethod("encrypt");

        assertThat(condition.getType()).isEqualTo(SigningCondition.ConditionType.METHOD);
        assertThat(condition.getMethod()).isEqualTo("encrypt");
        assertThat(condition.isAllowCondition()).isTrue();
    }

    @Test
    void timeWindowShouldCreateCondition() {
        Instant from = Instant.now();
        Instant until = from.plus(1, ChronoUnit.HOURS);

        SigningCondition condition = SigningCondition.timeWindow(from, until);

        assertThat(condition.getType()).isEqualTo(SigningCondition.ConditionType.TIME_WINDOW);
        assertThat(condition.getValidFrom()).isEqualTo(from);
        assertThat(condition.getValidUntil()).isEqualTo(until);
        assertThat(condition.isAllowCondition()).isTrue();
    }

    @Test
    void rateLimitShouldCreateCondition() {
        SigningCondition condition = SigningCondition.rateLimit(100, 3600);

        assertThat(condition.getType()).isEqualTo(SigningCondition.ConditionType.RATE_LIMIT);
        assertThat(condition.getMaxOperations()).isEqualTo(100);
        assertThat(condition.getRateLimitWindowSeconds()).isEqualTo(3600);
        assertThat(condition.isAllowCondition()).isTrue();
    }

    @Test
    void isWithinTimeWindowShouldReturnTrueWhenWithinWindow() {
        SigningCondition condition = SigningCondition.builder()
                .type(SigningCondition.ConditionType.TIME_WINDOW)
                .validFrom(Instant.now().minus(1, ChronoUnit.HOURS))
                .validUntil(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();

        assertThat(condition.isWithinTimeWindow()).isTrue();
    }

    @Test
    void isWithinTimeWindowShouldReturnFalseWhenBeforeStart() {
        SigningCondition condition = SigningCondition.builder()
                .type(SigningCondition.ConditionType.TIME_WINDOW)
                .validFrom(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();

        assertThat(condition.isWithinTimeWindow()).isFalse();
    }

    @Test
    void isWithinTimeWindowShouldReturnFalseWhenAfterEnd() {
        SigningCondition condition = SigningCondition.builder()
                .type(SigningCondition.ConditionType.TIME_WINDOW)
                .validUntil(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();

        assertThat(condition.isWithinTimeWindow()).isFalse();
    }

    @Test
    void isWithinTimeWindowShouldReturnTrueWhenNoTimeRestrictions() {
        SigningCondition condition = SigningCondition.builder()
                .type(SigningCondition.ConditionType.METHOD)
                .method("sign_event")
                .build();

        assertThat(condition.isWithinTimeWindow()).isTrue();
    }

    @Test
    void appliesToEventKindShouldReturnTrueWhenMatches() {
        SigningCondition condition = SigningCondition.builder()
                .eventKind(1)
                .build();

        assertThat(condition.appliesTo(1)).isTrue();
        assertThat(condition.appliesTo(4)).isFalse();
    }

    @Test
    void appliesToEventKindShouldReturnTrueWhenKindNull() {
        SigningCondition condition = SigningCondition.builder().build();

        assertThat(condition.appliesTo(1)).isTrue();
        assertThat(condition.appliesTo(4)).isTrue();
    }

    @Test
    void appliesToMethodShouldReturnTrueWhenMatches() {
        SigningCondition condition = SigningCondition.builder()
                .method("sign_event")
                .build();

        assertThat(condition.appliesTo("sign_event")).isTrue();
        assertThat(condition.appliesTo("encrypt")).isFalse();
    }

    @Test
    void appliesToMethodShouldReturnTrueWhenMethodNull() {
        SigningCondition condition = SigningCondition.builder().build();

        assertThat(condition.appliesTo("sign_event")).isTrue();
        assertThat(condition.appliesTo("encrypt")).isTrue();
    }

    @Test
    void matchesContentPatternShouldReturnTrueWhenMatches() {
        SigningCondition condition = SigningCondition.builder()
                .contentPattern(".*hello.*")
                .build();

        assertThat(condition.matchesContentPattern("say hello world")).isTrue();
        assertThat(condition.matchesContentPattern("goodbye")).isFalse();
    }

    @Test
    void matchesContentPatternShouldReturnTrueWhenNoPattern() {
        SigningCondition condition = SigningCondition.builder().build();

        assertThat(condition.matchesContentPattern("any content")).isTrue();
    }

    @Test
    void matchesContentPatternShouldReturnTrueWhenEmptyPattern() {
        SigningCondition condition = SigningCondition.builder()
                .contentPattern("")
                .build();

        assertThat(condition.matchesContentPattern("any content")).isTrue();
    }

    @Test
    void matchesContentPatternShouldReturnFalseWhenContentNull() {
        SigningCondition condition = SigningCondition.builder()
                .contentPattern(".*test.*")
                .build();

        assertThat(condition.matchesContentPattern(null)).isFalse();
    }

    @Test
    void isDenyConditionShouldReturnTrueWhenAllowFalse() {
        SigningCondition condition = SigningCondition.builder()
                .allow(false)
                .build();

        assertThat(condition.isDenyCondition()).isTrue();
        assertThat(condition.isAllowCondition()).isFalse();
    }

    @Test
    void toBuilderShouldCreateCopy() {
        SigningCondition original = SigningCondition.builder()
                .type(SigningCondition.ConditionType.METHOD)
                .method("sign_event")
                .allow(true)
                .build();

        SigningCondition copy = original.toBuilder()
                .allow(false)
                .build();

        assertThat(copy.getMethod()).isEqualTo("sign_event");
        assertThat(copy.isAllowCondition()).isFalse();
        assertThat(original.isAllowCondition()).isTrue();
    }

    @Test
    void equalsShouldWorkCorrectly() {
        SigningCondition condition1 = SigningCondition.builder()
                .type(SigningCondition.ConditionType.EVENT_KIND)
                .eventKind(1)
                .allow(true)
                .build();

        SigningCondition condition2 = SigningCondition.builder()
                .type(SigningCondition.ConditionType.EVENT_KIND)
                .eventKind(1)
                .allow(true)
                .build();

        assertThat(condition1).isEqualTo(condition2);
        assertThat(condition1.hashCode()).isEqualTo(condition2.hashCode());
    }

    @Test
    void shouldDeserializeFromJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        String json = """
            {
                "type": "EVENT_KIND",
                "event_kind": 1,
                "method": "sign_event",
                "max_operations": 100,
                "rate_limit_window_seconds": 3600,
                "is_allow": true,
                "description": "Test"
            }
            """;

        SigningCondition condition = mapper.readValue(json, SigningCondition.class);

        assertThat(condition.getType()).isEqualTo(SigningCondition.ConditionType.EVENT_KIND);
        assertThat(condition.getEventKind()).isEqualTo(1);
        assertThat(condition.getMethod()).isEqualTo("sign_event");
        assertThat(condition.getMaxOperations()).isEqualTo(100);
        assertThat(condition.isAllow()).isTrue();
    }

    @Test
    void shouldSupportAllConditionTypes() {
        for (SigningCondition.ConditionType type : SigningCondition.ConditionType.values()) {
            SigningCondition condition = SigningCondition.builder()
                    .type(type)
                    .build();
            assertThat(condition.getType()).isEqualTo(type);
        }
    }
}
