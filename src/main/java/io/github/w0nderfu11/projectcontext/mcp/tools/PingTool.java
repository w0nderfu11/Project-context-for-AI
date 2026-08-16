package io.github.w0nderfu11.projectcontext.mcp.tools;

import io.github.w0nderfu11.projectcontext.application.PingService;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class PingTool {

    private static final String TOOL_NAME = "ping";
    private static final String TOOL_TITLE = "Ping";
    private static final String TOOL_DESCRIPTION =
            "Checks that project context is available";

    private static final Map<String, Object> INPUT_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(),
            "additionalProperties", false
    );

    private static final Map<String, Object> OUTPUT_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    "message", Map.of(
                            "type", "string"
                    )
            ),
            "required", List.of("message"),
            "additionalProperties", false
    );

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
                        INPUT_SCHEMA
                )
                .title(TOOL_TITLE)
                .description(TOOL_DESCRIPTION)
                .outputSchema(OUTPUT_SCHEMA)
                .annotations(
                        McpSchema.ToolAnnotations.builder()
                                .readOnlyHint(true)
                                .destructiveHint(false)
                                .openWorldHint(false)
                                .idempotentHint(true)
                                .build()
                )
                .meta(Map.of(
                        "openai/visibility", "public"
                ))
                .build();

        return McpServerFeatures.SyncToolSpecification.builder()
                .tool(tool)
                .callHandler((exchange, request) -> {
                    String message = pingService.ping();

                    return McpSchema.CallToolResult.builder()
                            .content(List.of(
                                    McpSchema.TextContent.builder(message)
                                            .build()
                            ))
                            .structuredContent(Map.of(
                                    "message", message
                            ))
                            .isError(false)
                            .build();
                })
                .build();
    }
}