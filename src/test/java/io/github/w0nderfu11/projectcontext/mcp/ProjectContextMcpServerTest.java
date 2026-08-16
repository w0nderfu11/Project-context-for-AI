package io.github.w0nderfu11.projectcontext.mcp;

import io.github.w0nderfu11.projectcontext.application.PingService;
import io.github.w0nderfu11.projectcontext.mcp.tools.PingTool;
import io.github.w0nderfu11.projectcontext.server.JettyServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectContextMcpServerTest {

    private static final String VERSION_RESOURCE = "/project-context.properties";
    private static final String VERSION_PROPERTY = "project.version";

    @Test
    void shouldCreateAndCloseMcpServer() {
        McpHttpTransport transport = new McpHttpTransport();
        PingTool pingTool = new PingTool(new PingService());

        assertDoesNotThrow(() ->
                new ProjectContextMcpServer(transport, pingTool).close()
        );
    }

    @Test
    @SuppressWarnings("resource")
    void shouldRejectNullTransport() {
        PingTool pingTool = new PingTool(new PingService());

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new ProjectContextMcpServer(null, pingTool)
        );

        assertEquals("transport must not be null", exception.getMessage());
    }


    @Test
    @SuppressWarnings("resource")
    void shouldRejectNullPingTool() {
        McpHttpTransport transport = new McpHttpTransport();

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new ProjectContextMcpServer(transport, null)
        );

        assertEquals("pingTool must not be null", exception.getMessage());
    }

    @Test
    void shouldInitializeMcpServerOverHttp() throws Exception {
        McpHttpTransport transport = new McpHttpTransport();
        PingTool pingTool = new PingTool(new PingService());

        try (ProjectContextMcpServer ignored =
                     new ProjectContextMcpServer(transport, pingTool)) {

            JettyServer httpServer = new JettyServer(
                    "127.0.0.1",
                    0,
                    transport.handler()
            );

            try {
                httpServer.start();

                URI endpoint = endpoint(httpServer);

                try (HttpClient client = HttpClient.newHttpClient()) {
                    HttpResponse<String> response = client.send(
                            initializeRequest(endpoint),
                            HttpResponse.BodyHandlers.ofString()
                    );

                    assertEquals(200, response.statusCode());
                    assertTrue(response.body().contains("\"project-context\""));
                    assertTrue(response.body().contains(
                            "\"" + projectVersion() + "\""
                    ));
                }
            } finally {
                httpServer.stop();
            }
        }
    }

    @Test
    void shouldCallPingToolOverHttp() throws Exception {
        McpHttpTransport transport = new McpHttpTransport();
        PingTool pingTool = new PingTool(new PingService());

        try (ProjectContextMcpServer ignored =
                     new ProjectContextMcpServer(transport, pingTool)) {

            JettyServer httpServer = new JettyServer(
                    "127.0.0.1",
                    0,
                    transport.handler()
            );

            try {
                httpServer.start();

                URI endpoint = endpoint(httpServer);

                try (HttpClient client = HttpClient.newHttpClient()) {
                    HttpResponse<String> initializeResponse = client.send(
                            initializeRequest(endpoint),
                            HttpResponse.BodyHandlers.ofString()
                    );

                    assertEquals(200, initializeResponse.statusCode());

                    String sessionId = initializeResponse.headers()
                            .firstValue("Mcp-Session-Id")
                            .orElseThrow();

                    //noinspection UastIncorrectHttpHeaderInspection
                    HttpRequest initializedRequest = HttpRequest.newBuilder()
                            .uri(endpoint)
                            .header("Content-Type", "application/json")
                            .header(
                                    "Accept",
                                    "application/json, text/event-stream"
                            )
                            .header("Mcp-Session-Id", sessionId)
                            .POST(HttpRequest.BodyPublishers.ofString("""
                                    {
                                      "jsonrpc": "2.0",
                                      "method": "notifications/initialized"
                                    }
                                    """))
                            .build();

                    HttpResponse<String> initializedResponse = client.send(
                            initializedRequest,
                            HttpResponse.BodyHandlers.ofString()
                    );

                    assertEquals(202, initializedResponse.statusCode());

                    //noinspection UastIncorrectHttpHeaderInspection
                    HttpRequest pingRequest = HttpRequest.newBuilder()
                            .uri(endpoint)
                            .header("Content-Type", "application/json")
                            .header(
                                    "Accept",
                                    "application/json, text/event-stream"
                            )
                            .header("Mcp-Session-Id", sessionId)
                            .POST(HttpRequest.BodyPublishers.ofString("""
                                    {
                                      "jsonrpc": "2.0",
                                      "id": 2,
                                      "method": "tools/call",
                                      "params": {
                                        "name": "ping",
                                        "arguments": {}
                                      }
                                    }
                                    """))
                            .build();

                    HttpResponse<String> pingResponse = client.send(
                            pingRequest,
                            HttpResponse.BodyHandlers.ofString()
                    );

                    assertEquals(200, pingResponse.statusCode());
                    assertTrue(
                            pingResponse.body().contains(
                                    "hello from Project Context"
                            )
                    );
                }
            } finally {
                httpServer.stop();
            }
        }
    }

    private static HttpRequest initializeRequest(URI endpoint) {
        return HttpRequest.newBuilder()
                .uri(endpoint)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString("""
                        {
                          "jsonrpc": "2.0",
                          "id": 1,
                          "method": "initialize",
                          "params": {
                            "protocolVersion": "2025-11-25",
                            "capabilities": {},
                            "clientInfo": {
                              "name": "project-context-test",
                              "version": "1.0"
                            }
                          }
                        }
                        """))
                .build();
    }

    private static URI endpoint(JettyServer server) {
        return URI.create(
                "http://127.0.0.1:"
                        + server.localPort()
                        + McpEndpoint.MCP
        );
    }

    private static String projectVersion() {
        Properties properties = new Properties();

        try (InputStream input =
                     ProjectContextMcpServerTest.class
                             .getResourceAsStream(VERSION_RESOURCE)) {

            if (input == null) {
                throw new IllegalStateException(
                        "Project version resource not found: " + VERSION_RESOURCE
                );
            }

            properties.load(input);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to load project version",
                    e
            );
        }

        String version = properties.getProperty(VERSION_PROPERTY);

        if (version == null || version.isBlank()) {
            throw new IllegalStateException(
                    "Project version is missing or blank"
            );
        }

        return version;
    }
}