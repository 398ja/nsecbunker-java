package xyz.tcheeric.nsecbunker.connection.testing;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link MockRelayServer}.
 */
class MockRelayServerTest {

    private MockRelayServer relay;
    private OkHttpClient client;

    @BeforeEach
    void setUp() throws Exception {
        relay = new MockRelayServer();
        relay.start();
        relay.enqueueWebSocketUpgrade();
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
        if (relay != null) {
            relay.close();
        }
    }

    @Test
    void startCreatesServer() {
        assertTrue(relay.isStarted());
        assertTrue(relay.getPort() > 0);
        assertTrue(relay.getUrl().startsWith("ws://"));
    }

    @Test
    void clientCanConnect() throws Exception {
        CountDownLatch connected = new CountDownLatch(1);

        Request request = new Request.Builder()
                .url(relay.getUrl())
                .build();

        client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                connected.countDown();
            }
        });

        assertTrue(connected.await(5, TimeUnit.SECONDS));
        assertTrue(relay.awaitConnection(1, TimeUnit.SECONDS));
        assertEquals(1, relay.getConnectionCount());
    }

    @Test
    void receivesMessages() throws Exception {
        CountDownLatch connected = new CountDownLatch(1);
        AtomicReference<WebSocket> wsRef = new AtomicReference<>();

        Request request = new Request.Builder()
                .url(relay.getUrl())
                .build();

        client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                wsRef.set(webSocket);
                connected.countDown();
            }
        });

        assertTrue(connected.await(5, TimeUnit.SECONDS));

        wsRef.get().send("[\"EVENT\",{\"id\":\"abc\",\"kind\":1,\"content\":\"test\"}]");

        assertTrue(relay.awaitMessageCount(1, 5, TimeUnit.SECONDS));
        assertEquals(1, relay.getReceivedMessageCount());
        assertTrue(relay.getReceivedMessages().get(0).contains("EVENT"));
    }

    @Test
    void sendsDefaultOkForEvents() throws Exception {
        CountDownLatch connected = new CountDownLatch(1);
        CountDownLatch okReceived = new CountDownLatch(1);
        AtomicReference<WebSocket> wsRef = new AtomicReference<>();
        AtomicReference<String> responseRef = new AtomicReference<>();

        Request request = new Request.Builder()
                .url(relay.getUrl())
                .build();

        client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                wsRef.set(webSocket);
                connected.countDown();
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                responseRef.set(text);
                okReceived.countDown();
            }
        });

        assertTrue(connected.await(5, TimeUnit.SECONDS));

        wsRef.get().send("[\"EVENT\",{\"id\":\"test-event-id\",\"kind\":1}]");

        assertTrue(okReceived.await(5, TimeUnit.SECONDS));
        String response = responseRef.get();
        assertTrue(response.contains("OK"));
        assertTrue(response.contains("test-event-id"));
        assertTrue(response.contains("true"));
    }

    @Test
    void customEventHandler() throws Exception {
        relay.onEvent(event -> {
            String id = event.get("id").asText();
            return String.format("[\"OK\",\"%s\",false,\"rejected\"]", id);
        });

        CountDownLatch connected = new CountDownLatch(1);
        CountDownLatch okReceived = new CountDownLatch(1);
        AtomicReference<WebSocket> wsRef = new AtomicReference<>();
        AtomicReference<String> responseRef = new AtomicReference<>();

        Request request = new Request.Builder()
                .url(relay.getUrl())
                .build();

        client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                wsRef.set(webSocket);
                connected.countDown();
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                responseRef.set(text);
                okReceived.countDown();
            }
        });

        assertTrue(connected.await(5, TimeUnit.SECONDS));

        wsRef.get().send("[\"EVENT\",{\"id\":\"test-id\",\"kind\":1}]");

        assertTrue(okReceived.await(5, TimeUnit.SECONDS));
        String response = responseRef.get();
        assertTrue(response.contains("false"));
        assertTrue(response.contains("rejected"));
    }

    @Test
    void reqMessageTriggersEose() throws Exception {
        CountDownLatch connected = new CountDownLatch(1);
        CountDownLatch eoseReceived = new CountDownLatch(1);
        AtomicReference<WebSocket> wsRef = new AtomicReference<>();
        AtomicReference<String> responseRef = new AtomicReference<>();

        Request request = new Request.Builder()
                .url(relay.getUrl())
                .build();

        client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                wsRef.set(webSocket);
                connected.countDown();
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                if (text.contains("EOSE")) {
                    responseRef.set(text);
                    eoseReceived.countDown();
                }
            }
        });

        assertTrue(connected.await(5, TimeUnit.SECONDS));

        wsRef.get().send("[\"REQ\",\"sub-id\",{\"kinds\":[1]}]");

        assertTrue(eoseReceived.await(5, TimeUnit.SECONDS));
        assertTrue(responseRef.get().contains("EOSE"));
        assertTrue(responseRef.get().contains("sub-id"));
    }

    @Test
    void reqMessageStoresSubscription() throws Exception {
        CountDownLatch connected = new CountDownLatch(1);
        CountDownLatch eoseReceived = new CountDownLatch(1);
        AtomicReference<WebSocket> wsRef = new AtomicReference<>();

        Request request = new Request.Builder()
                .url(relay.getUrl())
                .build();

        client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                wsRef.set(webSocket);
                connected.countDown();
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                if (text.contains("EOSE")) {
                    eoseReceived.countDown();
                }
            }
        });

        assertTrue(connected.await(5, TimeUnit.SECONDS));

        wsRef.get().send("[\"REQ\",\"my-sub\",{\"kinds\":[1]},{\"authors\":[\"abc\"]}]");

        assertTrue(eoseReceived.await(5, TimeUnit.SECONDS));
        assertTrue(relay.getSubscriptions().containsKey("my-sub"));
        assertEquals(2, relay.getSubscriptions().get("my-sub").size());
    }

    @Test
    void closeMessageRemovesSubscription() throws Exception {
        CountDownLatch connected = new CountDownLatch(1);
        CountDownLatch eoseReceived = new CountDownLatch(1);
        AtomicReference<WebSocket> wsRef = new AtomicReference<>();

        Request request = new Request.Builder()
                .url(relay.getUrl())
                .build();

        client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                wsRef.set(webSocket);
                connected.countDown();
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                if (text.contains("EOSE")) {
                    eoseReceived.countDown();
                }
            }
        });

        assertTrue(connected.await(5, TimeUnit.SECONDS));

        wsRef.get().send("[\"REQ\",\"to-close\",{\"kinds\":[1]}]");
        assertTrue(eoseReceived.await(5, TimeUnit.SECONDS));
        assertTrue(relay.getSubscriptions().containsKey("to-close"));

        wsRef.get().send("[\"CLOSE\",\"to-close\"]");
        Thread.sleep(100);

        assertFalse(relay.getSubscriptions().containsKey("to-close"));
    }

    @Test
    void broadcastSendsToAllClients() throws Exception {
        relay.acceptConnections(2);

        CountDownLatch connected = new CountDownLatch(2);
        CountDownLatch received = new CountDownLatch(2);
        List<String> messages = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            Request request = new Request.Builder()
                    .url(relay.getUrl())
                    .build();

            client.newWebSocket(request, new WebSocketListener() {
                @Override
                public void onOpen(WebSocket webSocket, Response response) {
                    connected.countDown();
                }

                @Override
                public void onMessage(WebSocket webSocket, String text) {
                    synchronized (messages) {
                        messages.add(text);
                    }
                    received.countDown();
                }
            });
        }

        assertTrue(connected.await(5, TimeUnit.SECONDS));
        // Wait for both connections to be registered on server side
        long deadline = System.currentTimeMillis() + 5000;
        while (relay.getConnectionCount() < 2 && System.currentTimeMillis() < deadline) {
            Thread.sleep(10);
        }
        assertEquals(2, relay.getConnectionCount());

        relay.broadcast("[\"NOTICE\",\"Hello all!\"]");

        assertTrue(received.await(5, TimeUnit.SECONDS));
        assertEquals(2, messages.size());
    }

    @Test
    void sendOkBroadcastsOkMessage() throws Exception {
        CountDownLatch connected = new CountDownLatch(1);
        CountDownLatch received = new CountDownLatch(1);
        AtomicReference<String> messageRef = new AtomicReference<>();

        Request request = new Request.Builder()
                .url(relay.getUrl())
                .build();

        client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                connected.countDown();
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                messageRef.set(text);
                received.countDown();
            }
        });

        assertTrue(connected.await(5, TimeUnit.SECONDS));
        // Wait for server to register the connection
        assertTrue(relay.awaitConnection(1, TimeUnit.SECONDS));

        relay.sendOk("event-123", true, "accepted");

        assertTrue(received.await(5, TimeUnit.SECONDS));
        String msg = messageRef.get();
        assertTrue(msg.contains("OK"));
        assertTrue(msg.contains("event-123"));
        assertTrue(msg.contains("true"));
    }

    @Test
    void sendNoticeBroadcastsNotice() throws Exception {
        CountDownLatch connected = new CountDownLatch(1);
        CountDownLatch received = new CountDownLatch(1);
        AtomicReference<String> messageRef = new AtomicReference<>();

        Request request = new Request.Builder()
                .url(relay.getUrl())
                .build();

        client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                connected.countDown();
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                messageRef.set(text);
                received.countDown();
            }
        });

        assertTrue(connected.await(5, TimeUnit.SECONDS));
        assertTrue(relay.awaitConnection(1, TimeUnit.SECONDS));

        relay.sendNotice("Server maintenance");

        assertTrue(received.await(5, TimeUnit.SECONDS));
        assertTrue(messageRef.get().contains("NOTICE"));
        assertTrue(messageRef.get().contains("Server maintenance"));
    }

    @Test
    void sendEventBroadcastsEvent() throws Exception {
        CountDownLatch connected = new CountDownLatch(1);
        CountDownLatch received = new CountDownLatch(1);
        AtomicReference<String> messageRef = new AtomicReference<>();

        Request request = new Request.Builder()
                .url(relay.getUrl())
                .build();

        client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                connected.countDown();
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                messageRef.set(text);
                received.countDown();
            }
        });

        assertTrue(connected.await(5, TimeUnit.SECONDS));
        assertTrue(relay.awaitConnection(1, TimeUnit.SECONDS));

        relay.sendEvent("sub-1", "{\"id\":\"e1\",\"kind\":1}");

        assertTrue(received.await(5, TimeUnit.SECONDS));
        assertTrue(messageRef.get().contains("EVENT"));
        assertTrue(messageRef.get().contains("sub-1"));
        assertTrue(messageRef.get().contains("e1"));
    }

    @Test
    void disconnectAllClosesConnections() throws Exception {
        CountDownLatch connected = new CountDownLatch(1);
        CountDownLatch closingOrClosed = new CountDownLatch(1);

        Request request = new Request.Builder()
                .url(relay.getUrl())
                .build();

        client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                connected.countDown();
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                closingOrClosed.countDown();
                webSocket.close(code, reason);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                closingOrClosed.countDown();
            }
        });

        assertTrue(connected.await(5, TimeUnit.SECONDS));
        // Wait for server to register the connection
        assertTrue(relay.awaitConnection(1, TimeUnit.SECONDS));
        assertEquals(1, relay.getConnectionCount());

        relay.disconnectAll();

        assertTrue(closingOrClosed.await(5, TimeUnit.SECONDS));
        // Give time for connection cleanup
        Thread.sleep(200);
        assertEquals(0, relay.getConnectionCount());
    }

    @Test
    void clearReceivedMessages() throws Exception {
        CountDownLatch connected = new CountDownLatch(1);
        AtomicReference<WebSocket> wsRef = new AtomicReference<>();

        Request request = new Request.Builder()
                .url(relay.getUrl())
                .build();

        client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                wsRef.set(webSocket);
                connected.countDown();
            }
        });

        assertTrue(connected.await(5, TimeUnit.SECONDS));

        wsRef.get().send("[\"REQ\",\"sub\",{}]");
        assertTrue(relay.awaitMessageCount(1, 5, TimeUnit.SECONDS));
        assertEquals(1, relay.getReceivedMessageCount());

        relay.clearReceivedMessages();

        assertEquals(0, relay.getReceivedMessageCount());
    }

    @Test
    void awaitMessageReturnsMessage() throws Exception {
        CountDownLatch connected = new CountDownLatch(1);
        AtomicReference<WebSocket> wsRef = new AtomicReference<>();

        Request request = new Request.Builder()
                .url(relay.getUrl())
                .build();

        client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                wsRef.set(webSocket);
                connected.countDown();
            }
        });

        assertTrue(connected.await(5, TimeUnit.SECONDS));

        wsRef.get().send("[\"REQ\",\"test\",{}]");

        String message = relay.awaitMessage(5, TimeUnit.SECONDS);

        assertNotNull(message);
        assertTrue(message.contains("REQ"));
    }

    @Test
    void closeShutdownsServer() throws Exception {
        relay.close();

        assertFalse(relay.isStarted());
    }

    @Test
    void customReqHandler() throws Exception {
        List<String> events = List.of(
                "[\"EVENT\",\"sub-id\",{\"id\":\"e1\",\"kind\":1}]",
                "[\"EVENT\",\"sub-id\",{\"id\":\"e2\",\"kind\":1}]"
        );
        relay.onReq(req -> events);

        CountDownLatch connected = new CountDownLatch(1);
        List<String> received = new ArrayList<>();
        CountDownLatch eoseReceived = new CountDownLatch(1);
        AtomicReference<WebSocket> wsRef = new AtomicReference<>();

        Request request = new Request.Builder()
                .url(relay.getUrl())
                .build();

        client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                wsRef.set(webSocket);
                connected.countDown();
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                synchronized (received) {
                    received.add(text);
                }
                if (text.contains("EOSE")) {
                    eoseReceived.countDown();
                }
            }
        });

        assertTrue(connected.await(5, TimeUnit.SECONDS));

        wsRef.get().send("[\"REQ\",\"sub-id\",{}]");

        assertTrue(eoseReceived.await(5, TimeUnit.SECONDS));
        assertTrue(received.size() >= 3); // 2 events + EOSE
    }
}
