package xyz.tcheeric.nsecbunker.examples.springboot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot demo application for nsecBunker integration.
 *
 * <p>This application demonstrates how to use the nsecbunker-spring-boot-starter
 * to build a Nostr signing service with:
 * <ul>
 *   <li>Auto-configured NsecBunkerSigner bean</li>
 *   <li>REST API for signing operations</li>
 *   <li>Health checks via Spring Boot Actuator</li>
 *   <li>Metrics for signing operations via Micrometer</li>
 * </ul>
 *
 * <h2>Running the Application</h2>
 * <pre>
 * # Set environment variables
 * export BUNKER_PUBKEY=your-bunker-pubkey
 * export CLIENT_PRIVKEY=your-client-private-key
 * export RELAY_URL=wss://your-relay.example.com
 *
 * # Run with Maven
 * mvn spring-boot:run
 *
 * # Or run the JAR
 * java -jar target/spring-boot-app.jar
 * </pre>
 *
 * <h2>Available Endpoints</h2>
 * <ul>
 *   <li>POST /api/sign - Sign a Nostr event</li>
 *   <li>GET /api/pubkey - Get the signing public key</li>
 *   <li>POST /api/encrypt - Encrypt a message (NIP-44)</li>
 *   <li>POST /api/decrypt - Decrypt a message (NIP-44)</li>
 *   <li>GET /actuator/health - Health check</li>
 *   <li>GET /actuator/info - Application info</li>
 *   <li>GET /actuator/metrics - Metrics</li>
 * </ul>
 */
@SpringBootApplication
public class NsecBunkerDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(NsecBunkerDemoApplication.class, args);
    }
}
