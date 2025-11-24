/**
 * Relay connection management for nsecBunker Java client library.
 *
 * <p>This package provides WebSocket-based connections to Nostr relays:
 *
 * <ul>
 *   <li>{@link xyz.tcheeric.nsecbunker.connection.RelayConnection} -
 *       WebSocket connection to a single relay</li>
 *   <li>{@link xyz.tcheeric.nsecbunker.connection.RelayPool} -
 *       Manages multiple relay connections with broadcasting and deduplication</li>
 *   <li>{@link xyz.tcheeric.nsecbunker.connection.RelayListener} -
 *       Listener interface for single relay events</li>
 *   <li>{@link xyz.tcheeric.nsecbunker.connection.RelayPoolListener} -
 *       Listener interface for pool-level events</li>
 *   <li>{@link xyz.tcheeric.nsecbunker.connection.ConnectionState} -
 *       Enumeration of connection states</li>
 * </ul>
 *
 * <p>Single relay example:
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
 * <p>Relay pool example:
 * <pre>{@code
 * RelayPool pool = RelayPool.builder()
 *     .relay("wss://relay1.example.com")
 *     .relay("wss://relay2.example.com")
 *     .deduplicateEvents(true)
 *     .build();
 * pool.connectAll();
 * pool.broadcastReq("sub1", "{\"kinds\":[1],\"limit\":10}");
 * }</pre>
 *
 * @since 0.1.0
 */
package xyz.tcheeric.nsecbunker.connection;
