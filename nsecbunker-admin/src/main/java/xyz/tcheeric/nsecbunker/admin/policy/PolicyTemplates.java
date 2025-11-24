package xyz.tcheeric.nsecbunker.admin.policy;

import xyz.tcheeric.nsecbunker.core.model.BunkerPolicy;
import xyz.tcheeric.nsecbunker.core.model.PolicyRule;

import java.time.Duration;

/**
 * Pre-defined policy templates for common admin scenarios.
 */
public final class PolicyTemplates {

    private PolicyTemplates() {
        // Utility class
    }

    /**
     * Full access policy that allows all methods and event kinds.
     *
     * @param name policy name
     * @return a full-access policy
     */
    public static BunkerPolicy fullAccess(String name) {
        PolicyRule allowAll = PolicyRule.builder()
                .type(PolicyRule.RuleType.ALLOW)
                .build();
        return PolicyBuilder.withName(name)
                .addRule(allowAll)
                .description("Allows all operations")
                .build();
    }

    /**
     * Read-only policy that permits metadata retrieval but blocks signing.
     *
     * @param name policy name
     * @return a read-only policy
     */
    public static BunkerPolicy readOnly(String name) {
        return PolicyBuilder.withName(name)
                .allowMethod("get_public_key")
                .allowMethod("ping")
                .denyMethod("sign_event")
                .description("Read-only: metadata only, no signing")
                .build();
    }

    /**
     * Time-bound full access policy that expires after the given duration.
     *
     * @param name     policy name
     * @param lifetime duration before expiration
     * @return a full-access policy with expiration
     */
    public static BunkerPolicy fullAccessExpiring(String name, Duration lifetime) {
        return PolicyBuilder.withName(name)
                .addRule(PolicyRule.builder()
                        .type(PolicyRule.RuleType.ALLOW)
                        .build())
                .expiresIn(lifetime)
                .description("Full access with expiration")
                .build();
    }
}
