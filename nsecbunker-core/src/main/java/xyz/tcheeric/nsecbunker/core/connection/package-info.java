/**
 * Connection management utilities for nsecBunker Java client library.
 *
 * <p>This package contains classes for handling bunker connections:
 *
 * <ul>
 *   <li>{@link xyz.tcheeric.nsecbunker.core.connection.BunkerConnectionString} -
 *       Parser and builder for bunker:// connection strings</li>
 * </ul>
 *
 * <p>Connection strings follow the format:
 * <pre>
 * bunker://&lt;remote-pubkey&gt;?relay=&lt;relay-url&gt;&amp;secret=&lt;secret&gt;
 * </pre>
 *
 * @since 0.1.0
 */
package xyz.tcheeric.nsecbunker.core.connection;
