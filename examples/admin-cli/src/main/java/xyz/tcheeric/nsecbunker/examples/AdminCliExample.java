package xyz.tcheeric.nsecbunker.examples;

import xyz.tcheeric.nsecbunker.admin.NsecBunkerAdminClient;

import java.util.List;

public final class AdminCliExample {
    public static void main(String[] args) {
        String bunkerPubkey = getenv("BUNKER_PUBKEY");
        String adminPrivkey = getenv("ADMIN_PRIVKEY");
        String relay = getenv("RELAY_URL");

        String command = args.length > 0 ? args[0] : "list-keys";

        try (NsecBunkerAdminClient client = NsecBunkerAdminClient.builder()
                .bunkerPubkey(bunkerPubkey)
                .adminPrivateKey(adminPrivkey)
                .relay(relay)
                .build()) {

            if ("list-keys".equals(command)) {
                client.connect();
                client.keyManager().listKeys().join().forEach(key ->
                        System.out.println(key.getName() + " " + key.getPublicKey()));
            } else {
                System.err.println("Unknown command: " + command);
            }
        }
    }

    private static String getenv(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing env var: " + key);
        }
        return value;
    }
}
