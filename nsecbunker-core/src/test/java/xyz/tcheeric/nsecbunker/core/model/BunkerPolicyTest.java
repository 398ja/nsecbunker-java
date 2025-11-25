package xyz.tcheeric.nsecbunker.core.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BunkerPolicyTest {

    @Test
    void shouldBuildWithDefaultValues() {
        BunkerPolicy policy = BunkerPolicy.builder()
                .name("test-policy")
                .build();

        assertThat(policy.getName()).isEqualTo("test-policy");
        assertThat(policy.isActive()).isTrue();
        assertThat(policy.getRules()).isEmpty();
    }

    @Test
    void shouldBuildWithAllFields() {
        Instant now = Instant.now();
        Instant expiry = now.plus(1, ChronoUnit.HOURS);
        PolicyRule rule = PolicyRule.allowMethod("sign_event");

        BunkerPolicy policy = BunkerPolicy.builder()
                .id("policy-123")
                .name("full-policy")
                .description("Test description")
                .rule(rule)
                .createdAt(now)
                .expiresAt(expiry)
                .active(true)
                .build();

        assertThat(policy.getId()).isEqualTo("policy-123");
        assertThat(policy.getName()).isEqualTo("full-policy");
        assertThat(policy.getDescription()).isEqualTo("Test description");
        assertThat(policy.getRules()).hasSize(1);
        assertThat(policy.getCreatedAt()).isEqualTo(now);
        assertThat(policy.getExpiresAt()).isEqualTo(expiry);
    }

    @Test
    void withNameShouldCreateBuilderWithName() {
        BunkerPolicy policy = BunkerPolicy.withName("my-policy")
                .description("desc")
                .build();

        assertThat(policy.getName()).isEqualTo("my-policy");
        assertThat(policy.getDescription()).isEqualTo("desc");
    }

    @Test
    void hasRulesShouldReturnTrueWhenRulesExist() {
        BunkerPolicy policy = BunkerPolicy.builder()
                .name("test")
                .rule(PolicyRule.allowMethod("sign_event"))
                .build();

        assertThat(policy.hasRules()).isTrue();
    }

    @Test
    void hasRulesShouldReturnFalseWhenNoRules() {
        BunkerPolicy policy = BunkerPolicy.builder()
                .name("test")
                .build();

        assertThat(policy.hasRules()).isFalse();
    }

    @Test
    void isExpiredShouldReturnTrueWhenExpired() {
        BunkerPolicy policy = BunkerPolicy.builder()
                .name("test")
                .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();

        assertThat(policy.isExpired()).isTrue();
    }

    @Test
    void isExpiredShouldReturnFalseWhenNotExpired() {
        BunkerPolicy policy = BunkerPolicy.builder()
                .name("test")
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();

        assertThat(policy.isExpired()).isFalse();
    }

    @Test
    void isExpiredShouldReturnFalseWhenNoExpiry() {
        BunkerPolicy policy = BunkerPolicy.builder()
                .name("test")
                .build();

        assertThat(policy.isExpired()).isFalse();
    }

    @Test
    void isValidShouldReturnTrueWhenActiveAndNotExpired() {
        BunkerPolicy policy = BunkerPolicy.builder()
                .name("test")
                .active(true)
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build();

        assertThat(policy.isValid()).isTrue();
    }

    @Test
    void isValidShouldReturnFalseWhenNotActive() {
        BunkerPolicy policy = BunkerPolicy.builder()
                .name("test")
                .active(false)
                .build();

        assertThat(policy.isValid()).isFalse();
    }

    @Test
    void isValidShouldReturnFalseWhenExpired() {
        BunkerPolicy policy = BunkerPolicy.builder()
                .name("test")
                .active(true)
                .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();

        assertThat(policy.isValid()).isFalse();
    }

    @Test
    void getTimeUntilExpirationShouldReturnDuration() {
        Instant expiry = Instant.now().plus(2, ChronoUnit.HOURS);
        BunkerPolicy policy = BunkerPolicy.builder()
                .name("test")
                .expiresAt(expiry)
                .build();

        Duration timeUntilExpiration = policy.getTimeUntilExpiration();
        assertThat(timeUntilExpiration).isNotNull();
        assertThat(timeUntilExpiration.toMinutes()).isBetween(119L, 121L);
    }

    @Test
    void getTimeUntilExpirationShouldReturnNullWhenNoExpiry() {
        BunkerPolicy policy = BunkerPolicy.builder()
                .name("test")
                .build();

        assertThat(policy.getTimeUntilExpiration()).isNull();
    }

    @Test
    void isMethodAllowedShouldReturnTrueWhenAllowed() {
        BunkerPolicy policy = BunkerPolicy.builder()
                .name("test")
                .active(true)
                .rule(PolicyRule.allowMethod("sign_event"))
                .build();

        assertThat(policy.isMethodAllowed("sign_event")).isTrue();
    }

    @Test
    void isMethodAllowedShouldReturnFalseWhenDenied() {
        BunkerPolicy policy = BunkerPolicy.builder()
                .name("test")
                .active(true)
                .rule(PolicyRule.allowMethod("sign_event"))
                .rule(PolicyRule.denyMethod("sign_event"))
                .build();

        assertThat(policy.isMethodAllowed("sign_event")).isFalse();
    }

    @Test
    void isMethodAllowedShouldReturnFalseWhenNotAllowed() {
        BunkerPolicy policy = BunkerPolicy.builder()
                .name("test")
                .active(true)
                .rule(PolicyRule.allowMethod("encrypt"))
                .build();

        assertThat(policy.isMethodAllowed("sign_event")).isFalse();
    }

    @Test
    void isMethodAllowedShouldReturnFalseWhenPolicyInvalid() {
        BunkerPolicy policy = BunkerPolicy.builder()
                .name("test")
                .active(false)
                .rule(PolicyRule.allowMethod("sign_event"))
                .build();

        assertThat(policy.isMethodAllowed("sign_event")).isFalse();
    }

    @Test
    void isMethodAllowedShouldReturnFalseWhenUsageLimitReached() {
        PolicyRule rule = PolicyRule.builder()
                .type(PolicyRule.RuleType.ALLOW)
                .method("sign_event")
                .maxUsage(10L)
                .currentUsage(10L)
                .build();

        BunkerPolicy policy = BunkerPolicy.builder()
                .name("test")
                .active(true)
                .rule(rule)
                .build();

        assertThat(policy.isMethodAllowed("sign_event")).isFalse();
    }

    @Test
    void isEventKindAllowedShouldReturnTrueWhenAllowed() {
        BunkerPolicy policy = BunkerPolicy.builder()
                .name("test")
                .active(true)
                .rule(PolicyRule.allowEventKind(1))
                .build();

        assertThat(policy.isEventKindAllowed(1)).isTrue();
    }

    @Test
    void isEventKindAllowedShouldReturnFalseWhenDenied() {
        BunkerPolicy policy = BunkerPolicy.builder()
                .name("test")
                .active(true)
                .rule(PolicyRule.allowEventKind(1))
                .rule(PolicyRule.denyEventKind(1))
                .build();

        assertThat(policy.isEventKindAllowed(1)).isFalse();
    }

    @Test
    void isEventKindAllowedShouldReturnFalseWhenNotAllowed() {
        BunkerPolicy policy = BunkerPolicy.builder()
                .name("test")
                .active(true)
                .rule(PolicyRule.allowEventKind(1))
                .build();

        assertThat(policy.isEventKindAllowed(4)).isFalse();
    }

    @Test
    void getRuleCountShouldReturnCorrectCount() {
        BunkerPolicy policy = BunkerPolicy.builder()
                .name("test")
                .rule(PolicyRule.allowMethod("sign_event"))
                .rule(PolicyRule.allowMethod("encrypt"))
                .rule(PolicyRule.denyEventKind(4))
                .build();

        assertThat(policy.getRuleCount()).isEqualTo(3);
    }

    @Test
    void getRuleCountShouldReturnZeroWhenNoRules() {
        BunkerPolicy policy = BunkerPolicy.builder()
                .name("test")
                .build();

        assertThat(policy.getRuleCount()).isZero();
    }

    @Test
    void toBuilderShouldCreateCopy() {
        BunkerPolicy original = BunkerPolicy.builder()
                .name("original")
                .description("desc")
                .active(true)
                .build();

        BunkerPolicy copy = original.toBuilder()
                .name("copy")
                .build();

        assertThat(copy.getName()).isEqualTo("copy");
        assertThat(copy.getDescription()).isEqualTo("desc");
        assertThat(original.getName()).isEqualTo("original");
    }

    @Test
    void equalsShouldWorkCorrectly() {
        BunkerPolicy policy1 = BunkerPolicy.builder()
                .name("test")
                .description("desc")
                .build();

        BunkerPolicy policy2 = BunkerPolicy.builder()
                .name("test")
                .description("desc")
                .build();

        assertThat(policy1).isEqualTo(policy2);
        assertThat(policy1.hashCode()).isEqualTo(policy2.hashCode());
    }

    @Test
    void shouldDeserializeFromJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());

        String json = """
            {
                "id": "policy-123",
                "name": "test-policy",
                "description": "Test",
                "active": true,
                "rules": [
                    {"type": "ALLOW", "method": "sign_event"}
                ]
            }
            """;

        BunkerPolicy policy = mapper.readValue(json, BunkerPolicy.class);

        assertThat(policy.getId()).isEqualTo("policy-123");
        assertThat(policy.getName()).isEqualTo("test-policy");
        assertThat(policy.isActive()).isTrue();
        assertThat(policy.getRules()).hasSize(1);
    }
}
