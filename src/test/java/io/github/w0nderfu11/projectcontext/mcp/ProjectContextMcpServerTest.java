package io.github.w0nderfu11.projectcontext.mcp;

import io.github.w0nderfu11.projectcontext.application.GetCurrentTreeService;
import io.github.w0nderfu11.projectcontext.application.PingService;
import io.github.w0nderfu11.projectcontext.application.ReadFileService;
import io.github.w0nderfu11.projectcontext.application.SearchService;
import io.github.w0nderfu11.projectcontext.mcp.tools.GetCurrentTreeTool;
import io.github.w0nderfu11.projectcontext.mcp.tools.PingTool;
import io.github.w0nderfu11.projectcontext.mcp.tools.ReadFileTool;
import io.github.w0nderfu11.projectcontext.mcp.tools.SearchTool;
import io.github.w0nderfu11.projectcontext.registry.ProjectRegistry;
import io.github.w0nderfu11.projectcontext.server.JettyServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectContextMcpServerTest {

    private static final String VERSION_RESOURCE = "/project-context.properties";
    private static final String VERSION_PROPERTY = "project.version";

    @TempDir
    Path tempDir;

    @Test
    void shouldCreateAndCloseMcpServer() throws IOException {
        McpHttpTransport transport = new McpHttpTransport();
        PingTool pingTool = new PingTool(new PingService());
        ReadFileTool readFileTool = readFileTool();
        GetCurrentTreeTool getCurrentTreeTool = getCurrentTreeTool();
        SearchTool searchTool = searchTool();

        assertDoesNotThrow(() ->
                new ProjectContextMcpServer(
                        transport,
                        pingTool,
                        readFileTool,
                        getCurrentTreeTool,
                        searchTool
                ).close()
        );
    }

    @Test
    @SuppressWarnings("resource")
    void shouldRejectNullTransport() throws IOException {
        PingTool pingTool = new PingTool(new PingService());
        ReadFileTool readFileTool = readFileTool();
        GetCurrentTreeTool getCurrentTreeTool = getCurrentTreeTool();
        SearchTool searchTool = searchTool();

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new ProjectContextMcpServer(
                        null,
                        pingTool,
                        readFileTool,
                        getCurrentTreeTool,
                        searchTool
                )
        );

        assertEquals(
                "transport must not be null",
                exception.getMessage()
        );
    }

    @Test
    @SuppressWarnings("resource")
    void shouldRejectNullPingTool() throws IOException {
        McpHttpTransport transport = new McpHttpTransport();
        ReadFileTool readFileTool = readFileTool();
        GetCurrentTreeTool getCurrentTreeTool = getCurrentTreeTool();
        SearchTool searchTool = searchTool();

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new ProjectContextMcpServer(
                        transport,
                        null,
                        readFileTool,
                        getCurrentTreeTool,
                        searchTool
                )
        );

        assertEquals(
                "pingTool must not be null",
                exception.getMessage()
        );
    }

    @Test
    @SuppressWarnings("resource")
    void shouldRejectNullReadFileTool() throws IOException {
        McpHttpTransport transport = new McpHttpTransport();
        PingTool pingTool = new PingTool(new PingService());
        GetCurrentTreeTool getCurrentTreeTool = getCurrentTreeTool();
        SearchTool searchTool = searchTool();

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new ProjectContextMcpServer(
                        transport,
                        pingTool,
                        null,
                        getCurrentTreeTool,
                        searchTool
                )
        );

        assertEquals(
                "readFileTool must not be null",
                exception.getMessage()
        );
    }

    @Test
    @SuppressWarnings("resource")
    void shouldRejectNullGetCurrentTreeTool() throws IOException {
        McpHttpTransport transport = new McpHttpTransport();
        PingTool pingTool = new PingTool(new PingService());
        ReadFileTool readFileTool = readFileTool();
        SearchTool searchTool = searchTool();

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new ProjectContextMcpServer(
                        transport,
                        pingTool,
                        readFileTool,
                        null,
                        searchTool
                )
        );

        assertEquals(
                "getCurrentTreeTool must not be null",
                exception.getMessage()
        );
    }

    @Test
    @SuppressWarnings("resource")
    void shouldRejectNullSearchTool() throws IOException {
        McpHttpTransport transport = new McpHttpTransport();
        PingTool pingTool = new PingTool(new PingService());
        ReadFileTool readFileTool = readFileTool();
        GetCurrentTreeTool getCurrentTreeTool = getCurrentTreeTool();

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new ProjectContextMcpServer(
                        transport,
                        pingTool,
                        readFileTool,
                        getCurrentTreeTool,
                        null
                )
        );

        assertEquals(
                "searchTool must not be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldInitializeMcpServerOverHttp() throws Exception {
        McpHttpTransport transport = new McpHttpTransport();
        PingTool pingTool = new PingTool(new PingService());
        ReadFileTool readFileTool = readFileTool();
        GetCurrentTreeTool getCurrentTreeTool = getCurrentTreeTool();
        SearchTool searchTool = searchTool();

        try (ProjectContextMcpServer ignored =
                     new ProjectContextMcpServer(
                             transport,
                             pingTool,
                             readFileTool,
                             getCurrentTreeTool,
                             searchTool
                     )) {

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
                    assertTrue(
                            response.body().contains("\"project-context\"")
                    );
                    assertTrue(
                            response.body().contains(
                                    "\"" + projectVersion() + "\""
                            )
                    );
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
        ReadFileTool readFileTool = readFileTool();
        GetCurrentTreeTool getCurrentTreeTool = getCurrentTreeTool();
        SearchTool searchTool = searchTool();

        try (ProjectContextMcpServer ignored =
                     new ProjectContextMcpServer(
                             transport,
                             pingTool,
                             readFileTool,
                             getCurrentTreeTool,
                             searchTool
                     )) {

            JettyServer httpServer = new JettyServer(
                    "127.0.0.1",
                    0,
                    transport.handler()
            );

            try {
                httpServer.start();

                URI endpoint = endpoint(httpServer);

                try (HttpClient client = HttpClient.newHttpClient()) {
                    String sessionId = initializeSession(
                            client,
                            endpoint
                    );

                    HttpResponse<String> pingResponse = client.send(
                            toolRequest(
                                    endpoint,
                                    sessionId,
                                    """
                                    {
                                      "jsonrpc": "2.0",
                                      "id": 2,
                                      "method": "tools/call",
                                      "params": {
                                        "name": "ping",
                                        "arguments": {}
                                      }
                                    }
                                    """
                            ),
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

    @Test
    void shouldCallReadFileToolOverHttp() throws Exception {
        Path file = Files.writeString(
                tempDir.resolve("Example.java"),
                """
                public class Example {

                    public void execute() {
                        System.out.println("hello");
                    }
                }
                """
        );

        McpHttpTransport transport = new McpHttpTransport();
        PingTool pingTool = new PingTool(new PingService());
        ReadFileTool readFileTool = readFileTool();
        GetCurrentTreeTool getCurrentTreeTool = getCurrentTreeTool();
        SearchTool searchTool = searchTool();

        try (ProjectContextMcpServer ignored =
                     new ProjectContextMcpServer(
                             transport,
                             pingTool,
                             readFileTool,
                             getCurrentTreeTool,
                             searchTool
                     )) {

            JettyServer httpServer = new JettyServer(
                    "127.0.0.1",
                    0,
                    transport.handler()
            );

            try {
                httpServer.start();

                URI endpoint = endpoint(httpServer);

                try (HttpClient client = HttpClient.newHttpClient()) {
                    String sessionId = initializeSession(
                            client,
                            endpoint
                    );

                    String requestBody = """
                            {
                              "jsonrpc": "2.0",
                              "id": 3,
                              "method": "tools/call",
                              "params": {
                                "name": "read_file",
                                "arguments": {
                                  "projectName": "project",
                                  "filePath": "%s"
                                }
                              }
                            }
                            """.formatted(
                            jsonPath(file.toRealPath())
                    );

                    HttpResponse<String> response = client.send(
                            toolRequest(
                                    endpoint,
                                    sessionId,
                                    requestBody
                            ),
                            HttpResponse.BodyHandlers.ofString()
                    );

                    assertEquals(200, response.statusCode());
                    assertTrue(
                            response.body().contains(
                                    "public class Example"
                            )
                    );
                    assertTrue(
                            response.body().contains(
                                    "System.out.println"
                            )
                    );
                }
            } finally {
                httpServer.stop();
            }
        }
    }

    @Test
    void shouldCallGetCurrentTreeToolOverHttp() throws Exception {
        Path directory = Files.createDirectory(
                tempDir.resolve("src")
        );

        Path nestedDirectory = Files.createDirectory(
                directory.resolve("main")
        );

        Path file = Files.createFile(
                directory.resolve("Example.java")
        );

        McpHttpTransport transport = new McpHttpTransport();
        PingTool pingTool = new PingTool(new PingService());
        ReadFileTool readFileTool = readFileTool();
        GetCurrentTreeTool getCurrentTreeTool = getCurrentTreeTool();
        SearchTool searchTool = searchTool();

        try (ProjectContextMcpServer ignored =
                     new ProjectContextMcpServer(
                             transport,
                             pingTool,
                             readFileTool,
                             getCurrentTreeTool,
                             searchTool
                     )) {

            JettyServer httpServer = new JettyServer(
                    "127.0.0.1",
                    0,
                    transport.handler()
            );

            try {
                httpServer.start();

                URI endpoint = endpoint(httpServer);

                try (HttpClient client = HttpClient.newHttpClient()) {
                    String sessionId = initializeSession(
                            client,
                            endpoint
                    );

                    String requestBody = """
                            {
                              "jsonrpc": "2.0",
                              "id": 4,
                              "method": "tools/call",
                              "params": {
                                "name": "get_current_tree",
                                "arguments": {
                                  "projectName": "project",
                                  "directoryPath": "%s"
                                }
                              }
                            }
                            """.formatted(
                            jsonPath(directory.toRealPath())
                    );

                    HttpResponse<String> response = client.send(
                            toolRequest(
                                    endpoint,
                                    sessionId,
                                    requestBody
                            ),
                            HttpResponse.BodyHandlers.ofString()
                    );

                    assertEquals(200, response.statusCode());
                    assertTrue(
                            response.body().contains(
                                    jsonPath(nestedDirectory.toRealPath())
                            )
                    );
                    assertTrue(
                            response.body().contains(
                                    jsonPath(file.toRealPath())
                            )
                    );
                    assertTrue(
                            response.body().contains("DIRECTORY")
                    );
                    assertTrue(
                            response.body().contains("FILE")
                    );
                }
            } finally {
                httpServer.stop();
            }
        }
    }

    @Test
    void shouldCallSearchToolOverHttp() throws Exception {
        Path directory = Files.createDirectories(
                tempDir.resolve("src/main/java")
        );

        Path file = Files.createFile(
                directory.resolve("ProjectContextApplication.java")
        );

        McpHttpTransport transport = new McpHttpTransport();
        PingTool pingTool = new PingTool(new PingService());
        ReadFileTool readFileTool = readFileTool();
        GetCurrentTreeTool getCurrentTreeTool = getCurrentTreeTool();
        SearchTool searchTool = searchTool();

        try (ProjectContextMcpServer ignored =
                     new ProjectContextMcpServer(
                             transport,
                             pingTool,
                             readFileTool,
                             getCurrentTreeTool,
                             searchTool
                     )) {

            JettyServer httpServer = new JettyServer(
                    "127.0.0.1",
                    0,
                    transport.handler()
            );

            try {
                httpServer.start();

                URI endpoint = endpoint(httpServer);

                try (HttpClient client = HttpClient.newHttpClient()) {
                    String sessionId = initializeSession(
                            client,
                            endpoint
                    );

                    String requestBody = """
                            {
                              "jsonrpc": "2.0",
                              "id": 5,
                              "method": "tools/call",
                              "params": {
                                "name": "search",
                                "arguments": {
                                  "projectName": "project",
                                  "fileName": "contextapp",
                                  "extension": "java"
                                }
                              }
                            }
                            """;

                    HttpResponse<String> response = client.send(
                            toolRequest(
                                    endpoint,
                                    sessionId,
                                    requestBody
                            ),
                            HttpResponse.BodyHandlers.ofString()
                    );

                    assertEquals(200, response.statusCode());
                    assertTrue(
                            response.body().contains(
                                    jsonPath(file.toRealPath())
                            )
                    );
                }
            } finally {
                httpServer.stop();
            }
        }
    }

    private ReadFileTool readFileTool() throws IOException {
        ProjectRegistry registry = new ProjectRegistry(
                Map.of(
                        "project",
                        tempDir
                )
        );

        ReadFileService readFileService =
                new ReadFileService(registry);

        return new ReadFileTool(readFileService);
    }

    private GetCurrentTreeTool getCurrentTreeTool() throws IOException {
        ProjectRegistry registry = new ProjectRegistry(
                Map.of(
                        "project",
                        tempDir
                )
        );

        GetCurrentTreeService getCurrentTreeService =
                new GetCurrentTreeService(registry);

        return new GetCurrentTreeTool(getCurrentTreeService);
    }

    private SearchTool searchTool() throws IOException {
        ProjectRegistry registry = new ProjectRegistry(
                Map.of(
                        "project",
                        tempDir
                )
        );

        SearchService searchService =
                new SearchService(registry);

        return new SearchTool(searchService);
    }

    private static String initializeSession(
            HttpClient client,
            URI endpoint
    ) throws Exception {
        HttpResponse<String> initializeResponse = client.send(
                initializeRequest(endpoint),
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(200, initializeResponse.statusCode());

        String sessionId = initializeResponse.headers()
                .firstValue("Mcp-Session-Id")
                .orElseThrow();

        HttpResponse<String> initializedResponse = client.send(
                initializedRequest(
                        endpoint,
                        sessionId
                ),
                HttpResponse.BodyHandlers.ofString()
        );

        assertEquals(202, initializedResponse.statusCode());

        return sessionId;
    }

    private static HttpRequest initializeRequest(URI endpoint) {
        return HttpRequest.newBuilder()
                .uri(endpoint)
                .header("Content-Type", "application/json")
                .header(
                        "Accept",
                        "application/json, text/event-stream"
                )
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

    private static HttpRequest initializedRequest(
            URI endpoint,
            String sessionId
    ) {
        //noinspection UastIncorrectHttpHeaderInspection
        return HttpRequest.newBuilder()
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
    }

    private static HttpRequest toolRequest(
            URI endpoint,
            String sessionId,
            String body
    ) {
        //noinspection UastIncorrectHttpHeaderInspection
        return HttpRequest.newBuilder()
                .uri(endpoint)
                .header("Content-Type", "application/json")
                .header(
                        "Accept",
                        "application/json, text/event-stream"
                )
                .header("Mcp-Session-Id", sessionId)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
    }

    private static URI endpoint(JettyServer server) {
        return URI.create(
                "http://127.0.0.1:"
                        + server.localPort()
                        + McpEndpoint.MCP
        );
    }

    private static String jsonPath(Path path) {
        return path.toString()
                .replace("\\", "\\\\")
                .replace("\"", "\\\"");
    }

    private static String projectVersion() {
        Properties properties = new Properties();

        try (InputStream input =
                     ProjectContextMcpServerTest.class
                             .getResourceAsStream(VERSION_RESOURCE)) {

            if (input == null) {
                throw new IllegalStateException(
                        "Project version resource not found: "
                                + VERSION_RESOURCE
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