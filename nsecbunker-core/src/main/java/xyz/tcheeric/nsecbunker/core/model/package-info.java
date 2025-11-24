/**
 * Core domain models for nsecBunker Java client library.
 *
 * <p>This package contains the fundamental data models used throughout
 * the nsecbunker-java library:
 *
 * <ul>
 *   <li>{@link xyz.tcheeric.nsecbunker.core.model.BunkerConnection} - Represents a connection to an nsecBunker instance</li>
 *   <li>{@link xyz.tcheeric.nsecbunker.core.model.BunkerKey} - Represents a key stored in nsecBunker</li>
 *   <li>{@link xyz.tcheeric.nsecbunker.core.model.BunkerPolicy} - Defines permissions for key access</li>
 *   <li>{@link xyz.tcheeric.nsecbunker.core.model.PolicyRule} - Individual rules within a policy</li>
 *   <li>{@link xyz.tcheeric.nsecbunker.core.model.KeyUser} - A user with access to a key</li>
 *   <li>{@link xyz.tcheeric.nsecbunker.core.model.SigningCondition} - Fine-grained ACL conditions</li>
 *   <li>{@link xyz.tcheeric.nsecbunker.core.model.AccessToken} - Tokens for temporary key access</li>
 * </ul>
 *
 * <p>All models in this package are immutable and use the builder pattern
 * via Lombok's {@code @Builder} annotation.
 *
 * @since 0.1.0
 */
package xyz.tcheeric.nsecbunker.core.model;
