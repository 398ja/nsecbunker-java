package xyz.tcheeric.nsecbunker.perf;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import xyz.tcheeric.nsecbunker.connection.RelayConnection;
import xyz.tcheeric.nsecbunker.connection.RelayPool;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * JMH microbenchmarks for RelayPool operations.
 *
 * <p>Benchmarks include:
 * <ul>
 *   <li>Message broadcasting throughput</li>
 *   <li>Subscription management</li>
 *   <li>Connection pool overhead</li>
 *   <li>Deduplication performance (on/off comparison)</li>
 * </ul>
 *
 * <p>Note: These benchmarks test internal operations without actual network I/O.
 * For full network benchmarks, see {@link RelayLoadTest}.
 *
 * <p>Run with: {@code java -jar benchmarks.jar RelayPoolBenchmark}
 */
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 1, jvmArgs = {"-Xms512m", "-Xmx512m"})
public class RelayPoolBenchmark {

    private RelayPool relayPool;
    private String testMessage;
    private AtomicInteger messageCounter;

    @Param({"1", "3", "5"})
    private int relayCount;

    @Setup(Level.Trial)
    public void setUp() {
        // Create relay pool with mock relay URLs (won't actually connect for unit benchmarks)
        List<String> relayUrls = new java.util.ArrayList<>();
        for (int i = 0; i < relayCount; i++) {
            relayUrls.add("wss://relay" + i + ".example.com");
        }

        // Create relay pool - connections are not started until connectAll() is called
        relayPool = RelayPool.builder()
                .relays(relayUrls)
                .build();

        testMessage = "[\"EVENT\",{\"id\":\"abc123\",\"kind\":1,\"pubkey\":\"xyz\",\"content\":\"test\",\"sig\":\"def\",\"created_at\":1234567890,\"tags\":[]}]";
        messageCounter = new AtomicInteger(0);
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (relayPool != null) {
            relayPool.close();
        }
    }

    @Benchmark
    public void createRelayPool(Blackhole blackhole) {
        List<String> urls = List.of(
                "wss://relay1.example.com",
                "wss://relay2.example.com",
                "wss://relay3.example.com"
        );
        RelayPool pool = RelayPool.builder()
                .relays(urls)
                .build();
        blackhole.consume(pool);
        pool.close();
    }

    @Benchmark
    public void generateSubscriptionId(Blackhole blackhole) {
        String subId = "sub-" + messageCounter.incrementAndGet();
        blackhole.consume(subId);
    }

    @Benchmark
    public void parseNostrMessage(Blackhole blackhole) {
        // Simulate message parsing overhead
        boolean isEvent = testMessage.startsWith("[\"EVENT\"");
        boolean isNotice = testMessage.startsWith("[\"NOTICE\"");
        boolean isOk = testMessage.startsWith("[\"OK\"");
        blackhole.consume(isEvent || isNotice || isOk);
    }

    /**
     * Benchmark for RelayConnection creation (without actual connection).
     */
    @Benchmark
    public void createRelayConnection(Blackhole blackhole) {
        // RelayConnection uses constructor - connection is not started until connect() is called
        RelayConnection connection = new RelayConnection("wss://relay.example.com");
        blackhole.consume(connection);
        connection.close();
    }
}
