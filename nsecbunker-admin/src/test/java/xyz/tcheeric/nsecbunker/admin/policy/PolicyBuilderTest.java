package xyz.tcheeric.nsecbunker.admin.policy;

import org.junit.jupiter.api.Test;
import xyz.tcheeric.nsecbunker.core.model.BunkerPolicy;
import xyz.tcheeric.nsecbunker.core.model.PolicyRule;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyBuilderTest {

    /**
     * Ensures the builder composes a policy with rules, description, and expiration.
     */
    @Test
    void shouldBuildPolicyWithRulesAndMetadata() {
        // Arrange & Act
        BunkerPolicy policy = PolicyBuilder.withName("wallet-policy")
                .description("allow limited signing")
                .allowMethod("sign_event")
                .allowEventKindWithUsage(1, 5)
                .denyEventKind(4)
                .expiresIn(Duration.ofDays(7))
                .build();

        // Assert
        assertThat(policy.getName()).isEqualTo("wallet-policy");
        assertThat(policy.getDescription()).isEqualTo("allow limited signing");
        assertThat(policy.getRules()).hasSize(3);
        assertThat(policy.isActive()).isTrue();
        assertThat(policy.getExpiresAt()).isAfter(Instant.now().minusSeconds(1));
    }

    /**
     * Ensures the predefined templates generate expected rule sets.
     */
    @Test
    void shouldCreateTemplates() {
        // Arrange & Act
        BunkerPolicy full = PolicyTemplates.fullAccess("full");
        BunkerPolicy readOnly = PolicyTemplates.readOnly("readonly");
        BunkerPolicy expiring = PolicyTemplates.fullAccessExpiring("temp", Duration.ofHours(1));

        // Assert
        assertThat(full.getRules()).singleElement()
                .extracting(PolicyRule::isAllow)
                .isEqualTo(true);

        List<PolicyRule> readOnlyRules = readOnly.getRules();
        assertThat(readOnlyRules).extracting(PolicyRule::getMethod)
                .contains("get_public_key", "ping", "sign_event");
        assertThat(readOnlyRules).filteredOn(r -> "sign_event".equals(r.getMethod()))
                .first()
                .extracting(PolicyRule::isDeny)
                .isEqualTo(true);

        assertThat(expiring.getExpiresAt()).isAfter(Instant.now());
    }
}
