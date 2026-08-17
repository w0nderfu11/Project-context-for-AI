package io.github.w0nderfu11.projectcontext.mcp.tools;

import io.github.w0nderfu11.projectcontext.application.ReadFileService;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ReadFileTool {

    private static final String TOOL_NAME = "read_file";
    private static final String TOOL_TITLE = "Read File";
    private static final String TOOL_DESCRIPTION =
            "Reads a text file from a registered Project Context project";

    private static final String PROJECT_NAME_ARGUMENT = "projectName";
    private static final String FILE_PATH_ARGUMENT = "filePath";

    private static final Map<String, Object> INPUT_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    PROJECT_NAME_ARGUMENT, Map.of(
                            "type", "string",
                            "description", "Name of the registered project"
                    ),
                    FILE_PATH_ARGUMENT, Map.of(
                            "type", "string",
                            "description", "Full path to the file"
                    )
            ),
            "required", List.of(
                    PROJECT_NAME_ARGUMENT,
                    FILE_PATH_ARGUMENT
            ),
            "additionalProperties", false
    );

    private static final Map<String, Object> OUTPUT_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    "content", Map.of(
                            "type", "string"
                    )
            ),
            "required", List.of("content"),
            "additionalProperties", false
    );

    private final ReadFileService readFileService;

    public ReadFileTool(ReadFileService readFileService) {
        this.readFileService = Objects.requireNonNull(
                readFileService,
                "readFileService must not be null"
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
                    try {
                        String projectName = argument(
                                request,
                                PROJECT_NAME_ARGUMENT
                        );

                        String filePath = argument(
                                request,
                                FILE_PATH_ARGUMENT
                        );

                        String content = readFileService.read(
                                projectName,
                                Path.of(filePath)
                        );

                        return McpSchema.CallToolResult.builder()
                                .content(List.of(
                                        McpSchema.TextContent.builder(content)
                                                .build()
                                ))
                                .structuredContent(Map.of(
                                        "content", content
                                ))
                                .isError(false)
                                .build();
                    } catch (IllegalArgumentException | IOException e) {
                        return McpSchema.CallToolResult.builder()
                                .content(List.of(
                                        McpSchema.TextContent.builder(
                                                e.getMessage()
                                        ).build()
                                ))
                                .isError(true)
                                .build();
                    }
                })
                .build();
    }

    private static String argument(
            McpSchema.CallToolRequest request,
            String name
    ) {
        Object value = request.arguments().get(name);

        if (!(value instanceof String stringValue)
                || stringValue.isBlank()) {
            throw new IllegalArgumentException(
                    "argument must be a non-blank string: " + name
            );
        }

        return stringValue;
    }
}