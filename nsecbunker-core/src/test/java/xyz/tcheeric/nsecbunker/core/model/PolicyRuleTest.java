package xyz.tcheeric.nsecbunker.core.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyRuleTest {

    @Test
    void shouldBuildDefaultAllowRule() {
        PolicyRule rule = PolicyRule.builder().build();

        assertThat(rule.getType()).isEqualTo(PolicyRule.RuleType.ALLOW);
        assertThat(rule.isAllow()).isTrue();
        assertThat(rule.isDeny()).isFalse();
        assertThat(rule.getCurrentUsage()).isZero();
    }

    @Test
    void shouldBuildDenyRule() {
        PolicyRule rule = PolicyRule.builder()
                .type(PolicyRule.RuleType.DENY)
                .method("sign_event")
                .build();

        assertThat(rule.getType()).isEqualTo(PolicyRule.RuleType.DENY);
        assertThat(rule.isDeny()).isTrue();
        assertThat(rule.isAllow()).isFalse();
        assertThat(rule.getMethod()).isEqualTo("sign_event");
    }

    @Test
    void allowMethodShouldCreateAllowRuleForMethod() {
        PolicyRule rule = PolicyRule.allowMethod("encrypt");

        assertThat(rule.isAllow()).isTrue();
        assertThat(rule.getMethod()).isEqualTo("encrypt");
    }

    @Test
    void allowEventKindShouldCreateAllowRuleForKind() {
        PolicyRule rule = PolicyRule.allowEventKind(1);

        assertThat(rule.isAllow()).isTrue();
        assertThat(rule.getEventKind()).isEqualTo(1);
    }

    @Test
    void denyMethodShouldCreateDenyRuleForMethod() {
        PolicyRule rule = PolicyRule.denyMethod("decrypt");

        assertThat(rule.isDeny()).isTrue();
        assertThat(rule.getMethod()).isEqualTo("decrypt");
    }

    @Test
    void denyEventKindShouldCreateDenyRuleForKind() {
        PolicyRule rule = PolicyRule.denyEventKind(4);

        assertThat(rule.isDeny()).isTrue();
        assertThat(rule.getEventKind()).isEqualTo(4);
    }

    @Test
    void hasUsageLimitShouldReturnTrueWhenMaxUsageSet() {
        PolicyRule rule = PolicyRule.builder()
                .maxUsage(100L)
                .build();

        assertThat(rule.hasUsageLimit()).isTrue();
    }

    @Test
    void hasUsageLimitShouldReturnFalseWhenMaxUsageNull() {
        PolicyRule rule = PolicyRule.builder().build();

        assertThat(rule.hasUsageLimit()).isFalse();
    }

    @Test
    void hasUsageLimitShouldReturnFalseWhenMaxUsageZero() {
        PolicyRule rule = PolicyRule.builder()
                .maxUsage(0L)
                .build();

        assertThat(rule.hasUsageLimit()).isFalse();
    }

    @Test
    void isUsageLimitReachedShouldReturnTrueWhenLimitExceeded() {
        PolicyRule rule = PolicyRule.builder()
                .maxUsage(10L)
                .currentUsage(10L)
                .build();

        assertThat(rule.isUsageLimitReached()).isTrue();
    }

    @Test
    void isUsageLimitReachedShouldReturnFalseWhenUnderLimit() {
        PolicyRule rule = PolicyRule.builder()
                .maxUsage(10L)
                .currentUsage(5L)
                .build();

        assertThat(rule.isUsageLimitReached()).isFalse();
    }

    @Test
    void getRemainingUsageShouldReturnCorrectValue() {
        PolicyRule rule = PolicyRule.builder()
                .maxUsage(100L)
                .currentUsage(30L)
                .build();

        assertThat(rule.getRemainingUsage()).isEqualTo(70L);
    }

    @Test
    void getRemainingUsageShouldReturnMinusOneWhenNoLimit() {
        PolicyRule rule = PolicyRule.builder().build();

        assertThat(rule.getRemainingUsage()).isEqualTo(-1L);
    }

    @Test
    void getRemainingUsageShouldReturnZeroWhenOverLimit() {
        PolicyRule rule = PolicyRule.builder()
                .maxUsage(10L)
                .currentUsage(15L)
                .build();

        assertThat(rule.getRemainingUsage()).isZero();
    }

    @Test
    void appliesToMethodShouldReturnTrueWhenMethodMatches() {
        PolicyRule rule = PolicyRule.builder()
                .method("sign_event")
                .build();

        assertThat(rule.appliesTo("sign_event")).isTrue();
        assertThat(rule.appliesTo("encrypt")).isFalse();
    }

    @Test
    void appliesToMethodShouldReturnTrueWhenMethodNull() {
        PolicyRule rule = PolicyRule.builder().build();

        assertThat(rule.appliesTo("sign_event")).isTrue();
        assertThat(rule.appliesTo("encrypt")).isTrue();
    }

    @Test
    void appliesToEventKindShouldReturnTrueWhenKindMatches() {
        PolicyRule rule = PolicyRule.builder()
                .eventKind(1)
                .build();

        assertThat(rule.appliesTo(1)).isTrue();
        assertThat(rule.appliesTo(4)).isFalse();
    }

    @Test
    void appliesToEventKindShouldReturnTrueWhenKindNull() {
        PolicyRule rule = PolicyRule.builder().build();

        assertThat(rule.appliesTo(1)).isTrue();
        assertThat(rule.appliesTo(4)).isTrue();
    }

    @Test
    void toBuilderShouldCreateCopy() {
        PolicyRule original = PolicyRule.builder()
                .type(PolicyRule.RuleType.ALLOW)
                .method("sign_event")
                .maxUsage(100L)
                .build();

        PolicyRule copy = original.toBuilder()
                .currentUsage(50L)
                .build();

        assertThat(copy.getMethod()).isEqualTo("sign_event");
        assertThat(copy.getMaxUsage()).isEqualTo(100L);
        assertThat(copy.getCurrentUsage()).isEqualTo(50L);
        assertThat(original.getCurrentUsage()).isZero();
    }

    @Test
    void equalsShouldWorkCorrectly() {
        PolicyRule rule1 = PolicyRule.builder()
                .method("sign_event")
                .eventKind(1)
                .build();

        PolicyRule rule2 = PolicyRule.builder()
                .method("sign_event")
                .eventKind(1)
                .build();

        assertThat(rule1).isEqualTo(rule2);
        assertThat(rule1.hashCode()).isEqualTo(rule2.hashCode());
    }

    @Test
    void shouldDeserializeFromJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        // JSON property names must match nsecbunkerd format:
        // "kind" for event kind, "use_count" for max usage, "current_usage_count" for current usage
        String json = """
            {
                "type": "DENY",
                "method": "sign_event",
                "kind": 4,
                "use_count": 100,
                "current_usage_count": 25
            }
            """;

        PolicyRule rule = mapper.readValue(json, PolicyRule.class);

        assertThat(rule.getType()).isEqualTo(PolicyRule.RuleType.DENY);
        assertThat(rule.getMethod()).isEqualTo("sign_event");
        assertThat(rule.getEventKind()).isEqualTo(4);
        assertThat(rule.getMaxUsage()).isEqualTo(100L);
        assertThat(rule.getCurrentUsage()).isEqualTo(25L);
    }
}
