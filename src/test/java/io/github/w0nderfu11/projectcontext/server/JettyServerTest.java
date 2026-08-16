package io.github.w0nderfu11.projectcontext.server;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JettyServerTest {

    private static final String HOST = "127.0.0.1";
    private static final int DYNAMIC_PORT = 0;
    private static final long TEST_TIMEOUT_SECONDS = 3;

    private static final Handler HANDLER = new Handler.Abstract() {
        @Override
        public boolean handle(Request request, Response response, Callback callback) {
            callback.succeeded();
            return true;
        }
    };

    @Test
    void shouldStartAndStopServer() throws Exception {
        JettyServer server = new JettyServer(HOST, DYNAMIC_PORT, HANDLER);

        assertFalse(server.isRunning());

        try {
            server.start();

            assertTrue(server.isRunning());
        } finally {
            server.stop();
        }

        assertFalse(server.isRunning());
    }

    @Test
    void shouldAssignLocalPortWhenConfiguredWithZero() throws Exception {
        JettyServer server = new JettyServer(HOST, DYNAMIC_PORT, HANDLER);

        try {
            server.start();

            assertTrue(
                    server.localPort() > 0,
                    "Server should bind to an available local port"
            );
        } finally {
            server.stop();
        }
    }

    @Test
    void shouldRejectNullHost() {
        assertThrows(
                NullPointerException.class,
                () -> new JettyServer(null, 8080, HANDLER)
        );
    }

    @Test
    void shouldRejectBlankHost() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new JettyServer(" ", 8080, HANDLER)
        );
    }

    @Test
    void shouldRejectNegativePort() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new JettyServer(HOST, -1, HANDLER)
        );
    }

    @Test
    void shouldRejectPortAboveMaximum() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new JettyServer(HOST, 65536, HANDLER)
        );
    }

    @Test
    void shouldRejectNullHandler() {
        assertThrows(
                NullPointerException.class,
                () -> new JettyServer(HOST, 8080, null)
        );
    }

    @Test
    @SuppressWarnings("HttpUrlsUsage")
    void shouldWaitForActiveRequestDuringGracefulShutdown() throws Exception {
        CountDownLatch requestStarted = new CountDownLatch(1);
        CountDownLatch allowRequestToComplete = new CountDownLatch(1);

        JettyServer server = createServerWithBlockingHandler(
                requestStarted,
                allowRequestToComplete
        );

        try {
            server.start();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + HOST + ":" + server.localPort()))
                    .GET()
                    .build();

            try (
                    HttpClient client = HttpClient.newHttpClient();
                    var executor = Executors.newVirtualThreadPerTaskExecutor()
            ) {
                CompletableFuture<HttpResponse<String>> responseFuture =
                        CompletableFuture.supplyAsync(() -> {
                            try {
                                return client.send(
                                        request,
                                        HttpResponse.BodyHandlers.ofString()
                                );
                            } catch (Exception exception) {
                                throw new RuntimeException(exception);
                            }
                        }, executor);

                assertTrue(
                        requestStarted.await(
                                TEST_TIMEOUT_SECONDS,
                                TimeUnit.SECONDS
                        ),
                        "HTTP request did not reach the handler within the expected time"
                );

                CompletableFuture<Void> stopFuture =
                        CompletableFuture.runAsync(() -> {
                            try {
                                server.stop();
                            } catch (Exception exception) {
                                throw new RuntimeException(exception);
                            }
                        }, executor);

                assertThrows(
                        TimeoutException.class,
                        () -> stopFuture.get(100, TimeUnit.MILLISECONDS),
                        "Server shutdown should wait while an active request is still running"
                );

                allowRequestToComplete.countDown();

                HttpResponse<String> response = responseFuture.get(
                        TEST_TIMEOUT_SECONDS,
                        TimeUnit.SECONDS
                );

                stopFuture.get(
                        TEST_TIMEOUT_SECONDS,
                        TimeUnit.SECONDS
                );

                assertEquals(
                        200,
                        response.statusCode(),
                        "Active request should complete successfully during graceful shutdown"
                );

                assertFalse(
                        server.isRunning(),
                        "Server should be stopped after the active request completes"
                );
            }
        } finally {
            allowRequestToComplete.countDown();

            if (server.isRunning()) {
                server.stop();
            }
        }
    }

    private static JettyServer createServerWithBlockingHandler(
            CountDownLatch requestStarted,
            CountDownLatch allowRequestToComplete
    ) {
        Handler blockingHandler = new Handler.Abstract() {
            @Override
            public boolean handle(
                    Request request,
                    Response response,
                    Callback callback
            ) {
                requestStarted.countDown();

                try {
                    allowRequestToComplete.await();

                    response.setStatus(200);
                    callback.succeeded();
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    callback.failed(exception);
                }

                return true;
            }
        };

        return new JettyServer(HOST, DYNAMIC_PORT, blockingHandler);
    }
}