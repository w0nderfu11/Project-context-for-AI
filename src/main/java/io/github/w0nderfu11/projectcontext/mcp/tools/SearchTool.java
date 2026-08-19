package io.github.w0nderfu11.projectcontext.mcp.tools;

import io.github.w0nderfu11.projectcontext.application.SearchService;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class SearchTool {

    private static final String TOOL_NAME = "search";
    private static final String TOOL_TITLE = "Search";
    private static final String TOOL_DESCRIPTION =
            "Searches for files by name and extension in a registered Project Context project";

    private static final String PROJECT_NAME_ARGUMENT = "projectName";
    private static final String FILE_NAME_ARGUMENT = "fileName";
    private static final String EXTENSION_ARGUMENT = "extension";
    private static final String DIRECTORY_PATH_ARGUMENT = "directoryPath";

    private static final Map<String, Object> INPUT_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    PROJECT_NAME_ARGUMENT, Map.of(
                            "type", "string",
                            "description", "Name of the registered project"
                    ),
                    FILE_NAME_ARGUMENT, Map.of(
                            "type", "string",
                            "description", "Full or partial file name"
                    ),
                    EXTENSION_ARGUMENT, Map.of(
                            "type", "string",
                            "description", "Exact file extension without the dot"
                    ),
                    DIRECTORY_PATH_ARGUMENT, Map.of(
                            "type", "string",
                            "description", "Optional full path to a directory that limits the search scope"
                    )
            ),
            "required", List.of(
                    PROJECT_NAME_ARGUMENT,
                    FILE_NAME_ARGUMENT,
                    EXTENSION_ARGUMENT
            ),
            "additionalProperties", false
    );

    private static final Map<String, Object> OUTPUT_SCHEMA = Map.of(
            "type", "object",
            "properties", Map.of(
                    "paths", Map.of(
                            "type", "array",
                            "items", Map.of(
                                    "type", "string"
                            )
                    )
            ),
            "required", List.of("paths"),
            "additionalProperties", false
    );

    private final SearchService searchService;

    public SearchTool(SearchService searchService) {
        this.searchService = Objects.requireNonNull(
                searchService,
                "searchService must not be null"
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

                        String fileName = argument(
                                request,
                                FILE_NAME_ARGUMENT
                        );

                        String extension = argument(
                                request,
                                EXTENSION_ARGUMENT
                        );

                        String directoryPath = optionalArgument(
                                request
                        );

                        List<String> paths = searchService.search(
                                projectName,
                                fileName,
                                extension,
                                directoryPath == null
                                        ? null
                                        : Path.of(directoryPath)
                        );

                        return McpSchema.CallToolResult.builder()
                                .content(List.of(
                                        McpSchema.TextContent.builder(
                                                paths.toString()
                                        ).build()
                                ))
                                .structuredContent(Map.of(
                                        "paths",
                                        paths
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

    private static String optionalArgument(
            McpSchema.CallToolRequest request
    ) {
        Object value = request.arguments().get(DIRECTORY_PATH_ARGUMENT);

        if (value == null) {
            return null;
        }

        if (!(value instanceof String stringValue)
                || stringValue.isBlank()) {
            throw new IllegalArgumentException(
                    "argument must be a non-blank string: "
                            + DIRECTORY_PATH_ARGUMENT
            );
        }

        return stringValue;
    }
}