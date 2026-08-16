package io.github.w0nderfu11.projectcontext.mcp;

import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;

public final class ProjectContextMcpServer {

    private final McpSyncServer server;

    public ProjectContextMcpServer(McpHttpTransport transport) {
        this.server = McpServer.sync(transport.transport())
                .serverInfo("project-context", "0.1.0")
                .build();
    }
}