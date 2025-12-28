# nsecBunker Performance Tests

JMH microbenchmarks and performance tests for nsecbunker-java.

## Overview

This module contains performance benchmarks using [JMH (Java Microbenchmark Harness)](https://openjdk.java.net/projects/code-tools/jmh/) to measure:

- **Cryptographic operations**: Schnorr signing/verification, SHA-256 hashing, key generation
- **JSON serialization**: NIP-46 request/response encoding/decoding
- **RelayPool operations**: Connection management, message broadcasting overhead
- **End-to-end latency**: Admin/signer round-trip against Testcontainers relays

## Running Benchmarks

### Build the benchmark JAR

```bash
cd nsecbunker-java
mvn clean package -pl nsecbunker-tests/nsecbunker-perf -am -DskipTests
```

### Run all benchmarks

```bash
java -jar nsecbunker-tests/nsecbunker-perf/target/benchmarks.jar
```

### Run specific benchmark

```bash
# Run only crypto benchmarks
java -jar nsecbunker-tests/nsecbunker-perf/target/benchmarks.jar CryptoBenchmark

# Run only serialization benchmarks
java -jar nsecbunker-tests/nsecbunker-perf/target/benchmarks.jar Nip46SerializationBenchmark

# Run only RelayPool benchmarks
java -jar nsecbunker-tests/nsecbunker-perf/target/benchmarks.jar RelayPoolBenchmark
```

### Configure benchmark parameters

```bash
# More iterations for statistical accuracy
java -jar benchmarks.jar -wi 5 -i 10 -f 3

# Output results to JSON
java -jar benchmarks.jar -rf json -rff results.json

# Profile with async-profiler
java -jar benchmarks.jar -prof async:output=flamegraph
```

## Benchmark Categories

### 1. CryptoBenchmark

Measures low-level cryptographic operations:

| Benchmark | Description |
|-----------|-------------|
| `signMessage` | Schnorr signature generation |
| `verifySignature` | Schnorr signature verification |
| `signAndVerify` | Combined sign + verify |
| `generateKeyPair` | New identity generation |
| `sha256Hash` | SHA-256 hashing (event ID) |

### 2. Nip46SerializationBenchmark

Measures JSON serialization overhead:

| Benchmark | Description |
|-----------|-------------|
| `encodeRequest` | Nip46Request → JSON |
| `decodeRequest` | JSON → Nip46Request |
| `encodeResponse` | Nip46Response → JSON |
| `decodeResponse` | JSON → Nip46Response |
| `roundTripRequest` | Encode + decode cycle |

### 3. RelayPoolBenchmark

Measures connection pool operations:

| Benchmark | Description |
|-----------|-------------|
| `createRelayPool` | Pool initialization |
| `createRelayConnection` | Single connection creation |
| `parseNostrMessage` | Message type detection |

## Interpreting Results

JMH reports:

- **Throughput**: Operations per time unit (higher is better)
- **Average time**: Mean time per operation (lower is better)
- **Sample time**: Percentile distribution of operation times

Example output:
```
Benchmark                              Mode  Cnt    Score    Error  Units
CryptoBenchmark.signMessage           thrpt    5   12345.6 ±  234.5  ops/ms
CryptoBenchmark.signMessage            avgt    5      81.2 ±    1.5  us/op
```

## Performance Targets

Recommended baselines for nsecbunker-java:

| Operation | Target p99 |
|-----------|-----------|
| Schnorr sign | < 200 μs |
| Schnorr verify | < 200 μs |
| NIP-46 encode | < 10 μs |
| NIP-46 decode | < 20 μs |
| Full sign request | < 500 μs |

## Continuous Benchmarking

Consider integrating with [JMH Gradle Plugin](https://github.com/melix/jmh-gradle-plugin) or GitHub Actions for tracking performance regressions over time.
