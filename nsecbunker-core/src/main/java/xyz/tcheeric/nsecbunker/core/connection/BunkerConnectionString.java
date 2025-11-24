package xyz.tcheeric.nsecbunker.core.connection;

import xyz.tcheeric.nsecbunker.core.model.BunkerConnection;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Parser and builder for nsecBunker connection strings.
 *
 * <p>Connection strings follow the format:
 * <pre>
 * bunker://&lt;remote-pubkey&gt;?relay=&lt;relay-url&gt;&amp;relay=&lt;relay-url&gt;&amp;secret=&lt;secret&gt;
 * </pre>
 *
 * <p>Examples:
 * <ul>
 *   <li>{@code bunker://npub1abc...?relay=wss://relay.example.com}</li>
 *   <li>{@code bunker://abc123...?relay=wss://relay1.com&relay=wss://relay2.com&secret=mysecret}</li>
 * </ul>
 *
 * <p>The parser supports both npub (bech32) and hex format for public keys.
 *
 * @see BunkerConnection
 */
public final class BunkerConnectionString {

    /**
     * The URI scheme for bunker connections.
     */
    public static final String SCHEME = "bunker";

    /**
     * Query parameter name for relay URLs.
     */
    public static final String PARAM_RELAY = "relay";

    /**
     * Query parameter name for the secret.
     */
    public static final String PARAM_SECRET = "secret";

    /**
     * Pattern for validating hex public keys (64 characters).
     */
    private static final Pattern HEX_PUBKEY_PATTERN = Pattern.compile("^[0-9a-fA-F]{64}$");

    /**
     * Pattern for validating npub public keys.
     */
    private static final Pattern NPUB_PATTERN = Pattern.compile("^npub1[a-z0-9]{58}$");

    /**
     * Pattern for validating WebSocket relay URLs.
     */
    private static final Pattern RELAY_URL_PATTERN = Pattern.compile("^wss?://[^\\s]+$");

    private BunkerConnectionString() {
        // Utility class
    }

    /**
     * Parses a bunker connection string into a {@link BunkerConnection}.
     *
     * @param connectionString the connection string to parse
     * @return the parsed BunkerConnection
     * @throws IllegalArgumentException if the connection string is invalid
     */
    public static BunkerConnection parse(String connectionString) {
        Objects.requireNonNull(connectionString, "Connection string must not be null");

        String trimmed = connectionString.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Connection string must not be empty");
        }

        URI uri;
        try {
            uri = new URI(trimmed);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid connection string URI: " + e.getMessage(), e);
        }

        // Validate scheme
        String scheme = uri.getScheme();
        if (scheme == null || !scheme.equalsIgnoreCase(SCHEME)) {
            throw new IllegalArgumentException(
                    "Invalid scheme: expected '" + SCHEME + "' but got '" + scheme + "'"
            );
        }

        // Extract pubkey from host/authority
        String pubkey = extractPubkey(uri);
        if (pubkey == null || pubkey.isEmpty()) {
            throw new IllegalArgumentException("Missing public key in connection string");
        }

        // Validate pubkey format
        if (!isValidPubkey(pubkey)) {
            throw new IllegalArgumentException(
                    "Invalid public key format: must be 64-char hex or npub1..."
            );
        }

        // Parse query parameters
        Map<String, List<String>> params = parseQueryParams(uri.getRawQuery());

        // Extract relays
        List<String> relays = params.getOrDefault(PARAM_RELAY, Collections.emptyList());
        List<String> validatedRelays = new ArrayList<>();
        for (String relay : relays) {
            String decoded = urlDecode(relay);
            if (!isValidRelayUrl(decoded)) {
                throw new IllegalArgumentException("Invalid relay URL: " + decoded);
            }
            validatedRelays.add(decoded);
        }

        // Extract secret
        List<String> secrets = params.getOrDefault(PARAM_SECRET, Collections.emptyList());
        String secret = secrets.isEmpty() ? null : urlDecode(secrets.get(0));

        return BunkerConnection.builder()
                .remotePubkey(pubkey)
                .relays(validatedRelays)
                .secret(secret)
                .build();
    }

    /**
     * Attempts to parse a connection string, returning null if invalid.
     *
     * @param connectionString the connection string to parse
     * @return the parsed BunkerConnection, or null if invalid
     */
    public static BunkerConnection tryParse(String connectionString) {
        try {
            return parse(connectionString);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Checks if a string is a valid bunker connection string.
     *
     * @param connectionString the string to check
     * @return true if the string is a valid connection string
     */
    public static boolean isValid(String connectionString) {
        return tryParse(connectionString) != null;
    }

    /**
     * Builds a connection string from components.
     *
     * @param pubkey the remote public key (hex or npub)
     * @param relays the relay URLs
     * @param secret the optional secret
     * @return the formatted connection string
     */
    public static String build(String pubkey, List<String> relays, String secret) {
        Objects.requireNonNull(pubkey, "Public key must not be null");

        if (!isValidPubkey(pubkey)) {
            throw new IllegalArgumentException("Invalid public key format");
        }

        StringBuilder sb = new StringBuilder();
        sb.append(SCHEME).append("://").append(pubkey);

        boolean hasParams = false;

        if (relays != null && !relays.isEmpty()) {
            for (String relay : relays) {
                if (!isValidRelayUrl(relay)) {
                    throw new IllegalArgumentException("Invalid relay URL: " + relay);
                }
                sb.append(hasParams ? "&" : "?");
                sb.append(PARAM_RELAY).append("=").append(urlEncode(relay));
                hasParams = true;
            }
        }

        if (secret != null && !secret.isEmpty()) {
            sb.append(hasParams ? "&" : "?");
            sb.append(PARAM_SECRET).append("=").append(urlEncode(secret));
        }

        return sb.toString();
    }

    /**
     * Builds a connection string from a {@link BunkerConnection}.
     *
     * @param connection the connection to convert
     * @return the formatted connection string
     */
    public static String build(BunkerConnection connection) {
        Objects.requireNonNull(connection, "Connection must not be null");
        return build(
                connection.getRemotePubkey(),
                connection.getRelays(),
                connection.getSecret()
        );
    }

    /**
     * Checks if a public key is valid (hex or npub format).
     *
     * @param pubkey the public key to validate
     * @return true if the public key is valid
     */
    public static boolean isValidPubkey(String pubkey) {
        if (pubkey == null || pubkey.isEmpty()) {
            return false;
        }
        return HEX_PUBKEY_PATTERN.matcher(pubkey).matches()
                || NPUB_PATTERN.matcher(pubkey).matches();
    }

    /**
     * Checks if a URL is a valid WebSocket relay URL.
     *
     * @param url the URL to validate
     * @return true if the URL is a valid relay URL
     */
    public static boolean isValidRelayUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        return RELAY_URL_PATTERN.matcher(url).matches();
    }

    /**
     * Extracts the public key from the URI.
     */
    private static String extractPubkey(URI uri) {
        // The pubkey can be in the host or authority part
        String host = uri.getHost();
        if (host != null && !host.isEmpty()) {
            return host;
        }

        // Try authority (for URIs without proper host parsing)
        String authority = uri.getAuthority();
        if (authority != null && !authority.isEmpty()) {
            // Remove any user info or port
            int atIndex = authority.indexOf('@');
            if (atIndex >= 0) {
                authority = authority.substring(atIndex + 1);
            }
            int colonIndex = authority.indexOf(':');
            if (colonIndex >= 0) {
                authority = authority.substring(0, colonIndex);
            }
            return authority;
        }

        // Try scheme-specific part
        String ssp = uri.getSchemeSpecificPart();
        if (ssp != null && ssp.startsWith("//")) {
            String remaining = ssp.substring(2);
            int queryIndex = remaining.indexOf('?');
            if (queryIndex >= 0) {
                remaining = remaining.substring(0, queryIndex);
            }
            return remaining;
        }

        return null;
    }

    /**
     * Parses query parameters from a raw query string.
     */
    private static Map<String, List<String>> parseQueryParams(String rawQuery) {
        Map<String, List<String>> params = new LinkedHashMap<>();

        if (rawQuery == null || rawQuery.isEmpty()) {
            return params;
        }

        String[] pairs = rawQuery.split("&");
        for (String pair : pairs) {
            int eqIndex = pair.indexOf('=');
            String key;
            String value;

            if (eqIndex >= 0) {
                key = pair.substring(0, eqIndex);
                value = pair.substring(eqIndex + 1);
            } else {
                key = pair;
                value = "";
            }

            params.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        }

        return params;
    }

    /**
     * URL-decodes a string.
     */
    private static String urlDecode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            // UTF-8 is always supported
            return value;
        }
    }

    /**
     * URL-encodes a string.
     */
    private static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            // UTF-8 is always supported
            return value;
        }
    }
}
