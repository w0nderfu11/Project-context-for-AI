package io.github.w0nderfu11.projectcontext.mcp;

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

        assertDoesNotThrow(() -> new ProjectContextMcpServer(transport));
    }

    @Test
    void shouldInitializeMcpServerOverHttp() throws Exception {
        McpHttpTransport transport = new McpHttpTransport();
        new ProjectContextMcpServer(transport);

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
}