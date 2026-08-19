package io.github.w0nderfu11.projectcontext.mcp;

import io.github.w0nderfu11.projectcontext.mcp.tools.GetCurrentTreeTool;
import io.github.w0nderfu11.projectcontext.mcp.tools.PingTool;
import io.github.w0nderfu11.projectcontext.mcp.tools.ReadFileTool;
import io.github.w0nderfu11.projectcontext.mcp.tools.SearchTool;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;

import java.io.IOException;
import java.util.Objects;
import java.util.Properties;

public final class ProjectContextMcpServer implements AutoCloseable {

    private static final String SERVER_NAME = "project-context";
    private static final String VERSION_RESOURCE = "/project-context.properties";
    private static final String VERSION_PROPERTY = "project.version";

    private final McpSyncServer server;

    public ProjectContextMcpServer(
            McpHttpTransport transport,
            PingTool pingTool,
            ReadFileTool readFileTool,
            GetCurrentTreeTool getCurrentTreeTool,
            SearchTool searchTool
    ) {
        Objects.requireNonNull(transport, "transport must not be null");
        Objects.requireNonNull(pingTool, "pingTool must not be null");
        Objects.requireNonNull(readFileTool, "readFileTool must not be null");
        Objects.requireNonNull(
                getCurrentTreeTool,
                "getCurrentTreeTool must not be null"
        );
        Objects.requireNonNull(
                searchTool,
                "searchTool must not be null"
        );

        this.server = McpServer.sync(transport.transport())
                .serverInfo(SERVER_NAME, projectVersion())
                .tools(
                        pingTool.specification(),
                        readFileTool.specification(),
                        getCurrentTreeTool.specification(),
                        searchTool.specification()
                )
                .build();
    }

    @Override
    public void close() {
        server.close();
    }

    private static String projectVersion() {
        Properties properties = new Properties();

        try (var input = ProjectContextMcpServer.class
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