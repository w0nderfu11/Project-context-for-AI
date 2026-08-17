package io.github.w0nderfu11.projectcontext;

import io.github.w0nderfu11.projectcontext.application.PingService;
import io.github.w0nderfu11.projectcontext.application.ReadFileService;
import io.github.w0nderfu11.projectcontext.mcp.McpHttpTransport;
import io.github.w0nderfu11.projectcontext.mcp.ProjectContextMcpServer;
import io.github.w0nderfu11.projectcontext.mcp.tools.PingTool;
import io.github.w0nderfu11.projectcontext.mcp.tools.ReadFileTool;
import io.github.w0nderfu11.projectcontext.registry.ProjectRegistry;
import io.github.w0nderfu11.projectcontext.server.JettyServer;

import java.nio.file.Path;
import java.util.Map;

public final class ProjectContextApplication {

    public static void main(String[] args) throws Exception {
        ProjectRegistry projectRegistry = new ProjectRegistry(
                Map.of(
                        "project-context",
                        Path.of("D:\\project-context")
                )
        );

        PingService pingService = new PingService();
        ReadFileService readFileService =
                new ReadFileService(projectRegistry);

        PingTool pingTool = new PingTool(pingService);
        ReadFileTool readFileTool =
                new ReadFileTool(readFileService);

        McpHttpTransport transport = new McpHttpTransport();

        try (ProjectContextMcpServer ignored =
                     new ProjectContextMcpServer(
                             transport,
                             pingTool,
                             readFileTool
                     )) {

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