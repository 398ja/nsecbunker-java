package xyz.tcheeric.nsecbunker.protocol.testing;

import okhttp3.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.nsecbunker.protocol.nip46.*;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link MockBunkerServer}.
 */
class MockBunkerServerTest {

    private MockBunkerServer bunker;
    private OkHttpClient client;
    private Nip46Encoder encoder;
    private Nip46Decoder decoder;

    @BeforeEach
    void setUp() throws Exception {
        bunker = new MockBunkerServer();
        bunker.start();
        encoder = new Nip46Encoder();
        decoder = new Nip46Decoder();
        client = new OkHttpClient.Builder()
                .readTimeout(5, TimeUnit.SECONDS)
                .build();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (client != null) {
            client.dispatcher().executorService().shutdown();
            client.connectionPool().evictAll();
        }
        if (bunker != null) {
            bunker.close();
        }
    }

    @Test
    void startCreatesBunker() {
        assertNotNull(bunker.getRelayUrl());
        assertTrue(bunker.getRelayUrl().startsWith("ws://"));
    }

    @Test
    void getConnectionStringReturnsValidFormat() {
        String connStr = bunker.getConnectionString();

        assertTrue(connStr.startsWith("bunker://"));
        assertTrue(connStr.contains(bunker.getBunkerPublicKey()));
        assertTrue(connStr.contains("relay="));
    }

    @Test
    void defaultPublicKeyIsSet() {
        assertNotNull(bunker.getBunkerPublicKey());
        assertEquals(64, bunker.getBunkerPublicKey().length());
    }

