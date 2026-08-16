package io.github.w0nderfu11.projectcontext.mcp.tools;

import io.github.w0nderfu11.projectcontext.application.PingService;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class PingTool {

    private static final String TOOL_NAME = "ping";
    private static final String TOOL_DESCRIPTION =
            "Checks that Project Context is available";

    private final PingService pingService;

    public PingTool(PingService pingService) {
        this.pingService = Objects.requireNonNull(
                pingService,
                "pingService must not be null"
        );
    }

    public McpServerFeatures.SyncToolSpecification specification() {
        McpSchema.Tool tool = McpSchema.Tool.builder(
                        TOOL_NAME,
                        Map.of(
                                "type", "object",
                                "properties", Map.of()
                        )
                )
                .description(TOOL_DESCRIPTION)
                .build();

        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler((exchange, request) ->
                        McpSchema.CallToolResult.builder()
                                .content(List.of(
                                        McpSchema.TextContent.builder(
                                                        pingService.ping()
                                                )
                                                .build()
                                ))
                                .isError(false)
                                .build()
                )
                .build();
    }
}