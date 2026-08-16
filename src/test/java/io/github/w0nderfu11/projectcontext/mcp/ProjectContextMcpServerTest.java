package io.github.w0nderfu11.projectcontext.mcp;

import io.github.w0nderfu11.projectcontext.application.PingService;
import io.github.w0nderfu11.projectcontext.mcp.tools.PingTool;
import io.github.w0nderfu11.projectcontext.server.JettyServer;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectContextMcpServerTest {

    @Test
    void shouldCreateMcpServer() {
        McpHttpTransport transport = new McpHttpTransport();
        PingService pingService = new PingService();
        PingTool pingTool = new PingTool(pingService);

        assertDoesNotThrow(() ->
                new ProjectContextMcpServer(transport, pingTool)
        );
    }

    @Test
    void shouldInitializeMcpServerOverHttp() throws Exception {
        McpHttpTransport transport = new McpHttpTransport();
        PingService pingService = new PingService();
        PingTool pingTool = new PingTool(pingService);

        new ProjectContextMcpServer(transport, pingTool);

        JettyServer httpServer = new JettyServer(
                "127.0.0.1",
                0,
                transport.handler()
        );

        try {
            httpServer.start();

            String requestBody = """
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
                    """;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(
                            "http://127.0.0.1:"
                                    + httpServer.localPort()
                                    + McpEndpoint.MCP
                    ))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json, text/event-stream")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            try (HttpClient client = HttpClient.newHttpClient()) {
                HttpResponse<String> response = client.send(
                        request,
                        HttpResponse.BodyHandlers.ofString()
                );

                assertEquals(200, response.statusCode());
                assertTrue(response.body().contains("\"project-context\""));
                assertTrue(response.body().contains("\"0.1.0\""));
            }
        } finally {
            httpServer.stop();
        }
    }

    @Test
    void shouldCallPingToolOverHttp() throws Exception {
        McpHttpTransport transport = new McpHttpTransport();
        PingService pingService = new PingService();
        PingTool pingTool = new PingTool(pingService);

        new ProjectContextMcpServer(transport, pingTool);

        JettyServer httpServer = new JettyServer(
                "127.0.0.1",
                0,
                transport.handler()
        );

        try {
            httpServer.start();

            URI endpoint = URI.create(
                    "http://127.0.0.1:"
                            + httpServer.localPort()
                            + McpEndpoint.MCP
            );

            try (HttpClient client = HttpClient.newHttpClient()) {

                String initializeBody = """
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
                    """;

                HttpRequest initializeRequest = HttpRequest.newBuilder()
                        .uri(endpoint)
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json, text/event-stream")
                        .POST(HttpRequest.BodyPublishers.ofString(initializeBody))
                        .build();

                HttpResponse<String> initializeResponse = client.send(
                        initializeRequest,
                        HttpResponse.BodyHandlers.ofString()
                );

                assertEquals(200, initializeResponse.statusCode());

                String sessionId = initializeResponse.headers()
                        .firstValue("Mcp-Session-Id")
                        .orElseThrow();

                String initializedBody = """
                    {
                      "jsonrpc": "2.0",
                      "method": "notifications/initialized"
                    }
                    """;

                HttpRequest initializedRequest = HttpRequest.newBuilder()
                        .uri(endpoint)
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json, text/event-stream")
                        .header("Mcp-Session-Id", sessionId)
                        .POST(HttpRequest.BodyPublishers.ofString(initializedBody))
                        .build();

                HttpResponse<String> initializedResponse = client.send(
                        initializedRequest,
                        HttpResponse.BodyHandlers.ofString()
                );

                assertEquals(202, initializedResponse.statusCode());

                String pingBody = """
                    {
                      "jsonrpc": "2.0",
                      "id": 2,
                      "method": "tools/call",
                      "params": {
                        "name": "ping",
                        "arguments": {}
                      }
                    }
                    """;

                HttpRequest pingRequest = HttpRequest.newBuilder()
                        .uri(endpoint)
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json, text/event-stream")
                        .header("Mcp-Session-Id", sessionId)
                        .POST(HttpRequest.BodyPublishers.ofString(pingBody))
                        .build();

                HttpResponse<String> pingResponse = client.send(
                        pingRequest,
                        HttpResponse.BodyHandlers.ofString()
                );

                assertEquals(200, pingResponse.statusCode());
                assertTrue(
                        pingResponse.body().contains("hello from Project Context")
                );
            }
        } finally {
            httpServer.stop();
        }
    }
}