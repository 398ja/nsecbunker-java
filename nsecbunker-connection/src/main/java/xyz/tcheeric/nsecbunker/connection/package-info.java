/**
 * Relay connection management for nsecBunker Java client library.
 *
 * <p>This package provides WebSocket-based connections to Nostr relays:
 *
 * <ul>
 *   <li>{@link xyz.tcheeric.nsecbunker.connection.RelayConnection} -
 *       WebSocket connection to a single relay</li>
 *   <li>{@link xyz.tcheeric.nsecbunker.connection.RelayListener} -
 *       Listener interface for connection events</li>
 *   <li>{@link xyz.tcheeric.nsecbunker.connection.ConnectionState} -
 *       Enumeration of connection states</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * RelayConnection relay = new RelayConnection("wss://relay.example.com");
 * relay.addListener(new RelayListener() {
 *     @Override
 *     public void onEvent(RelayConnection relay, String subId, GenericEvent event) {
 *         System.out.println("Event: " + event.getId());
 *     }
 * });
 * relay.connect();
 * relay.sendReq("sub1", "{\"kinds\":[1],\"limit\":10}");
 * }</pre>
 *
 * @since 0.1.0
 */
package xyz.tcheeric.nsecbunker.connection;
