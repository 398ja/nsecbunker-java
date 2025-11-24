/**
 * Exception hierarchy for nsecBunker Java client library.
 *
 * <p>This package contains the exception types used throughout
 * the nsecbunker-java library:
 *
 * <ul>
 *   <li>{@link xyz.tcheeric.nsecbunker.core.exception.BunkerException} - Base exception class</li>
 *   <li>{@link xyz.tcheeric.nsecbunker.core.exception.BunkerConnectionException} - Connection-related errors</li>
 *   <li>{@link xyz.tcheeric.nsecbunker.core.exception.BunkerAuthenticationException} - Authentication failures</li>
 *   <li>{@link xyz.tcheeric.nsecbunker.core.exception.BunkerAuthorizationException} - Permission denied errors</li>
 *   <li>{@link xyz.tcheeric.nsecbunker.core.exception.BunkerProtocolException} - NIP-46 protocol errors</li>
 *   <li>{@link xyz.tcheeric.nsecbunker.core.exception.BunkerTimeoutException} - Timeout errors</li>
 *   <li>{@link xyz.tcheeric.nsecbunker.core.exception.BunkerKeyException} - Key management errors</li>
 * </ul>
 *
 * <p>The exception hierarchy allows for catching specific error types
 * or catching the base {@link xyz.tcheeric.nsecbunker.core.exception.BunkerException}
 * to handle all library exceptions.
 *
 * @since 0.1.0
 */
package xyz.tcheeric.nsecbunker.core.exception;
