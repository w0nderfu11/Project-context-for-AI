package io.github.w0nderfu11.projectcontext.mcp.tools;

import io.github.w0nderfu11.projectcontext.application.GetCurrentTreeService;
import io.github.w0nderfu11.projectcontext.application.TreeEntryType;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class GetCurrentTreeTool {

    private static final String TOOL_NAME = "get_current_tree";
    private static final String TOOL_TITLE = "Get Current Tree";
    private static final String TOOL_DESCRIPTION =
            "Lists files and directories one level below a directory in a registered Project Context project";

    private static final String PROJECT_NAME_ARGUMENT = "projectName";
    private static final String DIRECTORY_PATH_ARGUMENT = "directoryPath";

    private static final Map<String, Object> INPUT_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    PROJECT_NAME_ARGUMENT, Map.of(
                            "type", "string",
                            "description", "Name of the registered project"
                    ),
                    DIRECTORY_PATH_ARGUMENT, Map.of(
                            "type", "string",
                            "description", "Full path to the directory"
                    )
            ),
            "required", List.of(
                    PROJECT_NAME_ARGUMENT,
                    DIRECTORY_PATH_ARGUMENT
            ),
            "additionalProperties", false
    );

    private static final Map<String, Object> OUTPUT_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    "entries", Map.of(
                            "type", "object",
                            "additionalProperties", Map.of(
                                    "type", "string",
                                    "enum", List.of(
                                            TreeEntryType.FILE.name(),
                                            TreeEntryType.DIRECTORY.name()
                                    )
                            )
                    )
            ),
            "required", List.of("entries"),
            "additionalProperties", false
    );

    private final GetCurrentTreeService getCurrentTreeService;

    public GetCurrentTreeTool(
            GetCurrentTreeService getCurrentTreeService
    ) {
        this.getCurrentTreeService = Objects.requireNonNull(
                getCurrentTreeService,
                "getCurrentTreeService must not be null"
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

                        String directoryPath = argument(
                                request,
                                DIRECTORY_PATH_ARGUMENT
                        );

                        Map<String, TreeEntryType> tree =
                                getCurrentTreeService.get(
                                        projectName,
                                        Path.of(directoryPath)
                                );

                        Map<String, String> entries = new HashMap<>();

                        tree.forEach((path, type) ->
                                entries.put(
                                        path,
                                        type.name()
                                )
                        );

                        return McpSchema.CallToolResult.builder()
                                .content(List.of(
                                        McpSchema.TextContent.builder(
                                                entries.toString()
                                        ).build()
                                ))
                                .structuredContent(Map.of(
                                        "entries",
                                        entries
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