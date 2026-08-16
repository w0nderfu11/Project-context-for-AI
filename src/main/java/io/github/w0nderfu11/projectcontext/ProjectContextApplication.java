package io.github.w0nderfu11.projectcontext;

import io.github.w0nderfu11.projectcontext.application.PingService;
import io.github.w0nderfu11.projectcontext.mcp.McpHttpTransport;
import io.github.w0nderfu11.projectcontext.mcp.ProjectContextMcpServer;
import io.github.w0nderfu11.projectcontext.mcp.tools.PingTool;
import io.github.w0nderfu11.projectcontext.server.JettyServer;

public final class ProjectContextApplication {

    public static void main(String[] args) throws Exception {
        PingService pingService = new PingService();
        PingTool pingTool = new PingTool(pingService);

        McpHttpTransport transport = new McpHttpTransport();

        try (ProjectContextMcpServer ignored =
                     new ProjectContextMcpServer(transport, pingTool)) {

            JettyServer server = new JettyServer(
                    "127.0.0.1",
                    8765,
                    transport.handler()
            );

            server.start();
            server.join();
        }
    }
}