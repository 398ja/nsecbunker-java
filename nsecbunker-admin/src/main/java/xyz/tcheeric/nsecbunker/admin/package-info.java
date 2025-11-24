/**
 * Admin client for nsecBunker key and permission management.
 *
 * <p>This package provides the admin client interface for managing
 * nsecBunker instances, including:
 *
 * <ul>
 *   <li>Key management (create, list, delete, unlock)</li>
 *   <li>Policy management (create, update, delete)</li>
 *   <li>Permission management (grant, revoke, list)</li>
 *   <li>Token management (create, revoke, list)</li>
 * </ul>
 *
 * <p>Main entry point: {@link xyz.tcheeric.nsecbunker.admin.NsecBunkerAdminClient}
 *
 * <p>Usage example:
 * <pre>{@code
 * NsecBunkerAdminClient client = NsecBunkerAdminClient.builder()
 *     .bunkerPubkey("npub1...")
 *     .adminPrivateKey("nsec1...")
 *     .relay("wss://relay.example.com")
 *     .build();
 *
 * client.connect();
 *
 * // Perform admin operations
 * String result = client.ping().join();
 * System.out.println("Ping result: " + result);
 *
 * client.close();
 * }</pre>
 *
 * @see xyz.tcheeric.nsecbunker.admin.NsecBunkerAdminClient
 * @see xyz.tcheeric.nsecbunker.admin.AdminConfig
 */
package xyz.tcheeric.nsecbunker.admin;
