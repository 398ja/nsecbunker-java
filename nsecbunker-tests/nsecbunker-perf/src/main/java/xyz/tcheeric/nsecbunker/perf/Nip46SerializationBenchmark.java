package xyz.tcheeric.nsecbunker.perf;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Request;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Response;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * JMH microbenchmarks for NIP-46 JSON serialization/deserialization.
 *
 * <p>Benchmarks include:
 * <ul>
 *   <li>Nip46Request encoding (to JSON)</li>
 *   <li>Nip46Request decoding (from JSON)</li>
 *   <li>Nip46Response encoding</li>
 *   <li>Nip46Response decoding</li>
 *   <li>Round-trip serialization</li>
 * </ul>
 *
 * <p>Run with: {@code java -jar benchmarks.jar Nip46SerializationBenchmark}
 */
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 1, jvmArgs = {"-Xms512m", "-Xmx512m"})
public class Nip46SerializationBenchmark {

    private ObjectMapper objectMapper;
    private Nip46Request request;
    private Nip46Response response;
    private String requestJson;
    private String responseJson;
    private String signEventJson;

    @Setup(Level.Trial)
    public void setUp() throws JsonProcessingException {
        objectMapper = new ObjectMapper();

        // Create sample request
        request = Nip46Request.builder()
                .id(UUID.randomUUID().toString())
                .method("sign_event")
                .params(List.of("{\"kind\":1,\"content\":\"Hello world\",\"tags\":[],\"created_at\":1234567890}"))
                .build();

        // Create sample response
        response = Nip46Response.builder()
                .id(request.getId())
                .result("{\"id\":\"abc123\",\"sig\":\"def456\",\"kind\":1,\"pubkey\":\"xyz789\",\"content\":\"Hello world\",\"tags\":[],\"created_at\":1234567890}")
                .build();

        // Pre-serialize for decode benchmarks
        requestJson = objectMapper.writeValueAsString(request);
        responseJson = objectMapper.writeValueAsString(response);

        // Complex sign_event request
        signEventJson = """
            {"id":"a1b2c3d4-e5f6-7890-abcd-ef1234567890","method":"sign_event","params":["{\\"kind\\":1,\\"content\\":\\"This is a longer test message with some unicode \\\\u2764 and special characters!\\",\\"tags\\":[[\\"p\\",\\"abc123\\"],[\\"e\\",\\"def456\\"]],\\"created_at\\":1700000000}"]}
            """.strip();
    }

    @Benchmark
    public void encodeRequest(Blackhole blackhole) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(request);
        blackhole.consume(json);
    }

    @Benchmark
    public void decodeRequest(Blackhole blackhole) throws JsonProcessingException {
        Nip46Request req = objectMapper.readValue(requestJson, Nip46Request.class);
        blackhole.consume(req);
    }

    @Benchmark
    public void encodeResponse(Blackhole blackhole) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(response);
        blackhole.consume(json);
    }

    @Benchmark
    public void decodeResponse(Blackhole blackhole) throws JsonProcessingException {
        Nip46Response resp = objectMapper.readValue(responseJson, Nip46Response.class);
        blackhole.consume(resp);
    }

    @Benchmark
    public void roundTripRequest(Blackhole blackhole) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(request);
        Nip46Request decoded = objectMapper.readValue(json, Nip46Request.class);
        blackhole.consume(decoded);
    }

    @Benchmark
    public void decodeComplexSignEvent(Blackhole blackhole) throws JsonProcessingException {
        Nip46Request req = objectMapper.readValue(signEventJson, Nip46Request.class);
        blackhole.consume(req);
    }
}
