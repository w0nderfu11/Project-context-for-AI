package io.github.w0nderfu11.projectcontext.mcp.tools;

import io.github.w0nderfu11.projectcontext.application.PingService;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.List;
import java.util.Map;

public final class PingTool {

    private final PingService pingService;

    public PingTool(PingService pingService) {
        this.pingService = pingService;
    }

    public McpServerFeatures.SyncToolSpecification specification() {
        McpSchema.Tool tool = McpSchema.Tool.builder(
                        "ping",
                        Map.of(
                                "type", "object",
                                "properties", Map.of()
                        )
                )
                .description("Checks that Project Context is available")
                .build();

        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler((exchange, request) ->
                        McpSchema.CallToolResult.builder()
                                .content(List.of(
                                        McpSchema.TextContent.builder(pingService.ping())
                                                .build()
                                ))
                                .isError(false)
                                .build()
                )
                .build();
    }
}