    @Test
    void canConnectToRelay() throws Exception {
        CountDownLatch connected = new CountDownLatch(1);

        Request request = new Request.Builder()
                .url(bunker.getRelayUrl())
                .build();

        client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                connected.countDown();
            }
        });

        assertTrue(connected.await(5, TimeUnit.SECONDS));
    }

    @Test
    void receivesPingRequest() throws Exception {
        CountDownLatch connected = new CountDownLatch(1);
        AtomicReference<WebSocket> wsRef = new AtomicReference<>();

        Request request = new Request.Builder()
                .url(bunker.getRelayUrl())
                .build();

        client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                wsRef.set(webSocket);
                connected.countDown();
            }
        });

        assertTrue(connected.await(5, TimeUnit.SECONDS));

        // Send a ping request as a Nostr event
        Nip46Request pingReq = Nip46Request.ping();
        String requestJson = encoder.encodeRequest(pingReq);
        String eventJson = String.format("[\"EVENT\",{\"kind\":24133,\"content\":\"%s\"}]",
                escapeJson(requestJson));
        wsRef.get().send(eventJson);

        // Wait for bunker to receive it
        assertTrue(bunker.awaitRequestCount(1, 5, TimeUnit.SECONDS));

        List<Nip46Request> requests = bunker.getReceivedRequests();
        assertEquals(1, requests.size());
        assertEquals("ping", requests.get(0).getMethod());
    }

    @Test
    void respondsWithPong() throws Exception {
        CountDownLatch connected = new CountDownLatch(1);
        CountDownLatch responseReceived = new CountDownLatch(1);
        AtomicReference<WebSocket> wsRef = new AtomicReference<>();
        AtomicReference<String> responseRef = new AtomicReference<>();

        Request request = new Request.Builder()
                .url(bunker.getRelayUrl())
                .build();

        client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                wsRef.set(webSocket);
                connected.countDown();
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                if (text.contains("EVENT") && text.contains("pong")) {
                    responseRef.set(text);
                    responseReceived.countDown();
                }
            }
        });

        assertTrue(connected.await(5, TimeUnit.SECONDS));

        // Subscribe to receive events
        wsRef.get().send("[\"REQ\",\"nip46\",{\"kinds\":[24133]}]");
        Thread.sleep(100);

        // Send ping request
        Nip46Request pingReq = Nip46Request.ping();
        String requestJson = encoder.encodeRequest(pingReq);
        String eventJson = String.format("[\"EVENT\",{\"kind\":24133,\"content\":\"%s\"}]",
                escapeJson(requestJson));
        wsRef.get().send(eventJson);

        assertTrue(responseReceived.await(5, TimeUnit.SECONDS));
        assertTrue(responseRef.get().contains("pong"));
    }

    @Test
    void customPingHandler() throws Exception {
        bunker.onPing(req -> "custom-pong");

        CountDownLatch connected = new CountDownLatch(1);
        CountDownLatch responseReceived = new CountDownLatch(1);
        AtomicReference<WebSocket> wsRef = new AtomicReference<>();
        AtomicReference<String> responseRef = new AtomicReference<>();

        Request request = new Request.Builder()
                .url(bunker.getRelayUrl())
                .build();

        client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                wsRef.set(webSocket);
                connected.countDown();
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                if (text.contains("EVENT") && text.contains("custom-pong")) {
                    responseRef.set(text);
                    responseReceived.countDown();
                }
            }
        });

        assertTrue(connected.await(5, TimeUnit.SECONDS));
        wsRef.get().send("[\"REQ\",\"nip46\",{\"kinds\":[24133]}]");
        Thread.sleep(100);

        Nip46Request pingReq = Nip46Request.ping();
        String requestJson = encoder.encodeRequest(pingReq);
        String eventJson = String.format("[\"EVENT\",{\"kind\":24133,\"content\":\"%s\"}]",
                escapeJson(requestJson));
        wsRef.get().send(eventJson);

        assertTrue(responseReceived.await(5, TimeUnit.SECONDS));
        assertTrue(responseRef.get().contains("custom-pong"));
    }

    @Test
    void getRequestsByMethod() throws Exception {
        CountDownLatch connected = new CountDownLatch(1);
        AtomicReference<WebSocket> wsRef = new AtomicReference<>();

        Request request = new Request.Builder()
                .url(bunker.getRelayUrl())
                .build();

        client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                wsRef.set(webSocket);
                connected.countDown();
            }
        });

        assertTrue(connected.await(5, TimeUnit.SECONDS));

        // Send multiple requests
        sendRequest(wsRef.get(), Nip46Request.ping());
        sendRequest(wsRef.get(), Nip46Request.getPublicKey());
        sendRequest(wsRef.get(), Nip46Request.ping());

        assertTrue(bunker.awaitRequestCount(3, 5, TimeUnit.SECONDS));

        List<Nip46Request> pings = bunker.getRequestsByMethod("ping");
        assertEquals(2, pings.size());

        List<Nip46Request> getPubKeys = bunker.getRequestsByMethod("get_public_key");
        assertEquals(1, getPubKeys.size());
    }

    @Test
    void rejectMethodReturnsError() throws Exception {
        bunker.rejectMethod("sign_event", "FORBIDDEN", "Signing not allowed");

        CountDownLatch connected = new CountDownLatch(1);
        CountDownLatch responseReceived = new CountDownLatch(1);
        AtomicReference<WebSocket> wsRef = new AtomicReference<>();
        AtomicReference<String> responseRef = new AtomicReference<>();

        Request request = new Request.Builder()
                .url(bunker.getRelayUrl())
                .build();

        client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                wsRef.set(webSocket);
                connected.countDown();
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                if (text.contains("EVENT") && text.contains("FORBIDDEN")) {
                    responseRef.set(text);
                    responseReceived.countDown();
                }
            }
        });

        assertTrue(connected.await(5, TimeUnit.SECONDS));
        wsRef.get().send("[\"REQ\",\"nip46\",{\"kinds\":[24133]}]");
        Thread.sleep(100);

        sendRequest(wsRef.get(), Nip46Request.signEvent("{\"kind\":1}"));

        assertTrue(responseReceived.await(5, TimeUnit.SECONDS));
        assertTrue(responseRef.get().contains("FORBIDDEN"));
        assertTrue(responseRef.get().contains("Signing not allowed"));
    }

    @Test
    void clearReceivedRequests() throws Exception {
        CountDownLatch connected = new CountDownLatch(1);
        AtomicReference<WebSocket> wsRef = new AtomicReference<>();

        Request request = new Request.Builder()
                .url(bunker.getRelayUrl())
                .build();

        client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                wsRef.set(webSocket);
                connected.countDown();
            }
        });

        assertTrue(connected.await(5, TimeUnit.SECONDS));

        sendRequest(wsRef.get(), Nip46Request.ping());
        assertTrue(bunker.awaitRequestCount(1, 5, TimeUnit.SECONDS));
        assertEquals(1, bunker.getReceivedRequestCount());

        bunker.clearReceivedRequests();
        assertEquals(0, bunker.getReceivedRequestCount());
    }

    @Test
    void awaitRequestReturnsRequest() throws Exception {
        CountDownLatch connected = new CountDownLatch(1);
        AtomicReference<WebSocket> wsRef = new AtomicReference<>();

        Request request = new Request.Builder()
                .url(bunker.getRelayUrl())
                .build();

        client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                wsRef.set(webSocket);
                connected.countDown();
            }
        });

        assertTrue(connected.await(5, TimeUnit.SECONDS));

        Nip46Request sentReq = Nip46Request.ping();
        sendRequest(wsRef.get(), sentReq);

        Nip46Request receivedReq = bunker.awaitRequest(5, TimeUnit.SECONDS);

        assertNotNull(receivedReq);
        assertEquals(sentReq.getId(), receivedReq.getId());
        assertEquals("ping", receivedReq.getMethod());
    }

    @Test
    void autoRespondCanBeDisabled() throws Exception {
        bunker.setAutoRespond(false);

        CountDownLatch connected = new CountDownLatch(1);
        CountDownLatch responseReceived = new CountDownLatch(1);
        AtomicReference<WebSocket> wsRef = new AtomicReference<>();

        Request request = new Request.Builder()
                .url(bunker.getRelayUrl())
                .build();

        client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                wsRef.set(webSocket);
                connected.countDown();
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                if (text.contains("pong")) {
                    responseReceived.countDown();
                }
            }
        });

        assertTrue(connected.await(5, TimeUnit.SECONDS));
        wsRef.get().send("[\"REQ\",\"nip46\",{\"kinds\":[24133]}]");
        Thread.sleep(100);

        sendRequest(wsRef.get(), Nip46Request.ping());

        // Should NOT receive a response
        assertFalse(responseReceived.await(500, TimeUnit.MILLISECONDS));

        // But request should be recorded
        assertEquals(1, bunker.getReceivedRequestCount());
    }

    @Test
    void responseDelayWorks() throws Exception {
        bunker.setResponseDelayMs(200);

        CountDownLatch connected = new CountDownLatch(1);
        CountDownLatch responseReceived = new CountDownLatch(1);
        AtomicReference<WebSocket> wsRef = new AtomicReference<>();
        AtomicReference<Long> responseTime = new AtomicReference<>();

        Request request = new Request.Builder()
                .url(bunker.getRelayUrl())
                .build();

        client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                wsRef.set(webSocket);
                connected.countDown();
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                if (text.contains("pong")) {
                    responseTime.set(System.currentTimeMillis());
                    responseReceived.countDown();
                }
            }
        });

        assertTrue(connected.await(5, TimeUnit.SECONDS));
        wsRef.get().send("[\"REQ\",\"nip46\",{\"kinds\":[24133]}]");
        Thread.sleep(100);

        long startTime = System.currentTimeMillis();
        sendRequest(wsRef.get(), Nip46Request.ping());

        assertTrue(responseReceived.await(5, TimeUnit.SECONDS));
        long elapsed = responseTime.get() - startTime;
        assertTrue(elapsed >= 200, "Response should be delayed by at least 200ms, was " + elapsed);
    }

    @Test
    void onSignEventHandler() throws Exception {
        bunker.onSignEvent(eventJson -> "{\"signed\":true}");

        CountDownLatch connected = new CountDownLatch(1);
        CountDownLatch responseReceived = new CountDownLatch(1);
        AtomicReference<WebSocket> wsRef = new AtomicReference<>();
        AtomicReference<String> responseRef = new AtomicReference<>();

        Request request = new Request.Builder()
                .url(bunker.getRelayUrl())
                .build();

        client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                wsRef.set(webSocket);
                connected.countDown();
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                if (text.contains("EVENT") && text.contains("signed")) {
                    responseRef.set(text);
                    responseReceived.countDown();
                }
            }
        });

        assertTrue(connected.await(5, TimeUnit.SECONDS));
        wsRef.get().send("[\"REQ\",\"nip46\",{\"kinds\":[24133]}]");
        Thread.sleep(100);

        sendRequest(wsRef.get(), Nip46Request.signEvent("{\"kind\":1,\"content\":\"test\"}"));

        assertTrue(responseReceived.await(5, TimeUnit.SECONDS));
        assertTrue(responseRef.get().contains("signed"));
    }

    @Test
    void acceptsMultipleConnections() throws Exception {
        bunker.acceptConnections(2);

        CountDownLatch connected = new CountDownLatch(2);

        for (int i = 0; i < 2; i++) {
            Request request = new Request.Builder()
                    .url(bunker.getRelayUrl())
                    .build();

            client.newWebSocket(request, new WebSocketListener() {
                @Override
                public void onOpen(WebSocket webSocket, Response response) {
                    connected.countDown();
                }
            });
        }

        assertTrue(connected.await(5, TimeUnit.SECONDS));
    }

    private void sendRequest(WebSocket ws, Nip46Request req) {
        String requestJson = encoder.encodeRequest(req);
        String eventJson = String.format("[\"EVENT\",{\"kind\":24133,\"content\":\"%s\"}]",
                escapeJson(requestJson));
        ws.send(eventJson);
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }
}
