package xyz.tcheeric.nsecbunker.admin.policy;

import xyz.tcheeric.nsecbunker.core.model.BunkerPolicy;
import xyz.tcheeric.nsecbunker.core.model.PolicyRule;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Fluent builder for {@link BunkerPolicy} definitions.
 *
 * <p>Provides convenience methods for adding allow/deny rules and setting
 * metadata such as expiration and description.</p>
 */
public final class PolicyBuilder {

    private final String name;
    private String description;
    private Instant expiresAt;
    private boolean active = true;
    private final List<PolicyRule> rules = new ArrayList<>();

    private PolicyBuilder(String name) {
        this.name = Objects.requireNonNull(name, "name must not be null");
    }

    /**
     * Creates a builder with the required policy name.
     *
     * @param name the policy name
     * @return a new builder
     */
    public static PolicyBuilder withName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Policy name must not be blank");
        }
        return new PolicyBuilder(name);
    }

    /**
     * Adds a custom rule.
     *
     * @param rule the rule to add
     * @return this builder
     */
    public PolicyBuilder addRule(PolicyRule rule) {
        if (rule != null) {
            rules.add(rule);
        }
        return this;
    }

    /**
     * Allows a NIP-46 method.
     *
     * @param method the method to allow
     * @return this builder
     */
    public PolicyBuilder allowMethod(String method) {
        return addRule(PolicyRule.allowMethod(method));
    }

    /**
     * Denies a NIP-46 method.
     *
     * @param method the method to deny
     * @return this builder
     */
    public PolicyBuilder denyMethod(String method) {
        return addRule(PolicyRule.denyMethod(method));
    }

    /**
     * Allows an event kind.
     *
     * @param eventKind the event kind to allow
     * @return this builder
     */
    public PolicyBuilder allowEventKind(int eventKind) {
        return addRule(PolicyRule.allowEventKind(eventKind));
    }

    /**
     * Denies an event kind.
     *
     * @param eventKind the event kind to deny
     * @return this builder
     */
    public PolicyBuilder denyEventKind(int eventKind) {
        return addRule(PolicyRule.denyEventKind(eventKind));
    }

    /**
     * Allows a method with a max usage limit.
     *
     * @param method   the method to allow
     * @param maxUsage the usage limit
     * @return this builder
     */
    public PolicyBuilder allowMethodWithUsage(String method, long maxUsage) {
        PolicyRule rule = PolicyRule.builder()
                .type(PolicyRule.RuleType.ALLOW)
                .method(method)
                .maxUsage(maxUsage)
                .build();
        return addRule(rule);
    }

    /**
     * Allows an event kind with a max usage limit.
     *
     * @param eventKind the event kind
     * @param maxUsage  the usage limit
     * @return this builder
     */
    public PolicyBuilder allowEventKindWithUsage(int eventKind, long maxUsage) {
        PolicyRule rule = PolicyRule.builder()
                .type(PolicyRule.RuleType.ALLOW)
                .eventKind(eventKind)
                .maxUsage(maxUsage)
                .build();
        return addRule(rule);
    }

    /**
     * Marks the policy as inactive.
     *
     * @return this builder
     */
    public PolicyBuilder inactive() {
        this.active = false;
        return this;
    }

    /**
     * Sets the description.
     *
     * @param description description text
     * @return this builder
     */
    public PolicyBuilder description(String description) {
        this.description = description;
        return this;
    }

    /**
     * Sets the expiration instant.
     *
     * @param expiresAt the expiration time
     * @return this builder
     */
    public PolicyBuilder expiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
        return this;
    }

    /**
     * Sets the expiration using a duration from now.
     *
     * @param duration the lifetime duration
     * @return this builder
     */
    public PolicyBuilder expiresIn(Duration duration) {
        if (duration != null) {
            this.expiresAt = Instant.now().plus(duration);
        }
        return this;
    }

    /**
     * Builds the {@link BunkerPolicy}.
     *
     * @return the policy
     */
    public BunkerPolicy build() {
        return BunkerPolicy.builder()
                .name(name)
                .description(description)
                .expiresAt(expiresAt)
                .active(active)
                .rules(rules.isEmpty() ? Collections.emptyList() : List.copyOf(rules))
                .build();
    }
}
