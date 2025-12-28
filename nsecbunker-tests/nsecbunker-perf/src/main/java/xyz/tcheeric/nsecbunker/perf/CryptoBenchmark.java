package xyz.tcheeric.nsecbunker.perf;

import nostr.crypto.schnorr.Schnorr;
import nostr.id.Identity;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * JMH microbenchmarks for cryptographic operations.
 *
 * <p>Benchmarks include:
 * <ul>
 *   <li>Schnorr signing (per-op latency and throughput)</li>
 *   <li>Schnorr verification</li>
 *   <li>SHA-256 hashing (event ID generation)</li>
 *   <li>Key generation</li>
 * </ul>
 *
 * <p>Run with: {@code java -jar benchmarks.jar CryptoBenchmark}
 */
@BenchmarkMode({Mode.Throughput, Mode.AverageTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 1, jvmArgs = {"-Xms512m", "-Xmx512m"})
public class CryptoBenchmark {

    private Identity identity;
    private byte[] messageHash;
    private byte[] signature;
    private byte[] publicKey;
    private byte[] auxRand;

    @Setup(Level.Trial)
    public void setUp() throws Exception {
        // Generate a test identity
        identity = Identity.generateRandomIdentity();

        // Pre-compute a message hash (simulating event ID)
        String message = "Test message for signing benchmark - " + System.currentTimeMillis();
        messageHash = sha256(message.getBytes(StandardCharsets.UTF_8));

        // Generate auxiliary random data for BIP-340 Schnorr signing
        auxRand = new byte[32];
        new SecureRandom().nextBytes(auxRand);

        // Pre-compute a signature for verification benchmarks
        signature = Schnorr.sign(messageHash, identity.getPrivateKey().getRawData(), auxRand);
        publicKey = identity.getPublicKey().getRawData();
    }

    @Benchmark
    public void signMessage(Blackhole blackhole) throws Exception {
        byte[] sig = Schnorr.sign(messageHash, identity.getPrivateKey().getRawData(), auxRand);
        blackhole.consume(sig);
    }

    @Benchmark
    public void verifySignature(Blackhole blackhole) throws Exception {
        boolean valid = Schnorr.verify(messageHash, publicKey, signature);
        blackhole.consume(valid);
    }

    @Benchmark
    public void signAndVerify(Blackhole blackhole) throws Exception {
        byte[] sig = Schnorr.sign(messageHash, identity.getPrivateKey().getRawData(), auxRand);
        boolean valid = Schnorr.verify(messageHash, publicKey, sig);
        blackhole.consume(valid);
    }

    @Benchmark
    public void generateKeyPair(Blackhole blackhole) {
        Identity newIdentity = Identity.generateRandomIdentity();
        blackhole.consume(newIdentity);
    }

    @Benchmark
    public void sha256Hash(Blackhole blackhole) throws NoSuchAlgorithmException {
        byte[] hash = sha256(messageHash);
        blackhole.consume(hash);
    }

    private static byte[] sha256(byte[] input) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(input);
    }
}
